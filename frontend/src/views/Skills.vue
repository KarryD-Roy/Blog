<template>
  <div class="page">
    <div class="skills-header">
      <div>
        <h1 class="page-title">技能树</h1>
        <p class="subtitle">拖拽节点可固化坐标，点击节点查看关联文章</p>
      </div>
      <div class="header-actions">
        <button class="btn ghost" @click="fetchGraph">刷新技能树</button>
        <button class="btn primary" @click="openCreate">新增技能</button>
      </div>
    </div>

    <div class="graph-card">
      <div v-show="loading" class="center-text loading-overlay">加载中...</div>
      <div ref="chartRef" class="graph-canvas" :style="{ opacity: loading ? 0 : 1 }"></div>
      <div v-if="!loading && !graphNodes.length" class="empty-hint">暂无技能节点</div>
    </div>

    <div v-if="drawerOpen" class="drawer">
      <div class="drawer-header">
        <div>
          <h2 class="drawer-title">{{ selectedSkill?.title }}</h2>
          <p class="drawer-meta">{{ selectedSkill?.category }}</p>
        </div>
        <button class="btn ghost" @click="closeDrawer">关闭</button>
      </div>

      <div class="drawer-section">
        <h3 class="section-title">理论及面试题</h3>
        <div v-if="selectedSkill?.id" class="drawer-desc">
          <router-link :to="`/theory/${selectedSkill.id}`" class="theory-link">
            查看该技能的理论知识与面试题汇总 &rarr;
          </router-link>
        </div>
        <div v-else class="empty-hint" style="margin-top: 0">暂无理论及面试题汇总</div>
      </div>

      <div class="drawer-section">
        <h3 class="section-title">关联实战文章</h3>
        <div v-if="postLoading" class="center-text">加载文章...</div>
        <div v-else>
          <div v-if="posts.length" class="post-list">
            <div v-for="post in posts" :key="post.id" class="post-item">
              <div>
                <div class="post-title" @click="openPost(post.id)">{{ post.title }}</div>
                <div class="post-summary">{{ post.summary }}</div>
              </div>
              <button class="btn ghost" @click="openPost(post.id)">查看</button>
            </div>
          </div>
          <div v-else class="empty-hint" style="margin-top: 0">暂无关联文章</div>

          <div v-if="postTotalPages > 1" class="pagination">
            <button :disabled="postPage === 1" @click="changePostPage(postPage - 1)">
              上一页
            </button>
            <span>第 {{ postPage }} / {{ postTotalPages }} 页</span>
            <button
              :disabled="postPage === postTotalPages"
              @click="changePostPage(postPage + 1)"
            >
              下一页
            </button>
          </div>
        </div>
      </div>

      <div class="drawer-actions">
        <button class="btn primary" @click="editSkill">编辑技能</button>
        <button class="btn danger" @click="deleteSkill">删除技能</button>
      </div>
    </div>

    <div v-if="showEditor" class="modal-backdrop">
      <div class="modal">
        <h2 class="card-title" style="margin-bottom: 0.8rem">
          {{ editingSkill ? '编辑技能' : '新增技能' }}
        </h2>
        <div class="editor-grid">
          <div class="select-wrapper" ref="selectWrapper">
            <div class="select-input" :class="{ open: isSelectOpen }" @click="isSelectOpen = !isSelectOpen">
              {{ currentParentLabel }}
            </div>
            <div v-if="isSelectOpen" class="select-options">
              <div
                v-for="option in parentOptions"
                :key="option.value"
                class="select-option"
                @click="selectParent(option.value)"
              >
                {{ option.label }}
              </div>
            </div>
          </div>
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
            <button class="btn ghost" @click="closeEditor">
              取消
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, nextTick, computed } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import axios from 'axios';

const router = useRouter();
const chartRef = ref(null);
let chartInstance = null;
let resizeHandler = null;

const loading = ref(false);
const graphNodes = ref([]);
const graphLinks = ref([]);
const graphSkills = ref(new Map());
const coordQueue = ref(new Map());
let coordTimer = null;

const drawerOpen = ref(false);
const selectedSkill = ref(null);
const posts = ref([]);
const postPage = ref(1);
const postTotalPages = ref(1);
const postLoading = ref(false);

const showEditor = ref(false);
const editingSkill = ref(null);
const form = reactive({
  title: '',
  description: '',
  parentValue: ''
});

const isSelectOpen = ref(false);
const selectWrapper = ref(null);

const handleClickOutside = (event) => {
  if (selectWrapper.value && !selectWrapper.value.contains(event.target)) {
    isSelectOpen.value = false;
  }
};

const predefinedCategories = ['前端', '后端', '开发工具链', '编程语言与基础能力', '基础技能'];

