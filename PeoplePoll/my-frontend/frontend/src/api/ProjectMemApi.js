import request from '@/api/index';


//请求全部
export const getAll = () => {
  return request.get('/projectMem');
}




