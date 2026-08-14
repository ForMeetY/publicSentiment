package com.weibo.utils
import com.weibo.myconfig.Config
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

/**
 * @author Xbx
 * @date 2026/5/6 21:11
 */
/**
 * Kafka 工具类
 * 作用：获取 Kafka 数据流
 */
object KafkaUtils {

  /**
   * 获取 Kafka 实时数据流
   */
  def getKafkaStream(spark: SparkSession, topic: String): DataFrame = {
    spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", Config.KAFKA_BOOTSTRAP)
      .option("subscribe", topic)
//      .option("startingOffsets", "latest")
      .option("startingOffsets","earliest") //
      .option("failOnDataLoss", "false")

      .load()
      .selectExpr("CAST(value AS STRING)")
      .toDF("json_data")
  }

  /**
   * 把 JSON 字符串解析成结构化的 DataFrame
   */
  def parseWeiboJson(df: DataFrame): DataFrame = {
    df.select(
      get_json_object(col("json_data"), "$.id").alias("id"),
      get_json_object(col("json_data"), "$.bid").alias("bid"),
      get_json_object(col("json_data"), "$.user_id").alias("user_id"),
      get_json_object(col("json_data"), "$.screen_name").alias("screen_name"),
      get_json_object(col("json_data"), "$.text").alias("text"),
      get_json_object(col("json_data"), "$.article_url").alias("article_url"),
      get_json_object(col("json_data"), "$.location").alias("location"),
      get_json_object(col("json_data"), "$.at_users").alias("at_users"),
      get_json_object(col("json_data"), "$.topics").alias("topics"),
      get_json_object(col("json_data"), "$.pics").alias("pics"),
      get_json_object(col("json_data"), "$.video_url").alias("video_url"),
      get_json_object(col("json_data"), "$.reposts_count").cast("int").alias("reposts_count"),
      get_json_object(col("json_data"), "$.comments_count").cast("int").alias("comments_count"),
      get_json_object(col("json_data"), "$.attitudes_count").cast("int").alias("attitudes_count"),
      get_json_object(col("json_data"), "$.created_at").alias("created_at"),
      get_json_object(col("json_data"), "$.source").alias("source"),
      get_json_object(col("json_data"), "$.retweet_id").alias("retweet_id"),
      get_json_object(col("json_data"), "$.ip").alias("ip"),
      get_json_object(col("json_data"), "$.user_authentication").alias("user_authentication"),
      get_json_object(col("json_data"), "$.vip_type").alias("vip_type"),
      get_json_object(col("json_data"), "$.vip_level").cast("int").alias("vip_level")
    )
  }

}
