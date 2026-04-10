<template>
  <div class="notice-page">
    <canvas ref="bgCanvas" class="bg-canvas" />

    <div class="inner">
      <header class="header">
        <div>
          <h2>校园公告中心</h2>
          <p class="sub">
            汇总学校与学院的重要通知、活动预告与教务安排，支持按类型筛选与重点公告高亮展示。
          </p>
        </div>
        <div class="toolbar">
          <el-select
            v-model="filterType"
            size="small"
            class="toolbar-select"
            placeholder="全部类型"
            clearable
          >
            <el-option
              v-for="t in types"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            size="small"
            range-separator="至"
            start-placeholder="起始日期"
            end-placeholder="结束日期"
            class="toolbar-date"
          />
          <el-input
            v-model="keyword"
            size="small"
            placeholder="搜索标题 / 内容关键字"
            clearable
            class="toolbar-search"
          />
        </div>
      </header>

      <section class="layout">
        <aside class="side panel-glass">
          <h3 class="side-title">置顶公告</h3>
          <el-scrollbar class="side-list">
            <div
              v-for="n in pinnedNotices"
              :key="n.id"
              :class="['side-item', { active: n.id === currentId }]"
              @click="selectNotice(n.id)"
            >
              <div class="side-item-top">
                <span class="pill">置顶</span>
                <span class="type">{{ typeLabel(n.type) }}</span>
              </div>
              <div class="title">{{ n.title }}</div>
              <div class="meta">
                <span>{{ n.time }}</span>
                <span class="from">{{ n.from }}</span>
              </div>
            </div>
            <div v-if="!pinnedNotices.length" class="empty-tip">
              暂无置顶公告。
            </div>
          </el-scrollbar>
        </aside>

        <main class="main">
          <el-card class="panel-glass list-card">
            <header class="list-header">
              <h3>全部公告</h3>
              <div class="list-meta">
                <span>共 {{ filteredNotices.length }} 条</span>
              </div>
            </header>

            <el-scrollbar class="list-scroll">
              <div
                v-for="n in filteredNotices"
                :key="n.id"
                :class="['list-item', { active: n.id === currentId }]"
                @click="selectNotice(n.id)"
              >
                <div class="list-item-main">
                  <div class="title-row">
                    <span class="title">{{ n.title }}</span>
                    <span v-if="n.pinned" class="flag pinned">置顶</span>
                    <span v-if="!isRead(n.id)" class="flag new">未读</span>
                  </div>
                  <div class="info-row">
                    <span class="type">{{ typeLabel(n.type) }}</span>
                    <span class="from">{{ n.from }}</span>
                    <span class="time">{{ n.time }}</span>
                  </div>
                </div>
                <div class="preview">
                  {{ n.preview }}
                </div>
              </div>
              <div v-if="!filteredNotices.length" class="empty-tip">
                暂无符合条件的公告。
              </div>
            </el-scrollbar>
          </el-card>

          <el-card v-if="currentNotice" class="panel-glass detail-card">
            <header class="detail-header">
              <div>
                <div class="detail-tags">
                  <span v-if="currentNotice.pinned" class="flag pinned">置顶</span>
                  <span class="detail-type">{{ typeLabel(currentNotice.type) }}</span>
                </div>
                <h3 class="detail-title">{{ currentNotice.title }}</h3>
                <div class="detail-meta">
                  <span>{{ currentNotice.time }}</span>
                  <span>发布单位：{{ currentNotice.from }}</span>
                </div>
              </div>
              <el-button
                size="small"
                type="primary"
                text
                @click="toggleRead(currentNotice.id)"
              >
                {{ isRead(currentNotice.id) ? '标记为未读' : '标记为已读' }}
              </el-button>
            </header>

            <el-scrollbar class="detail-body">
              <p
                v-for="(p, idx) in currentNotice.paragraphs"
                :key="idx"
                class="detail-paragraph"
              >
                {{ p }}
              </p>
            </el-scrollbar>
          </el-card>
        </main>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { campusAnnouncementApi } from '../../api'
import { resolveCssVarColor } from '../../theme'

const bgCanvas = ref(null)
let animationId

/** 前端展示用结构（由接口数据映射） */
const notices = ref([])

function mapRow(row) {
  const content = row.content || ''
  const oneLine = content.replace(/\s+/g, ' ').trim()
  const paragraphs = content
    .split(/\n/)
    .map((s) => s.trim())
    .filter(Boolean)
  const timeStr =
    row.publishDate ||
    (row.createTime && String(row.createTime).slice(0, 10)) ||
    ''
  return {
    id: row.id,
    title: row.title,
    type: row.noticeType || 'teaching',
    time: timeStr,
    from: row.publisher || '教务处',
    pinned: !!row.pinned,
    preview: oneLine.length > 120 ? `${oneLine.slice(0, 120)}…` : oneLine || '（无摘要）',
    paragraphs: paragraphs.length ? paragraphs : [oneLine || '（暂无正文）'],
  }
}

