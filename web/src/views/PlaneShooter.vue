<template>
  <div class="plane-page">
    <div class="plane-head">
      <h1>飞机大战</h1>
      <p class="plane-lead">
        ← → 或 A D 移动，空格连发子弹；下方有「左 / 开火 / 右」触摸键。本机最高分进入页面时会自动同步到服务器（高于服务器记录时）。被敌弹击中或与敌机相撞会扣血，血条清空则结束。漏网的敌机会飞出屏幕，不判负。破纪录会同步排行榜。
      </p>
      <div class="plane-stats">
        <span>生命 <strong>{{ hpDisplay }}</strong> / {{ maxHp }}</span>
        <span>得分 <strong>{{ scoreDisplay }}</strong></span>
        <span v-if="hiLocal > 0">本机最高 <strong>{{ hiLocal }}</strong></span>
        <span v-if="serverBest != null">服务器最佳 <strong>{{ serverBest }}</strong></span>
      </div>
    </div>

    <div class="plane-layout">
      <div class="plane-main">
        <div class="plane-stage-wrap study-glass-block">
          <canvas
            ref="canvasRef"
            class="plane-canvas"
            :width="W * DPR"
            :height="H * DPR"
            tabindex="0"
            @click="onCanvasTap"
          />
          <div v-if="!playing && !gameOver" class="plane-overlay">
            <p>按空格、点画布或点「开始游戏」</p>
            <el-button type="primary" class="plane-start-btn" @click="startGame">开始游戏</el-button>
          </div>
          <div v-if="gameOver" class="plane-overlay plane-overlay--dead">
            <p>生命值耗尽 · 得分 {{ scoreDisplay }}</p>
            <el-button type="primary" @click="restart">再来一局</el-button>
          </div>
        </div>
        <div class="plane-touch-bar" aria-label="触摸控制">
          <button
            type="button"
            class="touch-btn touch-btn--dir"
            @pointerdown.prevent="onLeftDown"
            @pointerup.prevent="onLeftUp"
            @pointercancel.prevent="onLeftUp"
            @pointerleave.prevent="onLeftUp"
          >
            左
          </button>
          <button
            type="button"
            class="touch-btn touch-btn--fire"
            @pointerdown.prevent="onFireDown"
            @pointerup.prevent="onFireUp"
            @pointercancel.prevent="onFireUp"
            @pointerleave.prevent="onFireUp"
          >
            开火
          </button>
          <button
            type="button"
            class="touch-btn touch-btn--dir"
            @pointerdown.prevent="onRightDown"
            @pointerup.prevent="onRightUp"
            @pointercancel.prevent="onRightUp"
            @pointerleave.prevent="onRightUp"
          >
            右
          </button>
        </div>
      </div>

      <aside class="plane-lb study-glass-block">
        <h3 class="plane-lb-title">
          <PixelIcon name="spark" size="sm" class="plane-lb-ico" />
          排行榜
        </h3>
        <p class="plane-lb-sub">按历史最佳分排序（每人一条纪录）</p>
        <p v-if="myRank != null" class="plane-lb-my">我的排名：第 {{ myRank }} 名</p>
        <div v-loading="lbLoading" class="plane-lb-body">
          <el-empty
            v-if="!lbLoading && !displayLbRows.length"
            description="暂无数据，来玩一局吧"
            :image-size="72"
          />
          <ol v-else class="plane-lb-list">
            <li
              v-for="row in displayLbRows"
              :key="String(row.userId) + '-' + row.rank + (row._outside ? '-o' : '')"
              class="plane-lb-row"
              :class="{ 'is-me': isMeRow(row), 'plane-lb-row--outside': row._outside }"
            >
              <span class="plane-lb-rank" :class="{ 'is-top': row.rank <= 3 }">{{ row.rank }}</span>
              <span class="plane-lb-name" :title="row.displayName">
                {{ row.displayName }}
                <span v-if="row._outside" class="plane-lb-out">（未进前 20）</span>
              </span>
              <span class="plane-lb-score">{{ row.score }}</span>
            </li>
          </ol>
        </div>
      </aside>
    </div>

    <p class="plane-hint">敌机飞出屏幕不扣血；分数与恐龙跑酷分开统计。</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { planeApi } from '../api'

const HI_KEY = 'sh_plane_hi'

const W = 800
const H = 520
const DPR = Math.min(2, typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1)

const canvasRef = ref(null)
const playing = ref(false)
const gameOver = ref(false)
const score = ref(0)