const parentOptions = computed(() => {
  const options = [];

  // Categories
  const existingCats = Array.from(new Set(graphNodes.value.map(s => s.category).filter(Boolean)));
  const allCats = Array.from(new Set([...predefinedCategories, ...existingCats]));

  allCats.forEach(cat => {
    options.push({ value: `c_${cat}`, label: `[分类节点] ${cat}` });
  });

  // Skills
  graphNodes.value.forEach(skill => {
    options.push({ value: `s_${skill.id}`, label: `[技能节点] ${skill.title}` });
  });

  return options;
});

const currentParentLabel = computed(() => {
  if (!form.parentValue) return '请选择父级节点（必选）';
  const opt = parentOptions.value.find(o => o.value === form.parentValue);
  return opt ? opt.label : '请选择父级节点';
});

const selectParent = (val) => {
  form.parentValue = val;
  isSelectOpen.value = false;
};

const buildGraphOption = () => {
  const skills = graphNodes.value;
  const originalLinks = graphLinks.value;
  const categories = Array.from(new Set(skills.map((skill) => skill.category))).map((name) => ({ name }));
  const hasFixedCoords = skills.length > 0 && skills.every((skill) => skill.xAxis !== null && skill.yAxis !== null);

  // 为每个 category 创建虚拟根节点
  const finalCategories = [...categories];

  const data = skills.map((skill) => ({
    id: String(skill.id),
    name: skill.title,
    category: skill.category,
    value: skill.category,
    x: hasFixedCoords ? skill.xAxis : undefined,
    y: hasFixedCoords ? skill.yAxis : undefined,
    draggable: true,
    symbolSize: skill.pinned ? 80 : 65, // 稍微增大尺寸
    skillId: skill.id,
    version: skill.version ?? 0,
    description: skill.description || ''
  }));

  const links = originalLinks.map((link) => ({
    source: String(link.source),
    target: String(link.target)
  }));

  // 如果没有层级关系，人为地为孤立技能点连接其所属的类别虚拟节点
  categories.forEach((cat, idx) => {
    const catNodeId = `cat-${idx}`;
    data.push({
      id: catNodeId,
      name: cat.name,
      category: cat.name,
      value: cat.name,
      draggable: true,
      symbolSize: 100, // 增大分类节点尺寸
      itemStyle: { borderColor: '#ffffff', borderWidth: 2, shadowBlur: 10, shadowColor: 'rgba(255,255,255,0.3)' },
      label: { show: true, fontSize: 16, fontWeight: 'bold' }
    });

    skills.filter(s => s.category === cat.name).forEach(skill => {
      // 若该节点没有作为子节点被连接过，则连接到类别节点
      if (!originalLinks.some(l => l.target == skill.id || l.source == skill.id)) {
        links.push({
          source: catNodeId,
          target: String(skill.id)
        });
      }
    });

    // 各大分类相连（形成中心网）
    if(idx > 0) {
        links.push({
            source: 'cat-0',
            target: catNodeId
        });
    }
  });

  return {
    backgroundColor: 'transparent',
    tooltip: {
      confine: true,
      extraCssText: 'max-width: 300px; white-space: normal; word-break: break-word;',
      formatter: (params) => {
        if (params.dataType !== 'node') return '';
        return params.data.description || params.data.name;
      }
    },
    legend: [
      {
        data: categories.map((item) => item.name),
        textStyle: { color: '#e5e7eb' }
      }
    ],
    series: [
      {
        type: 'graph',
        layout: hasFixedCoords ? 'none' : 'force',
        roam: true,
        data,
        links: links.map((link) => ({
          source: String(link.source),
          target: String(link.target)
        })),
        categories,
        label: {
          show: true,
          color: '#e5e7eb'
        },
        lineStyle: {
          color: 'rgba(148, 163, 184, 0.5)',
          width: 2,
          curveness: 0.1 // 增加一点点曲线感，显得不那么生硬
        },
        force: {
          repulsion: 2500, // 大幅增加排斥力，防止拥挤
          edgeLength: [150, 300], // 增加边长范围
          gravity: 0.02 // 降低引力，让其更发散
        },
        emphasis: {
          focus: 'adjacency'
        }
      }
    ]
  };
};

const renderGraph = () => {
  if (!chartRef.value) return;
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }
  chartInstance.setOption(buildGraphOption(), true);
  bindChartEvents();
};

const bindChartEvents = () => {
  if (!chartInstance) return;
  chartInstance.off('click');
  chartInstance.off('mouseup');

  chartInstance.on('click', (params) => {
    if (params.dataType !== 'node') return;
    // 排除虚拟类别节点
    if (String(params.data.id).startsWith('cat-')) return;

    const skillId = params.data.skillId;
    const skill = graphSkills.value.get(skillId);
    if (!skill) return;
    openDrawer(skill);
  });

  chartInstance.on('mouseup', (params) => {
    if (params.dataType !== 'node') return;
    queueCoordUpdate(params.data);
  });
};

