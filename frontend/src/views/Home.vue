<template>
  <div class="page">
    <h1 class="page-title">最新文章</h1>

    <section class="card" style="cursor: default">
      <h2 class="card-title" style="margin-bottom: 0.6rem">
        {{ editingPost ? '编辑文章' : '新增文章' }}
      </h2>
      <div class="editor-grid">
        <input
          v-model="form.title"
          class="input"
          type="text"
          placeholder="标题（必填）"
        />
        <input
          v-model="form.summary"
          class="input"
          type="text"
          placeholder="摘要 / 简短说明"
        />
        <input
          v-model="form.tags"
          class="input"
          type="text"
          placeholder="标签，以英文逗号分隔，例如：Vue,后端,随笔"
        />
        <textarea
          v-model="form.content"
          class="input"
          rows="5"
          placeholder="正文内容，支持纯文本、HTML，以及 Markdown 语法（详情见 README）"
        ></textarea>
        <div class="editor-actions">
          <button class="btn primary" @click="submitPost">
            {{ editingPost ? '保存修改' : '发布文章' }}
          </button>
          <button
            v-if="editingPost"
            class="btn ghost"
            @click="resetForm"
          >
            取消编辑
          </button>
        </div>
      </div>
    </section>
    <div v-if="loading" class="center-text">加载中...</div>
    <div v-else>
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
        <div class="card-meta" style="margin-top: 0.6rem">
          <button
            class="btn ghost"
            style="font-size: 0.8rem"
            @click.stop="startEdit(post)"
          >
            编辑
          </button>
          <button
            class="btn ghost"
            style="font-size: 0.8rem; color: #fca5a5; border-color: rgba(248, 113, 113, 0.6)"
            @click.stop="deletePost(post.id)"
          >
            删除
          </button>
        </div>
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
import { onMounted, ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const posts = ref([]);
const loading = ref(false);
const page = ref(1);
const totalPages = ref(1);

const editingPost = ref(null);
const form = reactive({
  title: '',
  summary: '',
  content: '',
  tags: ''
});

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

const resetForm = () => {
  editingPost.value = null;
  form.title = '';
  form.summary = '';
  form.content = '';
  form.tags = '';
};

const startEdit = (post) => {
  editingPost.value = { ...post };
  form.title = post.title || '';
  form.summary = post.summary || '';
  form.content = post.content || '';
  form.tags = post.tags || '';
};

const submitPost = async () => {
  if (!form.title.trim()) {
    return;
  }
  const payload = {
    title: form.title,
    summary: form.summary,
    content: form.content,
    tags: form.tags
  };
  if (editingPost.value && editingPost.value.id) {
    await axios.put(`/api/posts/${editingPost.value.id}`, payload);
  } else {
    await axios.post('/api/posts', payload);
  }
  resetForm();
  fetchPosts();
};

const deletePost = async (id) => {
  // 简单确认，详细交互见 README
  if (!window.confirm('确认删除该文章吗？')) return;
  await axios.delete(`/api/posts/${id}`);
  fetchPosts();
};

const goDetail = (id) => {
  router.push({ name: 'post-detail', params: { id } });
};

onMounted(fetchPosts);
</script>