const hiLocal = ref(0)
const serverBest = ref(null)
const lbRows = ref([])
const lbLoading = ref(false)
const myUserId = ref(null)
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
const maxHp = 100
const hp = ref(maxHp)
const hpDisplay = computed(() => Math.max(0, Math.ceil(hp.value)))

let ctx = null
let raf = 0
let frame = 0

const keys = { left: false, right: false, fire: false }

let playerX = W / 2 - 24
const playerY = H - 56
const playerW = 48
const playerH = 36
const playerSpeed = 8

let bullets = []
let enemyBullets = []
let enemies = []
let stars = []
let fireCd = 0
let spawnCd = 0
/** 受伤后短暂无敌（帧） */
let invuln = 0

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

function loadHi() {
  try {
    const n = parseInt(localStorage.getItem(HI_KEY) || '0', 10)
    hiLocal.value = Number.isFinite(n) ? n : 0
  } catch {
    hiLocal.value = 0
  }
}

function saveHi(s) {
  if (s > hiLocal.value) {
    hiLocal.value = s
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
    const data = await planeApi.leaderboard(20)
    lbRows.value = Array.isArray(data) ? data : []
    try {
      const raw = localStorage.getItem('user')
      if (raw) {
        const u = JSON.parse(raw)
        if (u.role !== 'ADMIN') {
          const entry = await planeApi.myEntry()
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
    const n = await planeApi.myBest()
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
    await planeApi.submitBest(s)
    await loadLeaderboard()
    await loadServerBest()
  } catch {
    /* ignore */
  }
}

/** 进入页面时：若本机最高分高于服务器记录，自动上传以便上榜 */
async function syncLocalBestIfNeeded() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) return
    const u = JSON.parse(raw)
    if (u.role === 'ADMIN') return
    const local = hiLocal.value
    const srv = serverBest.value ?? 0
    if (local > srv) {
      await planeApi.submitBest(local)
      await loadServerBest()
      await loadLeaderboard()
    }
  } catch {
    /* ignore */
  }
}

function initStars() {
  stars = []
  for (let i = 0; i < 80; i += 1) {
    stars.push({
      x: Math.random() * W,
      y: Math.random() * H,
      s: 1 + Math.random() * 2,
      sp: 0.3 + Math.random() * 1.2,
    })
  }
}

function resetGame() {
  score.value = 0
  hp.value = maxHp
  frame = 0
  bullets = []
  enemyBullets = []
  enemies = []
  initStars()
  playerX = W / 2 - playerW / 2
  fireCd = 0
  spawnCd = 0
  invuln = 0
  gameOver.value = false
}

function restart() {
  resetGame()
  playing.value = true
  loop()
}

function focusCanvas() {
  canvasRef.value?.focus()
}

function startGame() {
  if (playing.value || gameOver.value) return
  playing.value = true
  resetGame()
  loop()
  focusCanvas()
}

function onCanvasTap() {
  if (!playing.value && !gameOver.value) {
    startGame()
    return
  }
  focusCanvas()
}

function onLeftDown() {
  keys.left = true
  focusCanvas()
}
function onLeftUp() {
  keys.left = false
}
function onRightDown() {
  keys.right = true
  focusCanvas()
}
function onRightUp() {
  keys.right = false
}
function onFireDown() {
  keys.fire = true
  focusCanvas()
  if (!playing.value && !gameOver.value) {
    startGame()
  }
}
function onFireUp() {
  keys.fire = false
}

function onKeyDown(e) {
  if (e.code === 'Space') e.preventDefault()
  if (e.code === 'ArrowLeft' || e.code === 'KeyA') keys.left = true
  if (e.code === 'ArrowRight' || e.code === 'KeyD') keys.right = true
  if (e.code === 'Space') {
    keys.fire = true
    if (!playing.value && !gameOver.value) {
      startGame()
    }
  }
}

function onKeyUp(e) {
  if (e.code === 'ArrowLeft' || e.code === 'KeyA') keys.left = false
  if (e.code === 'ArrowRight' || e.code === 'KeyD') keys.right = false
  if (e.code === 'Space') keys.fire = false
}

function spawnEnemy() {
  const w = 36 + Math.floor(Math.random() * 16)
  const h = 28 + Math.floor(Math.random() * 12)
  const x = 20 + Math.random() * (W - w - 40)
  const vy = 2.2 + Math.min(4.5, score.value * 0.004)
  enemies.push({
    x,
    y: -h - 4,
    w,
    h,
    vy,
    shootCd: 50 + Math.floor(Math.random() * 40),
  })
}

function hit(ax, ay, aw, ah, bx, by, bw, bh) {
  return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by
}

