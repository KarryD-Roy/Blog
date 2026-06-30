<template>
  <div class="page profile-page">
    <h1 class="page-title">个人中心</h1>

    <div v-if="loading" class="center-text">加载中...</div>
    <div v-else-if="profile" class="profile-layout">
      <!-- Profile Card -->
      <div class="profile-card card">
        <div class="avatar-placeholder">{{ displayInitial }}</div>
        <h2 class="profile-name">{{ profile.nickname || profile.username }}</h2>
        <p class="profile-username">@{{ profile.username }}</p>
        <div class="profile-roles">
          <span v-for="role in profile.roles" :key="role" class="role-badge">{{ role }}</span>
        </div>
        <div class="profile-info">
          <div class="info-row" v-if="profile.email">
            <span class="info-label">邮箱</span>
            <span class="info-value">{{ profile.email }}</span>
          </div>
          <div class="info-row" v-if="profile.bio">
            <span class="info-label">简介</span>
            <span class="info-value">{{ profile.bio }}</span>
          </div>
          <div class="info-row" v-if="profile.createdAt">
            <span class="info-label">注册时间</span>
            <span class="info-value">{{ formatDate(profile.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="profile-content">
        <div class="tab-nav">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'posts' }"
            @click="activeTab = 'posts'"
          >我的文章</button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'comments' }"
            @click="activeTab = 'comments'"
          >评论历史</button>
        </div>

        <div class="tab-content">
          <div v-if="activeTab === 'posts'" class="posts-section">
            <div v-if="myPosts.length === 0" class="empty-state">暂无文章</div>
            <div v-for="post in myPosts" :key="post.id" class="post-item" @click="goDetail(post.id)">
              <h3>{{ post.title }}</h3>
              <p class="post-meta">{{ post.createdAt }} · {{ post.viewCount }} 浏览</p>
              <p class="post-summary">{{ post.summary }}</p>
            </div>
          </div>

          <div v-if="activeTab === 'comments'" class="comments-section">
            <div v-if="myComments.length === 0" class="empty-state">暂无评论</div>
            <div v-for="comment in myComments" :key="comment.id" class="comment-item">
              <p class="comment-content">{{ comment.content }}</p>
              <p class="comment-meta">评论于文章 #{{ comment.postId }} · {{ formatDate(comment.createdAt) }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="center-text">请先登录</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '../../stores/auth.js';
import { getProfile } from '../../api/auth.js';
import { listPosts } from '../../api/posts.js';

const router = useRouter();
const { isAuthenticated } = useAuth();

const profile = ref(null);
const myPosts = ref([]);
const myComments = ref([]);
const loading = ref(true);
const activeTab = ref('posts');

const displayInitial = computed(() => {
  const name = profile.value?.nickname || profile.value?.username || 'U';
  return name.charAt(0).toUpperCase();
});

const formatDate = (d) => {
  if (!d) return '';
  const s = String(d).replace('T', ' ').slice(0, 16);
  return s;
};

const fetchProfile = async () => {
  loading.value = true;
  try {
    const res = await getProfile();
    if (res.data.code === 0) {
      profile.value = res.data.data;
      await loadMyPosts();
    }
  } catch { /* ignore */ }
  finally { loading.value = false; }
};

const loadMyPosts = async () => {
  try {
    const res = await listPosts(1, 50);
    if (res.data.code === 0) {
      const all = res.data.data.records || [];
      myPosts.value = all.filter(p => p.userId === profile.value?.id);
    }
  } catch { /* ignore */ }
};

const goDetail = (id) => router.push({ name: 'post-detail', params: { id } });

onMounted(() => {
  if (isAuthenticated.value) fetchProfile();
});
</script>

<style scoped>
.profile-page { max-width: 1000px; margin: 0 auto; padding: 0 1rem; }

.page-title {
  font-family: 'Syne', sans-serif;
  font-size: 2rem;
  font-weight: 800;
  color: #fafafa;
  text-transform: uppercase;
  margin-bottom: 2rem;
  border-bottom: 4px solid #333;
  padding-bottom: 1rem;
}

.profile-layout { display: grid; grid-template-columns: 320px 1fr; gap: 2rem; align-items: start; }

.profile-card {
  background: #09090b;
  border: 2px solid #333;
  padding: 2.5rem 2rem;
  text-align: center;
  box-shadow: 12px 12px 0 rgba(204, 255, 0, 0.1);
}

.avatar-placeholder {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: #ccff00;
  color: #09090b;
  font-family: 'Syne', sans-serif;
  font-size: 2rem;
  font-weight: 800;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 1rem;
}

.profile-name {
  font-family: 'Syne', sans-serif;
  font-size: 1.5rem;
  font-weight: 800;
  color: #fafafa;
  margin: 0 0 0.25rem;
}

.profile-username {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  color: #888;
  margin: 0 0 1rem;
}

.profile-roles { display: flex; gap: 0.5rem; justify-content: center; margin-bottom: 1.5rem; }

.role-badge {
  background: rgba(204, 255, 0, 0.15);
  border: 1px solid #ccff00;
  color: #ccff00;
  padding: 0.25rem 0.75rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  text-transform: uppercase;
}

.profile-info { text-align: left; border-top: 2px solid #333; padding-top: 1.5rem; }

.info-row { display: flex; justify-content: space-between; margin-bottom: 0.75rem; }

.info-label {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  color: #888;
  text-transform: uppercase;
}

.info-value {
  font-family: 'Manrope', sans-serif;
  font-size: 0.9rem;
  color: #d4d4d8;
}

.profile-content { min-height: 400px; }

.tab-nav {
  display: flex;
  gap: 0;
  border-bottom: 2px solid #333;
  margin-bottom: 2rem;
}

.tab-btn {
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  color: #888;
  padding: 1rem 2rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover { color: #fafafa; }

.tab-btn.active {
  color: #ccff00;
  border-bottom-color: #ccff00;
}

.empty-state {
  text-align: center;
  color: #555;
  font-family: 'JetBrains Mono', monospace;
  padding: 3rem 0;
}

.post-item {
  background: #09090b;
  border: 2px solid #333;
  padding: 1.5rem;
  margin-bottom: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.post-item:hover { border-color: #ccff00; transform: translate(-2px, -2px); }

.post-item h3 {
  font-family: 'Syne', sans-serif;
  font-size: 1.1rem;
  color: #fafafa;
  margin: 0 0 0.5rem;
}

.post-meta {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  color: #888;
  margin: 0 0 0.5rem;
}

.post-summary {
  font-family: 'Manrope', sans-serif;
  font-size: 0.9rem;
  color: #a1a1aa;
  margin: 0;
}

.comment-item {
  background: #09090b;
  border: 2px solid #333;
  padding: 1.25rem;
  margin-bottom: 0.75rem;
}

.comment-content {
  font-family: 'Manrope', sans-serif;
  font-size: 0.95rem;
  color: #d4d4d8;
  margin: 0 0 0.5rem;
}

.comment-meta {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  color: #555;
  margin: 0;
}

.center-text {
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
  color: #a1a1aa;
  text-transform: uppercase;
}

@media (max-width: 768px) {
  .profile-layout { grid-template-columns: 1fr; }
}
</style>
