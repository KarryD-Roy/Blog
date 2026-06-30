import { ref, computed } from 'vue';
import { login as apiLogin, register as apiRegister, getProfile } from '../api/auth.js';

const user = ref(null);
const token = ref(localStorage.getItem('token') || null);
const isAuthenticated = computed(() => !!token.value && !!user.value);

export function useAuth() {
  const login = async (username, password) => {
    const res = await apiLogin(username, password);
    if (res.data.code === 0) {
      const data = res.data.data;
      token.value = data.token;
      localStorage.setItem('token', data.token);
      user.value = {
        id: data.userId,
        username: data.username,
        nickname: data.nickname,
        roles: data.roles
      };
      localStorage.setItem('user', JSON.stringify(user.value));
      return data;
    }
    throw new Error(res.data.message || '登录失败');
  };

  const register = async (username, password, email, nickname) => {
    const res = await apiRegister(username, password, email, nickname);
    if (res.data.code === 0) {
      const data = res.data.data;
      token.value = data.token;
      localStorage.setItem('token', data.token);
      user.value = {
        id: data.userId,
        username: data.username,
        nickname: data.nickname,
        roles: data.roles
      };
      localStorage.setItem('user', JSON.stringify(user.value));
      return data;
    }
    throw new Error(res.data.message || '注册失败');
  };

  const logout = () => {
    token.value = null;
    user.value = null;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  };

  const fetchProfile = async () => {
    if (!token.value) return;
    try {
      const res = await getProfile();
      if (res.data.code === 0) {
        user.value = {
          id: res.data.data.id,
          username: res.data.data.username,
          nickname: res.data.data.nickname,
          roles: res.data.data.roles
        };
        localStorage.setItem('user', JSON.stringify(user.value));
      } else {
        logout();
      }
    } catch {
      // Token invalid or expired
    }
  };

  // Initialize from localStorage
  if (token.value && !user.value) {
    const saved = localStorage.getItem('user');
    if (saved) {
      try { user.value = JSON.parse(saved); } catch { /* ignore */ }
    }
    fetchProfile();
  }

  return { user, token, isAuthenticated, login, register, logout, fetchProfile };
}
