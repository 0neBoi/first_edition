<template>
  <div class="materials-page">
    <canvas ref="bgCanvas" class="materials-canvas" />

    <div class="materials-header materials-hero-row">
      <div class="materials-hero-text">
        <h2 class="page-title">资料空间</h2>
        <p class="page-subtitle">{{ materialsSubtitle }}</p>
      </div>
      <div class="materials-hero-art">
        <PixelHeroBits variant="books" />
      </div>
    </div>

    <el-alert
      v-if="materialPanel"
      class="materials-panel-alert"
      type="info"
      show-icon
      :closable="false"
      :title="panelAlertTitle"
      :description="panelAlertDesc"
    />

    <div class="keyword-layer" v-if="topKeywords.length">
      <span
        v-for="(kw, index) in topKeywords"
        :key="kw"
        class="keyword-bubble"
        :style="bubbleStyle(index, topKeywords.length)"
      >
        {{ kw }}
      </span>
    </div>

    <el-card class="upload-card" shadow="hover">
      <div class="upload-card-header">
        <div class="upload-card-title-row">
          <PixelIcon name="upload" size="md" class="upload-card-pix" />
          <span class="upload-title">快速导入资料</span>
        </div>
        <span class="upload-desc">支持 PDF / Word / TXT，稍后可预览原文并提炼要点。</span>
      </div>
      <el-upload
        class="upload-area"
        drag
        :auto-upload="true"
        :http-request="doUpload"
        :show-file-list="false"
        accept=".txt,.pdf,.docx"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或<em> 点击选择文件 </em></div>
        <template #tip>
          <div class="el-upload__tip">单文件不超过 20MB，暂不支持图片类格式</div>
        </template>
      </el-upload>
    </el-card>

    <div class="materials-table-wrap">
      <el-table :data="list" stripe>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="fileType" label="类型" width="80" />
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.id)">查看 / 提炼 / 出题</el-button>
            <el-button type="danger" link @click="doDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="list.length" class="material-quick-bar">
      <span class="quick-label">快捷进入</span>
      <el-select
        v-model="quickMaterialId"
        placeholder="选择资料"
        filterable
        clearable
        class="quick-select"
      >
        <el-option v-for="m in list" :key="m.id" :label="m.title || m.fileName" :value="m.id" />
      </el-select>
      <el-dropdown trigger="click" @command="onQuickJump">
        <el-button type="primary" :disabled="!quickMaterialId">
          进入详情
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="question">看题目（模拟题目）</el-dropdown-item>
            <el-dropdown-item command="knowledge">看总结（知识要点）</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, ArrowDown } from '@element-plus/icons-vue'
import { materialApi } from '../api'
import PixelHeroBits from '../components/PixelHeroBits.vue'

const router = useRouter()
const route = useRoute()
const list = ref([])
const quickMaterialId = ref(null)

const materialPanel = computed(() => route.meta.materialPanel || null)

const materialsSubtitle = computed(() => {
  if (materialPanel.value === 'refine') {
    return '本页侧重「提炼」：选择资料后进入详情，在知识要点中提炼或浏览要点。'
  }
  if (materialPanel.value === 'questions') {
    return '本页侧重「出题」：选择资料后进入详情，在模拟题目中生成或练习。'
  }
  return '上传课堂资料，统一管理并一键生成知识点与练习题。'
})

const panelAlertTitle = computed(() => {
  if (materialPanel.value === 'refine') return '提炼模式'
  if (materialPanel.value === 'questions') return '出题模式'
  return ''
})

const panelAlertDesc = computed(() => {
  if (materialPanel.value === 'refine') {
    return '在下方表格选择资料，用快捷进入打开「看总结（知识要点）」进入对应 Tab。'
  }
  if (materialPanel.value === 'questions') {
    return '在下方表格选择资料，用快捷进入打开「看题目（模拟题目）」进入对应 Tab。'
  }
  return ''
})
const bgCanvas = ref(null)
let animationId

