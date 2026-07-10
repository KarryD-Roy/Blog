import api from './index.js';

export function listPosts(page = 1, size = 10, keyword = '', categoryId = null) {
  return api.get('/posts/query', { params: { page, size, keyword, categoryId } });
}

export function getPost(id) {
  return api.get(`/posts/${id}`);
}

export function createPost(data) {
  return api.post('/posts', data);
}

export function updatePost(id, data) {
  return api.put(`/posts/${id}`, data);
}

export function deletePost(id) {
  return api.delete(`/posts/${id}`);
}

// 获取指定作者的公开文章列表（用于用户主页）
export function getUserPosts(authorId, page = 1, size = 10) {
  return api.get('/posts', { params: { page, size, authorId } });
}
