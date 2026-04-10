<template>
  <div class="register-page">
    <div class="register-orbs" aria-hidden="true">
      <span class="register-orb register-orb-a" />
      <span class="register-orb register-orb-b" />
      <span class="register-orb register-orb-c" />
    </div>

    <div class="register-shell">
      <section class="register-hero" aria-labelledby="register-brand-title">
        <div class="register-brand-badge">
          <span class="register-brand-icon">新</span>
        </div>
        <p class="register-eyebrow">加入校园工具</p>
        <h1 id="register-brand-title" class="register-title">
          开启你的<br />
          <span class="register-title-accent">学习账号</span>
        </h1>
        <p class="register-lead">注册后即可同步笔记、待办、练习记录与 AI 问答历史，数据与你账号绑定。</p>
        <ul class="register-pills">
          <li
            v-for="(t, i) in heroPills"
            :key="t"
            class="register-pill"
            :style="{ animationDelay: `${0.35 + i * 0.08}s` }"
          >
            {{ t }}
          </li>
        </ul>
      </section>

      <div class="register-card-outer">
        <div class="register-card-glow" aria-hidden="true" />
        <el-card class="register-card" shadow="never">
          <div class="register-card-inner">
            <header class="register-card-head">
              <h2 class="register-card-title">创建账号</h2>
              <p class="register-card-sub">填写信息完成注册</p>
            </header>

            <el-form :model="form" label-position="top" class="register-form" @submit.prevent="submit">
              <el-form-item label="用户名">
                <el-input v-model="form.username" placeholder="请输入用户名（登录用）" clearable>
                  <template #prefix>
                    <span class="login-input-pix"><PixelIcon name="user" size="sm" /></span>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="form.password" type="password" placeholder="至少 6 位" show-password clearable>
                  <template #prefix>
                    <span class="login-input-pix"><PixelIcon name="lock" size="sm" /></span>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="昵称（选填）">
                <el-input v-model="form.nickname" placeholder="显示名称，不填则用用户名" clearable>
                  <template #prefix>
                    <span class="login-input-pix"><PixelIcon name="notes" size="sm" /></span>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item class="register-submit-row">
                <el-button type="primary" class="register-submit" :loading="loading" native-type="submit">
                  <span class="register-submit-label">注册</span>
                </el-button>
              </el-form-item>
              <div class="register-footer-link">
                <el-button link type="primary" @click="goLogin">已有账号？去登录</el-button>
              </div>
            </el-form>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

const heroPills = ['同步多端', '错题沉淀', '学习报告', '校园服务']

