import request from "@/api/index";
// 请求微博整体数据统计
export const getWeiboTotalStatistic = () => {
  const res = request.get('/statistics');
  console.log(res);
  return res;
}

// 请求按用户认证状态统计
export const getWeiboTotalStatisticByAuth = () => {
  const res = request.get('/statistics/user');
  console.log(res);
  return res;
}

// 请求地图热点数据
export const getMapHotData = () => {
  const res = request.get('/statistics/region');
  console.log(res);
  return res;
}
