<template>
  <div class="page">
    <h1 class="page-title">最新文章</h1>
    <div v-if="loading" class="center-text">加载中...</div>
    <div v-else>
      <article v-for="post in posts" :key="post.id" class="card" @click="goDetail(post.id)">
        <h2 class="card-title">{{ post.title }}</h2>
        <p class="card-meta">
          <span>浏览：{{ post.viewCount }}</span>
          <span>发布时间：{{ post.createdAt }}</span>
        </p>
        <p class="card-summary">{{ post.summary }}</p>
        <p class="card-tags" v-if="post.tags">
          <span v-for="tag in post.tags.split(',')" :key="tag" class="tag"># {{ tag }}</span>
        </p>
      </article>

      <div class="pagination">
        <button :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
        <span>第 {{ page }} / {{ totalPages }} 页</span>
        <button :disabled="page === totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const posts = ref([]);
const loading = ref(false);
const page = ref(1);
const totalPages = ref(1);

const fetchPosts = async () => {
  loading.value = true;
  try {
    const res = await axios.get('/api/posts', {
      params: { page: page.value, size: 5 }
    });
    if (res.data.code === 0) {
      posts.value = res.data.data.records || [];
      totalPages.value = res.data.data.pages || 1;
    }
  } finally {
    loading.value = false;
  }
};

const changePage = (p) => {
  page.value = p;
  fetchPosts();
};

const goDetail = (id) => {
  router.push({ name: 'post-detail', params: { id } });
};

onMounted(fetchPosts);
</script>

