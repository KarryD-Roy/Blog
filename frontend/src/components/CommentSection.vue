<template>
  <div class="comment-section">
    <h3 class="section-title">评论 ({{ comments.length }})</h3>

    <!-- Comment Input -->
    <div class="comment-input-area" v-if="isAuthenticated">
      <textarea
        v-model="newComment"
        class="input comment-textarea"
        :placeholder="replyTo ? `回复 @${replyTo.nickname || ''}` : '写下你的评论...'"
        rows="3"
      ></textarea>
      <div class="comment-actions">
        <button v-if="replyTo" class="btn ghost small" @click="cancelReply">取消回复</button>
        <button class="btn primary small" :disabled="!newComment.trim() || submitting" @click="submitComment">
          {{ submitting ? '提交中...' : '发表评论' }}
        </button>
      </div>
    </div>
    <div v-else class="login-prompt">
      <RouterLink to="/login">登录</RouterLink> 后参与评论
    </div>

    <!-- Comments List -->
    <div v-if="loading" class="center-text">加载评论...</div>
    <div v-else-if="comments.length === 0" class="empty-comments">暂无评论，来说点什么吧</div>
    <div v-else class="comments-list">
      <!-- Top-level comments -->
      <div v-for="comment in topLevelComments" :key="comment.id" class="comment-thread">
        <div class="comment-item">
          <div class="comment-avatar">{{ getInitial(comment) }}</div>
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-author">{{ getAuthorName(comment) }}</span>
              <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            </div>
            <p class="comment-text">{{ comment.content }}</p>
            <div class="comment-footer">
              <button v-if="isAuthenticated" class="reply-btn" @click="startReply(comment)">
                回复
              </button>
              <button
                v-if="canDelete(comment)"
                class="delete-btn"
                @click="handleDelete(comment.id)"
              >删除</button>
            </div>

            <!-- Reply Input for this comment -->
            <div v-if="replyTo?.id === comment.id" class="reply-input-area">
              <textarea
                v-model="replyContent"
                class="input comment-textarea"
                :placeholder="`回复 @${getAuthorName(comment)}`"
                rows="2"
              ></textarea>
              <div class="comment-actions">
                <button class="btn ghost small" @click="cancelReply">取消</button>
                <button class="btn primary small" :disabled="!replyContent.trim()" @click="submitReply(comment.id)">
                  回复
                </button>
              </div>
            </div>

            <!-- Child comments -->
            <div v-if="getReplies(comment.id).length > 0" class="child-comments">
              <div v-for="reply in getReplies(comment.id)" :key="reply.id" class="comment-item child">
                <div class="comment-avatar small">{{ getInitial(reply) }}</div>
                <div class="comment-body">
                  <div class="comment-header">
                    <span class="comment-author">{{ getAuthorName(reply) }}</span>
                    <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                  </div>
                  <p class="comment-text">{{ reply.content }}</p>
                  <div class="comment-footer">
                    <button
                      v-if="canDelete(reply)"
                      class="delete-btn"
                      @click="handleDelete(reply.id)"
                    >删除</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { getComments, addComment, deleteComment } from '../api/comments.js';
import { useAuth } from '../stores/auth.js';

const props = defineProps({
  postId: { type: Number, required: true }
});

const { isAuthenticated, user } = useAuth();

const comments = ref([]);
const loading = ref(false);
const newComment = ref('');
const submitting = ref(false);
const replyTo = ref(null);
const replyContent = ref('');

const topLevelComments = computed(() => comments.value.filter(c => !c.parentId));
const getReplies = (parentId) => comments.value.filter(c => c.parentId === parentId);

const getInitial = (c) => {
  if (c.nickname) return c.nickname.charAt(0).toUpperCase();
  return 'U';
};

const getAuthorName = (c) => c.nickname || c.username || '未知用户';
const formatTime = (t) => t ? String(t).replace('T', ' ').slice(0, 16) : '';
const canDelete = (c) => user.value && (user.value.id === c.userId ||
  (user.value.roles && user.value.roles.includes('ADMIN')));

const fetchComments = async () => {
  loading.value = true;
  try {
    const res = await getComments(props.postId);
    if (res.data.code === 0) comments.value = res.data.data || [];
  } finally { loading.value = false; }
};

const submitComment = async () => {
  if (!newComment.value.trim()) return;
  submitting.value = true;
  try {
    await addComment(props.postId, newComment.value.trim(), null);
    newComment.value = '';
    await fetchComments();
  } catch (e) {
    console.error('评论失败', e);
  } finally { submitting.value = false; }
};

const startReply = (comment) => {
  replyTo.value = comment;
  replyContent.value = '';
};