async function loadNotices() {
  try {
    const list = await campusAnnouncementApi.publicList()
    notices.value = Array.isArray(list) ? list.map(mapRow) : []
    if (notices.value.length) {
      const exists = notices.value.some((n) => n.id === currentId.value)
      if (!exists) currentId.value = notices.value[0].id
    } else {
      currentId.value = null
    }
  } catch (e) {
    ElMessage.error(e.message || '加载公告失败')
    notices.value = []
    currentId.value = null
  }
}

const types = [
  { value: 'teaching', label: '教务教学' },
  { value: 'calendar', label: '校历放假' },
  { value: 'activity', label: '活动赛事' },
  { value: 'service', label: '服务提醒' },
  { value: 'security', label: '安全提示' },
]

const filterType = ref('')
const dateRange = ref(null)
const keyword = ref('')
const currentId = ref(null)
const readIds = ref(new Set())

const pinnedNotices = computed(() => notices.value.filter((n) => n.pinned))

const filteredNotices = computed(() => {
  let list = notices.value.slice().sort((a, b) => {
    if (a.pinned && !b.pinned) return -1
    if (!a.pinned && b.pinned) return 1
    return b.time.localeCompare(a.time)
  })

  if (filterType.value) {
    list = list.filter((n) => n.type === filterType.value)
  }

  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    const s = new Date(start).getTime()
    const e = new Date(end).getTime()
    list = list.filter((n) => {
      const t = new Date(n.time).getTime()
      return t >= s && t <= e
    })
  }

  const k = keyword.value.trim().toLowerCase()
  if (k) {
    list = list.filter((n) => {
      const text = `${n.title}${n.preview}${n.from}${typeLabel(n.type)}`.toLowerCase()
      return text.includes(k)
    })
  }

  return list
})

const currentNotice = computed(() => {
  const byId = notices.value.find((n) => n.id === currentId.value)
  return byId || filteredNotices.value[0] || null
})

function typeLabel(t) {
  const found = types.find((x) => x.value === t)
  return found ? found.label : t
}

function selectNotice(id) {
  currentId.value = id
}

function loadReadState() {
  try {
    const raw = localStorage.getItem('notice_read_ids')
    if (raw) {
      const arr = JSON.parse(raw)
      if (Array.isArray(arr)) {
        readIds.value = new Set(arr)
      }
    }
  } catch (_) {
    readIds.value = new Set()
  }
}

function saveReadState() {
  try {
    localStorage.setItem('notice_read_ids', JSON.stringify([...readIds.value]))
  } catch {
    // ignore
  }
}

function isRead(id) {
  return readIds.value.has(id)
}

function toggleRead(id) {
  if (readIds.value.has(id)) {
    readIds.value.delete(id)
  } else {
    readIds.value.add(id)
  }
  saveReadState()
}

onMounted(() => {
  loadReadState()
  loadNotices()
  setupBackground()
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
})

function setupBackground() {
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

  const nodes = []
  const lines = []
  const count = 40

  for (let i = 0; i < count; i += 1) {
    nodes.push({
      x: Math.random() * canvas.clientWidth,
      y: Math.random() * canvas.clientHeight * 0.9,
      r: 2.5 + Math.random() * 2,
      vx: (Math.random() - 0.5) * 0.25,
      vy: (Math.random() - 0.5) * 0.25,
      alpha: 0.4 + Math.random() * 0.4,
    })
  }

  for (let i = 0; i < count; i += 1) {
    for (let j = i + 1; j < count; j += 1) {
      if (Math.random() < 0.12) {
        lines.push({ i, j })
      }
    }
  }

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    const g = ctx.createLinearGradient(0, 0, canvas.clientWidth, canvas.clientHeight)
    g.addColorStop(0, resolveCssVarColor('--glass-98'))
    g.addColorStop(0.4, resolveCssVarColor('--glass-98'))
    g.addColorStop(1, resolveCssVarColor('--neon-bg-mid'))
    ctx.fillStyle = g
    ctx.fillRect(0, 0, canvas.clientWidth, canvas.clientHeight)

    lines.forEach((ln) => {
      const a = nodes[ln.i]
      const b = nodes[ln.j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 260) return
      const alpha = 0.12 + (1 - dist / 260) * 0.25
      ctx.strokeStyle = `rgba(56, 189, 248, ${alpha})`
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(a.x, a.y)
      ctx.lineTo(b.x, b.y)
      ctx.stroke()
    })

    nodes.forEach((n, idx) => {
      n.x += n.vx
      n.y += n.vy
      if (n.x < -40) n.x = canvas.clientWidth + 40
      if (n.x > canvas.clientWidth + 40) n.x = -40
      if (n.y < -40) n.y = canvas.clientHeight * 0.9 + 40
      if (n.y > canvas.clientHeight * 0.9 + 40) n.y = -40

      const pulse = 0.6 + Math.sin((Date.now() / 800 + idx * 0.2)) * 0.3
      ctx.beginPath()
      ctx.fillStyle = `rgba(191, 219, 254, ${n.alpha * pulse})`
      ctx.arc(n.x, n.y, n.r * pulse, 0, Math.PI * 2)
      ctx.fill()
    })

    animationId = requestAnimationFrame(draw)
  }

  draw()
}
</script>

