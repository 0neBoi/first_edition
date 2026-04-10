<template>
  <canvas ref="canvasRef" class="tetris-float-canvas" aria-hidden="true" />
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

/** 七种四连方块（相对格子坐标，含 I/O/T/L/J/S/Z） */
const SHAPES = [
  [
    [1, 1, 1, 1],
  ],
  [
    [1, 1],
    [1, 1],
  ],
  [
    [0, 1, 0],
    [1, 1, 1],
  ],
  [
    [1, 0],
    [1, 0],
    [1, 1],
  ],
  [
    [0, 1],
    [0, 1],
    [1, 1],
  ],
  [
    [0, 1, 1],
    [1, 1, 0],
  ],
  [
    [1, 1, 0],
    [0, 1, 1],
  ],
]

/** 与方块顺序对应的经典俄罗斯方块配色（I/O/T/L/J/S/Z） */
const TETRIS_RGB = [
  { r: 0, g: 229, b: 255 },
  { r: 255, g: 214, b: 0 },
  { r: 176, g: 92, b: 255 },
  { r: 255, g: 150, b: 32 },
  { r: 48, g: 120, b: 255 },
  { r: 56, g: 210, b: 88 },
  { r: 240, g: 72, b: 96 },
]

const canvasRef = ref(null)
let raf = 0
let pieces = []
let resizeObs = null

function spawnPiece(w, h) {
  const shapeIdx = (Math.random() * SHAPES.length) | 0
  const shape = SHAPES[shapeIdx]
  const rgb = TETRIS_RGB[shapeIdx] || TETRIS_RGB[0]
  const rows = shape.length
  const cols = shape[0].length
  const cell = 8 + Math.random() * 10
  const bw = cols * cell
  const bh = rows * cell
  return {
    shape,
    rgb,
    rows,
    cols,
    cell,
    x: Math.random() * Math.max(8, w - bw - 8),
    y: Math.random() * Math.max(8, h - bh - 8),
    vx: (Math.random() - 0.5) * 0.45,
    vy: (Math.random() - 0.4) * 0.5 + 0.12,
    rot: Math.random() * Math.PI * 2,
    rotSpeed: (Math.random() - 0.5) * 0.01,
    alpha: 0.22 + Math.random() * 0.28,
  }
}

function ensureCount(w, h) {
  const target = Math.min(42, Math.max(18, Math.floor((w * h) / 42000)))
  while (pieces.length < target) {
    pieces.push(spawnPiece(w, h))
  }
  if (pieces.length > target) {
    pieces.length = target
  }
}

function tick() {
  const canvas = canvasRef.value
  if (!canvas) return
  const parent = canvas.parentElement
  if (!parent) return

  const reduce = document.documentElement.dataset.reduceMotion === '1'
  const rect = parent.getBoundingClientRect()
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const w = Math.floor(rect.width)
  const h = Math.floor(rect.height)
  if (w < 4 || h < 4) {
    raf = requestAnimationFrame(tick)
    return
  }

  if (canvas.width !== w * dpr || canvas.height !== h * dpr) {
    canvas.width = w * dpr
    canvas.height = h * dpr
    canvas.style.width = `${w}px`
    canvas.style.height = `${h}px`
  }

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, w, h)

  ensureCount(w, h)

  for (const p of pieces) {
    if (!reduce) {
      p.x += p.vx
      p.y += p.vy
      p.rot += p.rotSpeed
    }
    const margin = 80
    if (p.x < -margin || p.x > w + margin || p.y < -margin || p.y > h + margin) {
      Object.assign(p, spawnPiece(w, h))
    }

    ctx.save()
    ctx.globalAlpha = p.alpha
    ctx.translate(p.x, p.y)
    ctx.rotate(p.rot)
    const c = p.cell
    const rgb = p.rgb || TETRIS_RGB[0]
    ctx.strokeStyle = `rgba(${rgb.r},${rgb.g},${rgb.b},0.72)`
    ctx.fillStyle = `rgba(${rgb.r},${rgb.g},${rgb.b},0.32)`
    ctx.lineWidth = 1.25
    for (let row = 0; row < p.rows; row++) {
      for (let col = 0; col < p.cols; col++) {
        if (!p.shape[row][col]) continue
        const x = col * c
        const y = row * c
        ctx.fillRect(x, y, c - 0.5, c - 0.5)
        ctx.strokeRect(x + 0.5, y + 0.5, c - 1, c - 1)
      }
    }
    ctx.restore()
  }

  raf = requestAnimationFrame(tick)
}

function onTheme() {
  /* 下一帧会用新 --neon-accent */
}

function bindResize(el) {
  resizeObs = new ResizeObserver(() => {
    pieces = []
  })
  resizeObs.observe(el)
}

onMounted(() => {
  const el = canvasRef.value?.parentElement
  if (el) bindResize(el)
  window.addEventListener('sh-theme-updated', onTheme)
  raf = requestAnimationFrame(tick)
})

onUnmounted(() => {
  cancelAnimationFrame(raf)
  resizeObs?.disconnect()
  window.removeEventListener('sh-theme-updated', onTheme)
})
</script>

<style scoped>
.tetris-float-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
