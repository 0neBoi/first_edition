<template>
  <div class="settings-page">
    <div class="settings-inner">
      <div class="settings-hero settings-hero-row">
        <div class="settings-hero-text">
          <header>
            <p class="settings-eyebrow">偏好与账号</p>
            <h1>设置</h1>
            <p class="settings-lead">
              在此统一管理登录安全、通知偏好与本地体验选项。下方内容较多，可随时上下滚动浏览；所有仅保存在浏览器的选项都会标注「本机」。
            </p>
          </header>
        </div>
        <div class="settings-hero-art">
          <PixelHeroBits variant="settings" />
        </div>
      </div>

      <!-- 账号与安全 -->
      <el-card class="settings-card" shadow="hover">
        <template #header>
          <div class="card-head">
            <div class="card-head-pix">
              <PixelIcon name="lock" size="sm" />
              <span class="card-title">账号与安全</span>
            </div>
            <span class="card-desc">修改登录密码，建议定期更换</span>
          </div>
        </template>
        <el-form :model="pwdForm" label-position="top" class="settings-form">
          <el-form-item label="原密码">
            <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password clearable />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwdForm.newPassword" type="password" placeholder="至少 6 位" show-password clearable />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="changingPwd" @click="changePassword">保存新密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 通知与提醒（本机） -->
      <el-card class="settings-card" shadow="hover">
        <template #header>
          <div class="card-head">
            <div class="card-head-pix">
              <PixelIcon name="bell" size="sm" />
              <span class="card-title">通知与提醒</span>
            </div>
            <span class="card-desc">本机 · 控制练习与复习相关提示（不影响服务端）</span>
          </div>
        </template>
        <div class="settings-rows">
          <div class="settings-row">
            <div>
              <div class="row-label">练习完成提示</div>
              <div class="row-hint">答题会话结束时显示小结</div>
            </div>
            <el-switch v-model="prefs.notifyPracticeDone" @change="savePrefs" />
          </div>
          <div class="settings-row">
            <div>
              <div class="row-label">复习清单到期提醒</div>
              <div class="row-hint">进入复习页时高亮今日到期项</div>
            </div>
            <el-switch v-model="prefs.notifyReviewDue" @change="savePrefs" />
          </div>
          <div class="settings-row">
            <div>
              <div class="row-label">待办截止前提醒</div>
              <div class="row-hint">在待办列表中标记即将到期</div>
            </div>
            <el-switch v-model="prefs.notifyTodoSoon" @change="savePrefs" />
          </div>
        </div>
      </el-card>

      <!-- 学习与练习 -->
      <el-card class="settings-card" shadow="hover">
        <template #header>
          <div class="card-head">
            <div class="card-head-pix">
              <PixelIcon name="practice" size="sm" />
              <span class="card-title">学习与练习</span>
            </div>
            <span class="card-desc">本机 · 影响部分页面的默认行为</span>
          </div>
        </template>
        <div class="settings-rows">
          <div class="settings-row">
            <div>
              <div class="row-label">练习面板紧凑布局</div>
              <div class="row-hint">减少留白，一屏展示更多题干</div>
            </div>
            <el-switch v-model="prefs.compactPractice" @change="savePrefs" />
          </div>
          <div class="settings-row">
            <div>
              <div class="row-label">答错后自动展开解析</div>
              <div class="row-hint">提交错误答案后立即展开参考解析</div>
            </div>
            <el-switch v-model="prefs.autoExpandWrong" @change="savePrefs" />
          </div>
          <div class="settings-row settings-row-block">
            <div class="row-label">默认出题数量</div>
            <el-radio-group v-model="prefs.defaultQuestionCount" class="radio-group" @change="savePrefs">
              <el-radio label="5">5 题</el-radio>
              <el-radio label="10">10 题</el-radio>
              <el-radio label="15">15 题</el-radio>
            </el-radio-group>
          </div>
        </div>
      </el-card>

      <!-- 界面与显示 -->
      <el-card class="settings-card" shadow="hover">
        <template #header>
          <div class="card-head">
            <div class="card-head-pix">
              <PixelIcon name="palette" size="sm" />
              <span class="card-title">界面与显示</span>
            </div>
            <span class="card-desc">本机 · 阅读舒适度与动效</span>
          </div>
        </template>
        <div class="settings-rows">
          <div class="settings-row settings-row-block">
            <div class="row-label">主体颜色（霓虹像素风）</div>
            <div class="accent-swatches" role="radiogroup" aria-label="选择主体颜色">
              <button
                v-for="a in accentOptions"
                :key="a.id"
                type="button"
                class="accent-swatch"
                :class="{ 'is-active': prefs.accentId === a.id }"
                :title="a.label"
                :style="{ '--swatch': a.primary }"
                @click="onAccentPick(a.id)"
              >
                <span class="accent-swatch-inner" />
              </button>
            </div>
            <p class="row-note">
              全站为霓虹像素卡通风背景；此处仅切换主色（按钮高亮、侧栏描边等）。可在下方关闭「减少动态效果」以静止格子背景。
            </p>
          </div>
          <div class="settings-row settings-row-block">
            <div class="row-label">正文字号偏好</div>
            <el-segmented v-model="prefs.fontScale" :options="fontOptions" block @change="savePrefs" />
            <p class="row-note">部分页面会参考此项；与系统无障碍设置可同时生效。</p>
          </div>
          <div class="settings-row">
            <div>
              <div class="row-label">减少动态效果</div>
              <div class="row-hint">弱化背景粒子、过渡动画（本机）</div>
            </div>
            <el-switch v-model="prefs.reduceMotion" @change="onReduceMotion" />
          </div>
          <div class="settings-row">
            <div>
              <div class="row-label">侧边栏折叠记忆</div>
              <div class="row-hint">预留：后续若支持折叠侧栏将读取此项</div>
            </div>
            <el-switch v-model="prefs.rememberSidebar" @change="savePrefs" />
          </div>
        </div>
      </el-card>

      <!-- 数据与隐私 -->
      <el-card class="settings-card" shadow="hover">
        <template #header>
          <div class="card-head">
            <div class="card-head-pix">
              <PixelIcon name="database" size="sm" />
              <span class="card-title">数据与隐私</span>
            </div>
            <span class="card-desc">浏览器本地缓存与离线数据</span>
          </div>
        </template>
        <p class="block-text">
          课程表、部分待办与练习草稿可能保存在本机 localStorage。清除缓存不会删除服务器上的账号资料与已上传文件，但会移除本地课表与未同步草稿，请谨慎操作。
        </p>
        <div class="settings-actions">
          <el-button @click="clearLocalDrafts">清除本地草稿与临时缓存</el-button>
          <el-button type="danger" plain @click="clearAllLocalExceptAuth">清除全部本地数据（保留登录态）</el-button>
        </div>
      </el-card>

      <!-- 关于 -->
      <el-card class="settings-card settings-card-last" shadow="hover">
        <template #header>
          <div class="card-head">
            <div class="card-head-pix">
              <PixelIcon name="info" size="sm" />
              <span class="card-title">关于与帮助</span>
            </div>
            <span class="card-desc">校园工具 · 学习效率中台</span>
          </div>
        </template>
        <ul class="about-list">
          <li><span>前端版本</span><span>Web {{ appVersion }}</span></li>
          <li><span>界面主题</span><span>霓虹像素卡通风 · 主体色在上方选择</span></li>
          <li>
            <span>相关页面</span>
            <span>
              <router-link to="/profile">个人中心</router-link>
              ·
              <router-link to="/home">首页</router-link>
            </span>
          </li>
        </ul>
        <p class="block-text muted">
          若修改密码后无法继续操作，请尝试重新登录。仅保存在本机的选项存储在当前浏览器中，更换设备或清除站点数据后需重新设置。
        </p>
      </el-card>

      <div class="settings-spacer" aria-hidden="true" />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authApi } from '../api'
