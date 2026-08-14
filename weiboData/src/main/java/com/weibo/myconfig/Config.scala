package com.weibo.myconfig

/**
 * @author Xbx
 * @date 2026/5/6 21:08
 */

/*
* 配置信息
* */

  object Config {
    // Kafka
    val KAFKA_BOOTSTRAP = "master1:9092"
    val KAFKA_TOPIC = "weibo_topic"
    val GROUP_ID = "weibo-group"

    // MySQL
    val MYSQL_URL = "jdbc:mysql://master1:3306/weibo?useSSL=false"
    val MYSQL_USER = "root"
    val MYSQL_PASSWORD = "123456"
}
