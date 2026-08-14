import request from '@/api/index'

// 分页查询用户画像
export const getUserInfoPage = async (page, pageSize) => {
  const res = await request({
    url: '/userInfo',
    method: 'get',
     params: {
      page: page,
      pageSize: pageSize
    }
  })
  return res.data
}

// 根据用户Id查询用户画像详情
export const getUserInfoDetail = async (userId) => {
  const res = await request({
    url: '/userInfo/' + userId,
    method: 'get'
  })
  return res.data
}

// 调用deekseekV4接口，获取用户画像分析
export const getUserInfoAnalysis = async (userId) => {
  const res = await request({
    url: '/userInfo/ai-analysis/' + userId,
    timeout: 60000,
    method: 'get'
  })
  return res.data
}
