<template>
  <el-container class="layout" :class="{ 'layout--auth': isAuthPage }">
    <el-aside v-if="!isAuthPage" class="sidebar" width="220px">
      <div class="sidebar-inner">
        <div class="sidebar-top">
          <template v-if="user">
            <router-link v-if="user.role !== 'ADMIN'" to="/profile" class="user-link user-link-top">
              <el-avatar :size="40" :src="headerAvatarSrc" class="header-avatar">
                {{ (user.nickname || user.username || '').slice(0, 1) }}
              </el-avatar>
              <div class="user-text">
                <span class="nickname">{{ user.nickname || user.username }}</span>
                <span class="user-sub">查看个人中心</span>
              </div>
            </router-link>
            <div v-else class="user-link admin-user-strip user-link-top">
              <el-avatar :size="40" :src="headerAvatarSrc" class="header-avatar">
                {{ (user.nickname || user.username || '').slice(0, 1) }}
              </el-avatar>
              <div class="user-text">
                <span class="nickname">{{ user.nickname || user.username }}</span>
                <span class="user-sub">管理员</span>
              </div>
            </div>
          </template>
          <div v-else class="sidebar-guest">
            <span class="guest-hint">未登录</span>
            <div class="guest-links">
              <router-link to="/login" class="auth-link">登录</router-link>
              <router-link to="/register" class="auth-link">注册</router-link>
            </div>
          </div>
        </div>

        <router-link :to="user?.role === 'ADMIN' ? '/admin/notice' : '/guide'" class="sidebar-brand">
          <PixelIcon name="school" class="sidebar-brand-pix" size="lg" />
          <span>校园工具</span>
        </router-link>
        <div class="sidebar-wheel-shell">
        <div
          ref="sidebarMenuWheelRef"
          class="sidebar-menu-wheel"
          @scroll.passive="scheduleIosWheel"
          @click="onSidebarNavClick"
        >
        <el-menu
          v-if="user?.role === 'ADMIN'"
          :router="true"
          :default-active="activeMenu"
          class="sidebar-menu admin-sidebar-menu"
        >
          <el-menu-item index="/admin/notice" class="nav-tile nav-tile--admin-notice">
            <span class="nav-with-pix"><PixelIcon name="admin" size="nav" /><span>公告管理</span></span>
          </el-menu-item>
          <el-menu-item index="/admin/users" class="nav-tile nav-tile--admin-users">
            <span class="nav-with-pix"><PixelIcon name="user" size="nav" /><span>用户管理</span></span>
          </el-menu-item>
          <el-menu-item index="/game/dino" class="nav-tile nav-tile--dino">
            <span class="nav-with-pix"><PixelIcon name="dino" size="nav" /><span>小恐龙跑酷</span></span>
          </el-menu-item>
          <el-menu-item index="/game/plane" class="nav-tile nav-tile--plane">
            <span class="nav-with-pix"><PixelIcon name="plane" size="nav" /><span>飞机大战</span></span>
          </el-menu-item>
          <el-menu-item index="/plaza/discussion" class="nav-tile nav-tile--plaza-discuss">
            <span class="nav-with-pix"><PixelIcon name="plaza" size="nav" /><span>帖子交流</span></span>
          </el-menu-item>
          <el-menu-item index="/plaza/market" class="nav-tile nav-tile--plaza-market">
            <span class="nav-with-pix"><PixelIcon name="market" size="nav" /><span>闲置购物</span></span>
          </el-menu-item>
          <el-menu-item index="/messages" class="nav-tile nav-tile--messages">
            <span class="nav-with-pix"><PixelIcon name="messages" size="nav" /><span>私信</span></span>
          </el-menu-item>
          <el-menu-item index="/settings" class="nav-tile nav-tile--settings">
            <span class="nav-with-pix"><PixelIcon name="settings" size="nav" /><span>设置</span></span>
          </el-menu-item>
        </el-menu>
        <el-menu
          v-else
          :router="true"
          :default-active="activeMenu"
          :default-openeds="sidebarDefaultOpeneds"
          :unique-opened="false"
          class="sidebar-menu"
        >
          <el-menu-item index="/home" class="nav-tile nav-tile--home">
            <span class="nav-with-pix"><PixelIcon name="home" size="nav" /><span>首页</span></span>
          </el-menu-item>
          <el-menu-item index="/game/dino" class="nav-tile nav-tile--dino">
            <span class="nav-with-pix"><PixelIcon name="dino" size="nav" /><span>小恐龙跑酷</span></span>
          </el-menu-item>
          <el-menu-item index="/game/plane" class="nav-tile nav-tile--plane">
            <span class="nav-with-pix"><PixelIcon name="plane" size="nav" /><span>飞机大战</span></span>
          </el-menu-item>
          <el-sub-menu index="materials-hub" class="nav-tile nav-tile--materials">
            <template #title>
              <span class="nav-with-pix"><PixelIcon name="folder" size="nav" /><span>资料列表</span></span>
            </template>
            <el-menu-item index="/materials" class="nav-tile nav-tile--materials-all">
              <span class="nav-with-pix"><PixelIcon name="materials" size="nav" /><span>全部资料</span></span>
            </el-menu-item>
            <el-menu-item index="/materials/refine" class="nav-tile nav-tile--refine">
              <span class="nav-with-pix"><PixelIcon name="refine" size="nav" /><span>提炼</span></span>
            </el-menu-item>
            <el-menu-item index="/materials/questions" class="nav-tile nav-tile--questions">
              <span class="nav-with-pix"><PixelIcon name="questions" size="nav" /><span>出题</span></span>
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="study-hub" class="nav-tile nav-tile--study">
            <template #title>
              <span class="nav-with-pix"><PixelIcon name="study" size="nav" /><span>学习</span></span>
            </template>
            <el-menu-item index="/notes" class="nav-tile nav-tile--notes">
              <span class="nav-with-pix"><PixelIcon name="notes" size="nav" /><span>笔记</span></span>
            </el-menu-item>
            <el-menu-item index="/todos" class="nav-tile nav-tile--todos">
              <span class="nav-with-pix"><PixelIcon name="todos" size="nav" /><span>待办</span></span>
            </el-menu-item>
            <el-menu-item index="/clock" class="nav-tile nav-tile--clock">
              <span class="nav-with-pix"><PixelIcon name="clock" size="nav" /><span>打卡</span></span>
            </el-menu-item>
            <el-menu-item index="/practice" class="nav-tile nav-tile--practice">
              <span class="nav-with-pix"><PixelIcon name="practice" size="nav" /><span>练习</span></span>
            </el-menu-item>
            <el-menu-item index="/wrong-book" class="nav-tile nav-tile--wrong">
              <span class="nav-with-pix"><PixelIcon name="wrong" size="nav" /><span>错题本</span></span>
            </el-menu-item>
            <el-menu-item index="/review" class="nav-tile nav-tile--review">
              <span class="nav-with-pix"><PixelIcon name="review" size="nav" /><span>复习清单</span></span>
            </el-menu-item>
            <el-menu-item index="/report" class="nav-tile nav-tile--report">
              <span class="nav-with-pix"><PixelIcon name="report" size="nav" /><span>学习报告</span></span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/ask" class="nav-tile nav-tile--ask">
            <span class="nav-with-pix"><PixelIcon name="spark" size="nav" /><span>AI 问答</span></span>
          </el-menu-item>
          <el-menu-item index="/schedule" class="nav-tile nav-tile--schedule">
            <span class="nav-with-pix"><PixelIcon name="schedule" size="nav" /><span>课程表</span></span>
          </el-menu-item>
          <el-menu-item index="/map" class="nav-tile nav-tile--map">
            <span class="nav-with-pix"><PixelIcon name="map" size="nav" /><span>校园地图</span></span>
          </el-menu-item>
          <el-menu-item index="/notice" class="nav-tile nav-tile--notice">
            <span class="nav-with-pix"><PixelIcon name="notice" size="nav" /><span>校园公告</span></span>
          </el-menu-item>
          <el-menu-item index="/plaza/discussion" class="nav-tile nav-tile--plaza-discuss">
            <span class="nav-with-pix"><PixelIcon name="plaza" size="nav" /><span>帖子交流</span></span>
          </el-menu-item>
          <el-menu-item index="/plaza/market" class="nav-tile nav-tile--plaza-market">
            <span class="nav-with-pix"><PixelIcon name="market" size="nav" /><span>闲置购物</span></span>
          </el-menu-item>
          <el-menu-item index="/messages" class="nav-tile nav-tile--messages">
            <span class="nav-with-pix"><PixelIcon name="messages" size="nav" /><span>私信</span></span>
          </el-menu-item>
          <el-menu-item index="/profile" class="nav-tile nav-tile--profile">
            <span class="nav-with-pix"><PixelIcon name="profile" size="nav" /><span>个人中心</span></span>
          </el-menu-item>
          <el-menu-item index="/settings" class="nav-tile nav-tile--settings">
            <span class="nav-with-pix"><PixelIcon name="settings" size="nav" /><span>设置</span></span>
          </el-menu-item>
        </el-menu>
        </div>
        <div class="sidebar-wheel-pointer" aria-hidden="true" />
        </div>
        <div v-if="user" class="sidebar-footer">
          <el-button type="primary" link class="sidebar-logout" @click="logout">退出登录</el-button>
        </div>
      </div>
    </el-aside>
    <el-main class="main" :class="{ 'is-study': isStudyRoute }">
      <div v-show="showNeonBg" class="neon-bg-layer neon-bg-anim" aria-hidden="true">
        <div class="neon-grid" />
        <div class="neon-glow-orbs" />
      </div>
      <div v-show="showNeonBg" class="tetris-float-wrap" aria-hidden="true">
        <TetrisFloatBg />
      </div>
      <div class="study-main-inner">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAvatarUrl, authApi } from './api'
