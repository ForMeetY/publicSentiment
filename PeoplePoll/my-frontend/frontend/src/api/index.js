import axios from 'axios';
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
});
let loadingInstance = null;
import { ElLoading } from 'element-plus';

// 添加请求拦截器
request.interceptors.request.use(function (config) {
    // 在发送请求之前做些什么
    /* 添加加载动画 */
    loadingInstance = ElLoading.service({
    lock: true,
    text: '加载中...',
    background: 'rgba(0, 0, 0, 0.7)',
  });
    return config;
  }, function (error) {
    // 对请求错误做些什么
    /* 关闭加载动画 */
    loadingInstance.close();
    return Promise.reject(error);
  });

// 添加响应拦截器
request.interceptors.response.use(function (res) {
    // 2xx 范围内的状态码都会触发该函数。
    // 对响应数据做点什么
    /* 关闭加载动画 */
    loadingInstance.close();
    return res.data;
  }, function (error) {
    // 超出 2xx 范围的状态码都会触发该函数。
    // 对响应错误做点什么
    return Promise.reject(error);
  });

  export default request
