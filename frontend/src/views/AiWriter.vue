<template>
  <div class="page ai-writer-page">
    <div class="page-header">
      <h1><span class="text">AI 写作助手</span></h1>
      <p class="subtitle">输入素材，选择风格，让 AI 为你生成初稿</p>
    </div>

    <div class="content-wrapper">
      <div class="input-section glass-panel">
        <label for="materials">写作素材 / 核心观点:</label>
        <textarea
          id="materials"
          v-model="materials"
          placeholder="在此输入你想写的内容要点、参考资料或草稿片段..."
        ></textarea>

        <div class="controls">
          <div class="control-group">
            <label>写作风格:</label>
            <div class="custom-select-wrapper" ref="selectWrapper">
              <div class="custom-select" :class="{ open: isSelectOpen }" @click="isSelectOpen = !isSelectOpen">
                {{ currentStyleLabel }}
              </div>
              <div class="custom-options" v-show="isSelectOpen">
                <div class="custom-option"
                     v-for="opt in styleOptions"
                     :key="opt.value"
                     :class="{ active: style === opt.value }"
                     @click="selectStyle(opt.value)">
                  {{ opt.label }}
                </div>
              </div>
            </div>
          </div>
          <button class="btn primary" @click="generateDraft" :disabled="loading || !materials.trim()">
            {{ loading ? '深度思考中...' : '生成草稿' }}
          </button>
        </div>
      </div>

      <div class="output-section glass-panel" :class="{ maximized: isMaximized }" v-if="draft || loading || thinkingProcess">
        <div class="section-header">
          <h3>生成结果</h3>
          <div class="meta-info" v-if="totalTime && !loading">
             <span class="time-tag">耗时: {{ totalTime }}</span>
          </div>
          <div class="actions" v-if="draft">
            <button class="btn small" @click="toggleMaximize">
              {{ isMaximized ? '还原' : '放大' }}
            </button>
            <button class="btn small" @click="copyToClipboard">复制</button>
            <button class="btn primary small" @click="saveAsPost">存为文章</button>
          </div>
        </div>

        <div v-if="thinkingProcess" class="thinking-box">
            <details :open="loading">
                <summary>深度思考过程 (点击展开/收起)</summary>
                <div class="thinking-content markdown-body" v-render-markdown="thinkingProcess">
                </div>
            </details>
        </div>

        <div v-if="loading && !draft" class="loading-placeholder">
          <div class="spinner"></div>
          <p>AI 正在进行深度思考与撰写...</p>
        </div>

        <div v-else class="markdown-preview">
            <div class="preview-mode markdown-body" v-render-markdown="draft"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
// Import the custom directive
import vRenderMarkdown from '../directives/vRenderMarkdown';

const router = useRouter();
const materials = ref('');
const style = ref('technical');
const draft = ref('');
const thinkingProcess = ref('');
const totalTime = ref('');
const loading = ref(false);
const isMaximized = ref(false);

// Custom select logic
const isSelectOpen = ref(false);
const selectWrapper = ref(null);

const styleOptions = [
  { value: 'technical', label: '技术硬核 (Technical)' },
  { value: 'tutorial', label: '教程指南 (Tutorial)' },
  { value: 'storytelling', label: '叙事风格 (Storytelling)' },
  { value: 'academic', label: '学术严谨 (Academic)' },
  { value: 'casual', label: '轻松随笔 (Casual)' }
];

const currentStyleLabel = computed(() => {
  const opt = styleOptions.find(o => o.value === style.value);
  return opt ? opt.label : '选择风格';
});

const selectStyle = (value) => {
  style.value = value;
  isSelectOpen.value = false;
};

const handleClickOutside = (event) => {
  if (selectWrapper.value && !selectWrapper.value.contains(event.target)) {
    isSelectOpen.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});