import { readAccentId, applyAccent } from './theme'
import TetrisFloatBg from './components/TetrisFloatBg.vue'

const router = useRouter()
const route = useRoute()
const user = ref(null)

const headerAvatarSrc = computed(() => getAvatarUrl(user.value?.avatar))

const isStudyRoute = computed(() => route.matched.some((r) => r.meta.studyGlass))

/** 登录 / 注册页不显示侧栏 */
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')

/** 全局霓虹像素底（登录/注册页不叠主区背景层） */
const showNeonBg = computed(() => !isAuthPage.value)

/** iOS 时间选择器式圆柱滚轮：按项相对视区中心的偏移施加 rotateX（非整栏倾斜） */
const sidebarMenuWheelRef = ref(null)
let iosWheelRaf = 0
let iosWheelCleanup = null

function scheduleIosWheel() {
  cancelAnimationFrame(iosWheelRaf)
  iosWheelRaf = requestAnimationFrame(updateIosWheel)
}

function prefersReducedMotion() {
  return (
    document.documentElement.dataset.reduceMotion === '1' ||
    (typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches)
  )
}

/** 将点击的侧栏项平滑滚动到滚轮视区垂直中线（圆柱导航） */
function scrollNavItemToCenter(li) {
  const container = sidebarMenuWheelRef.value
  if (!container || !li || !container.contains(li)) return
  const cr = container.getBoundingClientRect()
  const tr = li.getBoundingClientRect()
  const delta = tr.top + tr.height / 2 - (cr.top + cr.height / 2)
  const maxScroll = Math.max(0, container.scrollHeight - container.clientHeight)
  const targetTop = Math.round(Math.max(0, Math.min(maxScroll, container.scrollTop + delta)))
  if (prefersReducedMotion()) {
    container.scrollTop = targetTop
  } else {
    container.scrollTo({ top: targetTop, behavior: 'smooth' })
  }
  scheduleIosWheel()
}

