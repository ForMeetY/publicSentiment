package com.weibo.etl

import com.weibo.utils.Constant
import com.weibo.utils.mysqlUtils.{ReadMysql, WriteMysql}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._

/**
 * @author Xbx
 * @date 2026/6/4 14:55
 *
 * 最终得分 final_score = hot_score_norm * 0.6 + score * 0.4
 *
 * 热度得分 hot_score = log1p(reposts * 0.5 + comments * 0.2 + attitudes * 0.1)
 *   - 权重依据：转发 > 评论 > 点赞，反映用户主动传播意愿
 *   - log1p 对数平滑压缩长尾爆款，规避极少数千万级顶流微博
 *   - MinMax 归一化至 [0,1] 后再与情感分加权，统一量纲
 *
 * 情感得分 score：来自下游深度学习 Chinese RoBERTa 模型对微博正文产出的
 *   3 分类情感概率 [0,1]（正向情感概率越高得分越高）
 *
 * 个性化得分 personalized_score = final_score * (1 + personalized_bonus * 0.2)
 *   - 乘法混合：基础分低的内容即使精准匹配用户偏好也不会被顶上去
 *   - personalized_score ∈ [0.5, 1.2]
 *
 * 双缓冲写入：v1 <-> v2 热切换，保证前端视图零停机
 *   - CREATE OR REPLACE VIEW + UPDATE sys_config 在同一事务内原子提交
 */
object Recommend {

