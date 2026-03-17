<template>
  <div class="page">

    <div class="home-layout">
      <div class="left-column">
        <aside class="hot-news-card">
        <div class="hot-news-header">
          <div>
            <div class="hot-news-title">技术热点资讯</div>
            <div class="hot-news-subtitle">每日更新，点击跳转查看详情</div>
          </div>
          <div class="hot-news-date">{{ today }}</div>
        </div>

        <div class="hot-news-carousel" v-if="hotNews.length">
          <swiper
            :modules="modules"
            :slides-per-view="1"
            :space-between="20"
            :pagination="{ clickable: true }"
            :autoplay="{ delay: 4000, disableOnInteraction: false }"
            :loop="true"
            class="my-swiper"
          >
            <swiper-slide v-for="item in hotNews" :key="item.id">
              <div class="hot-news-slide" @click="openLink(item.url)">
                <div class="hot-news-image" :style="bgImage(item.imageUrl)"></div>
                <div class="hot-news-info">
                  <div class="hot-news-item-title">{{ item.title }}</div>
                  <div class="hot-news-meta">
                    <span>{{ item.source || '来源' }}</span>
                    <span>{{ formatDate(item.publishDate) }}</span>
                  </div>
                </div>
              </div>
            </swiper-slide>
          </swiper>
        </div>
        <div v-else class="center-text" style="padding: 1rem;">暂无热点资讯</div>
      </aside>

      <div class="quick-nav-card" style="margin-top: 1rem;">
          <div class="hot-news-title" style="margin-bottom: 1rem;">快捷导航</div>
          <div class="nav-list">
            <div
              v-for="nav in quickNavs"
              :key="nav.id"
              class="nav-item"
              @click="openLink(nav.url)"
            >
              <img :src="nav.icon" class="nav-icon" alt="icon" />
              <div class="nav-info">
                <div class="nav-title">{{ nav.title }}</div>
                <div class="nav-desc">{{ nav.desc }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="quick-nav-card" style="margin-top: 1rem;">
          <div class="hot-news-title" style="margin-bottom: 1rem;">API官方文档</div>
          <div class="nav-list">
            <div
              v-for="doc in apiDocs"
              :key="doc.id"
              class="nav-item"
              @click="openLink(doc.url)"
            >
              <div class="nav-info">
                <div class="nav-title">{{ doc.title }}</div>
                <div class="nav-desc">{{ doc.desc }}</div>
              </div>
            </div>
          </div>
        </div>
    </div>

      <div class="main-column">
        <div class="main-header">
           <h1 class="page-title" style="margin-bottom: 0;">最新文章</h1>
           <div class="toolbar" style="margin-bottom: 0;">
            <button class="btn primary" @click="openCreate">
              新增文章
            </button>
          </div>
        </div>
        <div v-if="loading && posts.length === 0" class="center-text">加载中...</div>
        <div v-else>
          <div v-if="loading" class="center-text" style="font-size: 0.8rem; margin-bottom: 0.5rem">Updating...</div>
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
          <div
            class="custom-select-container"
            @mouseenter="showCategoryDropdown = true"
            @mouseleave="showCategoryDropdown = false"
          >
            <div
              class="input select-trigger"
            >
              <span>{{ selectedCategoryName }}</span>
              <!-- 下箭头图标由 input 样式的 background-image 提供，此处可不再额外添加 -->
            </div>

            <transition name="fade">
              <div v-if="showCategoryDropdown" class="select-dropdown">
                <div
                  class="select-option placeholder"
                  @click="selectCategory(null)"
                  :class="{ selected: form.categoryId === null }"
                >
                  选择分类（可选）
                </div>
                <div
                  v-for="c in categories"
                  :key="c.id"
                  class="select-option"
                  :class="{ selected: form.categoryId === c.id }"
                  @click="selectCategory(c.id)"
                >
                  {{ c.name }}
                </div>
              </div>
            </transition>
          </div>
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
import { computed, onMounted, ref, reactive, watch, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import MarkdownIt from 'markdown-it';
import { Swiper, SwiperSlide } from 'swiper/vue';
import { Pagination, Autoplay } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/pagination';

const modules = [Pagination, Autoplay];

defineOptions({
  name: 'Home'
});

const router = useRouter();
const route = useRoute();
const posts = ref([]);
const categories = ref([]);
const loading = ref(false);
const page = ref(route.query.page ? parseInt(route.query.page) : 1);
const totalPages = ref(0);

const showEditor = ref(false);
const showCategoryDropdown = ref(false); // 控制自定义下拉菜单显示
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
  router.push({ query: { ...route.query, page: p } });
};

watch(
  () => route.query.page,
  (newPage) => {
    if (route.name !== 'home') return;
    page.value = newPage ? parseInt(newPage) : 1;
    fetchPosts();
  }
);

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

// 获取当前选中的分类名称
const selectedCategoryName = computed(() => {
  if (!form.categoryId) return '选择分类（可选）';
  const cat = categories.value.find(c => c.id === form.categoryId);
  return cat ? cat.name : '选择分类（可选）';
});

// 选择分类
const selectCategory = (id) => {
  form.categoryId = id;
  showCategoryDropdown.value = false;
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

const quickNavs = ref([
  {
    id: 1,
    title: 'GitHub',
    desc: '全球最大的代码托管平台',
    url: 'https://github.com',
    icon: 'https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png'
  },
  {
    id: 2,
    title: 'Bilibili',
    desc: '国内知名的视频弹幕网站',
    url: 'https://www.bilibili.com',
    icon: 'https://www.bilibili.com/favicon.ico'
  },
  {
    id: 3,
    title: 'CSDN',
    desc: '专业开发者社区',
    url: 'https://www.csdn.net',
    icon: 'https://g.csdnimg.cn/static/logo/favicon32.ico'
  },
  {
    id: 4,
    title: 'Stack Overflow',
    desc: '全球最大的程序员问答社区',
    url: 'https://stackoverflow.com',
    icon: 'https://cdn.sstatic.net/Sites/stackoverflow/Img/apple-touch-icon.png'
  }
]);

const apiDocs = ref([
  { id: 1, title: 'Spring Framework API', desc: 'Spring 官方参考与 Javadoc', url: 'https://docs.spring.io/spring-framework/reference/' },
  { id: 2, title: 'Spring Boot API', desc: 'Spring Boot 参考文档', url: 'https://docs.spring.io/spring-boot/docs/current/reference/html/' },
  { id: 3, title: 'LangChain JS/TS', desc: 'LangChain 官方文档 (JS/TS)', url: 'https://js.langchain.com/docs/' },
  { id: 4, title: 'LangChain Python', desc: 'LangChain 官方文档 (Python)', url: 'https://python.langchain.com/docs/' },
  { id: 5, title: 'Vue 3 文档', desc: 'Vue 3 官方文档', url: 'https://cn.vuejs.org/guide/introduction.html' }
]);

const hotNews = ref([]);
const today = computed(() => new Date().toLocaleDateString());

const bgImage = (url) => ({ backgroundImage: `url(${url || 'https://picsum.photos/seed/news/800/420'})` });

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return dateStr.toString().replace('T', ' ').slice(0, 16);
};

const openLink = (url) => {
  if (url) window.open(url, '_blank');
};

const fetchHotNews = async () => {
  try {
    const res = await axios.get('/api/hot-news');
    if (res.data.code === 0) {
      hotNews.value = res.data.data || [];
    }
  } catch (err) {
    console.error('获取热点资讯失败', err);
  }
};

onMounted(() => {
  fetchPosts();
  fetchCategories();
  fetchHotNews();
});
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.home-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 1.5rem;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem 0;
  align-items: start;
}

