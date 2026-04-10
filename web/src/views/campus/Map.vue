<template>
  <div class="campus-page">
    <canvas ref="bgCanvas" class="campus-canvas" />

    <div class="campus-inner">
      <header class="campus-header">
        <div>
          <h2>校园地图与导航</h2>
          <p class="sub">
            快速查看教学楼、图书馆、实验楼、宿舍与生活区的分布，估算步行时间，合理规划行程。
          </p>
        </div>
      </header>

      <section class="campus-layout">
        <aside class="campus-side panel-glass">
          <div class="side-search">
            <el-input
              v-model="keyword"
              placeholder="搜索楼名 / 类型，如 教一、图书馆"
              size="small"
              clearable
            />
            <div class="side-filters">
              <el-tag
                v-for="t in types"
                :key="t.value"
                :type="activeType === t.value ? 'success' : 'info'"
                size="small"
                class="filter-tag"
                @click="toggleType(t.value)"
              >
                {{ t.label }}
              </el-tag>
            </div>
          </div>

          <el-scrollbar class="side-list">
            <div
              v-for="b in filteredBuildings"
              :key="b.id"
              :class="['building-item', { active: b.id === selectedId }]"
              @click="selectBuilding(b.id)"
            >
              <div class="building-name">{{ b.name }}</div>
              <div class="building-meta">
                <span>{{ typeLabel(b.type) }}</span>
                <span v-if="b.openTime">开放：{{ b.openTime }}</span>
              </div>
              <div class="building-desc">{{ b.desc }}</div>
            </div>
            <div v-if="!filteredBuildings.length" class="empty-tip">
              未找到匹配的建筑，请尝试使用不同关键字或类型筛选。
            </div>
          </el-scrollbar>
        </aside>

        <main class="campus-main panel-glass">
          <div class="map-header">
            <div>
              <h3>校园示意图</h3>
              <p class="hint">并非真实比例，仅用于相对位置与导航参考。</p>
            </div>
            <div class="nav-select">
              <span class="nav-label">出发位置：</span>
              <el-select v-model="startKey" size="small" style="width: 160px;">
                <el-option
                  v-for="s in startLocations"
                  :key="s.key"
                  :label="s.label"
                  :value="s.key"
                />
              </el-select>
            </div>
          </div>

          <div class="map-wrapper">
            <div class="map-grid">
              <div class="map-orbit orbit-outer" />
              <div class="map-orbit orbit-inner" />
              <div
                v-for="b in buildings"
                :key="b.id"
                class="map-node"
                :class="['type-' + b.type, { active: b.id === selectedId }]"
                :style="nodeStyle(b)"
                @click="selectBuilding(b.id)"
              >
                <span class="dot" />
                <span class="label">{{ b.short || b.name }}</span>
              </div>
            </div>
          </div>

          <div v-if="currentBuilding" class="nav-info">
            <div class="nav-title">
              <span class="pill">导航信息</span>
              <span>{{ currentBuilding.name }}</span>
            </div>
            <p class="nav-text">
              从「{{ currentStart.label }}」步行到「{{ currentBuilding.name }}」
              预计需要约
              <strong>{{ estimate.walkMinutes }}</strong>
              分钟，约
              <strong>{{ estimate.distanceText }}</strong
              >。建议提前 <strong>5~10</strong> 分钟出门。
            </p>
          </div>
        </main>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { resolveCssVarColor } from '../../theme'

const bgCanvas = ref(null)
let animationId

