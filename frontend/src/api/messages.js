import api from './index.js';

export function getMessages(page = 1, size = 10) {
  return api.get('/messages', { params: { page, size } });
}

export function getUnreadCount() {
  return api.get('/messages/unread-count');
}

export function markAsRead(id) {
  return api.put(`/messages/${id}/read`);
}

export function markAllAsRead() {
  return api.put('/messages/read-all');
}
