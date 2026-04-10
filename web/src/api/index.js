import axios from 'axios'

/**
 * 后端 `server.servlet.context-path: /api`，所有接口必须以 `/api` 为前缀。
 * - 未设置 env：用相对路径 `/api`，由 Vite 代理到 8080。
 * - 绝对地址（含局域网 IP）：若未以 `/api` 结尾则自动补全。
 * - 相对路径：仅允许 `/api`；误写成其它路径（如 `/`）时一律回退为 `/api`，
 *   否则会请求到 `/plaza/...` 而触发 “No static resource plaza/post”。
 */
function resolveApiBase() {
  const raw = import.meta.env.VITE_API_BASE?.trim()
  if (!raw) return '/api'
  const noTrail = raw.replace(/\/+$/, '')
  if (!noTrail || noTrail === '/') return '/api'
  if (/^https?:\/\//i.test(noTrail)) {
    return noTrail.endsWith('/api') ? noTrail : `${noTrail}/api`
  }
  if (noTrail === '/api' || noTrail.startsWith('/api/')) {
    return noTrail
  }
  return '/api'
}

const baseURL = resolveApiBase()

/** 供头像、广场图片等拼接静态文件 URL，与 axios baseURL 规则一致 */
export function getApiBase() {
  return resolveApiBase()
}
const request = axios.create({
  baseURL,
  timeout: 120000,
})

// 请求时带上 token；每次请求重算 baseURL，避免实例被改写后丢失 /api
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const b = resolveApiBase()
  config.baseURL = b
  // 若 baseURL 仍异常，将路径改为以 /api 开头（避免落到 Vite 静态资源）
  const u = config.url
  if (typeof u === 'string' && u.startsWith('/') && !u.startsWith('/api') && (!b || b === '')) {
    config.url = `/api${u}`
    config.baseURL = ''
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const { code, data, msg } = res.data
    if (code !== 200 && code !== 201) {
      return Promise.reject(new Error(msg || '请求失败'))
    }
    return data
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login'
      }
    }
    const msg = err.response?.data?.msg || err.response?.data?.message || err.message || ''
    const isStaticResource = /static resource|404|Not Found/i.test(msg) || err.code === 'ERR_NETWORK'
    const friendly = isStaticResource
      ? '无法连接后端接口，请确认后端服务已启动（开发时需同时运行后端与 npm run dev）'
      : (msg || '网络错误')
    return Promise.reject(new Error(friendly))
  }
)

export const materialApi = {
  list: () => request.get('/material/list'),
  get: (id) => request.get(`/material/${id}`),
  upload: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/material/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  delete: (id) => request.delete(`/material/${id}`),
}

export const knowledgeApi = {
  list: (materialId) => request.get('/knowledge/list', { params: { materialId } }),
  extract: (materialId) =>
    request.post('/knowledge/extract', null, {
      params: { materialId },
      timeout: 300000,
    }),
  delete: (id) => request.delete(`/knowledge/${id}`),
}

export const questionApi = {
  list: (materialId) => request.get('/question/list', { params: { materialId } }),
  /** replace: 首轮 true 会清空本资料旧题；后续题型批次须传 false 以追加，避免并行覆盖 */
  generate: (materialId, count, types, replace = true) =>
    request.post('/question/generate', null, {
      params: {
        materialId,
        count: count || 5,
        types: Array.isArray(types) && types.length ? types.join(',') : undefined,
        replace,
      },
      timeout: 300000,
    }),
  delete: (id) => request.delete(`/question/${id}`),
}

export const qwenApi = {
  ask: (question) =>
    request.post('/qwen/ask', null, {
      params: { question },
      timeout: 300000,
    }),
}

export const campusAnnouncementApi = {
  publicList: () => request.get('/campus-announcement/public/list'),
  create: (data) => request.post('/campus-announcement', data),
  update: (id, data) => request.put(`/campus-announcement/${id}`, data),
  delete: (id) => request.delete(`/campus-announcement/${id}`),
}

/** 小恐龙跑酷：排行榜与提交最佳分 */
export const dinoApi = {
  leaderboard: (size = 20) => request.get('/game/dino/leaderboard', { params: { size } }),
  myBest: () => request.get('/game/dino/my-best'),
  /** 当前用户全局名次与分数（未上传过则为 null） */
  myEntry: () => request.get('/game/dino/my-entry'),
  submitBest: (score) => request.post('/game/dino/best', { score }),
}

