package com.weibo.utils.mysqlUtils

import org.apache.spark.sql.{DataFrame, SparkSession}
import com.weibo.utils.Constant
/**
 * @author Xbx
 * @date 2026/5/8 17:07
 */

// 读取固定的表
object ReadMysql {
  private val url = Constant.url
  private val user = Constant.user
  private val password = Constant.pwd
  def readTable(spark:SparkSession, tableName: String): DataFrame = {
      spark.read
      .format("jdbc")
      .option("url", url)
      .option("dbtable", tableName)
      .option("user", user)
      .option("password", password)
      .load()
  }



}