  def generateRecommendations(spark: SparkSession): Unit = {

    println("Recommend 读取用户画像...")
    // 读取用户地狱偏好
    val userTimeProfile = ReadMysql.readTable(spark, "user_time_sentiment_profile")
      .withColumn("uid_time", col("user_id").cast("string"))
      .select("uid_time", "prefer_time_period")
    // 时段偏好
    val userRegionProfile = ReadMysql.readTable(spark, "user_region_sentiment_profile")
      .withColumn("uid_region", col("user_id").cast("string"))
      .select("uid_region", "top_focus_region")


    // 构建候选微博池，计算原始热度分
    println("Recommend 读取并过滤候选微博池...")

    // 读取近 7 天的数据 + 筛选
    val candidateRaw = spark.table("weibo.topic_classify")
      .filter(col("dt") >= date_sub(current_date(), 7))
      .filter(col("topic_label").isNotNull)
      .filter(col("confidence") >= 0.7)
      .withColumn("author_id", col("user_id").cast("string"))
      .select(
        "id", "bid", "author_id", "screen_name", "text",
        "topic_label", "location", "time_period"
      )

    // 读取 情感预测结果
    val predstatistic = spark.table("weibo.predstatistic")
      .select("id", "score", "reposts_count", "comments_count", "attitudes_count")

    //  转发(reposts)   权重 0.5 —— 主动传播，信号最强
    //  评论(comments)  权重 0.2 —— 情感互动，次之
    //  点赞(attitudes) 权重 0.1 —— 低成本行为，点赞基数大故权重最低
    // 计算热度分，用转发、评论、点赞加权后取对数平滑 并与情感表合并
    val candidateWithRawScore = candidateRaw
      .join(predstatistic, Seq("id"), "left")
      .withColumn(
        "hot_score",
        log1p(
          coalesce(col("reposts_count"),  lit(0)) * 0.5 +
            coalesce(col("comments_count"), lit(0)) * 0.2 +
            coalesce(col("attitudes_count"),lit(0)) * 0.1
        )
      )
      .cache()

    // 获得全局的最大最小
    val statsRow = candidateWithRawScore
      .agg(min("hot_score").as("hs_min"), max("hot_score").as("hs_max"))
      .collect()(0)
    val hsMin  = statsRow.getAs[Double]("hs_min")
    val hsMax  = statsRow.getAs[Double]("hs_max")
    val hsRange = if (hsMax - hsMin < 1e-9) 1.0 else hsMax - hsMin  // 防止除零

    val candidateWithScore = candidateWithRawScore
      // 归一化
      .withColumn(
        "hot_score_norm",
        (col("hot_score") - lit(hsMin)) / lit(hsRange)
      )
      // 要和情感得分加权求和
      .withColumn(
        "final_score",
        col("hot_score_norm") * 0.6 + coalesce(col("score"), lit(0.0)) * 0.4
      )
      //过滤掉低分
      .filter(col("final_score") >= 0.5)
      .select(
        col("id"), col("bid"), col("author_id"), col("screen_name"), col("text"),
        col("topic_label"), col("location"), col("time_period"), col("final_score"),
        coalesce(col("score"), lit(0.0)).as("score")
      )
      .cache()
    // 拿到整体候选微博


    candidateWithRawScore.unpersist()
    //  多路召回
    println("Recommend执行多路召回...")

    val regionRecall = broadcast(userRegionProfile)
      .join(candidateWithScore, col("top_focus_region") === col("location"), "inner")
      .select(
        col("uid_region").as("target_user_id"),
        col("id"), col("bid"), col("author_id"), col("screen_name"),
        col("text"), col("topic_label"), col("location"), col("time_period"),
        col("score"), col("final_score"), lit("region").as("recall_reason")
      )

    val timeRecall = broadcast(userTimeProfile)
      .join(candidateWithScore, col("prefer_time_period") === col("time_period"), "inner")
      .select(
        col("uid_time").as("target_user_id"),
        col("id"), col("bid"), col("author_id"), col("screen_name"),
        col("text"), col("topic_label"), col("location"), col("time_period"),
        col("score"), col("final_score"), lit("time_period").as("recall_reason")
      )
    // 每个用户的候选内容列表
    //  合并去重 + GMM 个性化
    println("Recommend合并去重注入 GMM 软概率进行个性")

    // 融合召回的结果 + 把用户自己发的去掉
    val rawRecallDF = regionRecall
      .unionByName(timeRecall)
      .filter(col("target_user_id") =!= col("author_id"))  // 过滤自身内容
      .dropDuplicates("target_user_id", "id")              // 同用户同微博去重

    // 读取用户画像表
    val userGmmProfile = ReadMysql.readTable(spark, "user_gmm_profile")
      .select(
        col("user_id").as("u_id"),
        col("cluster_id"),
        col("prob_0").as("life_prob"),       // 生活号概率
        col("prob_1").as("night_cat_prob"),  // 夜猫子概率
        col("prob_2").as("big_v_weight"),    // 大V权重
        col("prob_3").as("vent_prob")        // 情绪宣泄概率
      )

    // personalized_score = final_score * (1 + personalized_bonus * 0.2)
    // 乘法混合保证：基础分低的内容即使精准匹配用户偏好也不会被顶上去
    // personalized_score ∈ [0.5, 1.2]
    // personalized_bonus 五个维度并行叠加
    //   life_prob   城市问题|民生服务 = 生活号 * 民生内容
    //   night_cat_prob  公众反馈/举报|城市问题  = 夜猫子 * 夜间治理热点
    //   big_v_weight  政务发布|公众反馈/举报  = 大V * 权威/舆情内容
    //   vent_prob   公众反馈/举报  = 情绪宣泄 * 投诉出口
    //   vent_prob   (1 - score)  = 负面内容情绪共鸣加成
    // 融合 召回表 和 用户画像表
    val personalizedRankDF = rawRecallDF
      .join(userGmmProfile, col("target_user_id") === col("u_id"), "left")
      .withColumn("life_prob",      coalesce(col("life_prob"),      lit(0.0)))
      .withColumn("night_cat_prob", coalesce(col("night_cat_prob"), lit(0.0)))
      .withColumn("big_v_weight",   coalesce(col("big_v_weight"),   lit(0.0)))
      .withColumn("vent_prob",      coalesce(col("vent_prob"),      lit(0.0)))
      .withColumn(
        // 计算个人加权
        "personalized_bonus",
        col("life_prob")      * when(col("topic_label").isin("城市问题", "民生服务"),        lit(1.0)).otherwise(lit(0.0)) +
          col("night_cat_prob") * when(col("topic_label").isin("公众反馈/举报", "城市问题"),   lit(1.0)).otherwise(lit(0.0)) +
          col("big_v_weight")   * when(col("topic_label").isin("政务发布", "公众反馈/举报"),   lit(1.0)).otherwise(lit(0.0)) +
          col("vent_prob")      * when(col("topic_label") === "公众反馈/举报",                lit(1.0)).otherwise(lit(0.0)) +
          col("vent_prob")      * (lit(1.0) - col("score"))
      )
      .withColumn(
        "personalized_score",
        col("final_score") * (lit(1.0) + col("personalized_bonus") * lit(1.2))
      )

    val windowSpec = Window.partitionBy("target_user_id")
      .orderBy(col("personalized_score").desc)

    // 拿到近8条微博
    val topNDF = personalizedRankDF
      .withColumn("rn", row_number().over(windowSpec))
      .filter(col("rn") <= 15)
      .select(
        col("target_user_id").as("user_id"),
        col("id").as("weibo_id"),
        col("bid"),
        col("screen_name"),
        col("text"),
        col("topic_label"),
        col("location"),
        col("personalized_score").as("score"),
        col("recall_reason"),
        current_timestamp().as("create_time")
      )
      .cache()

    val total = topNDF.count()
    println(s"[Recommend] 待写入推荐记录: $total 条")

    if (total == 0) {
      println("[Recommend] 无推荐结果，跳过写入")
      topNDF.unpersist()
      candidateWithScore.unpersist()
      return
    }


    //  写入 MySQL + 事务内切换视图
    println("Recommend 写入 MySQL...")
    val targetTable = getNextTable()
    WriteMysql.writeTable(topNDF.repartition(8), targetTable)
    switchView(targetTable)

    println(s"Recommend 写入完成，共 $total 条，目标表: $targetTable")

    topNDF.unpersist()
    candidateWithScore.unpersist()
  }