function onSidebarNavClick(e) {
  const menu = sidebarMenuWheelRef.value?.querySelector('.sidebar-menu')
  if (!menu) return
  const li = e.target.closest('li.el-menu-item, li.el-sub-menu')
  if (!li || !menu.contains(li)) return
  /* 子菜单展开后高度变化，略延迟再算几何，过渡更准 */
  nextTick(() => {
    requestAnimationFrame(() => {
      setTimeout(() => scrollNavItemToCenter(li), 60)
    })
  })
}

/** 滚轮在首尾「无限循环」：到底继续向下回顶，到顶继续向上回底 */
function onSidebarWheelLoop(e) {
  const el = sidebarMenuWheelRef.value
  if (!el) return
  const max = el.scrollHeight - el.clientHeight
  if (max <= 4) return
  const threshold = 8
  const reduce = prefersReducedMotion()
  if (e.deltaY > 0 && el.scrollTop >= max - threshold) {
    e.preventDefault()
    if (reduce) el.scrollTop = 0
    else el.scrollTo({ top: 0, behavior: 'smooth' })
    scheduleIosWheel()
  } else if (e.deltaY < 0 && el.scrollTop <= threshold) {
    e.preventDefault()
    if (reduce) el.scrollTop = max
    else el.scrollTo({ top: max, behavior: 'smooth' })
    scheduleIosWheel()
  }
}