const buildings = [
  {
    id: 1,
    name: '教一楼',
    short: '教一',
    type: 'teaching',
    x: 24,
    y: 30,
    openTime: '07:30 - 22:30',
    desc: '主要承载公共基础课与数理类课程，教室数量较多。',
  },
  {
    id: 2,
    name: '教二楼',
    short: '教二',
    type: 'teaching',
    x: 38,
    y: 34,
    openTime: '07:30 - 22:30',
    desc: '偏理工科课程，配备多媒体与讨论教室。',
  },
  {
    id: 3,
    name: '图书馆',
    short: '图书馆',
    type: 'library',
    x: 52,
    y: 26,
    openTime: '08:00 - 22:00',
    desc: '自习与资料检索的核心区域，安静学习的首选地点。',
  },
  {
    id: 4,
    name: '实验楼 A',
    short: '实验A',
    type: 'lab',
    x: 64,
    y: 40,
    openTime: '08:00 - 21:30',
    desc: '理工科实验教学与课程设计主要场所。',
  },
  {
    id: 5,
    name: '实验楼 B',
    short: '实验B',
    type: 'lab',
    x: 72,
    y: 54,
    openTime: '08:00 - 21:30',
    desc: '高年级实验与项目实践，部分实验室需要提前预约。',
  },
  {
    id: 6,
    name: '宿舍区东',
    short: '宿舍东',
    type: 'dorm',
    x: 26,
    y: 62,
    openTime: '全天',
    desc: '靠近教学区，步行前往教一/教二较为便利。',
  },
  {
    id: 7,
    name: '宿舍区西',
    short: '宿舍西',
    type: 'dorm',
    x: 16,
    y: 70,
    openTime: '全天',
    desc: '环境相对安静，距离体育场与生活区更近。',
  },
  {
    id: 8,
    name: '体育场',
    short: '操场',
    type: 'sports',
    x: 50,
    y: 70,
    openTime: '06:00 - 22:00',
    desc: '晨跑、晚跑与体育课程集中地，也是活动与迎新典礼场所。',
  },
  {
    id: 9,
    name: '学生生活服务中心',
    short: '生活中心',
    type: 'life',
    x: 60,
    y: 60,
    openTime: '08:00 - 21:00',
    desc: '食堂、超市、快递点与服务大厅集中点。',
  },
  {
    id: 10,
    name: '行政楼',
    short: '行政楼',
    type: 'admin',
    x: 40,
    y: 20,
    openTime: '08:30 - 17:30',
    desc: '校办、教务、人事等行政部门集中办公区域。',
  },
]

const types = [
  { value: 'all', label: '全部' },
  { value: 'teaching', label: '教学楼' },
  { value: 'library', label: '图书馆' },
  { value: 'lab', label: '实验楼' },
  { value: 'dorm', label: '宿舍区' },
  { value: 'life', label: '生活服务' },
  { value: 'sports', label: '体育场地' },
  { value: 'admin', label: '行政楼' },
]

const startLocations = [
  { key: 'dorm_east', label: '宿舍区东', x: 26, y: 62 },
  { key: 'dorm_west', label: '宿舍区西', x: 16, y: 70 },
  { key: 'library', label: '图书馆', x: 52, y: 26 },
  { key: 'life_center', label: '生活服务中心', x: 60, y: 60 },
]

const keyword = ref('')
const activeType = ref('all')
const selectedId = ref(buildings[0].id)
const startKey = ref(startLocations[0].key)

const filteredBuildings = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return buildings.filter((b) => {
    if (activeType.value !== 'all' && b.type !== activeType.value) return false
    if (!k) return true
    const text = `${b.name}${b.short || ''}${typeLabel(b.type)}${b.desc}`.toLowerCase()
    return text.includes(k)
  })
})

const currentBuilding = computed(() =>
  buildings.find((b) => b.id === selectedId.value),
)

const currentStart = computed(() =>
  startLocations.find((s) => s.key === startKey.value) || startLocations[0],
)

const estimate = computed(() => {
  const b = currentBuilding.value
  const s = currentStart.value
  if (!b || !s) return { walkMinutes: '--', distanceText: '--' }
  const dx = b.x - s.x
  const dy = b.y - s.y
  const dist = Math.sqrt(dx * dx + dy * dy)
  const meters = Math.round(dist * 15) // 把坐标系映射为大约 15 米一个单位
  const minutes = Math.max(2, Math.round(meters / 80 + 1))
  return {
    walkMinutes: minutes,
    distanceText: `${meters} 米左右`,
  }
})

