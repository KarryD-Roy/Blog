<template>
  <div class="layout">
    <header class="header">
      <div class="logo">Karry 的技术博客</div>
      <nav class="nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/skills">技能图谱</RouterLink>
        <RouterLink to="/statistics">数据统计</RouterLink>
        <RouterLink to="/search">搜索</RouterLink>
        <RouterLink to="/ai/writer">AI 帮写</RouterLink>
        <RouterLink to="/ai/recommendation">AI 推荐</RouterLink>
        <RouterLink to="/posts">文章大全</RouterLink>
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
      © {{ new Date().getFullYear() }} Karry · Personal Tech Blog
    </footer>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>
