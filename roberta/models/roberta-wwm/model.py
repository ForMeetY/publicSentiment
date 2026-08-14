from transformers import BertTokenizer, BertForSequenceClassification
import torch
print("CUDA:", torch.cuda.is_available())
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model_name = "hfl/chinese-roberta-wwm-ext"
# tokenizer
tokenizer = BertTokenizer.from_pretrained(model_name)
# model
model = BertForSequenceClassification.from_pretrained(
    model_name,
    num_labels=3
)
model.to(device)
model.eval()   # 推理模式
print("模型加载完成")
texts = [
    "这个产品真的很好",
    "垃圾，再也不用了",
    "一般般吧"
]
inputs = tokenizer(
    texts,
    padding=True,
    truncation=True,
    max_length=128,
    return_tensors="pt"
)
inputs = {k: v.to(device) for k, v in inputs.items()}
with torch.no_grad():
    outputs = model(**inputs)
logits = outputs.logits
preds = torch.argmax(logits, dim=1)
print("logits:", logits)
print("预测:", preds)
# 保存模型
model.save_pretrained("./models/roberta")
tokenizer.save_pretrained("./models/roberta")