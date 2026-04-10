<template>
  <div class="guide-page">
    <header class="guide-hero study-glass-block">
      <PixelIcon name="school" size="lg" class="guide-hero-ico" />
      <h1>校园工具 · 功能一览</h1>
      <p class="guide-lead">
        这里汇总了本应用的主要能力。按模块浏览，点击卡片即可进入对应页面；首次使用建议从上到下扫一遍，再进入「首页」查看你的学习实况。
      </p>
      <div class="guide-hero-actions">
        <el-button type="primary" @click="router.push('/home')">进入学习实况首页</el-button>
      </div>
    </header>

    <section v-for="sec in sections" :key="sec.id" class="guide-section study-glass-block">
      <h2 class="guide-sec-title">{{ sec.title }}</h2>
      <p v-if="sec.desc" class="guide-sec-desc">{{ sec.desc }}</p>
      <div class="guide-cards">
        <button
          v-for="item in sec.items"
          :key="item.path + item.name"
          type="button"
          class="guide-card"
          @click="go(item.path)"
        >
          <PixelIcon :name="item.icon" size="sm" class="guide-card-pix" />
          <span class="guide-card-name">{{ item.name }}</span>
          <span class="guide-card-hint">{{ item.hint }}</span>
        </button>
      </div>
    </section>

    <footer class="guide-footer study-glass-block">
      <p class="guide-footer-t">准备好开始了吗？</p>
      <el-button type="primary" size="large" @click="router.push('/home')">返回学习实况首页</el-button>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function go(path) {
  router.push(path)
}

const sections = [
  {
    id: 'materials',
    title: '资料与 AI',
    desc: '上传课件与文档，提炼知识点、自动生成练习题，并用大模型辅助答疑。',
    items: [
      { path: '/materials', name: '全部资料', hint: '上传与管理文件', icon: 'materials' },
      { path: '/materials/refine', name: '提炼', hint: '从资料提取知识要点', icon: 'refine' },
      { path: '/materials/questions', name: '出题', hint: '按资料生成模拟题', icon: 'questions' },
      { path: '/ask', name: 'AI 问答', hint: '学习问题智能解答', icon: 'spark' },
    ],
  },
  {
    id: 'study',
    title: '学习与打卡',
    desc: '笔记、待办、打卡与练习闭环，配合错题本与复习清单巩固记忆。',
    items: [
      { path: '/notes', name: '笔记', hint: '记录与整理', icon: 'notes' },
      { path: '/todos', name: '待办', hint: '任务清单', icon: 'todos' },
      { path: '/clock', name: '打卡', hint: '学习时长统计', icon: 'clock' },
      { path: '/practice', name: '练习', hint: '答题与巩固', icon: 'practice' },
      { path: '/wrong-book', name: '错题本', hint: '错题回顾', icon: 'wrong' },
      { path: '/review', name: '复习清单', hint: '按节奏复习', icon: 'review' },
      { path: '/report', name: '学习报告', hint: '数据汇总', icon: 'report' },
    ],
  },
  {
    id: 'campus',
    title: '校园与广场',
    desc: '课表、地图与公告；广场支持帖子交流与闲置交易，可与同学私信沟通。',
    items: [
      { path: '/schedule', name: '课程表', hint: '周课表与节次', icon: 'schedule' },
      { path: '/map', name: '校园地图', hint: '校内地点', icon: 'map' },
      { path: '/notice', name: '校园公告', hint: '教务与通知', icon: 'notice' },
      { path: '/plaza/discussion', name: '帖子交流', hint: '讨论区', icon: 'plaza' },
      { path: '/plaza/market', name: '闲置购物', hint: '二手信息', icon: 'market' },
      { path: '/messages', name: '私信', hint: '与同学聊天', icon: 'messages' },
    ],
  },
  {
    id: 'games',
    title: '休闲小游戏',
    desc: '学习与放松兼顾，排行榜需登录后参与（管理员账号不参与排行）。',
    items: [
      { path: '/game/dino', name: '小恐龙跑酷', hint: '跳跃闯关', icon: 'dino' },
      { path: '/game/plane', name: '飞机大战', hint: '射击得分', icon: 'plane' },
    ],
  },
  {
    id: 'account',
    title: '账户与设置',
    desc: '个人信息、头像与主题等偏好设置。',
    items: [
      { path: '/profile', name: '个人中心', hint: '资料与头像', icon: 'profile' },
      { path: '/settings', name: '设置', hint: '主题与账号', icon: 'settings' },
    ],
  },
]
</script>

<style scoped>
.guide-page {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 18px 48px;
  color: #e2e8f0;
}

.guide-hero {
  padding: 22px 20px 24px;
  border-radius: 16px;
  margin-bottom: 18px;
  text-align: center;
}

.guide-hero-ico {
  display: block;
  margin: 0 auto 12px;
  opacity: 0.95;
}

.guide-hero h1 {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 700;
  color: #f8fafc;
}

.guide-lead {
  margin: 0 auto 18px;
  max-width: 40em;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(203, 213, 225, 0.95);
  text-align: left;
}

.guide-hero-actions {
  display: flex;
  justify-content: center;
}

.guide-section {
  padding: 18px 18px 20px;
  border-radius: 14px;
  margin-bottom: 14px;
}

.guide-sec-title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  color: #f1f5f9;
}

.guide-sec-desc {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.55;
  color: #94a3b8;
}

.guide-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}

.guide-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 12px 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: color-mix(in srgb, var(--neon-accent) 6%, rgba(15, 23, 42, 0.5));
  color: inherit;
  font: inherit;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.15s ease,
    background 0.15s ease,
    transform 0.12s ease;
}

.guide-card:hover {
  border-color: color-mix(in srgb, var(--el-color-primary) 45%, transparent);
  background: color-mix(in srgb, var(--neon-accent) 12%, rgba(15, 23, 42, 0.55));
  transform: translateY(-1px);
}

.guide-card-pix {
  flex-shrink: 0;
  opacity: 0.95;
}

.guide-card-name {
  font-size: 14px;
  font-weight: 600;
  color: #f1f5f9;
}

.guide-card-hint {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.35;
}

.guide-footer {
  padding: 20px;
  border-radius: 14px;
  text-align: center;
}

.guide-footer-t {
  margin: 0 0 14px;
  font-size: 14px;
  color: #94a3b8;
}
</style>
