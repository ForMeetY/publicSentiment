from fastapi import FastAPI
from pydantic import BaseModel
from typing import List

from utils import predict_single, predict_batch

app = FastAPI(title="RoBERTa Sentiment Service")



# 请求结构

class TextRequest(BaseModel):
    text: str


class BatchRequest(BaseModel):
    texts: List[str]



# 单条预测

@app.post("/predict")
def predict(req: TextRequest):
    return predict_single(req.text)



# 批量预测（Spark主用）

@app.post("/predict_batch")
def predict_batch_api(req: BatchRequest):
    return {
        "results": predict_batch(req.texts),
        "count": len(req.texts),
        "timestamp": __import__("datetime").datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }



# 健康检查

@app.get("/health")
def health():
    return {"status": "ok"}


# 启动入口

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)