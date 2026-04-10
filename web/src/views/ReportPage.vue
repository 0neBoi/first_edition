<template>
  <div class="sh-page" v-loading="loading">
    <div class="sh-page-head study-glass-block head-pad">
      <h2>学习报告</h2>
      <p class="muted">本周学习时长、待办完成、练习正确率与薄弱资料可视化汇总。</p>
    </div>

    <template v-if="data">
      <div class="viz-grid">
        <div class="study-glass-block card-pad ring-card">
          <div class="viz-title">练习正确率</div>
          <div class="ring-wrap">
            <div
              class="ring"
              :style="{
                background: `conic-gradient(#38bdf8 ${accuracyDeg}deg, rgba(255,255,255,0.12) 0)`,
              }"
            >
              <div class="ring-inner">
                <span class="ring-num">{{ data.practiceAccuracy }}%</span>
                <span class="ring-sub">正确率</span>
              </div>
            </div>
          </div>
          <div class="ring-meta">
            答对 <strong>{{ data.practiceCorrectAttempts }}</strong> / 共
            <strong>{{ data.practiceTotalAttempts }}</strong> 次
          </div>
        </div>

        <div class="study-glass-block card-pad">
          <div class="viz-title">本周学习时长</div>
          <div class="bar-viz">
            <div class="bar-track">
              <div
                class="bar-fill bar-time"
                :style="{ width: `${timeBarPct}%` }"
              />
            </div>
            <div class="bar-labels">
              <span>{{ data.weekStudyMinutes }} 分钟</span>
              <span class="hint">相对目标 {{ timeGoal }} 分钟</span>
            </div>
          </div>
          <div class="viz-title spaced">本周完成待办</div>
          <div class="bar-viz">
            <div class="bar-track">
              <div
                class="bar-fill bar-todo"
                :style="{ width: `${todoBarPct}%` }"
              />
            </div>
            <div class="bar-labels">
              <span>{{ data.weekTodosDone }} 项</span>
            </div>
          </div>
        </div>

        <div class="study-glass-block card-pad">
          <div class="viz-title">今日复习队列</div>
          <div class="review-bubble">
            <span class="big">{{ data.reviewQueueSize }}</span>
            <span class="unit">题待复习</span>
          </div>
          <p class="mini-hint">与「练习 → 今日复习」同源推荐</p>
        </div>
      </div>

      <div class="study-glass-block card-pad section-block">
        <h3 class="section-title">薄弱资料 · 错误率条形图</h3>
        <el-empty v-if="!(data.weakMaterials || []).length" description="暂无练习数据，先去练习页做题吧" />
        <div v-else class="weak-bars">
          <div
            v-for="row in data.weakMaterials"
            :key="row.materialId"
            class="weak-row"
          >
            <div class="weak-name" :title="row.title">{{ row.title }}</div>
            <div class="weak-track">
              <div
                class="weak-fill"
                :style="{ width: `${weakWidth(row.wrongRate)}%` }"
              />
            </div>
            <div class="weak-stats">
              错 {{ row.wrongAttempts }} / {{ row.totalAttempts }} · {{ row.wrongRate }}%
            </div>
            <el-button type="primary" size="small" @click="goPractice(row.materialId)">
              练习
            </el-button>
          </div>
        </div>
      </div>

      <el-table
        class="glass-table"
        :data="data.weakMaterials || []"
        stripe
        empty-text="暂无数据"
      >
        <el-table-column prop="title" label="资料" min-width="200" />
        <el-table-column prop="wrongAttempts" label="错误次数" width="100" />
        <el-table-column prop="totalAttempts" label="总作答" width="90" />
        <el-table-column prop="wrongRate" label="错误率 %" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="goPractice(row.materialId)">去练习</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { analyticsApi } from '../api'

const router = useRouter()
const loading = ref(false)
const data = ref(null)

const timeGoal = 300

const accuracyDeg = computed(() => {
  const d = data.value
  if (!d) return 0
  const a = Number(d.practiceAccuracy) || 0
  return Math.min(360, Math.max(0, (a / 100) * 360))
})

const timeBarPct = computed(() => {
  const m = data.value?.weekStudyMinutes ?? 0
  return Math.min(100, Math.round((m / timeGoal) * 100))
})

const todoBarPct = computed(() => {
  const n = data.value?.weekTodosDone ?? 0
  const cap = Math.max(n, 20)
  return Math.min(100, Math.round((n / cap) * 100))
})

function weakWidth(rate) {
  const r = Number(rate) || 0
  return Math.min(100, Math.max(8, r))
}

async function load() {
  loading.value = true
  try {
    data.value = await analyticsApi.dashboard()
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function goPractice(materialId) {
  router.push({ path: '/practice', query: { materialId: String(materialId) } })
}

onMounted(load)
</script>

<style scoped>
.sh-page {
  padding: 24px;
  max-width: 1040px;
  margin: 0 auto;
  color: #e5e7eb;
}
.head-pad {
  padding: 20px 22px;
  margin-bottom: 20px;
}
.sh-page-head h2 {
  margin: 0 0 8px;
  font-size: 22px;
}
.muted {
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.5;
}
.viz-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.card-pad {
  padding: 18px 20px;
}
.viz-title {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 12px;
}
.viz-title.spaced {
  margin-top: 18px;
}
.ring-wrap {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}
.ring {
  width: 132px;
  height: 132px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.ring-inner {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: var(--glass-85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.ring-num {
  font-size: 22px;
  font-weight: 700;
  color: #38bdf8;
}
.ring-sub {
  font-size: 11px;
  color: #94a3b8;
}
.ring-meta {
  text-align: center;
  font-size: 12px;
  color: #cbd5e1;
  margin-top: 8px;
}
.bar-viz {
  margin-top: 4px;
}
.bar-track {
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
}
.bar-time {
  background: linear-gradient(90deg, #38bdf8, #818cf8);
}
.bar-todo {
  background: linear-gradient(90deg, #a855f7, #ec4899);
}
.bar-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 13px;
  color: #e5e7eb;
}
.bar-labels .hint {
  color: #64748b;
  font-size: 12px;
}
.review-bubble {
  text-align: center;
  padding: 16px 0;
}
.review-bubble .big {
  font-size: 42px;
  font-weight: 800;
  background: linear-gradient(120deg, #e0f2fe, #38bdf8);
  -webkit-background-clip: text;
  color: transparent;
  background-clip: text;
}
.review-bubble .unit {
  display: block;
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}
.mini-hint {
  font-size: 12px;
  color: #64748b;
  margin: 0;
  text-align: center;
}
.section-block {
  margin-bottom: 16px;
}
.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #f1f5f9;
}
.weak-bars {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.weak-row {
  display: grid;
  grid-template-columns: minmax(100px, 1.2fr) minmax(120px, 2fr) auto auto;
  gap: 12px;
  align-items: center;
}
@media (max-width: 720px) {
  .weak-row {
    grid-template-columns: 1fr;
  }
}
.weak-name {
  font-size: 13px;
  color: #e5e7eb;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.weak-track {
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
.weak-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #f97316, #ef4444);
  min-width: 4px;
  transition: width 0.35s ease;
}
.weak-stats {
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
}
.glass-table {
  border-radius: 12px;
  overflow: hidden;
}
</style>
