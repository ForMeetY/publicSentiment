package com.weibo.utils.hiveUtils

import java.time.LocalDate

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * @author Xbx
 * @date 2026/5/8 17:53
 */
/*
* 读取hive表
* */
object ReadHiveUtils {
  def readHiveclassByDayAndHour(spark: SparkSession,db:String,tableName:String): DataFrame = {
    println(s"正在读取表${db}.${tableName}")
    // 去重
    val df = spark.sql(s"select * from ${db}.${tableName}").dropDuplicates("dt", "id")
    df
  }
  // 只读当天的
  def readHiveTodayPartition(spark: SparkSession, db: String, tableName: String): DataFrame = {
    val today = LocalDate.now().toString // 格式：2026-05-07
    println(s"正在读取表${db}.${tableName} 的当天分区数据：dt=$today")

    // 只查当天分区  分区裁剪，不读全量
    val df = spark.sql(s"select * from ${db}.${tableName} where dt = '$today'")
      .dropDuplicates("dt", "id")

    df
  }

  // 只读取指定天的分区  时间格式：yyyy-MM-dd
  def readHiveBySpecifiedDay(
                              spark: SparkSession,
                              db: String,
                              tableName: String,
                              targetDay: String  // 外部传入日期
                            ): DataFrame = {
    println(s"正在读取表 ${db}.${tableName} 的指定分区数据：dt=$targetDay")

    val df = spark.sql(s"select * from ${db}.${tableName} where dt = '$targetDay'")
      .dropDuplicates("dt", "id")

    df
  }


}
