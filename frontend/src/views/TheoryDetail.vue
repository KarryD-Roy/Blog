<template>
  <div class="page">
    <div class="theory-header">
      <button class="btn ghost" @click="goBack">&larr; 返回技能树</button>
      <h1 class="page-title">理论知识与面试题: {{ skill?.title }}</h1>
    </div>

    <div class="theory-card card">
      <div v-if="loading" class="center-text">加载中...</div>
      <div v-else>
        <div v-if="!isEditing">
          <div v-if="theory?.content" class="theory-content markdown-body" v-html="renderedContent"></div>
          <div v-else class="empty-hint">暂无理论知识，请点击编辑新增</div>
          <div class="theory-actions">
            <button class="btn primary" @click="startEdit">编辑内容</button>
          </div>
        </div>
        <div v-else class="theory-editor">
          <textarea
            v-model="editForm.content"
            class="input theory-textarea"
            rows="20"
            placeholder="使用 Markdown 编写理论知识与面试题..."
          ></textarea>
          <div class="editor-help">提示：支持标准 Markdown 语法</div>
          <div class="theory-actions">
            <button class="btn primary" @click="saveTheory">保存</button>
            <button class="btn ghost" @click="cancelEdit">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

const route = useRoute();
const router = useRouter();
const skillId = route.params.skillId;

const skill = ref(null);
const theory = ref(null);
const loading = ref(false);
const isEditing = ref(false);
const editForm = ref({ content: '' });

const fetchSkillAndTheory = async () => {
  loading.value = true;
  try {
    const [skillRes, theoryRes] = await Promise.all([
      axios.get(`/api/skills/${skillId}`), // 假设后端有这个接口，如果没有则需要 fetchGraph 后过滤
      axios.get(`/api/theories/skill/${skillId}`)
    ]);

    // 如果没有获取单个技能接口，可以使用 list 接口兜底
    if (!skillRes.data.data) {
        const allRes = await axios.get('/api/skills');
        skill.value = allRes.data.data.find(s => s.id == skillId);
    } else {
        skill.value = skillRes.data.data;
    }

    theory.value = theoryRes.data.data;
  } catch (err) {
    console.error('Fetch error:', err);
  } finally {
    loading.value = false;
  }
};

const renderedContent = computed(() => {
  if (!theory.value?.content) return '';
  return DOMPurify.sanitize(marked.parse(theory.value.content));
});

const startEdit = () => {
  editForm.value.content = theory.value?.content || '';
  isEditing.value = true;
};

const cancelEdit = () => {
  isEditing.value = false;
};

const saveTheory = async () => {
  try {
    const res = await axios.post('/api/theories', {
      skillId: Number(skillId),
      content: editForm.value.content
    });
    if (res.data.code === 0) {
      theory.value = res.data.data;
      isEditing.value = false;
    }
  } catch (err) {
    alert('保存失败');
  }
};

const goBack = () => {
  router.push('/skills');
};

onMounted(fetchSkillAndTheory);
</script>

<style scoped>
.theory-header {
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.theory-card {
  padding: 2rem;
  min-height: 400px;
}
.theory-content {
  color: #e5e7eb;
  line-height: 1.6;
}
.theory-textarea {
  width: 100%;
  font-family: monospace;
  background: #0f172a;
  color: #e5e7eb;
  padding: 1rem;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
}
.theory-actions {
  margin-top: 2rem;
  display: flex;
  gap: 1rem;
}
.editor-help {
  color: #94a3b8;
  font-size: 0.85rem;
  margin-bottom: 1rem;
}
</style>
