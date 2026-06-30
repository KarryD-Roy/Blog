import api from './index.js';

export function getComments(postId) {
  return api.get(`/posts/${postId}/comments`);
}

export function addComment(postId, content, parentId = null) {
  return api.post(`/posts/${postId}/comments`, { postId, content, parentId });
}

export function deleteComment(postId, commentId) {
  return api.delete(`/posts/${postId}/comments/${commentId}`);
}