function damagePlayer(amount) {
  if (invuln > 0) return
  hp.value -= amount
  invuln = 45
  if (hp.value <= 0) {
    hp.value = 0
    endGame()
  }
}

function update() {
  frame += 1
  score.value += 0.08
  if (invuln > 0) invuln -= 1

  for (const s of stars) {
    s.y += s.sp
    if (s.y > H) {
      s.y = 0
      s.x = Math.random() * W
    }
  }

  if (keys.left) playerX -= playerSpeed
  if (keys.right) playerX += playerSpeed
  playerX = Math.max(8, Math.min(W - playerW - 8, playerX))

  if (fireCd > 0) fireCd -= 1
  if (keys.fire && fireCd <= 0 && playing.value) {
    bullets.push({
      x: playerX + playerW / 2 - 3,
      y: playerY - 4,
      w: 6,
      h: 16,
      vy: -15,
    })
    fireCd = 8
  }

  for (const b of bullets) {
    b.y += b.vy
  }
  bullets = bullets.filter((b) => b.y > -20)

  spawnCd -= 1
  const spawnRate = Math.max(22, 52 - Math.floor(score.value / 120))
  if (spawnCd <= 0) {
    spawnEnemy()
    spawnCd = spawnRate + Math.floor(Math.random() * 18)
  }

  for (const e of enemies) {
    e.y += e.vy
    e.shootCd = (e.shootCd ?? 55) - 1
    if (e.shootCd <= 0) {
      enemyBullets.push({
        x: e.x + e.w / 2 - 3,
        y: e.y + e.h,
        w: 6,
        h: 12,
        vy: 7 + Math.min(4, score.value * 0.003),
      })
      e.shootCd = 55 + Math.floor(Math.random() * 55)
    }
  }

  for (const eb of enemyBullets) {
    eb.y += eb.vy
  }
  enemyBullets = enemyBullets.filter((eb) => eb.y < H + 40)

  const px = playerX
  const py = playerY
  for (const eb of enemyBullets) {
    if (invuln > 0) break
    if (hit(eb.x, eb.y, eb.w, eb.h, px + 8, py + 6, playerW - 16, playerH - 10)) {
      eb.dead = true
      damagePlayer(12)
      if (gameOver.value) return
    }
  }
  enemyBullets = enemyBullets.filter((eb) => !eb.dead)

  for (const e of enemies) {
    e.hit = false
  }
  for (const b of bullets) {
    for (const e of enemies) {
      if (e.hit) continue
      if (hit(b.x, b.y, b.w, b.h, e.x, e.y, e.w, e.h)) {
        e.hit = true
        b.dead = true
        score.value += 10
      }
    }
  }
  enemies = enemies.filter((e) => !e.hit)
  bullets = bullets.filter((b) => !b.dead)

  for (const e of enemies) {
    if (hit(px + 8, py + 6, playerW - 16, playerH - 10, e.x, e.y, e.w, e.h)) {
      e.hit = true
      damagePlayer(28)
      if (gameOver.value) return
    }
  }
  enemies = enemies.filter((e) => !e.hit)
  enemies = enemies.filter((e) => e.y < H + 80)
}

function endGame() {
  gameOver.value = true
  playing.value = false
  const s = Math.floor(score.value)
  saveHi(s)
  queueMicrotask(() => submitServerScore(s))
}

function drawStars() {
  ctx.fillStyle = 'rgba(148, 163, 184, 0.35)'
  for (const s of stars) {
    ctx.fillRect(s.x, s.y, s.s, s.s)
  }
}

function drawPlayer() {
  const px = playerX
  const py = playerY
  const c = getComputedStyle(document.documentElement).getPropertyValue('--el-color-primary').trim() || '#38bdf8'
  if (invuln > 0 && (frame >> 2) % 2 === 0) {
    ctx.globalAlpha = 0.45
  }
  ctx.fillStyle = c
  ctx.beginPath()
  ctx.moveTo(px + playerW / 2, py)
  ctx.lineTo(px + playerW, py + playerH)
  ctx.lineTo(px, py + playerH)
  ctx.closePath()
  ctx.fill()
  ctx.fillRect(px + playerW / 2 - 3, py + playerH - 6, 6, 6)
  ctx.globalAlpha = 1
}

function drawBullets() {
  ctx.fillStyle = 'rgba(250, 204, 21, 0.95)'
  for (const b of bullets) {
    ctx.fillRect(b.x, b.y, b.w, b.h)
  }
  ctx.fillStyle = 'rgba(251, 113, 133, 0.95)'
  for (const eb of enemyBullets) {
    ctx.fillRect(eb.x, eb.y, eb.w, eb.h)
  }
}

