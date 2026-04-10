<template>
  <div class="sh-landing">
    <canvas ref="canvasRef" class="sh-particle-canvas" />

        <main class="hm-main">
      <div class="hm-brand-row">
        <router-link to="/guide" class="hm-brand-link">
          <PixelIcon name="school" size="sm" class="hm-brand-ico" />
          <span class="hm-brand-text">校园工具</span>
          <span class="hm-brand-cue">功能一览 · 新手指南</span>
        </router-link>
      </div>
      <div class="hm-top hm-hero-row">
        <div class="hm-hero-text">
          <span class="hm-live-badge">LIVE</span>
          <h1>学习实况监视</h1>
          <p class="hm-lead">聚合资料、题库、笔记、待办与广场等动态数据；雷达看结构、折线看趋势，下方弹幕滚动播报关键指标。</p>
          <div class="hm-meta-row">
            <span class="hm-refresh">上次刷新 {{ lastRefresh }}</span>
            <el-button type="primary" size="small" :loading="refreshing" @click="refreshAll">刷新</el-button>
          </div>
        </div>
        <div class="hm-hero-art">
          <PixelHeroBits variant="monitor" />
        </div>
      </div>

      <section class="hm-overview study-glass-block">
        <header class="hm-overview-head">
          <h2 class="hm-overview-title">数据一览</h2>
          <p class="hm-overview-desc">
            数字为当前统计值；下方色条表示该指标在「自动缩放区间」内的相对占比，便于一眼看出哪类数据最多。右侧为当前时钟。
          </p>
        </header>
        <div class="hm-overview-grid">
          <article
            v-for="c in overviewCards"
            :key="c.k"
            class="hm-stat-card"
            :class="`hm-stat-card--${c.k}`"
          >
            <div class="hm-stat-card-top">
              <PixelIcon :name="c.icon" size="sm" class="hm-stat-ico" />
              <span class="hm-stat-label">{{ c.label }}</span>
            </div>
            <div class="hm-stat-num">
              {{ c.v }}<span class="hm-stat-unit">{{ c.unit }}</span>
            </div>
            <div class="hm-stat-bar" role="presentation">
              <span class="hm-stat-bar-fill" :style="{ width: barWidthPct(c.v, c.max) }" />
            </div>
          </article>
          <article class="hm-stat-card hm-stat-card--clock">
            <div class="hm-stat-card-top">
              <PixelIcon name="clock" size="sm" class="hm-stat-ico" />
              <span class="hm-stat-label">当前时间</span>
            </div>
            <div class="hm-clock-compact">
              <svg class="hm-clock-svg" viewBox="0 0 200 200" aria-hidden="true">
                <defs>
                  <linearGradient id="hm-clock-face-home" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color: rgba(30,41,59,0.95)" />
                    <stop offset="100%" style="stop-color: rgba(15,23,42,0.98)" />
                  </linearGradient>
                </defs>
                <circle cx="100" cy="100" r="92" fill="url(#hm-clock-face-home)" stroke="rgba(148,163,184,0.35)" stroke-width="3" />
                <circle cx="100" cy="100" r="87" fill="none" stroke="rgba(56,189,248,0.12)" stroke-width="1" />
                <g v-for="i in 12" :key="'tk' + i" :transform="`rotate(${(i - 1) * 30} 100 100)`">
                  <line x1="100" y1="26" x2="100" y2="34" stroke="rgba(148,163,184,0.55)" stroke-width="2" stroke-linecap="round" />
                </g>
                <g :transform="`rotate(${clockAngles.hour} 100 100)`">
                  <line x1="100" y1="100" x2="100" y2="54" stroke="rgba(241,245,249,0.96)" stroke-width="5" stroke-linecap="round" />
                </g>
                <g :transform="`rotate(${clockAngles.minute} 100 100)`">
                  <line x1="100" y1="100" x2="100" y2="36" stroke="rgba(203,213,225,0.9)" stroke-width="3" stroke-linecap="round" />
                </g>
                <g :transform="`rotate(${clockAngles.second} 100 100)`">
                  <line x1="100" y1="112" x2="100" y2="34" stroke="rgba(56,189,248,0.88)" stroke-width="1.5" stroke-linecap="round" />
                </g>
                <circle cx="100" cy="100" r="6" fill="rgba(15,23,42,0.98)" stroke="rgba(148,163,184,0.4)" stroke-width="1" />
                <rect x="46" y="116" width="108" height="38" rx="6" fill="#020617" stroke="rgba(56,189,248,0.4)" stroke-width="1" />
                <text x="100" y="141" text-anchor="middle" class="hm-digit">{{ digitalTime }}</text>
              </svg>
            </div>
          </article>
        </div>
      </section>

      <div class="hm-charts">
        <div class="hm-chart-card study-glass-block">
          <h3 class="hm-chart-title">六维能力雷达</h3>
          <p class="hm-chart-hint">相对峰值归一化</p>
          <svg class="hm-svg-radar" viewBox="0 0 200 200">
            <polygon
              v-for="(s, gi) in radarGridScales"
              :key="gi"
              :points="radarHexPoints(s)"
              fill="none"
              stroke="rgba(148,163,184,0.22)"
              stroke-width="1"
            />
            <polygon :points="radarDataPoints" fill="rgba(56,189,248,0.15)" stroke="#38bdf8" stroke-width="1.5" />
            <text v-for="(lab, i) in radarLabels" :key="lab" :x="radarLabelXY(i).x" :y="radarLabelXY(i).y" class="hm-radar-t">{{ lab }}</text>
          </svg>
        </div>
        <div class="hm-chart-card study-glass-block">
          <h3 class="hm-chart-title">近 7 日资料上传</h3>
          <svg class="hm-svg-line" viewBox="0 0 320 150">
            <polyline :points="linePolylineAttr" fill="none" stroke="#a78bfa" stroke-width="2" />
            <circle v-for="(pt, i) in linePoints" :key="i" :cx="pt.x" :cy="pt.y" r="3" fill="#e2e8f0" />
            <text v-for="(lb, i) in lineDayLabels" :key="i" :x="22 + i * 42" y="142" class="hm-line-t">{{ lb }}</text>
          </svg>
        </div>
      </div>

      <div class="hm-barrage-wrap" aria-hidden="true">
        <div
          v-for="b in barrageItems"
          :key="b.id"
          class="hm-bullet"
          :style="{ top: b.top + '%', animationDuration: b.sec + 's' }"
        >
          {{ b.text }}
        </div>
      </div>

      <div class="hm-feature-grid">
        <button
          v-for="f in featureMonitorLinks"
          :key="f.path"
          type="button"
          class="hm-feat study-glass-block"
          @click="router.push(f.path)"
        >
          <PixelIcon :name="f.icon" size="sm" class="hm-feat-pix" />
          <span class="hm-feat-name">{{ f.name }}</span>
          <span class="hm-feat-sub">{{ f.hint }}</span>
        </button>
      </div>

      <div class="hm-row2">
        <div class="hm-dash study-glass-block">
          <header class="hm-dash-h">
            <h3>今日课程</h3>
            <el-button type="primary" text size="small" @click="goSchedule">课表</el-button>
          </header>
          <el-empty v-if="!homeTodayCourses.length" description="今日无课或尚未录入课表" />
          <ul v-else class="hm-today">
            <li v-for="c in homeTodayCourses" :key="c.id">
              <span class="t1">{{ slotLabel(c.startSlot, c.endSlot) }}</span>
              <span class="t2">{{ c.name }}</span>
            </li>
          </ul>
        </div>
        <div class="hm-dash study-glass-block">
          <header class="hm-dash-h">
            <h3>公告摘要</h3>
            <el-button type="primary" text size="small" @click="goNotice">全部</el-button>
          </header>
          <ul class="hm-notice">
            <li v-for="n in latestNotices" :key="n.id">
              <span class="ntl">{{ n.typeLabel }}</span>
              <span class="ntt">{{ n.title }}</span>
            </li>
            <li v-if="!latestNotices.length" class="hm-empty">暂无</li>
          </ul>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { materialApi, knowledgeApi, questionApi, noteApi, todoApi, plazaApi } from '../api'