function typeLabel(t) {
  const map = {
    teaching: '教学楼',
    library: '图书馆',
    lab: '实验楼',
    dorm: '宿舍区',
    life: '生活服务',
    sports: '体育场地',
    admin: '行政楼',
  }
  return map[t] || t
}

function toggleType(t) {
  activeType.value = t === activeType.value ? 'all' : t
}

function selectBuilding(id) {
  selectedId.value = id
}

function nodeStyle(b) {
  return {
    left: `${b.x}%`,
    top: `${b.y}%`,
  }
}

onMounted(() => {
  setupStarfield()
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
})

function setupStarfield() {
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

  const stars = []
  const count = 160
  for (let i = 0; i < count; i += 1) {
    const isMeteor = i % 9 === 0
    stars.push({
      x: Math.random() * canvas.clientWidth,
      y: Math.random() * canvas.clientHeight,
      r: isMeteor ? 1.6 + Math.random() * 1.2 : Math.random() * 1.1 + 0.4,
      speed: isMeteor ? 1 + Math.random() * 0.8 : 0.1 + Math.random() * 0.3,
      alpha: 0.5 + Math.random() * 0.5,
      twinkle: Math.random() * Math.PI * 2,
      meteor: isMeteor,
    })
  }

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    const grd = ctx.createRadialGradient(
      canvas.clientWidth * 0.3,
      canvas.clientHeight * 0.1,
      0,
      canvas.clientWidth * 0.5,
      canvas.clientHeight * 0.5,
      canvas.clientHeight,
    )
    grd.addColorStop(0, 'rgba(56, 189, 248, 0.45)')
    grd.addColorStop(0.3, resolveCssVarColor('--glass-95'))
    grd.addColorStop(1, 'rgba(0, 0, 0, 1)')
    ctx.fillStyle = grd
    ctx.fillRect(0, 0, canvas.clientWidth, canvas.clientHeight)

    stars.forEach((s, idx) => {
      s.y += s.speed
      if (s.meteor) s.x -= s.speed * 1.1

      if (s.y > canvas.clientHeight + 80 || s.x < -80) {
        s.y = -40 - Math.random() * 60
        s.x = canvas.clientWidth * (0.4 + Math.random() * 0.6)
      }
      s.twinkle += 0.02 + idx * 0.00025
      const twinkleFactor = 0.6 + Math.sin(s.twinkle) * 0.4

      if (s.meteor) {
        const tailLength = canvas.clientWidth * 0.3
        const grad = ctx.createLinearGradient(
          s.x,
          s.y,
          s.x + tailLength,
          s.y - tailLength * 0.65,
        )
        grad.addColorStop(0, `rgba(248, 250, 252, ${0.9 * twinkleFactor})`)
        grad.addColorStop(0.5, `rgba(96, 165, 250, ${0.8 * twinkleFactor})`)
        grad.addColorStop(1, 'transparent')
        ctx.strokeStyle = grad
        ctx.lineWidth = 2.2

        ctx.beginPath()
        ctx.moveTo(s.x, s.y)
        ctx.lineTo(s.x + tailLength, s.y - tailLength * 0.65)
        ctx.stroke()

        ctx.beginPath()
        ctx.fillStyle = `rgba(248, 250, 252, ${0.95 * twinkleFactor})`
        ctx.arc(s.x, s.y, s.r * 1.6, 0, Math.PI * 2)
        ctx.fill()
      } else {
        ctx.beginPath()
        ctx.fillStyle = `rgba(191, 219, 254, ${s.alpha * twinkleFactor})`
        ctx.arc(s.x, s.y, s.r * twinkleFactor, 0, Math.PI * 2)
        ctx.fill()
      }
    })

    animationId = requestAnimationFrame(draw)
  }

  draw()
}
</script>

<style scoped>
.campus-page {
  max-width: 1080px;
  margin: 24px auto 32px;
  padding: 0 24px 32px;
  position: relative;
  min-height: calc(100vh - 160px);
}

.campus-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.campus-inner {
  position: relative;
  z-index: 1;
}

.campus-header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #e5e7eb;
}

