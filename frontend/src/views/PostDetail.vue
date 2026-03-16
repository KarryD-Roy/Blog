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

      <div v-if="attachments.length > 0" class="attachments-section">
        <h3>附件列表</h3>
        <div v-for="(file, index) in attachments" :key="index" class="attachment-item">
          <span class="file-name">{{ file.name }}</span>
          <div class="file-actions">
            <button class="btn ghost small" @click="downloadFile(file.url)">下载</button>
            <button v-if="isPreviewable(file.name)" class="btn ghost small" @click="previewFile(file.url)">在线预览</button>
          </div>
        </div>
      </div>
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

const attachments = computed(() => {
  if (!post.value || !post.value.content) return [];
  const content = post.value.content;
  const regex = /\[附件：([^\]]+)\]\(([^)]+)\)/g;
  const found = [];
  let match;
  while ((match = regex.exec(content)) !== null) {
    found.push({ name: match[1], url: match[2] });
  }
  return found;
});

const isPreviewable = (name) => {
  const ext = name.split('.').pop().toLowerCase();
  return ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'txt'].includes(ext);
};

const downloadFile = (url) => {
  window.open(url, '_blank');
};

const previewFile = (url) => {
  window.open(url, '_blank');
};

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

<style scoped>
.page {
  padding: 20px;
}

.back-btn {
  background: none;
  border: none;
  color: #007bff;
  cursor: pointer;
  font-size: 16px;
  margin-bottom: 20px;
}

.center-text {
  text-align: center;
}

.card {
  /* Use global styles for background/color */
  min-height: 60vh;
}

.card-title {
  font-size: 24px;
  margin: 0 0 10px;
}

.card-meta {
  /* Use global color */
  font-size: 14px;
  margin: 0 0 10px;
}

.card-summary {
  font-size: 16px;
  margin: 0 0 20px;
}

.post-content {
  font-size: 16px;
  line-height: 1.6;
}

.attachments-section {
  margin-top: 20px;
}

.attachment-item {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.file-name {
  font-size: 16px;
}

.file-actions {
  display: flex;
  gap: 10px;
}

.btn {
  background-color: #007bff;
  border: none;
  border-radius: 4px;
  color: white;
  cursor: pointer;
  padding: 10px 15px;
  text-align: center;
}

.btn.ghost {
  background-color: transparent;
  border: 1px solid #007bff;
  color: #007bff;
}

.btn.small {
  font-size: 14px;
  padding: 5px 10px;
}
</style>
