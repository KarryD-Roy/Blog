<template>
  <div class="page messages-page">
    <div class="messages-header">
      <h1 class="page-title">我的消息</h1>
      <button v-if="messages.length > 0" class="btn ghost small" @click="handleMarkAllRead">
        全部已读
      </button>
    </div>

    <div v-if="loading" class="center-text">加载中...</div>
    <div v-else-if="messages.length === 0" class="empty-state">
      <p>暂无消息</p>
    </div>
    <div v-else class="messages-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-item"
        :class="{ unread: !msg.isRead }"
        @click="handleClickMsg(msg)"
      >
        <div class="msg-indicator" v-if="!msg.isRead"></div>
        <div class="msg-content">
          <div class="msg-header">
            <span class="msg-type">{{ msg.type }}</span>
            <span class="msg-time">{{ formatDate(msg.createdAt) }}</span>
          </div>
          <h3 class="msg-title">{{ msg.title }}</h3>
          <p class="msg-body">{{ msg.content }}</p>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page }} / {{ totalPages }} 页</span>
      <button :disabled="page === totalPages" @click="changePage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getMessages, markAsRead, markAllAsRead } from '../../api/messages.js';
import { useMessages } from '../../stores/message.js';

const router = useRouter();
const { fetchUnreadCount } = useMessages();

const messages = ref([]);
const loading = ref(true);
const page = ref(1);
const totalPages = ref(0);

const formatDate = (d) => {
  if (!d) return '';
  return String(d).replace('T', ' ').slice(0, 16);
};

const fetchMessages = async () => {
  loading.value = true;
  try {
    const res = await getMessages(page.value, 10);
    if (res.data.code === 0) {
      messages.value = res.data.data.records || [];
      totalPages.value = res.data.data.pages || 0;
    }
  } finally { loading.value = false; }
};

const handleClickMsg = async (msg) => {
  if (!msg.isRead) {
    await markAsRead(msg.id);
    msg.isRead = true;
    fetchUnreadCount();
  }
  if (msg.relatedPostId) {
    router.push({ name: 'post-detail', params: { id: msg.relatedPostId } });
  }
};

const handleMarkAllRead = async () => {
  await markAllAsRead();
  messages.value.forEach(m => m.isRead = true);
  fetchUnreadCount();
};

const changePage = (p) => {
  page.value = p;
  fetchMessages();
};

onMounted(fetchMessages);
</script>

<style scoped>
.messages-page { max-width: 800px; margin: 0 auto; padding: 0 1rem; }

.messages-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  border-bottom: 4px solid #333;
  padding-bottom: 1rem;
}

.page-title {
  font-family: 'Syne', sans-serif;
  font-size: 2rem;
  font-weight: 800;
  color: #fafafa;
  text-transform: uppercase;
  margin: 0;
}

.center-text {
  text-align: center;
  font-family: 'JetBrains Mono', monospace;
  color: #a1a1aa;
  text-transform: uppercase;
}

.empty-state {
  text-align: center;
  padding: 4rem 0;
  color: #555;
  font-family: 'JetBrains Mono', monospace;
}

.messages-list { display: flex; flex-direction: column; gap: 0.75rem; }

.message-item {
  display: flex;
  background: #09090b;
  border: 2px solid #333;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 80px;
}

.message-item:hover { border-color: #555; transform: translate(-2px, -2px); }

.message-item.unread { border-left: 4px solid #ccff00; }

.msg-indicator {
  width: 8px;
  min-height: 100%;
  background: transparent;
}

.message-item.unread .msg-indicator { background: #ccff00; }

.msg-content { padding: 1.25rem 1.5rem; flex: 1; }

.msg-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.msg-type {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  color: #ccff00;
  text-transform: uppercase;
  background: rgba(204, 255, 0, 0.1);
  padding: 0.15rem 0.5rem;
}

.msg-time {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  color: #555;
}

.msg-title {
  font-family: 'Syne', sans-serif;
  font-size: 1rem;
  font-weight: 700;
  color: #fafafa;
  margin: 0 0 0.4rem;
}

.msg-body {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  color: #888;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.btn:hover { background: #ccff00; color: #09090b; border-color: #ccff00; }
.btn.ghost { border-style: dashed; }
.btn.small { font-size: 0.8rem; padding: 0.4rem 0.8rem; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
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
</style>
