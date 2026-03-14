<template>
  <div class="page">
    <h1 class="page-title">搜索</h1>

    <section class="card" style="cursor: default">
      <div style="display: flex; flex-direction: column; gap: 0.6rem">
        <input
          v-model="keyword"
          type="text"
          class="input"
          placeholder="输入关键词（标题 / 摘要 / 正文 / 标签）回车搜索"
          @keyup.enter="handleSearch"
        />
        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap">
          <button class="btn primary" @click="handleSearch">搜索</button>
          <button class="btn ghost" @click="resetSearch">重置</button>
        </div>
      </div>
    </section>

    <section class="card" v-if="tags.length" style="cursor: default">
      <div style="margin-bottom: 0.5rem; font-size: 0.9rem; color: #9ca3af">
        快捷标签
      </div>
      <div>
        <button
          v-for="tag in tags"
          :key="tag"
          class="tag-btn"
          :class="{ active: tag === selectedTag }"
          @click="toggleTag(tag)"
        >
          # {{ tag }}
        </button>
      </div>
    </section>

    <div v-if="loading" class="center-text">搜索中...</div>

    <div v-else>
      <div v-if="posts.length === 0" class="center-text">
        暂无搜索结果，请尝试更换关键词或标签。
      </div>

      <article
        v-for="post in posts"
        :key="post.id"
        class="card"
        @click="goDetail(post.id)"
      >
        <h2 class="card-title">{{ post.title }}</h2>
        <p class="card-meta">
          <span>浏览：{{ post.viewCount }}</span>
          <span>发布时间：{{ post.createdAt }}</span>
        </p>
        <p class="card-summary">{{ post.summary }}</p>
        <p class="card-tags" v-if="post.tags">
          <span
            v-for="tag in post.tags.split(',')"
            :key="tag"
            class="tag"
          >
            # {{ tag }}
          </span>
        </p>
      </article>

      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="page === 1" @click="changePage(page - 1)">
          上一页
        </button>
        <span>第 {{ page }} / {{ totalPages }} 页</span>
        <button :disabled="page === totalPages" @click="changePage(page + 1)">
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();

const keyword = ref('');
const selectedTag = ref('');
const tags = ref([]);
const posts = ref([]);
const loading = ref(false);
const page = ref(1);
const totalPages = ref(1);

const fetchTags = async () => {
  try {
    const res = await axios.get('/api/posts/tags');
    if (res.data.code === 0 && Array.isArray(res.data.data)) {
      tags.value = res.data.data;
    } else if (res.data.code === 0 && res.data.data) {
      tags.value = Array.from(res.data.data);
    }
  } catch {
    // ignore tag load error
  }
};

const fetchPosts = async () => {
  loading.value = true;
  try {
    const res = await axios.get('/api/posts/query', {
      params: {
        page: page.value,
        size: 5,
        keyword: keyword.value || undefined,
        tag: selectedTag.value || undefined
      }
    });
    if (res.data.code === 0) {
      posts.value = res.data.data.records || [];
      totalPages.value = res.data.data.pages || 1;
    }
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  fetchPosts();
};

const resetSearch = () => {
  keyword.value = '';
  selectedTag.value = '';
  page.value = 1;
  fetchPosts();
};

const toggleTag = (tag) => {
  if (selectedTag.value === tag) {
    selectedTag.value = '';
  } else {
    selectedTag.value = tag;
  }
  page.value = 1;
  fetchPosts();
};

const changePage = (p) => {
  page.value = p;
  fetchPosts();
};

const goDetail = (id) => {
  router.push({ name: 'post-detail', params: { id } });
};

onMounted(() => {
  fetchTags();
  fetchPosts();
});
</script>