<style scoped>
.notice-page {
  max-width: 1120px;
  margin: 24px auto 32px;
  padding: 0 24px 32px;
  min-height: calc(100vh - 160px);
  position: relative;
}

.bg-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.inner {
  position: relative;
  z-index: 1;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 18px;
}

.header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #e5e7eb;
}

.header .sub {
  margin: 0;
  font-size: 13px;
  color: #cbd5f5;
}

.toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar-select,
.toolbar-date,
.toolbar-search {
  min-width: 160px;
}

.layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(0, 1.8fr);
  gap: 16px;
  align-items: flex-start;
}

.panel-glass {
  border-radius: 18px;
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.22), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(37, 99, 235, 0.25), transparent 60%),
    var(--glass-78);
  border: 1px solid rgba(148, 163, 184, 0.5);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 40px var(--glass-90);
}

.side {
  padding: 12px 12px 14px;
  min-width: 0;
}

.side-title {
  margin: 0 0 6px;
  font-size: 14px;
  color: #e5e7eb;
}

.side-list {
  height: 320px;
}

.side-item {
  padding: 8px 8px 7px;
  border-radius: 12px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.1s ease;
}

.side-item:hover {
  background-color: var(--glass-96);
  transform: translateY(-1px);
}

.side-item.active {
  background-color: rgba(37, 99, 235, 0.36);
}

.side-item-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.pill {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  background-color: rgba(56, 189, 248, 0.2);
  color: #e0f2fe;
}

.side-item .type {
  font-size: 11px;
  color: #cbd5f5;
}

.side-item .title {
  font-size: 13px;
  color: #e5e7eb;
}

.side-item .meta {
  margin-top: 2px;
  font-size: 11px;
  color: #9ca3af;
  display: flex;
  gap: 8px;
}

.side-item .from {
  color: #a5b4fc;
}

.empty-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #9ca3af;
}

.main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-card {
  padding: 10px 10px 12px;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.list-header h3 {
  margin: 0;
  font-size: 15px;
  color: #e5e7eb;
}

.list-meta {
  font-size: 12px;
  color: #9ca3af;
}

.list-scroll {
  max-height: 260px;
}

.list-item {
  padding: 8px 8px 6px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.1s ease;
}

.list-item + .list-item {
  margin-top: 4px;
}

.list-item:hover {
  background-color: var(--glass-96);
  transform: translateY(-1px);
}

.list-item.active {
  background-color: rgba(37, 99, 235, 0.36);
}

.title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.title-row .title {
  font-size: 14px;
  color: #e5e7eb;
}

.flag {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
}

.flag.pinned {
  background-color: rgba(248, 250, 252, 0.08);
  color: #facc15;
  border: 1px solid rgba(250, 204, 21, 0.6);
}

.flag.new {
  background-color: rgba(56, 189, 248, 0.18);
  color: #e0f2fe;
  border: 1px solid rgba(56, 189, 248, 0.6);
}

.info-row {
  font-size: 11px;
  color: #9ca3af;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.info-row .from {
  color: #a5b4fc;
}

.preview {
  margin-top: 2px;
  font-size: 12px;
  color: #cbd5f5;
}

.detail-card {
  padding: 10px 12px 12px;
  min-height: 160px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.detail-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.detail-type {
  font-size: 11px;
  color: #cbd5f5;
}

.detail-title {
  margin: 0;
  font-size: 16px;
  color: #e5e7eb;
}

.detail-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #9ca3af;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-body {
  max-height: 220px;
}

.detail-paragraph {
  margin: 0 0 8px;
  font-size: 13px;
  color: #e5e7eb;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .notice-page {
    padding-inline: 16px;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar {
    justify-content: flex-start;
  }

  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