import { getAccentRgbObject } from '../theme'
import PixelHeroBits from '../components/PixelHeroBits.vue'

const router = useRouter()
const canvasRef = ref(null)
const stats = ref({
  materials: 0,
  knowledgePoints: 0,
  questions: 0,
  recentMaterials: 0,
})

const timeline = ref([])
const materialsList = ref([])
const monitorNotes = ref(0)
const monitorTodos = ref(0)
const plazaPostsTotal = ref(0)
const lastRefresh = ref('—')
const refreshing = ref(false)

const radarGridScales = [0.2, 0.4, 0.6, 0.8, 1]
const radarLabels = ['资料', '知识点', '题目', '笔记', '待办', '广场']

function radarHexPoints(scale) {
  const cx = 100
  const cy = 100
  const R = 75 * scale
  const N = 6
  const pts = []
  for (let i = 0; i < N; i += 1) {
    const ang = -Math.PI / 2 + (i * 2 * Math.PI) / N
    pts.push(`${cx + R * Math.cos(ang)},${cy + R * Math.sin(ang)}`)
  }
  return pts.join(' ')
}

function radarLabelXY(i) {
  const cx = 100
  const cy = 100
  const R = 90
  const N = 6
  const ang = -Math.PI / 2 + (i * 2 * Math.PI) / N
  return { x: cx + R * Math.cos(ang) - 14, y: cy + R * Math.sin(ang) + 5 }
}

const radarSeries = computed(() => {
  const raw = [
    stats.value.materials,
    stats.value.knowledgePoints,
    stats.value.questions,
    monitorNotes.value,
    monitorTodos.value,
    Math.min(100, plazaPostsTotal.value),
  ]
  const max = Math.max(...raw, 1)
  return raw.map((x) => Math.min(1, x / max))
})

const radarDataPoints = computed(() => {
  const cx = 100
  const cy = 100
  const R = 75
  const vals = radarSeries.value
  const N = 6
  return vals
    .map((v, i) => {
      const ang = -Math.PI / 2 + (i * 2 * Math.PI) / N
      const r = R * v
      return `${cx + r * Math.cos(ang)},${cy + r * Math.sin(ang)}`
    })
    .join(' ')
})

