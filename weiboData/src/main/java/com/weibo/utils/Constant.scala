package com.weibo.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * @author Xbx
 * @date 2026/5/28 22:52
 */
object Constant {
  final val url = "jdbc:mysql://master1:3306/weibo?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
  final val user = "root"
  final val pwd = "1234"
  // 当前时间  格式 "yyyy-MM-dd"
  private val today: LocalDate = LocalDate.now()
  val now: String = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  // 3. 今天 - n 天 = n天前
  final val time: String = today.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  // 控制用户画像的统计时间
  final val startHour = 22
  final val startMin = 0
  final val endMin = 59
  final val endHour = 23

}
