<template>
  <div class="profile-page">
    <canvas ref="bgCanvas" class="profile-canvas" />

    <div class="profile-inner">
      <header class="profile-header">
        <div>
          <h2>个人中心</h2>
          <p class="subtitle">管理你的账号信息与学习档案，在同一处完成资料与身份的维护。</p>
        </div>
      </header>

      <section class="profile-metrics">
        <div class="metric-card">
          <div class="metric-gauge gauge-1">
            <span class="gauge-arc" />
            <span class="gauge-pointer" />
            <span class="gauge-dot" />
          </div>
          <div class="metric-value">{{ stats.materials }}</div>
          <div class="metric-label">已上传资料</div>
        </div>
        <div class="metric-card">
          <div class="metric-gauge gauge-2">
            <span class="gauge-arc" />
            <span class="gauge-pointer" />
            <span class="gauge-dot" />
          </div>
          <div class="metric-value">{{ stats.questions }}</div>
          <div class="metric-label">已生成题目</div>
        </div>
        <div class="metric-card">
          <div class="metric-gauge gauge-3">
            <span class="gauge-arc" />
            <span class="gauge-pointer" />
            <span class="gauge-dot" />
          </div>
          <div class="metric-value">{{ stats.weeklyCourses }}</div>
          <div class="metric-label">本周课程数</div>
        </div>
      </section>

      <el-row :gutter="18" class="profile-grid">
        <el-col :xs="24" :md="10">
          <section class="panel panel-left">
            <div class="panel-left-header">
              <div class="panel-badge">ID</div>
              <span class="panel-label">账号概览</span>
            </div>
            <div class="avatar-orbit">
              <div class="orbit orbit-a" />
              <div class="orbit orbit-b" />
              <el-avatar :size="120" :src="avatarUrl" class="avatar">
                {{ avatarText }}
              </el-avatar>
            </div>
            <p class="username">用户名：{{ profile.username }}</p>
            <el-upload
              class="avatar-upload"
              :show-file-list="false"
              :http-request="onAvatarUpload"
              accept=".jpg,.jpeg,.png,.gif,.webp"
            >
              <el-button type="primary" size="small">更换头像</el-button>
            </el-upload>

            <ul class="meta-list">
              <li>
                <span class="meta-label">昵称</span>
                <span class="meta-value">{{ form.nickname || '-' }}</span>
              </li>
              <li>
                <span class="meta-label">学校</span>
                <span class="meta-value">{{ form.school || '-' }}</span>
              </li>
              <li>
                <span class="meta-label">专业</span>
                <span class="meta-value">{{ form.major || '-' }}</span>
              </li>
            </ul>
          </section>
        </el-col>

        <el-col :xs="24" :md="14">
          <section class="panel panel-main">
            <div class="panel-main-body">
              <div class="panel-main-block block-basic">
                <h3>基本资料</h3>
                <el-form :model="form" label-width="80" label-position="top">
                  <el-form-item label="昵称">
                    <el-input v-model="form.nickname" placeholder="显示名称" clearable />
                  </el-form-item>
                  <el-form-item label="手机号">
                    <el-input v-model="form.phone" placeholder="选填" clearable />
                  </el-form-item>
                  <el-form-item label="学校">
                    <el-input v-model="form.school" placeholder="选填" clearable />
                  </el-form-item>
                  <el-form-item label="专业">
                    <el-input v-model="form.major" placeholder="选填" clearable />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
                  </el-form-item>
                </el-form>
                <p class="profile-settings-hint">
                  修改密码请在
                  <router-link to="/settings">设置</router-link>
                  中完成。
                </p>
              </div>
            </div>
          </section>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi, getAvatarUrl, materialApi, knowledgeApi, questionApi } from '../api'
import { resolveCssVarColor } from '../theme'

const profile = ref({})
const form = reactive({ nickname: '', avatar: '', phone: '', school: '', major: '' })
const saving = ref(false)
const bgCanvas = ref(null)
let animationId
const stats = ref({
  materials: 0,
  knowledgePoints: 0,
  questions: 0,
  weeklyCourses: 0,
})

const avatarUrl = computed(() => getAvatarUrl(profile.value.avatar))
const avatarText = computed(() => {
  const s = profile.value.nickname || profile.value.username || ''
  return s ? s.slice(0, 1) : '?'
})

function loadProfile() {
  authApi.me().then((data) => {
    profile.value = data
    form.nickname = data.nickname || ''
    form.avatar = data.avatar || ''
    form.phone = data.phone || ''
    form.school = data.school || ''
    form.major = data.major || ''
  }).catch(() => {
    ElMessage.error('获取个人信息失败')
  })
}

