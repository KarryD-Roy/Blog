import 'github-markdown-css/github-markdown-dark.css'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import MarkdownIt from 'markdown-it'
import markdownItMultimdTable from 'markdown-it-multimd-table'
// 定义匹配数学公式的正则表达式，用于将不同格式的数学公式替换为标记字符串

const MATH_PATTERNS = [
  {
    pattern: /\$\$(.*?)\$\$/gs, // 匹配块级公式 $$公式$$
    replacement: 'MATH_DISPLAY_DOLLAR_OPEN$1MATH_DISPLAY_DOLLAR_CLOSE',
  },
  {
    pattern: /\$(.*?)\$/gs, // 匹配行内公式 $公式$
    replacement: 'MATH_INLINE_DOLLAR_OPEN$1MATH_INLINE_DOLLAR_CLOSE'
  },
  {
    pattern: /\\\[([\s\S]*?)\\]/g, // 匹配块级公式 \[公式\]
    replacement: 'MATH_DISPLAY_BRACKET_OPEN$1MATH_DISPLAY_BRACKET_CLOSE',
  },
  {
    pattern: /\\\(([\s\S]*?)\\\)/g, // 匹配行内公式 \(公式\)
    replacement: 'MATH_INLINE_PAREN_OPEN$1MATH_INLINE_PAREN_CLOSE',
  },
]

// 定义恢复数学公式的正则表达式，用于将标记字符串替换回原始的数学公式语法
const RESTORE_PATTERNS = [
  {
    pattern: /MATH_DISPLAY_DOLLAR_OPEN(.*?)MATH_DISPLAY_DOLLAR_CLOSE/gs,
    replacement: '$$$1$$'
  },
  {
    pattern: /MATH_INLINE_DOLLAR_OPEN(.*?)MATH_INLINE_DOLLAR_CLOSE/gs,
    replacement: '$$1$'
  },
  {
    pattern: /MATH_DISPLAY_BRACKET_OPEN(.*?)MATH_DISPLAY_BRACKET_CLOSE/g,
    replacement: '\\[$1\\]'
  },
  {
    pattern: /MATH_INLINE_PAREN_OPEN(.*?)MATH_INLINE_PAREN_CLOSE/g,
    replacement: '\\($1\\)'
  },
]

/**
 * 保护数学公式，将特定数学公式语法替换为标记字符串
 * 目的：避免Markdown解析器误处理公式中的特殊字符（如#、*、_等）
 * @param {string} content - 待处理的内容
 * @returns {string} 处理后的内容
 */
const protectMathExpressions = (content) => {
  let processedContent = content
  for (const { pattern, replacement } of MATH_PATTERNS) {
    processedContent = processedContent.replace(pattern, replacement)
  }
  return processedContent
}

/**
 * 恢复数学公式，将标记字符串替换回原始数学公式语法
 * 目的：让MathJax能够识别并渲染公式
 * @param {string} content - 待处理的内容
 * @returns {string} 处理后的内容
 */
const restoreMathExpressions = (content) => {
  let processedContent = content
  for (const { pattern, replacement } of RESTORE_PATTERNS) {
    processedContent = processedContent.replace(pattern, replacement)
  }
  return processedContent
}

/**
 * 预处理Markdown文本
 * 策略调整：最小化前端处理，信任后端已完成格式化
 * 只做基本的安全检查和清理，避免与后端处理冲突
 * @param {string} text - 原始Markdown文本
 * @returns {string} 清理后的Markdown文本
 */
