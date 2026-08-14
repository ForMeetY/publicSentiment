package com.weibo.etl.userPortrait

import com.weibo.utils.Api.CallBatchApi
import com.weibo.utils.mysqlUtils.WriteMysql
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * @author Xbx
 * @date 2026/5/29 21:12
 * 用户画像特征工程
 */

object UserPortrait {

  // 进行全量用户的统计
  def fullUserProfile(spark: SparkSession): Unit = {
    import spark.implicits._
    import org.apache.spark.sql.functions._
    println("开始聚合用户特征，准备全量GMM预测")

    val nowStr = java.time.LocalDateTime.now()
      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    // 读取情绪预测表
    val predDF = spark.table("weibo.predstatistic").cache()

    // 1+2  情绪和时段特征
    val feat12 = predDF
      .groupBy("user_id")
      .agg(
        first("screen_name", ignoreNulls = true).as("screen_name"),
        //  把认证信息带下来
        first("user_authentication", ignoreNulls = true).as("user_authentication"),
        // 计算情绪的标准差作为 跨小时情绪波动度
        round(stddev_samp("score"), 4).as("hourly_sentiment_stddev"),
        // 统计夜间发帖比例
        round(
          sum(when(col("hour") >= 23 || col("hour") < 4, 1).otherwise(0)).cast("double") / count("*"), 4
        ).as("night_post_ratio")
      )
      .na.fill(0.0, Seq("hourly_sentiment_stddev"))
      .na.fill("普通用户", Seq("user_authentication"))


    // 先处理地域的格式
    val explodedRegion = predDF
      .filter(col("location").isNotNull && col("location") =!= "")
      .select(col("user_id"), col("score"),
        explode(split(col("location"), " \\| ")).as("region"))
      .filter(col("region").isNotNull && col("region") =!= "")

    // 最关注的地方
    val topRegionDF = explodedRegion
      .groupBy("user_id", "region")
      // 统计每个用户在每个地区的发帖数
      .agg(count("*").as("cnt"), round(avg("score"), 4).as("region_avg_score"))
      .withColumn("rk", row_number().over(
        // 按cnt降序
        org.apache.spark.sql.expressions.Window.partitionBy("user_id").orderBy(desc("cnt"))))
      // 取发帖最多的地域
      .filter(col("rk") === 1)
      .select(col("user_id"), col("region_avg_score").as("top_region_avg_score"))

    // 地域多样性
    val regionDiversityDF = explodedRegion
      .groupBy("user_id")
      // 关注过多少地方
      .agg(countDistinct("region").as("region_diversity_score"))

    // 3+4  最关注的地狱和地狱多样性
    val feat34 = topRegionDF
      .join(regionDiversityDF, Seq("user_id"), "left")
      .na.fill(0.0, Seq("top_region_avg_score"))
      .na.fill(0L, Seq("region_diversity_score"))

    //  情绪杠杆率
    //  情绪杠杆率
    // 情绪杠杆率
    val feat5 = predDF
      .groupBy("user_id")
      .agg(
        // 负面篇均互动
        round(
          sum(when(col("label") === "负面",
            col("reposts_count") + col("comments_count") + col("attitudes_count")).otherwise(0)).cast("double") /
            greatest(sum(when(col("label") === "负面", 1).otherwise(0)).cast("double"), lit(1.0)), 2
        ).as("neg_avg"),
        // 正面篇均互动
        round(
          sum(when(col("label") === "正面",
            col("reposts_count") + col("comments_count") + col("attitudes_count")).otherwise(0)).cast("double") /
            greatest(sum(when(col("label") === "正面", 1).otherwise(0)).cast("double"), lit(1.0)), 2
        ).as("pos_avg")
      )
      // 原始杠杆计算
      .withColumn("sentiment_leverage_raw",
        col("neg_avg") / when(col("pos_avg") === 0, lit(1.0)).otherwise(col("pos_avg"))
      )
      .withColumn("sentiment_leverage",
        when(
          // 判断NaN：自己不等于自己
          col("sentiment_leverage_raw") =!= col("sentiment_leverage_raw")
            // 判断无穷大/负无穷：绝对值极大
            || abs(col("sentiment_leverage_raw")) > lit(1e300),
          lit(0.0)
        )
          // 上限匹配 decimal(10,4) 最大值 999999.9999
          .when(col("sentiment_leverage_raw") > lit(999999.9999), lit(999999.9999))
          .when(col("sentiment_leverage_raw") < lit(-999999.9999), lit(-999999.9999))
          .otherwise(round(col("sentiment_leverage_raw"), 4))
      )
      .select("user_id", "sentiment_leverage")
      .na.fill(0.0, Seq("sentiment_leverage"))
      //比值越高说明这个用户发负面内容比发正面内容更容易引发互动，煽动能效越强

    // 拼接成一个大表
    val featureDF = feat12
      .join(feat34, Seq("user_id"), "left")
      .join(feat5, Seq("user_id"), "left")
      .na.fill(0.0)
      .withColumn("region_diversity_score", col("region_diversity_score").cast("long"))
      .cache()


    println(s"特征聚合完成，共 ${featureDF.count()} 条，开始批量调用GMM接口...")
    predDF.unpersist()

    // 1. 拦截官媒
    val officialDF = featureDF.filter(
      col("user_authentication").contains("官方") ||
        col("user_authentication").contains("机构") ||
        col("user_authentication").contains("蓝V") ||
        col("screen_name").like("%官微%") ||
        col("screen_name").like("%融媒%") ||
        col("screen_name").like("%新闻%") ||
        col("screen_name").like("%发布%")
    ).cache() // 建议cache，因为后面多次left_anti用到

    // 分流：沉默用户不调接口剔除掉官媒
    val silentDF = featureDF.filter(
      col("hourly_sentiment_stddev") <= 0.001 &&
        col("night_post_ratio") === 0.0 &&
        col("region_diversity_score") === 0L &&
        col("sentiment_leverage") === 0.0
    ).join(officialDF, Seq("user_id"), "left_anti")

    // 需要调用接口的活跃普通网民
    val activeDF = featureDF
      .join(officialDF, Seq("user_id"), "left_anti")
      .join(silentDF, Seq("user_id"), "left_anti")
      .cache()

    println(s"机构官媒: ${officialDF.count()} 人 | 沉默路人: ${silentDF.count()} 人 | 活跃预测网民: ${activeDF.count()} 人")

    import scala.collection.JavaConverters._

    // 活跃用户调接口拼结果
    val activeRows = activeDF.collectAsList().asScala.toList
    val resultMap = CallBatchApi.batchGmmPredict(activeRows)

    val activeResult = activeRows.map { row =>
      val uid = row.getAs[String]("user_id")
      val (clusterId, prob0, prob1, prob2, prob3) = resultMap.getOrElse(uid, (-1, 0.25, 0.25, 0.25, 0.25))
      (uid, row.getAs[String]("screen_name"),
        row.getAs[Double]("hourly_sentiment_stddev"), row.getAs[Double]("night_post_ratio"),
        row.getAs[Long]("region_diversity_score"), java.util.Optional.ofNullable(row.getAs[Double]("top_region_avg_score")).orElse(0.0),
        row.getAs[Double]("sentiment_leverage"),
        clusterId, prob0, prob1, prob2, prob3, nowStr)
    }

    // 沉默用户直接填默认值
    val silentRows = silentDF.collectAsList().asScala.toList
    val silentResult = silentRows.map { row =>
      val uid = row.getAs[String]("user_id")
      (uid, row.getAs[String]("screen_name"),
        row.getAs[Double]("hourly_sentiment_stddev"), row.getAs[Double]("night_post_ratio"),
        row.getAs[Long]("region_diversity_score"), java.util.Optional.ofNullable(row.getAs[Double]("top_region_avg_score")).orElse(0.0),
        row.getAs[Double]("sentiment_leverage"),
        -1, 0.25, 0.25, 0.25, 0.25, nowStr)
    }

    // 官媒结果（cluster_id = 99）
    val officialRows = officialDF.collectAsList().asScala.toList
    val officialResult = officialRows.map { row =>
      val uid = row.getAs[String]("user_id")
      (uid, row.getAs[String]("screen_name"),
        row.getAs[Double]("hourly_sentiment_stddev"), row.getAs[Double]("night_post_ratio"),
        row.getAs[Long]("region_diversity_score"), java.util.Optional.ofNullable(row.getAs[Double]("top_region_avg_score")).orElse(0.0),
        row.getAs[Double]("sentiment_leverage"),
        99, 0.0, 0.0, 0.0, 0.0, nowStr) // 99专属标志位，概率全清空
    }

    // 结果拼接
    val profileDF = (activeResult ++ silentResult ++ officialResult).toDF(
      "user_id", "screen_name",
      "hourly_sentiment_stddev", "night_post_ratio",
      "region_diversity_score", "top_region_avg_score", "sentiment_leverage",
      "cluster_id", "prob_0", "prob_1", "prob_2", "prob_3", "update_time"
    )

    WriteMysql.writeTable(profileDF, "user_gmm_profile")

    // 善后释放内存
    activeDF.unpersist()
    officialDF.unpersist()
    featureDF.unpersist()
    println("完成画像")

  }



}