<template>
  <div class="ask-page">
    <canvas ref="bgCanvas" class="ask-canvas" />

    <div class="ask-inner">
      <header class="ask-header">
        <h2>AI 问答</h2>
        <p class="tip">面向学习资料的智能问答助手，适合做知识梳理、举例说明和考前冲刺。支持 Markdown 与 LaTeX 公式（$…$、$$…$$）。</p>
      </header>

      <section class="ask-main">
        <div class="ask-input-panel">
          <el-input
            v-model="question"
            type="textarea"
            :rows="4"
            placeholder="输入你的问题，如：帮我总结这份资料的 3 个重点，或：请用通俗的例子解释卷积神经网络。"
            maxlength="500"
            show-word-limit
          />
          <el-button type="primary" :loading="loading" @click="ask" class="ask-button">
            提问
          </el-button>
        </div>

        <el-card v-if="answer" class="answer-card" shadow="hover">
          <template #header>回答</template>
          <div class="markdown-answer" v-html="renderedAnswer"></div>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import texmath from 'markdown-it-texmath'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import 'markdown-it-texmath/css/texmath.css'
import { qwenApi } from '../api'
import { resolveCssVarColor } from '../theme'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})
md.use(texmath, {
  engine: katex,
  delimiters: 'dollars',
  katexOptions: {
    throwOnError: false,
    strict: false,
  },
})

const question = ref('')
const answer = ref('')
const loading = ref(false)
const bgCanvas = ref(null)
let animationId

const renderedAnswer = computed(() => {
  const raw = answer.value
  if (raw == null || !String(raw).trim()) return ''
  try {
    return md.render(String(raw))
  } catch (e) {
    console.warn('Markdown/KaTeX render failed', e)
    return escapeHtml(String(raw)).replace(/\n/g, '<br>')
  }
})

function escapeHtml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

async function ask() {
  const q = question.value?.trim()
  if (!q) {
    ElMessage.warning('请输入问题')
    return
  }
  loading.value = true
  answer.value = ''
  try {
    answer.value = await qwenApi.ask(q)
  } catch (e) {
    const msg = e.message || '请求失败'
    if (msg.startsWith('百炼 API 返回为空：') || msg.startsWith('百炼 API 返回为空:')) {
      const content = msg.replace(/^百炼 API 返回为空[：:]?\s*/, '')
      if (content.trim()) {
        answer.value = content.trim()
        ElMessage.info('回答已显示在下方')
        return
      }
    }
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  setupNeuralParticles()
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
})

function setupNeuralParticles() {
  const canvas = bgCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const resize = () => {
    const rect = canvas.parentElement.getBoundingClientRect()
    const ratio = window.devicePixelRatio || 1
    canvas.width = rect.width * ratio
    canvas.height = rect.height * ratio
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
  }

  resize()
  window.addEventListener('resize', resize)

  const nodeCount = 42
  const nodes = []

  for (let i = 0; i < nodeCount; i += 1) {
    const speed = 0.3 + Math.random() * 0.4
    nodes.push({
      x: Math.random() * canvas.clientWidth,
      y: Math.random() * canvas.clientHeight * 0.7,
      vx: (Math.random() - 0.5) * speed,
      vy: (Math.random() - 0.5) * speed,
      r: 2.2 + Math.random() * 2.2,
      phase: Math.random() * Math.PI * 2,
    })
  }

  const maxDist = 220

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    const grd = ctx.createLinearGradient(0, 0, canvas.clientWidth, canvas.clientHeight)
    grd.addColorStop(0, resolveCssVarColor('--glass-40'))
    grd.addColorStop(1, resolveCssVarColor('--glass-60'))
    ctx.fillStyle = grd
    ctx.fillRect(0, 0, canvas.clientWidth, canvas.clientHeight)

    for (let i = 0; i < nodes.length; i += 1) {
      for (let j = i + 1; j < nodes.length; j += 1) {
        const a = nodes[i]
        const b = nodes[j]
        const dx = a.x - b.x
        const dy = a.y - b.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < maxDist) {
          const t = 1 - dist / maxDist
          const alpha = t * 0.55
          ctx.strokeStyle = `rgba(56, 189, 248, ${alpha})`
          ctx.lineWidth = 0.7 * t + 0.2
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.stroke()
        }
      }
    }

    nodes.forEach((n, idx) => {
      n.x += n.vx
      n.y += n.vy
      n.phase += 0.02 + idx * 0.0003

      if (n.x < -40 || n.x > canvas.clientWidth + 40) n.vx *= -1
      if (n.y < -40 || n.y > canvas.clientHeight + 40) n.vy *= -1

      const pulse = 0.6 + Math.sin(n.phase) * 0.4
      const radius = n.r * (0.8 + pulse * 0.4)

      const grad = ctx.createRadialGradient(n.x, n.y, 0, n.x, n.y, radius * 3)
      grad.addColorStop(0, `rgba(59, 130, 246, ${0.9 * pulse})`)
      grad.addColorStop(1, 'transparent')

      ctx.fillStyle = grad
      ctx.beginPath()
      ctx.arc(n.x, n.y, radius * 3, 0, Math.PI * 2)
      ctx.fill()

      ctx.fillStyle = '#e0f2fe'
      ctx.beginPath()
      ctx.arc(n.x, n.y, radius, 0, Math.PI * 2)
      ctx.fill()
    })

    animationId = requestAnimationFrame(draw)
  }

  draw()
}
</script>

