<template>
  <div class="page">
    <h1 class="page-title">最新文章</h1>

    <div class="toolbar">
      <button class="btn primary" @click="openCreate">
        新增文章
      </button>
    </div>
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

    <div v-if="showEditor" class="modal-backdrop">
      <div class="modal">
        <h2 class="card-title" style="margin-bottom: 0.8rem">
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
          <select v-model="form.categoryId" class="input">
            <option :value="null">选择分类（可选）</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">
              {{ c.name }}
            </option>
          </select>
          <div class="upload-row">
            <input
              ref="fileInput"
              type="file"
              class="file-input-hidden"
              @change="handleFileChange"
            />
            <button class="btn ghost" @click="triggerFile('image')">
              上传图片并插入正文
            </button>
            <button class="btn ghost" @click="triggerFile('attachment')">
              上传附件并插入链接
            </button>
          </div>
          <textarea
            v-model="form.content"
            class="input"
            rows="8"
            placeholder="正文内容，支持纯文本、HTML，以及 Markdown 语法（详情见 README）"
          ></textarea>
          <div class="preview-box" v-if="previewHtml">
            <div class="preview-title">实时预览</div>
            <div class="post-content" v-html="previewHtml"></div>
          </div>
          <div class="editor-actions">
            <button class="btn primary" @click="submitPost">
              {{ editingPost ? '保存修改' : '发布文章' }}
            </button>
            <button class="btn ghost" @click="closeEditor">
              取消
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import MarkdownIt from 'markdown-it';

const router = useRouter();
const posts = ref([]);
const categories = ref([]);
const loading = ref(false);
const page = ref(1);
const totalPages = ref(0);

const showEditor = ref(false);
const editingPost = ref(null);
const form = reactive({
  title: '',
  summary: '',
  content: '',
  tags: '',
  categoryId: null
});

const fileInput = ref(null);
const uploadType = ref('image');

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true
});

const previewHtml = computed(() => {
  if (!form.content) return '';
  const raw = form.content.trim();
  if (raw.startsWith('<')) {
    return raw;
  }
  return md.render(raw);
});

const fetchPosts = async () => {
  loading.value = true;
  try {
    const res = await axios.get('/api/posts', {
      params: { page: page.value, size: 4 }
    });
    if (res.data.code === 0) {
      posts.value = res.data.data.records;
      totalPages.value = res.data.data.pages;
    }
  } finally {
    loading.value = false;
  }
};

const fetchCategories = async () => {
  try {
    const res = await axios.get('/api/categories');
    if (res.data.code === 0) {
      categories.value = res.data.data;
    }
  } catch (err) {
    console.error('获取分类失败', err);
  }
};

const changePage = (p) => {
  page.value = p;
  fetchPosts();
};

const openCreate = () => {
  resetForm();
  showEditor.value = true;
};

const closeEditor = () => {
  showEditor.value = false;
};

const resetForm = () => {
  editingPost.value = null;
  form.title = '';
  form.summary = '';
  form.content = '';
  form.tags = '';
  form.categoryId = null;
};

const startEdit = (post) => {
  editingPost.value = { ...post };
  form.title = post.title || '';
  form.summary = post.summary || '';
  form.content = post.content || '';
  form.tags = post.tags || '';
  form.categoryId = post.categoryId || null;
  showEditor.value = true;
};

const submitPost = async () => {
  if (!form.title.trim()) {
    return;
  }
  const payload = {
    title: form.title,
    summary: form.summary,
    content: form.content,
    tags: form.tags,
    categoryId: form.categoryId
  };
  if (editingPost.value && editingPost.value.id) {
    await axios.put(`/api/posts/${editingPost.value.id}`, payload);
  } else {
    await axios.post('/api/posts', payload);
  }
  resetForm();
  closeEditor();
  fetchPosts();
};

const deletePost = async (id) => {
  // 简单确认，详细交互见 README
  if (!window.confirm('确认删除该文章吗？')) return;
  await axios.delete(`/api/posts/${id}`);
  fetchPosts();
};

const triggerFile = (type) => {
  uploadType.value = type;
  if (fileInput.value) {
    fileInput.value.value = '';
    fileInput.value.click();
  }
};

const handleFileChange = async (e) => {
  const file = e.target.files && e.target.files[0];
  if (!file) return;
  const formData = new FormData();
  formData.append('file', file);
  try {
    const res = await axios.post('/api/oss/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    if (res.data.code === 0 && res.data.data) {
      const url = res.data.data;
      if (uploadType.value === 'image') {
        form.content += `\n\n![${file.name}](${url})\n\n`;
      } else {
        form.content += `\n\n[附件：${file.name}](${url})\n\n`;
      }
    }
  } catch (err) {
    console.error('上传失败', err);
  }
};

const goDetail = (id) => {
  router.push({ name: 'post-detail', params: { id } });
};

onMounted(() => {
  fetchPosts();
  fetchCategories();
});
</script>



