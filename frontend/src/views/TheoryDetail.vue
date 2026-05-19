<template>
  <div class="page">
    <div class="theory-header">
      <button class="btn ghost back-btn" @click="goBack">&larr; 返回技能树</button>
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
  margin-bottom: 3rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  border-bottom: 4px solid #333;
  padding-bottom: 1.5rem;
}

.back-btn {
  align-self: flex-start;
  margin-bottom: 1rem;
  padding: 0.6rem 1.2rem;
  border-radius: 0;
  border: 2px solid #333;
  background: transparent;
  color: #fafafa;
  cursor: pointer;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
  font-weight: bold;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: #ccff00;
  color: #09090b;
  border-color: #ccff00;
  transform: translate(-4px, -4px);
  box-shadow: 4px 4px 0 rgba(255,255,255,0.2);
}

.page-title {
  font-family: 'Syne', sans-serif;
  font-weight: 800;
  font-size: 2.5rem;
  color: #fafafa;
  margin: 0;
  text-transform: uppercase;
}

.theory-card {
  padding: 3rem;
  min-height: 400px;
  background: #09090b;
  border: 2px solid #333;
  border-radius: 0;
  box-shadow: 12px 12px 0 rgba(204, 255, 0, 0.2);
  transition: all 0.3s ease;
}

.theory-card:hover {
  border-color: #ccff00;
  transform: translate(-4px, -4px);
  box-shadow: 16px 16px 0 rgba(204, 255, 0, 0.4);
}

.theory-content {
  color: #fafafa;
  line-height: 1.8;
  font-family: 'Manrope', sans-serif;
  font-size: 1.1rem;
}

.theory-textarea {
  width: 100%;
  font-family: 'JetBrains Mono', monospace;
  background: transparent;
  color: #fafafa;
  padding: 1.5rem;
  border-radius: 0;
  margin-bottom: 1.5rem;
  border: 2px solid #333;
  resize: vertical;
}

.theory-textarea:focus {
  outline: none;
  border-color: #ccff00;
  background: #000;
  box-shadow: 4px 4px 0 rgba(204,255,0,0.2);
}

.theory-actions {
  margin-top: 2rem;
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

.editor-help {
  color: #a1a1aa;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.9rem;
  margin-bottom: 1rem;
  text-transform: uppercase;
}
</style>
