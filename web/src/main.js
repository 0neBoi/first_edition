import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/study-theme.css'
import './styles/pixel-theme.css'
import './styles/pixel-icons.css'
import { applyAccentFromStorage } from './theme'
import App from './App.vue'
import router from './router'
import PixelIcon from './components/PixelIcon.vue'

applyAccentFromStorage()

const app = createApp(App)
app.component('PixelIcon', PixelIcon)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
