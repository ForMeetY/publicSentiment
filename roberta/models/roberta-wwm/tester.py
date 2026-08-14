import torch
import numpy as np
import evaluate
from datasets import load_dataset
from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    Trainer,
    DataCollatorWithPadding
)
from sklearn.metrics import classification_report, confusion_matrix

# 1. 配置路径
model_path = "./models/weibo-sentiment-3class"
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 2. 加载模型与分词器
tokenizer = AutoTokenizer.from_pretrained(model_path)
model = AutoModelForSequenceClassification.from_pretrained(model_path).to(device)

# 3. 准备测试数据 (逻辑必须与训练时完全一致)
dataset = load_dataset("deepeye/weibo-emotion-classification")

def convert_label(example):
    label_map = {"负面": 0, "中性": 1, "正面": 2}
    return {"labels": label_map[example["label"]]}

# 使用与训练时相同的切分逻辑 (这里假设测试集逻辑一致)
dataset = dataset.map(convert_label)
split_dataset = dataset["train"].train_test_split(test_size=0.1, seed=42)
test_dataset = split_dataset["test"]

def preprocess(example):
    return tokenizer(example["text"], truncation=True, max_length=128)

test_dataset = test_dataset.map(preprocess)
test_dataset = test_dataset.remove_columns([c for c in test_dataset.column_names if c not in ["input_ids", "attention_mask", "labels"]])
test_dataset.set_format("torch")

# 4. 指标定义
f1_metric = evaluate.load("f1")

def compute_metrics(eval_pred):
    logits, labels = eval_pred
    preds = np.argmax(logits, axis=1)
    return {
        "accuracy": (preds == labels).mean(),
        "f1_macro": f1_metric.compute(predictions=preds, references=labels, average="macro")["f1"]
    }

# 5. 执行评估
trainer = Trainer(
    model=model,
    eval_dataset=test_dataset,
    tokenizer=tokenizer,
    data_collator=DataCollatorWithPadding(tokenizer=tokenizer),
    compute_metrics=compute_metrics
)

print("正在进行模型评估...")
metrics = trainer.evaluate()
print("评估结果:", metrics)

# 6. 生成详细分析报告
predictions = trainer.predict(test_dataset)
preds = np.argmax(predictions.predictions, axis=1)
labels = predictions.label_ids
target_names = ['负面', '中性', '正面']

print("\n--- 详细分类报告 ---")
print(classification_report(labels, preds, target_names=target_names))

print("\n--- 混淆矩阵 ---")
print(confusion_matrix(labels, preds))