function dayKey(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const last7DaysKeys = computed(() => {
  const out = []
  const now = new Date()
  for (let i = 6; i >= 0; i -= 1) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    d.setHours(0, 0, 0, 0)
    out.push(dayKey(d))
  }
  return out
})

const lineDayLabels = computed(() => {
  const out = []
  const now = new Date()
  for (let i = 6; i >= 0; i -= 1) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    out.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return out
})

const last7DayCounts = computed(() => {
  const keys = last7DaysKeys.value
  const counts = keys.map(() => 0)
  const list = materialsList.value || []
  list.forEach((m) => {
    if (!m.createTime) return
    const t = new Date(m.createTime)
    if (Number.isNaN(t.getTime())) return
    const k = dayKey(t)
    const idx = keys.indexOf(k)
    if (idx >= 0) counts[idx] += 1
  })
  return counts
})

const linePoints = computed(() => {
  const counts = last7DayCounts.value
  const max = Math.max(...counts, 1)
  const w = 320
  const h = 150
  const padL = 24
  const padR = 16
  const padT = 16
  const padB = 32
  const innerW = w - padL - padR
  const innerH = h - padT - padB
  const n = counts.length
  return counts.map((c, i) => {
    const x = n <= 1 ? padL + innerW / 2 : padL + (i * innerW) / (n - 1)
    const y = padT + innerH - (c / max) * innerH
    return { x, y }
  })
})

const linePolylineAttr = computed(() => linePoints.value.map((p) => `${p.x},${p.y}`).join(' '))

// 首页课表简要（从课程表页面使用的本地存储中读取）
const homeSchedule = ref([])
const days = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' },
]

const slots = [
  { value: 1, label: '第 1-2 节', time: '08:00 - 09:40' },
  { value: 2, label: '第 3-4 节', time: '10:00 - 11:40' },
  { value: 3, label: '第 5-6 节', time: '14:00 - 15:40' },
  { value: 4, label: '第 7-8 节', time: '16:00 - 17:40' },
  { value: 5, label: '第 9-10 节', time: '19:00 - 20:40' },
]

const todayIndex = computed(() => {
  const jsDay = new Date().getDay() || 7
  return days.findIndex((d) => d.value === jsDay)
})

const homeTodayCourses = computed(() => {
  if (!homeSchedule.value.length || todayIndex.value === -1) return []
  const weekday = days[todayIndex.value].value
  return homeSchedule.value
    .filter((c) => c.weekday === weekday)
    .sort((a, b) => a.startSlot - b.startSlot || a.endSlot - b.endSlot)
})

const monitorStatCards = computed(() => [
  { k: 'm', v: stats.value.materials, label: '资料', icon: 'materials' },
  { k: 'k', v: stats.value.knowledgePoints, label: '知识点', icon: 'refine' },
  { k: 'q', v: stats.value.questions, label: '题目', icon: 'questions' },
  { k: 'n', v: monitorNotes.value, label: '笔记', icon: 'notes' },
  { k: 't', v: monitorTodos.value, label: '待办', icon: 'todos' },
  { k: 'c', v: homeTodayCourses.value.length, label: '今日课', icon: 'schedule' },
  { k: 'p', v: plazaPostsTotal.value, label: '广场帖', icon: 'plaza' },
])

const now = ref(new Date())
let clockRafId = 0

function gaugeMaxVal(v) {
  const n = Number(v) || 0
  return Math.max(8, Math.ceil(n * 1.15) + 2)
}

const clockAngles = computed(() => {
  const d = now.value
  const ms = d.getMilliseconds()
  const s = d.getSeconds() + ms / 1000
  const m = d.getMinutes() + s / 60
  const h = (d.getHours() % 12) + m / 60
  return { hour: h * 30, minute: m * 6, second: s * 6 }
})

const digitalTime = computed(() => {
  const d = now.value
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
})

const overviewUnits = { m: '份', k: '条', q: '道', n: '条', t: '项', c: '节', p: '帖' }

const overviewCards = computed(() =>
  monitorStatCards.value.map((c) => ({
    ...c,
    unit: overviewUnits[c.k] || '',
    max: gaugeMaxVal(c.v),
  })),
)

function barWidthPct(value, max) {
  const t = Math.min(100, max > 0 ? (Number(value) / max) * 100 : 0)
  return `${t}%`
}

function clockLoop() {
  now.value = new Date()
  clockRafId = requestAnimationFrame(clockLoop)
}

const featureMonitorLinks = computed(() => [
  { path: '/materials', name: '资料', hint: `${stats.value.materials} 份`, icon: 'materials' },
  { path: '/materials/refine', name: '提炼', hint: '知识要点', icon: 'refine' },
  { path: '/materials/questions', name: '出题', hint: '模拟题', icon: 'questions' },
  { path: '/ask', name: 'AI 问答', hint: '助学', icon: 'spark' },
  { path: '/notes', name: '笔记', hint: `${monitorNotes.value} 条`, icon: 'notes' },
  { path: '/todos', name: '待办', hint: `${monitorTodos.value} 项`, icon: 'todos' },
  { path: '/practice', name: '练习', hint: '答题', icon: 'practice' },
  { path: '/clock', name: '打卡', hint: '时长', icon: 'clock' },
  { path: '/schedule', name: '课表', hint: '课程', icon: 'schedule' },
  { path: '/plaza/discussion', name: '帖子交流', hint: '讨论', icon: 'messages' },
  { path: '/plaza/market', name: '闲置购物', hint: '二手', icon: 'market' },
  { path: '/notice', name: '公告', hint: '通知', icon: 'notice' },
  { path: '/settings', name: '设置', hint: '主题', icon: 'settings' },
])

