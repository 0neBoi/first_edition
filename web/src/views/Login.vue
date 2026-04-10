<template>
  <div class="login-page">
    <div class="login-orbs" aria-hidden="true">
      <span class="login-orb login-orb-a" />
      <span class="login-orb login-orb-b" />
      <span class="login-orb login-orb-c" />
    </div>

    <div class="login-shell">
      <section class="login-hero" aria-labelledby="login-brand-title">
        <div class="login-brand-badge">
          <span class="login-brand-icon">校</span>
        </div>
        <p class="login-eyebrow">校园工具</p>
        <h1 id="login-brand-title" class="login-title">
          学习与备考<br />
          <span class="login-title-accent">一站掌控</span>
        </h1>
        <p class="login-lead">
          资料、笔记、练习与 AI 问答，陪你把节奏找回来。
        </p>
        <ul class="login-pills">
          <li v-for="(t, i) in heroPills" :key="t" class="login-pill" :style="{ animationDelay: `${0.35 + i * 0.08}s` }">
            {{ t }}
          </li>
        </ul>
      </section>

      <div class="login-card-outer">
        <div class="login-card-glow" aria-hidden="true" />
        <el-card class="login-card" shadow="never">
          <div class="login-card-inner">
            <header class="login-card-head">
              <h2 class="login-card-title">欢迎回来</h2>
              <p class="login-card-sub">使用已有账号登录</p>
            </header>

            <el-form :model="form" label-position="top" class="login-form" @submit.prevent="submit">
              <el-form-item label="用户名">
                <el-input v-model="form.username" placeholder="请输入用户名" clearable @keyup.enter="submit">
                  <template #prefix>
                    <span class="login-input-pix"><PixelIcon name="user" size="sm" /></span>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="密码">
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  show-password
                  clearable
                  @keyup.enter="submit"
                >
                  <template #prefix>
                    <el-icon class="login-input-icon"><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item class="login-submit-row">
                <el-button type="primary" class="login-submit" :loading="loading" native-type="submit">
                  <span class="login-submit-label">登录</span>
                </el-button>
              </el-form-item>
              <div class="login-footer-link">
                <el-button link type="primary" @click="goRegister">没有账号？去注册</el-button>
              </div>
            </el-form>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const heroPills = ['资料空间', 'AI 问答', '学习打卡', '错题本']

async function submit() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    const data = await authApi.login(form.username, form.password)
    localStorage.setItem('token', data.token)
    localStorage.setItem(
      'user',
      JSON.stringify({
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        avatar: data.avatar || '',
        role: data.role || 'STUDENT',
      }),
    )
    ElMessage.success('登录成功')
    if (data.role === 'ADMIN') {
      router.replace('/admin/notice')
    } else {
      router.replace(route.query.redirect || '/home')
    }
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<style scoped>
.login-page {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(24px, 5vw, 48px) clamp(16px, 4vw, 32px);
  overflow: hidden;
}

/* 背景漂浮光斑（与流星层呼应） */
.login-orbs {
  pointer-events: none;
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}
.login-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(64px);
  opacity: 0.45;
  animation: login-orb-float 14s ease-in-out infinite;
}
.login-orb-a {
  width: min(42vw, 320px);
  height: min(42vw, 320px);
  left: -8%;
  top: 12%;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.55) 0%, transparent 70%);
  animation-delay: 0s;
}
.login-orb-b {
  width: min(36vw, 280px);
  height: min(36vw, 280px);
  right: -5%;
  bottom: 18%;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.42) 0%, transparent 70%);
  animation-delay: -4s;
  animation-duration: 18s;
}
.login-orb-c {
  width: min(28vw, 200px);
  height: min(28vw, 200px);
  left: 38%;
  bottom: -5%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.35) 0%, transparent 70%);
  animation-delay: -7s;
  animation-duration: 16s;
}
@keyframes login-orb-float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(18px, -22px) scale(1.06);
  }
  66% {
    transform: translate(-14px, 12px) scale(0.96);
  }
}

.login-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1040px;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(300px, 400px);
  gap: clamp(32px, 6vw, 72px);
  align-items: center;
  animation: login-shell-in 0.85s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes login-shell-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-hero {
  animation: login-hero-in 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}
