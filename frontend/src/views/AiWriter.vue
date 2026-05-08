<template>
  <div class="page ai-writer-page">
    <div class="header">
      <h1>🤖 AI 写作助手</h1>
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
            <select v-model="style">
              <option value="technical">技术硬核 (Technical)</option>
              <option value="tutorial">教程指南 (Tutorial)</option>
              <option value="storytelling">叙事风格 (Storytelling)</option>
              <option value="academic">学术严谨 (Academic)</option>
              <option value="casual">轻松随笔 (Casual)</option>
            </select>
          </div>
          <button class="btn primary" @click="generateDraft" :disabled="loading || !materials.trim()">
            {{ loading ? '深度思考中...' : '✨ 生成草稿' }}
          </button>
        </div>
      </div>

      <div class="output-section glass-panel" :class="{ maximized: isMaximized }" v-if="draft || loading || thinkingProcess">
        <div class="section-header">
          <h3>生成结果</h3>
          <div class="meta-info" v-if="totalTime && !loading">
             <span class="time-tag">⏱️ 耗时: {{ totalTime }}</span>
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
                <summary>🤔 深度思考过程 (点击展开/收起)</summary>
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
import { ref, computed } from 'vue';
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
  padding: 2rem;
  box-sizing: border-box;
  color: #ffffff;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #000000;
}

.header {
  text-align: center;
  margin-bottom: 2rem;
  flex-shrink: 0;
}
.subtitle {
  color: #a0a0a0;
  font-size: 1.1em;
}

.content-wrapper {
  display: flex;
  gap: 2rem;
  flex: 1;
  min-height: 0; /* Important for nested flex scroll */
}

.glass-panel {
    background: rgba(30, 30, 30, 0.6);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 12px;
    padding: 1.5rem;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
}

.input-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.output-section {
  flex: 1.2; /* Give output slightly more space */
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden; /* Prevent container from growing */
}

.output-section.maximized {
  position: fixed;
  top: 1.5rem;
  left: 1.5rem;
  right: 1.5rem;
  bottom: 1.5rem;
  z-index: 1000;
  background: rgba(18, 18, 18, 0.95);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.5);
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #4fc3f7;
}

textarea#materials {
  flex: 1;
  width: 100%;
  padding: 1rem;
  background-color: #2b2b2b;
  border: 1px solid #444;
  border-radius: 8px;
  resize: none;
  font-family: inherit;
  color: #fff;
  font-size: 1rem;
  margin-bottom: 1.5rem;
  box-sizing: border-box;
}

textarea#materials:focus {
    outline: none;
    border-color: #007bff;
    box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.25);
}

.controls {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
}

.control-group {
    flex-grow: 1;
}

select {
  width: 100%;
  padding: 0.8rem;
  background-color: #2b2b2b;
  border: 1px solid #444;
  border-radius: 8px; /* Consistent R angle */
  color: white;
  font-size: 0.95rem;
  transition: all 0.2s;
  outline: none;
}

select:focus {
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.25); /* Blue focus shadow */
}

.btn {
  padding: 0.8rem 1.5rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
  white-space: nowrap;
}

.primary {
  background: linear-gradient(135deg, #007bff, #0056b3);
  color: white;
  box-shadow: 0 2px 4px rgba(0, 123, 255, 0.3);
}

.primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(0, 123, 255, 0.4);
}
.primary:disabled {
    opacity: 0.7;
    cursor: wait;
}

.btn.small {
    padding: 0.4rem 0.8rem;
    font-size: 0.9rem;
    background-color: #444;
    color: #ddd;
}
.btn.small:hover {
    background-color: #555;
    color: #fff;
}
.actions .primary.small {
    background-color: #007bff;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
    flex-shrink: 0;
}

.time-tag {
    font-size: 0.9rem;
    color: #ffa726;
    background: rgba(255, 167, 38, 0.1);
    padding: 2px 8px;
    border-radius: 4px;
}

.thinking-box {
    margin-bottom: 1rem;
    background: #0b0b0b;
    border-radius: 8px;
    border: 1px solid #222;
    overflow: hidden;
    flex-shrink: 0;
}

details > summary {
    padding: 10px;
    background: #0b0b0b;
    cursor: pointer;
    font-size: 0.9rem;
    color: #ffffff;
    user-select: none;
}
details > summary:hover {
    color: #fff;
}

.thinking-content {
    padding: 15px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 0.9rem;
    color: #ffffff;
    background: #0b0b0b;
    white-space: pre-wrap;
    max-height: 200px;
    overflow-y: auto;
    border-top: 1px solid #222;
}

.loading-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem;
    color: #888;
}

.spinner {
    width: 40px;
    height: 40px;
    border: 4px solid rgba(255,255,255,0.1);
    border-left-color: #007bff;
    border-radius: 50%;
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
  background: #0b0b0b;
  border-radius: 8px;
  border: 1px solid #222;
  color: #ffffff;
  line-height: 1.7;
}

:deep(.markdown-content) {
  color: #ffffff;
  background: transparent;
}

:deep(.markdown-content a) {
  color: #7dd3fc;
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3),
:deep(.markdown-content h4),
:deep(.markdown-content h5),
:deep(.markdown-content h6) {
  color: #ffffff;
  border-bottom: 1px solid #2a2a2a;
}

:deep(.markdown-content p),
:deep(.markdown-content li),
:deep(.markdown-content td),
:deep(.markdown-content th) {
  color: #ffffff;
}

:deep(.markdown-content code) {
  background-color: #141414;
  color: #e6e6e6;
  padding: 2px 4px;
  border-radius: 3px;
}

:deep(.markdown-content pre) {
  background-color: #0f111a;
  border: 1px solid #1f1f1f;
  padding: 1rem;
  border-radius: 6px;
  overflow-x: auto;
}

:deep(.markdown-content pre code) {
  background: transparent;
  padding: 0;
}

:deep(.markdown-content .hljs) {
  background: transparent;
  color: #e6e6e6;
}

/* 代码块增强样式 */
:deep(.code-block-wrapper) {
  position: relative;
  margin: 1rem 0;
  border-radius: 8px;
  overflow: hidden;
  background: #0f111a;
  border: 1px solid #1f1f1f;
}

:deep(.code-block-toolbar) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  background: #1a1d2e;
  border-bottom: 1px solid #2a2a2a;
}

:deep(.code-block-lang) {
  font-size: 0.75rem;
  font-weight: 600;
  color: #7dd3fc;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

:deep(.code-block-copy) {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.6rem;
  background: transparent;
  border: 1px solid #3a3a3a;
  border-radius: 4px;
  color: #a0a0a0;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

:deep(.code-block-copy:hover) {
  background: #2a2a2a;
  border-color: #4a4a4a;
  color: #ffffff;
}

:deep(.code-block-copy.copied) {
  background: #1e3a1e;
  border-color: #2d5a2d;
  color: #4ade80;
}

:deep(.code-block-copy svg) {
  width: 14px;
  height: 14px;
}

:deep(.code-block-wrapper pre) {
  margin: 0 !important;
  border: none !important;
  border-radius: 0 !important;
}

:deep(.code-block-wrapper pre code) {
  display: block;
  padding: 1rem !important;
}
</style>