const queueCoordUpdate = (node) => {
  if (!node || node.skillId == null) return;
  coordQueue.value.set(node.skillId, {
    id: node.skillId,
    xAxis: node.x,
    yAxis: node.y,
    version: node.version ?? 0
  });
  if (coordTimer) {
    clearTimeout(coordTimer);
  }
  coordTimer = setTimeout(() => {
    flushCoords();
  }, 1200);
};

const flushCoords = async () => {
  if (!coordQueue.value.size) return;
  const updates = Array.from(coordQueue.value.values());
  coordQueue.value.clear();
  try {
    const res = await axios.put('/api/skills/coords', updates);
    if (res.data.code === 0) {
      updates.forEach((update) => {
        const skill = graphSkills.value.get(update.id);
        if (skill) {
          skill.xAxis = update.xAxis;
          skill.yAxis = update.yAxis;
          skill.version = (skill.version ?? 0) + 1;
        }
      });
    } else {
      window.alert(res.data.message || '坐标保存失败');
      fetchGraph();
    }
  } catch (err) {
    window.alert('坐标保存失败');
    fetchGraph();
  }
};

const fetchGraph = async () => {
  loading.value = true;
  // 移除 disposal 逻辑，除非确实需要重置
  try {
    const res = await axios.get('/api/skills/graph');
    if (res.data.code === 0) {
      graphNodes.value = res.data.data.nodes || [];
      graphLinks.value = res.data.data.links || [];
      graphSkills.value = new Map(
        graphNodes.value.map((skill) => [skill.id, skill])
      );

      // 只有在实例不存在时初始化
      if (!chartInstance && chartRef.value) {
        chartInstance = echarts.init(chartRef.value);
      }
      if (chartInstance) {
        chartInstance.setOption(buildGraphOption(), true);
        bindChartEvents();
      }
    }
  } finally {
    loading.value = false;
  }
};

const openDrawer = (skill) => {
  selectedSkill.value = skill;
  drawerOpen.value = true;
  postPage.value = 1;
  fetchPosts();
  axios.post(`/api/skills/${skill.id}/visit`).catch(() => null);
};

const closeDrawer = () => {
  drawerOpen.value = false;
};

const editSkill = () => {
  editingSkill.value = selectedSkill.value;
  form.title = selectedSkill.value.title;
  form.description = selectedSkill.value.description;
  form.parentValue = selectedSkill.value.parentId ? `s_${selectedSkill.value.parentId}` : `c_${selectedSkill.value.category}`;
  showEditor.value = true;
};

const deleteSkill = async () => {
  if (confirm(`确定要删除技能 "${selectedSkill.value.title}" 吗？该操作不可恢复！`)) {
    try {
      await axios.delete(`/api/skills/${selectedSkill.value.id}`);
      drawerOpen.value = false;
      fetchGraph();
    } catch (e) {
      alert('删除失败，可能有子节点');
    }
  }
};

const fetchPosts = async () => {
  if (!selectedSkill.value) return;
  postLoading.value = true;
  try {
    const res = await axios.get(`/api/skills/${selectedSkill.value.id}/posts`, {
      params: {
        page: postPage.value,
        size: 6
      }
    });
    if (res.data.code === 0) {
      posts.value = res.data.data.records || [];
      postTotalPages.value = res.data.data.pages || 1;
    }
  } finally {
    postLoading.value = false;
  }
};

const changePostPage = (nextPage) => {
  postPage.value = nextPage;
  fetchPosts();
};

const openPost = (id) => {
  drawerOpen.value = false;
  router.push(`/posts/${id}`);
};

const openCreate = () => {
  resetForm();
  // 默认父级节点为空
  form.parentValue = '';
  showEditor.value = true;
};

const resetForm = () => {
  editingSkill.value = null;
  form.title = '';
  form.description = '';
  form.parentValue = '';
};

const closeEditor = () => {
  showEditor.value = false;
};

const submitSkill = async () => {
  if (!form.parentValue || !form.title.trim()) {
    window.alert('请选择父级节点并输入技能名称');
    return;
  }

  let categoryStr = '';
  let parentIdNum = null;
  if (form.parentValue.startsWith('c_')) {
    categoryStr = form.parentValue.substring(2);
  } else if (form.parentValue.startsWith('s_')) {
    parentIdNum = Number(form.parentValue.substring(2));
    const parentSkill = graphNodes.value.find(s => s.id === parentIdNum);
    categoryStr = parentSkill ? parentSkill.category : '基础技能';
  }

  const payload = {
    category: categoryStr,
    title: form.title,
    description: form.description,
    parentId: parentIdNum
  };

  if (editingSkill.value && editingSkill.value.id) {
    await axios.put(`/api/skills/${editingSkill.value.id}`, payload);
  } else {
    // 新增逻辑：若 payload 中 parentId 为空，且 category 不在 predefinedCategories 中，
    // 可能需要作为根节点连接
    await axios.post('/api/skills', payload);
  }

  // 提交后重置表单并关闭
  resetForm();
  closeEditor();

  // 关键：fetchGraph 后需要确保 layout 重新计算或位置正确
  await fetchGraph();
};

