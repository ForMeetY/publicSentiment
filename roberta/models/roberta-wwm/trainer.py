from datasets import load_dataset

from transformers import (
    BertTokenizer,
    BertForSequenceClassification,
    TrainingArguments,
    Trainer,
    DataCollatorWithPadding
)
import numpy as np
import evaluate
import torch

# GPU
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print("device:", device)

# 模型
model_name = "hfl/chinese-roberta-wwm-ext"

# 分词器
tokenizer = BertTokenizer.from_pretrained(model_name)

# 初始化模型 做三分类
model = BertForSequenceClassification.from_pretrained(
    model_name,
    num_labels=3  # 三分类
).to(device)

#  数据集
dataset = load_dataset("deepeye/weibo-emotion-classification")

print(dataset)
print("columns:", dataset["train"].column_names)


# label 处理
def convert_label(example):
    label_map = {
        "负面": 0,
        "中性": 1,
        "正面": 2
    }
    return {"labels": label_map[example["label"]]}


dataset = dataset.map(convert_label)
# 切分
# 91
dataset = dataset["train"].train_test_split(test_size=0.1)
print(dataset)


# tokenizer
def preprocess(example):
    return tokenizer(
        example["text"],  # 这个数据集是 text
        truncation=True,  # 截断
        max_length=128
    )


dataset = dataset.map(preprocess)

# 删除无用列
dataset = dataset.remove_columns(
    [col for col in dataset["train"].column_names if col not in ["input_ids", "attention_mask", "labels"]]
)

dataset.set_format(
    type="torch",
    columns=["input_ids", "attention_mask", "labels"]
)

# 6. metrics F1
f1 = evaluate.load("f1")


# 计算评估指标
# 使用 acc 和 f1
def compute_metrics(eval_pred):
    logits, labels = eval_pred
    preds = np.argmax(logits, axis=1)
    return {
        "accuracy": (preds == labels).mean(),
        "f1_macro": f1.compute(
            predictions=preds,
            references=labels,
            average="macro"
        )["f1"]
    }

# 动态padding 对齐微博正文的长度
data_collator = DataCollatorWithPadding(tokenizer=tokenizer)

# 训练参数
training_args = TrainingArguments(
    output_dir="./output",  # 模型保存路径

    learning_rate=2e-5,  # 指定学习率
    per_device_train_batch_size=16,  # 训练批次大小
    per_device_eval_batch_size=16,  # 评估批次大小

    num_train_epochs=3,  # 训练轮数
    weight_decay=0.01,  # 权重衰减 防止过拟合 L2

    evaluation_strategy="epoch",  # 评估策略
    save_strategy="epoch",  # 保存策略
    logging_steps=50,  # 日志步数 没50步保存一次

    fp16=torch.cuda.is_available(),  # 混合精度

    load_best_model_at_end=True,  # 保存最好的模型
    metric_for_best_model="f1_macro"  # 评估指标 f1
)

# Trainer
trainer = Trainer(
    model=model,
    args=training_args,  # 训练参数
    train_dataset=dataset["train"],  # 训练集
    eval_dataset=dataset["test"],  # 测试集
    tokenizer=tokenizer,  # 分词器
    data_collator=data_collator,  # 动态padding
    compute_metrics=compute_metrics
)

# train
trainer.train()

# #save
# save_path = "./models/weibo-sentiment-3class"
# trainer.save_model(save_path)
# tokenizer.save_pretrained(save_path)
# print("训练完成！模型已保存到:", save_path)
