<template>
  <div class="dino-page">
    <div class="dino-head">
      <h1>小恐龙跑酷</h1>
      <p class="dino-lead">
        空格 / 点击画面或下方「跳跃」按钮；本机最高分进入页面时会自动同步到服务器排行榜（高于服务器记录时）。
      </p>
      <div class="dino-stats">
        <span>本局 <strong>{{ scoreDisplay }}</strong></span>
        <span v-if="hiScore > 0">本机最高 <strong>{{ hiScore }}</strong></span>
        <span v-if="serverBest != null">服务器最佳 <strong>{{ serverBest }}</strong></span>
      </div>
    </div>

    <div class="dino-layout">
      <div class="dino-main">
        <div class="dino-stage-wrap study-glass-block">
          <canvas
            ref="canvasRef"
            class="dino-canvas"
            :width="W * DPR"
            :height="H * DPR"
            @click="onTap"
          />
          <div v-if="!playing && !gameOver" class="dino-overlay">
            <p>按空格、点画面或点「跳跃」开始</p>
          </div>
          <div v-if="gameOver" class="dino-overlay dino-overlay--dead">
            <p>撞到了！得分 {{ scoreDisplay }}</p>
            <el-button type="primary" @click="restart">再来一局</el-button>
          </div>
        </div>
        <div class="dino-touch-bar" aria-label="触摸控制">
          <button type="button" class="touch-btn touch-btn--jump" @pointerdown.prevent="onJumpBtn" @click.prevent="onJumpBtn">
            跳跃
          </button>
        </div>
      </div>

      <aside class="dino-lb study-glass-block">
        <h3 class="dino-lb-title">
          <PixelIcon name="spark" size="sm" class="dino-lb-ico" />
          排行榜
        </h3>
        <p class="dino-lb-sub">按历史最佳分排序（每人一条纪录）</p>
        <p v-if="myRank != null" class="dino-lb-my">我的排名：第 {{ myRank }} 名</p>
        <div v-loading="lbLoading" class="dino-lb-body">
          <el-empty
            v-if="!lbLoading && !displayLbRows.length"
            description="暂无数据，来玩一局吧"
            :image-size="72"
          />
          <ol v-else class="dino-lb-list">
            <li
              v-for="row in displayLbRows"
              :key="String(row.userId) + '-' + row.rank + (row._outside ? '-o' : '')"
              class="dino-lb-row"
              :class="{ 'is-me': isMeRow(row), 'dino-lb-row--outside': row._outside }"
            >
              <span class="dino-lb-rank" :class="{ 'is-top': row.rank <= 3 }">{{ row.rank }}</span>
              <span class="dino-lb-name" :title="row.displayName">
                {{ row.displayName }}
                <span v-if="row._outside" class="dino-lb-out">（未进前 20）</span>
              </span>
              <span class="dino-lb-score">{{ row.score }}</span>
            </li>
          </ol>
        </div>
      </aside>
    </div>

    <p class="dino-hint">本机分数仍保存在浏览器；破纪录时若已登录会写入服务器排行榜（管理员账号不参与）。</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { dinoApi } from '../api'

const HI_KEY = 'sh_dino_hi'

/** 逻辑画布（放大游戏区） */
const W = 960
const H = 320
const DPR = Math.min(2, typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1)

const canvasRef = ref(null)
const playing = ref(false)
const gameOver = ref(false)
const score = ref(0)

const hiScore = ref(0)
const serverBest = ref(null)
const lbRows = ref([])
const lbLoading = ref(false)
const myUserId = ref(null)
/** 含真实全局名次，用于未进前 20 时展示 */
const myEntryRow = ref(null)

const scoreDisplay = computed(() => Math.floor(score.value))

function uidEq(a, b) {
  if (a == null || b == null) return false
  return String(a) === String(b)
}

function isMeRow(row) {
  return myUserId.value != null && uidEq(row.userId, myUserId.value)
}

const displayLbRows = computed(() => {
  const base = lbRows.value.map((r) => ({ ...r, _outside: false }))
  if (myUserId.value == null || !myEntryRow.value) return base
  if (base.some((r) => uidEq(r.userId, myUserId.value))) return base
  return [...base, { ...myEntryRow.value, _outside: true }]
})

