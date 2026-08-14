import request from "@/api/index"
/* 获取情感统计数据 */
export  const getLabelCount = async () => {
  const res = await request.get("/labelCount");
  return res.data;
}
/* 获取情感线性数据数据 */
export const getSentimentLinear = async () => {
  const res = await request.get("/labelCountByTime");
  return res.data;
}

/* 获取情感地图数据 */
export const getSentimentMap = async () => {
  const res = await request.get("/regionEmotion");
  return res.data;
}

// 获取交互度情感分析
export const getSentimentInteraction = async () => {
  const res = await request.get("/interactionEmotion");
  return res.data;
}

// 获取用户认证情感统计
export const getSentimentUserAuth = async () => {
  const res = await request.get("/userAuthEmotion");
  return res.data;
}

// 获取情感堆叠图数据
export const getSentimentStack = async () => {
  const res = await request.get("/opinionTrend");
  return res.data;
}

