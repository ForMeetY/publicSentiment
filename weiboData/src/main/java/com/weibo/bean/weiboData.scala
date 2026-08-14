package com.weibo.bean

/**
 * 微博实体类（对应你爬虫发送到 Kafka 的 JSON 结构）
 */
case class WeiboInfo(
                      // 微博核心信息
                      id: String,
                      bid: String,
                      user_id: String,
                      screen_name: String,      // 用户名
                      text: String,            // 正文
                      article_url: String, // 去除

                      // 位置、话题、@
                      location: String, // 去除
                      at_users: String, // 去除
                      topics: String,

                      // 图片、视频  去除
                      pics: String, // 去除
                      video_url: String,// 去除

                      // 互动数据
                      reposts_count: Int,      // 转发
                      comments_count: Int,     // 评论
                      attitudes_count: Int,    // 点赞

                      // 发布信息
                      created_at: String,      // 发布时间
                      source: String,          // 发布工具  去除
                      retweet_id: String,  // 去除

                      // 扩展信息
                      ip: String, // 去除
                      user_authentication: String,
                      vip_type: String,
                      vip_level: Int
                    )
