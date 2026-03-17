<template>
  <div class="page">
    <h1 class="page-title">文章大全</h1>

    <section class="card" style="cursor: default">
      <div class="filters">
        <input
          v-model="keyword"
          type="text"
          class="input"
          placeholder="标题 / 摘要 / 正文 关键词"
          @keyup.enter="handleFilter"
        />

        <div class="custom-select-container"
             @mouseenter="showCategoryDropdown = true"
             @mouseleave="showCategoryDropdown = false">
          <div class="input select-trigger">
            <span>{{ selectedCategoryName }}</span>
          </div>
          <transition name="fade">
            <div v-if="showCategoryDropdown" class="select-dropdown">
              <div class="select-option placeholder"
                   :class="{ selected: categoryId === '' }"
                   @click="selectCategory('')">
                全部分类
              </div>
              <div class="select-option"
                   v-for="c in categories"
                   :key="c.id"
                   :class="{ selected: categoryId === c.id }"
                   @click="selectCategory(c.id)">
                {{ c.name }}
              </div>
            </div>
          </transition>
        </div>

        <input
          v-model="tag"
          type="text"
          class="input"
          placeholder="标签（单个标签）"
          @keyup.enter="handleFilter"
        />

        <div class="filter-actions">
          <button class="btn primary" @click="handleFilter">筛选</button>
          <button class="btn ghost" @click="resetFilter">重置</button>
        </div>
      </div>
    </section>

    <div v-if="loading && posts.length === 0" class="center-text">加载中...</div>

    <div v-else>
      <div v-if="loading" class="center-text" style="font-size: 0.8rem; margin-bottom: 0.5rem">Updating...</div>
      <div v-if="posts.length === 0" class="center-text">
        暂无文章，请调整筛选条件。
      </div>

      <article
        v-for="post in posts"
        :key="post.id"
        class="card"
        @click="goDetail(post.id)"
      >
        <h2 class="card-title">
          <span v-if="post.pinned" class="tag" style="margin-right: 0.4rem">置顶</span>
          {{ post.title }}
        </h2>
        <p class="card-meta">
          <span>浏览：{{ post.viewCount }}</span>
          <span>发布时间：{{ post.createdAt }}</span>
          <span v-if="post.categoryId" class="category-info">分类ID：{{ post.categoryId }}</span>
        </p>
        <p class="card-summary">{{ post.summary }}</p>
        <p class="card-tags" v-if="post.tags">
          <span
            v-for="tg in post.tags.split(',')"
            :key="tg"
            class="tag"
          >
            # {{ tg }}
          </span>
        </p>
        <div class="card-meta" style="margin-top: 0.4rem">
          <button
            class="btn ghost"
            style="font-size: 0.75rem"
            @click.stop="togglePin(post)"
          >
            {{ post.pinned ? '取消置顶' : '置顶' }}
          </button>
        </div>
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
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';

defineOptions({
  name: 'PostList'
});

const router = useRouter();
const route = useRoute();

const keyword = ref(route.query.keyword || '');
const categoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : '');
const tag = ref(route.query.tag || '');
const categories = ref([]);
const posts = ref([]);
const loading = ref(false);
const page = ref(route.query.page ? parseInt(route.query.page) : 1);
const totalPages = ref(1);
const showCategoryDropdown = ref(false);

const selectedCategoryName = computed(() => {
  if (categoryId.value === '' || categoryId.value === null) return '全部分类';
  const cat = categories.value.find(c => c.id === categoryId.value);
  return cat ? cat.name : '全部分类';
});

const fetchCategories = async () => {
  try {
    const res = await axios.get('/api/categories');
    if (res.data.code === 0) {
      categories.value = res.data.data || [];
    }
  } catch {
    // ignore
  }
};

const fetchPosts = async () => {
  loading.value = true;
  try {
    const res = await axios.get('/api/posts/query', {
      params: {
        page: page.value,
        size: 10,
        keyword: keyword.value || undefined,
        categoryId: categoryId.value || undefined,
        tag: tag.value || undefined
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

const updateQuery = () => {
  router.push({
    query: {
      page: page.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      tag: tag.value || undefined
    }
  });
};

const handleFilter = () => {
  page.value = 1;
  updateQuery();
};

const togglePin = async (post) => {
  const target = !post.pinned;
  await axios.put(`/api/posts/${post.id}/pin`, null, {
    params: { pinned: target }
  });
  fetchPosts();
};

const resetFilter = () => {
  keyword.value = '';
  categoryId.value = '';
  tag.value = '';
  page.value = 1;
  updateQuery();
};

const changePage = (p) => {
  page.value = p;
  updateQuery();
};

const goDetail = (id) => {
  router.push({ name: 'post-detail', params: { id } });
};

const selectCategory = (id) => {
  categoryId.value = id;
  handleFilter();
};

watch(
  () => route.query,
  (newQuery) => {
    if (route.name !== 'post-list') return;
    page.value = newQuery.page ? parseInt(newQuery.page) : 1;
    keyword.value = newQuery.keyword || '';
    categoryId.value = newQuery.categoryId ? Number(newQuery.categoryId) : '';
    tag.value = newQuery.tag || '';
    fetchPosts();
  }
);

onMounted(() => {
  fetchCategories();
  fetchPosts();
});
</script>