async function saveProfile() {
  saving.value = true
  try {
    const data = await authApi.updateProfile({
      nickname: form.nickname,
      avatar: form.avatar || undefined,
      phone: form.phone,
      school: form.school,
      major: form.major,
    })
    profile.value = data
    const raw = localStorage.getItem('user')
    if (raw) {
      const user = JSON.parse(raw)
      user.nickname = data.nickname
      user.avatar = data.avatar
      localStorage.setItem('user', JSON.stringify(user))
      window.dispatchEvent(new Event('sh-user-updated'))
    }
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function onAvatarUpload({ file }) {
  if (!/\.(jpg|jpeg|png|gif|webp)$/i.test(file.name)) {
    ElMessage.error('请选择 jpg/png/gif/webp 图片')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像不能超过 2MB')
    return
  }
  try {
    const path = await authApi.uploadAvatar(file)
    form.avatar = path
    profile.value = { ...profile.value, avatar: path }
    await saveProfile()
    ElMessage.success('头像已更新')
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  }
}

async function loadStats() {
  try {
    const list = (await materialApi.list()) || []
    stats.value.materials = list.length

    const ids = list.map((m) => m.id).filter(Boolean)
    if (ids.length) {
      const [knowledgeRes, questionRes] = await Promise.all([
        Promise.all(ids.map((id) => knowledgeApi.list(id).catch(() => []))),
        Promise.all(ids.map((id) => questionApi.list(id).catch(() => []))),
      ])

      stats.value.knowledgePoints = knowledgeRes.reduce(
        (sum, arr) => sum + (Array.isArray(arr) ? arr.length : 0),
        0,
      )
      stats.value.questions = questionRes.reduce(
        (sum, arr) => sum + (Array.isArray(arr) ? arr.length : 0),
        0,
      )
    } else {
      stats.value.knowledgePoints = 0
      stats.value.questions = 0
    }

    try {
      const raw = localStorage.getItem('sh_schedule')
      const schedule = raw ? JSON.parse(raw) : []
      stats.value.weeklyCourses = Array.isArray(schedule) ? schedule.length : 0
    } catch {
      stats.value.weeklyCourses = 0
    }
  } catch {
    // 保持默认值即可
  }
}

onMounted(() => {
  loadProfile()
  loadStats()
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
  const count = 140
  for (let i = 0; i < count; i += 1) {
    stars.push({
      x: Math.random() * canvas.clientWidth,
      y: Math.random() * canvas.clientHeight,
      r: Math.random() * 1.4 + 0.4,
      speed: 0.15 + Math.random() * 0.3,
      alpha: 0.4 + Math.random() * 0.6,
      twinkle: Math.random() * Math.PI * 2,
    })
  }

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    const grd = ctx.createRadialGradient(
      canvas.clientWidth * 0.8,
      canvas.clientHeight * 0.1,
      0,
      canvas.clientWidth * 0.5,
      canvas.clientHeight * 0.5,
      canvas.clientHeight * 0.9,
    )
    grd.addColorStop(0, resolveCssVarColor('--neon-accent'))
    grd.addColorStop(0.4, resolveCssVarColor('--glass-90'))
    grd.addColorStop(1, resolveCssVarColor('--neon-bg-deep'))
    ctx.fillStyle = grd
    ctx.fillRect(0, 0, canvas.clientWidth, canvas.clientHeight)

    stars.forEach((s, idx) => {
      s.y += s.speed
      if (s.y > canvas.clientHeight + 20) {
        s.y = -20
        s.x = Math.random() * canvas.clientWidth
      }
      s.twinkle += 0.02 + idx * 0.0002
      const twinkleFactor = 0.6 + Math.sin(s.twinkle) * 0.4

      ctx.beginPath()
      ctx.fillStyle = `rgba(191, 219, 254, ${s.alpha * twinkleFactor})`
      ctx.arc(s.x, s.y, s.r * twinkleFactor, 0, Math.PI * 2)
      ctx.fill()
    })

    animationId = requestAnimationFrame(draw)
  }

  draw()
}
</script>

<style scoped>
.profile-page {
  max-width: 1000px;
  margin: 24px auto;
  padding: 0 24px 32px;
  position: relative;
  min-height: calc(100vh - 160px);
}
.profile-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.profile-inner {
  position: relative;
  z-index: 1;
}

.profile-header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #e5e7eb;
}

.profile-header .subtitle {
  margin: 0 0 18px;
  font-size: 13px;
  color: #cbd5f5;
}

.profile-grid {
  row-gap: 16px;
}

.profile-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}

