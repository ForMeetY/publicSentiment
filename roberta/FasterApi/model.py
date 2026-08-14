import torch
from transformers import BertTokenizer, BertForSequenceClassification

MODEL_PATH = "../models/roberta-wwm/models/weibo-sentiment-3class"

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

tokenizer = BertTokenizer.from_pretrained(MODEL_PATH)
model = BertForSequenceClassification.from_pretrained(MODEL_PATH)

model.to(device)
model.eval()


LABEL_MAP = {
    0: "负面",
    1: "中性",
    2: "正面"
}