import { applyAccent, ACCENT_PRESETS } from '../theme'
import PixelHeroBits from '../components/PixelHeroBits.vue'

const STORAGE_KEY = 'sh_app_prefs'
const appVersion = '1.0'

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const changingPwd = ref(false)

const defaultPrefs = () => ({
  notifyPracticeDone: true,
  notifyReviewDue: true,
  notifyTodoSoon: true,
  compactPractice: false,
  autoExpandWrong: true,
  defaultQuestionCount: '10',
  accentId: 'cyan',
  fontScale: 'default',
  reduceMotion: false,
  rememberSidebar: true,
})

const prefs = reactive(defaultPrefs())

const accentOptions = ACCENT_PRESETS

const fontOptions = [
  { label: '标准', value: 'default' },
  { label: '稍大', value: 'large' },
  { label: '更大', value: 'larger' },
]

function loadPrefs() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      Object.assign(prefs, defaultPrefs())
      return
    }
    const o = JSON.parse(raw)
    Object.assign(prefs, defaultPrefs(), o)
  } catch {
    Object.assign(prefs, defaultPrefs())
  }
}

function savePrefs() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...prefs }))
    ElMessage.success('偏好已保存到本机')
  } catch {
    ElMessage.error('无法写入本地存储')
  }
}

function onAccentPick(id) {
  prefs.accentId = id
  savePrefs()
  applyAccent(prefs.accentId)
}