function drawEnemies() {
  for (const e of enemies) {
    ctx.fillStyle = 'rgba(248, 113, 113, 0.95)'
    ctx.fillRect(e.x, e.y, e.w, e.h)
    ctx.fillStyle = 'rgba(185, 28, 28, 0.9)'
    ctx.fillRect(e.x + e.w * 0.35, e.y - 6, e.w * 0.3, 6)
  }
}

function draw() {
  if (!ctx) return
  ctx.setTransform(DPR, 0, 0, DPR, 0, 0)
  ctx.fillStyle = 'rgba(15, 23, 42, 0.92)'
  ctx.fillRect(0, 0, W, H)
  drawStars()
  drawEnemies()
  drawBullets()
  drawPlayer()
  const barW = 200
  const barX = 16
  const barY = 42
  ctx.fillStyle = 'rgba(30, 41, 59, 0.9)'
  ctx.fillRect(barX, barY, barW, 12)
  ctx.fillStyle = 'rgba(34, 197, 94, 0.85)'
  ctx.fillRect(barX, barY, (barW * hp.value) / maxHp, 12)
  ctx.strokeStyle = 'rgba(148, 163, 184, 0.6)'
  ctx.lineWidth = 1
  ctx.strokeRect(barX, barY, barW, 12)
  ctx.fillStyle = 'rgba(226, 232, 240, 0.85)'
  ctx.font = '20px VT323, ui-monospace, monospace'
  ctx.fillText(`得分 ${Math.floor(score.value)}`, 16, 28)
  ctx.font = '14px VT323, ui-monospace, monospace'
  ctx.fillText(`HP ${Math.ceil(hp.value)}/${maxHp}`, barX + barW + 10, barY + 10)
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
  initStars()
  draw()
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('keyup', onKeyUp)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('keyup', onKeyUp)
})
</script>

<style scoped>
.plane-page {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px 18px 48px;
  color: #e2e8f0;
}

.plane-head h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}

.plane-lead {
  margin: 0 0 12px;
  font-size: 14px;
  color: rgba(203, 213, 225, 0.92);
  line-height: 1.5;
}

.plane-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 18px;
}

.plane-stats strong {
  color: var(--el-color-primary);
  font-size: 18px;
}

.plane-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
  align-items: start;
}

@media (max-width: 960px) {
  .plane-layout {
    grid-template-columns: 1fr;
  }
}

.plane-stage-wrap {
  position: relative;
  border-radius: 14px;
  padding: 14px;
  overflow: hidden;
}

.plane-canvas {
  display: block;
  margin: 0 auto;
  outline: none;
  cursor: pointer;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.plane-overlay {
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

.plane-start-btn {
  pointer-events: auto;
}

.plane-overlay--dead {
  pointer-events: auto;
}

.plane-main {
  min-width: 0;
}

.plane-touch-bar {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  padding: 0 4px;
}

.touch-btn {
  min-width: 88px;
  min-height: 48px;
  padding: 12px 18px;
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

.touch-btn--fire {
  min-width: 120px;
}

.plane-lb {
  border-radius: 14px;
  padding: 14px 14px 16px;
  position: sticky;
  top: 16px;
}

.plane-lb-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f1f5f9;
}

.plane-lb-ico {
  flex-shrink: 0;
}

.plane-lb-sub {
  margin: 0 0 10px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.plane-lb-my {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--el-color-primary);
  font-weight: 600;
}

.plane-lb-body {
  min-height: 120px;
}

.plane-lb-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.plane-lb-row {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 6px 4px;
  font-size: 13px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.plane-lb-row:last-child {
  border-bottom: none;
}

.plane-lb-row.is-me {
  background: rgba(var(--neon-accent-rgb), 0.12);
  border-radius: 8px;
  border-bottom-color: transparent;
}

.plane-lb-row--outside {
  border-top: 1px dashed rgba(148, 163, 184, 0.28);
  margin-top: 4px;
  padding-top: 10px;
}

.plane-lb-out {
  font-size: 11px;
  color: #64748b;
  font-weight: 400;
  margin-left: 4px;
}

.plane-lb-rank {
  font-weight: 700;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

.plane-lb-rank.is-top {
  color: var(--el-color-primary);
}

.plane-lb-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #e2e8f0;
}

.plane-lb-score {
  font-weight: 600;
  color: #a5f3fc;
  font-variant-numeric: tabular-nums;
}

.plane-hint {
  margin-top: 16px;
  font-size: 12px;
  color: #64748b;
}
</style>