const cancelReply = () => {
  replyTo.value = null;
  replyContent.value = '';
};

const submitReply = async (parentId) => {
  if (!replyContent.value.trim()) return;
  try {
    await addComment(props.postId, replyContent.value.trim(), parentId);
    cancelReply();
    await fetchComments();
  } catch (e) {
    console.error('回复失败', e);
  }
};

const handleDelete = async (commentId) => {
  if (!confirm('确认删除这条评论吗？')) return;
  try {
    await deleteComment(props.postId, commentId);
    await fetchComments();
  } catch (e) {
    console.error('删除失败', e);
  }
};

watch(() => props.postId, fetchComments);
onMounted(fetchComments);
</script>

<style scoped>
.comment-section {
  margin-top: 3rem;
  border-top: 2px dashed #333;
  padding-top: 2rem;
}

.section-title {
  font-family: 'Syne', sans-serif;
  font-size: 1.3rem;
  font-weight: 800;
  color: #fafafa;
  margin: 0 0 1.5rem;
  text-transform: uppercase;
}

.comment-input-area { margin-bottom: 2rem; }

.comment-textarea {
  width: 100%;
  box-sizing: border-box;
}

.input {
  background: #1a1a1a;
  border: 2px solid #333;
  border-radius: 0;
  color: #fafafa;
  padding: 0.9rem 1rem;
  font-family: 'Manrope', sans-serif;
  font-size: 0.95rem;
  outline: none;
  resize: vertical;
  transition: border-color 0.2s;
}

.input:focus { border-color: #ccff00; }

.comment-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.login-prompt {
  text-align: center;
  padding: 2rem;
  color: #888;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
  border: 2px dashed #333;
  margin-bottom: 2rem;
}

.login-prompt a { color: #ccff00; text-decoration: none; font-weight: bold; }

.empty-comments {
  text-align: center;
  color: #555;
  font-family: 'JetBrains Mono', monospace;
  padding: 2rem 0;
}

.comments-list { display: flex; flex-direction: column; gap: 1rem; }

.comment-thread { /* container */ }

.comment-item {
  display: flex;
  gap: 1rem;
  padding: 1.25rem;
  background: #09090b;
  border: 2px solid #333;
}

.comment-item.child {
  margin-left: 3rem;
  margin-top: 0.75rem;
  border-left: 3px solid #ccff00;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  min-width: 36px;
  border-radius: 50%;
  background: #ccff00;
  color: #09090b;
  font-family: 'Syne', sans-serif;
  font-size: 1rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.comment-avatar.small { width: 28px; height: 28px; min-width: 28px; font-size: 0.8rem; }

.comment-body { flex: 1; min-width: 0; }

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 0.4rem;
}

.comment-author {
  font-family: 'Syne', sans-serif;
  font-weight: 700;
  font-size: 0.9rem;
  color: #ccff00;
}

.comment-time {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  color: #555;
}

.comment-text {
  font-family: 'Manrope', sans-serif;
  font-size: 0.9rem;
  color: #d4d4d8;
  margin: 0 0 0.5rem;
  line-height: 1.6;
  word-break: break-word;
}

.comment-footer { display: flex; gap: 1rem; }

.reply-btn {
  background: none;
  border: none;
  color: #888;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  cursor: pointer;
  text-transform: uppercase;
  padding: 0;
}

.reply-btn:hover { color: #ccff00; }

.delete-btn {
  background: none;
  border: none;
  color: #fca5a5;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  cursor: pointer;
  text-transform: uppercase;
  padding: 0;
}

.delete-btn:hover { text-decoration: underline; }

.reply-input-area {
  margin-top: 0.75rem;
  padding-left: 0;
}

.child-comments { margin-top: 0.5rem; }

.btn {
  background-color: transparent;
  border: 2px solid #333;
  border-radius: 0;
  color: #fafafa;
  cursor: pointer;
  padding: 0.6rem 1.2rem;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
  font-weight: bold;
  transition: all 0.2s ease;
}

.btn:hover { background: #ccff00; color: #09090b; border-color: #ccff00; }
.btn.primary { background: #ccff00; color: #09090b; border-color: #ccff00; }
.btn.ghost { border-style: dashed; }
.btn.small { font-size: 0.8rem; padding: 0.4rem 0.8rem; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.center-text {
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
  color: #a1a1aa;
  font-size: 0.8rem;
}

@media (max-width: 640px) {
  .comment-item.child { margin-left: 1.5rem; }
  .comment-avatar { width: 30px; height: 30px; min-width: 30px; font-size: 0.9rem; }
}
</style>
