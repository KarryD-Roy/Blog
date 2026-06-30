import { ref } from 'vue';
import { getUnreadCount } from '../api/messages.js';

const unreadCount = ref(0);
let pollingTimer = null;

export function useMessages() {
  const fetchUnreadCount = async () => {
    const token = localStorage.getItem('token');
    if (!token) return;
    try {
      const res = await getUnreadCount();
      if (res.data.code === 0) {
        unreadCount.value = res.data.data || 0;
      }
    } catch {
      // Silently fail
    }
  };

  const startPolling = (intervalMs = 30000) => {
    stopPolling();
    fetchUnreadCount();
    pollingTimer = setInterval(fetchUnreadCount, intervalMs);
  };

  const stopPolling = () => {
    if (pollingTimer) {
      clearInterval(pollingTimer);
      pollingTimer = null;
    }
  };

  return { unreadCount, fetchUnreadCount, startPolling, stopPolling };
}
