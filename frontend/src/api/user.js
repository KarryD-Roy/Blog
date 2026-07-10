import api from './index.js';

// 用户个人相关接口
export function getProfile() {
  return api.get('/users/profile');
}

export function updateProfile(data) {
  return api.put('/users/profile', data);
}

// 获取我的文章（分页）
export function myPosts(page = 1, size = 10, status) {
  const params = { page, size };
  if (status) params.status = status;
  return api.get('/users/posts', { params });
}

// 签到相关
export function checkIn() {
  return api.post('/users/checkin');
}

export function getCheckinStatus() {
  return api.get('/users/checkin/status');
}

// Admin: 角色授权
export function assignRole(userId, roleName) {
  return api.put(`/users/${userId}/role/${roleName}`);
}

// 获取指定用户的公开主页信息
export function getUserById(id) {
  return api.get(`/users/${id}`);
}