const barrageItems = ref([])
let barrageId = 0
let barrageTimer = null

function pushBarrage() {
  const pool = [
    `资料 ${stats.value.materials} 份`,
    `知识点 ${stats.value.knowledgePoints} 条`,
    `题目 ${stats.value.questions} 道`,
    `笔记 ${monitorNotes.value} 条`,
    `待办 ${monitorTodos.value} 项`,
    `广场帖子约 ${plazaPostsTotal.value} 条`,
    `今日课程 ${homeTodayCourses.value.length} 节`,
    `近 7 日新上传资料 ${stats.value.recentMaterials} 份`,
  ]
  const text = pool[Math.floor(Math.random() * pool.length)]
  barrageItems.value.push({
    id: (barrageId += 1),
    text,
    top: 10 + Math.random() * 70,
    sec: 11 + Math.random() * 9,
  })
  if (barrageItems.value.length > 18) barrageItems.value.shift()
}

function slotLabel(start, end) {
  const startSlot = slots.find((s) => s.value === start)
  const endSlot = slots.find((s) => s.value === end)
  if (!startSlot || !endSlot) return ''
  if (start === end) return startSlot.label
  return `${startSlot.label} ~ ${endSlot.label}`
}

function loadHomeSchedule() {
  try {
    const raw = localStorage.getItem('sh_schedule')
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

// 首页公告简要（与校园公告模块文案保持一致方向）
const homeNotices = [
  {
    id: 1,
    title: '关于春季学期课程选课与补退选安排的通知',
    type: '教务教学',
    time: '2025-02-20',
  },
  {
    id: 2,
    title: '关于清明节放假及调课安排的通知',
    type: '校历放假',
    time: '2025-03-25',
  },
  {
    id: 3,
    title: '“校园 AI 创新应用大赛”报名通知',
    type: '活动赛事',
    time: '2025-03-10',
  },
  {
    id: 4,
    title: '图书馆期末开放时间延长说明',
    type: '服务提醒',
    time: '2025-06-01',
  },
]

const latestNotices = computed(() =>
  homeNotices
    .slice()
    .sort((a, b) => b.time.localeCompare(a.time))
    .slice(0, 3)
    .map((n) => ({ ...n, typeLabel: n.type })),
)

function goMaterials() {
  router.push('/materials')
}

function goAsk() {
  router.push('/ask')
}

function goSchedule() {
  router.push('/schedule')
}

function goMap() {
  router.push('/map')
}

function goNotice() {
  router.push('/notice')
}

function goProfile() {
  router.push('/profile')
}

function goNotes() {
  router.push('/notes')
}

function goTodos() {
  router.push('/todos')
}

function goReport() {
  router.push('/report')
}

let animationId
let resizeHandler
let mouseMoveHandler

onMounted(() => {
  setupParticles()
  loadStats()
  homeSchedule.value = loadHomeSchedule()
  barrageTimer = setInterval(pushBarrage, 4200)
  pushBarrage()
  clockLoop()
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (clockRafId) cancelAnimationFrame(clockRafId)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (mouseMoveHandler) window.removeEventListener('mousemove', mouseMoveHandler)
  if (barrageTimer) clearInterval(barrageTimer)
})

function setupParticles() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const particles = []
  const maxParticles = 70
  const connectDistance = 130

  const resize = () => {
    const ratio = window.devicePixelRatio || 1
    canvas.width = window.innerWidth * ratio
    canvas.height = window.innerHeight * ratio
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
  }

  resize()
  resizeHandler = resize
  window.addEventListener('resize', resize)

  const createParticle = () => {
    const speed = Math.random() * 0.5 + 0.2
    return {
      x: Math.random() * window.innerWidth,
      y: Math.random() * window.innerHeight,
      vx: (Math.random() - 0.5) * speed,
      vy: (Math.random() - 0.5) * speed,
      size: Math.random() * 2 + 0.6,
      alpha: Math.random() * 0.5 + 0.2,
    }
  }

  for (let i = 0; i < maxParticles; i += 1) {
    particles.push(createParticle())
  }

  const mouse = { x: null, y: null }
  const onMouseMove = (e) => {
    mouse.x = e.clientX
    mouse.y = e.clientY
  }
  mouseMoveHandler = onMouseMove
  window.addEventListener('mousemove', onMouseMove)

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    const ac = getAccentRgbObject()

    particles.forEach((p) => {
      p.x += p.vx
      p.y += p.vy

      if (p.x < -50 || p.x > window.innerWidth + 50) p.vx *= -1
      if (p.y < -50 || p.y > window.innerHeight + 50) p.vy *= -1

      const gradient = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.size * 6)
      gradient.addColorStop(0, `rgba(${ac.r},${ac.g},${ac.b},${p.alpha})`)
      gradient.addColorStop(1, 'transparent')

      ctx.fillStyle = gradient
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size * 4, 0, Math.PI * 2)
      ctx.fill()
    })

    for (let i = 0; i < particles.length; i += 1) {
      for (let j = i + 1; j < particles.length; j += 1) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)

        if (dist < connectDistance) {
          const alpha = 1 - dist / connectDistance
          ctx.strokeStyle = `rgba(148, 163, 184, ${alpha * 0.55})`
          ctx.lineWidth = 0.7
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.stroke()
        }
      }
    }

    if (mouse.x !== null) {
      particles.forEach((p) => {
        const dx = p.x - mouse.x
        const dy = p.y - mouse.y
        const dist = Math.sqrt(dx * dx + dy * dy)

        if (dist < 140) {
          const force = (140 - dist) / 140
          p.x += (dx / dist) * force * 1.2
          p.y += (dy / dist) * force * 1.2
        }
      })
    }

    animationId = requestAnimationFrame(draw)
  }

  draw()
}