// 提取思考过程和正文
const extractThinkAndMain = (text) => {
  if (!text) return { think: '', main: '' };

  // 使用更强大的正则兼容可能的格式
  const thinkRegex = /<think>(.*?)<\/think>/gis;
  const encodedThinkRegex = /&lt;think&gt;(.*?)&lt;\/think&gt;/gis;

  let think = '';
  let main = text;

  // 尝试完整匹配
  let match = thinkRegex.exec(text) || encodedThinkRegex.exec(text);

  if (match) {
    think = match[1];
    main = text.replace(match[0], '');
    return { think, main };
  }

  // 处理流式输出中不完整的闭合标签
  const openRegex = /(?:<think>|&lt;think&gt;)/i;
  const openMatch = openRegex.exec(text);

  if (openMatch) {
    const openIndex = openMatch.index;
    const openLength = openMatch[0].length;

    // 如果没有找到闭合标签，说明<think>还没结束
    think = text.slice(openIndex + openLength);
    main = text.slice(0, openIndex);
  }

  return { think, main };
};

const toggleMaximize = () => {
  isMaximized.value = !isMaximized.value;
  if (isMaximized.value) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
};

const generateDraft = async () => {
  if (!materials.value.trim()) return;

  loading.value = true;
  draft.value = '';
  thinkingProcess.value = '';
  totalTime.value = '0.0s';
  isMaximized.value = false;
  let fullText = '';

  // Start timer
  const startTime = Date.now();
  const timerInterval = setInterval(() => {
    const elapsed = (Date.now() - startTime) / 1000;
    totalTime.value = elapsed.toFixed(1) + 's';
  }, 100);

  try {
    const materialsList = materials.value.split(/\n\n+/).filter(m => m.trim());

    // Use fetch for streaming
    const response = await fetch('/api/ai/write', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        materials: materialsList,
        style: style.value
      })
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    let buffer = '';

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        buffer += chunk;

        // 按照 SSE 规范，通过双换行符分割事件
        const events = buffer.split(/\r?\n\r?\n/);
        // 保留最后一个可能不完整的块
        buffer = events.pop() || '';

        for (const event of events) {
            // 每个事件可能包含多行 data:
            const lines = event.split(/\r?\n/);
            const dataLines = [];

            for (const line of lines) {
                // 忽略空行
                if (!line.trim()) continue;

                // 检查是否以 data: 开头
                if (line.trim().startsWith('data:')) {
                    // 找到 data: 的位置（兼容前面可能有空格）
                    const dataIndex = line.indexOf('data:');
                    let content = line.slice(dataIndex + 5);

                    // 如果紧跟着一个空格，去除它（SSE 规范）
                    if (content.startsWith(' ')) {
                        content = content.slice(1);
                      }
                      dataLines.push(content);
                }
            }

            if (dataLines.length > 0) {
                // 合并多行数据，用换行符连接
                const joinedData = dataLines.join('\n');

                if (joinedData.trim() === '[DONE]') continue;

                // 拼接到全文
                fullText += joinedData;

                // 更新 UI
                const { think, main } = extractThinkAndMain(fullText);
                thinkingProcess.value = think;
                draft.value = main; // 渲染前自动修复
            }
        }
    }

    const finalParts = extractThinkAndMain(fullText);
    thinkingProcess.value = finalParts.think;
    draft.value = finalParts.main;

  } catch (err) {
    console.error(err);
    alert('请求出错，请重试');
  } finally {
    clearInterval(timerInterval);
    loading.value = false;
  }
};


const copyToClipboard = () => {
  navigator.clipboard.writeText(draft.value).then(() => {
    alert('已复制到剪贴板');
  });
};

const saveAsPost = () => {
    sessionStorage.setItem('ai_draft_content', draft.value);
    router.push('/');
};
</script>

<style scoped>
.ai-writer-page {
  max-width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 150px);
}

