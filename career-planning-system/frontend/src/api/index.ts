import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';

const service = axios.create({
  baseURL: '/api',
  timeout: 300000
});

// 请求拦截器：自动携带 Token
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器：统一错误处理 + 401 跳转登录
service.interceptors.response.use(
  response => {
    const res = response.data;
    if (res.code !== 200) {
      // 业务错误
      if (res.code === 400) {
        ElMessage.warning(res.message || '参数校验失败');
      } else if (res.code === 401) {
        // Token 无效 / 过期 / 黑名单：清空本地 Token，跳转登录
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        ElMessage.error(res.message || '登录已失效，请重新登录');
        router.push('/login');
        return Promise.reject(new Error(res.message || 'Unauthorized'));
      } else {
        ElMessage.error(res.message || '请求失败');
      }
      return Promise.reject(new Error(res.message || 'Error'));
    }
    return res.data;
  },
  error => {
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，AI任务仍可能在后台执行，请稍后重试或刷新页面查看结果');
      return Promise.reject(error);
    }
    // HTTP 层面的 401（如网络层面）
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      ElMessage.error('登录已过期，请重新登录');
      router.push('/login');
      return Promise.reject(error);
    }
    ElMessage.error(error.message || '网络请求错误');
    return Promise.reject(error);
  }
);

export default service;
