<template>
  <div class="sh-page">
    <div class="sh-page-head study-glass-block head-pad">
      <h2>复习清单</h2>
      <p class="muted">
        优先「最近一次答错」，并补充低掌握度或未练题目，与「练习 → 今日复习」同一套推荐逻辑。下方可预览今日队列与题型分布。
      </p>
    </div>

    <div class="action-row study-glass-block card-pad">
      <el-button type="primary" size="large" @click="goReviewPractice">进入今日复习练习</el-button>
      <el-button @click="loadAll" :loading="loading">刷新清单</el-button>
    </div>

    <div v-if="stats" class="stats-row">
      <div class="study-glass-block stat-card">
        <div class="stat-label">复习队列</div>
        <div class="stat-val">{{ questions.length }} 题</div>
      </div>
      <div class="study-glass-block stat-card">
        <div class="stat-label">错题本规模</div>
        <div class="stat-val">{{ stats.wrongBookSize ?? 0 }} 题</div>
      </div>
      <div class="study-glass-block stat-card">
        <div class="stat-label">历史正确率</div>
        <div class="stat-val">{{ stats.accuracy ?? 0 }}%</div>
      </div>
    </div>

    <div class="study-glass-block card-pad viz-section" v-if="questions.length">
      <h3 class="sub-title">今日队列 · 题型分布</h3>
      <div class="donut-row">
        <div
          class="donut"
          :style="{ background: donutStyle }"
        >
          <div class="donut-hole">
            <span class="d-n">{{ questions.length }}</span>
            <span class="d-t">题</span>
          </div>
        </div>
        <ul class="legend">
          <li v-for="(c, k) in typeCounts" :key="k">
            <span class="dot" :class="'t-' + k" />
            {{ typeLabel(k) }} {{ c }}
          </li>
        </ul>
      </div>
    </div>

    <div class="study-glass-block card-pad list-section">
      <h3 class="sub-title">题目预览（{{ questions.length }}）</h3>
      <el-empty v-if="!loading && !questions.length" description="暂无推荐，请先在练习中积累作答记录或生成题目" />
      <div v-else class="q-cards" v-loading="loading">
        <div v-for="(q, i) in questions" :key="q.id || i" class="q-card">
          <div class="q-meta">
            <span class="badge">{{ typeLabel(q.type) }}</span>
            <span class="idx">#{{ i + 1 }}</span>
          </div>
          <p class="q-text">{{ clip(q.questionText, 220) }}</p>
          <div v-if="parseOpts(q.optionsJson).length" class="q-opts">
            <span v-for="o in parseOpts(q.optionsJson)" :key="o.key">{{ o.key }}. {{ o.value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { practiceApi } from '../api'

const router = useRouter()
const loading = ref(false)
const questions = ref([])
const stats = ref(null)

const typeCounts = computed(() => {
  const m = { single: 0, multiple: 0, fill: 0, essay: 0 }
  for (const q of questions.value) {
    const t = q.type || 'essay'
    if (m[t] !== undefined) m[t]++
    else m.essay++
  }
  return m
})

const donutStyle = computed(() => {
  const m = typeCounts.value
  const total = questions.value.length || 1
  let start = 0
  const parts = []
  const colors = {
    single: '#38bdf8',
    multiple: '#a855f7',
    fill: '#34d399',
    essay: '#fbbf24',
  }
  for (const k of ['single', 'multiple', 'fill', 'essay']) {
    const n = m[k] || 0
    if (!n) continue
    const deg = (n / total) * 360
    const end = start + deg
    parts.push(`${colors[k]} ${start}deg ${end}deg`)
    start = end
  }
  if (!parts.length) return 'conic-gradient(#334155 0deg 360deg)'
  if (start < 360) parts.push(`rgba(255,255,255,0.1) ${start}deg 360deg`)
  return `conic-gradient(${parts.join(', ')})`
})

function typeLabel(type) {
  const map = { single: '单选', multiple: '多选', fill: '填空', essay: '简答' }
  return map[type] || type
}

function clip(t, n) {
  if (!t) return ''
  const s = String(t).replace(/\s+/g, ' ').trim()
  return s.length > n ? `${s.slice(0, n)}…` : s
}

function parseOpts(json) {
  if (!json) return []
  try {
    return JSON.parse(json)
  } catch {
    return []
  }
}

async function loadAll() {
  loading.value = true
  try {
    const [qs, st] = await Promise.all([
      practiceApi.reviewToday(40).catch(() => []),
      practiceApi.stats().catch(() => null),
    ])
    questions.value = Array.isArray(qs) ? qs : []
    stats.value = st
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function goReviewPractice() {
  router.push({ path: '/practice', query: { tab: 'review' } })
}

onMounted(loadAll)
</script>

<style scoped>
.sh-page {
  padding: 24px;
  max-width: 920px;
  margin: 0 auto;
  color: #e5e7eb;
}
.head-pad {
  padding: 20px 22px;
  margin-bottom: 16px;
}
.sh-page-head h2 {
  margin: 0 0 8px;
  font-size: 22px;
}
.muted {
  margin: 0;
  color: #94a3b8;
  font-size: 14px;
  line-height: 1.6;
}
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}
.card-pad {
  padding: 18px 20px;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  padding: 16px 18px;
  text-align: center;
}
.stat-label {
  font-size: 12px;
  color: #94a3b8;
}
.stat-val {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: #38bdf8;
}
.viz-section {
  margin-bottom: 16px;
}
.sub-title {
  margin: 0 0 14px;
  font-size: 15px;
  color: #f1f5f9;
}
.donut-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 28px;
}
.donut {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  position: relative;
  flex-shrink: 0;
}
.donut-hole {
  position: absolute;
  inset: 22px;
  border-radius: 50%;
  background: var(--glass-92);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.d-n {
  font-size: 26px;
  font-weight: 800;
  color: #f1f5f9;
}
.d-t {
  font-size: 11px;
  color: #94a3b8;
}
.legend {
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: 13px;
  color: #cbd5e1;
}
.legend li {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}
.dot.t-single {
  background: #38bdf8;
}
.dot.t-multiple {
  background: #a855f7;
}
.dot.t-fill {
  background: #34d399;
}
.dot.t-essay {
  background: #fbbf24;
}
.list-section {
  margin-bottom: 24px;
}
.q-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 520px;
  overflow-y: auto;
}
.q-card {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.q-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.2);
  color: #7dd3fc;
}
.idx {
  font-size: 12px;
  color: #64748b;
}
.q-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  color: #e5e7eb;
}
.q-opts {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