/** 上下留白：含子菜单项在内任意一行都能对齐视区中心（取所有 li 所需边距的最大值） */
function updateWheelPadding() {
  const container = sidebarMenuWheelRef.value
  if (!container) return
  const menu = container.querySelector('.sidebar-menu')
  if (!menu) return
  const allItems = [...menu.querySelectorAll('li.el-menu-item, li.el-sub-menu')].filter(
    (li) => li.offsetHeight > 4,
  )
  if (allItems.length === 0) return
  const h = container.clientHeight
  if (h < 8) return
  const edgePad = Math.max(
    0,
    ...allItems.map((li) => Math.max(0, h / 2 - li.offsetHeight / 2)),
  )
  menu.style.paddingTop = `${edgePad}px`
  menu.style.paddingBottom = `${edgePad}px`
}

function updateIosWheel() {
  const container = sidebarMenuWheelRef.value
  if (!container) return
  const menu = container.querySelector('.sidebar-menu')
  if (!menu) return

  const reduceMotion = document.documentElement.dataset.reduceMotion === '1'
  const items = menu.querySelectorAll('li.el-menu-item, li.el-sub-menu')
  const cRect = container.getBoundingClientRect()
  if (cRect.height < 8) return

  const centerY = cRect.top + cRect.height / 2
  const halfH = cRect.height / 2

  let closest = null
  let closestDist = Infinity

  items.forEach((li) => {
    li.classList.remove('nav-wheel-center')
    if (reduceMotion) {
      li.style.removeProperty('transform')
      return
    }
    const ir = li.getBoundingClientRect()
    if (ir.height < 1) return
    const itemCenterY = ir.top + ir.height / 2
    const dist = Math.abs(itemCenterY - centerY)
    if (dist < closestDist) {
      closestDist = dist
      closest = li
    }
    const norm = (itemCenterY - centerY) / halfH
    const clamped = Math.max(-1, Math.min(1, norm))
    /* 用 asin 映射更接近圆柱面；中心行 translateZ 略大，边缘略沉入 */
    const asinDeg = Math.asin(clamped) * (180 / Math.PI)
    const rotateX = -asinDeg * 0.52
    const scale = 1 - Math.abs(clamped) * 0.11
    const tz = 30 * (1 - Math.abs(clamped))
    li.style.transformOrigin = 'center center'
    li.style.transform = `rotateX(${rotateX}deg) translateZ(${tz}px) scale(${scale})`
  })

  if (!reduceMotion && closest) {
    closest.classList.add('nav-wheel-center')
  }
}

function bindIosWheel() {
  iosWheelCleanup?.()
  iosWheelCleanup = null
  const el = sidebarMenuWheelRef.value
  if (!el) return

  const onScroll = () => scheduleIosWheel()
  const onResize = () => {
    updateWheelPadding()
    scheduleIosWheel()
  }
  el.addEventListener('scroll', onScroll, { passive: true })
  el.addEventListener('wheel', onSidebarWheelLoop, { passive: false })
  window.addEventListener('resize', onResize, { passive: true })

  const ro = new ResizeObserver(onResize)
  ro.observe(el)

  const menu = el.querySelector('.sidebar-menu')
  let mo = null
  if (menu) {
    mo = new MutationObserver(onResize)
    mo.observe(menu, {
      subtree: true,
      childList: true,
      attributes: true,
      attributeFilter: ['class', 'style'],
    })
  }

  updateWheelPadding()
  scheduleIosWheel()

  iosWheelCleanup = () => {
    el.removeEventListener('scroll', onScroll)
    el.removeEventListener('wheel', onSidebarWheelLoop)
    window.removeEventListener('resize', onResize)
    ro.disconnect()
    mo?.disconnect()
  }
}

