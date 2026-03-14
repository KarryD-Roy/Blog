<template>
  <div class="layout">
    <header class="header">
      <div class="logo">Karry 的技术博客</div>
      <nav class="nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/skills">技能图谱</RouterLink>
      </nav>
    </header>
    <main class="main">
      <RouterView />
    </main>
    <footer class="footer">
      © {{ new Date().getFullYear() }} Karry · Personal Tech Blog
    </footer>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