  /**
   * 读取当前活跃表，返回下一张写入表（v1 <-> v2 双缓冲）。
   */
  def getNextTable(): String = {
    val conn = java.sql.DriverManager.getConnection(Constant.url, Constant.user, Constant.pwd)
    try {
      val rs = conn.createStatement().executeQuery(
        "SELECT value FROM sys_config WHERE key_name = 'recommend_table'"
      )
      val current = if (rs.next()) rs.getString("value") else "user_recommendations_v1"
      if (current == "user_recommendations_v1") "user_recommendations_v2"
      else "user_recommendations_v1"
    } finally conn.close()
  }

  /**
   * 在同一事务内完成视图切换与配置更新，保证原子性：
   *   1. CREATE OR REPLACE VIEW（比 ALTER VIEW 更安全，视图不存在时也能执行）
   *   2. UPDATE sys_config 标记当前活跃表
   */
  def switchView(nextTable: String): Unit = {
    val conn = java.sql.DriverManager.getConnection(Constant.url, Constant.user, Constant.pwd)
    try {
      conn.setAutoCommit(false)
      val stmt = conn.createStatement()
      stmt.execute(s"CREATE OR REPLACE VIEW v_recommendations AS SELECT * FROM $nextTable")
      stmt.execute(s"UPDATE sys_config SET value = '$nextTable' WHERE key_name = 'recommend_table'")
      conn.commit()
      println(s"[Recommend] 视图已切换至: $nextTable")
    } catch {
      case e: Exception =>
        conn.rollback()
        throw new RuntimeException(s"[Recommend] 视图切换失败，已回滚: ${e.getMessage}", e)
    } finally {
      conn.setAutoCommit(true)
      conn.close()
    }
  }
}