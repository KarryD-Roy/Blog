import api from './index.js';

export function toggleLike(postId) {
  return api.post(`/posts/${postId}/like`);
}

export function getLikeStatus(postId) {
  return api.get(`/posts/${postId}/like`);
}