.metric-card {
  flex: 1;
  min-width: 180px;
  border-radius: 16px;
  padding: 10px 12px;
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.2), transparent 60%),
    var(--glass-95);
  border: 1px solid rgba(148, 163, 184, 0.6);
  box-shadow: 0 12px 30px var(--glass-90);
  display: flex;
  align-items: center;
  gap: 10px;
}

.metric-gauge {
  position: relative;
  width: 50px;
  height: 50px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 0%, rgba(191, 219, 254, 0.9), rgba(37, 99, 235, 0.3));
  box-shadow:
    0 0 0 1px var(--glass-90),
    0 10px 24px var(--glass-95),
    0 0 24px rgba(56, 189, 248, 0.65);
  overflow: hidden;
}

.gauge-arc {
  position: absolute;
  inset: 6px;
  border-radius: 999px;
  border: 2px dashed var(--glass-70);
  opacity: 0.9;
}

.gauge-pointer {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 2px;
  height: 18px;
  background: linear-gradient(to bottom, #e0f2fe, rgba(56, 189, 248, 0));
  transform-origin: 50% 90%;
  animation: gauge-swing 3.4s ease-in-out infinite;
}

.gauge-dot {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #e0f2fe;
  box-shadow: 0 0 18px rgba(56, 189, 248, 0.9);
  left: 50%;
  top: 10%;
  transform: translateX(-50%);
}

.gauge-2 .gauge-pointer {
  animation-duration: 4.1s;
}

.gauge-3 .gauge-pointer {
  animation-duration: 3s;
}

.metric-value {
  font-size: 18px;
  font-weight: 600;
  color: #e5e7eb;
}

.metric-label {
  font-size: 12px;
  color: #9ca3af;
}

@keyframes gauge-swing {
  0% {
    transform: translate(-50%, -50%) rotate(-25deg);
  }
  50% {
    transform: translate(-50%, -50%) rotate(20deg);
  }
  100% {
    transform: translate(-50%, -50%) rotate(-25deg);
  }
}

.panel {
  position: relative;
  border-radius: 18px;
  padding: 14px 16px 16px;
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.18), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(168, 85, 247, 0.22), transparent 60%),
    var(--glass-96);
  border: 1px solid rgba(148, 163, 184, 0.5);
  box-shadow: 0 18px 40px var(--glass-95);
}

.panel-left-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.panel-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  background-color: rgba(56, 189, 248, 0.2);
  color: #e0f2fe;
}

.panel-label {
  font-size: 13px;
  color: #cbd5f5;
}

.avatar-orbit {
  position: relative;
  width: 180px;
  height: 160px;
  margin: 0 auto 6px;
}

.orbit {
  position: absolute;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.6);
}

.orbit-a {
  inset: 4px 12px;
}

.orbit-b {
  inset: 12px 4px;
  border-color: rgba(56, 189, 248, 0.7);
}

.avatar {
  position: absolute;
  inset: 50% auto auto 50%;
  transform: translate(-50%, -50%);
  margin-bottom: 0;
  box-shadow: 0 10px 30px var(--glass-90);
}

.avatar-upload {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.username {
  color: #e5e7eb;
  font-size: 14px;
  margin: 0 0 4px;
  text-align: center;
}

.meta-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  font-size: 13px;
  color: #e5e7eb;
}

.meta-list li {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  margin: 4px 0;
}

.meta-label {
  color: #9ca3af;
}

.meta-value {
  font-weight: 500;
}

.panel-main {
  padding: 16px 18px 18px;
}

.panel-main-body {
  display: block;
  row-gap: 12px;
}

.profile-settings-hint {
  margin: 12px 0 0;
  font-size: 13px;
  color: rgba(148, 163, 184, 0.95);
}

.profile-settings-hint a {
  color: var(--el-color-primary-light-3);
  text-decoration: none;
}

.profile-settings-hint a:hover {
  text-decoration: underline;
}

.panel-main-block h3 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #e5e7eb;
}

.block-basic {
  border-radius: 12px;
  padding: 10px 12px 12px;
  background-color: var(--glass-92);
  border: 1px solid rgba(148, 163, 184, 0.5);
}

@media (max-width: 768px) {
  .profile-page {
    padding-inline: 16px;
  }

  .panel {
    margin-bottom: 10px;
  }
}
</style>
