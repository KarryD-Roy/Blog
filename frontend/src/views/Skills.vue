<template>
  <div class="page">
    <h1 class="page-title">技能图谱</h1>
    <section class="card" style="cursor: default">
      <h2 class="card-title" style="margin-bottom: 0.6rem">
        {{ editingSkill ? '编辑技能' : '新增技能' }}
      </h2>
      <div class="editor-grid">
        <input
          v-model="form.category"
          class="input"
          type="text"
          placeholder="技能分类（例如：后端、前端、基础能力）"
        />
        <input
          v-model="form.title"
          class="input"
          type="text"
          placeholder="技能名称，例如：Spring Boot / Vue3"
        />
        <textarea
          v-model="form.description"
          class="input"
          rows="3"
          placeholder="简短说明（可选）"
        ></textarea>
        <div class="editor-actions">
          <button class="btn primary" @click="submitSkill">
            {{ editingSkill ? '保存修改' : '添加技能' }}
          </button>
          <button
            v-if="editingSkill"
            class="btn ghost"
            @click="resetForm"
          >
            取消编辑
          </button>
        </div>
      </div>
    </section>

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
            <div style="margin-top: 0.3rem; display: flex; gap: 0.4rem">
              <button
                class="btn ghost"
                style="font-size: 0.75rem"
                @click="startEdit(item)"
              >
                编辑
              </button>
              <button
                class="btn ghost"
                style="font-size: 0.75rem; color: #fca5a5; border-color: rgba(248, 113, 113, 0.6)"
                @click="deleteSkill(item.id)"
              >
                删除
              </button>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import axios from 'axios';

const skills = ref([]);
const loading = ref(false);
const editingSkill = ref(null);
const form = reactive({
  category: '',
  title: '',
  description: ''
});

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

const resetForm = () => {
  editingSkill.value = null;
  form.category = '';
  form.title = '';
  form.description = '';
};

const startEdit = (item) => {
  editingSkill.value = { ...item };
  form.category = item.category || '';
  form.title = item.title || '';
  form.description = item.description || '';
};

const submitSkill = async () => {
  if (!form.category.trim() || !form.title.trim()) {
    return;
  }
  const payload = {
    category: form.category,
    title: form.title,
    description: form.description
  };
  if (editingSkill.value && editingSkill.value.id) {
    await axios.put(`/api/skills/${editingSkill.value.id}`, payload);
  } else {
    await axios.post('/api/skills', payload);
  }
  resetForm();
  fetchSkills();
};

const deleteSkill = async (id) => {
  if (!window.confirm('确认删除该技能吗？')) return;
  await axios.delete(`/api/skills/${id}`);
  fetchSkills();
};

onMounted(fetchSkills);
</script>