/** 飞机大战：排行榜与提交最佳分 */
export const planeApi = {
  leaderboard: (size = 20) => request.get('/game/plane/leaderboard', { params: { size } }),
  myBest: () => request.get('/game/plane/my-best'),
  myEntry: () => request.get('/game/plane/my-entry'),
  submitBest: (score) => request.post('/game/plane/best', { score }),
}

/** 管理员：注册用户列表、重置密码、禁用账号 */
export const adminUserApi = {
  page: (params) => request.get('/admin/users', { params }),
  resetPassword: (id, newPassword) =>
    request.put(`/admin/users/${id}/password`, { newPassword }),
  delete: (id) => request.delete(`/admin/users/${id}`),
}

export const authApi = {
  login: (username, password) =>
    request.post('/auth/login', null, { params: { username, password } }),
  register: (username, password, nickname) =>
    request.post('/auth/register', null, { params: { username, password, nickname } }),
  me: () => request.get('/auth/me'),
  updateProfile: (data) => request.put('/auth/profile', data),
  updatePassword: (oldPassword, newPassword) =>
    request.put('/auth/password', { oldPassword, newPassword }),
  uploadAvatar: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/auth/avatar', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

export const noteApi = {
  list: (params) => request.get('/note/list', { params }),
  get: (id) => request.get(`/note/${id}`),
  create: (data) => request.post('/note/create', data),
  update: (id, data) => request.put(`/note/${id}`, data),
  delete: (id) => request.delete(`/note/${id}`),
}

export const todoApi = {
  list: (params) => request.get('/todo/list', { params }),
  today: () => request.get('/todo/today'),
  get: (id) => request.get(`/todo/${id}`),
  create: (data) => request.post('/todo/create', data),
  update: (id, data) => request.put(`/todo/${id}`, data),
  delete: (id) => request.delete(`/todo/${id}`),
}

export const clockApi = {
  upsertDay: (data) => request.post('/clock/day', data),
  range: (from, to) => request.get('/clock/range', { params: { from, to } }),
  month: (yearMonth) => request.get('/clock/month', { params: { yearMonth } }),
  streak: () => request.get('/clock/streak'),
}

export const practiceApi = {
  submit: (data) => request.post('/practice/submit', data),
  wrongBook: (materialId) =>
    request.get('/practice/wrong-book', { params: materialId ? { materialId } : {} }),
  reviewToday: (limit) => request.get('/practice/review-today', { params: { limit } }),
  stats: () => request.get('/practice/stats'),
  mastery: (questionIds) =>
    request.get('/practice/mastery', { params: { questionIds: questionIds.join(',') } }),
}

export const analyticsApi = {
  dashboard: () => request.get('/analytics/dashboard'),
}

export function getAvatarUrl(avatarPath) {
  if (!avatarPath) return ''
  const base = resolveApiBase()
  return avatarPath.startsWith('http') ? avatarPath : `${base}/files/${avatarPath}`
}

/** 广场帖子图片路径 plaza/xxx.jpg → /api/files/plaza/xxx.jpg */
export function getPlazaImageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  const base = resolveApiBase()
  const name = String(path).replace(/^plaza\//, '')
  return `${base}/files/plaza/${name}`
}

export const plazaApi = {
  postPage: (params) => request.get('/plaza/post/page', { params }),
  postGet: (id) => request.get(`/plaza/post/${id}`),
  postCreate: (data) => request.post('/plaza/post', data),
  postDelete: (id) => request.delete(`/plaza/post/${id}`),
  toggleLike: (id) => request.post(`/plaza/post/${id}/like`),
  toggleFavorite: (id) => request.post(`/plaza/post/${id}/favorite`),
  updateTrade: (id, tradeStatus) =>
    request.put(`/plaza/post/${id}/trade`, { tradeStatus }),
  comments: (id) => request.get(`/plaza/post/${id}/comments`),
  addComment: (id, data) => request.post(`/plaza/post/${id}/comment`, data),
  deleteComment: (id) => request.delete(`/plaza/comment/${id}`),
  upload: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/plaza/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  messages: (params) => request.get('/plaza/message/page', { params }),
  sendMessage: (data) => request.post('/plaza/message', data),
  chatPartners: () => request.get('/plaza/chat/partners'),
}
