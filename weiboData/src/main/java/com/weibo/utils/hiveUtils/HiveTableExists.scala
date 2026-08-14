package com.weibo.utils.hiveUtils

import org.apache.spark.sql.SparkSession

/**
 * @author Xbx
 * @date 2026/5/8 17:59
 */
object HiveTableExists {
  // 判断一个hive表是否存在
  def tableExists(spark: SparkSession, dbName: String, tableName: String): Boolean = {
    try {
      // 尝试读表
      spark.table(s"$dbName.$tableName")
      true
    } catch {
      case _: Exception => false
    }
  }

}
