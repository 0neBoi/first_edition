import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Landing', component: () => import('../views/Landing.vue'), meta: { title: '校园工具', public: true } },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', public: true, studyGlass: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: '注册', public: true, studyGlass: true },
  },
  { path: '/home', name: 'Home', component: () => import('../views/HomeLanding.vue'), meta: { title: '首页', studyGlass: true } },
  {
    path: '/guide',
    name: 'Guide',
    component: () => import('../views/GuidePage.vue'),
    meta: { title: '功能介绍', studyGlass: true },
  },
  {
    path: '/game/dino',
    name: 'DinoRunner',
    component: () => import('../views/DinoRunner.vue'),
    meta: { title: '小恐龙跑酷', studyGlass: true },
  },
  {
    path: '/game/plane',
    name: 'PlaneShooter',
    component: () => import('../views/PlaneShooter.vue'),
    meta: { title: '飞机大战', studyGlass: true },
  },
  { path: '/materials', name: 'Materials', component: () => import('../views/MaterialList.vue'), meta: { title: '资料列表' } },
  {
    path: '/materials/refine',
    name: 'MaterialsRefine',
    component: () => import('../views/MaterialList.vue'),
    meta: { title: '资料 · 提炼', materialPanel: 'refine' },
  },
  {
    path: '/materials/questions',
    name: 'MaterialsQuestions',
    component: () => import('../views/MaterialList.vue'),
    meta: { title: '资料 · 出题', materialPanel: 'questions' },
  },
  { path: '/material/:id', name: 'MaterialDetail', component: () => import('../views/MaterialDetail.vue'), meta: { title: '资料详情' } },
  { path: '/ask', name: 'Ask', component: () => import('../views/Ask.vue'), meta: { title: 'AI 问答' } },
  { path: '/schedule', name: 'Schedule', component: () => import('../views/campus/Schedule.vue'), meta: { title: '课程表' } },
  { path: '/map', name: 'CampusMap', component: () => import('../views/campus/Map.vue'), meta: { title: '校园地图' } },
  { path: '/notice', name: 'Notice', component: () => import('../views/campus/Notice.vue'), meta: { title: '校园公告' } },
  { path: '/plaza', redirect: '/plaza/discussion' },
  {
    path: '/plaza/discussion',
    name: 'PlazaDiscussion',
    component: () => import('../views/PlazaPage.vue'),
    meta: { title: '帖子交流', studyGlass: true, plazaMode: 'discussion' },
  },
  {
    path: '/plaza/market',
    name: 'PlazaMarket',
    component: () => import('../views/PlazaPage.vue'),
    meta: { title: '闲置购物', studyGlass: true, plazaMode: 'market' },
  },
  {
    path: '/messages',
    redirect: { path: '/plaza/discussion', query: { openChat: '1' } },
  },
  {
    path: '/admin/notice',
    name: 'AdminNotice',
    component: () => import('../views/admin/AdminNoticePage.vue'),
    meta: { title: '公告管理', requiresAdmin: true },
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('../views/admin/AdminUsersPage.vue'),
    meta: { title: '用户管理', requiresAdmin: true, studyGlass: true },
  },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue'), meta: { title: '个人信息' } },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/SettingsPage.vue'),
    meta: { title: '设置', studyGlass: true },
  },
  { path: '/notes', name: 'Notes', component: () => import('../views/NoteList.vue'), meta: { title: '笔记', studyGlass: true } },
  { path: '/notes/:id', name: 'NoteEditor', component: () => import('../views/NoteEditor.vue'), meta: { title: '编辑笔记', studyGlass: true } },
  { path: '/todos', name: 'Todos', component: () => import('../views/TodoPage.vue'), meta: { title: '待办', studyGlass: true } },
  { path: '/clock', name: 'Clock', component: () => import('../views/ClockPage.vue'), meta: { title: '学习打卡', studyGlass: true } },
  { path: '/practice', name: 'Practice', component: () => import('../views/PracticePage.vue'), meta: { title: '练习', studyGlass: true } },
  { path: '/wrong-book', name: 'WrongBook', component: () => import('../views/WrongBookPage.vue'), meta: { title: '错题本', studyGlass: true } },
  { path: '/review', name: 'Review', component: () => import('../views/ReviewPage.vue'), meta: { title: '复习清单', studyGlass: true } },
  { path: '/report', name: 'Report', component: () => import('../views/ReportPage.vue'), meta: { title: '学习报告', studyGlass: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function parseUserRole() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) return null
    const u = JSON.parse(raw)
    return u.role || 'STUDENT'
  } catch {
    return null
  }
}

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const isPublic = to.matched.some((r) => r.meta.public)
  if (isPublic) {
    if (token && (to.path === '/login' || to.path === '/register')) {
      next(parseUserRole() === 'ADMIN' ? '/admin/notice' : '/home')
    } else if (to.path === '/' && token) {
      next(parseUserRole() === 'ADMIN' ? '/admin/notice' : '/home')
    } else if (to.path === '/' && !token) {
      next('/login')
    } else {
      next()
    }
    return
  }
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.matched.some((r) => r.meta.requiresAdmin) && parseUserRole() !== 'ADMIN') {
    next('/home')
    return
  }
  next()
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 校园工具` : '校园工具'
})

export default router
