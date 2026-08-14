import numpy as np
import pandas as pd
import joblib
from datetime import datetime
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional

# ── 模型加载 ──────────────────────────────────────────────
gmm          = joblib.load("../models/GMM/gmm_cluster_model.pkl")
scaler       = joblib.load("../models/GMM/scaler.pkl")
feature_cols = joblib.load("../models/GMM/feature_cols.pkl")

# clip 上界：训练时用 quantile(0.99) 截断但没保存，这里用保守固定值兜底
# 如果你重新训练时 joblib.dump(clip_bounds, "clip_bounds.pkl") 保存了就直接加载
# clip_bounds = joblib.load("../models/GMM/clip_bounds.pkl")
# 否则用这个经验值（与训练数据量级匹配即可，主要防大V极端值）
CLIP_BOUNDS = {
    "hourly_sentiment_stddev": 1.0,
    "night_post_ratio":        1.0,   # 本身就是 0~1，clip 到 1 无损
    "region_diversity_score":  50.0,
    "top_region_avg_score":    1.0,   # score 范围 -1~1
    "sentiment_leverage":      20.0,  # log1p 前截断
}

app = FastAPI(title="GMM 用户圈层聚类服务")

CLUSTER_LABELS = {
    -1: "沉默用户",
     0: "生活号",
     1: "夜猫子",
     2: "舆情大V",
     3: "情绪宣泄者",
}

# ── 数据模型 ──────────────────────────────────────────────
class UserFeatures(BaseModel):
    user_id:                  Optional[str] = None   # 透传，方便批量结果对齐
    hourly_sentiment_stddev:  float
    night_post_ratio:         float
    region_diversity_score:   float
    top_region_avg_score:     float
    sentiment_leverage:       float


class BatchRequest(BaseModel):
    users: List[UserFeatures]


# ── 核心预处理（训练/推断必须镜像） ─────────────────────────
def preprocess(df: pd.DataFrame) -> np.ndarray:
    """
    与训练脚本完全镜像：
      1. clip 长尾截断
      2. log1p 平滑 sentiment_leverage
      3. StandardScaler transform（注意：不是 fit_transform）
    """
    df = df[feature_cols].copy()

    # Step1: clip
    for col in feature_cols:
        df[col] = df[col].clip(upper=CLIP_BOUNDS.get(col, None))

    # Step2: log1p（只对 sentiment_leverage）
    df["sentiment_leverage"] = np.log1p(df["sentiment_leverage"])

    # Step3: 标准化（保留 DataFrame 让 sklearn 不报 feature names 警告）
    return scaler.transform(df)


def is_silent(row: dict) -> bool:
    """与训练时的 is_active 判断镜像"""
    return not (
        row["hourly_sentiment_stddev"] > 0.001
        or row["night_post_ratio"]       > 0
        or row["region_diversity_score"] > 0
        or row["sentiment_leverage"]     > 0
    )

def make_silent_result(user_id: Optional[str]) -> dict:
    return {
        "user_id":    user_id,
        "cluster_id": -1,
        "cluster_label": CLUSTER_LABELS[-1],
        "prob_0": 0.25,
        "prob_1": 0.25,
        "prob_2": 0.25,
        "prob_3": 0.25,
    }

#  单条预测
@app.post("/gmm/predict")
def predict(user: UserFeatures):
    data = user.model_dump()
    if is_silent(data):
        result = make_silent_result(data.get("user_id"))
    else:
        df = pd.DataFrame([data])
        x  = preprocess(df)
        cid  = int(gmm.predict(x)[0])
        prob = gmm.predict_proba(x)[0].round(4)
        result = {
            "user_id":       data.get("user_id"),
            "cluster_id":    cid,
            "cluster_label": CLUSTER_LABELS.get(cid, "未知"),
            "prob_0": float(prob[0]),
            "prob_1": float(prob[1]),
            "prob_2": float(prob[2]),
            "prob_3": float(prob[3]),
        }

    result["time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    return result

# 批量预测
@app.post("/gmm/predict_batch")
def predict_batch(batch: BatchRequest):
    if not batch.users:
        return {"count": 0, "results": [], "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

    all_data = [u.model_dump() for u in batch.users]

    # 分流：沉默 vs 活跃
    silent_rows = [(i, d) for i, d in enumerate(all_data) if is_silent(d)]
    active_rows = [(i, d) for i, d in enumerate(all_data) if not is_silent(d)]

    results = [None] * len(all_data)

    # 沉默用户直接填默认值
    for i, d in silent_rows:
        results[i] = make_silent_result(d.get("user_id"))

    # 活跃用户批量推断
    if active_rows:
        indices, data_list = zip(*active_rows)
        df_active   = pd.DataFrame(data_list)
        X_scaled    = preprocess(df_active)
        cluster_ids = gmm.predict(X_scaled)
        probs       = gmm.predict_proba(X_scaled).round(4)

        for pos, (original_idx, d) in enumerate(zip(indices, data_list)):
            cid = int(cluster_ids[pos])
            results[original_idx] = {
                "user_id":       d.get("user_id"),
                "cluster_id":    cid,
                "cluster_label": CLUSTER_LABELS.get(cid, "未知"),
                "prob_0": float(probs[pos][0]),
                "prob_1": float(probs[pos][1]),
                "prob_2": float(probs[pos][2]),
                "prob_3": float(probs[pos][3]),
            }

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    for r in results:
        r["time"] = now

    return {"count": len(results), "results": results, "time": now}


#  健康检查
@app.get("/health")
def health():
    return {
        "status":        "ok",
        "model":         "GaussianMixture",
        "n_components":  gmm.n_components,
        "feature_cols":  feature_cols,
        "time":          datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)