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

      <div class="ai-summary-section">
        <button v-if="!summaryLoading && !rawSummary" @click="generateSummary" class="btn primary small">✨ AI 一键总结</button>
        <div v-if="summaryLoading || rawSummary" class="ai-summary-box">
          <h4>🤖 AI 智能总结</h4>

          <div v-if="thinkingProcess" class="thinking-process-container">
            <div class="thinking-header" @click="isThinkingExpanded = !isThinkingExpanded">
              <span class="thinking-icon">🤔</span>
              <span class="thinking-title">思考过程</span>
              <span class="thinking-toggle">{{ isThinkingExpanded ? '收起' : '展开' }}</span>
            </div>
            <div v-if="isThinkingExpanded" class="thinking-content">
              {{ thinkingProcess }}
            </div>
          </div>

          <div class="ai-summary-content" v-html="renderedSummary"></div>
          <span v-if="summaryLoading" class="cursor">|</span>
        </div>
      </div>

      <div class="post-content" v-html="renderedContent"></div>

      <!-- Like Button -->
      <div class="post-interactions" v-if="post">
        <LikeButton :postId="post.id" />
      </div>

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

    <!-- Comment Section -->
    <CommentSection v-if="post" :postId="post.id" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';
import LikeButton from '../components/LikeButton.vue';
import CommentSection from '../components/CommentSection.vue';

const route = useRoute();
const post = ref(null);
const loading = ref(false);
const summaryLoading = ref(false);
const rawSummary = ref('');
const isThinkingExpanded = ref(false);

const thinkingProcess = computed(() => {
  const content = rawSummary.value;
  const match = content.match(/<think>([\s\S]*?)(?:<\/think>|$)/);
  return match ? match[1].trim() : '';
});

const cleanSummary = computed(() => {
  const content = rawSummary.value;
  const thinkEnd = content.indexOf('</think>');
  if (thinkEnd !== -1) {
    return content.substring(thinkEnd + 8).trim();
  }
  // If we are still streaming thinking process (no closing tag yet)
  if (content.startsWith('<think>')) {
      return '';
  }
  return content;
});

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

const renderedSummary = computed(() => {
  if (!cleanSummary.value) return '';
  let content = cleanSummary.value;

  // 1. 移除 "(空行)" 替换为换行，避免文字粘连
  content = content.replace(/\(空行\)/g, '\n');

  // 2. 去除常见的模板包裹 prefix (保留内容)
  // Remove the closing bracket of the insight block if present
  content = content.replace(/]\s*(\n|$)/, '$1');

  // 3. 统一换行符
  content = content.replace(/\r\n/g, '\n');

  // 4. 修复 Markdown 列表格式 (Aggressive fixes)

  // Fix: 强制文本后跟随的 - ** 换行 (处理 "text - **Title**" 情况)
  content = content.replace(/([^\n])\s*[-*]\s*(\*\*)/g, '$1\n\n- $2');

  // Fix: 标点符号后跟随的 - Item 强制换行
  content = content.replace(/([。！？.!?])\s*[-*]\s+(?=[^\s])/g, '$1\n\n- ');

  // Fix: 确保列表符号后有空格 (-Item -> - Item) - Markdown 必须
  content = content.replace(/(^|\n)\s*([-*])(?=[^\s])/g, '$1$2 ');

  // Fix: 确保列表项前有空行 (text\n- Item -> text\n\n- Item)
  content = content.replace(/([^\n])\n\s*([-*]\s)/g, '$1\n\n$2');

  // 5. 移除分割线
  content = content.replace(/^\s*([-*_])\1{2,}\s*$/gm, '');

  content = content.trim();

  const rawHtml = marked.parse(content);
  return DOMPurify.sanitize(rawHtml, { ADD_ATTR: ['class'] });
});

const attachments = computed(() => {
  if (!post.value || !post.value.content) return [];
  const content = post.value.content;
  const regex = /\[附件：([^\]]+)]\(([^)]+)\)/g;
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
  const rawHtml = marked.parse(raw);
  return DOMPurify.sanitize(rawHtml, { ADD_ATTR: ['class'] });
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