@media (max-width: 1100px) {
  .home-layout {
    grid-template-columns: 1fr;
  }
}

.left-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.hot-news-card {
  width: 100%;
  background: radial-gradient(circle at top left, #1e293b, #020617);
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.7);
  padding: 1rem;
  color: #e5e7eb;
}

.hot-news-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.4rem;
  margin-bottom: 0.6rem;
}

.hot-news-title {
  font-weight: 700;
  font-size: 1.05rem;
}

.hot-news-subtitle {
  font-size: 0.8rem;
  color: #9ca3af;
}

.hot-news-date {
  font-size: 0.8rem;
  color: #7dd3fc;
}

.hot-news-carousel {
  position: relative;
  overflow: hidden;
}

.my-swiper {
  width: 100%;
  padding-bottom: 30px;
}

.hot-news-slide {
  display: flex;
  flex-direction: column;
  border-radius: 1rem;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: #0f172a;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.hot-news-slide:hover {
  transform: translateY(-2px);
  border-color: #38bdf8;
  box-shadow: 0 16px 36px rgba(8, 47, 73, 0.55);
}

.hot-news-image {
  width: 100%;
  height: 180px;
  background-size: cover;
  background-position: center;
}

.hot-news-info {
  padding: 0.8rem 0.9rem 0.9rem;
}

.hot-news-item-title {
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.45;
  color: #e5e7eb;
}

.hot-news-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 0.35rem;
  font-size: 0.8rem;
  color: #94a3b8;
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
  padding: 0 0.2rem;
}

.toolbar {
  display: flex;
  gap: 0.6rem;
}

.center-text {
  text-align: center;
  color: #9ca3af;
}
</style>
