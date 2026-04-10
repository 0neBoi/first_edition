<template>
  <div class="material-detail-page" v-loading="loading">
    <el-page-header @back="goBack" :title="material?.title || '资料详情'" />
    <div class="material-top-actions">
      <el-button type="primary" @click="goPractice">练习本卷题目</el-button>
    </div>

    <el-tabs v-model="activeTab" style="margin-top: 16px;">
      <el-tab-pane label="原文预览" name="preview">
        <el-empty v-if="!material?.contentText" description="当前资料暂时没有可预览的文本内容" />
        <div v-else class="preview-box">
          <pre class="preview-text">{{ material.contentText }}</pre>
        </div>
      </el-tab-pane>

      <el-tab-pane label="知识要点" name="knowledge">
        <el-button type="primary" :loading="extractLoading" @click="extractKnowledge">AI 提炼知识要点</el-button>
        <el-empty v-if="!knowledgeList.length && !extractLoading" description="点击上方按钮从本资料中提炼知识要点" />
        <el-card v-for="(item, i) in knowledgeList" :key="item.id" class="knowledge-card">
          <template #header>
            <span>{{ item.title || ('要点 ' + (i + 1)) }}</span>
            <el-button v-if="!String(item.id).startsWith('err-')" type="danger" link style="float: right;" @click="deleteKnowledge(item.id)">删除</el-button>
          </template>
          <div class="content">{{ item.content }}</div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="模拟题目" name="question">
        <div class="question-toolbar">
          <div class="question-controls">
            <span class="toolbar-label">题型与数量：</span>
            <div class="question-types">
              <div class="type-row">
                <el-checkbox v-model="typeConfig.single.enabled">单选</el-checkbox>
                <el-input-number
                  v-model="typeConfig.single.count"
                  :min="1"
                  :max="20"
                  size="small"
                  :disabled="!typeConfig.single.enabled"
                />
              </div>
              <div class="type-row">
                <el-checkbox v-model="typeConfig.multiple.enabled">多选</el-checkbox>
                <el-input-number
                  v-model="typeConfig.multiple.count"
                  :min="1"
                  :max="20"
                  size="small"
                  :disabled="!typeConfig.multiple.enabled"
                />
              </div>
              <div class="type-row">
                <el-checkbox v-model="typeConfig.fill.enabled">填空</el-checkbox>
                <el-input-number
                  v-model="typeConfig.fill.count"
                  :min="1"
                  :max="20"
                  size="small"
                  :disabled="!typeConfig.fill.enabled"
                />
              </div>
              <div class="type-row">
                <el-checkbox v-model="typeConfig.essay.enabled">简答</el-checkbox>
                <el-input-number
                  v-model="typeConfig.essay.count"
                  :min="1"
                  :max="20"
                  size="small"
                  :disabled="!typeConfig.essay.enabled"
                />
              </div>
            </div>
            <el-button type="primary" :loading="genLoading" @click="generateQuestions">生成题目</el-button>
          </div>
        </div>
        <el-empty v-if="!questionList.length && !genLoading" description="根据资料自动生成练习题，可选择题型和数量" />
        <el-card v-for="(q, i) in questionList" :key="q.id" class="question-card">
          <div class="q-title">{{ i + 1 }}. [{{ typeLabel(q.type) }}] {{ cleanQuestionText(q.questionText) }}</div>
          <div v-if="q.optionsJson" class="options">
            <div v-for="opt in parseOptions(q.optionsJson)" :key="opt.key">{{ opt.key }}. {{ opt.value }}</div>
          </div>
          <el-collapse>
            <el-collapse-item title="答案与解析" name="answer">
              <p><strong>答案：</strong>{{ cleanAnswer(q.answer) }}</p>
              <p v-if="q.analysis"><strong>解析：</strong>{{ q.analysis }}</p>
            </el-collapse-item>
          </el-collapse>
          <el-button v-if="!String(q.id).startsWith('err-')" type="danger" link size="small" @click="deleteQuestion(q.id)">删除</el-button>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="生成记录" name="history">
        <el-empty v-if="!historyList.length" description="暂无生成记录，先去生成一批题目吧" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="item in historyList"
            :key="item.id"
            :timestamp="item.time"
          >
            <el-card class="history-card">
              <div class="history-header">
                <span>共 {{ item.questions.length }} 题 · 题型：{{ formatTypes(item.types) }}</span>
                <el-button type="primary" link @click="loadHistoryRecord(item)">查看这次题目</el-button>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { materialApi, knowledgeApi, questionApi } from '../api'

const route = useRoute()
const router = useRouter()
const materialId = computed(() => Number(route.params.id))
const loading = ref(true)
const material = ref(null)
const activeTab = ref('preview')
const knowledgeList = ref([])
const questionList = ref([])
const extractLoading = ref(false)
const genLoading = ref(false)
const typeConfig = ref({
  single: { enabled: true, count: 5 },
  multiple: { enabled: false, count: 3 },
  fill: { enabled: false, count: 3 },
  essay: { enabled: false, count: 2 },
})
const historyList = ref([])