async function loadStats() {
  refreshing.value = true
  try {
    const list = (await materialApi.list()) || []
    materialsList.value = list
    stats.value.materials = list.length

    const now = Date.now()
    const sevenDays = 7 * 24 * 60 * 60 * 1000
    stats.value.recentMaterials = list.filter((m) => {
      if (!m.createTime) return false
      const t = new Date(m.createTime).getTime()
      return !Number.isNaN(t) && now - t <= sevenDays
    }).length

    const ids = list.map((m) => m.id).filter(Boolean)
    const batchK = ids.length
      ? Promise.all(ids.map((id) => knowledgeApi.list(id).catch(() => [])))
      : Promise.resolve([])
    const batchQ = ids.length
      ? Promise.all(ids.map((id) => questionApi.list(id).catch(() => [])))
      : Promise.resolve([])

    const [knowledgeRes, questionRes, notesRes, todosRes, plazaData] = await Promise.all([
      batchK,
      batchQ,
      noteApi.list({}).catch(() => []),
      todoApi.list({}).catch(() => []),
      plazaApi.postPage({ page: 1, size: 1 }).catch(() => ({ total: 0 })),
    ])

    if (ids.length) {
      stats.value.knowledgePoints = knowledgeRes.reduce(
        (sum, arr) => sum + (Array.isArray(arr) ? arr.length : 0),
        0,
      )
      stats.value.questions = questionRes.reduce(
        (sum, arr) => sum + (Array.isArray(arr) ? arr.length : 0),
        0,
      )

      const events = []
      list
        .slice()
        .sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
        .slice(0, 3)
        .forEach((m) => {
          events.push({
            time: m.createTime ? new Date(m.createTime).toLocaleString() : '时间未知',
            desc: `上传资料《${m.title || m.fileName || '未命名'}》`,
          })
        })

      questionRes.forEach((arr, index) => {
        if (!Array.isArray(arr) || !arr.length) return
        const material = list[index]
        const q = arr
          .slice()
          .sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))[0]
        events.push({
          time: q.createTime ? new Date(q.createTime).toLocaleString() : '时间未知',
          desc: `为《${material?.title || material?.fileName || '未命名'}》生成了题目`,
        })
      })

      events.sort((a, b) => new Date(b.time) - new Date(a.time))
      timeline.value = events.slice(0, 4)
    } else {
      stats.value.knowledgePoints = 0
      stats.value.questions = 0
      timeline.value = []
    }

    monitorNotes.value = Array.isArray(notesRes) ? notesRes.length : 0
    monitorTodos.value = Array.isArray(todosRes) ? todosRes.length : 0
    plazaPostsTotal.value = typeof plazaData?.total === 'number' ? plazaData.total : 0
    lastRefresh.value = new Date().toLocaleTimeString()
  } catch (e) {
    ElMessage.error(e.message || '加载学习数据失败')
  } finally {
    refreshing.value = false
  }
}

async function refreshAll() {
  await loadStats()
  homeSchedule.value = loadHomeSchedule()
}

</script>

<style scoped>
.sh-landing {
  position: relative;
  min-height: calc(100vh - 0px);
  background:
    radial-gradient(circle at 0% 0%, color-mix(in srgb, var(--neon-accent) 22%, transparent), transparent 55%),
    radial-gradient(circle at 100% 100%, color-mix(in srgb, var(--neon-accent) 16%, transparent), transparent 55%),
    linear-gradient(
      180deg,
      var(--neon-bg-deep) 0%,
      color-mix(in srgb, var(--neon-accent) 10%, var(--neon-bg-mid)) 40%,
      var(--neon-bg-deep) 85%,
      #000 100%
    );
  color: #e5e7eb;
  overflow: hidden;
}

.sh-particle-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.sh-container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 20px;
}

.sh-header {
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(18px);
  background: linear-gradient(
    to bottom,
    var(--glass-96),
    var(--glass-85),
    transparent
  );
  border-bottom: 1px solid rgba(148, 163, 184, 0.35);
}

.sh-header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0 8px;
}

