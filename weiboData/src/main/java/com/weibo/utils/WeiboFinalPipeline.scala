package com.weibo.utils

import com.huaban.analysis.jieba.JiebaSegmenter
import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{HashingTF, IDF, StandardScaler, VectorAssembler}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.{Pipeline, PipelineModel}

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

object WeiboFinalPipeline {

  // 训练模型
  def trainModel(spark: SparkSession) = {

    // 读取CSV 数据 作为数据集
    var df = spark.read.format("csv")
      .option("header", "true")
      .option("encoding","GBK")
      .load("file:///E:/Java/weiboData/src/main/java/com/weibo/utils/data/data.csv")
    df = df.filter("label is not null AND label != ''")
    // label转数字
    val dfFixed = df.withColumn("label", col("label").cast("double"))
    // 划分训练集和测试集
    val Array(trainDF, testDF) = dfFixed.randomSplit(Array(0.8, 0.2), seed = 42)

    // 特征工程
    val ruleDF = RuleFeatureBuilder.build(trainDF)

    // 结巴分词
    val wordsDF = jieba(ruleDF)

    // TF-IDF 文本特征
    val hashingTF = new HashingTF()
      .setInputCol("words")  // 输入：分词后的词语
      .setOutputCol("rawFeatures") // 输出：TF-IDF
      .setNumFeatures(1500)   // 词表大小

    //
    val idf = new IDF()
      .setInputCol("rawFeatures") // 输入分好的词
      // TF 是词频，IDF 是逆文档频率，TF-IDF 是 TF * IDF
      .setOutputCol("tfidf_features")// 真正的TF-IDF特征


    // 特征融合
    // 列出特征
    val ruleFeatures = Array(
      "text_length",
      "spam_keyword_count",
      "promotion_count", "is_viral", "engagement_score",
      "spam_density", "promotion_density",
      "engagement", "engagement_ratio"
    )

    // 最终逻辑回归的输入 将特征和词向量进行特征融合
    val assembler = new VectorAssembler()
      .setInputCols(ruleFeatures :+ "tfidf_features") // 输入：特征和词向量
      .setOutputCol("final_features")// 压缩成一个向量
      .setHandleInvalid("skip") // 忽略无效字段

    // 特征和TF-IDF的单位不同 标准化
    val scaler = new StandardScaler()
      .setInputCol("final_features")       // 输入：融合后的大向量
      .setOutputCol("scaled_features")      // 输出：标准化后的特征
      .setWithMean(true)                    // 中心化（减均值）
      .setWithStd(true)                     // 归一化（除以方差）

    // 定义逻辑回归模型
    val lr = new LogisticRegression() // 逻辑回归
      .setLabelCol("label") // 预测标签
      .setFeaturesCol("scaled_features") // 输入特征
      .setPredictionCol("prediction") // 预测结果

    // 构建管道 将上面的步骤组成管道
    val pipeline = new Pipeline()
      .setStages(Array(hashingTF, idf, assembler,scaler ,lr))

    val model = pipeline.fit(wordsDF)
    //测试
    simpleTestAccuracy(spark,model, testDF)
    model
  }

  // 分词
  def jieba(df: DataFrame): DataFrame = {
    val jiebaUDF = udf((text: String) => {
      val segmenter = new JiebaSegmenter()
      if (text == null || text.trim.isEmpty) Seq.empty[String]
      else segmenter.sentenceProcess(text)
        .filter(_ != null)
        .map(_.toString)
        .filter(_.length > 1)
        .toSeq
    })
    // 分词并添加到数据集
    df.withColumn("words", jiebaUDF(col("text")))
  }





  // 特征工程
  object RuleFeatureBuilder {
    def build(df: DataFrame): DataFrame = {
      df
        // 正文长度
        .withColumn("text_length", length(col("text")))
        // 垃圾关键词数量
        .withColumn("spam_keyword_count", size(split(col("text"), "加V|抽奖|秒杀|代理|私信|领取|福利|红包|点击|下单|推广|包邮|免费|送|宝子|亲亲|抽|双11")))
        // 推广数量
        .withColumn("engagement", col("reposts_count") + col("comments_count") + col("attitudes_count"))
        // 推广占比
        .withColumn("engagement_ratio", col("engagement") / (length(col("text")) + 1))
        // 推广关键词数量
        .withColumn("promotion_count", (length(col("text")) - length(regexp_replace(col("text"), "关注|转发|评论|点赞|进店|抢购|优惠|又好又便宜", ""))) / 8)
        // 是否 互动度是不是高
        .withColumn("is_viral", when(col("reposts_count") > 1000, 1).otherwise(0))
        // 互动度得分三者相加
        .withColumn("engagement_score", col("reposts_count") + col("comments_count") + col("attitudes_count"))
        // 垃圾关键词的密度
        .withColumn("spam_density", col("spam_keyword_count") / (length(col("text")) + 1))
        // 推广关键词的密度
        .withColumn("promotion_density", col("promotion_count") / (length(col("text")) + 1))
    }
  }

  // 简单测试正确率
  def simpleTestAccuracy(spark: SparkSession, model: PipelineModel,df:DataFrame): Unit = {

    // 测试集只需要做：补齐字段 + 类型转换
    val dfFixed = df.withColumn("label", col("label").cast("double"))
    // 构造特征
    val ruleDF = buildFeatures(dfFixed)
    // 直接用训练好的模型预测
    val result = model.transform(ruleDF)

    // 统计准确率
    val total = result.count()
    val correct = result.filter(col("label") === col("prediction")).count()
    val accuracy = correct.toDouble / total

    println("模型测试结果 ")
    println(s"总样本数：$total")
    println(s"预测正确：$correct")
    println(f"正确率：${accuracy * 100}%.2f %%")

    // 误判样本
    println("\n真实有效但被误判为垃圾的样本 ")
    result
      .filter(col("label") === 1.0 && col("prediction") === 0.0)
      .select("text", "label", "prediction")
      .show(false)
    println("\n真实垃圾但被误判为有效样本 ")
    result
      .filter(col("label") === 0.0 && col("prediction") === 1.0)
      .select("text", "label", "prediction")
      .show(false)

  }

  // 构造特征
  def buildFeatures(df: DataFrame): DataFrame = {
    val featureDF = RuleFeatureBuilder.build(df)
    // 分词
    val jiebaUDF = udf { text: String =>
      val seg = new com.huaban.analysis.jieba.JiebaSegmenter()
      if (text == null || text.trim.isEmpty) Seq.empty[String]
      else seg.sentenceProcess(text).toArray.filter(_ != null).map(_.toString).filter(_.length > 1).toSeq
    }
    val wordsDF = featureDF.withColumn("words", jiebaUDF(col("text")))
    wordsDF
  }


}