const myRank = computed(() => {
  if (myUserId.value == null) return null
  const inList = lbRows.value.find((r) => uidEq(r.userId, myUserId.value))
  if (inList) return inList.rank
  if (myEntryRow.value && uidEq(myEntryRow.value.userId, myUserId.value)) return myEntryRow.value.rank
  return null
})

let ctx = null
let raf = 0
let speed = 6
let frame = 0
let obstacles = []
let nextSpawn = 90

const groundY = H - 88
let dinoY = groundY
let dinoVy = 0
/** 恐龙与仙人掌整体放大 */
const dinoW = 72
const dinoH = 66
const dinoX = 100
const gravity = 0.78
const jumpV = -13

function loadHi() {
  try {
    const n = parseInt(localStorage.getItem(HI_KEY) || '0', 10)
    hiScore.value = Number.isFinite(n) ? n : 0
  } catch {
    hiScore.value = 0
  }
}

function saveHi(s) {
  if (s > hiScore.value) {
    hiScore.value = s
    try {
      localStorage.setItem(HI_KEY, String(s))
    } catch {
      /* ignore */
    }
  }
}

async function loadLeaderboard() {
  lbLoading.value = true
  myEntryRow.value = null
  try {
    const data = await dinoApi.leaderboard(20)
    lbRows.value = Array.isArray(data) ? data : []
    try {
      const raw = localStorage.getItem('user')
      if (raw) {
        const u = JSON.parse(raw)
        if (u.role !== 'ADMIN') {
          const entry = await dinoApi.myEntry()
          myEntryRow.value = entry ?? null
        }
      }
    } catch {
      myEntryRow.value = null
    }
  } catch {
    lbRows.value = []
    myEntryRow.value = null
  } finally {
    lbLoading.value = false
  }
}

async function loadServerBest() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) {
      serverBest.value = null
      return
    }
    const u = JSON.parse(raw)
    if (u.role === 'ADMIN') {
      serverBest.value = null
      return
    }
    const n = await dinoApi.myBest()
    serverBest.value = n == null || n === '' ? null : Number(n)
  } catch {
    serverBest.value = null
  }
}

async function submitServerScore(s) {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) return
    const u = JSON.parse(raw)
    if (u.role === 'ADMIN') return
    await dinoApi.submitBest(s)
    await loadLeaderboard()
    await loadServerBest()
  } catch {
    /* 离线或未登录 */
  }
}

/** 进入页面时：若本机最高分高于服务器记录，自动上传以便上榜 */
async function syncLocalBestIfNeeded() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) return
    const u = JSON.parse(raw)
    if (u.role === 'ADMIN') return
    const local = hiScore.value
    const srv = serverBest.value ?? 0
    if (local > srv) {
      await dinoApi.submitBest(local)
      await loadServerBest()
      await loadLeaderboard()
    }
  } catch {
    /* ignore */
  }
}

function onJumpBtn() {
  jump()
}

function readMyUserId() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) {
      myUserId.value = null
      return
    }
    const u = JSON.parse(raw)
    const id = u.userId
    myUserId.value = id != null && id !== '' ? Number(id) : null
  } catch {
    myUserId.value = null
  }
}

function resetGame() {
  score.value = 0
  speed = 4
  frame = 0
  obstacles = []
  nextSpawn = 70 + Math.floor(Math.random() * 40)
  dinoY = groundY
  dinoVy = 0
  gameOver.value = false
}

function restart() {
  resetGame()
  playing.value = true
  loop()
}

function jump() {
  if (!playing.value || gameOver.value) {
    if (!gameOver.value) {
      playing.value = true
      resetGame()
      loop()
    }
    return
  }
  if (dinoY >= groundY - 0.5) {
    dinoVy = jumpV
  }
}

function onTap() {
  jump()
}

function onKey(e) {
  if (e.code === 'Space' || e.key === ' ') {
    e.preventDefault()
    jump()
  }
}

