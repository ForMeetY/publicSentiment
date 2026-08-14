import math
import os
import numpy as np
import torch
import evaluate
from datasets import load_dataset
from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer,
    DataCollatorWithPadding
)
from sklearn.metrics import classification_report

#  模型
model_name = "hfl/chinese-roberta-wwm-ext"
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 分词器
tokenizer = AutoTokenizer.from_pretrained(model_name)

def is_valid_label(x):
    v = x["topic_id"]
    if v is None:
        return False
    try:
        v = float(v)
        return not math.isnan(v) and 1 <= int(v) <= 4
    except:
        return False

def process_label(example):  # 标签是1 - 4 转换
    return {"labels": int(float(example["topic_id"])) - 1}

def tokenize(example):
    return tokenizer(example["text"], truncation=True, max_length=128)

#训练 是一个四分类
model = AutoModelForSequenceClassification.from_pretrained(
    model_name, num_labels=4
).to(device)
path = "../label_roberta/data/train.csv"
train_dataset = load_dataset("csv", data_files={"train": path})
train_dataset = train_dataset.filter(is_valid_label)
train_dataset = train_dataset.map(process_label)
train_dataset = train_dataset.map(tokenize)

metric = evaluate.load("accuracy")
def compute_metrics(ep):
    logits, labels = ep
    preds = np.argmax(logits, axis=-1)
    return metric.compute(predictions=preds, references=labels)

collator = DataCollatorWithPadding(tokenizer=tokenizer)

train_args = TrainingArguments(
    output_dir="data/results",
    per_device_train_batch_size=8,
    learning_rate=2e-5,
    num_train_epochs=3,
    logging_steps=10,
    fp16=torch.cuda.is_available()
)

trainer = Trainer(
    model=model,
    args=train_args,
    train_dataset=train_dataset["train"],
    data_collator=collator,
    compute_metrics=compute_metrics
)

# trainer.train()
# model.save_pretrained("./final_weibo_model")
# tokenizer.save_pretrained("./final_weibo_model")
# print("训练完成，模型已保存！")

#：释放显存
del model
del trainer
torch.cuda.empty_cache()
print("显存已释放，开始测试...")


#  测试
model = AutoModelForSequenceClassification.from_pretrained(
    "data/final_weibo_model"
).to(device)
model.eval()
path = '../label_roberta/data/test.csv'
test_dataset = load_dataset("csv", data_files={"test": path})
test_dataset = test_dataset.filter(is_valid_label)
test_dataset = test_dataset.map(process_label)
test_dataset = test_dataset.map(tokenize)

test_data = test_dataset["test"]
test_data = test_data.remove_columns(
    [c for c in test_data.column_names if c not in ["input_ids", "attention_mask", "labels"]]
)

test_args = TrainingArguments(
    output_dir="./tmp_predict",
    per_device_eval_batch_size=16,
    fp16=torch.cuda.is_available()
)

tester = Trainer(
    model=model,
    args=test_args,
    data_collator=DataCollatorWithPadding(tokenizer=tokenizer)
)

out = tester.predict(test_data)

preds = np.argmax(out.predictions, axis=-1)
true = out.label_ids

print("测试结果：")
print(classification_report(
    true, preds,
    target_names=["类别1", "类别2", "类别3", "类别4"]
))

# # 查看前5个预测结果
# print("\n" + "=" * 40)
# print("前5个样本预测详情：")
#
# label_names = ["类别1", "类别2", "类别3", "类别4"]
#
# # 读取原始文本
# import pandas as pd
# test_df = pd.read_csv("data/test.csv")
#
# for i in range(5):
#     text = test_df["text"].iloc[i]
#     true_label = label_names[true[i]]
#     pred_label = label_names[preds[i]]
#     correct = "预测错误" if true[i] == preds[i] else "预测正确"
#
#     print(f"\n样本 {i+1}{correct}")
#     print(f"  文本：{text[:50]}{'...' if len(text) > 50 else ''}")
#     print(f"  真实标签：{true_label}")
#     print(f"  预测标签：{pred_label}")