onMounted(() => {
  fetchGraph();
  resizeHandler = () => {
    if (chartInstance) {
      chartInstance.resize();
    }
  };
  window.addEventListener('resize', resizeHandler);
  document.addEventListener('click', handleClickOutside);
});

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler);
  }
  document.removeEventListener('click', handleClickOutside);
  if (coordTimer) {
    clearTimeout(coordTimer);
  }
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
});
</script>

<style scoped>
.skills-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.2rem;
  margin-bottom: 1.2rem;
}

.subtitle {
  margin: 0.4rem 0 0;
  color: #94a3b8;
  font-size: 0.9rem;
}

.header-actions {
  display: flex;
  gap: 0.6rem;
}

.graph-card {
  background: radial-gradient(circle at top left, #1e293b, #020617);
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.3);
  padding: 1rem;
  min-height: 720px;
  position: relative;
}

.graph-canvas {
  width: 100%;
  height: 700px;
  transition: opacity 0.3s;
}

.loading-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
}

.empty-hint {
  text-align: center;
  color: #94a3b8;
  margin-top: 1.4rem;
}

.drawer {
  position: fixed;
  right: 1.5rem;
  top: 6.5rem;
  width: 360px;
  max-height: calc(100vh - 9rem);
  background: rgba(2, 6, 23, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 1rem;
  padding: 1.2rem;
  overflow-y: auto;
  box-shadow: 0 18px 50px rgba(2, 6, 23, 0.7);
  z-index: 20;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.8rem;
  margin-bottom: 0.6rem;
}

.drawer-title {
  margin: 0;
}

.drawer-meta {
  margin: 0.2rem 0 0;
  color: #94a3b8;
  font-size: 0.85rem;
}

.drawer-section {
  margin-bottom: 1.2rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 500;
  color: #e5e7eb;
  margin: 0 0 0.6rem;
}

.drawer-desc {
  color: #cbd5f5;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.theory-link {
  color: #38bdf8;
  text-decoration: none;
  font-weight: 500;
}

.theory-link:hover {
  text-decoration: underline;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}

.post-item {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  padding: 0.6rem 0.2rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.post-title {
  font-weight: 600;
  cursor: pointer;
}

.post-summary {
  color: #94a3b8;
  font-size: 0.85rem;
  margin-top: 0.3rem;
}

.drawer-actions {
  display: flex;
  gap: 0.8rem;
  margin-top: 1.5rem;
}

.select-wrapper {
  position: relative;
  width: 100%;
}

.select-input {
  width: 100%;
  padding: 0.8rem;
  background-color: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.4);
  border-radius: 0.8rem;
  color: #e5e7eb;
  font-size: 0.95rem;
  transition: all 0.2s;
  outline: none;
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%239ca3af'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.7rem center;
  background-size: 1rem;
  padding-right: 2.5rem;
  box-sizing: border-box;
  text-align: left;
}

.select-input:hover, .select-input.open {
  border-color: #38bdf8;
  box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.2);
}

.select-options {
  position: absolute;
  top: calc(100% + 0.5rem);
  left: 0;
  width: 100%;
  background: #1e293b;
  border: 1px solid rgba(148, 163, 184, 0.4);
  border-radius: 0.8rem;
  overflow: hidden;
  z-index: 10;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
  box-sizing: border-box;
  animation: fadeIn 0.15s ease-out;
  max-height: 250px;
  overflow-y: auto;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-5px); }
  to { opacity: 1; transform: translateY(0); }
}

.select-option {
  padding: 0.8rem 1rem;
  color: #e5e7eb;
  cursor: pointer;
  transition: all 0.2s;
}

.select-option:hover {
  background-color: rgba(56, 189, 248, 0.15);
  color: #e0f2fe;
}

.btn.danger {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.4);
}

.btn.danger:hover {
  background: rgba(239, 68, 68, 0.3);
}

@media (max-width: 900px) {
  .skills-header {
    flex-direction: column;
  }

  .drawer {
    position: static;
    width: 100%;
    margin-top: 1rem;
    max-height: none;
  }
}

/* 滚动条暗黑主题样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-track {
  background: #1e293b;
  border-radius: 4px;
}
::-webkit-scrollbar-thumb {
  background: #475569;
  border-radius: 4px;
}
::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}

select.input option {
  background-color: #0f172a;
  color: #e5e7eb;
}
</style>