.page-header {
  text-align: center;
  margin-bottom: 2rem;
  flex-shrink: 0;
  border-bottom: 4px solid #333;
  padding-bottom: 1.5rem;
}
.page-header h1 {
  font-family: 'Syne', sans-serif;
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  text-transform: uppercase;
}
.page-header h1 .text {
  color: #ccff00;
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
}
.page-header h1 .text::before {
  content: '';
  width: 0.55em;
  height: 0.55em;
  background: #ccff00;
  transform: rotate(45deg);
  box-shadow: 0 0 12px rgba(204, 255, 0, 0.5);
}
.subtitle {
  color: #a1a1aa;
  font-family: 'JetBrains Mono', monospace;
  font-size: 1rem;
  text-transform: uppercase;
}

.content-wrapper {
  display: flex;
  gap: 2rem;
  flex: 1;
  min-height: 0;
}

.glass-panel {
  background: #09090b;
  border: 2px solid #333;
  border-radius: 0;
  padding: 1.5rem;
  box-shadow: 8px 8px 0 rgba(204, 255, 0, 0.2);
  transition: all 0.2s ease;
}

.glass-panel:hover {
  border-color: #ccff00;
  box-shadow: 8px 8px 0 rgba(204, 255, 0, 0.4);
}

.input-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.output-section {
  flex: 1.2;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  position: relative;
}

.output-section.maximized {
  position: fixed;
  top: 1.5rem;
  left: 1.5rem;
  right: 1.5rem;
  bottom: 1.5rem;
  z-index: 1000;
  background: #09090b;
  border: 4px solid #ccff00;
  box-shadow: 16px 16px 0 rgba(204, 255, 0, 0.4);
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 800;
  color: #ccff00;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
}

textarea#materials {
  flex: 1;
  width: 100%;
  padding: 1rem;
  background-color: transparent;
  border: 2px solid #333;
  border-radius: 0;
  resize: none;
  font-family: 'Manrope', sans-serif;
  color: #fafafa;
  font-size: 1rem;
  margin-bottom: 1.5rem;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

textarea#materials:focus {
  outline: none;
  border-color: #ccff00;
  box-shadow: 4px 4px 0 rgba(204, 255, 0, 0.2);
}

.controls {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
}

.control-group {
  flex-grow: 1;
}

.custom-select-wrapper {
  position: relative;
  width: 100%;
}

.custom-select {
  width: 100%;
  padding: 0.8rem;
  background-color: transparent;
  border: 2px solid #333;
  border-radius: 0;
  color: #fafafa;
  font-family: ' जेटBrains Mono', monospace;
  font-size: 0.95rem;
  transition: all 0.2s;
  outline: none;
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%23333'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.7rem center;
  background-size: 1rem;
  padding-right: 2.5rem;
  box-sizing: border-box;
  text-align: left;
}

.custom-select:hover, .custom-select.open {
  border-color: #ccff00;
}

.custom-options {
  position: absolute;
  top: calc(100% + 0.5rem);
  left: 0;
  width: 100%;
  background: #09090b;
  border: 2px solid #ccff00;
  border-radius: 0;
  overflow: hidden;
  z-index: 10;
  box-shadow: 4px 4px 0 rgba(204, 255, 0, 0.4);
  box-sizing: border-box;
  animation: slideIn 0.15s ease-out;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-5px); }
  to { opacity: 1; transform: translateY(0); }
}

.custom-option {
  padding: 1rem;
  color: #a1a1aa;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid #333;
}

.custom-option:last-child {
  border-bottom: none;
}

.custom-option:hover {
  background-color: #ccff00;
  color: #09090b;
  padding-left: 1.5rem;
}

.custom-option.active {
  background-color: #ccff00;
  color: #09090b;
  font-weight: 800;
}

.btn {
  padding: 0.8rem 1.5rem;
  border: 2px solid #333;
  border-radius: 0;
  cursor: pointer;
  font-family: 'JetBrains Mono', monospace;
  font-weight: bold;
  text-transform: uppercase;
  transition: all 0.2s;
  white-space: nowrap;
  background: transparent;
  color: #fafafa;
}

.primary {
  background: #ccff00;
  color: #09090b;
  border-color: #ccff00;
}

