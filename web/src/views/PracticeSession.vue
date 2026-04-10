<template>
  <div v-if="cur" class="session">
    <div class="progress">第 {{ idx + 1 }} / {{ questions.length }} 题 · {{ typeLabel(cur.type) }}</div>
    <div class="qtext">{{ cur.questionText }}</div>

    <el-radio-group v-if="cur.type === 'single' && options.length" v-model="userAnswer" class="opt-group">
      <el-radio v-for="o in options" :key="o.key" :label="o.key">{{ o.key }}. {{ o.value }}</el-radio>
    </el-radio-group>

    <div v-else-if="cur.type === 'multiple' && options.length" class="multi">
      <label v-for="o in options" :key="o.key" class="chk">
        <input type="checkbox" :checked="multiSet.has(o.key)" @change="toggleMulti(o.key, $event)" />
        {{ o.key }}. {{ o.value }}
      </label>
    </div>

    <el-input
      v-else-if="cur.type === 'fill' || cur.type === 'essay' || (cur.type === 'single' && !options.length)"
      v-model="userAnswer"
      :type="cur.type === 'essay' ? 'textarea' : 'text'"
      :rows="cur.type === 'essay' ? 5 : 1"
      placeholder="填写答案"
    />

    <div class="actions">
      <el-button :disabled="idx === 0" @click="prev">上一题</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交本题</el-button>
      <el-button :disabled="idx >= questions.length - 1" @click="next">下一题</el-button>
    </div>

    <el-collapse v-if="lastResult" v-model="collapseNames" style="margin-top: 12px">
      <el-collapse-item :title="lastResult.correct ? '查看解析' : '参考答案'" name="r">
        <p>结果：{{ lastResult.correct ? '正确' : '错误' }} · 掌握度评分 {{ lastResult.masteryScore }}/5</p>
        <p>参考答案：{{ lastResult.standardAnswer || '—' }}</p>
        <p v-if="lastResult.analysis">解析：{{ lastResult.analysis }}</p>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { practiceApi } from '../api'

const props = defineProps({
  questions: { type: Array, required: true },
})
const emit = defineEmits(['submitted'])

const idx = ref(0)
const userAnswer = ref('')
const lastResult = ref(null)
const submitting = ref(false)
const startTs = ref(Date.now())
const collapseNames = ref([])

const cur = computed(() => props.questions[idx.value])

const options = computed(() => {
  if (!cur.value?.optionsJson) return []
  try {
    return JSON.parse(cur.value.optionsJson)
  } catch {
    return []
  }
})

const multiSet = computed(() => new Set(userAnswer.value ? userAnswer.value.split(',').filter(Boolean) : []))

function typeLabel(type) {
  const m = { single: '单选', multiple: '多选', fill: '填空', essay: '简答' }
  return m[type] || type
}

function resetQuestionState() {
  userAnswer.value = ''
  lastResult.value = null
  collapseNames.value = []
  startTs.value = Date.now()
}

function toggleMulti(key, e) {
  const s = new Set(multiSet.value)
  if (e.target.checked) s.add(key)
  else s.delete(key)
  userAnswer.value = Array.from(s).sort().join(',')
}

watch(idx, resetQuestionState)

watch(
  () => props.questions,
  () => {
    idx.value = 0
    resetQuestionState()
  },
  { deep: true },
)

async function submit() {
  if (!cur.value) return
  submitting.value = true
  try {
    const timeCostMs = Date.now() - startTs.value
    const res = await practiceApi.submit({
      questionId: cur.value.id,
      userAnswer: userAnswer.value,
      timeCostMs,
    })
    lastResult.value = res
    collapseNames.value = ['r']
    emit('submitted')
    ElMessage[res.correct ? 'success' : 'warning'](res.correct ? '回答正确' : '再想想看')
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function next() {
  if (idx.value < props.questions.length - 1) idx.value += 1
  else ElMessage.info('已是最后一题')
}

function prev() {
  if (idx.value > 0) idx.value -= 1
}
</script>

<style scoped>
.session {
  margin-top: 12px;
}
.progress {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 8px;
}
.qtext {
  margin-bottom: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.opt-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.multi {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.chk {
  font-size: 14px;
  color: #e5e7eb;
}
.actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