function spawnObstacle() {
  const tall = Math.random() > 0.55
  const h = tall ? 48 : 32
  const w = tall ? 26 : 32
  obstacles.push({ x: W + 30, w, h, passed: false })
}

function hit(ax, ay, aw, ah, bx, by, bw, bh) {
  return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by
}

function update() {
  frame += 1
  score.value += speed * 0.075
  speed = Math.min(10.5, 4 + score.value * 0.00055)

  dinoVy += gravity
  dinoY += dinoVy
  if (dinoY >= groundY) {
    dinoY = groundY
    dinoVy = 0
  }

  nextSpawn -= 1
  if (nextSpawn <= 0) {
    spawnObstacle()
    nextSpawn = 52 + Math.floor(Math.random() * 62) + Math.max(0, 40 - speed * 1.6)
  }

  for (const o of obstacles) {
    o.x -= speed
    const oy = groundY - o.h
    if (!o.passed && o.x + o.w < dinoX) {
      o.passed = true
    }
    if (
      hit(dinoX + 10, dinoY - dinoH + 8, dinoW - 20, dinoH - 14, o.x, oy, o.w, o.h)
    ) {
      gameOver.value = true
      playing.value = false
      const s = Math.floor(score.value)
      saveHi(s)
      queueMicrotask(() => submitServerScore(s))
      return
    }
  }
  obstacles = obstacles.filter((o) => o.x > -60)
}

function drawGround() {
  ctx.fillStyle = 'rgba(148, 163, 184, 0.35)'
  ctx.fillRect(0, groundY, W, H - groundY)
  const rgb = getComputedStyle(document.documentElement).getPropertyValue('--neon-accent-rgb').trim() || '56, 189, 248'
  ctx.strokeStyle = `rgba(${rgb}, 0.65)`
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(0, groundY)
  ctx.lineTo(W, groundY)
  ctx.stroke()
}

function drawDino() {
  const px = dinoX
  const py = dinoY - dinoH
  const c = getComputedStyle(document.documentElement).getPropertyValue('--el-color-primary').trim() || '#38bdf8'
  ctx.fillStyle = c
  ctx.fillRect(px + 27, py + 6, 33, 24)
  ctx.fillRect(px + 48, py, 21, 18)
  ctx.fillRect(px + 9, py + 27, 42, 21)
  ctx.fillRect(px, py + 33, 15, 12)
  const leg = (frame >> 2) % 2 === 0
  ctx.fillRect(px + 15, py + dinoH - 12, 12, 12)
  ctx.fillRect(px + (leg ? 39 : 45), py + dinoH - 12, 12, 12)
}

function drawObstacles() {
  for (const o of obstacles) {
    const oy = groundY - o.h
    ctx.fillStyle = 'rgba(34, 197, 94, 0.9)'
    ctx.fillRect(o.x, oy, o.w, o.h)
    ctx.fillStyle = 'rgba(22, 163, 74, 0.95)'
    const armH = Math.min(12, Math.floor(o.h * 0.2))
    ctx.fillRect(o.x + Math.floor(o.w * 0.22), oy - armH, Math.max(9, Math.floor(o.w * 0.22)), armH)
  }
}

function drawClouds() {
  ctx.fillStyle = 'rgba(148, 163, 184, 0.2)'
  const t = frame * 0.28
  ;[
    [160 + (t % 900) * 0.16, 48],
    [480 + (t % 720) * 0.13, 36],
    [760 + (t % 800) * 0.11, 62],
  ].forEach(([cx, cy]) => {
    ctx.fillRect(cx, cy, 52, 12)
    ctx.fillRect(cx + 14, cy - 10, 32, 12)
  })
}

function draw() {
  if (!ctx) return
  ctx.setTransform(DPR, 0, 0, DPR, 0, 0)
  ctx.clearRect(0, 0, W, H)
  drawClouds()
  drawGround()
  drawObstacles()
  drawDino()
  ctx.fillStyle = 'rgba(226, 232, 240, 0.9)'
  ctx.font = '22px VT323, ui-monospace, monospace'
  ctx.fillText(`得分 ${Math.floor(score.value)}`, 20, 32)
}