const preprocessMarkdown = (text) => {
  if (!text) return ''
  
  let processed = text

  // 开启调试模式时打印原始内容（生产环境应关闭）
  const DEBUG = true
  if (DEBUG) {
    console.group('Markdown预处理（简化版）')
    console.log('原始内容:\n', text)
  }

  // 策略：信任后端已完成格式化，前端只做最基本的清理
  
  // 1. 清理开头和结尾的空白
  processed = processed.trim()
  
  // 2. 清理过多的连续空行（4个及以上换行符压缩为2个）
  // 注意：保持宽松，避免破坏后端的格式化结果
  processed = processed.replace(/\n{4,}/g, '\n\n')

  // 3. 修复列表项符号被异常切分的问题
  // 问题：当加粗文本 **text** 出现在行首时，可能被误识别为列表项
  // 解决方案：在行首的加粗/斜体标记前添加零宽空格，避免被识别为列表符号
  // 匹配模式：换行符后紧跟 * 或 ** 或 *** 开头的加粗/斜体文本
  processed = processed.replace(/\n(\*+)([^\s*])/g, (match, stars, nextChar) => {
    // 只处理加粗/斜体语法（后面紧跟非空格、非星号的字符）
    // 不处理真正的列表项（* 后面通常有空格）
    return '\n\u200B' + stars + nextChar
  })

  // 4. 修复Markdown标题缺少空格的问题（例如 "###准备工作" -> "### 准备工作"）
  processed = processed.replace(/^([ \t]*#{1,6})([^ \t#\n])/gm, '$1 $2')

  // 5. 修复有序列表项粘连在同一行的问题（例如 "步骤：1. 访问... 2. 运行..."）
  // 在标点符号后紧跟的列表项前添加换行
  processed = processed.replace(/([：。！？;:!?])([ \t]*)(\d+\.[ \t]+)/g, '$1\n\n$3')
  // 强行把非行首的 " 数字. " 前面加上换行
  processed = processed.replace(/([^\n])([ \t]+)(\d+\.[ \t]+)/g, '$1\n\n$3')

  // 6. 修复代码块前后缺少换行导致格式混乱甚至解析失败的问题
  // 修复偶尔出现的 ``` 没有独立成行的问题（比如紧跟一段文字： code``` ）
  processed = processed.replace(/([^\n`])([ \t]*```+)(?=\n|$)/g, '$1\n$2')
  // 确保 ``` 前面有完整的空行（适用于代码块开始和结束前，如果之前不是空行的话）
  processed = processed.replace(/([^\n])\n([ \t]*```+[a-zA-Z0-9]*[ \t]*\n)/g, '$1\n\n$2')
  // 确保 ``` 后面有完整的空行（适用于代码块结束后，防止和下一段文字粘连）
  processed = processed.replace(/([ \t]*```+[ \t]*)\n([^\n])/g, '$1\n\n$2')

  if (DEBUG) {
    console.log('处理后内容:\n', processed)
    console.groupEnd()
  }

  return processed
}

/**
 * 核心渲染函数
 * @param {HTMLElement} el - 指令绑定的DOM元素
 * @param {object} bind - 指令绑定信息（包含传入的Markdown内容）
 */
const renderMarkdown = (el, bind) => {
  const { value } = bind

  // 内容为空时清空并退出，避免残留
  if (!value) {
    el.innerHTML = ''
    return
  }

  const md = new MarkdownIt({
    linkify: true,
    typographer: true,
    breaks: true,
    html: true,
  })

  md.use(markdownItMultimdTable, {
    multiline: true,
    rowspan: true,
    colspan: true,
    headerless: true,
    multibody: true,
    autolabel: true,
  })

  // 预处理Markdown文本，修复格式问题
  const preprocessedValue = preprocessMarkdown(value)
  const protectedContent = protectMathExpressions(preprocessedValue)
  let htmlContent = md.render(protectedContent)

  // 使用 DOMPurify 清理 HTML 以避免 XSS
  const safeHtml = DOMPurify.sanitize(htmlContent)

  el.classList.add('markdown-content', 'github-markdown')
  el.innerHTML = safeHtml

  // 增强代码块渲染：添加语言标签和复制按钮
  el.querySelectorAll('pre code').forEach((block) => {
    try {
      // 应用代码高亮
      hljs.highlightElement(block)
      
      // 获取代码块的语言类型
      const classList = Array.from(block.classList);
      const langClass = classList.find(cls => cls.startsWith('language-'));
      const language = langClass ? langClass.replace('language-', '') : 'text';
      
      // 获取父级 pre 元素
      const pre = block.parentElement;
      if (!pre || pre.tagName !== 'PRE') return;
      
      // 检查是否已经添加过包装器（避免重复处理）
      if (pre.parentElement?.classList.contains('code-block-wrapper')) return;
      
      // 创建代码块包装器
      const wrapper = document.createElement('div');
      wrapper.className = 'code-block-wrapper';
      
      // 创建顶部工具栏
      const toolbar = document.createElement('div');
      toolbar.className = 'code-block-toolbar';
      
      // 创建语言标签
      const langLabel = document.createElement('span');
      langLabel.className = 'code-block-lang';
      langLabel.textContent = language.toUpperCase();
      
      // 创建复制按钮
      const copyBtn = document.createElement('button');
      copyBtn.className = 'code-block-copy';
      copyBtn.innerHTML = '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M4 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm0 1a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1H4z"/><path d="M2 6h1v7h7v1H2V6z"/></svg> 复制';
      copyBtn.title = '复制代码';
      
      // 复制功能
      copyBtn.addEventListener('click', async () => {
        const code = block.textContent || '';
        try {
          await navigator.clipboard.writeText(code);
          copyBtn.innerHTML = '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M13.5 2l-7 7-3-3L2 7.5l4.5 4.5L15 3.5z"/></svg> 已复制';
          copyBtn.classList.add('copied');
          setTimeout(() => {
            copyBtn.innerHTML = '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M4 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm0 1a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1H4z"/><path d="M2 6h1v7h7v1H2V6z"/></svg> 复制';
            copyBtn.classList.remove('copied');
          }, 2000);
        } catch (err) {
          console.error('复制失败：', err);
          copyBtn.textContent = '复制失败';
          setTimeout(() => {
            copyBtn.innerHTML = '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M4 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm0 1a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1H4z"/><path d="M2 6h1v7h7v1H2V6z"/></svg> 复制';
          }, 2000);
        }
      });
      
      // 组装工具栏
      toolbar.appendChild(langLabel);
      toolbar.appendChild(copyBtn);
      
      // 将 pre 元素包装到新容器中
      pre.parentNode.insertBefore(wrapper, pre);
      wrapper.appendChild(toolbar);
      wrapper.appendChild(pre);
      
    } catch (error) {
      console.error('代码块增强失败：', error)
    }
  })

  // 恢复数学公式原始语法（避免代码块内替换）
  const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT)
  while (walker.nextNode()) {
    const textNode = walker.currentNode
    let parent = textNode.parentNode
    let shouldSkip = false
    while (parent) {
      if (parent.nodeName === 'CODE' || parent.nodeName === 'PRE') {
        shouldSkip = true
        break
      }
      parent = parent.parentNode
    }
    if (!shouldSkip) {
      textNode.textContent = restoreMathExpressions(textNode.textContent)
    }
  }

  if (window.MathJax) {
    window.MathJax.typesetPromise([el])
      .then(() => {
        // console.log('数学公式渲染完成')
      })
      .catch((error) => {
        console.error('数学公式渲染失败：', error)
      })
  }
}

// 定义Vue3指令
const vRenderMarkdown = {
  // 元素挂载时执行渲染
  mounted(el, bind) {
    renderMarkdown(el, bind)
  },
  // 元素更新时重新渲染（处理内容变化场景）
  updated(el, bind) {
    // 避免重复渲染（当内容未变化时）
    if (bind.value === bind.oldValue) return
    renderMarkdown(el, bind)
  },
  // 元素卸载时清理（可选，根据需求添加）
  unmounted(el) {
    el.replaceChildren()
    el.classList.remove('markdown-content', 'github-markdown')
  }
}

// 导出指令，供全局或局部注册使用
export default vRenderMarkdown
