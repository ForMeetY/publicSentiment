from datetime import datetime
import torch
import numpy as np

from model import model, tokenizer, device, LABEL_MAP



# 单条预测
def predict_single(text: str):

    inputs = tokenizer(
        text,
        return_tensors="pt",
        truncation=True,
        padding=True,
        max_length=128
    )

    inputs = {k: v.to(device) for k, v in inputs.items()}

    with torch.no_grad():
        outputs = model(**inputs)
        logits = outputs.logits

        probs = torch.softmax(logits, dim=1).cpu().numpy()[0]
        pred = int(np.argmax(probs))

        # score 越接近 1  越正面
        # score 越接近 -1  越负面
        # score 接近 0    中性
        score = float(
            probs[2] * 1.0 +
            probs[1] * 0.0 +
            probs[0] * -1.0
        )

    return {
        "text": text,
        "label": LABEL_MAP[pred],
        "label_id": pred,
        "confidence": float(probs[pred]),
        "score": score,
        "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }



# 批量预测（Spark用）

def predict_batch(texts: list):

    inputs = tokenizer(
        texts,
        return_tensors="pt",
        truncation=True,
        padding=True,
        max_length=128
    )

    inputs = {k: v.to(device) for k, v in inputs.items()}

    with torch.no_grad():
        outputs = model(**inputs)
        logits = outputs.logits

        probs = torch.softmax(logits, dim=1).cpu().numpy()
        preds = np.argmax(probs, axis=1)

    result = []

    for i, text in enumerate(texts):

        score = float(
            probs[i][2] * 1.0 +
            probs[i][1] * 0.0 +
            probs[i][0] * -1.0
        )

        result.append({
            "text": text,
            "label": LABEL_MAP[int(preds[i])],
            "label_id": int(preds[i]),
            "confidence": float(probs[i][preds[i]]),
            "score": score,
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        })

    return result