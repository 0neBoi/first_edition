<template>
  <div class="sh-page">
    <div class="sh-page-head study-glass-block head-pad">
      <h2>错题本</h2>
      <p class="muted">
        展示「最近一次作答为错误」的题目。答对后将从本列表消失。下方含题型分布与资料维度可视化。
      </p>
    </div>

    <div class="row study-glass-block card-pad">
      <span class="lbl">按资料筛选</span>
      <el-select v-model="materialId" clearable placeholder="全部资料" style="width: 280px" @change="load">
        <el-option v-for="m in materials" :key="m.id" :label="m.title || m.fileName" :value="m.id" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <el-button type="primary" @click="goPractice">去练习</el-button>
    </div>

    <div v-if="stats" class="viz-stats">
      <div class="study-glass-block mini-stat">
        <div class="ms-label">当前列表</div>
        <div class="ms-val">{{ list.length }} 题</div>
      </div>
      <div class="study-glass-block mini-stat">
        <div class="ms-label">错题本总题数</div>
        <div class="ms-val">{{ stats.wrongBookSize ?? 0 }}</div>
      </div>
      <div class="study-glass-block mini-stat">
        <div class="ms-label">累计作答</div>
        <div class="ms-val">{{ stats.totalAttempts ?? 0 }}</div>
      </div>
      <div class="study-glass-block mini-stat">
        <div class="ms-label">历史正确率</div>
        <div class="ms-val">{{ stats.accuracy ?? 0 }}%</div>
      </div>
    </div>

    <div class="study-glass-block card-pad chart-section" v-if="list.length">
      <h3 class="sub-title">题型分布</h3>
      <div class="h-bars">
        <div v-for="row in typeBarRows" :key="row.key" class="h-row">
          <span class="h-name">{{ row.label }}</span>
          <div class="h-track">
            <div class="h-fill" :class="'c-' + row.key" :style="{ width: row.pct + '%' }" />
          </div>
          <span class="h-num">{{ row.count }}</span>
        </div>
      </div>
    </div>

    <div class="study-glass-block card-pad chart-section" v-if="materialBarRows.length">
      <h3 class="sub-title">按资料 · Top 错题数</h3>
      <div class="mat-bars">
        <div v-for="r in materialBarRows" :key="r.id" class="mat-row">
          <span class="mat-title" :title="r.title">{{ r.title }}</span>
          <div class="mat-track">
            <div class="mat-fill" :style="{ width: r.pct + '%' }" />
          </div>
          <span class="mat-n">{{ r.count }}</span>
        </div>
      </div>
    </div>

    <div class="study-glass-block card-pad">
      <h3 class="sub-title">题目明细</h3>
      <el-table
        v-if="list.length || loading"
        :data="list"
        stripe
        v-loading="loading"
        class="detail-table"
        empty-text="暂无错题"
      >
        <el-table-column label="题目摘要" min-width="220">
          <template #default="{ row }">
            <span class="clip">{{ clip(row.questionText, 100) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="题型" width="88">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="tagType(row.type)">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="openMaterial(row.materialId)">资料详情</el-button>
            <el-button link @click="practiceMaterial(row.materialId)">练习该卷</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无错题，继续保持或先去练习几套题" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { materialApi, practiceApi } from '../api'

const router = useRouter()
const materials = ref([])
const materialId = ref(null)
const list = ref([])
const loading = ref(false)
const stats = ref(null)

const typeCounts = computed(() => {
  const m = { single: 0, multiple: 0, fill: 0, essay: 0 }
  for (const q of list.value) {
    const t = q.type || 'essay'
    if (m[t] !== undefined) m[t]++
    else m.essay++
  }
  return m
})

const typeBarRows = computed(() => {
  const m = typeCounts.value
  const max = Math.max(1, ...Object.values(m))
  const labels = { single: '单选', multiple: '多选', fill: '填空', essay: '简答' }
  return ['single', 'multiple', 'fill', 'essay'].map((key) => ({
    key,
    label: labels[key],
    count: m[key],
    pct: Math.round((m[key] / max) * 100),
  }))
})

const materialBarRows = computed(() => {
  const map = new Map()
  for (const q of list.value) {
    const id = q.materialId
    if (id == null) continue
    const title =
      materials.value.find((x) => x.id === id)?.title ||
      materials.value.find((x) => x.id === id)?.fileName ||
      `资料 #${id}`
    map.set(id, { id, title, count: (map.get(id)?.count || 0) + 1 })
  }
  const rows = Array.from(map.values()).sort((a, b) => b.count - a.count).slice(0, 8)
  const max = Math.max(1, ...rows.map((r) => r.count))
  return rows.map((r) => ({ ...r, pct: Math.round((r.count / max) * 100) }))
})

function typeLabel(type) {
  const m = { single: '单选', multiple: '多选', fill: '填空', essay: '简答' }
  return m[type] || type
}

function tagType(type) {
  const m = { single: 'primary', multiple: 'warning', fill: 'success', essay: 'info' }
  return m[type] || 'info'
}

function clip(t, n) {
  if (!t) return ''
  const s = String(t).replace(/\s+/g, ' ').trim()
  return s.length > n ? `${s.slice(0, n)}…` : s
}

async function load() {
  loading.value = true
  try {
    const [book, st] = await Promise.all([
      practiceApi.wrongBook(materialId.value || undefined),
      practiceApi.stats().catch(() => null),
    ])
    list.value = Array.isArray(book) ? book : []
    stats.value = st
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openMaterial(mid) {
  router.push(`/material/${mid}`)
}

function practiceMaterial(mid) {
  router.push({ path: '/practice', query: { materialId: String(mid) } })
}

function goPractice() {
  router.push('/practice')
}

onMounted(async () => {
  try {
    materials.value = (await materialApi.list()) || []
  } catch (e) {
    ElMessage.error(e.message || '加载资料失败')
  }
  load()
})
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
  margin-bottom: 16px;
}
.sh-page-head h2 {
  margin: 0 0 8px;
  font-size: 22px;
}
.muted {
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.55;
}
.card-pad {
  padding: 16px 18px;
}
.row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}
.lbl {
  font-size: 13px;
  color: #94a3b8;
}
.viz-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}
.mini-stat {
  padding: 14px 16px;
  text-align: center;
}
.ms-label {
  font-size: 11px;
  color: #94a3b8;
}
.ms-val {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 700;
  color: #f97316;
}
.chart-section {
  margin-bottom: 14px;
}
.sub-title {
  margin: 0 0 14px;
  font-size: 15px;
  color: #f1f5f9;
}
.h-row {
  display: grid;
  grid-template-columns: 52px 1fr 36px;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}
.h-name {
  font-size: 12px;
  color: #94a3b8;
}
.h-track {
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
.h-fill {
  height: 100%;
  border-radius: 999px;
  min-width: 4px;
  transition: width 0.35s ease;
}
.h-fill.c-single {
  background: linear-gradient(90deg, #0ea5e9, #38bdf8);
}
.h-fill.c-multiple {
  background: linear-gradient(90deg, #9333ea, #c084fc);
}
.h-fill.c-fill {
  background: linear-gradient(90deg, #059669, #34d399);
}
.h-fill.c-essay {
  background: linear-gradient(90deg, #d97706, #fbbf24);
}
.h-num {
  font-size: 12px;
  color: #e5e7eb;
  text-align: right;
}
.mat-row {
  display: grid;
  grid-template-columns: minmax(80px, 1.4fr) 1fr 32px;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}
.mat-title {
  font-size: 12px;
  color: #cbd5e1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mat-track {
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
.mat-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #f97316, #fb923c);
  min-width: 4px;
}
.mat-n {
  font-size: 12px;
  color: #94a3b8;
  text-align: right;
}
.clip {
  font-size: 13px;
  line-height: 1.45;
}
</style>