<style scoped>
.ask-page {
  position: relative;
  max-width: 980px;
  margin: 24px auto 32px;
  padding: 0 24px 32px;
  min-height: calc(100vh - 160px);
}

.ask-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.ask-inner {
  position: relative;
  z-index: 1;
}

.ask-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #e5e7eb;
}

.ask-header .tip {
  margin: 0 0 18px;
  font-size: 13px;
  color: #cbd5f5;
}

.ask-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ask-input-panel {
  padding: 14px 16px 16px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.18), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(168, 85, 247, 0.22), transparent 60%),
    var(--glass-90);
  box-shadow: 0 18px 40px var(--glass-90);
}

.ask-button {
  margin-top: 10px;
}

.answer-card {
  background-color: var(--glass-94);
  border: 1px solid rgba(148, 163, 184, 0.5);
  color: #e5e7eb;
}

.markdown-answer {
  line-height: 1.65;
  font-size: 15px;
  color: #e5e7eb;
  overflow-x: auto;
}

.markdown-answer :deep(h1),
.markdown-answer :deep(h2),
.markdown-answer :deep(h3) {
  margin: 1em 0 0.5em;
  font-weight: 600;
  color: #f1f5f9;
}

.markdown-answer :deep(h1) {
  font-size: 1.35em;
}
.markdown-answer :deep(h2) {
  font-size: 1.2em;
}
.markdown-answer :deep(h3) {
  font-size: 1.08em;
}

.markdown-answer :deep(p) {
  margin: 0.6em 0;
}

.markdown-answer :deep(ul),
.markdown-answer :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.4em;
}

.markdown-answer :deep(li) {
  margin: 0.25em 0;
}

.markdown-answer :deep(hr) {
  border: none;
  border-top: 1px solid rgba(148, 163, 184, 0.35);
  margin: 1em 0;
}

.markdown-answer :deep(code) {
  font-family: ui-monospace, monospace;
  font-size: 0.9em;
  padding: 0.12em 0.35em;
  border-radius: 4px;
  background: rgba(30, 41, 59, 0.9);
  color: #e2e8f0;
}

.markdown-answer :deep(pre) {
  margin: 0.75em 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: var(--glass-95);
  border: 1px solid rgba(148, 163, 184, 0.25);
  overflow-x: auto;
}

.markdown-answer :deep(pre code) {
  padding: 0;
  background: none;
}

.markdown-answer :deep(a) {
  color: #38bdf8;
}

.markdown-answer :deep(blockquote) {
  margin: 0.6em 0;
  padding-left: 1em;
  border-left: 3px solid rgba(56, 189, 248, 0.5);
  color: #cbd5e1;
}

/* KaTeX：行内与块公式在深色背景下的可读性 */
.markdown-answer :deep(.katex) {
  font-size: 1.05em;
  color: #f1f5f9;
}

.markdown-answer :deep(.katex-display) {
  margin: 1em 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 0.5em 0;
}

.markdown-answer :deep(.katex-display > .katex) {
  color: #f1f5f9;
}

@media (max-width: 768px) {
  .ask-page {
    padding-inline: 16px;
  }
}
</style>
