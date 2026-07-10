<template>
  <div class="page ai-recommendation-page">
    <div class="page-header">
      <h1><span class="text">AI 智能推荐</span></h1>
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
          搜索
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
            <h3>AI 的推荐理由</h3>
          </div>
          <div class="card-body">
            <div v-if="thinkingProcess" class="thinking-box" style="margin-bottom: 1rem;">
                <details>
                    <summary>深度思考过程 (点击展开/收起)</summary>
                    <div class="thinking-content markdown-body" v-render-markdown="thinkingProcess">
                    </div>
                </details>
            </div>
            <div v-if="recommendations.explanation" class="markdown-body" v-render-markdown="explanationMain"></div>
            <p v-else>根据您的查询，为您推荐以下内容。</p>
          </div>
        </div>

        <div class="recommendation-list">
          <h3>相关文章</h3>

          <div v-if="recommendations.related_chunks && recommendations.related_chunks.length > 0" class="chunk-list">
             <div
                v-for="(item, index) in recommendations.related_chunks"
                :key="index"
                class="chunk-item"
                @click="item.metadata ? goToArticle(item.metadata.article_id) : null"
              >
                <div v-if="item.metadata" class="chunk-header">
                   <h4 class="chunk-title">{{ item.metadata.title || '无标题文章' }}</h4>
                   <div v-if="item.metadata.tags" class="chunk-tags">
                     <span v-for="tag in item.metadata.tags.split(',')" :key="tag" class="tag">{{ tag.trim() }}</span>
                   </div>
                </div>
                <p class="chunk-content">{{ item.content }}</p>
                <div class="chunk-footer">
                  <span class="read-more">阅读全文 →</span>
                </div>
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
  color: #fafafa;
}

.header {
  text-align: center;
  margin-bottom: 3rem;
  border-bottom: 4px solid #333;
  padding-bottom: 2rem;
}

.header h1 {
  font-family: 'Syne', sans-serif;
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  text-transform: uppercase;
}

.header h1 .text {
  color: #ccff00;
}

.page-header h1 .text {
  color: #ccff00;
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
}
.page-header h1 .text::before {
  content: '';
  width: 0.55em;
  height: 0.55em;
  background: #ccff00;
  transform: rotate(45deg);
  box-shadow: 0 0 12px rgba(204, 255, 0, 0.5);
}

.subtitle {
  color: #a1a1aa;
  font-family: 'JetBrains Mono', monospace;
  font-size: 1rem;
  text-transform: uppercase;
}

.search-section {
  display: flex;
  justify-content: center;
  margin-bottom: 4rem;
}

.search-box {
  display: flex;
  width: 100%;
  gap: 1rem;
  background: #09090b;
  padding: 1rem;
  border-radius: 0;
  border: 2px solid #333;
  box-shadow: 8px 8px 0 rgba(204, 255, 0, 0.2);
  transition: all 0.2s ease;
}

.search-box:focus-within {
  border-color: #ccff00;
  box-shadow: 8px 8px 0 rgba(204, 255, 0, 0.5);
  transform: translate(-4px, -4px);
}

input {
  flex: 1;
  padding: 0.8rem 1rem;
  border: none;
  background: transparent;
  color: #fafafa;
  font-family: 'Manrope', sans-serif;
  font-size: 1.1rem;
  outline: none;
}

input::placeholder {
  color: #666;
}

.btn {
  padding: 0.8rem 2.5rem;
  border: 2px solid #333;
  border-radius: 0;
  font-family: 'JetBrains Mono', monospace;
  font-weight: bold;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn.primary {
  background: #ccff00;
  color: #09090b;
  border-color: #ccff00;
}

.btn.primary:hover:not(:disabled) {
  transform: translate(-4px, -4px);
  box-shadow: 4px 4px 0 rgba(255, 255, 255, 0.2);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(100%);
}

.loading-state {
  text-align: center;
  padding: 4rem;
  color: #a1a1aa;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
}

.spinner {
  border: 4px solid #333;
  border-top: 4px solid #ccff00;
  border-radius: 0;
  width: 48px;
  height: 48px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1.5rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.results-content {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

.ai-explanation-card {
  background: #09090b;
  border-radius: 0;
  padding: 2rem;
  margin-bottom: 3rem;
  border: 2px solid #ccff00;
  box-shadow: 12px 12px 0 rgba(204, 255, 0, 0.2);
  position: relative;
}

.card-header h3 {
  margin: 0 0 1.5rem;
  color: #ccff00;
  font-family: 'Syne', sans-serif;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 1.5rem;
  text-transform: uppercase;
}

.card-body p {
  line-height: 1.8;
  color: #fafafa;
  font-family: 'Manrope', sans-serif;
  margin: 0;
  font-size: 1.1rem;
}

.recommendation-list h3 {
  margin-bottom: 2rem;
  color: #fafafa;
  font-family: 'Syne', sans-serif;
  font-weight: 800;
  font-size: 1.8rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  text-transform: uppercase;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.chunk-item {
  background: #09090b;
  border: 2px solid #333;
  padding: 2rem;
  border-radius: 0;
  transition: all 0.2s ease;
  position: relative;
  cursor: pointer;
}

.chunk-item:hover {
  border-color: #ccff00;
  transform: translate(-4px, -4px);
  box-shadow: 8px 8px 0 rgba(204, 255, 0, 0.3);
}

.chunk-header {
  margin-bottom: 1.5rem;
  border-bottom: 2px solid #333;
  padding-bottom: 1rem;
}

.chunk-item:hover .chunk-header {
  border-color: #ccff00;
}

.chunk-title {
  font-family: 'Syne', sans-serif;
  font-weight: 800;
  font-size: 1.4rem;
  color: #fafafa;
  margin: 0 0 0.75rem 0;
  line-height: 1.4;
  text-transform: uppercase;
  transition: color 0.2s;
}

.chunk-item:hover .chunk-title {
  color: #ccff00;
}

.chunk-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.chunk-tags .tag {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  color: #09090b;
  background: #ccff00;
  padding: 0.3rem 0.6rem;
  border-radius: 0;
  font-weight: bold;
  text-transform: uppercase;
}

.chunk-content {
  color: #a1a1aa;
  font-family: 'Manrope', sans-serif;
  font-size: 1.05rem;
  line-height: 1.8;
  margin: 0 0 1.5rem 0;
  white-space: pre-wrap;
}

.chunk-footer {
  display: flex;
  justify-content: flex-end;
}

.read-more {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.9rem;
  color: #ccff00;
  font-weight: bold;
  text-transform: uppercase;
  transition: transform 0.2s;
}

.chunk-item:hover .read-more {
  transform: translateX(4px);
}

.no-results {
  text-align: center;
  color: #888;
  padding: 4rem;
  background: transparent;
  border-radius: 0;
  border: 2px dashed #333;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
}
</style>
