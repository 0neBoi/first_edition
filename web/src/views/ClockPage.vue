<template>
  <div class="sh-page">
    <div class="sh-page-head">
      <h2>学习打卡</h2>
      <p class="muted">按日记录学习分钟数；日历颜色越深表示当日投入越多。连续天数支持「昨日也算」的宽松规则。</p>
    </div>
    <el-row :gutter="20">
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <div class="stat-block">
            <div class="stat-label">本月累计（分钟）</div>
            <div class="stat-value">{{ monthTotal }}</div>
          </div>
          <div class="stat-block">
            <div class="stat-label">连续学习天数</div>
            <div class="stat-value">{{ streak }}</div>
          </div>
          <el-form label-width="100px" class="clock-form">
            <el-form-item label="日期">
              <el-date-picker v-model="formDate" type="date" value-format="YYYY-MM-DD" placeholder="选日期" />
            </el-form-item>
            <el-form-item label="分钟数">
              <el-input-number v-model="formMinutes" :min="0" :max="1440" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="formRemark" maxlength="255" placeholder="可选" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveDay">保存当日</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="16">
        <el-card shadow="hover">
          <div class="cal-head">
            <el-button text @click="shiftMonth(-1)">上月</el-button>
            <span class="ym">{{ yearMonth }}</span>
            <el-button text @click="shiftMonth(1)">下月</el-button>
          </div>
          <el-calendar v-model="calDate">
            <template #date-cell="{ data }">
              <div class="heat-cell" :style="cellStyle(data.day)">
                <span class="d">{{ data.day.split('-').slice(2).join('') }}</span>
                <span class="m" v-if="minutesForDay(data.day)">{{ minutesForDay(data.day) }}′</span>
              </div>
            </template>
          </el-calendar>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { clockApi } from '../api'

const calDate = ref(new Date())
const monthMap = ref({})
const monthTotal = ref(0)
const streak = ref(0)
const saving = ref(false)
const formDate = ref(new Date().toISOString().slice(0, 10))
const formMinutes = ref(30)
const formRemark = ref('')

const yearMonth = computed(() => {
  const d = calDate.value
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  return `${y}-${String(m).padStart(2, '0')}`
})

function minutesForDay(day) {
  return monthMap.value[day] || 0
}

function cellStyle(day) {
  const m = minutesForDay(day)
  if (!m) return {}
  const t = Math.min(m / 120, 1)
  const a = 0.15 + t * 0.75
  return { background: `rgba(56, 189, 248, ${a})` }
}

async function loadMonth() {
  try {
    const res = await clockApi.month(yearMonth.value)
    monthMap.value = res.days || {}
    monthTotal.value = res.totalMinutes ?? 0
  } catch (e) {
    ElMessage.error(e.message || '加载月数据失败')
  }
}

async function loadStreak() {
  try {
    const res = await clockApi.streak()
    streak.value = res.streakDays ?? 0
  } catch (e) {
    ElMessage.error(e.message || '加载连续天数失败')
  }
}

function shiftMonth(delta) {
  const d = new Date(calDate.value)
  d.setMonth(d.getMonth() + delta)
  calDate.value = d
}

async function saveDay() {
  saving.value = true
  try {
    await clockApi.upsertDay({
      clockDate: formDate.value,
      minutes: formMinutes.value,
      remark: formRemark.value || undefined,
    })
    ElMessage.success('已记录')
    await Promise.all([loadMonth(), loadStreak()])
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

watch(calDate, () => loadMonth())
watch(yearMonth, () => loadMonth())

onMounted(() => {
  loadMonth()
  loadStreak()
})
</script>

<style scoped>
.sh-page {
  padding: 24px;
  max-width: 1100px;
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
.stat-block {
  margin-bottom: 16px;
}
.stat-label {
  font-size: 12px;
  color: #94a3b8;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #38bdf8;
}
.clock-form {
  margin-top: 12px;
}
.cal-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 8px;
}
.ym {
  font-weight: 600;
}
.heat-cell {
  min-height: 52px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 12px;
}
.heat-cell .m {
  font-size: 11px;
  color: var(--neon-bg-deep);
  margin-top: 2px;
}
</style>
