<template>
  <div class="page ai-recommendation-page">
    <div class="header">
      <h1>🧠 AI 智能推荐</h1>
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
            <p v-if="recommendations.explanation">{{ recommendations.explanation }}</p>
            <p v-else>根据您的查询，为您推荐以下内容。</p>
          </div>
        </div>

        <div class="recommendation-list">
          <h3>📚 相关文章</h3>
           <!-- Correctly implementing list rendering based on backend response shape -->
           <!-- The backend returns Map<String, Object> where one key is likely "related_chunks" or similar list -->
           <!-- Let's assume the response structure based on AiService/Controller -->

          <div v-if="recommendations.related_chunks && recommendations.related_chunks.length > 0" class="chunk-list">
             <div v-for="(item, index) in recommendations.related_chunks" :key="index" class="chunk-item">
                <div v-if="item.metadata" class="chunk-meta">
                   <RouterLink :to="'/posts/' + item.metadata.article_id" class="chunk-title">
                     {{ item.metadata.title || '无标题文章' }}
                   </RouterLink>
                   <span class="chunk-score" v-if="item.score">相关度: {{ (item.score * 100).toFixed(0) }}%</span>
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
import { ref } from 'vue';
import axios from 'axios';

const query = ref('');
const loading = ref(false);
const recommendations = ref(null);

const getRecommendations = async () => {
  if (!query.value.trim()) return;

  loading.value = true;
  recommendations.value = null;

  try {
    const response = await axios.post('/api/ai/recommend', {
      query: query.value
    });

    if (response.data.code === 0) {
      recommendations.value = response.data.data;
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
</script>

<style scoped>
.ai-recommendation-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.search-section {
  display: flex;
  justify-content: center;
  margin-bottom: 40px;
}

.search-box {
  display: flex;
  width: 100%;
  max-width: 600px;
  gap: 10px;
}

input {
  flex: 1;
  padding: 12px 20px;
  border: 2px solid #ddd;
  border-radius: 30px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.3s;
}

input:focus {
  border-color: #007bff;
}

.btn {
  padding: 0 25px;
  border: none;
  border-radius: 30px;
  background-color: #007bff;
  color: white;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

.btn:hover:not(:disabled) {
  background-color: #0056b3;
}

.btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.loading-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.ai-explanation-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.card-header h3 {
  margin: 0 0 16px;
  color: #0369a1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-body p {
  line-height: 1.6;
  color: #334155;
  margin: 0;
}

.recommendation-list h3 {
  margin-bottom: 20px;
  color: #333;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.chunk-item {
  background: white;
  border: 1px solid #eee;
  padding: 15px;
  border-radius: 8px;
  transition: box-shadow 0.2s;
}

.chunk-item:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.chunk-item p {
    margin: 0;
    color: #555;
    font-size: 14px;
    line-height: 1.5;
}

.chunk-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chunk-title {
  font-weight: bold;
  font-size: 16px;
  color: #007bff;
  text-decoration: none;
}

.chunk-title:hover {
  text-decoration: underline;
}

.chunk-content {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.no-results {
    text-align: center;
    color: #999;
    padding: 20px;
}
</style>
