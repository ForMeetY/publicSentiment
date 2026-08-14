import pandas as pd
import json
import time
import os
from openai import OpenAI
from concurrent.futures import ThreadPoolExecutor, as_completed
from tqdm import tqdm

# 配置
API_KEY = os.environ.get("DEEPSEEK_API_KEY", "")  # DeepSeek API
BASE_URL = "https://api.deepseek.com"
INPUT_FILE = './data_raw.csv'  # 替换为你实际的 CSV 路径
OUTPUT_FILE = './labeled_weibo_data.csv'
MAX_WORKERS = 5  # 并发线程数

client = OpenAI(api_key=API_KEY, base_url=BASE_URL)

SYSTEM_PROMPT = """
你是一位专业的政务舆情分析专家。请阅读给定的微博内容，进行分类并评估情绪。
类别：[1]政务发布, [2]城市问题, [3]民生服务, [4]公众反馈/举报。
输出要求：严格返回纯 JSON 格式，不包含任何 Markdown 代码块标签，不要任何解释。
情绪得分：-1.0(负面) 到 1.0(正面)。
JSON模板：
{"topic_id": 1, "topic_label": "政务发布", "topic_weights": {"1": 1.0, "2": 0.0, "3": 0.0, "4": 0.0}, "sentiment_score": 0.0}
"""


def tag_content(row):
    """单条调用逻辑"""
    bid, text = row['bid'], row['text']
    for _ in range(3):  # 最多重试3次
        try:
            response = client.chat.completions.create(
                model="deepseek-chat",
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": f"微博内容: {text}"}
                ],
                response_format={"type": "json_object"}
            )
            content = response.choices[0].message.content
            data = json.loads(content)
            data['bid'] = bid
            return data
        except Exception as e:
            time.sleep(1)  # 等待后再试
            continue
    return {"bid": bid, "error": "failed"}


# 2. 主程序
if __name__ == "__main__":
    path = './train_dataset_csv/part-00000-6d4a1db3-6fec-41a9-9e63-f946299833be-c000.csv'

    df = pd.read_csv(path, on_bad_lines='skip', engine='python')
    #
    processed_bids = set()
    if os.path.exists(OUTPUT_FILE):
        processed_df = pd.read_csv(OUTPUT_FILE)
        processed_bids = set(processed_df['bid'])

    tasks = [row for _, row in df.iterrows() if row['bid'] not in processed_bids]

    print(f"待处理任务数: {len(tasks)}")
    results = []
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = {executor.submit(tag_content, task): task for task in tasks}

        for future in tqdm(as_completed(futures), total=len(tasks)):
            results.append(future.result())
            # 每处理 50 条数据存一次盘，防止数据丢失
            if len(results) >= 50:
                pd.DataFrame(results).to_csv(OUTPUT_FILE, mode='a', header=not os.path.exists(OUTPUT_FILE), index=False)
                results = []

    print("全部任务处理完毕！")