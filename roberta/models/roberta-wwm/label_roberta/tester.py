import math
import numpy as np
from transformers import AutoModelForSequenceClassification, AutoTokenizer, Trainer
from datasets import load_dataset
from sklearn.metrics import classification_report
import os
os.environ["CUDA_LAUNCH_BLOCKING"] = "1"
os.environ["CUDA_VISIBLE_DEVICES"] = ""
def main():
    model = AutoModelForSequenceClassification.from_pretrained("data/final_weibo_model")
    tokenizer = AutoTokenizer.from_pretrained("data/final_weibo_model")

    path = 'data/test.csv'
    # 用字典指定 test
    dataset = load_dataset("csv", data_files=path)

    def is_valid_label(x):
        v = x["topic_id"]
        if v is None:
            return False
        try:
            v = float(v)
            if math.isnan(v):
                return False
            return 1 <= int(v) <= 4
        except (ValueError, TypeError):
            return False

    dataset = dataset.filter(is_valid_label)

    def process_label(example):
        return {"labels": int(example["topic_id"]) - 1}

    def tokenize(example):
        return tokenizer(example["text"], truncation=True, max_length=128)

    dataset = dataset.map(process_label)
    dataset = dataset.map(tokenize)

    # key 改为 "test"
    test_dataset = dataset["test"]
    test_dataset = test_dataset.remove_columns(
        [c for c in test_dataset.column_names if c not in ["input_ids", "attention_mask", "labels"]]
    )

    trainer = Trainer(model=model)
    out = trainer.predict(test_dataset)

    preds = np.argmax(out.predictions, axis=-1)
    true = out.label_ids

    print(classification_report(true, preds, target_names=["类别1","类别2","类别3","类别4"]))

if __name__ == "__main__":
    main()