.primary:hover {
  transform: translate(-4px, -4px);
  box-shadow: 4px 4px 0 rgba(255, 255, 255, 0.2);
}
.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(100%);
  transform: none;
  box-shadow: none;
}

.btn.small {
  padding: 0.4rem 0.9rem;
  font-size: 0.85rem;
}
.btn.small:hover {
  background-color: #ccff00;
  color: #09090b;
  border-color: #ccff00;
}

.actions .primary.small {
  background: #ccff00;
  color: #09090b;
  border-color: #ccff00;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-shrink: 0;
}

.time-tag {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
  color: #09090b;
  background: #ccff00;
  padding: 0.2rem 0.6rem;
  border-radius: 0;
  font-weight: bold;
}

.thinking-box {
  margin-bottom: 1rem;
  background: transparent;
  border-radius: 0;
  border: 2px solid #333;
  overflow: hidden;
  flex-shrink: 0;
}

details > summary {
  padding: 10px;
  background: transparent;
  cursor: pointer;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.9rem;
  color: #a1a1aa;
  user-select: none;
  text-transform: uppercase;
}
details > summary:hover {
  color: #fafafa;
}

.thinking-content {
  padding: 15px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.9rem;
  color: #666;
  background: #000;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
  border-top: 2px solid #333;
}

.loading-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: #a1a1aa;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #333;
  border-left-color: #ccff00;
  border-radius: 0;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.markdown-preview {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  background: transparent;
  border-radius: 0;
  border: 2px solid #333;
  color: #fafafa;
  line-height: 1.7;
}

:deep(.markdown-content) {
  color: #fafafa;
  background: transparent;
  font-family: 'Manrope', sans-serif;
}

:deep(.markdown-content a) {
  color: #ccff00;
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3),
:deep(.markdown-content h4),
:deep(.markdown-content h5),
:deep(.markdown-content h6) {
  font-family: 'Syne', sans-serif;
  color: #fafafa;
  border-bottom: 2px solid #333;
}

:deep(.markdown-content p),
:deep(.markdown-content li),
:deep(.markdown-content td),
:deep(.markdown-content th) {
  color: #d4d4d8;
}

:deep(.markdown-content code) {
  background-color: #333;
  color: #ccff00;
  padding: 2px 4px;
  border-radius: 0;
  border: 1px solid #666;
  font-family: 'JetBrains Mono', monospace;
}

:deep(.markdown-content pre) {
  background-color: #000;
  border: 2px solid #333;
  padding: 1rem;
  border-radius: 0;
  overflow-x: auto;
}

:deep(.markdown-content pre code) {
  background: transparent;
  padding: 0;
  border: none;
  color: #e5e7eb;
}

:deep(.markdown-content .hljs) {
  background: transparent;
  color: #e5e7eb;
}

/* 代码增强 */
:deep(.code-block-wrapper) {
  position: relative;
  margin: 1.5rem 0;
  border-radius: 0;
  overflow: hidden;
  background: #000;
  border: 2px solid #333;
}

:deep(.code-block-toolbar) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  background: #09090b;
  border-bottom: 2px solid #333;
}

:deep(.code-block-lang) {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  font-weight: 800;
  color: #ccff00;
  text-transform: uppercase;
}

:deep(.code-block-copy) {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.3rem 0.6rem;
  background: transparent;
  border: 1px solid #333;
  border-radius: 0;
  color: #a1a1aa;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
  text-transform: uppercase;
}

:deep(.code-block-copy:hover) {
  background: #ccff00;
  border-color: #ccff00;
  color: #09090b;
}

:deep(.code-block-copy.copied) {
  background: #fafafa;
  border-color: #fafafa;
  color: #09090b;
}

:deep(.code-block-wrapper pre) {
  margin: 0 !important;
  border: none !important;
  border-radius: 0 !important;
}

:deep(.code-block-wrapper pre code) {
  display: block;
  padding: 1rem !important;
  font-family: 'JetBrains Mono', monospace;
}
</style>