watch(
  sidebarMenuWheelRef,
  (el) => {
    iosWheelCleanup?.()
    iosWheelCleanup = null
    if (el) {
      nextTick(() => {
        bindIosWheel()
      })
    }
  },
  { flush: 'post' },
)

watch(
  () => route.fullPath,
  () => {
    nextTick(() => {
      updateWheelPadding()
      scheduleIosWheel()
    })
  },
)

watch(
  () => user.value?.role,
  () => {
    nextTick(() => {
      bindIosWheel()
    })
  },
)

/** 保持资料/学习子菜单默认展开，避免进入资料页时像「侧栏消失」 */
const sidebarDefaultOpeneds = ['materials-hub', 'study-hub']

const activeMenu = computed(() => {
  const p = route.path
  if (p === '/ask') return '/ask'
  if (p.startsWith('/material/')) return '/materials'
  if (p.startsWith('/materials')) {
    if (p === '/materials/refine') return '/materials/refine'
    if (p === '/materials/questions') return '/materials/questions'
    return '/materials'
  }
  if (p.startsWith('/notes')) return '/notes'
  if (
    ['/todos', '/clock', '/practice', '/wrong-book', '/review', '/report'].includes(p)
  ) {
    return p
  }
  if (p === '/home') return '/home'
  if (p === '/guide') return '/__none__'
  if (p === '/game/dino') return '/game/dino'
  if (p === '/game/plane') return '/game/plane'
  if (['/schedule', '/map', '/notice'].includes(p)) return p
  if (p.startsWith('/plaza')) {
    if (p.includes('/market')) return '/plaza/market'
    return '/plaza/discussion'
  }
  if (p === '/messages') return '/messages'
  if (p === '/admin/notice') return '/admin/notice'
  if (p === '/admin/users') return '/admin/users'
  if (p === '/profile') return '/profile'
  if (p === '/settings') return '/settings'
  return '/home'
})

function loadUser() {
  try {
    const raw = localStorage.getItem('user')
    if (raw) user.value = JSON.parse(raw)
    else user.value = null
  } catch (_) {
    user.value = null
  }
}

/** 从服务端同步头像等（登录态 localStorage 可能缺 avatar） */
async function syncUserFromServer() {
  const token = localStorage.getItem('token')
  if (!token) return
  let prev = {}
  try {
    const raw = localStorage.getItem('user')
    if (raw) prev = JSON.parse(raw)
  } catch {
    return
  }
  if (prev.role === 'ADMIN') return
  try {
    const data = await authApi.me()
    const merged = {
      userId: data.id,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar || '',
      role: prev.role || 'STUDENT',
    }
    user.value = merged
    localStorage.setItem('user', JSON.stringify(merged))
  } catch {
    /* 保持仅 localStorage */
  }
}

function onUserUpdated() {
  loadUser()
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  user.value = null
  router.push('/login')
}

function applyStoredReduceMotion() {
  try {
    const raw = localStorage.getItem('sh_app_prefs')
    if (!raw) return
    const o = JSON.parse(raw)
    if (o.reduceMotion) document.documentElement.dataset.reduceMotion = '1'
    else document.documentElement.dataset.reduceMotion = ''
  } catch {
    /* ignore */
  }
}

function onThemeUpdated() {
  applyAccent(readAccentId())
}

onMounted(() => {
  applyAccent(readAccentId())
  loadUser()
  syncUserFromServer()
  applyStoredReduceMotion()
  window.addEventListener('sh-user-updated', onUserUpdated)
  window.addEventListener('sh-theme-updated', onThemeUpdated)
})
onBeforeUnmount(() => {
  iosWheelCleanup?.()
  iosWheelCleanup = null
  cancelAnimationFrame(iosWheelRaf)
  window.removeEventListener('sh-user-updated', onUserUpdated)
  window.removeEventListener('sh-theme-updated', onThemeUpdated)
})
router.afterEach(() => {
  loadUser()
})
</script>

