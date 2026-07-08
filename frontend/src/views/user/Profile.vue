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
          <span v-for="role in profile.roles" :key="role" :class="['role-badge', role === 'ADMIN' ? 'admin-badge' : '']">{{ role }}</span>
        </div>

        <!-- Daily Check-in Section -->
        <div v-if="!isAdmin" class="checkin-section card-inner">
          <h3>每日签到</h3>
          <div class="checkin-info">
            <div class="checkin-stat">
              <span class="stat-value">{{ checkinData.consecutiveDays }}</span>
              <span class="stat-label">连续天数</span>
            </div>
            <div class="checkin-stat">
              <span class="stat-value">{{ checkinData.totalCheckIns }}</span>
              <span class="stat-label">累计签到</span>
            </div>
          </div>
          <button
            class="btn checkin-btn"
            :class="{ checked: checkinData.checkedToday, primary: !checkinData.checkedToday }"
            :disabled="checkinData.checkedToday || checkingIn"
            @click="handleCheckIn"
          >
            {{ checkinData.checkedToday ? '已签到' : '立即签到' }}
          </button>
          <p v-if="checkinData.message" class="checkin-msg">{{ checkinData.message }}</p>
        </div>

        <!-- Edit Profile Form -->
        <div class="edit-profile-section card-inner">
          <h3>编辑资料</h3>
          <form @submit.prevent="handleUpdateProfile" class="profile-form">
            <div class="form-group">
              <label>昵称</label>
              <input type="text" v-model="editForm.nickname" class="input" />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input type="email" v-model="editForm.email" class="input" />
            </div>
            <div class="form-group">
              <label>个人简介</label>
              <textarea v-model="editForm.bio" class="input textarea" rows="3"></textarea>
            </div>
            <button type="submit" class="btn primary" :disabled="updating">
              {{ updating ? '保存中...' : '保存修改' }}
            </button>
          </form>
        </div>

        <div class="profile-info">
          <div class="info-row" v-if="profile.email && !editing">
            <span class="info-label">邮箱</span>
            <span class="info-value">{{ profile.email }}</span>
          </div>
          <div class="info-row" v-if="profile.bio && !editing">
            <span class="info-label">简介</span>
            <span class="info-value text-truncate">{{ profile.bio }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">注册时间</span>
            <span class="info-value">{{ formatDate(profile.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- Right Content Area -->
      <div class="profile-content">

        <!-- ADMIN: Article Review Panel -->
        <div v-if="isAdmin" class="admin-panel">
          <h2 class="section-title">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            管理面板
          </h2>

          <!-- Tabs: Review / Role Management -->
          <div class="tab-nav">
            <button
              class="tab-btn"
              :class="{ active: adminTab === 'review' }"
              @click="adminTab = 'review'"
            >文章审核</button>
            <button
              class="tab-btn"
              :class="{ active: adminTab === 'roles' }"
              @click="adminTab = 'roles'; loadAllUsers()"
            >角色授权</button>
          </div>

          <!-- Review Tab -->
          <div v-if="adminTab === 'review'" class="admin-tab-content">
            <div class="review-filter">
              <label>筛选状态:</label>
              <select v-model="reviewFilter" class="input" style="width:auto;display:inline-block;">
                <option value="PENDING">待审核</option>
                <option value="">全部状态</option>
              </select>
            </div>
            <div v-if="pendingPosts.length === 0" class="empty-state">暂无待审核文章</div>
            <div v-for="post in pendingPosts" :key="post.id" class="review-item">
              <div class="review-info">
                <h4>{{ post.title }}</h4>
                <p class="review-meta">作者ID: {{ post.userId }} · 状态: <span :class="'status-' + post.status">{{ post.status }}</span></p>
                <p class="review-summary">{{ post.summary || '(无摘要)' }}</p>
              </div>
              <div class="review-actions">
                <textarea v-model="reviewReasons[post.id]" placeholder="审核意见（可选）" class="input textarea-sm"></textarea>
                <div class="action-btns">
                  <button class="btn approve-btn" @click="doReview(post, 'APPROVED')">通过</button>
                  <button class="btn reject-btn" @click="doReview(post, 'REJECTED')">驳回</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Role Management Tab -->
          <div v-if="adminTab === 'roles'" class="admin-tab-content">
            <div v-if="allUsers.length === 0" class="empty-state">加载中...</div>
            <table v-else class="user-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>昵称</th>
                  <th>当前角色</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="u in allUsers" :key="u.id">
                  <td>{{ u.id }}</td>
                  <td>{{ u.username }}</td>
                  <td>{{ u.nickname || '-' }}</td>
                  <td>
                    <span v-for="role in (u.roles || [])" :key="role" class="role-badge" :class="role === 'ADMIN' ? 'admin-badge' : ''">{{ role }}</span>
                    <span v-if="!u.roles?.length" class="role-badge">USER</span>
                  </td>
                  <td>
                    <button
                      v-if="!u.roles?.includes('ADMIN')"
                      class="btn ghost grant-btn"
                      @click="doGrantAdmin(u)"
                      :disabled="grantingUserId === u.id"
                    >
                      {{ grantingUserId === u.id ? '处理中...' : '授予ADMIN' }}
                    </button>
                    <span v-else class="text-muted">已是管理员</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Regular User / All Users: My Posts Tab -->
        <div class="tabs-wrapper">
          <div class="tab-nav">
            <button
              class="tab-btn"
              :class="{ active: activeTab === 'posts' }"
              @click="activeTab = 'posts'; loadMyPosts()"
            >我的文章</button>
            <button
              v-if="isAdmin"
              class="tab-btn"
              :class="{ active: activeTab === 'comments' }"
              @click="activeTab = 'comments'"
            >评论历史</button>
          </div>

          <div class="tab-content">
            <!-- My Posts with Management -->
            <div v-if="activeTab === 'posts'" class="posts-section">
              <div v-if="myPostsLoading" class="center-text">加载中...</div>
              <div v-else-if="myPostsData.records?.length === 0" class="empty-state">暂无文章</div>
              <template v-else>
                <div v-for="post in myPostsData.records" :key="post.id" class="post-item-manage">
                  <div class="post-main" @click="goDetail(post.id)">
                    <div class="post-header-row">
                      <h3>{{ post.title }}</h3>
                      <span :class="'status-tag status-' + post.status">{{ statusLabel(post.status) }}</span>
                    </div>
                    <p class="post-meta">{{ formatDate(post.createdAt) }} · {{ post.viewCount ?? 0 }} 浏览</p>
                    <p class="post-summary">{{ post.summary || '(无摘要)' }}</p>
                  </div>
                  <div class="post-actions" @click.stop>
                    <button class="btn ghost edit-btn" @click="openEdit(post)">编辑</button>
                    <button class="btn ghost delete-btn" @click="confirmDelete(post)">删除</button>
                  </div>
                </div>
                <!-- Pagination -->
                <div class="pagination" v-if="myPages > 1">
                  <button :disabled="myCurrentPage <= 1" @click="changePage(myCurrentPage - 1)">&lt;</button>
                  <span>{{ myCurrentPage }} / {{ myPages }}</span>
                  <button :disabled="myCurrentPage >= myPages" @click="changePage(myCurrentPage + 1)">&gt;</button>
                </div>
              </template>
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

        <!-- Edit Post Modal -->
        <div v-if="showEditModal" class="modal-backdrop" @click.self="showEditModal = false">
          <div class="modal edit-modal">
            <h3>编辑文章</h3>
            <form @submit.prevent="submitEditPost" class="editor-grid">
              <div class="form-group">
                <label>标题</label>
                <input type="text" v-model="editPostForm.title" class="input" required />
              </div>
              <div class="form-group">
                <label>摘要</label>
                <input type="text" v-model="editPostForm.summary" class="input" />
              </div>
              <div class="form-group">
                <label>标签（逗号分隔）</label>
                <input type="text" v-model="editPostForm.tags" class="input" />
              </div>
              <div class="form-group">
                <label>内容</label>
                <textarea v-model="editPostForm.content" class="input textarea" rows="12"></textarea>
              </div>
              <div class="editor-actions">
                <button type="submit" class="btn primary" :disabled="savingEdit">保存</button>
                <button type="button" class="btn ghost" @click="showEditModal = false">取消</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="center-text">请先登录</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '../../stores/auth.js';
import { getProfile, updateProfile, myPosts, checkIn as apiCheckIn, getCheckinStatus, assignRole as apiAssignRole } from '../../api/user.js';
import { listPosts } from '../../api/posts.js';
import { updatePost, deletePost } from '../../api/posts.js';
import { getPendingPosts, reviewPost } from '../../api/admin.js';
import api from '../../api/index.js';

const router = useRouter();
const { isAuthenticated, user } = useAuth();

// State
const profile = ref(null);
const loading = ref(true);
const activeTab = ref('posts');
const adminTab = ref('review');

// Check-in state
const checkinData = ref({ checkedToday: false, consecutiveDays: 0, totalCheckIns: 0, message: '' });
const checkingIn = ref(false);

// Profile edit
const updating = ref(false);
const editing = computed(() => true); // always show editable form
const editForm = reactive({ nickname: '', email: '', bio: '' });

// My posts
const myPostsData = ref({ records: [], total: 0, size: 10, current: 1 });
const myPostsLoading = ref(false);
const myCurrentPage = ref(1);
const myPageSize = ref(5);

// Comments
const myComments = ref([]);

// Admin review
const pendingPosts = ref([]);
const reviewFilter = ref('PENDING');
const reviewReasons = reactive({});
const reviewingId = ref(null);

// Admin roles
const allUsers = ref([]);
const grantingUserId = ref(null);

// Post editing modal
const showEditModal = ref(false);
const savingEdit = ref(false);
const editPostForm = reactive({ id: null, title: '', summary: '', content: '', tags: '' });

const isAdmin = computed(() => {
  return user.value?.roles?.includes('ADMIN') || profile.value?.roles?.includes('ADMIN');
});

const displayInitial = computed(() => {
  const name = profile.value?.nickname || profile.value?.username || 'U';
  return name.charAt(0).toUpperCase();
});

// Computed for pagination
const myPages = computed(() => Math.max(1, Math.ceil((myPostsData.value.total || 0) / myPageSize.value)));

const formatDate = (d) => {
  if (!d) return '';
  const s = String(d).replace('T', ' ').slice(0, 16);
  return s;
};

const statusLabel = (status) => {
  const map = { PUBLISHED: '已发布', PENDING: '待审核', REJECTED: '已驳回' };
  return map[status] || status;
};

// Load profile
const fetchProfile = async () => {
  loading.value = true;
  try {
    const res = await getProfile();
    if (res.data.code === 0) {
      profile.value = res.data.data;
      editForm.nickname = res.data.data.nickname || '';
      editForm.email = res.data.data.email || '';
      editForm.bio = res.data.data.bio || '';
      await Promise.all([loadMyPosts(), loadCheckinStatus()]);
      // If admin, load review data
      if (isAdmin.value) {
        loadPendingPosts();
      }
    }
  } catch { /* ignore */ }
  finally { loading.value = false; }
};

// Check-in
const handleCheckIn = async () => {
  checkingIn.value = true;
  try {
    const res = await apiCheckIn();
    if (res.data.code === 0) {
      checkinData.value = res.data.data;
    }
  } catch { /* ignore */ }
  finally { checkingIn.value = false; }
};

const loadCheckinStatus = async () => {
  try {
    const res = await getCheckinStatus();
    if (res.data.code === 0) {
      checkinData.value = res.data.data;
    }
  } catch { /* ignore */ }
};

// Update profile
const handleUpdateProfile = async () => {
  updating.value = true;
  try {
    const res = await updateProfile(editForm);
    if (res.data.code === 0) {
      profile.value = res.data.data;
      alert('资料更新成功！');
    }
  } catch (e) {
    alert(e.response?.data?.message || '更新失败');
  }
  finally { updating.value = false; }
};

// My posts
const loadMyPosts = async () => {
  myPostsLoading.value = true;
  try {
    const res = await myPosts(myCurrentPage.value, myPageSize.value);
    if (res.data.code === 0) {
      myPostsData.value = res.data.data;
    }
  } catch { /* ignore */ }
  finally { myPostsLoading.value = false; }
};

const changePage = (page) => {
  myCurrentPage.value = page;
  loadMyPosts();
};

// Post edit/delete
const openEdit = (post) => {
  editPostForm.id = post.id;
  editPostForm.title = post.title;
  editPostForm.summary = post.summary || '';
  editPostForm.content = post.content || '';
  editPostForm.tags = post.tags || '';
  showEditModal.value = true;
};

const submitEditPost = async () => {
  savingEdit.value = true;
  try {
    const res = await updatePost(editPostForm.id, editPostForm);
    if (res.data.code === 0) {
      showEditModal.value = false;
      await loadMyPosts();
    }
  } catch (e) {
    alert(e.response?.data?.message || '保存失败');
  }
  finally { savingEdit.value = false; }
};

const confirmDelete = async (post) => {
  if (!confirm(`确定删除《${post.title}》吗？此操作不可撤销。`)) return;
  try {
    const res = await deletePost(post.id);
    if (res.data.code === 0) {
      await loadMyPosts();
    }
  } catch (e) {
    alert(e.response?.data?.message || '删除失败');
  }
};

// Admin: Review
const loadPendingPosts = async () => {
  try {
    const res = await getPendingPosts();
    if (res.data.code === 0) {
      pendingPosts.value = res.data.data || [];
    }
  } catch { /* ignore */ }
};

const doReview = async (post, status) => {
  reviewingId.value = post.id;
  try {
    const reason = reviewReasons[post.id] || '';
    await reviewPost(post.id, status, reason);
    alert(`已${status === 'APPROVED' ? '通过' : '驳回'}该文章`);
    delete reviewReasons[post.id];
    await loadPendingPosts();
  } catch (e) {
    alert(e.response?.data?.message || '操作失败');
  }
  finally { reviewingId.value = null; }
};

// Admin: Role management
const loadAllUsers = async () => {
  try {
    const res = await api.get('/users');
    if (res.data.code === 0) {
      allUsers.value = res.data.data || [];
    }
  } catch { /* ignore */ }
};

const doGrantAdmin = async (u) => {
  if (!confirm(`确定将 ${u.username} 授予管理员权限？`)) return;
  grantingUserId.value = u.id;
  try {
    await apiAssignRole(u.id, 'ADMIN');
    alert(`已将 ${u.username} 设为管理员`);
    await loadAllUsers();
  } catch (e) {
    alert(e.response?.data?.message || '操作失败');
  }
  finally { grantingUserId.value = null; }
};

const goDetail = (id) => router.push({ name: 'post-detail', params: { id } });

onMounted(() => {
  if (isAuthenticated.value) fetchProfile();
});

onActivated(() => {
  // 页面重新激活时（如从其他页面返回）自动刷新我的文章列表
  if (isAuthenticated.value) {
    loadMyPosts();
    if (isAdmin.value) loadPendingPosts();
  }
});
</script>

<style scoped>
.profile-page { max-width: 1200px; margin: 0 auto; padding: 0 1rem; }

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
  position: sticky;
  top: 100px;
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

.profile-roles { display: flex; gap: 0.5rem; justify-content: center; margin-bottom: 1.5rem; flex-wrap: wrap; }

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

/* Check-in section */
.checkin-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 2px solid #333;
  text-align: left;
}

.card-inner h3 {
  font-family: 'Syne', sans-serif;
  font-size: 0.95rem;
  font-weight: 800;
  color: #ccff00;
  text-transform: uppercase;
  margin: 0 0 1rem;
  letter-spacing: 0.05em;
}

.checkin-info {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 1rem;
}

.checkin-stat {
  text-align: center;
}

.stat-value {
  display: block;
  font-family: 'Syne', sans-serif;
  font-size: 1.75rem;
  font-weight: 800;
  color: #ccff00;
}

.stat-label {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  color: #888;
  text-transform: uppercase;
}

.checkin-btn {
  width: 100%;
  padding: 0.75rem;
  font-size: 0.9rem;
}

.checkin-btn.checked {
  background: transparent;
  border-color: #555;
  color: #666;
  cursor: not-allowed;
}

.checkin-msg {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.72rem;
  color: #a1a1aa;
  margin: 0.5rem 0 0;
}

/* Edit profile section */
.edit-profile-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 2px solid #333;
  text-align: left;
}

.profile-form .form-group {
  margin-bottom: 0.75rem;
  text-align: left;
}

.profile-form label {
  display: block;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  color: #888;
  text-transform: uppercase;
  margin-bottom: 0.35rem;
}

.textarea {
  resize: vertical;
  min-height: 60px;
}

.textarea-sm {
  resize: vertical;
  min-height: 50px;
}

.profile-info { text-align: left; border-top: 2px solid #333; padding-top: 1.5rem; }

.info-row { display: flex; justify-content: space-between; margin-bottom: 0.75rem; gap: 0.5rem;}

.info-label {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  color: #888;
  text-transform: uppercase;
  white-space: nowrap;
}

.info-value {
  font-family: 'Manrope', sans-serif;
  font-size: 0.9rem;
  color: #d4d4d8;
  text-align: right;
}

.text-truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.profile-content { min-height: 400px; }

.section-title {
  font-family: 'Syne', sans-serif;
  font-size: 1.25rem;
  font-weight: 800;
  color: #fafafa;
  text-transform: uppercase;
  margin: 0 0 1.25rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tab-nav {
  display: flex;
  gap: 0;
  border-bottom: 2px solid #333;
  margin-bottom: 1.5rem;
}

.tab-btn {
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  color: #888;
  padding: 0.85rem 1.5rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.82rem;
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

/* Admin Panel */
.admin-panel {
  background: #09090b;
  border: 2px solid #333;
  padding: 2rem;
  margin-bottom: 2rem;
}

.admin-tab-content {
  min-height: 200px;
}

.review-filter {
  margin-bottom: 1.5rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  color: #888;
}

.review-filter label { margin-right: 0.5rem; }

.review-item {
  background: #111;
  border: 1px solid #333;
  padding: 1.25rem;
  margin-bottom: 1rem;
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
}

.review-info { flex: 1; min-width: 0; }

.review-info h4 {
  font-family: 'Syne', sans-serif;
  font-size: 1rem;
  font-weight: 700;
  color: #fafafa;
  margin: 0 0 0.35rem;
}

.review-meta {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  color: #888;
  margin: 0 0 0.5rem;
}

.review-summary {
  font-size: 0.85rem;
  color: #a1a1aa;
  margin: 0;
}

.review-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-width: 180px;
  align-items: flex-end;
}

.action-btns {
  display: flex;
  gap: 0.5rem;
}

.approve-btn {
  background: #22c55e;
  border-color: #22c55e;
  color: #09090b;
  padding: 0.4rem 1rem;
  font-size: 0.78rem;
}
.approve-btn:hover {
  background: #16a34a;
  box-shadow: none;
  transform: none;
}

.reject-btn {
  background: #ef4444;
  border-color: #ef4444;
  color: #fff;
  padding: 0.4rem 1rem;
  font-size: 0.78rem;
}
.reject-btn:hover {
  background: #dc2626;
  box-shadow: none;
  transform: none;
}

/* Status tags */
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

/* User Table */
.user-table {
  width: 100%;
  border-collapse: collapse;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
}

.user-table th,
.user-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #333;
  text-align: left;
}

.user-table th {
  background: #18181b;
  color: #ccff00;
  text-transform: uppercase;
  font-weight: 700;
}

.user-table td { color: #d4d4d8; }
.user-table tr:hover td { background: #18181b; }

.grant-btn {
  padding: 0.35rem 0.8rem;
  font-size: 0.72rem;
  border-color: #f59e0b;
  color: #f59e0b;
}
.grant-btn:hover {
  background: #f59e0b;
  color: #09090b;
  transform: none;
  box-shadow: none;
}

.text-muted {
  color: #555;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.72rem;
}

/* Post item with management actions */
.post-item-manage {
  background: #09090b;
  border: 2px solid #333;
  padding: 1.25rem;
  margin-bottom: 1rem;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.post-item-manage:hover { border-color: #ccff00; transform: translate(-2px, -2px); }

.post-main {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.post-header-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}

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

.post-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.post-item-manage:hover .post-actions { opacity: 1; }

.edit-btn { border-color: #3b82f6; color: #3b82f6; padding: 0.35rem 0.8rem; font-size: 0.72rem; }
.edit-btn:hover { background: #3b82f6; color: #09090b; transform: none; box-shadow: none; }

.delete-btn { border-color: #ef4444; color: #ef4444; padding: 0.35rem 0.8rem; font-size: 0.72rem; }
.delete-btn:hover { background: #ef4444; color: #fff; transform: none; box-shadow: none; }

/* Edit Modal */
.edit-modal {
  max-height: 90vh;
  overflow-y: auto;
}

.edit-modal h3 {
  font-family: 'Syne', sans-serif;
  font-size: 1.3rem;
  font-weight: 800;
  color: #fafafa;
  margin: 0 0 1.5rem;
}

.edit-modal .form-group { margin-bottom: 1rem; }
.edit-modal label {
  display: block;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  color: #888;
  text-transform: uppercase;
  margin-bottom: 0.4rem;
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
  .profile-card { position: static; }
  .review-item { flex-direction: column; }
  .review-actions { align-items: stretch; }
  .post-item-manage { flex-direction: column; }
  .post-actions { opacity: 1; }
}
</style>