function goBack() {
  router.push({ name: 'Home' })
}

function goPractice() {
  router.push({ path: '/practice', query: { materialId: String(materialId.value) } })
}

function typeLabel(type) {
  const m = { single: '单选', multiple: '多选', fill: '填空', essay: '简答' }
  return m[type] || type
}

function parseOptions(json) {
  if (!json) return []
  try {
    return JSON.parse(json)
  } catch {
    return []
  }
}

/** 清理题目文本中可能混入的【选项】【答案】【解析】等，避免重复展示 */
function cleanQuestionText(text) {
  if (!text || typeof text !== 'string') return ''
  return text
    .replace(/\s*【选项】[\s\S]*/g, '')
    .replace(/\s*【答案】[\s\S]*/g, '')
    .replace(/\s*【解析】[\s\S]*/g, '')
    .replace(/\s*\[选项\][\s\S]*/gi, '')
    .replace(/\s*\[答案\][\s\S]*/gi, '')
    .replace(/\s*\[解析\][\s\S]*/gi, '')
    .trim()
}

/** 答案中若混入【解析】则只保留答案部分 */
function cleanAnswer(text) {
  if (!text || typeof text !== 'string') return ''
  const idx = text.indexOf('【解析】')
  if (idx !== -1) return text.substring(0, idx).trim()
  return text.trim()
}