.sh-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.sh-logo-mark {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: radial-gradient(circle at 20% 0, #e0f2fe 0, #0ea5e9 45%, #0369a1 100%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #0b1120;
  box-shadow:
    0 0 0 1px var(--glass-90),
    0 0 40px rgba(56, 189, 248, 0.55);
}

.sh-logo-text {
  font-weight: 600;
  letter-spacing: 0.02em;
  font-size: 14px;
}

.sh-nav {
  display: flex;
  gap: 18px;
  font-size: 13px;
  color: #9ca3af;
}

.sh-nav a {
  position: relative;
  padding: 4px 0;
}

.sh-nav a::after {
  content: '';
  position: absolute;
  inset-inline: 0;
  bottom: -4px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, #38bdf8, #a855f7);
  transform-origin: center;
  transform: scaleX(0);
  transition: transform 0.18s ease-out;
}

.sh-nav a:hover::after {
  transform: scaleX(1);
}

.sh-header-actions {
  display: flex;
  gap: 10px;
}

.sh-hero {
  padding: 56px 0 40px;
}

.sh-hero-inner {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 40px;
  align-items: center;
}

.sh-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.6);
  font-size: 11px;
  color: #9ca3af;
  background: var(--glass-80);
  margin-bottom: 10px;
}

.sh-hero-content h1 {
  font-size: clamp(28px, 4.3vw, 34px);
  line-height: 1.2;
  margin: 0 0 12px;
}

.sh-hero-content h1 span {
  background: linear-gradient(120deg, #e5e7eb, #38bdf8, #a855f7);
  -webkit-background-clip: text;
  color: transparent;
}

.sh-hero-subtitle {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #e5e7eb;
  max-width: 460px;
}

.sh-hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.sh-hero-metas {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 22px;
  font-size: 12px;
}

.sh-hero-metas strong {
  display: block;
  font-size: 16px;
}

.sh-hero-metas span {
  color: #e5e7eb;
}

.sh-hero-panel {
  position: relative;
  min-height: 260px;
}

.sh-hero-orbit {
  position: absolute;
  inset: 8px 18%;
  border-radius: 999px;
  border: 1px dashed rgba(148, 163, 184, 0.5);
  opacity: 0.9;
}

.sh-hero-orbit-inner {
  inset: 32px 30%;
  border-style: solid;
  border-color: rgba(56, 189, 248, 0.9);
}

.sh-hero-entry {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.5);
  background: var(--glass-96);
  color: #e5e7eb;
  font-size: 12px;
  cursor: pointer;
  text-align: left;
  transition:
    transform 0.15s ease-out,
    box-shadow 0.15s ease-out,
    border-color 0.15s ease-out,
    background 0.15s ease-out;
  box-shadow: 0 10px 22px var(--glass-75);
}

.sh-hero-entry:hover {
  transform: translateY(-3px);
  border-color: rgba(56, 189, 248, 0.9);
  box-shadow: 0 16px 35px var(--glass-95);
}

.sh-hero-entry .tag {
  font-size: 11px;
  color: #a5b4fc;
}

.sh-hero-entry .title {
  font-size: 14px;
  font-weight: 600;
}

.sh-hero-entry .desc {
  font-size: 12px;
  color: #cbd5f5;
}

.sh-hero-entry-materials {
  top: 16%;
  left: 12%;
}

.sh-hero-entry-ask {
  top: 10%;
  right: 10%;
}

.sh-hero-entry-schedule {
  bottom: 18%;
  left: 18%;
}

.sh-hero-entry-map {
  bottom: 10%;
  right: 14%;
}

.sh-panel-window {
  position: relative;
  border-radius: 22px;
  padding: 14px 14px 16px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.24), transparent 55%),
    radial-gradient(circle at 100% 100%, rgba(168, 85, 247, 0.18), transparent 55%),
    var(--glass-96);
  box-shadow: 0 20px 45px var(--glass-85);
}

.sh-panel-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 11px;
  color: #9ca3af;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
}
.dot-red {
  background: #f97373;
}
.dot-yellow {
  background: #facc15;
}
.dot-green {
  background: #4ade80;
}

.header-title {
  margin-left: 8px;
}

.sh-panel-body {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.1fr);
  gap: 12px;
}

.sh-panel-cards {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
}

.sh-mini-card {
  border-radius: 14px;
  padding: 10px 12px;
  background: var(--glass-96);
  border: 1px solid rgba(148, 163, 184, 0.4);
  font-size: 12px;
}

.sh-mini-card .label {
  margin: 0 0 4px;
  color: #9ca3af;
}

.sh-mini-card .value {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.sh-mini-card .hint {
  margin: 4px 0 0;
  color: #9ca3af;
}

.sh-timeline {
  border-radius: 14px;
  padding: 10px 12px;
  background: var(--glass-90);
  border: 1px solid rgba(148, 163, 184, 0.4);
  font-size: 12px;
}

.timeline-title {
  margin: 0 0 6px;
  color: #e5e7eb;
}

.sh-timeline ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.sh-timeline li {
  display: flex;
  gap: 8px;
  padding: 4px 0;
}

.sh-timeline .time {
  color: #cbd5f5;
  white-space: nowrap;
}

.sh-timeline .desc {
  color: #e5e7eb;
}

.sh-section {
  padding: 32px 0 36px;
}

.sh-section-quick {
  padding-top: 12px;
}

.sh-section-alt {
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.16), transparent 55%),
    radial-gradient(circle at 100% 100%, rgba(168, 85, 247, 0.16), transparent 55%),
    var(--glass-98);
  border-block: 1px solid rgba(148, 163, 184, 0.25);
}

