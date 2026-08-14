from kafka import KafkaProducer
import json

# 写上全部集群节点，容错更高
brokers = "master1:9092,worker1:9092,worker2:9092"
topic = "weibo_topic"

# 创建生产者
producer = KafkaProducer(
    bootstrap_servers=brokers.split(','),
    # 自动把字典转 json 字符串
    value_serializer=lambda x: json.dumps(x, ensure_ascii=False).encode('utf-8')
)

# 模拟一条微博数据
msg = {
    "id": "30",
    "text": "力于恢复民众正常生活秩序。#抵御台风摩羯海南消防在行动#L海南消防的微博视频台风肆虐后的温情守护，海南消防全力推进灾",
    "time": "2026-05-06",
    "reposts_count":"100",
    "comments_count": "100",
    "attitudes_count":"100",
    "user_authentication":"NULL"
}

# 发往主题
producer.send(topic, value=msg)
producer.flush()
print("发送成功！")