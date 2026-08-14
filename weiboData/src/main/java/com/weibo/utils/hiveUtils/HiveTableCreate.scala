package com.weibo.utils.hiveUtils

import org.apache.spark.sql.SparkSession

/**
 * @author Xbx
 * @date 2026/5/19 11:40
 */
object HiveTableCreate {

  def createPredTable(spark: SparkSession): Unit = {
    spark.sql(
      """
       CREATE TABLE IF NOT EXISTS weibo.predstatistic (
        |    id                  STRING,
        |    text                STRING,
        |    topics              STRING,
        |    reposts_count       INT,
        |    comments_count      INT,
        |    attitudes_count     INT,
        |    created_at          TIMESTAMP,
        |    location            STRING,
        |    user_authentication STRING,
        |    hour                INT,
        |    time_period         STRING,
        |    label               STRING,
        |    label_id            INT,
        |    score               DOUBLE,
        |    confidence          DOUBLE
        |)
        |PARTITIONED BY (dt STRING)
        |STORED AS PARQUET
        |TBLPROPERTIES ("parquet.compression" = "snappy");
        |""".stripMargin
    )
  }

}
