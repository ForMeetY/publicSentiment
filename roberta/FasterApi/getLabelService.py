# api.py
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import uvicorn

# 加载模型
MODEL_PATH = "../models/roberta-wwm/label_roberta/data/final_weibo_model"
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH)
model = AutoModelForSequenceClassification.from_pretrained(MODEL_PATH).to(device)
model.eval()


app = FastAPI(title="微博分类模型 API")

#请求/响应结构
class TextRequest(BaseModel):
    texts: List[str]          # 支持批量输入

class PredictResult(BaseModel):
    text: str
    label: str
    label_id: int
    confidence: float         # 置信度

class PredictResponse(BaseModel):
    results: List[PredictResult]

# 预测接口
LABEL_NAMES = ["政务发布", "城市问题", "民生服务", "公众反馈/举报"]
BATCH_SIZE = 16  # 每批最多16条，防止OOM

@app.post("/predict", response_model=PredictResponse)
def predict(request: TextRequest):
    all_results = []
    texts = request.texts

    # 分批处理
    for i in range(0, len(texts), BATCH_SIZE):
        batch_texts = texts[i: i + BATCH_SIZE]

        inputs = tokenizer(
            batch_texts,
            truncation=True,
            max_length=128,
            padding=True,
            return_tensors="pt"
        ).to(device)

        with torch.no_grad():
            logits = model(**inputs).logits
            probs = torch.softmax(logits, dim=-1)
            preds = torch.argmax(probs, dim=-1)

        for j, text in enumerate(batch_texts):
            label_id = preds[j].item()
            all_results.append(PredictResult(
                text=text,
                label=LABEL_NAMES[label_id],
                label_id=label_id + 1,        # 返回 1-4
                confidence=round(probs[j][label_id].item(), 4)
            ))

        # 每批结束释放显存
        del inputs, logits, probs, preds
        torch.cuda.empty_cache()
    return PredictResponse(results=all_results)

# 健康检查
@app.get("/health")
def health():
    return {"status": "ok", "device": str(device)}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001)