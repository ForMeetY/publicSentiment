import os
import torch
from transformers import BertTokenizer, BertForSequenceClassification

# 解决 Windows 显存碎片
os.environ["PYTORCH_CUDA_ALLOC_CONF"] = "expandable_segments:True"

model_path = "./models/roberta-sentiment"
tokenizer = BertTokenizer.from_pretrained(model_path)
model = BertForSequenceClassification.from_pretrained(model_path)

model = model.half().cuda()
model.eval()

# 你要的配置
CHUNK_SIZE = 300      # 后端一次处理 500 条
BATCH_SIZE = 10        # GPU 内部小批量
MAX_LENGTH = 64

#  预测函数
def predict_batch(texts):
    all_preds = []
    torch.cuda.empty_cache()

    # 1. 先把所有数据切成 500 条一组
    for i in range(0, len(texts), CHUNK_SIZE):
        chunk = texts[i : i + CHUNK_SIZE]

        # 2. 每组内部，再切成 10 条用 GPU 跑
        for j in range(0, len(chunk), BATCH_SIZE):
            batch = chunk[j : j + BATCH_SIZE]

            inputs = tokenizer(
                batch,
                padding=True,
                truncation=True,
                max_length=MAX_LENGTH,
                return_tensors="pt"
            ).to("cuda")

            with torch.no_grad():
                outputs = model(**inputs)

            preds = torch.argmax(outputs.logits, dim=1).cpu().tolist()
            all_preds.extend(preds)

        # 3. 每跑完 500 条，清空一次显存
        torch.cuda.empty_cache()

    return all_preds