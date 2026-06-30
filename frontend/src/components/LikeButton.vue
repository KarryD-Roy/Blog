<template>
  <button class="like-btn" :class="{ liked: isLiked, animating }" @click="handleClick">
    <svg class="heart-icon" viewBox="0 0 24 24" width="20" height="20">
      <path
        :d="isLiked
          ? 'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z'
          : 'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z'
        "
        :fill="isLiked ? '#ccff00' : 'none'"
        :stroke="isLiked ? '#ccff00' : '#888'"
        stroke-width="1.5"
      />
    </svg>
    <span class="like-count">{{ likeCount }}</span>
  </button>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { toggleLike, getLikeStatus } from '../api/like.js';
import { useAuth } from '../stores/auth.js';

const props = defineProps({
  postId: { type: Number, required: true }
});

const { isAuthenticated } = useAuth();
const isLiked = ref(false);
const likeCount = ref(0);
const animating = ref(false);

const fetchStatus = async () => {
  try {
    const res = await getLikeStatus(props.postId);
    if (res.data.code === 0) {
      likeCount.value = res.data.data.likeCount || 0;
      isLiked.value = Boolean(res.data.data.liked);
    }
  } catch (e) { /* silent */ }
};

const handleClick = async () => {
  if (!isAuthenticated.value) {
    window.location.href = '/login';
    return;
  }
  animating.value = true;
  // Optimistic update
  const wasLiked = isLiked.value;
  isLiked.value = !wasLiked;
  likeCount.value += wasLiked ? -1 : 1;

  try {
    const res = await toggleLike(props.postId);
    if (res.data.code === 0) {
      isLiked.value = Boolean(res.data.data.liked);
      likeCount.value = res.data.data.likeCount || 0;
    }
  } catch (e) {
    // Revert on failure
    isLiked.value = wasLiked;
    likeCount.value += wasLiked ? 1 : -1;
  } finally {
    setTimeout(() => { animating.value = false; }, 300);
  }
};

watch(() => props.postId, fetchStatus);
onMounted(fetchStatus);
</script>

<style scoped>
.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: transparent;
  border: 2px solid #333;
  padding: 0.6rem 1.2rem;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'JetBrains Mono', monospace;
  color: #fafafa;
}

.like-btn:hover {
  border-color: #ccff00;
  transform: translate(-2px, -2px);
  box-shadow: 2px 2px 0 rgba(204, 255, 0, 0.3);
}

.like-btn.liked { border-color: #ccff00; }

.heart-icon { transition: transform 0.3s ease; }
.like-btn.animating .heart-icon { transform: scale(1.3); }

.like-count {
  font-size: 0.9rem;
  font-weight: bold;
  color: #a1a1aa;
}

.like-btn.liked .like-count { color: #ccff00; }
</style>
