package com.weibo.utils.mysqlUtils
import org.apache.spark.sql.{DataFrame, SaveMode}
import com.weibo.utils.Constant
/**
 * @author Xbx
 * @date 2026/5/9 14:17
 */
// 写入mysql

object WriteMysql {
  private val url      = Constant.url
  private val password = Constant.pwd
  private val user     = Constant.user

  // 基础写入，调用方指定 SaveMode
  private def write(data: DataFrame, tableName: String, mode: SaveMode): Unit = {
    data.write
      .format("jdbc")
      .option("url",      url)
      .option("user",     user)
      .option("password", password)
      .option("dbtable",  tableName)
      .mode(mode)
      .save()
    println(s"$tableName 写入成功 mode=$mode")
  }

  // 全量覆盖：清空表再写（全量重算 首次初始化用）
  def writeTable(data: DataFrame, tableName: String): Unit = {
    data.write
      .format("jdbc")
      .option("url",      url)
      .option("user",     user)
      .option("password", password)
      .option("dbtable",  tableName)
      .option("truncate", "true")   // truncate 而不是 drop+recreate，保留表结构
      .mode(SaveMode.Overwrite)
      .save()
    println(s"$tableName 全量覆盖写入成功")
  }

  // 追加写入：直接 append（历史数据补录，MySQL 表有 UNIQUE KEY 会报错，需配合 upsert）
  def appendTable(data: DataFrame, tableName: String): Unit = {
    write(data, tableName, SaveMode.Append)
  }

  // Upsert：有则更新无则插入（增量 / 补录推荐用这个，幂等安全）
  // Spark JDBC 本身不支持 upsert，通过先写临时表再执行 INSERT ... ON DUPLICATE KEY UPDATE 实现
  def upsertTable(data: DataFrame, tableName: String, uniqueKeys: Seq[String]): Unit = {
    val tmpTable = s"${tableName}_tmp_${System.currentTimeMillis()}"

    // 1. 写入临时表
    write(data, tmpTable, SaveMode.Overwrite)

    // 2. 拼接 upsert SQL
    val columns     = data.columns
    val updateCols  = columns.filterNot(uniqueKeys.contains) // 非主键列才 update
    val insertCols  = columns.mkString(", ")
    val selectCols  = columns.map(c => s"t.$c").mkString(", ")
    val updateParts = updateCols.map(c => s"$c = t.$c").mkString(", ")

    val sql =
      s"""
         |INSERT INTO $tableName ($insertCols)
         |SELECT $selectCols FROM $tmpTable t
         |ON DUPLICATE KEY UPDATE $updateParts
         |""".stripMargin

    // 3. 执行 upsert
    val conn = java.sql.DriverManager.getConnection(url, user, password)
    try {
      conn.createStatement().execute(sql)
      println(s"$tableName upsert 成功")
    } finally {
      // 4. 清理临时表
      conn.createStatement().execute(s"DROP TABLE IF EXISTS $tmpTable")
      conn.close()
    }
  }
}