.sh-section-header h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.sh-section-header p {
  margin: 0;
  font-size: 13px;
  color: #9ca3af;
}

.sh-quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.sh-quick-card {
  border-radius: 16px;
  padding: 12px 12px 14px;
  border: 1px solid rgba(148, 163, 184, 0.55);
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.28), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(37, 99, 235, 0.28), transparent 60%),
    var(--glass-96);
  box-shadow: 0 16px 32px var(--glass-85);
  color: #e5e7eb;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.15s ease-out,
    box-shadow 0.15s ease-out,
    border-color 0.15s ease-out,
    background 0.15s ease-out;
}

.sh-quick-card:hover {
  transform: translateY(-4px);
  border-color: rgba(56, 189, 248, 0.9);
  box-shadow: 0 20px 42px var(--glass-98);
}

.sh-quick-card .badge {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #e0f2fe;
  background-color: rgba(56, 189, 248, 0.28);
  margin-bottom: 6px;
}

.sh-quick-card h3 {
  margin: 0 0 4px;
  font-size: 15px;
}

.sh-quick-card p {
  margin: 0;
  font-size: 13px;
  color: #cbd5f5;
}

.sh-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: flex-start;
}

.sh-dash-card {
  border-radius: 18px;
  padding: 12px 12px 14px;
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.2), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(37, 99, 235, 0.24), transparent 60%),
    var(--glass-92);
  border: 1px solid rgba(148, 163, 184, 0.55);
  box-shadow: 0 18px 40px var(--glass-95);
}

.sh-dash-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.sh-dash-header h3 {
  margin: 0;
  font-size: 15px;
}

.sh-dash-header .hint {
  font-size: 12px;
  color: #9ca3af;
}

.timeline-empty {
  font-size: 12px;
  color: #9ca3af;
}

.sh-today-list {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
}

.sh-today-item {
  display: flex;
  gap: 8px;
  padding: 4px 0;
}

.sh-today-item .time {
  color: #cbd5f5;
  white-space: nowrap;
}

.sh-today-item .name {
  font-weight: 600;
}

.sh-today-item .meta {
  margin-top: 2px;
  color: #9ca3af;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.inline-tip {
  font-size: 12px;
  color: #9ca3af;
}

.sh-notice-list {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
}

.sh-notice-item + .sh-notice-item {
  margin-top: 6px;
}

.sh-notice-item .line1 {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: #9ca3af;
}

.sh-notice-item .type {
  color: #a5b4fc;
}

.sh-notice-item .title {
  margin-top: 2px;
  color: #e5e7eb;
}

.sh-notice-empty {
  font-size: 12px;
  color: #9ca3af;
}

[data-animate] {
  opacity: 0;
  transform: translateY(26px);
  transition: opacity 0.7s ease, transform 0.7s ease;
}

[data-animate='fade-down'] {
  transform: translateY(-20px);
}

[data-animate].is-visible {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 960px) {
  .sh-hero-inner {
    grid-template-columns: minmax(0, 1fr);
  }

  .sh-hero-panel {
    order: -1;
  }

  .sh-dashboard {
    grid-template-columns: minmax(0, 1fr);
  }

  .sh-quick-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .sh-nav {
    display: none;
  }
}

@media (max-width: 768px) {
  .sh-hero {
    padding-top: 40px;
  }
}

/* 实况监视首页 */
.hm-main {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px 18px 48px;
}

.hm-brand-row {
  margin-bottom: 14px;
}

.hm-brand-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 8px 14px;
  border-radius: 12px;
  text-decoration: none;
  color: #e2e8f0;
  background: color-mix(in srgb, var(--neon-accent) 10%, rgba(15, 23, 42, 0.45));
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 35%, transparent);
  transition:
    background 0.15s ease,
    border-color 0.15s ease,
    transform 0.12s ease;
}

.hm-brand-link:hover {
  background: color-mix(in srgb, var(--neon-accent) 16%, rgba(15, 23, 42, 0.5));
  border-color: color-mix(in srgb, var(--el-color-primary) 50%, transparent);
  transform: translateY(-1px);
}

.hm-brand-ico {
  flex-shrink: 0;
  opacity: 0.95;
}

.hm-brand-text {
  font-size: 16px;
  font-weight: 700;
  color: #f8fafc;
}

.hm-brand-cue {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.hm-top {
  margin-bottom: 20px;
}

.hm-live-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: #fecaca;
  background: rgba(239, 68, 68, 0.35);
  border: 1px solid rgba(248, 113, 113, 0.5);
  margin-bottom: 10px;
  animation: hm-pulse 2s ease-in-out infinite;
}

@keyframes hm-pulse {
  0%,
  100% {
    opacity: 0.85;
  }
  50% {
    opacity: 1;
  }
}

.hm-main h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #f8fafc;
}

