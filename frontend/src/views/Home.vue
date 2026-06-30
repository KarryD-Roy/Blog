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
            <button v-if="isAuthenticated" class="btn primary" @click="openCreate">
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
            placeholder="标签，支持中/英文逗号分隔，例如：Vue，后端，随笔"
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
          <div
            class="custom-select-container"
            @mouseenter="showSkillDropdown = true"
            @mouseleave="showSkillDropdown = false"
          >
            <div class="input select-trigger">
              <span>{{ selectedSkillsText }}</span>
            </div>
            <transition name="fade">
              <div v-if="showSkillDropdown" class="select-dropdown" style="max-height: 200px; overflow-y: auto;">
                <div
                  v-for="s in skills"
                  :key="s.id"
                  class="select-option"
                  :class="{ selected: form.skillIds.includes(s.id) }"
                  @click="toggleSkill(s.id)"
                >
                  <label style="cursor: pointer;">
                    <input type="checkbox" :checked="form.skillIds.includes(s.id)" @click.stop="toggleSkill(s.id)" style="margin-right: 0.5rem;" />
                    {{ s.title }}
                  </label>
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
import { computed, onMounted, onActivated, ref, reactive, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';
import { Swiper, SwiperSlide } from 'swiper/vue';
import { Pagination, Autoplay } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/pagination';
import clawhubIcon from '../static/favicon.ico';
import { useAuth } from '../stores/auth.js';

const modules = [Pagination, Autoplay];

defineOptions({
  name: 'Home'
});

const router = useRouter();
const route = useRoute();
const { isAuthenticated } = useAuth();
const posts = ref([]);
const categories = ref([]);
const skills = ref([]);
const loading = ref(false);
const page = ref(route.query.page ? parseInt(route.query.page) : 1);
const totalPages = ref(0);

const showEditor = ref(false);
const showCategoryDropdown = ref(false); // 控制自定义下拉菜单显示
const showSkillDropdown = ref(false); // 控制技能多选下拉菜单显示
const editingPost = ref(null);
const form = reactive({
  title: '',
  summary: '',
  content: '',
  tags: '',
  categoryId: null,
  skillIds: []
});

const fileInput = ref(null);
const uploadType = ref('image');

const renderer = new marked.Renderer();
renderer.code = (code, infostring) => {
  const lang = (infostring || '').match(/\S+/)?.[0];
  let highlighted = '';

  if (lang && hljs.getLanguage(lang)) {
    highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value;
  } else {
    highlighted = hljs.highlightAuto(code).value;
  }

  const langClass = lang ? `language-${lang}` : 'language-plaintext';
  return `<pre class="hljs"><code class="${langClass}">${highlighted}</code></pre>`;
};

marked.setOptions({
  gfm: true,
  breaks: true,
  renderer
});

const previewHtml = computed(() => {
  if (!form.content) return '';
  const raw = form.content.trim();
  if (raw.startsWith('<')) {
    return raw;
  }
  const rawHtml = marked.parse(raw);
  return DOMPurify.sanitize(rawHtml, { ADD_ATTR: ['class'] });
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

const fetchSkills = async () => {
  try {
    const res = await axios.get('/api/skills');
    if (res.data.code === 0) {
      skills.value = res.data.data;
    }
  } catch (err) {
    console.error('获取技能失败', err);
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

watch(
  () => route.name,
  (newName) => {
    if (newName === 'home') {
      fetchPosts();
      checkDraftContent();
    }
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
  form.skillIds = [];
};

const startEdit = (post) => {
  editingPost.value = { ...post };
  form.title = post.title || '';
  form.summary = post.summary || '';
  form.content = post.content || '';
  form.tags = post.tags || '';
  form.categoryId = post.categoryId || null;
  form.skillIds = post.skillIds || [];
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

// 当前选中技能展示文本
const selectedSkillsText = computed(() => {
  if (!form.skillIds || form.skillIds.length === 0) return '关联特定技能点（可选）';
  const selectedNames = form.skillIds
    .map(id => {
      const s = skills.value.find(sk => sk.id === id);
      return s ? s.title : null;
    })
    .filter(Boolean);
  return selectedNames.length > 0 ? selectedNames.join(', ') : '关联特定技能点（可选）';
});

// 切换技能勾选
const toggleSkill = (id) => {
  if (!form.skillIds) {
    form.skillIds = [];
  }
  const idx = form.skillIds.indexOf(id);
  if (idx > -1) {
    form.skillIds.splice(idx, 1);
  } else {
    form.skillIds.push(id);
  }
};

const submitPost = async () => {
  if (!form.title.trim()) {
    return;
  }
  const payload = {
    title: form.title,
    summary: form.summary,
    content: form.content,
    tags: form.tags ? form.tags.replace(/，/g, ',') : '',
    categoryId: form.categoryId,
    skillIds: form.skillIds
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
    title: 'ClawHub',
    desc: '技术资源与知识分享平台',
    url: 'https://clawhub.ai/',
    icon: clawhubIcon
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

const checkDraftContent = () => {
  const draft = sessionStorage.getItem('ai_draft_content');
  if (draft) {
    sessionStorage.removeItem('ai_draft_content');
    openCreate();
    form.content = draft;
  }
};

onMounted(() => {
  fetchPosts();
  fetchCategories();
  fetchSkills();
  fetchHotNews();
  checkDraftContent();
});

onActivated(() => {
  fetchPosts();
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
  gap: 2rem;
  width: 100%;
  max-width: 1400px;
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
  gap: 2rem;
}

.hot-news-card {
  width: 100%;
  background: #09090b;
  border-radius: 0;
  border: 2px solid #333;
  padding: 2rem;
  color: #fafafa;
  box-shadow: 12px 12px 0 rgba(204, 255, 0, 0.2);
}

.hot-news-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
  border-bottom: 2px solid #333;
  padding-bottom: 1rem;
}

.hot-news-title {
  font-family: 'Syne', sans-serif;
  font-weight: 800;
  font-size: 1.25rem;
  text-transform: uppercase;
}

.hot-news-subtitle {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  color: #888;
  margin-top: 0.5rem;
  text-transform: uppercase;
}

.hot-news-date {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  color: #ccff00;
  font-weight: bold;
}

.hot-news-carousel {
  position: relative;
  overflow: hidden;
  margin-top: 1rem;
}

.my-swiper {
  width: 100%;
  padding-bottom: 40px;
}

.hot-news-slide {
  display: flex;
  flex-direction: column;
  border-radius: 0;
  overflow: hidden;
  border: 2px solid #333;
  background: #000;
  transition: all 0.2s ease;
}

.hot-news-slide:hover {
  transform: translate(-4px, -4px);
  border-color: #ccff00;
  box-shadow: 8px 8px 0 rgba(204, 255, 0, 0.4);
}

.hot-news-image {
  width: 100%;
  height: 200px;
  background-size: cover;
  background-position: center;
  filter: grayscale(100%) contrast(1.2);
  transition: filter 0.3s ease;
}

.hot-news-slide:hover .hot-news-image {
  filter: grayscale(0%) contrast(1.1);
}

.hot-news-info {
  padding: 1.5rem;
}

.hot-news-item-title {
  font-family: 'Syne', sans-serif;
  font-size: 1.1rem;
  font-weight: 800;
  line-height: 1.45;
  color: #fafafa;
}

.hot-news-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 1rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  color: #888;
  text-transform: uppercase;
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding: 0 0.5rem;
  border-bottom: 4px solid #333;
  padding-bottom: 1rem;
}

.toolbar {
  display: flex;
  gap: 1rem;
}

.center-text {
  text-align: center;
  color: #a1a1aa;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
}
</style>
