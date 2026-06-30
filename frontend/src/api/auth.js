import api from './index.js';

export function login(username, password) {
  return api.post('/auth/login', { username, password });
}

export function register(username, password, email, nickname) {
  return api.post('/auth/register', { username, password, email, nickname });
}

export function getProfile() {
  return api.get('/auth/profile');
}