.hm-lead {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.65;
  color: rgba(203, 213, 225, 0.95);
  max-width: 52em;
}

.hm-meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hm-refresh {
  font-size: 12px;
  color: #94a3b8;
}

.hm-overview {
  padding: 16px 16px 18px;
  border-radius: 16px;
  margin-bottom: 18px;
}

.hm-overview-head {
  margin-bottom: 14px;
}

.hm-overview-title {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 600;
  color: #f1f5f9;
}

.hm-overview-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.55;
  color: #94a3b8;
  max-width: 56em;
}

.hm-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(148px, 1fr));
  gap: 12px;
  align-items: stretch;
}

.hm-stat-card {
  padding: 12px 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(15, 23, 42, 0.35);
  border-left: 3px solid color-mix(in srgb, var(--el-color-primary) 55%, transparent);
}

.hm-stat-card--m {
  border-left-color: #38bdf8;
}
.hm-stat-card--k {
  border-left-color: #a78bfa;
}
.hm-stat-card--q {
  border-left-color: #f472b6;
}
.hm-stat-card--n {
  border-left-color: #34d399;
}
.hm-stat-card--t {
  border-left-color: #fbbf24;
}
.hm-stat-card--c {
  border-left-color: #22d3ee;
}
.hm-stat-card--p {
  border-left-color: #fb923c;
}

.hm-stat-card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.hm-stat-ico {
  flex-shrink: 0;
  opacity: 0.9;
}

.hm-stat-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.hm-stat-num {
  font-size: 26px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: #f8fafc;
  line-height: 1.15;
  margin-bottom: 10px;
  letter-spacing: -0.02em;
}

.hm-stat-unit {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
}

.hm-stat-bar {
  height: 6px;
  border-radius: 999px;
  background: rgba(30, 41, 59, 0.85);
  overflow: hidden;
}

.hm-stat-bar-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, color-mix(in srgb, var(--el-color-primary) 70%, #38bdf8), #38bdf8);
  transition: width 0.35s ease;
}

.hm-stat-card--clock {
  border-left-color: rgba(148, 163, 184, 0.45);
  grid-column: span 1;
}

@media (min-width: 720px) {
  .hm-stat-card--clock {
    grid-column: span 2;
  }
}

.hm-clock-compact {
  display: flex;
  justify-content: center;
}

.hm-clock-svg {
  width: min(240px, 100%);
  max-height: 200px;
  height: auto;
  display: block;
  filter: drop-shadow(0 6px 18px rgba(0, 0, 0, 0.3));
}

.hm-digit {
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', monospace;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.12em;
  fill: #38bdf8;
}

.hm-charts {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}

@media (max-width: 900px) {
  .hm-charts {
    grid-template-columns: 1fr;
  }
}

.hm-chart-card {
  padding: 14px;
  border-radius: 14px;
}

.hm-chart-title {
  margin: 0 0 4px;
  font-size: 15px;
  color: #f1f5f9;
}

.hm-chart-hint {
  margin: 0 0 8px;
  font-size: 11px;
  color: #64748b;
}

.hm-svg-radar {
  width: 100%;
  max-width: 280px;
  height: auto;
  display: block;
  margin: 0 auto;
}

.hm-radar-t {
  font-size: 9px;
  fill: #94a3b8;
}

.hm-svg-line {
  width: 100%;
  height: auto;
  display: block;
}

.hm-line-t {
  font-size: 9px;
  fill: #64748b;
}

.hm-barrage-wrap {
  position: relative;
  height: 120px;
  margin-bottom: 16px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--glass-45);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.hm-bullet {
  position: absolute;
  left: 100%;
  white-space: nowrap;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #e2e8f0;
  background: rgba(56, 189, 248, 0.2);
  border: 1px solid rgba(56, 189, 248, 0.35);
  animation: hm-barrage linear forwards;
}

@keyframes hm-barrage {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(calc(-100vw - 200px));
  }
}

.hm-feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.hm-feat {
  text-align: left;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  border: none;
  color: inherit;
  font: inherit;
}

.hm-feat-name {
  display: block;
  font-weight: 600;
  color: #f1f5f9;
  margin-bottom: 4px;
}

.hm-feat-sub {
  font-size: 12px;
  color: #94a3b8;
}

.hm-row2 {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

@media (max-width: 768px) {
  .hm-row2 {
    grid-template-columns: 1fr;
  }
}

.hm-dash {
  padding: 14px;
  border-radius: 14px;
}

.hm-dash-h {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.hm-dash-h h3 {
  margin: 0;
  font-size: 15px;
  color: #f1f5f9;
}

.hm-today {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 13px;
}

.hm-today li {
  display: flex;
  gap: 10px;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.hm-today .t1 {
  color: #94a3b8;
  white-space: nowrap;
}

.hm-today .t2 {
  color: #e5e7eb;
}

.hm-notice {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
}

.hm-notice li {
  padding: 6px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.hm-notice .ntl {
  display: block;
  color: #a5b4fc;
  margin-bottom: 2px;
}

.hm-notice .ntt {
  color: #cbd5e1;
}

.hm-empty {
  color: #64748b;
  font-size: 12px;
}
</style>