function onReduceMotion() {
  savePrefs()
  document.documentElement.dataset.reduceMotion = prefs.reduceMotion ? '1' : ''
}

async function changePassword() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changingPwd.value = true
  try {
    await authApi.updatePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    changingPwd.value = false
  }
}

function clearLocalDrafts() {
  ElMessageBox.confirm('将清除本地草稿类键，不影响已登录账号与服务器数据。是否继续？', '清除草稿', {
    type: 'warning',
    confirmButtonText: '清除',
    cancelButtonText: '取消',
  })
    .then(() => {
      const keys = Object.keys(localStorage)
      let n = 0
      keys.forEach((k) => {
        if (/draft|temp|cache/i.test(k) && k !== 'token' && k !== 'user') {
          localStorage.removeItem(k)
          n += 1
        }
      })
      ElMessage.success(n ? `已清除 ${n} 项` : '无匹配的草稿项')
    })
    .catch(() => {})
}

function clearAllLocalExceptAuth() {
  ElMessageBox.confirm(
    '将清除本站点除登录信息外的全部本地数据（含课表、待办缓存等）。确定继续？',
    '危险操作',
    {
      type: 'warning',
      confirmButtonText: '确定清除',
      cancelButtonText: '取消',
    },
  )
    .then(() => {
      const token = localStorage.getItem('token')
      const user = localStorage.getItem('user')
      localStorage.clear()
      if (token) localStorage.setItem('token', token)
      if (user) localStorage.setItem('user', user)
      loadPrefs()
      onReduceMotion()
      ElMessage.success('已清除，偏好已重新加载')
    })
    .catch(() => {})
}

onMounted(() => {
  loadPrefs()
  applyAccent(prefs.accentId)
  document.documentElement.dataset.reduceMotion = prefs.reduceMotion ? '1' : ''
})
</script>

<style scoped>
.settings-page {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 24px clamp(16px, 3vw, 40px) 80px;
}

.settings-inner {
  max-width: 720px;
  margin: 0 auto;
}

.settings-hero.settings-hero-row {
  margin-bottom: 28px;
}

.settings-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(186, 230, 253, 0.75);
}

.settings-hero h1 {
  margin: 0 0 12px;
  font-size: clamp(1.5rem, 3vw, 1.85rem);
  font-weight: 700;
  color: #f8fafc;
}

.settings-lead {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: rgba(203, 213, 225, 0.92);
  max-width: 52em;
}

.settings-card {
  margin-bottom: 18px;
  border-radius: 16px !important;
}

.settings-card-last {
  margin-bottom: 0;
}

.card-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.card-desc {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
  font-weight: normal;
}

.settings-form :deep(.el-form-item__label) {
  color: rgba(226, 232, 240, 0.9) !important;
}

.settings-rows {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.settings-row-block {
  flex-direction: column;
  align-items: stretch;
}

.row-label {
  font-size: 14px;
  color: #e5e7eb;
  font-weight: 500;
}

.row-hint {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
  margin-top: 2px;
}

.row-note {
  margin: 8px 0 0;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.9);
}

.radio-group {
  margin-top: 8px;
}

.block-text {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(203, 213, 225, 0.9);
}

.block-text.muted {
  margin-bottom: 0;
  margin-top: 16px;
  color: rgba(148, 163, 184, 0.95);
}

.settings-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.about-list {
  list-style: none;
  margin: 0 0 16px;
  padding: 0;
}

.about-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 13px;
  color: #e5e7eb;
}

.about-list li:last-child {
  border-bottom: none;
}

.about-list a {
  color: var(--el-color-primary-light-3);
  text-decoration: none;
}

.about-list a:hover {
  text-decoration: underline;
}

.accent-swatches {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}

.accent-swatch {
  width: 36px;
  height: 36px;
  padding: 0;
  border-radius: 8px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  background: var(--glass-60);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition:
    transform 0.12s ease,
    box-shadow 0.12s ease;
}

.accent-swatch:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 14px color-mix(in srgb, var(--swatch) 45%, transparent);
}

.accent-swatch.is-active {
  border-color: var(--swatch);
  box-shadow:
    0 0 0 2px var(--glass-90),
    0 0 16px color-mix(in srgb, var(--swatch) 55%, transparent);
}

.accent-swatch-inner {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: var(--swatch);
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.25);
}

.settings-spacer {
  height: 48px;
}
</style>
