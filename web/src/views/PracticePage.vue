<template>
  <div class="sh-page">
    <div class="sh-page-head study-glass-block head-pad">
      <h2>练习</h2>
      <p class="muted">提交答案后立即判分并写入练习记录，用于错题本与掌握度统计。</p>
    </div>
    <el-tabs v-model="tab" class="tabs-block">
      <el-tab-pane label="按资料练习" name="material">
        <div class="row">
          <span class="lbl">选择资料</span>
          <el-select
            v-model="materialId"
            placeholder="请选择"
            filterable
            style="width: 320px"
            @change="loadQuestions"
          >
            <el-option v-for="m in materials" :key="m.id" :label="m.title || m.fileName" :value="m.id" />
          </el-select>
          <el-button :disabled="!materialId" @click="loadQuestions">刷新题目</el-button>
        </div>
        <practice-session
          v-if="materialId && questions.length"
          :questions="questions"
          @submitted="onSubmitted"
        />
        <el-empty v-else-if="materialId" description="该资料暂无题目，请先在资料详情中生成" />
      </el-tab-pane>
      <el-tab-pane label="今日复习" name="review">
        <el-button type="primary" :loading="reviewLoading" @click="loadReview">加载复习清单</el-button>
        <practice-session v-if="reviewQs.length" :questions="reviewQs" @submitted="onSubmitted" />
        <el-empty v-else description="先点击加载复习清单（优先错题与低掌握度题目）" />
      </el-tab-pane>
      <el-tab-pane label="练习统计" name="stats">
        <el-descriptions v-if="stats" :column="1" border>
          <el-descriptions-item label="总作答次数">{{ stats.totalAttempts }}</el-descriptions-item>
          <el-descriptions-item label="答对次数">{{ stats.correctAttempts }}</el-descriptions-item>
          <el-descriptions-item label="正确率">{{ stats.accuracy }}%</el-descriptions-item>
          <el-descriptions-item label="错题本题目数">{{ stats.wrongBookSize }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { materialApi, questionApi, practiceApi } from '../api'
import PracticeSession from './PracticeSession.vue'

const route = useRoute()
const tab = ref('material')
const materials = ref([])
const materialId = ref(null)
const questions = ref([])
const reviewQs = ref([])
const reviewLoading = ref(false)
const stats = ref(null)

function onSubmitted() {
  loadStatsBrief()
}

async function loadStatsBrief() {
  try {
    stats.value = await practiceApi.stats()
  } catch (_) {
    /* ignore */
  }
}

async function loadMaterials() {
  try {
    materials.value = (await materialApi.list()) || []
    const qid = route.query.materialId
    if (qid) {
      materialId.value = Number(qid)
      await loadQuestions()
    }
  } catch (e) {
    ElMessage.error(e.message || '加载资料失败')
  }
}

async function loadQuestions() {
  if (!materialId.value) return
  try {
    questions.value = (await questionApi.list(materialId.value)) || []
  } catch (e) {
    ElMessage.error(e.message || '加载题目失败')
  }
}

async function loadReview() {
  reviewLoading.value = true
  try {
    reviewQs.value = (await practiceApi.reviewToday(25)) || []
    if (!reviewQs.value.length) {
      ElMessage.info('当前没有推荐的复习题，先去练习或生成题目吧')
    }
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    reviewLoading.value = false
  }
}

watch(
  () => route.query.materialId,
  (v) => {
    if (v) {
      materialId.value = Number(v)
      tab.value = 'material'
      loadQuestions()
    }
  },
)

onMounted(() => {
  loadMaterials()
  loadStatsBrief()
  if (route.query.tab === 'review') {
    tab.value = 'review'
    loadReview()
  }
})
</script>

<style scoped>
.sh-page {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
  color: #e5e7eb;
}
.sh-page-head h2 {
  margin: 0 0 8px;
}
.muted {
  margin: 0 0 16px;
  color: #94a3b8;
  font-size: 13px;
}
.row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.lbl {
  font-size: 13px;
  color: #94a3b8;
}
.head-pad {
  padding: 18px 20px;
  margin-bottom: 16px;
}
.tabs-block {
  padding: 12px 16px 20px;
  border-radius: 14px;
  background: var(--glass-38);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
</style>
