package com.weibo.etl

import java.time.LocalTime

import org.apache.spark.sql.{DataFrame, SparkSession}
import com.weibo.utils.{Constant, KafkaUtils, WeiboFinalPipeline}
import com.weibo.myconfig.Config
import org.apache.spark.sql.functions.{col, length, regexp_replace, when}
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.types.IntegerType
import java.util.{Timer, TimerTask}
import java.time.LocalDate

/**
 * @author Xbx
 * @date 2026/5/6 21:10
 */
object WeiBoKafkaConsumer {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("WeiBoKafkaConsumer")
      .master("local[16]")
      .enableHiveSupport()
      .config("hive.metastore.uris", "thrift://master1:9083")
      .config("spark.local.dir", "F:/spark-tmp")   //
      .config("spark.sql.shuffle.partitions", "10") // 减少shuffle文件数
      .getOrCreate()

    val model = WeiboFinalPipeline.trainModel(spark)
    import spark.implicits._

    var data = KafkaUtils.getKafkaStream(spark, Config.KAFKA_TOPIC)

    data = KafkaUtils.parseWeiboJson(data)
    data = dropColumns(data)
    data = cleanRemark(data)
    data = cleanText(data)
    data = cleanUserAuth(data)
    data = cleanInvalidText(spark, data, model)

    // 写入 Hive（流式，持续运行）
    Classify.classByDayAndHour(spark, data)



    val timer1 = new Timer(true)
    val timer2 = new Timer(true)

    //  1常规统计 + 夜间画像，每 30 min 执行
    // delay=6s：等 Spark Streaming 启动完成后再跑，避免抢资源
    var lastRefreshDate: LocalDate = LocalDate.MIN
    timer1.schedule(new TimerTask() {
      override def run(): Unit = {
        try {
          val nowTime = LocalTime.now()
          val today   = LocalDate.now()
          println(s"[统计任务] ${nowTime.toString.take(8)} 开始执行...")

          Classify.incrementalTopicClassify(spark)
          Classify.incrementalSentimentAnalysis(spark)
          Classify.classByIndicator(spark)

          // 23:00 ~ 23:59 做一次全量画像重刷
          val startBoundary = LocalTime.of(Constant.startHour, Constant.startMin)
          val endBoundary   = LocalTime.of(Constant.endHour, Constant.endMin)
          if (nowTime.isAfter(startBoundary) && nowTime.isBefore(endBoundary)) {
            if (lastRefreshDate != today) {
              println(s"[画像] 启动夜间全量重刷...")
              Classify.fullUserPortrait(spark)
              lastRefreshDate = today
              println(s"[画像] 今天 ($today) 全量画像完成")
            } else {
              println("[画像] 今晚已重刷过，跳过")
            }
          }

          println(s"[统计任务] ${nowTime.toString.take(8)} 执行完毕")
        } catch {
          case e: Exception =>
            println("[统计任务] 本次出错，跳过：" + e.getMessage)
            e.printStackTrace()
            // 无论画像跑没跑，定时器执行完才释放
        }
      }
    }, 1 * 100L, 10 * 60 * 1000L)  // delay=1min，每10min一次

    // 推荐计算，每 30 min 执行
    // delay=3min：错开统计任务，避免两个任务同时启动争抢 Spark 资源
    timer2.schedule(new TimerTask() {
      override def run(): Unit = {
        try {
          println("推荐任务开始执行...")
          Recommend.generateRecommendations(spark)
          println("推荐任务执行完毕")
        } catch {
          case e: Exception =>
            println("推荐任务出错：" + e.getMessage)
            e.printStackTrace()
        }
      }
    }, 1 * 60 * 1000L, 30 * 60 * 1000L)  // delay=3min，每30min一次

    sys.addShutdownHook {
      println("正在关闭定时器...")
      timer1.cancel()
      timer2.cancel()
      spark.stop()
    }
    spark.streams.awaitAnyTermination()
  }

  def dropColumns(data: DataFrame): DataFrame = {
    data.drop(
      "article_url", "at_users", "pics", "video_url",
      "source", "retweet_id", "ip", "vip_type", "vip_level"
    )
  }

  def cleanUserAuth(data: DataFrame): DataFrame = {
    data.withColumn(
      "user_authentication",
      when(
        col("user_authentication").isNull || col("user_authentication") === "",
        "普通用户"
      ).otherwise(col("user_authentication"))
    )
  }

  def cleanInvalidText(spark: SparkSession, cleaned: DataFrame, model: PipelineModel): DataFrame = {
    val featureDF = WeiboFinalPipeline.RuleFeatureBuilder.build(cleaned)
    val df        = WeiboFinalPipeline.jieba(featureDF)
    val result    = model.transform(df)
    result
      .filter(col("prediction") === 1.0)
      .select(cleaned.columns.map(col): _*)
  }

  def cleanText(data: DataFrame): DataFrame = {
    data
      .withColumn("text", regexp_replace(col("text"), "@\\S+", ""))
      .filter(!col("text").contains("常识打卡"))
      .filter(!col("text").contains("天猫"))
      .filter(!col("text").contains("京东"))
      .filter(!col("text").contains("淘宝"))
      .filter(!col("text").contains("拼多多"))
      .filter(!col("text").contains("肖战"))
      .filter(!col("text").contains("杨紫"))
      .filter(!col("text").contains("常识翻身打卡计划"))
      .filter(!col("text").contains("翻身打卡"))
      .filter(col("text").isNotNull)
      .filter(length(col("text")) > 5)
  }

  // 过滤掉三项互动全为 0 的垃圾帖
  def cleanRemark(data: DataFrame): DataFrame = {
    data
      .withColumn("reposts_count",  col("reposts_count").cast(IntegerType))
      .withColumn("comments_count", col("comments_count").cast(IntegerType))
      .withColumn("attitudes_count", col("attitudes_count").cast(IntegerType))
      .dropDuplicates("id")
  }
}