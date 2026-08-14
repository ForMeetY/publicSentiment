package com.weibo.etl

import java.time.{LocalDate, LocalTime}

import org.apache.spark.sql.functions.{max,asc, avg, col, count, countDistinct, date_format, desc, explode, first, hour, lit, round, row_number, split, stddev_samp, sum, when}
import org.apache.spark.sql.{DataFrame, SparkSession, functions}
import com.weibo.utils.hiveUtils.{HiveTableExists, ReadHiveUtils}
import org.apache.spark.sql.streaming.Trigger
import com.weibo.utils.mysqlUtils.WriteMysql
import com.weibo.utils.Api.CallBatchApi
import com.weibo.etl.userPortrait.UserPortrait
import com.weibo.utils.Constant
import org.apache.spark.sql.expressions.Window
import org.apache.spark.storage.StorageLevel

/**
 * @author Xbx
 * @date 2026/5/7 22:39
 */
object Classify {


  // 微博话题分类（增量）
  def incrementalTopicClassify(spark: SparkSession): Unit = {
    import spark.implicits._

    println("正在执行：增量话题分类")

    spark.sql("SET hive.exec.dynamic.partition = true")
    spark.sql("SET hive.exec.dynamic.partition.mode = nonstrict")

    val rawDF = ReadHiveUtils.readHiveBySpecifiedDay(spark, "weibo", "classbydayandhour", Constant.time)

    val classifiedDF = if (HiveTableExists.tableExists(spark, "weibo", "topic_classify")) {
      spark.table("weibo.topic_classify").select("id")
    } else {
      spark.sql(
        """
          |CREATE TABLE IF NOT EXISTS weibo.topic_classify (
          |    id                  STRING,
          |    bid                 STRING,
          |    user_id             STRING,
          |    screen_name         STRING,
          |    text                STRING,
          |    topic_label         STRING,
          |    topic_id            INT,
          |    confidence          DOUBLE,
          |    created_at          TIMESTAMP,
          |    location            STRING,
          |    user_authentication STRING,
          |    hour                INT,
          |    time_period         STRING,
          |    tags_generated_at   TIMESTAMP
          |)
          |PARTITIONED BY (dt STRING)
          |STORED AS PARQUET
          |TBLPROPERTIES ("parquet.compression" = "snappy")
          |""".stripMargin
      )
      spark.table("weibo.topic_classify").select("id")
    }

    // 只保留需要的列，减少 collect 时的传输量
    val needDF = rawDF
      .join(classifiedDF, Seq("id"), "left_anti")
      .filter(col("text").isNotNull && col("text") =!= "")
      .select(
        "id", "bid", "user_id", "screen_name", "text",
        "created_at", "location", "user_authentication",
        "hour", "time_period", "dt"
      ).cache()

    val cnt = needDF.count()
    println(s"待分类数据：$cnt 条")
    if (cnt == 0) { println("暂无新数据需要分类"); return }

    val rows = needDF.repartition(10).collectAsList()
    import scala.collection.JavaConverters._
    val rowsScala = rows.asScala.toList
    val texts = rowsScala.map(_.getAs[String]("text"))
    val resultMap = CallBatchApi.batchTopicPredict(texts)

    val nowTs = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())

    val writeDF = rowsScala.map { row =>
      val text = row.getAs[String]("text")
      val (label, labelId, confidence) = resultMap(text)
      (
        row.getAs[String]("id"),
        row.getAs[String]("bid"),
        row.getAs[String]("user_id"),
        row.getAs[String]("screen_name"),
        text, label, labelId, confidence,
        row.getAs[java.sql.Timestamp]("created_at"),
        row.getAs[String]("location"),
        row.getAs[String]("user_authentication"),
        row.getAs[Int]("hour"),
        row.getAs[String]("time_period"),
        nowTs,
        row.getAs[String]("dt")
      )
    }.toDF(
      "id", "bid", "user_id", "screen_name", "text",
      "topic_label", "topic_id", "confidence",
      "created_at", "location", "user_authentication",
      "hour", "time_period", "tags_generated_at", "dt"
    )