@keyframes login-hero-in {
  from {
    opacity: 0;
    transform: translateX(-16px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.login-brand-badge {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  background: linear-gradient(145deg, rgba(56, 189, 248, 0.35), rgba(168, 85, 247, 0.28));
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 12px 40px rgba(56, 189, 248, 0.15);
  animation: login-badge-pulse 4s ease-in-out infinite;
}
@keyframes login-badge-pulse {
  0%,
  100% {
    box-shadow: 0 12px 40px rgba(56, 189, 248, 0.15);
    transform: scale(1);
  }
  50% {
    box-shadow: 0 16px 48px rgba(168, 85, 247, 0.22);
    transform: scale(1.03);
  }
}
.login-brand-icon {
  font-size: 22px;
  font-weight: 700;
  color: #f0f9ff;
  text-shadow: 0 0 24px rgba(56, 189, 248, 0.5);
}

.login-eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(186, 230, 253, 0.75);
}

.login-title {
  margin: 0 0 16px;
  font-size: clamp(1.75rem, 4vw, 2.25rem);
  font-weight: 700;
  line-height: 1.25;
  color: #f8fafc;
  letter-spacing: -0.02em;
}
.login-title-accent {
  background: linear-gradient(110deg, #7dd3fc 0%, #c4b5fd 45%, #22d3ee 100%);
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: login-shimmer 7s ease-in-out infinite;
}
@keyframes login-shimmer {
  0%,
  100% {
    background-position: 0% center;
  }
  50% {
    background-position: 100% center;
  }
}

.login-lead {
  margin: 0 0 24px;
  max-width: 36em;
  font-size: 15px;
  line-height: 1.65;
  color: rgba(226, 232, 240, 0.82);
}

.login-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.login-pill {
  padding: 8px 14px;
  font-size: 13px;
  color: rgba(241, 245, 249, 0.92);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  backdrop-filter: blur(8px);
  animation: login-pill-in 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes login-pill-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 玻璃卡片 + 流动描边 */
.login-card-outer {
  position: relative;
  animation: login-card-in 0.85s cubic-bezier(0.22, 1, 0.36, 1) 0.15s both;
}
@keyframes login-card-in {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.login-card-glow {
  position: absolute;
  inset: -40%;
  background: conic-gradient(
    from 180deg at 50% 50%,
    rgba(56, 189, 248, 0.15),
    rgba(168, 85, 247, 0.12),
    rgba(34, 211, 238, 0.14),
    rgba(56, 189, 248, 0.15)
  );
  animation: login-glow-spin 18s linear infinite;
  opacity: 0.65;
  filter: blur(28px);
  pointer-events: none;
}
@keyframes login-glow-spin {
  to {
    transform: rotate(360deg);
  }
}

.login-card-outer::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 22px;
  padding: 1.5px;
  background: linear-gradient(
    125deg,
    rgba(56, 189, 248, 0.55),
    rgba(168, 85, 247, 0.4),
    rgba(34, 211, 238, 0.45),
    rgba(56, 189, 248, 0.55)
  );
  background-size: 300% 300%;
  animation: login-border-flow 8s ease infinite;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
@keyframes login-border-flow {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.login-card {
  position: relative;
  z-index: 1;
  border-radius: 20px !important;
  overflow: hidden;
  border: none !important;
}

.login-card-inner {
  padding: 4px 4px 8px;
}

.login-card-head {
  margin-bottom: 22px;
  text-align: center;
}
.login-card-title {
  margin: 0 0 6px;
  font-size: 1.35rem;
  font-weight: 600;
  color: #f1f5f9;
}
.login-card-sub {
  margin: 0;
  font-size: 14px;
  color: rgba(148, 163, 184, 0.95);
}

.login-form :deep(.el-form-item__label) {
  color: rgba(226, 232, 240, 0.9) !important;
  font-weight: 500;
}

.login-input-icon {
  color: rgba(148, 163, 184, 0.95);
  font-size: 16px;
}

.login-submit-row {
  margin-bottom: 8px;
}
.login-submit {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  transition:
    transform 0.2s ease,
    box-shadow 0.25s ease;
}
.login-submit:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(56, 189, 248, 0.28);
}
.login-submit-label {
  display: inline-block;
  animation: login-submit-idle 3s ease-in-out infinite;
}
@keyframes login-submit-idle {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.92;
  }
}

.login-footer-link {
  text-align: center;
  padding-top: 4px;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 440px;
  }
  .login-hero {
    text-align: center;
  }
  .login-brand-badge {
    margin-left: auto;
    margin-right: auto;
  }
  .login-lead {
    margin-left: auto;
    margin-right: auto;
  }
  .login-pills {
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-orb,
  .login-brand-badge,
  .login-title-accent,
  .login-card-glow,
  .login-card-outer::before,
  .login-submit-label {
    animation: none !important;
  }
  .login-shell,
  .login-hero,
  .login-card-outer,
  .login-pill {
    animation: none !important;
    opacity: 1;
    transform: none;
  }
}
</style>
