import request from '@/api/index'

//按用户id获取推荐
export const getRecommendByUserID  = async (userId) => {
  const res = await request.get(`/recommend/list`, {
    params: {
      userId
    }
  })
  return res.data
}
