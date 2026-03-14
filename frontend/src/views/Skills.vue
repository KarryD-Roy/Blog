<template>
  <div class="page">
    <h1 class="page-title">技能图谱</h1>
    <div v-if="loading" class="center-text">加载中...</div>
    <div v-else class="skills-grid">
      <section
        v-for="group in groupedSkills"
        :key="group.category"
        class="card"
      >
        <h2 class="card-title">{{ group.category }}</h2>
        <ul class="skill-list">
          <li v-for="item in group.items" :key="item.id">
            <h3 class="skill-title">{{ item.title }}</h3>
            <p class="skill-desc">{{ item.description }}</p>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import axios from 'axios';

const skills = ref([]);
const loading = ref(false);

const fetchSkills = async () => {
  loading.value = true;
  try {
    const res = await axios.get('/api/skills');
    if (res.data.code === 0) {
      skills.value = res.data.data || [];
    }
  } finally {
    loading.value = false;
  }
};

const groupedSkills = computed(() => {
  const map = new Map();
  for (const s of skills.value) {
    if (!map.has(s.category)) {
      map.set(s.category, []);
    }
    map.get(s.category).push(s);
  }
  return Array.from(map.entries()).map(([category, items]) => ({
    category,
    items
  }));
});

onMounted(fetchSkills);
</script>

