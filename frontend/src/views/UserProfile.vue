<template>
  <div class="page user-profile-page">
    <div v-if="loading" class="center-text">加载中...</div>
    <div v-else-if="profile" class="profile-layout">
      <!-- User Header Card -->
      <div class="profile-card card">
        <div class="avatar-wrapper">
          <img v-if="profile.avatar" :src="profile.avatar" class="avatar-img" alt="头像" />
          <div v-else class="avatar-placeholder">{{ displayInitial }}</div>
        </div>
        <h2 class="profile-name">{{ profile.nickname || profile.username }}</h2>
        <p class="profile-username">@{{ profile.username }}</p>
        <div class="profile-roles" v-if="profile.roles?.length">
          <span v-for="role in profile.roles" :key="role" :class="['role-badge', role === 'ADMIN' ? 'admin-badge' : '']">{{ role }}</span>
        </div>
        <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>
        <div class="info-row">
          <span class="info-label">加入时间</span>
          <span class="info-value">{{ formatDate(profile.createdAt) }}</span>
        </div>
      </div>

      <!-- Posts -->
      <div class="profile-content">
        <h2 class="section-title">TA 的文章</h2>
        <div v-if="postsLoading" class="center-text">加载中...</div>
        <div v-else-if="posts.length === 0" class="empty-state">该用户暂无已发布文章</div>
        <template v-else>
          <div v-for="post in posts" :key="post.id" class="post-item" @click="goDetail(post.id)">
            <div class="post-header-row">
              <h3>{{ post.title }}</h3>
              <span :class="'status-tag status-' + post.status">{{ statusLabel(post.status) }}</span>
            </div>
            <p class="post-meta">{{ formatDate(post.createdAt) }} · {{ post.viewCount ?? 0 }} 浏览</p>
            <p class="post-summary">{{ post.summary || '(无摘要)' }}</p>
          </div>
          <div class="pagination" v-if="totalPages > 1">
            <button :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
            <span>第 {{ page }} / {{ totalPages }} 页</span>
            <button :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
          </div>
        </template>
      </div>
    </div>
    <div v-else class="center-text">用户不存在</div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getUserById } from '../api/user.js';
import { getUserPosts } from '../api/posts.js';

const route = useRoute();
const router = useRouter();

const profile = ref(null);
const loading = ref(true);
const posts = ref([]);
const postsLoading = ref(false);
const page = ref(1);
const totalPages = ref(0);
const pageSize = 8;

const displayInitial = () => {
  const name = profile.value?.nickname || profile.value?.username || 'U';
  return name.charAt(0).toUpperCase();
};

const formatDate = (d) => {
  if (!d) return '';
  return String(d).replace('T', ' ').slice(0, 16);
};

const statusLabel = (status) => {
  const map = { PUBLISHED: '已发布', PENDING: '待审核', REJECTED: '已驳回' };
  return map[status] || status;
};

const goDetail = (id) => router.push({ name: 'post-detail', params: { id } });

const fetchProfile = async () => {
  loading.value = true;
  profile.value = null;
  try {
    const res = await getUserById(route.params.id);
    if (res.data.code === 0) {
      profile.value = res.data.data;
      page.value = 1;
      await loadPosts();
    }
  } catch { /* ignore */ }
  finally { loading.value = false; }
};

const loadPosts = async () => {
  postsLoading.value = true;
  try {
    const res = await getUserPosts(route.params.id, page.value, pageSize);
    if (res.data.code === 0) {
      posts.value = res.data.data.records || [];
      totalPages.value = res.data.data.pages || 0;
    }
  } catch { /* ignore */ }
  finally { postsLoading.value = false; }
};

const changePage = (p) => {
  page.value = p;
  loadPosts();
};

onMounted(fetchProfile);
watch(() => route.params.id, fetchProfile);
</script>

<style scoped>
.user-profile-page { max-width: 1200px; margin: 0 auto; padding: 0 1rem; }

.profile-layout { display: grid; grid-template-columns: 320px 1fr; gap: 2rem; align-items: start; }

.profile-card {
  background: #09090b;
  border: 2px solid #333;
  padding: 2.5rem 2rem;
  text-align: center;
  position: sticky;
  top: 100px;
}

.avatar-wrapper {
  width: 96px; height: 96px;
  margin: 0 auto 1rem;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-img {
  width: 100%; height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.avatar-placeholder {
  width: 96px; height: 96px;
  border-radius: 50%;
  background: #ccff00;
  color: #09090b;
  font-family: 'Syne', sans-serif;
  font-size: 2.4rem;
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

.profile-roles { display: flex; gap: 0.5rem; justify-content: center; margin-bottom: 1rem; flex-wrap: wrap; }

.role-badge {
  background: rgba(204, 255, 0, 0.15);
  border: 1px solid #ccff00;
  color: #ccff00;
  padding: 0.25rem 0.75rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  text-transform: uppercase;
}

.admin-badge {
  background: rgba(255, 100, 100, 0.15);
  border-color: #ff6464;
  color: #ff6464;
}

.profile-bio {
  font-family: 'Manrope', sans-serif;
  font-size: 0.9rem;
  color: #a1a1aa;
  margin: 0 0 1rem;
  line-height: 1.6;
}

.info-row { display: flex; justify-content: space-between; border-top: 2px solid #333; padding-top: 1rem; }
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

.section-title {
  font-family: 'Syne', sans-serif;
  font-size: 1.25rem;
  font-weight: 800;
  color: #fafafa;
  text-transform: uppercase;
  margin: 0 0 1.25rem;
  border-bottom: 4px solid #333;
  padding-bottom: 1rem;
}

.post-item {
  background: #09090b;
  border: 2px solid #333;
  padding: 1.25rem;
  margin-bottom: 1rem;
  transition: all 0.2s;
  cursor: pointer;
}

.post-item:hover { border-color: #ccff00; transform: translate(-2px, -2px); }

.post-header-row { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.35rem; }
.post-header-row h3 {
  font-family: 'Syne', sans-serif;
  font-size: 1.05rem;
  font-weight: 800;
  color: #fafafa;
  margin: 0;
}

.post-meta {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.72rem;
  color: #888;
  margin: 0 0 0.5rem;
}

.post-summary {
  font-family: 'Manrope', sans-serif;
  font-size: 0.88rem;
  color: #a1a1aa;
  margin: 0;
}

.status-tag {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  padding: 0.2rem 0.6rem;
  text-transform: uppercase;
  border: 1px solid;
}
.status-PUBLISHED { border-color: #22c55e; color: #22c55e; }
.status-PENDING { border-color: #f59e0b; color: #f59e0b; }
.status-REJECTED { border-color: #ef4444; color: #ef4444; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1rem;
  font-family: 'JetBrains Mono', monospace;
  color: #a1a1aa;
}
.pagination button {
  background: transparent;
  border: 2px solid #333;
  color: #fafafa;
  padding: 0.5rem 1rem;
  cursor: pointer;
  font-family: 'JetBrains Mono', monospace;
}
.pagination button:hover:not(:disabled) { background: #ccff00; color: #09090b; border-color: #ccff00; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }

.center-text {
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
  color: #a1a1aa;
  text-transform: uppercase;
}

.empty-state {
  text-align: center;
  color: #555;
  font-family: 'JetBrains Mono', monospace;
  padding: 3rem 0;
}

@media (max-width: 768px) {
  .profile-layout { grid-template-columns: 1fr; }
  .profile-card { position: static; }
}
</style>