async function submit() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!form.password || form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    const data = await authApi.register(form.username, form.password, form.nickname)
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
    ElMessage.success('注册成功')
    router.replace(route.query.redirect || '/home')
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.register-page {
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

.register-orbs {
  pointer-events: none;
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}
.register-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(64px);
  opacity: 0.45;
  animation: register-orb-float 14s ease-in-out infinite;
}
.register-orb-a {
  width: min(42vw, 320px);
  height: min(42vw, 320px);
  left: -8%;
  top: 12%;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.55) 0%, transparent 70%);
  animation-delay: 0s;
}
.register-orb-b {
  width: min(36vw, 280px);
  height: min(36vw, 280px);
  right: -5%;
  bottom: 18%;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.42) 0%, transparent 70%);
  animation-delay: -4s;
  animation-duration: 18s;
}
.register-orb-c {
  width: min(28vw, 200px);
  height: min(28vw, 200px);
  left: 38%;
  bottom: -5%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.35) 0%, transparent 70%);
  animation-delay: -7s;
  animation-duration: 16s;
}
@keyframes register-orb-float {
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

.register-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1040px;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(300px, 400px);
  gap: clamp(32px, 6vw, 72px);
  align-items: center;
  animation: register-shell-in 0.85s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes register-shell-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.register-hero {
  animation: register-hero-in 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}
@keyframes register-hero-in {
  from {
    opacity: 0;
    transform: translateX(-16px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.register-brand-badge {
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
  animation: register-badge-pulse 4s ease-in-out infinite;
}
@keyframes register-badge-pulse {
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
.register-brand-icon {
  font-size: 22px;
  font-weight: 700;
  color: #f0f9ff;
  text-shadow: 0 0 24px rgba(56, 189, 248, 0.5);
}

.register-eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(186, 230, 253, 0.75);
}

.register-title {
  margin: 0 0 16px;
  font-size: clamp(1.75rem, 4vw, 2.25rem);
  font-weight: 700;
  line-height: 1.25;
  color: #f8fafc;
  letter-spacing: -0.02em;
}
.register-title-accent {
  background: linear-gradient(110deg, #7dd3fc 0%, #c4b5fd 45%, #22d3ee 100%);
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: register-shimmer 7s ease-in-out infinite;
}
@keyframes register-shimmer {
  0%,
  100% {
    background-position: 0% center;
  }
  50% {
    background-position: 100% center;
  }
}

.register-lead {
  margin: 0 0 24px;
  max-width: 36em;
  font-size: 15px;
  line-height: 1.65;
  color: rgba(226, 232, 240, 0.82);
}

.register-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.register-pill {
  padding: 8px 14px;
  font-size: 13px;
  color: rgba(241, 245, 249, 0.92);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  backdrop-filter: blur(8px);
  animation: register-pill-in 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes register-pill-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.register-card-outer {
  position: relative;
  animation: register-card-in 0.85s cubic-bezier(0.22, 1, 0.36, 1) 0.15s both;
}
@keyframes register-card-in {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.register-card-glow {
  position: absolute;
  inset: -40%;
  background: conic-gradient(
    from 180deg at 50% 50%,
    rgba(56, 189, 248, 0.15),
    rgba(168, 85, 247, 0.12),
    rgba(34, 211, 238, 0.14),
    rgba(56, 189, 248, 0.15)
  );
  animation: register-glow-spin 18s linear infinite;
  opacity: 0.65;
  filter: blur(28px);
  pointer-events: none;
}
@keyframes register-glow-spin {
  to {
    transform: rotate(360deg);
  }
}

.register-card-outer::before {
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
  animation: register-border-flow 8s ease infinite;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
@keyframes register-border-flow {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.register-card {
  position: relative;
  z-index: 1;
  border-radius: 20px !important;
  overflow: hidden;
  border: none !important;
}

.register-card-inner {
  padding: 4px 4px 8px;
}

.register-card-head {
  margin-bottom: 22px;
  text-align: center;
}
.register-card-title {
  margin: 0 0 6px;
  font-size: 1.35rem;
  font-weight: 600;
  color: #f1f5f9;
}
.register-card-sub {
  margin: 0;
  font-size: 14px;
  color: rgba(148, 163, 184, 0.95);
}

.register-form :deep(.el-form-item__label) {
  color: rgba(226, 232, 240, 0.9) !important;
  font-weight: 500;
}

.register-input-icon {
  color: rgba(148, 163, 184, 0.95);
  font-size: 16px;
}

.register-submit-row {
  margin-bottom: 8px;
}
.register-submit {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  transition:
    transform 0.2s ease,
    box-shadow 0.25s ease;
}
.register-submit:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(56, 189, 248, 0.28);
}
.register-submit-label {
  display: inline-block;
  animation: register-submit-idle 3s ease-in-out infinite;
}
@keyframes register-submit-idle {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.92;
  }
}

.register-footer-link {
  text-align: center;
  padding-top: 4px;
}

@media (max-width: 900px) {
  .register-shell {
    grid-template-columns: 1fr;
    max-width: 440px;
  }
  .register-hero {
    text-align: center;
  }
  .register-brand-badge {
    margin-left: auto;
    margin-right: auto;
  }
  .register-lead {
    margin-left: auto;
    margin-right: auto;
  }
  .register-pills {
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .register-orb,
  .register-brand-badge,
  .register-title-accent,
  .register-card-glow,
  .register-card-outer::before,
  .register-submit-label {
    animation: none !important;
  }
  .register-shell,
  .register-hero,
  .register-card-outer,
  .register-pill {
    animation: none !important;
    opacity: 1;
    transform: none;
  }
}
</style>
