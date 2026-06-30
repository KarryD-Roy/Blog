<template>
  <div class="page auth-page">
    <div class="auth-card">
      <h1 class="auth-title">登录</h1>
      <p class="auth-subtitle">欢迎回到 KARRY TECH BLOG</p>
      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input v-model="form.username" type="text" class="input" placeholder="输入用户名" required />
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <input v-model="form.password" type="password" class="input" placeholder="输入密码" required />
        </div>
        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
        <button type="submit" class="btn primary auth-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <p class="auth-switch">
        没有账号？
        <RouterLink to="/register">立即注册</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter, useRoute, RouterLink } from 'vue-router';
import { useAuth } from '../../stores/auth.js';

const { login } = useAuth();
const router = useRouter();
const route = useRoute();

const form = reactive({ username: '', password: '' });
const loading = ref(false);
const errorMsg = ref('');

const handleLogin = async () => {
  if (!form.username.trim() || !form.password.trim()) {
    errorMsg.value = '请填写用户名和密码';
    return;
  }
  loading.value = true;
  errorMsg.value = '';
  try {
    await login(form.username, form.password);
    const redirect = route.query.redirect || '/';
    router.push(redirect);
  } catch (e) {
    errorMsg.value = e.message || '登录失败，请检查用户名和密码';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  padding: 2rem;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: #09090b;
  border: 2px solid #333;
  padding: 3rem 2.5rem;
  box-shadow: 12px 12px 0 rgba(204, 255, 0, 0.15);
}

.auth-title {
  font-family: 'Syne', sans-serif;
  font-size: 2rem;
  font-weight: 800;
  color: #fafafa;
  text-transform: uppercase;
  margin: 0 0 0.5rem;
  text-align: center;
}

.auth-subtitle {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  color: #888;
  text-align: center;
  margin: 0 0 2rem;
  text-transform: uppercase;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  color: #a1a1aa;
  text-transform: uppercase;
  font-weight: bold;
}

.input {
  background: #1a1a1a;
  border: 2px solid #333;
  border-radius: 0;
  color: #fafafa;
  padding: 0.9rem 1rem;
  font-family: 'Manrope', sans-serif;
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s;
}

.input:focus {
  border-color: #ccff00;
}

.input::placeholder {
  color: #555;
}

.error-msg {
  background: rgba(248, 113, 113, 0.15);
  border: 1px solid rgba(248, 113, 113, 0.4);
  color: #fca5a5;
  padding: 0.75rem 1rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
}

.auth-btn {
  margin-top: 0.5rem;
  width: 100%;
  padding: 1rem;
  font-size: 1rem;
}

.btn {
  background-color: transparent;
  border: 2px solid #333;
  border-radius: 0;
  color: #fafafa;
  cursor: pointer;
  padding: 0.6rem 1.2rem;
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
  font-weight: bold;
  transition: all 0.2s ease;
}

.btn.primary {
  background: #ccff00;
  color: #09090b;
  border-color: #ccff00;
}

.btn.primary:hover {
  background: #b3e600;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-switch {
  margin-top: 2rem;
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  color: #888;
}

.auth-switch a {
  color: #ccff00;
  text-decoration: none;
  font-weight: bold;
}

.auth-switch a:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .auth-card {
    padding: 2rem 1.5rem;
  }
  .auth-title {
    font-size: 1.5rem;
  }
}
</style>
