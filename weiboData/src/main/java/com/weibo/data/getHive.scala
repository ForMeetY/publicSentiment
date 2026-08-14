package com.weibo.data

import com.weibo.utils.hiveUtils.ReadHiveUtils
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col

/**
 * @author Xbx
 * @date 2026/6/3 21:18
 */
object getHive {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("LocalExport")
      .master("local[*]")
      .enableHiveSupport()
      .config("hive.metastore.uris", "thrift://192.168.88.120:9083")
      .config("spark.sql.warehouse.dir", "/user/hive/warehouse") // 加上这行，明确指定 HDFS 仓库地址
      .getOrCreate()
    import org.apache.spark.sql.types._

    // 1. 手动定义你只关心的那两个字段
    val schema = StructType(Array(
      StructField("bid", StringType, true),
      StructField("text", StringType, true)
    ))

    // 2. 直接读取 HDFS 物理地址
    // 注意：如果你不知道确切的 HDFS 路径，请在 Hive 里执行 `DESCRIBE FORMATTED weibo.classbydayandhour` 查看 "Location"
    val path = "hdfs://192.168.88.120:8020/user/hive/warehouse/weibo.db/classbydayandhour"

    val data = spark.read
      .schema(schema) // 强制只读取这两个字段，跳过其他列
      .parquet(path)
      .filter(col("text").isNotNull && col("text") =!= "").limit(10000) // 你的过滤逻辑

    // 3. 导出
    data.coalesce(1)
      .write
      .mode("overwrite")
      .option("header", "true")
      .csv("file:///E:/Python/roberta/models/roberta-wwm/label_roberta/data/train_dataset_csv")
  }
}