.campus-header .sub {
  margin: 0 0 18px;
  font-size: 13px;
  color: #cbd5f5;
}

.campus-layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(0, 1.8fr);
  gap: 16px;
  align-items: flex-start;
}

.panel-glass {
  border-radius: 18px;
  padding: 14px 14px 16px;
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.25), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(37, 99, 235, 0.3), transparent 60%),
    var(--glass-72);
  border: 1px solid rgba(148, 163, 184, 0.5);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 40px var(--glass-90);
}

.campus-side {
  min-width: 0;
}

.side-search {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.side-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.filter-tag {
  cursor: pointer;
}

.side-list {
  height: 320px;
}

.building-item {
  padding: 8px 6px;
  border-radius: 10px;
  margin-bottom: 4px;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.1s ease;
}

.building-item:hover {
  background-color: var(--glass-90);
  transform: translateY(-1px);
}

.building-item.active {
  background-color: rgba(37, 99, 235, 0.3);
}

.building-name {
  font-size: 14px;
  font-weight: 600;
  color: #e5e7eb;
}

.building-meta {
  margin-top: 2px;
  font-size: 11px;
  color: #9ca3af;
  display: flex;
  gap: 8px;
}

.building-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #cbd5f5;
}

.empty-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #9ca3af;
}

.campus-main {
  min-width: 0;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  margin-bottom: 10px;
}

.map-header h3 {
  margin: 0;
  font-size: 16px;
  color: #e5e7eb;
}

.map-header .hint {
  margin: 2px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.nav-select {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-label {
  font-size: 12px;
  color: #cbd5f5;
}

.map-wrapper {
  border-radius: 14px;
  padding: 10px;
  background-color: var(--glass-85);
  border: 1px solid rgba(148, 163, 184, 0.5);
}

.map-grid {
  position: relative;
  height: 260px;
  border-radius: 12px;
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.35), transparent 65%),
    radial-gradient(circle at 100% 100%, rgba(37, 99, 235, 0.4), transparent 60%),
    radial-gradient(circle at 50% 50%, var(--glass-90), rgba(3, 7, 18, 1));
  overflow: hidden;
}

.map-orbit {
  position: absolute;
  inset: 16px 26%;
  border-radius: 999px;
  border: 1px dashed rgba(148, 163, 184, 0.55);
}

.orbit-inner {
  inset: 32px 34%;
  border-style: solid;
  border-color: rgba(56, 189, 248, 0.7);
}

.map-node {
  position: absolute;
  transform: translate(-50%, -50%);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #e5e7eb;
  cursor: pointer;
}

.map-node .dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background-color: #e0f2fe;
  box-shadow: 0 0 14px rgba(56, 189, 248, 0.85);
}

.map-node .label {
  padding: 3px 8px;
  border-radius: 999px;
  background-color: var(--glass-80);
  border: 1px solid rgba(148, 163, 184, 0.6);
  backdrop-filter: blur(6px);
}

.map-node.active .label {
  border-color: rgba(56, 189, 248, 0.9);
  box-shadow: 0 0 18px rgba(56, 189, 248, 0.9);
}

.map-node.type-library .dot {
  background-color: #f97316;
  box-shadow: 0 0 14px rgba(248, 171, 85, 0.9);
}

.map-node.type-dorm .dot {
  background-color: #22c55e;
  box-shadow: 0 0 14px rgba(34, 197, 94, 0.9);
}

.map-node.type-life .dot {
  background-color: #facc15;
  box-shadow: 0 0 14px rgba(250, 204, 21, 0.9);
}

.map-node.type-sports .dot {
  background-color: #a855f7;
  box-shadow: 0 0 14px rgba(168, 85, 247, 0.9);
}

.nav-info {
  margin-top: 10px;
  font-size: 12px;
  color: #cbd5f5;
}

.nav-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.nav-title .pill {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  background-color: rgba(56, 189, 248, 0.2);
  color: #e0f2fe;
}

.nav-text strong {
  color: #e5e7eb;
}

@media (max-width: 768px) {
  .campus-page {
    padding-inline: 16px;
  }

  .campus-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