<style>
* { box-sizing: border-box; }
body {
  margin: 0;
  background: var(--neon-bg-deep, #0a1628);
}
.layout {
  min-height: 100vh;
  flex-direction: row;
  align-items: stretch;
}
.layout.layout--auth {
  flex-direction: column;
}

.sidebar {
  flex-shrink: 0;
  width: 220px;
  min-width: 220px;
  height: 100vh;
  position: sticky;
  top: 0;
  align-self: flex-start;
  z-index: 30;
  isolation: isolate;
  border-right: 1px solid rgba(148, 163, 184, 0.28);
  background: var(--glass-98);
  color: #e5e7eb;
  /* 勿用 overflow:hidden，会与菜单区 3D rotateX 叠加把内容裁没，像侧栏消失 */
  overflow-x: hidden;
  overflow-y: visible;
}
.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.sidebar-top {
  flex-shrink: 0;
  padding: 16px 14px 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
}
.user-link-top {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: inherit;
  min-width: 0;
  border-radius: 12px;
  padding: 4px;
  margin: -4px;
  transition: background 0.15s ease;
}
.user-link-top:hover {
  background: rgba(255, 255, 255, 0.06);
}
.user-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.user-link-top .nickname {
  font-size: 14px;
  font-weight: 600;
  color: #f1f5f9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-sub {
  font-size: 11px;
  color: rgba(148, 163, 184, 0.95);
}
.sidebar-guest {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.guest-hint {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.9);
}
.guest-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
}

.sidebar-brand {
  flex-shrink: 0;
  display: block;
  padding: 14px 18px 12px;
  font-size: 17px;
  font-weight: 600;
  text-decoration: none;
  color: #f1f5f9;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
}
.sidebar-brand:hover {
  color: var(--el-color-primary-light-3);
}

/* 外壳不滚动：箭头放在此层，固定在滚盘视区中线，不随菜单滚动 */
.sidebar-wheel-shell {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.sidebar-menu-wheel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scroll-snap-type: y proximity;
  /* iOS 时间选择器式圆柱透视：视口中心为「选中」行 */
  perspective: 1280px;
  perspective-origin: 50% 50%;
  transform-style: preserve-3d;
  /* 上下渐变遮罩，类似 iOS 滚轮边缘淡出 */
  mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    rgba(0, 0, 0, 0.82) 6%,
    rgba(0, 0, 0, 1) 50%,
    rgba(0, 0, 0, 0.82) 94%,
    transparent 100%
  );
  -webkit-mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    rgba(0, 0, 0, 0.82) 6%,
    rgba(0, 0, 0, 1) 50%,
    rgba(0, 0, 0, 0.82) 94%,
    transparent 100%
  );
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.sidebar-wheel-pointer {
  position: absolute;
  left: 5px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 6;
  width: 0;
  height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 11px solid var(--el-color-primary);
  filter: drop-shadow(0 0 10px rgba(var(--neon-accent-rgb), 0.95));
  pointer-events: none;
}
.sidebar-menu-wheel .sidebar-menu li.el-menu-item,
.sidebar-menu-wheel .sidebar-menu li.el-sub-menu {
  scroll-snap-align: center;
}
.sidebar-menu-wheel::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.sidebar-menu {
  width: 100%;
  transform-style: preserve-3d;
  border-right: none !important;
  background: transparent !important;
  --el-menu-text-color: rgba(255, 255, 255, 0.88);
  --el-menu-hover-text-color: #ffffff;
  --el-menu-active-color: var(--el-color-primary);
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
}
.sidebar-menu .el-menu-item,
.sidebar-menu .el-sub-menu__title {
  color: rgba(255, 255, 255, 0.9);
}
.sidebar-menu .el-sub-menu .el-menu-item {
  min-width: auto;
}
.sidebar-menu.el-menu--vertical > .el-sub-menu:not(.is-active) .el-sub-menu__title {
  color: rgba(255, 255, 255, 0.9) !important;
}
.sidebar-menu.el-menu--vertical > .el-sub-menu:not(.is-active) .el-sub-menu__icon-arrow {
  color: rgba(255, 255, 255, 0.65) !important;
}
.sidebar-menu li.el-menu-item,
.sidebar-menu li.el-sub-menu {
  transform-style: preserve-3d;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  transition: none;
}
.sidebar-menu .el-menu-item.is-active:not(.nav-tile) {
  background: rgba(56, 189, 248, 0.12) !important;
}
.sidebar-menu .el-sub-menu.is-active > .el-sub-menu__title {
  color: var(--el-color-primary) !important;
}

