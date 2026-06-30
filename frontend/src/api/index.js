import axios from 'axios';
import router from '../router/index.js';

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const status = error.response.status;
      if (status === 401 || status === 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        const currentPath = router.currentRoute?.value?.fullPath || '/';
        if (router.currentRoute?.value?.name !== 'login') {
          router.push({ name: 'login', query: { redirect: currentPath } });
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