async function loadMaterial() {
  try {
    material.value = await materialApi.get(materialId.value)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadKnowledge() {
  try {
    knowledgeList.value = await knowledgeApi.list(materialId.value) || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

async function loadQuestions() {
  try {
    questionList.value = await questionApi.list(materialId.value) || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

function getUserId() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) return null
    const u = JSON.parse(raw)
    return u.userId || null
  } catch {
    return null
  }
}

function getHistoryKey() {
  const uid = getUserId() || 'guest'
  return `sh_question_history_${uid}_${materialId.value}`
}

function loadHistory() {
  try {
    const raw = localStorage.getItem(getHistoryKey())
    historyList.value = raw ? JSON.parse(raw) : []
  } catch {
    historyList.value = []
  }
}

function saveHistory(newRecord) {
  const list = [newRecord, ...historyList.value].slice(0, 20)
  historyList.value = list
  localStorage.setItem(getHistoryKey(), JSON.stringify(list))
}

async function extractKnowledge() {
  extractLoading.value = true
  try {
    knowledgeList.value = await knowledgeApi.extract(materialId.value) || []
    activeTab.value = 'knowledge'
    ElMessage.success('知识要点已生成，请在本页下方查看')
  } catch (e) {
    const msg = e.message || '提炼失败'
    const parsed = parseKnowledgeFromError(msg)
    if (parsed.length) {
      knowledgeList.value = parsed
      activeTab.value = 'knowledge'
      ElMessage.info('已从返回内容解析出要点并显示在下方')
    } else {
      ElMessage.error(msg)
    }
  } finally {
    extractLoading.value = false
  }
}

function parseKnowledgeFromError(text) {
  if (!text || !text.includes('---') || !text.includes('标题')) return []
  const list = []
  const blocks = text.split(/\s*---\s*/)
  const titleRe = /标题[：:]?\s*(.+?)(?=内容|$)/s
  const contentRe = /内容[：:]?\s*(.+?)(?=标题|---|$)/s
  for (const block of blocks) {
    const t = block.match(titleRe)
    const c = block.match(contentRe)
    const title = (t && t[1]) ? t[1].trim() : ''
    const content = (c && c[1]) ? c[1].trim() : ''
    if (title || content) {
      list.push({ id: 'err-' + list.length, title: title || '要点', content })
    }
  }
  return list
}

async function generateQuestions() {
  const tasks = Object.entries(typeConfig.value)
    .filter(([, cfg]) => cfg.enabled && cfg.count > 0)
    .map(([key, cfg]) => ({ type: key, count: cfg.count }))

  if (!tasks.length) {
    ElMessage.warning('请至少勾选一种题型并设置数量')
    return
  }
  genLoading.value = true
  try {
    const expectedTotal = tasks.reduce((sum, t) => sum + t.count, 0)

    // 顺序请求：首轮 replace=true 清空旧题；后续批次 replace=false 追加。
    // 若并行请求，每批都会 delete 全资料题目，最后只剩最后一个批次的少量题。
    let firstBatch = true
    for (const t of tasks) {
      try {
        await questionApi.generate(materialId.value, t.count, [t.type], firstBatch)
        firstBatch = false
      } catch (e) {
        firstBatch = false
        ElMessage.error(e.message || `${t.type} 批次生成失败`)
      }
    }

    await loadQuestions()
    const actualCount = questionList.value.length

    const record = {
      id: Date.now(),
      time: new Date().toLocaleString(),
      types: tasks.map((t) => t.type),
      questions: [...questionList.value],
    }
    saveHistory(record)

    activeTab.value = 'question'

    if (actualCount < expectedTotal) {
      ElMessage.warning(
        `本次期望共 ${expectedTotal} 题，当前资料下共有 ${actualCount} 题。` +
          `偏少时多为模型单次输出未凑满或解析丢题，已自动补请求一轮；仍不足可适当减少单次数量或换更长的资料片段。`,
      )
    } else {
      ElMessage.success('题目已生成，请在本页下方查看')
    }
  } catch (e) {
    const msg = e.message || '生成失败'
    const parsed = parseQuestionsFromError(msg)
    if (parsed.length) {
      questionList.value = parsed
      activeTab.value = 'question'
      ElMessage.info('已从返回内容解析出题目并显示在下方')
    } else {
      ElMessage.error(msg)
    }
  } finally {
    genLoading.value = false
  }
}

function loadHistoryRecord(item) {
  questionList.value = item.questions || []
  const types = Array.isArray(item.types) && item.types.length
    ? item.types
    : ['single', 'multiple', 'fill', 'essay']
  const cfg = { ...typeConfig.value }
  Object.keys(cfg).forEach((key) => {
    cfg[key] = {
      enabled: types.includes(key),
      count: cfg[key].count || 3,
    }
  })
  typeConfig.value = cfg
  activeTab.value = 'question'
}

function formatTypes(types) {
  if (!Array.isArray(types) || !types.length) return '全部'
  const map = { single: '单选', multiple: '多选', fill: '填空', essay: '简答' }
  return types.map((t) => map[t] || t).join(' / ')
}

function parseQuestionsFromError(text) {
  if (!text || !text.includes('【题目】') || !text.includes('===')) return []
  const list = []
  const blocks = text.split(/\s*===\s*/)
  const typeRe = /【题型】\s*(.+?)(?=【|$)/s
  const questionRe = /【题目】\s*(.+?)(?=【|$)/s
  const optionsRe = /【选项】\s*(.+?)(?=【|$)/s
  const answerRe = /【答案】\s*(.+?)(?=【|$)/s
  const analysisRe = /【解析】\s*(.+?)(?=【|===|$)/s
  const typeMap = { '单选': 'single', '多选': 'multiple', '填空': 'fill', '简答': 'essay' }
  for (const block of blocks) {
    const typeM = block.match(typeRe)
    const qM = block.match(questionRe)
    const optM = block.match(optionsRe)
    const ansM = block.match(answerRe)
    const anaM = block.match(analysisRe)
    const questionText = (qM && qM[1]) ? qM[1].trim() : ''
    if (!questionText) continue
    const type = (typeM && typeM[1]) ? typeM[1].trim() : ''
    let typeKey = 'essay'
    for (const [k, v] of Object.entries(typeMap)) {
      if (type.includes(k)) { typeKey = v; break }
    }
    let optionsJson = null
    if (optM && optM[1] && !optM[1].includes('无')) {
      const opts = []
      const re = /([A-D])[.．、]\s*([^A-D.．、]+)/g
      let m
      while ((m = re.exec(optM[1])) !== null) opts.push({ key: m[1], value: m[2].trim() })
      if (opts.length) optionsJson = JSON.stringify(opts)
    }
    list.push({
      id: 'err-' + list.length,
      type: typeKey,
      questionText,
      optionsJson,
      answer: (ansM && ansM[1]) ? ansM[1].trim() : '',
      analysis: (anaM && anaM[1]) ? anaM[1].trim() : ''
    })
  }
  return list
}

async function deleteKnowledge(id) {
  try {
    await knowledgeApi.delete(id)
    loadKnowledge()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

async function deleteQuestion(id) {
  try {
    await questionApi.delete(id)
    loadQuestions()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

function applyTabFromQuery() {
  const t = route.query.tab
  if (t === 'question' || t === 'knowledge' || t === 'preview' || t === 'history') {
    activeTab.value = t
  }
}

watch(() => route.query.tab, () => applyTabFromQuery())

onMounted(async () => {
  await loadMaterial()
  loadKnowledge()
  loadQuestions()
  loadHistory()
  applyTabFromQuery()
})
</script>

<style scoped>
.material-detail-page {
  max-width: 1000px;
  margin: 24px auto;
  padding: 0 24px 24px;
}
.material-top-actions {
  margin-top: 12px;
}
.question-toolbar {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
}
.question-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.toolbar-label {
  font-size: 13px;
  color: #606266;
}
.question-types {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 24px;
}
.type-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.preview-box {
  max-height: 500px;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background-color: #111827;
  color: #e5e7eb;
  overflow-y: auto;
}
.preview-text {
  margin: 0;
  white-space: pre-wrap;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 13px;
  line-height: 1.6;
}
.knowledge-card, .question-card { margin-bottom: 12px; }
.content, .q-title { white-space: pre-wrap; }
.options { margin: 8px 0; padding-left: 16px; }
.history-card {
  font-size: 13px;
}
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