/* 侧栏导航「磁贴」：每项独立语义色条 + 渐变底 */
.sidebar-menu li.nav-tile {
  margin: 4px 8px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
  overflow: hidden;
}
.sidebar-menu .el-sub-menu.nav-tile .el-sub-menu__title,
.sidebar-menu li.nav-tile.el-menu-item {
  background: transparent !important;
}
.sidebar-menu .el-sub-menu .el-menu .nav-tile {
  margin: 3px 6px 3px 10px;
  border-radius: 8px;
}
.sidebar-menu li.nav-tile:hover {
  filter: brightness(1.06);
}
.sidebar-menu li.nav-tile.el-menu-item.is-active:not(.nav-wheel-center) {
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.28) !important;
}
.sidebar-menu li.nav-tile.el-sub-menu.is-active:not(.nav-wheel-center) {
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.26) !important;
}

.sidebar-menu li.nav-tile.nav-tile--home {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.22) 0%, var(--glass-55) 100%);
  border-color: rgba(14, 165, 233, 0.35);
}
.sidebar-menu li.nav-tile.nav-tile--dino {
  background: linear-gradient(135deg, rgba(52, 211, 153, 0.2) 0%, var(--glass-50) 100%);
  border-color: rgba(52, 211, 153, 0.34);
}
.sidebar-menu li.nav-tile.nav-tile--plane {
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.22) 0%, var(--glass-52) 100%);
  border-color: rgba(56, 189, 248, 0.36);
}
.sidebar-menu li.nav-tile.nav-tile--materials {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, var(--glass-52) 100%);
  border-color: rgba(16, 185, 129, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--materials-all {
  background: linear-gradient(135deg, rgba(52, 211, 153, 0.16) 0%, var(--glass-45) 100%);
  border-color: rgba(52, 211, 153, 0.28);
}
.sidebar-menu li.nav-tile.nav-tile--refine {
  background: linear-gradient(135deg, rgba(45, 212, 191, 0.18) 0%, var(--glass-48) 100%);
  border-color: rgba(45, 212, 191, 0.3);
}
.sidebar-menu li.nav-tile.nav-tile--questions {
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.2) 0%, var(--glass-50) 100%);
  border-color: rgba(20, 184, 166, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--study {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.22) 0%, var(--glass-55) 100%);
  border-color: rgba(139, 92, 246, 0.35);
}
.sidebar-menu li.nav-tile.nav-tile--notes {
  background: linear-gradient(135deg, rgba(250, 204, 21, 0.16) 0%, var(--glass-45) 100%);
  border-color: rgba(250, 204, 21, 0.28);
}
.sidebar-menu li.nav-tile.nav-tile--todos {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.18) 0%, var(--glass-48) 100%);
  border-color: rgba(251, 146, 60, 0.3);
}
.sidebar-menu li.nav-tile.nav-tile--clock {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.18) 0%, var(--glass-48) 100%);
  border-color: rgba(34, 197, 94, 0.3);
}
.sidebar-menu li.nav-tile.nav-tile--practice {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, var(--glass-50) 100%);
  border-color: rgba(59, 130, 246, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--wrong {
  background: linear-gradient(135deg, rgba(248, 113, 113, 0.18) 0%, var(--glass-48) 100%);
  border-color: rgba(248, 113, 113, 0.3);
}
.sidebar-menu li.nav-tile.nav-tile--review {
  background: linear-gradient(135deg, rgba(167, 139, 250, 0.18) 0%, var(--glass-48) 100%);
  border-color: rgba(167, 139, 250, 0.3);
}
.sidebar-menu li.nav-tile.nav-tile--report {
  background: linear-gradient(135deg, rgba(45, 212, 191, 0.16) 0%, var(--glass-45) 100%);
  border-color: rgba(45, 212, 191, 0.28);
}
.sidebar-menu li.nav-tile.nav-tile--ask {
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.2) 0%, var(--glass-52) 100%);
  border-color: rgba(236, 72, 153, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--schedule {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.2) 0%, var(--glass-50) 100%);
  border-color: rgba(249, 115, 22, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--map {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2) 0%, var(--glass-50) 100%);
  border-color: rgba(34, 197, 94, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--notice {
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.18) 0%, var(--glass-48) 100%);
  border-color: rgba(244, 63, 94, 0.3);
}
.sidebar-menu li.nav-tile.nav-tile--plaza-discuss {
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.2) 0%, var(--glass-50) 100%);
  border-color: rgba(56, 189, 248, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--plaza-market {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2) 0%, var(--glass-50) 100%);
  border-color: rgba(245, 158, 11, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--messages {
  background: linear-gradient(135deg, rgba(34, 211, 238, 0.18) 0%, var(--glass-48) 100%);
  border-color: rgba(34, 211, 238, 0.3);
}
.sidebar-menu li.nav-tile.nav-tile--profile {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, var(--glass-50) 100%);
  border-color: rgba(99, 102, 241, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--settings {
  background: linear-gradient(135deg, rgba(148, 163, 184, 0.18) 0%, var(--glass-48) 100%);
  border-color: rgba(148, 163, 184, 0.28);
}
.sidebar-menu li.nav-tile.nav-tile--admin-notice {
  background: linear-gradient(135deg, rgba(251, 113, 133, 0.2) 0%, var(--glass-50) 100%);
  border-color: rgba(251, 113, 133, 0.32);
}
.sidebar-menu li.nav-tile.nav-tile--admin-users {
  background: linear-gradient(135deg, rgba(129, 140, 248, 0.22) 0%, var(--glass-52) 100%);
  border-color: rgba(129, 140, 248, 0.34);
}

/* 滚盘中线边框：须写在各 nav-tile--* 之后以覆盖语义描边 */
.sidebar-menu li.nav-tile:not(.nav-wheel-center) {
  border-width: 1px !important;
  border-style: solid;
  border-color: rgba(0, 0, 0, 0.45) !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.035);
}
.sidebar-menu li.nav-tile.nav-wheel-center {
  z-index: 2;
  border-width: 2px !important;
  border-style: solid;
  border-color: var(--el-color-primary) !important;
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--neon-accent) 48%, transparent),
    0 0 22px rgba(var(--neon-accent-rgb), 0.45),
    inset 0 0 0 1px color-mix(in srgb, var(--neon-accent) 38%, transparent);
}
.sidebar-menu li.nav-tile.el-menu-item.is-active.nav-wheel-center,
.sidebar-menu li.nav-tile.el-sub-menu.is-active.nav-wheel-center {
  border-color: var(--el-color-primary) !important;
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--neon-accent) 58%, transparent),
    0 0 26px rgba(var(--neon-accent-rgb), 0.52),
    inset 0 0 0 1px color-mix(in srgb, var(--neon-accent) 42%, transparent);
}

.sidebar-footer {
  flex-shrink: 0;
  padding: 12px 16px 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
}
.sidebar-logout {
  padding-left: 0 !important;
}
.user-link:hover .nickname {
  color: var(--el-color-primary-light-3);
}
.header-avatar {
  flex-shrink: 0;
}
.auth-link {
  color: var(--el-color-primary-light-3);
  text-decoration: none;
  font-size: 14px;
}
.auth-link:hover {
  color: var(--el-color-primary-light-3);
}

.main {
  flex: 1;
  min-width: 0;
  padding: 0;
  position: relative;
  z-index: 0;
}
.study-main-inner {
  position: relative;
  z-index: 1;
  min-height: 100%;
}

/* 统一表格右侧固定列背景为透明，避免出现白色块 */
.el-table__fixed-right,
.el-table__fixed-right-patch,
.el-table__fixed-right .el-table__fixed-body-wrapper,
.el-table__fixed-right .el-table__fixed-header-wrapper {
  background-color: transparent !important;
}

/* 让资料列表等表格略微半透明，方便看到背后的粒子/气泡 */
.el-table {
  background-color: var(--glass-72) !important;
}
</style>
