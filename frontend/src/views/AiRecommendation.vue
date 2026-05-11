<template>
  <div class="page ai-recommendation-page">
    <div class="page-header">
      <h1><span class="emoji">🧠</span> <span class="text">AI 智能推荐</span></h1>
      <p class="subtitle">告诉我你想了解什么，AI 为你精准匹配并解读</p>
    </div>

    <div class="search-section">
      <div class="search-box">
        <input
          v-model="query"
          @keyup.enter="getRecommendations"
          type="text"
          placeholder="例如：'如何使用 Spring Security 实现 OAuth2？' 或 '推荐几篇关于微服务的文章'"
        />
        <button class="btn primary" @click="getRecommendations" :disabled="loading || !query.trim()">
          🔍 搜索
        </button>
      </div>
    </div>

    <div class="results-section" v-if="loading || recommendations">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>AI 正在检索并生成推荐理由...</p>
      </div>

      <div v-else-if="recommendations" class="results-content">
        <div class="ai-explanation-card">
          <div class="card-header">
            <h3>🤖 AI 的推荐理由</h3>
          </div>
          <div class="card-body">
            <div v-if="thinkingProcess" class="thinking-box" style="margin-bottom: 1rem;">
                <details>
                    <summary>🤔 深度思考过程 (点击展开/收起)</summary>
                    <div class="thinking-content markdown-body" v-render-markdown="thinkingProcess">
                    </div>
                </details>
            </div>
            <div v-if="recommendations.explanation" class="markdown-body" v-render-markdown="explanationMain"></div>
            <p v-else>根据您的查询，为您推荐以下内容。</p>
          </div>
        </div>

        <div class="recommendation-list">
          <h3>📚 相关文章</h3>
           <!-- Correctly implementing list rendering based on backend response shape -->
           <!-- The backend returns Map<String, Object> where one key is likely "related_chunks" or similar list -->
           <!-- Let's assume the response structure based on AiService/Controller -->

          <div v-if="recommendations.related_chunks && recommendations.related_chunks.length > 0" class="chunk-list">
             <div v-for="(item, index) in recommendations.related_chunks" :key="index" class="chunk-item" @click="item.metadata ? goToArticle(item.metadata.article_id) : null" style="cursor: pointer;">
                <div v-if="item.metadata" class="chunk-meta">
                   <span class="chunk-title">
                     {{ item.metadata.title || '无标题文章' }}
                   </span>
                   <span class="chunk-score" v-if="item.score !== undefined">距离/差异值: {{ item.score.toFixed(3) }}</span>
                </div>
                <p class="chunk-content">{{ item.content }}...</p>
             </div>
          </div>
          <div v-else class="no-results">
            <p>暂无直接匹配的文章片段。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import vRenderMarkdown from '../directives/vRenderMarkdown';

const router = useRouter();
const query = ref('');
const loading = ref(false);
const recommendations = ref(null);
const thinkingProcess = ref('');
const explanationMain = ref('');

// 提取思考过程和正��
const extractThinkAndMain = (text) => {
  if (!text) return { think: '', main: '' };

  const thinkRegex = /<think>(.*?)<\/think>/gis;
  const encodedThinkRegex = /&lt;think&gt;(.*?)&lt;\/think&gt;/gis;

  let think = '';
  let main = text;

  let match = thinkRegex.exec(text) || encodedThinkRegex.exec(text);

  if (match) {
    think = match[1];
    main = text.replace(match[0], '');
    return { think, main };
  }

  const openRegex = /(?:<think>|&lt;think&gt;)/i;
  const openMatch = openRegex.exec(text);

  if (openMatch) {
    const openIndex = openMatch.index;
    const openLength = openMatch[0].length;
    think = text.slice(openIndex + openLength);
    main = text.slice(0, openIndex);
  }

  return { think, main };
};