async function doUpload({ file }) {
  if (!beforeUpload(file)) return
  try {
    await materialApi.upload(file)
    ElMessage.success('上传成功')
    loadList()
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  }
}

function beforeUpload(file) {
  const ok = /\.(txt|pdf|docx)$/i.test(file.name)
  if (!ok) {
    ElMessage.error('仅支持 .txt / .pdf / .docx')
    return false
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('文件不能超过 20MB')
    return false
  }
  return true
}

function onUploadSuccess() {}
function onUploadError() {}

async function loadList() {
  try {
    list.value = await materialApi.list() || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

const topKeywords = computed(() => {
  const freq = {}
  list.value.forEach((item) => {
    const base = `${item.title || ''} ${item.fileName || ''}`
    const text = base
      .replace(/[0-9]/g, ' ')
      .replace(/[_\-\.]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
    if (!text) return
    const parts = text.split(' ').filter((w) => w.length >= 2 && w.length <= 8)
    parts.forEach((p) => {
      freq[p] = (freq[p] || 0) + 1
    })
  })
  return Object.keys(freq)
    .sort((a, b) => freq[b] - freq[a])
    .slice(0, 8)
})

function bubbleStyle(index, total) {
  const cols = 3
  const rows = Math.ceil(total / cols)
  const row = Math.floor(index / cols)
  const col = index % cols

  const topStart = 20
  const topEnd = 75
  const rowGap = rows > 1 ? (topEnd - topStart) / (rows - 1) : 0
  const top = topStart + row * rowGap

  const leftStart = 55
  const leftGap = 12
  const left = leftStart + col * leftGap

  // 为每个气泡设置不同的漂移动画偏移和时长
  const angle = (index / Math.max(total, 1)) * Math.PI * 2
  const distX = 40 + (index % 3) * 10
  const distY = 50 + (index % 4) * 8
  const dx = Math.cos(angle) * distX
  const dy = -Math.abs(Math.sin(angle)) * distY
  const duration = 12 + index * 1.8

  return {
    top: `${top}%`,
    left: `${left}%`,
    '--dx': `${dx}px`,
    '--dy': `${dy}px`,
    animationDuration: `${duration}s, 6s`,
  }
}

function goDetail(id) {
  router.push({ name: 'MaterialDetail', params: { id } })
}

function onQuickJump(tab) {
  if (!quickMaterialId.value) return
  router.push({
    name: 'MaterialDetail',
    params: { id: quickMaterialId.value },
    query: { tab },
  })
}

async function doDelete(row) {
  try {
    await materialApi.delete(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

function setupParticles() {
  const canvas = bgCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const particles = []
  const maxParticles = 40

  const resize = () => {
    const rect = canvas.parentElement.getBoundingClientRect()
    const ratio = window.devicePixelRatio || 1
    canvas.width = rect.width * ratio
    canvas.height = rect.height * ratio
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
  }

  resize()
  window.addEventListener('resize', resize)

  const createParticle = () => {
    const speed = Math.random() * 0.3 + 0.1
    return {
      x: Math.random() * canvas.clientWidth,
      y: Math.random() * canvas.clientHeight,
      vx: (Math.random() - 0.5) * speed,
      vy: (Math.random() - 0.5) * speed,
      size: Math.random() * 2 + 0.6,
      alpha: Math.random() * 0.4 + 0.2,
    }
  }

  for (let i = 0; i < maxParticles; i += 1) {
    particles.push(createParticle())
  }

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    particles.forEach((p) => {
      p.x += p.vx
      p.y += p.vy

      if (p.x < -20 || p.x > canvas.clientWidth + 20) p.vx *= -1
      if (p.y < -20 || p.y > canvas.clientHeight + 20) p.vy *= -1

      ctx.fillStyle = `rgba(56, 189, 248, ${p.alpha})`
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      ctx.fill()
    })

    animationId = requestAnimationFrame(draw)
  }

  draw()
}

watch(
  [list, materialPanel],
  () => {
    if (!list.value.length) return
    if (!quickMaterialId.value) quickMaterialId.value = list.value[0].id
  },
)

onMounted(() => {
  loadList()
  setupParticles()
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
})
</script>

<style scoped>
.materials-page {
  position: relative;
  padding: 24px;
  overflow: hidden;
  /* 不铺死色块，避免遮住主区霓虹/俄罗斯方块底 */
  background: transparent;
}
.materials-page::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 0 0, color-mix(in srgb, var(--neon-accent) 22%, transparent), transparent 50%),
    radial-gradient(circle at 100% 100%, color-mix(in srgb, var(--neon-accent) 18%, transparent), transparent 55%),
    var(--neon-bg-deep);
  opacity: 0.52;
}

.materials-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.materials-header {
  position: relative;
  z-index: 2;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: #e5e7eb;
}

.page-subtitle {
  margin: 0;
  font-size: 13px;
  color: #cbd5f5;
}

.materials-panel-alert {
  position: relative;
  z-index: 2;
  margin-top: 12px;
  max-width: 640px;
}

.keyword-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.keyword-bubble {
  position: absolute;
  padding: 24px 40px;
  border-radius: 999px;
  font-size: 20px;
  color: #ecfeff;
  border: 1px solid rgba(125, 211, 252, 0.7);
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.3), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(168, 85, 247, 0.35), transparent 55%),
    var(--glass-90);
  box-shadow:
    0 0 0 1px var(--glass-90),
    0 10px 30px var(--glass-90),
    0 0 30px rgba(56, 189, 248, 0.5);
  text-shadow: 0 0 8px rgba(56, 189, 248, 0.8);
  animation: float 14s ease-in-out infinite, glow 6s ease-in-out infinite;
}

@keyframes float {
  0% { transform: translate(0, 0); opacity: 0.9; }
  50% { transform: translate(var(--dx, 0px), var(--dy, -40px)); opacity: 1; }
  100% { transform: translate(0, 0); opacity: 0.9; }
}

@keyframes glow {
  0%, 100% { box-shadow:
    0 0 0 1px var(--glass-90),
    0 10px 30px var(--glass-90),
    0 0 25px rgba(56, 189, 248, 0.45); }
  50% { box-shadow:
    0 0 0 1px var(--glass-90),
    0 14px 40px var(--glass-98),
    0 0 40px rgba(56, 189, 248, 0.9); }
}

.upload-card {
  position: relative;
  z-index: 2;
  margin-top: 16px;
  max-width: 640px;
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.22), transparent 65%),
    radial-gradient(circle at 100% 100%, rgba(168, 85, 247, 0.24), transparent 60%),
    var(--glass-75);
  border: 1px solid rgba(148, 163, 184, 0.45);
  box-shadow: 0 18px 40px var(--glass-80);
}

