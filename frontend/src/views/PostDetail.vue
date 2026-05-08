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
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';

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

.ai-summary-section {
  margin: 24px 0;
}

.ai-summary-box {
  background: linear-gradient(to bottom right, rgba(56, 189, 248, 0.05), rgba(56, 189, 248, 0.02));
  border: 1px solid rgba(56, 189, 248, 0.15);
  border-radius: 12px;
  padding: 24px;
  margin-top: 16px;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.ai-summary-box:hover {
  border-color: rgba(56, 189, 248, 0.3);
  box-shadow: 0 8px 30px rgba(56, 189, 248, 0.1);
}

.ai-summary-box h4 {
  margin-top: 0;
  margin-bottom: 16px;
  color: #38bdf8;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.15rem;
  letter-spacing: 0.02em;
}

.ai-summary-content {
  color: #e5e7eb;
  line-height: 1.75;
  font-size: 1rem;
  word-break: break-word;
}

/* Headings within the summary */
.ai-summary-content :deep(h1),
.ai-summary-content :deep(h2),
.ai-summary-content :deep(h3) {
  margin: 1.5em 0 0.8em;
  color: #afecefe8;
  font-weight: 700;
  line-height: 1.3;
}

.ai-summary-content :deep(p) {
  margin-bottom: 1.2em;
}

.ai-summary-content :deep(p:last-child) {
  margin-bottom: 0;
}

/* Lists Styling */
.ai-summary-content :deep(ul),
.ai-summary-content :deep(ol) {
  margin: 1.25em 0;
  padding-left: 1.5rem; /* Ensure enough space for numbers */
  list-style-position: outside; /* Keep numbers/bullets outside the text block */
}

.ai-summary-content :deep(li) {
  margin-bottom: 0.85em; /* Increased spacing for better hierarchy */
  padding-left: 0.2rem;
  line-height: 1.6;
}

/* Ordered list marker styling is handled by browser for outside position,
   but we can style the list item text */

.ai-summary-content :deep(strong) {
  color: #38bdf8; /* Highlight key terms in blue as per theme */
  font-weight: 600;
}

.ai-summary-content :deep(blockquote) {
  border-left: 4px solid #38bdf8;
  background: rgba(56, 189, 248, 0.05);
  margin: 1em 0;
  padding: 0.8em 1.2em;
  color: #bae6fd;
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
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background-color: rgba(30, 30, 30, 0.5);
  overflow: hidden; /* Prevent child overflow */
}

.thinking-header {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  cursor: pointer;
  background-color: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  user-select: none;
  transition: background-color 0.2s;
}

.thinking-header:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.thinking-icon {
  margin-right: 8px;
  font-size: 1.2em;
}

.thinking-title {
  flex-grow: 1;
  font-weight: 600;
  color: #d1d5db; /* Gray-300 */
  font-size: 0.95em;
}

.thinking-toggle {
  font-size: 0.85em;
  color: #9ca3af; /* Gray-400 */
}

.thinking-content {
  padding: 15px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
  color: #9ca3af; /* Gray-400 */
  white-space: pre-wrap;
  word-break: break-word; /* Prevent overflow */
  background-color: rgba(0, 0, 0, 0.2);
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* Custom scrollbar for thinking content */
.thinking-content::-webkit-scrollbar {
  width: 6px;
}
.thinking-content::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}
.thinking-content::-webkit-scrollbar-track {
  background: transparent;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 1.2em;
  background-color: #38bdf8;
  margin-left: 4px;
  vertical-align: middle;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