const generateSummary = () => {
  if (!post.value || !post.value.id) return;

  summaryLoading.value = true;
  rawSummary.value = '';
  isThinkingExpanded.value = true; // Auto-expand when thinking starts

  const eventSource = new EventSource(`/api/ai/summary/${post.value.id}`);

  eventSource.onmessage = (event) => {
    // Some SSE implementations wrap the data in quotes or escape it
    let data = event.data;
    try {
      // If server sends JSON string, parse it. In this case, it seems to be raw text chunks.
      if (data.startsWith('"') && data.endsWith('"')) {
        data = JSON.parse(data);
      }
    } catch (e) {}

    rawSummary.value += data;
  };

  eventSource.onerror = (err) => {
    console.error('EventSource failed:', err);
    eventSource.close();
    summaryLoading.value = false;
  };

  // You might want to listen for a specific event or close on some condition
  // For now assuming server closes connection or we can close after some time/token
  // But typically standard SSE keeps connection open until server closes.
  // The server implementation uses SseEmitter and calls complete().
  // Browsers automatically reconnect on error/close unless explicitly closed.
  // To prevent infinite reconnection loop if server closes normally:
  eventSource.addEventListener('error', (e) => {
      if (e.eventPhase === EventSource.CLOSED) {
        eventSource.close();
        summaryLoading.value = false;
      }
  }, false);

};

onMounted(fetchDetail);
</script>

<style scoped>
.page {
  padding: 0;
  max-width: 1000px;
  margin: 0 auto;
}

.back-btn {
  margin-bottom: 2rem;
  padding: 0.8rem 1.5rem;
  border-radius: 0;
  border: 2px solid #333;
  background: transparent;
  color: #fafafa;
  cursor: pointer;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
  font-weight: bold;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: #ccff00;
  color: #09090b;
  border-color: #ccff00;
  transform: translate(-4px, -4px);
  box-shadow: 4px 4px 0 rgba(255,255,255,0.2);
}

.center-text {
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
  color: #a1a1aa;
}

.card {
  min-height: 60vh;
  border: 2px solid #333;
  border-radius: 0;
  background: #09090b;
  padding: 3rem;
  box-shadow: 12px 12px 0 rgba(204, 255, 0, 0.1);
}

.card-title {
  font-family: 'Syne', sans-serif;
  font-size: 2.5rem;
  font-weight: 800;
  margin: 0 0 1.5rem;
  color: #fafafa;
  text-transform: uppercase;
  border-bottom: 4px solid #333;
  padding-bottom: 1rem;
}

.card-meta {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
  margin: 0 0 1.5rem;
  color: #888;
  text-transform: uppercase;
}

.card-summary {
  font-family: 'Manrope', sans-serif;
  font-size: 1.1rem;
  margin: 0 0 2.5rem;
  color: #ccff00;
  border-left: 4px solid #ccff00;
  padding-left: 1rem;
}

.post-content {
  font-family: 'Manrope', sans-serif;
  font-size: 1.1rem;
  line-height: 1.8;
  color: #d4d4d8;
}

.attachments-section {
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 2px dashed #333;
}

.attachment-item {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 2px solid #333;
  background: #000;
}

.file-name {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.9rem;
  color: #fafafa;
}

.file-actions {
  display: flex;
  gap: 10px;
}

.btn {
  background-color: transparent;
  border: 2px solid #333;
  border-radius: 0;
  color: #fafafa;
  cursor: pointer;
  padding: 0.6rem 1.2rem;
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
  font-weight: bold;
  transition: all 0.2s ease;
}

.btn:hover {
  background: #ccff00;
  color: #09090b;
  border-color: #ccff00;
  transform: translate(-2px, -2px);
  box-shadow: 2px 2px 0 rgba(255,255,255,0.2);
}

.btn.ghost {
  border-style: dashed;
}

