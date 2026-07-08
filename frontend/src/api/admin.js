import api from './index.js';

// 管理员审核接口
export function getPendingPosts() {
  return api.get('/admin/review/posts');
}

export function getPendingSkills() {
  return api.get('/admin/review/skills');
}

export function reviewPost(id, status, reason) {
  return api.put(`/admin/review/posts/${id}`, { status, reason });
}

export function reviewSkill(id, status, reason) {
  return api.put(`/admin/review/skills/${id}`, { status, reason });
}
