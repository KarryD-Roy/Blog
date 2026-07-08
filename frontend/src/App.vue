<template>
  <div class="layout">
    <header class="header">
      <!-- Logo Row -->
      <div class="logo-row">
        <div class="logo">KARRY TECH BLOG</div>
        <nav class="nav user-nav">
          <template v-if="isAuthenticated">
            <div class="dropdown">
              <span class="dropdown-trigger user-trigger">{{ user?.nickname || user?.username }}</span>
              <div class="dropdown-content user-dropdown">
                <RouterLink to="/profile">个人中心</RouterLink>
                <RouterLink to="/messages" class="msg-link">
                  我的消息
                  <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
                </RouterLink>
                <div class="dropdown-divider"></div>
                <a href="#" @click.prevent="handleLogout">退出登录</a>
              </div>
            </div>
          </template>
          <template v-else>
            <RouterLink to="/login">登录</RouterLink>
            <RouterLink to="/register">注册</RouterLink>
          </template>
        </nav>
      </div>

      <!-- Centered Navigation Bar -->
      <nav class="nav main-nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/skills">技能树</RouterLink>
        <RouterLink to="/statistics">数据统计</RouterLink>
        <RouterLink to="/search">搜索</RouterLink>
        <div class="dropdown">
          <span class="dropdown-trigger">AI 工具</span>
          <div class="dropdown-content">
            <RouterLink to="/ai/writer">AI 帮写</RouterLink>
            <RouterLink to="/ai/recommendation">AI 推荐</RouterLink>
          </div>
        </div>
        <RouterLink to="/posts">文章大全</RouterLink>
        <RouterLink to="/profile" class="profile-nav-link">个人中心</RouterLink>
      </nav>
    </header>
    <main class="main">
      <router-view v-slot="{ Component }">
        <keep-alive include="Home,PostList,Search">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>
    <footer class="footer">
      &copy; {{ new Date().getFullYear() }} Karry &middot; Personal Tech Blog
    </footer>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, watch } from 'vue';
import { RouterLink, RouterView, useRouter } from 'vue-router';
import { useAuth } from './stores/auth.js';
import { useMessages } from './stores/message.js';

const router = useRouter();
const { isAuthenticated, user, logout } = useAuth();
const { unreadCount, startPolling, stopPolling } = useMessages();

const handleLogout = () => {
  logout();
  stopPolling();
  router.push('/');
};

watch(isAuthenticated, (val) => {
  if (val) startPolling();
  else stopPolling();
});

onMounted(() => {
  if (isAuthenticated.value) startPolling();
});

onUnmounted(() => {
  stopPolling();
});
</script>
