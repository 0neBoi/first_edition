/** 与 Settings 中 sh_app_prefs 的 accentId 同步；设置 Element 主色与霓虹 CSS 变量 */

export const PREFS_STORAGE_KEY = 'sh_app_prefs'

/** 七色主题：主体色（霓虹像素卡通风全局使用） */
export const ACCENT_PRESETS = [
  { id: 'cyan', label: '青', primary: '#22d3ee', rgb: '34, 211, 238', bg1: '#0a1628', bg2: '#0c1220' },
  { id: 'magenta', label: '洋红', primary: '#e879f9', rgb: '232, 121, 249', bg1: '#1a0a24', bg2: '#120818' },
  { id: 'violet', label: '紫', primary: '#a78bfa', rgb: '167, 139, 250', bg1: '#120a22', bg2: '#0c0618' },
  { id: 'lime', label: '柠绿', primary: '#a3e635', rgb: '163, 230, 53', bg1: '#0a1808', bg2: '#08120c' },
  { id: 'amber', label: '琥珀', primary: '#fbbf24', rgb: '251, 191, 36', bg1: '#1a1206', bg2: '#140e04' },
  { id: 'rose', label: '玫红', primary: '#fb7185', rgb: '251, 113, 133', bg1: '#1a0a10', bg2: '#14060c' },
  { id: 'sky', label: '天蓝', primary: '#38bdf8', rgb: '56, 189, 248', bg1: '#0a1422', bg2: '#060e18' },
]

const ACCENT_IDS = new Set(ACCENT_PRESETS.map((a) => a.id))

function presetById(id) {
  return ACCENT_PRESETS.find((a) => a.id === id) || ACCENT_PRESETS[0]
}

/** 从本地偏好读取 accentId，兼容旧版 uiTheme */
export function readAccentId() {
  try {
    const raw = localStorage.getItem(PREFS_STORAGE_KEY)
    if (!raw) return 'cyan'
    const o = JSON.parse(raw)
    if (o.accentId && ACCENT_IDS.has(o.accentId)) return o.accentId
    if (o.uiTheme === 'pixel') return 'lime'
    return 'cyan'
  } catch {
    return 'cyan'
  }
}

export function applyAccent(accentId) {
  const id = ACCENT_IDS.has(accentId) ? accentId : 'cyan'
  const p = presetById(id)
  const r = document.documentElement
  r.dataset.accent = id
  r.style.setProperty('--el-color-primary', p.primary)
  r.style.setProperty('--neon-accent', p.primary)
  r.style.setProperty('--neon-accent-rgb', p.rgb)
  r.style.setProperty('--neon-bg-deep', p.bg1)
  r.style.setProperty('--neon-bg-mid', p.bg2)
  window.dispatchEvent(new CustomEvent('sh-theme-updated', { detail: { accentId: id } }))
}

export function applyAccentFromStorage() {
  applyAccent(readAccentId())
}

/**
 * 将 CSS 变量（如 --glass-90）解析为浏览器计算后的颜色字符串，供 canvas 等无法使用 var() 的场景。
 */
export function resolveCssVarColor(varName) {
  if (typeof document === 'undefined') return 'rgba(0, 0, 0, 0.5)'
  const el = document.createElement('div')
  el.style.cssText = `position:absolute;left:-9999px;top:0;width:4px;height:4px;visibility:hidden;background:var(${varName})`
  document.documentElement.appendChild(el)
  const bg = getComputedStyle(el).backgroundColor
  el.remove()
  return bg && bg !== 'rgba(0, 0, 0, 0)' ? bg : 'rgba(0, 0, 0, 0.5)'
}

/** 当前主题主色 RGB（供 canvas 粒子等使用） */
export function getAccentRgbObject() {
  if (typeof document === 'undefined') return { r: 34, g: 211, b: 238 }
  const raw = getComputedStyle(document.documentElement).getPropertyValue('--neon-accent').trim()
  if (raw.startsWith('#')) {
    const h = raw.slice(1)
    const full = h.length === 3 ? h.split('').map((c) => c + c).join('') : h
    const n = parseInt(full, 16)
    return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
  }
  const m = /^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/i.exec(raw)
  if (m) return { r: +m[1], g: +m[2], b: +m[3] }
  return { r: 34, g: 211, b: 238 }
}

/** @deprecated 仅兼容旧引用 */
export function readUiTheme() {
  return 'default'
}

/** @deprecated */
export function applyUiTheme() {
  applyAccentFromStorage()
}

/** @deprecated */
export function applyUiThemeFromStorage() {
  applyAccentFromStorage()
}