.btn.small {
  font-size: 0.8rem;
  padding: 0.4rem 0.8rem;
}

.post-interactions {
  margin-top: 2.5rem;
  padding-top: 1.5rem;
  border-top: 2px solid #333;
}

.ai-summary-section {
  margin: 3rem 0;
}

.ai-summary-box {
  background: #09090b;
  border: 4px solid #ccff00;
  border-radius: 0;
  padding: 2.5rem;
  margin-top: 1rem;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: 12px 12px 0 rgba(204, 255, 0, 0.2);
}

.ai-summary-box:hover {
  transform: translate(-4px, -4px);
  box-shadow: 16px 16px 0 rgba(204, 255, 0, 0.4);
}

.ai-summary-box h4 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  color: #ccff00;
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 1.5rem;
  font-family: 'Syne', sans-serif;
  font-weight: 800;
  text-transform: uppercase;
}

.ai-summary-content {
  color: #fafafa;
  line-height: 1.8;
  font-family: 'Manrope', sans-serif;
  font-size: 1.1rem;
  word-break: break-word;
}

/* Headings within the summary */
.ai-summary-content :deep(h1),
.ai-summary-content :deep(h2),
.ai-summary-content :deep(h3) {
  margin: 1.5em 0 0.8em;
  color: #fafafa;
  font-family: 'Syne', sans-serif;
  font-weight: 800;
  line-height: 1.3;
}

.ai-summary-content :deep(p) { margin-bottom: 1.2em; }
.ai-summary-content :deep(p:last-child) { margin-bottom: 0; }

/* Lists Styling */
.ai-summary-content :deep(ul),
.ai-summary-content :deep(ol) {
  margin: 1.25em 0;
  padding-left: 2rem;
  list-style-position: outside;
}

.ai-summary-content :deep(li) {
  margin-bottom: 0.85em;
  line-height: 1.6;
}

.ai-summary-content :deep(strong) {
  color: #ccff00;
  font-weight: 800;
  background: rgba(204, 255, 0, 0.1);
  padding: 0 4px;
}

.ai-summary-content :deep(blockquote) {
  border-left: 4px solid #ccff00;
  background: #000;
  margin: 1em 0;
  padding: 1rem 1.5rem;
  color: #a1a1aa;
}

.ai-summary-content :deep(li > p) {
  margin-bottom: 0;
  display: inline;
}

.ai-summary-content :deep(p + ul),
.ai-summary-content :deep(p + ol) {
  margin-top: -4px;
}

.thinking-process-container {
  margin-bottom: 2rem;
  border: 2px solid #333;
  border-radius: 0;
  background-color: transparent;
  overflow: hidden;
}

.thinking-header {
  display: flex;
  align-items: center;
  padding: 1rem 1.5rem;
  cursor: pointer;
  background-color: #000;
  border-bottom: 2px solid #333;
  user-select: none;
  transition: background-color 0.2s;
}

.thinking-header:hover {
  background-color: #333;
}

.thinking-icon {
  margin-right: 1rem;
  font-size: 1.2rem;
}

.thinking-title {
  flex-grow: 1;
  font-family: 'JetBrains Mono', monospace;
  font-weight: bold;
  color: #a1a1aa;
  font-size: 0.95rem;
  text-transform: uppercase;
}

.thinking-toggle {
  font-size: 0.85em;
  color: #666;
}

.thinking-content {
  padding: 1.5rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.9rem;
  color: #888;
  white-space: pre-wrap;
  word-break: break-word;
  background-color: #000;
  line-height: 1.7;
  max-height: 400px;
  overflow-y: auto;
}

.thinking-content::-webkit-scrollbar { width: 8px; }
.thinking-content::-webkit-scrollbar-thumb { background-color: #333; border-radius: 0; }
.thinking-content::-webkit-scrollbar-track { background: transparent; border-left: 1px solid #333; }

.cursor {
  display: inline-block;
  width: 4px;
  height: 1.2em;
  background-color: #ccff00;
  margin-left: 4px;
  vertical-align: middle;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