    writeDF.write.mode("append").insertInto("weibo.topic_classify")
    println(s"话题分类写入完成，共写入 $cnt 条")
  }


  // 全量用户画像刷新

  def fullUserPortrait(spark: SparkSession): Unit = {
    UserPortrait.fullUserProfile(spark)
  }


  // 情感分析（增量）
  def incrementalSentimentAnalysis(spark: SparkSession): Unit = {
    import spark.implicits._
    import scala.collection.JavaConverters._
    println("正在执行：增量情感分析")

    spark.sql("SET hive.exec.dynamic.partition = true")
    spark.sql("SET hive.exec.dynamic.partition.mode = nonstrict")

    // 读取原始微博表
    val rawDF = ReadHiveUtils
      .readHiveBySpecifiedDay(spark, "weibo", "classbydayandhour", Constant.time)
    // 读取已经预测的微博表
    val predDF = spark.table("weibo.predstatistic").select("id")

    // 只 select 后续需要的列，不拉全表
    // 做 左反连接 只保留 左表 rawDF 中，在右表 predDF 里id完全匹配不到 的所有行
    val needDF = rawDF
      .join(predDF, Seq("id"), "left_anti")
      .filter(col("text").isNotNull && col("text") =!= "")
      .select(
        "id", "bid", "user_id", "screen_name", "text", "topics",
        "reposts_count", "comments_count", "attitudes_count",
        "created_at", "location", "user_authentication",
        "hour", "time_period", "dt"
      ).cache()

    val cnt = needDF.count()
    println(s"待预测数据：$cnt 条")
    if (cnt == 0) { println("暂无新数据需要预测"); return }

    val rows = needDF.repartition(10).collectAsList().asScala.toList
    val texts = rows.map(_.getAs[String]("text"))
    val resultMap = CallBatchApi.batchSentimentPredict(texts)

    needDF.unpersist()

    val writeDF = rows.map { row =>
      val text = row.getAs[String]("text")
      val (label, labelId, score, confidence) = resultMap(text)
      (
        row.getAs[String]("id"),
        row.getAs[String]("bid"),
        row.getAs[String]("user_id"),
        row.getAs[String]("screen_name"),
        text,
        row.getAs[String]("topics"),
        row.getAs[Int]("reposts_count"),
        row.getAs[Int]("comments_count"),
        row.getAs[Int]("attitudes_count"),
        row.getAs[java.sql.Timestamp]("created_at"),
        row.getAs[String]("location"),
        row.getAs[String]("user_authentication"),
        row.getAs[Int]("hour"),
        row.getAs[String]("time_period"),
        label, labelId, score, confidence,
        row.getAs[String]("dt")
      )
    }.toDF(
      "id", "bid", "user_id", "screen_name", "text", "topics",
      "reposts_count", "comments_count", "attitudes_count",
      "created_at", "location", "user_authentication",
      "hour", "time_period",
      "label", "label_id", "score", "confidence", "dt"
    )

    writeDF.write.mode("append").insertInto("weibo.predstatistic")
    println(s"情感预测写入完成，共写入 $cnt 条")
  }


  // 流式写入 Hive（classbydayandhour）
  def classByDayAndHour(spark: SparkSession, data: DataFrame): Unit = {
    println("进入方法 classByDayAndHour")
    data.writeStream
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        println(s"batchId=$batchId, count=${batchDF.count()}")
        val df2 = batchDF
          .withColumn("created_at", org.apache.spark.sql.functions.to_timestamp(col("created_at"), "yyyy-MM-dd HH:mm"))
          .withColumn("hour", hour(col("created_at")))
          .withColumn("time_period",
            // 发微博的时段打上标签
            when(col("hour") < 6, "凌晨")
              .when(col("hour") < 9, "早晨")
              .when(col("hour") < 12, "上午")
              .when(col("hour") < 18, "下午")
              .when(col("hour") < 22, "晚间")
              .otherwise("午夜")
          )
          .withColumn("dt", date_format(col("created_at"), "yyyy-MM-dd"))
          .withColumn("reposts_count",  col("reposts_count").cast("int"))
          .withColumn("comments_count", col("comments_count").cast("int"))
          .withColumn("attitudes_count", col("attitudes_count").cast("int"))

        val finalDF = df2.select(
          "id", "bid", "user_id", "screen_name", "text", "topics",
          "reposts_count", "comments_count", "attitudes_count",
          "created_at", "location", "user_authentication",
          "hour", "time_period", "dt"
        )

        try {
          if (!HiveTableExists.tableExists(spark, "weibo", "classbydayandhour")) {
            spark.sql(
              """
                |CREATE TABLE IF NOT EXISTS weibo.classbydayandhour (
                |   id STRING, bid STRING, user_id STRING, screen_name STRING,
                |   text STRING, topics STRING,
                |   reposts_count INT, comments_count INT, attitudes_count INT,
                |   created_at TIMESTAMP, location STRING,
                |   user_authentication STRING, hour INT, time_period STRING
                |) PARTITIONED BY (dt STRING) STORED AS PARQUET
                |""".stripMargin
            )
          }
          finalDF.write.mode("append").insertInto("weibo.classbydayandhour")
          spark.sql("MSCK REPAIR TABLE weibo.classbydayandhour")
          println("Hive 写入成功")
        } catch {
          case e: Exception =>
            println("写入 Hive 失败")
            e.printStackTrace()
        }
      }
      .option("checkpointLocation", "./checkpoint/weibo")
      .trigger(Trigger.ProcessingTime("220 seconds"))
      .start()
  }

  // 聚合统计MySQL
  def classByIndicator(spark: SparkSession): Unit = {
    println("进入方法 classByIndicator")

    if (!HiveTableExists.tableExists(spark, "weibo", "classbydayandhour")) {
      println("classbydayandhour 表不存在"); return
    }

    val data = ReadHiveUtils.readHiveclassByDayAndHour(spark, "weibo", "classbydayandhour")
    // 数据量大时允许溢写磁盘
    data.persist(StorageLevel.MEMORY_AND_DISK)

    val nowStr = java.time.LocalDateTime.now()
      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    val userCount         = data.groupBy("user_authentication").count().withColumn("create_time", lit(nowStr))
    val timePeriodCount   = data.groupBy("time_period").count().withColumn("create_time", lit(nowStr))
    val timeHourCount     = data.groupBy("hour").count().orderBy("hour").withColumn("create_time", lit(nowStr))
    val timeSeriesCount   = data.groupBy("dt").count().orderBy("dt").withColumn("create_time", lit(nowStr))
    val timePeriodUserCount = data.groupBy("time_period", "user_authentication").count().withColumn("create_time", lit(nowStr))
    val regionCount       = data
      .filter(col("location").isNotNull && col("location") =!= "")
      .select(explode(split(col("location"), " \\| ")).as("region"))
      .filter(col("region").isNotNull && col("region") =!= "")
      .groupBy("region").count().orderBy(desc("count"))
      .withColumn("create_time", lit(nowStr))

    WriteMysql.writeTable(userCount,           "user_post_count")
    WriteMysql.writeTable(timePeriodCount,     "time_period_count")
    WriteMysql.writeTable(timeHourCount,       "hour_post_count")
    WriteMysql.writeTable(timeSeriesCount,     "day_post_count")
    WriteMysql.writeTable(timePeriodUserCount, "time_period_user_count")
    WriteMysql.writeTable(regionCount,         "regionHot")

    // 写完 classbydayandhour 相关统计后立即释放，再加载 predstatistic
    data.unpersist()

    if (!HiveTableExists.tableExists(spark, "weibo", "predstatistic")) {
      println("predstatistic 表不存在"); return
    }

    val preData = ReadHiveUtils.readHiveclassByDayAndHour(spark, "weibo", "predstatistic")
    // computeSentiment 和 computeUserPortrait 会多次扫描
    preData.persist(StorageLevel.MEMORY_AND_DISK)

    if (preData.isEmpty) { println("预测表为空"); preData.unpersist(); return }

    // 情感统计
    computeSentiment(preData, nowStr)

    // 用户画像（定时窗口内才做，避免每次都跑重计算）
    import java.util.{Timer, TimerTask}
    val timer = new Timer(true)
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        try {
          val nowTime = LocalTime.now()
          val start   = LocalTime.of(Constant.startHour, Constant.startMin)
          val end     = LocalTime.of(Constant.endHour,   Constant.endMin)
          if (nowTime.isAfter(start) && nowTime.isBefore(end)) {
            println("启动全量用户画像")
            computeUserPortrait(preData, nowStr)
            println("夜间画像完成")
          }
        } catch {
          case e: Exception =>
            println("画像统计出错，跳过本次")
            e.printStackTrace()
        }
      }
    }, 0, 1000 * 60 * 25)

    // 画像任务是异步定时器，主线程在这里等足够的窗口期后再 unpersist
    // 这里保守地等计算完后由调用方决定；当前保持 unpersist 在方法末尾
    preData.unpersist()
  }


  // 情感统计模块
  def computeSentiment(preData: DataFrame, nowStr: String): Unit = {

    // 情感总体分布
    val labelCount = preData
      .filter(col("label").isin("正面", "负面", "中性"))
      .groupBy("label").count()
      .withColumnRenamed("count", "value")
      .withColumn("create_time", lit(nowStr))

    // 时段 + 情感
    val labelCountByTime = preData
      .groupBy("time_period", "label").count()
      .withColumnRenamed("count", "value")
      .withColumn("create_time", lit(nowStr))

    // 用户认证 + 情感
    val labelCountByUser = preData
      .groupBy("user_authentication", "label").count()
      .withColumnRenamed("count", "value")
      .withColumn("create_time", lit(nowStr))

    // 互动量 + 情感
    val interactionCountByLabel = preData
      .groupBy("label")
      .agg(
        round(avg("reposts_count"),  2).alias("avg_repost"),
        round(avg("comments_count"), 2).alias("avg_comment"),
        round(avg("attitudes_count"),2).alias("avg_attitude")
      )
      .withColumn("create_time", lit(nowStr))

    // 地区综合情感热度
    val regionHeatStatistic = preData
      .filter(col("location").isNotNull && col("location") =!= "" && col("score").isNotNull)
      .select(explode(split(col("location"), " \\| ")).as("region"), col("score"))
      .filter(col("region").isNotNull && col("region") =!= "")
      .groupBy("region")
      .agg(avg("score").as("avg_score"), count("*").as("count"))
      .filter(col("count") >= 5)
      .orderBy(desc("avg_score"))
      .withColumn("create_time", lit(nowStr))

    // dayLabelCount 被两次用到，提前 persist 避免重复扫描 preData
    val dayLabelCount = preData
      .filter(col("label").isNotNull && col("label") =!= "")
      .groupBy("dt", "label").count()
      .withColumnRenamed("count", "value")
      .persist(StorageLevel.MEMORY_AND_DISK)

    val dayTotalCount = dayLabelCount.groupBy("dt").agg(sum("value").as("total"))

    val dayLabelRatio = dayLabelCount
      .join(dayTotalCount, "dt")
      .withColumn("ratio", round(col("value") / col("total") * 100, 2))
      .withColumn("create_time", lit(nowStr))
      .orderBy("dt")

    // 置信度分布
    val confidenceLevelCount = preData
      .filter(col("confidence").isNotNull)
      .withColumn("level",
        when(col("confidence") >= 0.9, "高置信")
          .when(col("confidence") >= 0.7, "中置信")
          .otherwise("低置信")
      )
      .groupBy("level").count()
      .withColumnRenamed("count", "value")
      .withColumn("create_time", lit(nowStr))

    WriteMysql.writeTable(labelCount,             "label_count")
    WriteMysql.writeTable(labelCountByTime,       "label_count_by_time")
    WriteMysql.writeTable(labelCountByUser,       "user_auth_label_count")
    WriteMysql.writeTable(interactionCountByLabel,"interaction_count_by_label")
    WriteMysql.writeTable(regionHeatStatistic,    "region_heat_statistic")
    WriteMysql.writeTable(dayLabelRatio,          "day_label_ratio")
    WriteMysql.writeTable(confidenceLevelCount,   "confidence_level_count")

    // 写完后释放中间 persist
    dayLabelCount.unpersist()
  }

  // 用户画像模块
  def computeUserPortrait(preData: DataFrame, nowStr: String): Unit = {
    println("正在计算用户画像")

    val userWindow = Window.partitionBy("user_id")

    // 时段基础统计  后续被三个衍生 DF 复用，持久化
    val userPeriodStats = preData
      .groupBy("user_id", "time_period")
      .agg(
        count("*").as("period_post_cnt"),
        avg("score").as("period_avg_score")
      )
      .persist(StorageLevel.MEMORY_AND_DISK)

    // 一次开窗 算出三个时段偏好
    val userPeriodTop3 = userPeriodStats
      .withColumn("rank_post", row_number().over(userWindow.orderBy(desc("period_post_cnt"))))
      .withColumn("rank_emo",  row_number().over(userWindow.orderBy(asc("period_avg_score"))))
      .withColumn("rank_high", row_number().over(userWindow.orderBy(desc("period_avg_score"))))
      .filter("rank_post = 1 or rank_emo = 1 or rank_high = 1")
      .groupBy("user_id")
      .agg(
        max(when(col("rank_post") === 1, col("time_period"))).as("prefer_time_period"),
        max(when(col("rank_emo")  === 1, col("time_period"))).as("emo_time_period"),
        max(when(col("rank_high")=== 1, col("time_period"))).as("high_time_period")
      )


    val preferHourDF = preData
      .groupBy("user_id", "hour").count()
      .withColumn("rank", row_number().over(userWindow.orderBy(desc("count"))))
      .filter(col("rank") === 1)
      .select(col("user_id"), col("hour").as("prefer_hour"))

    val userGlobalTimeStats = preData
      .groupBy("user_id")
      .agg(
        round(stddev_samp("score"), 4).as("hourly_sentiment_stddev"),
        first("screen_name", ignoreNulls = true).as("screen_name"),
        round(
          sum(when(col("hour") >= 23 || col("hour") < 4, 1).otherwise(0)) / count("*"), 4
        ).as("night_post_ratio")
      )

    // 只 join 一次
    val userTimeProfileDF = userGlobalTimeStats
      .join(userPeriodTop3,  Seq("user_id"), "left")
      .join(preferHourDF,   Seq("user_id"), "left")
      .withColumn("create_time", lit(nowStr))
      .na.fill(0.0, Seq("hourly_sentiment_stddev"))

    // 地域炸开 被两个衍生 DF 复用，持久化
    val explodedUserRegionDF = preData
      .filter(col("location").isNotNull && col("location") =!= "")
      .select(
        col("user_id"), col("score"),
        explode(split(col("location"), " \\| ")).as("region")
      )
      .filter(col("region").isNotNull && col("region") =!= "")
      .persist(StorageLevel.MEMORY_AND_DISK)

    val userRegionGroupStats = explodedUserRegionDF
      .groupBy("user_id", "region")
      .agg(count("*").as("region_focus_count"), round(avg("score"), 4).as("top_region_avg_score"))

    val topRegionDF = userRegionGroupStats
      .withColumn("rank", row_number().over(Window.partitionBy("user_id").orderBy(desc("region_focus_count"))))
      .filter(col("rank") === 1)
      .select(
        col("user_id"), col("region").as("top_focus_region"),
        col("region_focus_count"), col("top_region_avg_score")
      )

    val regionDiversityDF = explodedUserRegionDF
      .groupBy("user_id")
      .agg(countDistinct("region").as("region_diversity_score"))

    val userRegionProfileDF = topRegionDF
      .join(regionDiversityDF, Seq("user_id"), "left")
      .withColumn("create_time", lit(nowStr))

    val userAuthProfileDF = preData
      .groupBy("user_id")
      .agg(
        first("user_authentication", ignoreNulls = true).as("user_authentication"),
        round(
          sum(when(col("label") === "正面", col("reposts_count") + col("comments_count") + col("attitudes_count")).otherwise(0)) /
            sum(when(col("label") === "正面", 1).otherwise(lit(1))), 2
        ).as("pos_interaction_avg"),
        round(
          sum(when(col("label") === "负面", col("reposts_count") + col("comments_count") + col("attitudes_count")).otherwise(0)) /
            sum(when(col("label") === "负面", 1).otherwise(lit(1))), 2
        ).as("neg_interaction_avg"),
        round(avg("confidence"), 4).as("avg_model_confidence")
      )
      .withColumn("sentiment_leverage",
        round(col("neg_interaction_avg") / when(col("pos_interaction_avg") === 0, 1).otherwise(col("pos_interaction_avg")), 2)
      )
      .withColumn("is_official",
        when(col("user_authentication").isNotNull && col("user_authentication") === "官方媒体(蓝V)", 1).otherwise(0)
      )
      .withColumn("create_time", lit(nowStr))

    WriteMysql.writeTable(userAuthProfileDF,   "user_auth_sentiment_profile")
    WriteMysql.writeTable(userRegionProfileDF, "user_region_sentiment_profile")
    WriteMysql.writeTable(userTimeProfileDF,   "user_time_sentiment_profile")

    // 写完后释放中间 persist
    userPeriodStats.unpersist()
    explodedUserRegionDF.unpersist()

    println("用户画像写入完成")
  }
}