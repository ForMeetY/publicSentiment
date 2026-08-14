package com.weibo.utils
import com.weibo.utils.Constant
/**
 * @author Xbx
 * @date 2026/5/28 22:49
 */

// 检测点 每次统计时做增量统计
object CheckpointUtils {

  private val url = Constant.url
  private val user = Constant.user
  private val pwd = Constant.pwd

  /** 读取上次处理到的 dt */
  def getLastDt(taskName: String): String = {
    val conn = java.sql.DriverManager.getConnection(url, user, pwd)
    try {
      val ps = conn.prepareStatement(
        "SELECT last_dt FROM etl_checkpoint WHERE task_name = ?"
      )
      ps.setString(1, taskName)
      val rs = ps.executeQuery()
      if (rs.next()) rs.getString("last_dt") else "1970-01-01"
    } finally conn.close()
  }

  /** 更新 dt（只在数据真正写完之后调用） 确保原子性 保证数据写入完全 */
  def updateLastDt(taskName: String, newDt: String): Unit = {
    val conn = java.sql.DriverManager.getConnection(url, user, pwd)
    try {
      val ps = conn.prepareStatement(
        """INSERT INTO etl_checkpoint (task_name, last_dt)
          |VALUES (?, ?)
          |ON DUPLICATE KEY UPDATE last_dt = VALUES(last_dt)
          |""".stripMargin
      )
      ps.setString(1, taskName)
      ps.setString(2, newDt)
      ps.executeUpdate()
    } finally conn.close()
  }
}
