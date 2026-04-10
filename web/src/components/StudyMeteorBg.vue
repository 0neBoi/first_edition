<template>
  <div class="study-meteor-layer" aria-hidden="true">
    <div class="study-meteor-gradient" />
    <canvas ref="canvasRef" class="study-meteor-canvas" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const canvasRef = ref(null)
let raf = 0
const meteors = []

function spawn(w, h) {
  return {
    x: Math.random() * w * 0.85 + w * 0.1,
    y: Math.random() * h * 0.35 - 40,
    vx: -10 - Math.random() * 14,
    vy: 8 + Math.random() * 12,
    len: 60 + Math.random() * 100,
    w: 1.2 + Math.random() * 1.8,
    alpha: 0.15 + Math.random() * 0.35,
    life: 1,
  }
}

function tick() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = canvas.width
  const h = canvas.height
  if (!ctx || !w || !h) {
    raf = requestAnimationFrame(tick)
    return
  }

  ctx.fillStyle = 'rgba(2, 6, 23, 0.22)'
  ctx.fillRect(0, 0, w, h)

  while (meteors.length < 28) {
    meteors.push(spawn(w, h))
  }

  for (let i = meteors.length - 1; i >= 0; i--) {
    const m = meteors[i]
    m.x += m.vx * 0.35
    m.y += m.vy * 0.35
    m.life -= 0.004

    const gx = Math.cos(Math.atan2(m.vy, m.vx))
    const gy = Math.sin(Math.atan2(m.vy, m.vx))
    const x2 = m.x - gx * m.len
    const y2 = m.y - gy * m.len

    const grd = ctx.createLinearGradient(m.x, m.y, x2, y2)
    grd.addColorStop(0, `rgba(255,255,255,${m.alpha * m.life})`)
    grd.addColorStop(0.35, `rgba(186, 230, 253,${m.alpha * 0.6 * m.life})`)
    grd.addColorStop(1, 'rgba(56, 189, 248, 0)')

    ctx.strokeStyle = grd
    ctx.lineWidth = m.w
    ctx.beginPath()
    ctx.moveTo(m.x, m.y)
    ctx.lineTo(x2, y2)
    ctx.stroke()

    if (m.y > h + 80 || m.x < -120 || m.life <= 0) {
      meteors.splice(i, 1)
    }
  }

  raf = requestAnimationFrame(tick)
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas?.parentElement) return
  const el = canvas.parentElement
  const r = window.devicePixelRatio || 1
  const rect = el.getBoundingClientRect()
  canvas.width = rect.width * r
  canvas.height = rect.height * r
  canvas.style.width = `${rect.width}px`
  canvas.style.height = `${rect.height}px`
  const ctx = canvas.getContext('2d')
  if (ctx) ctx.setTransform(r, 0, 0, r, 0, 0)
}

let onResize
onMounted(() => {
  resize()
  onResize = () => resize()
  window.addEventListener('resize', onResize)
  tick()
})

onBeforeUnmount(() => {
  if (raf) cancelAnimationFrame(raf)
  if (onResize) window.removeEventListener('resize', onResize)
  meteors.length = 0
})
</script>

<style scoped>
.study-meteor-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}
.study-meteor-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 120% 80% at 50% -20%, rgba(56, 189, 248, 0.18), transparent 55%),
    radial-gradient(ellipse 90% 60% at 100% 30%, rgba(168, 85, 247, 0.12), transparent 50%),
    radial-gradient(ellipse 70% 50% at 0% 80%, rgba(59, 130, 246, 0.1), transparent 45%),
    linear-gradient(180deg, #0b1220 0%, #0f172a 38%, #020617 100%);
}
.study-meteor-canvas {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
}
</style>