const getRecommendations = async () => {
  if (!query.value.trim()) return;

  loading.value = true;
  recommendations.value = null;

  try {
    const response = await axios.post('/api/ai/recommend', {
      query: query.value
    });

    if (response.data.code === 0) {
      // Extract recommendations from standard API structure
      const data = response.data.data.recommendations || response.data.data;

      // 去重：同一篇文章推荐多次
      if (data.related_chunks && data.related_chunks.length > 0) {
        const uniqueChunks = [];
        const seenIds = new Set();
        for (const item of data.related_chunks) {
            if (item.metadata && item.metadata.article_id) {
                if (!seenIds.has(item.metadata.article_id)) {
                    seenIds.add(item.metadata.article_id);
                    uniqueChunks.push(item);
                }
            } else {
                uniqueChunks.push(item);
            }
        }
        data.related_chunks = uniqueChunks;
      }
      recommendations.value = data;

      if (recommendations.value && recommendations.value.explanation) {
        const parts = extractThinkAndMain(recommendations.value.explanation);
        thinkingProcess.value = parts.think;
        explanationMain.value = parts.main;
      } else {
        thinkingProcess.value = '';
        explanationMain.value = '';
      }

      // 缓存搜索状态
      sessionStorage.setItem('ai_recommendations_state', JSON.stringify({
        query: query.value,
        recommendations: recommendations.value,
        thinkingProcess: thinkingProcess.value,
        explanationMain: explanationMain.value
      }));

    } else {
      alert('推荐失败: ' + response.data.message);
    }
  } catch (err) {
    console.error(err);
    alert('请求出错，请重试');
  } finally {
    loading.value = false;
  }
};

const goToArticle = (articleId) => {
  if (articleId) {
    router.push('/posts/' + articleId);
  }
};

// 恢复搜索状态
onMounted(() => {
  const savedState = sessionStorage.getItem('ai_recommendations_state');
  if (savedState) {
    try {
      const state = JSON.parse(savedState);
      query.value = state.query || '';
      recommendations.value = state.recommendations || null;
      thinkingProcess.value = state.thinkingProcess || '';
      explanationMain.value = state.explanationMain || '';
    } catch(e) {
      console.error(e);
    }
  }
});

</script>

<style scoped>
.ai-recommendation-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem;
  color: #e5e7eb;
}

.header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.header h1 {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.header h1 .text {
  background: linear-gradient(90deg, #e0f2fe, #38bdf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: #94a3b8;
  font-size: 1.1rem;
}

.search-section {
  display: flex;
  justify-content: center;
  margin-bottom: 3rem;
}

.search-box {
  display: flex;
  width: 100%;
  gap: 1rem;
  background: rgba(15, 23, 42, 0.6);
  padding: 0.5rem;
  border-radius: 100px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.3);
}

input {
  flex: 1;
  padding: 0.8rem 1.5rem;
  border: none;
  background: transparent;
  color: #e5e7eb;
  font-size: 1rem;
  outline: none;
}

input::placeholder {
  color: #64748b;
}

.btn {
  padding: 0.8rem 2rem;
  border: none;
  border-radius: 100px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn.primary {
  background: linear-gradient(90deg, #0ea5e9, #22d3ee);
  color: #0f172a;
}

.btn.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.4);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-state {
  text-align: center;
  padding: 4rem;
  color: #94a3b8;
}

.spinner {
  border: 4px solid rgba(148, 163, 184, 0.2);
  border-top: 4px solid #38bdf8;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1.5rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.results-content {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-explanation-card {
  background: radial-gradient(circle at top left, #1e293b, #020617);
  border-radius: 1rem;
  padding: 1.8rem;
  margin-bottom: 2.5rem;
  border: 1px solid rgba(148, 163, 184, 0.3);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.8);
  position: relative;
  overflow: hidden;
}

.ai-explanation-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #38bdf8, #818cf8);
}

.card-header h3 {
  margin: 0 0 1rem;
  color: #e0f2fe;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.3rem;
}

.card-body p {
  line-height: 1.7;
  color: #cbd5e1;
  margin: 0;
  font-size: 1.05rem;
}

.recommendation-list h3 {
  margin-bottom: 1.5rem;
  color: #f8fafc;
  font-size: 1.4rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.chunk-item {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.2);
  padding: 1.5rem;
  border-radius: 1rem;
  transition: all 0.2s ease;
}

.chunk-item:hover {
  border-color: #38bdf8;
  background: rgba(15, 23, 42, 0.8);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  transform: translateY(-2px);
}

.chunk-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.chunk-title {
  font-weight: 600;
  font-size: 1.2rem;
  color: #38bdf8;
  text-decoration: none;
  transition: color 0.2s;
}

.chunk-title:hover {
  color: #7dd3fc;
}

.chunk-score {
  font-size: 0.85rem;
  color: #fbbf24;
  background: rgba(245, 158, 11, 0.15);
  padding: 0.2rem 0.6rem;
  border-radius: 100px;
  font-weight: 600;
}

.chunk-content {
  color: #94a3b8;
  font-size: 0.95rem;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.no-results {
  text-align: center;
  color: #64748b;
  padding: 3rem;
  background: rgba(15, 23, 42, 0.4);
  border-radius: 1rem;
  border: 1px dashed rgba(148, 163, 184, 0.3);
}
</style>
