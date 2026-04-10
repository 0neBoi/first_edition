import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/** 与后端 `server.servlet.context-path: /api` 一致；开发/预览时把前端的 /api 转到 8080 */
const apiProxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    secure: false,
  },
}

/**
 * 默认 mode=development / production：仅本机可访问（localhost）。
 * `vite --mode lan` / `vite preview --mode lan`：监听 0.0.0.0，便于手机与同网设备访问。
 */
export default defineConfig(({ mode }) => {
  const listenLan = mode === 'lan'
  return {
    plugins: [vue()],
    server: {
      port: 5173,
      host: listenLan ? true : 'localhost',
      strictPort: false,
      proxy: apiProxy,
    },
    // `npm run build` 后执行 `vite preview` 时不会走 devServer，必须单独配置 preview 代理，
    // 否则请求 /api 会当作静态资源查找并出现 “No static resource”
    preview: {
      port: 4173,
      host: listenLan ? true : 'localhost',
      strictPort: false,
      proxy: apiProxy,
    },
  }
})
