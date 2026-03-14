<template>
  <div class="page">
    <button class="back-btn" @click="$router.back()">← 返回</button>
    <div v-if="loading" class="center-text">加载中...</div>
    <article v-else-if="post" class="card">
      <h1 class="card-title">{{ post.title }}</h1>
      <p class="card-meta">
        <span>浏览：{{ post.viewCount }}</span>
        <span>发布时间：{{ post.createdAt }}</span>
      </p>
      <p class="card-summary">{{ post.summary }}</p>
      <div class="post-content" v-html="renderedContent"></div>
    </article>
    <div v-else class="center-text">文章不存在或已删除。</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import MarkdownIt from 'markdown-it';

const route = useRoute();
const post = ref(null);
const loading = ref(false);

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true
});

const renderedContent = computed(() => {
  if (!post.value || !post.value.content) return '';
  const raw = post.value.content;
  const trimmed = raw.trim();
  if (trimmed.startsWith('<')) {
    return raw;
  }
  return md.render(raw);
});

const fetchDetail = async () => {
  loading.value = true;
  try {
    const res = await axios.get(`/api/posts/${route.params.id}`);
    if (res.data.code === 0) {
      post.value = res.data.data;
    }
  } finally {
    loading.value = false;
  }
};

onMounted(fetchDetail);
</script>