.upload-card-header {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 8px;
}

.upload-card-title-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.upload-card-pix {
  flex-shrink: 0;
}

.upload-title {
  font-size: 14px;
  font-weight: 600;
  color: #e5e7eb;
}

.upload-desc {
  font-size: 12px;
  color: #cbd5f5;
}

.upload-area {
  margin-top: 8px;
}

.materials-table-wrap {
  position: relative;
  z-index: 2;
  margin-top: 20px;
  max-width: 640px;
}

:deep(.el-table) {
  margin-top: 24px;
  background-color: var(--glass-96);
  color: #e5e7eb;
}

:deep(.el-table__header),
:deep(.el-table__body) {
  background-color: transparent;
}

:deep(.el-table th) {
  background-color: var(--glass-98);
}

:deep(.el-table tr) {
  background-color: transparent;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: var(--glass-90);
}

:deep(.el-table td),
:deep(.el-table th) {
  border-bottom: 1px solid rgba(148, 163, 184, 0.35);
}

.material-quick-bar {
  position: relative;
  z-index: 2;
  margin-top: 16px;
  max-width: 640px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 12px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--glass-75);
}

.quick-label {
  font-size: 13px;
  color: #94a3b8;
}

.quick-select {
  flex: 1;
  min-width: 180px;
  max-width: 280px;
}
</style>
