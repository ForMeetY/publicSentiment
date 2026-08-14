import pandas as pd

# 1.读原始两张csv
label_df = pd.read_csv('./data/labeled_weibo_data.csv', on_bad_lines='skip')
text_df = pd.read_csv('./data/data_raw.csv', on_bad_lines='skip', engine='python')

# 去重
label_df = label_df.drop_duplicates(subset=['bid'])
text_df = text_df.drop_duplicates(subset=['bid'])

# inner合并
df = pd.merge(
    text_df[['bid', 'text']],
    label_df[['bid', 'topic_id', 'sentiment_score']],
    on='bid', how='inner'
)
df = df.dropna(subset=['text', 'topic_id'])
df = df[['text', 'topic_id']] # 训练只需要文本+标签

import pandas as pd

# 读取你合并好的数据
df = pd.read_csv("./data/train_4class.csv")

# 固定随机切分（和你训练时一致）
df_test = df.sample(frac=0.1, random_state=42)
df_train = df.drop(df_test.index)

# 保存
df_train.to_csv("./data/train.csv", index=False, encoding="utf-8-sig")
df_test.to_csv("./data/test.csv", index=False, encoding="utf-8-sig")

print("切分完成！")
print("训练集：", len(df_train))
print("测试集：", len(df_test))