function loop() {
  cancelAnimationFrame(raf)
  if (!playing.value) {
    draw()
    return
  }
  update()
  draw()
  if (playing.value && !gameOver.value) {
    raf = requestAnimationFrame(loop)
  }
}

onMounted(async () => {
  readMyUserId()
  loadHi()
  await loadServerBest()
  await syncLocalBestIfNeeded()
  await loadLeaderboard()
  const c = canvasRef.value
  if (!c) return
  ctx = c.getContext('2d')
  c.style.width = '100%'
  c.style.maxWidth = `${W}px`
  c.style.height = 'auto'
  c.style.aspectRatio = `${W} / ${H}`
  draw()
  window.addEventListener('keydown', onKey)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('keydown', onKey)
})
</script>

<style scoped>
.dino-page {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px 18px 48px;
  color: #e2e8f0;
}

.dino-head h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}

.dino-lead {
  margin: 0 0 12px;
  font-size: 14px;
  color: rgba(203, 213, 225, 0.92);
  line-height: 1.5;
}

.dino-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 18px;
}

.dino-stats strong {
  color: var(--el-color-primary);
  font-size: 18px;
}

.dino-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
  align-items: start;
}

@media (max-width: 960px) {
  .dino-layout {
    grid-template-columns: 1fr;
  }
}

.dino-main {
  min-width: 0;
}

.dino-touch-bar {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 14px;
  padding: 0 4px;
}

.touch-btn {
  min-width: 120px;
  min-height: 48px;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 600;
  font-family: inherit;
  color: #f8fafc;
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--el-color-primary) 35%, transparent),
    rgba(15, 23, 42, 0.85)
  );
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 55%, transparent);
  border-radius: 12px;
  cursor: pointer;
  touch-action: manipulation;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.touch-btn:active {
  filter: brightness(1.08);
  transform: scale(0.98);
}

.touch-btn--jump {
  min-width: min(280px, 100%);
}

.dino-stage-wrap {
  position: relative;
  border-radius: 14px;
  padding: 14px;
  overflow: hidden;
}

.dino-canvas {
  display: block;
  margin: 0 auto;
  cursor: pointer;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
  border-radius: 10px;
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--neon-bg-deep) 40%, transparent) 0%,
    rgba(15, 23, 42, 0.65) 100%
  );
}

.dino-overlay {
  position: absolute;
  inset: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(15, 23, 42, 0.55);
  border-radius: 10px;
  pointer-events: none;
  font-size: 16px;
  color: #e2e8f0;
}

.dino-overlay--dead {
  pointer-events: auto;
}

.dino-lb {
  border-radius: 14px;
  padding: 14px 14px 16px;
  position: sticky;
  top: 16px;
}

.dino-lb-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f1f5f9;
}

.dino-lb-ico {
  flex-shrink: 0;
}

.dino-lb-sub {
  margin: 0 0 10px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.dino-lb-my {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--el-color-primary);
  font-weight: 600;
}

.dino-lb-body {
  min-height: 120px;
}

.dino-lb-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.dino-lb-row {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 6px 4px;
  font-size: 13px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.dino-lb-row:last-child {
  border-bottom: none;
}

.dino-lb-row.is-me {
  background: rgba(var(--neon-accent-rgb), 0.12);
  border-radius: 8px;
  border-bottom-color: transparent;
}

.dino-lb-row--outside {
  border-top: 1px dashed rgba(148, 163, 184, 0.28);
  margin-top: 4px;
  padding-top: 10px;
}

.dino-lb-out {
  font-size: 11px;
  color: #64748b;
  font-weight: 400;
  margin-left: 4px;
}

.dino-lb-rank {
  font-weight: 700;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

.dino-lb-rank.is-top {
  color: var(--el-color-primary);
}

.dino-lb-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #e2e8f0;
}

.dino-lb-score {
  font-weight: 600;
  color: #a7f3d0;
  font-variant-numeric: tabular-nums;
}

.dino-hint {
  margin-top: 16px;
  font-size: 12px;
  color: #64748b;
}
</style>
