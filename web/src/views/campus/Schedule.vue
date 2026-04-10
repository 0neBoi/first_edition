<template>
  <div class="page">
    <canvas ref="bgCanvas" class="schedule-canvas" />

    <header class="header">
      <div>
        <h2>本周课程表</h2>
        <p class="sub">按节次查看课程安排，可快速新增/编辑课程。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openDialog()">新增课程</el-button>
        <el-button link type="primary" @click="resetSchedule">清空本地课表</el-button>
      </div>
    </header>

    <section class="schedule-wrapper">
      <div class="legend">
        <span class="legend-dot legend-today" /> 今日
        <span class="legend-dot legend-course" /> 已排课程
      </div>

      <div class="schedule-layout">
        <div class="schedule-main">
          <el-scrollbar class="table-scroll">
            <table class="schedule-table">
              <thead>
                <tr>
                  <th class="time-col">节次</th>
                  <th
                    v-for="(day, index) in days"
                    :key="day.value"
                    :class="['day-col', { today: index === todayIndex }]"
                  >
                    <div class="day-name">{{ day.label }}</div>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="slot in slots" :key="slot.value">
                  <td class="time-col">
                    <div class="slot-name">{{ slot.label }}</div>
                    <div class="slot-time">{{ slot.time }}</div>
                  </td>
                  <td
                    v-for="(day, dayIndex) in days"
                    :key="day.value"
                    :class="['cell', { today: dayIndex === todayIndex }]"
                    @dblclick="openDialog(day.value, slot.value)"
                  >
                    <div
                      v-for="course in coursesByDaySlot(day.value, slot.value)"
                      :key="course.id"
                      class="course-card"
                      :style="courseStyle(course)"
                      @click.stop="openDialog(day.value, slot.value, course)"
                    >
                      <div class="course-name">{{ course.name }}</div>
                      <div class="course-meta">
                        <span v-if="course.teacher">{{ course.teacher }}</span>
                        <span v-if="course.location">{{ course.location }}</span>
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </el-scrollbar>

          <p v-if="!courses.length" class="empty-tip">
            还没有为本周添加课程，点击右上角“新增课程”或双击表格中的任意单元格开始添加。
          </p>
        </div>

        <aside class="schedule-side">
          <el-card class="side-card" shadow="hover">
            <template #header>
              <div class="side-header">今日课程概览</div>
            </template>
            <el-empty v-if="!todayCourses.length" description="今天没有课程安排" />
            <el-timeline v-else>
              <el-timeline-item
                v-for="item in todayCourses"
                :key="item.id"
                :timestamp="slotLabel(item.startSlot, item.endSlot)"
              >
                <div class="side-course-name">{{ item.name }}</div>
                <div class="side-course-meta">
                  <span v-if="item.teacher">{{ item.teacher }}</span>
                  <span v-if="item.location">{{ item.location }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-card>

          <el-card class="side-card" shadow="never" style="margin-top: 12px;">
            <template #header>
              <div class="side-header">本周统计</div>
            </template>
            <ul class="stats-list">
              <li><strong>课程总数：</strong>{{ weekStats.totalCourses }}</li>
              <li><strong>总节次数：</strong>{{ weekStats.totalSlots }}</li>
              <li><strong>平均每天课程：</strong>{{ weekStats.avgPerDay }}</li>
            </ul>
          </el-card>
        </aside>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingCourse ? '编辑课程' : '新增课程'" width="420px">
      <el-form :model="form" label-width="80">
        <el-form-item label="课程名称">
          <el-input v-model="form.name" placeholder="如：高等数学" />
        </el-form-item>
        <el-form-item label="星期">
          <el-select v-model="form.weekday" placeholder="请选择">
            <el-option v-for="d in days" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="节次范围">
          <el-select v-model="form.startSlot" placeholder="开始节次" style="width: 120px; margin-right: 8px;">
            <el-option v-for="s in slots" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-select v-model="form.endSlot" placeholder="结束节次" style="width: 120px;">
            <el-option v-for="s in slots" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="教师">
          <el-input v-model="form.teacher" placeholder="选填" />
        </el-form-item>
        <el-form-item label="教室">
          <el-input v-model="form.location" placeholder="选填，如：教一-305" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" show-alpha :predefine="preColors" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button v-if="editingCourse" type="danger" @click="removeCourse">删除</el-button>
          <el-button type="primary" @click="saveCourse">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resolveCssVarColor } from '../../theme'

const days = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' },
]

const slots = [
  { value: 1, label: '第 1-2 节', time: '08:00 - 09:40' },
  { value: 2, label: '第 3-4 节', time: '10:00 - 11:40' },
  { value: 3, label: '第 5-6 节', time: '14:00 - 15:40' },
  { value: 4, label: '第 7-8 节', time: '16:00 - 17:40' },
  { value: 5, label: '第 9-10 节', time: '19:00 - 20:40' },
]

const todayIndex = computed(() => {
  const jsDay = new Date().getDay() || 7 // 周日返回 0，这里统一成 7
  return days.findIndex((d) => d.value === jsDay)
})

const courses = ref(loadFromStorage())
const bgCanvas = ref(null)
let animationId

const dialogVisible = ref(false)
const editingCourse = ref(null)
const form = ref(initForm())

const preColors = [
  '#38bdf8',
  '#f97316',
  '#a855f7',
  '#22c55e',
  '#facc15',
  '#f43f5e',
]

function initForm() {
  return {
    id: null,
    name: '',
    weekday: days[0].value,
    startSlot: slots[0].value,
    endSlot: slots[0].value,
    teacher: '',
    location: '',
    color: '#38bdf8',
  }
}

function loadFromStorage() {
  try {
    const raw = localStorage.getItem('sh_schedule')
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveToStorage() {
  localStorage.setItem('sh_schedule', JSON.stringify(courses.value))
}

const todayCourses = computed(() => {
  if (todayIndex.value === -1) return []
  const weekday = days[todayIndex.value].value
  return courses.value
    .filter((c) => c.weekday === weekday)
    .sort((a, b) => a.startSlot - b.startSlot || a.endSlot - b.endSlot)
})

const weekStats = computed(() => {
  const totalCourses = courses.value.length
  const totalSlots = courses.value.reduce(
    (sum, c) => sum + (c.endSlot - c.startSlot + 1),
    0,
  )
  const daysWithCourses = new Set(courses.value.map((c) => c.weekday)).size || 1
  const avgPerDay = (totalCourses / daysWithCourses).toFixed(1)
  return { totalCourses, totalSlots, avgPerDay }
})

function coursesByDaySlot(weekday, slot) {
  return courses.value.filter(
    (c) =>
      c.weekday === weekday &&
      c.startSlot <= slot &&
      c.endSlot >= slot,
  )
}

function courseStyle(course) {
  return {
    background:
      course.color ||
      'linear-gradient(135deg, rgba(56,189,248,0.9), rgba(37,99,235,0.9))',
  }
}

function slotLabel(start, end) {
  const startSlot = slots.find((s) => s.value === start)
  const endSlot = slots.find((s) => s.value === end)
  if (!startSlot || !endSlot) return ''
  if (start === end) return startSlot.label
  return `${startSlot.label} ~ ${endSlot.label}`
}

function openDialog(weekday, slot, course) {
  if (course) {
    editingCourse.value = course
    form.value = { ...course }
  } else {
    editingCourse.value = null
    form.value = {
      ...initForm(),
      weekday: weekday || days[0].value,
      startSlot: slot || slots[0].value,
      endSlot: slot || slots[0].value,
    }
  }
  dialogVisible.value = true
}

function saveCourse() {
  const data = form.value
  if (!data.name.trim()) {
    ElMessage.warning('请填写课程名称')
    return
  }
  if (data.startSlot > data.endSlot) {
    ElMessage.warning('结束节次不能早于开始节次')
    return
  }

  if (editingCourse.value) {
    const idx = courses.value.findIndex((c) => c.id === editingCourse.value.id)
    if (idx !== -1) {
      courses.value[idx] = { ...data }
    }
  } else {
    const id = Date.now()
    courses.value.push({ ...data, id })
  }

  saveToStorage()
  dialogVisible.value = false
  ElMessage.success('已保存')
}

function removeCourse() {
  ElMessageBox.confirm('确认删除该课程？', '提示', {
    type: 'warning',
  })
    .then(() => {
      courses.value = courses.value.filter((c) => c.id !== editingCourse.value.id)
      saveToStorage()
      dialogVisible.value = false
      ElMessage.success('已删除')
    })
    .catch(() => {})
}

function resetSchedule() {
  ElMessageBox.confirm('将清空当前账号在本机保存的所有课表数据，是否继续？', '提示', {
    type: 'warning',
  })
    .then(() => {
      courses.value = []
      saveToStorage()
      ElMessage.success('已清空')
    })
    .catch(() => {})
}

function setupStarfield() {
  const canvas = bgCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const resize = () => {
    const rect = canvas.parentElement.getBoundingClientRect()
    const ratio = window.devicePixelRatio || 1
    canvas.width = rect.width * ratio
    canvas.height = rect.height * ratio
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
  }

  resize()
  window.addEventListener('resize', resize)

  const stars = []
  const count = 220
  for (let i = 0; i < count; i += 1) {
    const isMeteor = i % 7 === 0
    stars.push({
      x: Math.random() * canvas.clientWidth,
      y: Math.random() * canvas.clientHeight,
      r: isMeteor ? 1.8 + Math.random() * 1.4 : Math.random() * 1.2 + 0.5,
      speed: isMeteor ? 0.9 + Math.random() * 0.8 : 0.15 + Math.random() * 0.35,
      alpha: 0.5 + Math.random() * 0.5,
      twinkle: Math.random() * Math.PI * 2,
      meteor: isMeteor,
    })
  }

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    const grd = ctx.createRadialGradient(
      canvas.clientWidth * 0.5,
      canvas.clientHeight * 0.1,
      0,
      canvas.clientWidth * 0.5,
      canvas.clientHeight * 0.5,
      canvas.clientHeight * 0.95,
    )
    grd.addColorStop(0, 'rgba(56, 189, 248, 0.4)')
    grd.addColorStop(0.35, resolveCssVarColor('--glass-92'))
    grd.addColorStop(1, 'rgba(0, 0, 0, 1)')
    ctx.fillStyle = grd
    ctx.fillRect(0, 0, canvas.clientWidth, canvas.clientHeight)

    stars.forEach((s, idx) => {
      s.y += s.speed
      if (s.meteor) s.x -= s.speed * 1.1

      if (s.y > canvas.clientHeight + 80 || s.x < -80) {
        s.y = -40 - Math.random() * 60
        s.x = canvas.clientWidth * (0.4 + Math.random() * 0.6)
      }
      s.twinkle += 0.02 + idx * 0.00025
      const twinkleFactor = 0.6 + Math.sin(s.twinkle) * 0.4

      if (s.meteor) {
        const tailLength = 80
        const grad = ctx.createLinearGradient(
          s.x,
          s.y,
          s.x + tailLength,
          s.y - tailLength * 0.7,
        )
        grad.addColorStop(0, `rgba(191, 219, 254, ${0.95 * twinkleFactor})`)
        grad.addColorStop(1, 'transparent')
        ctx.strokeStyle = grad
        ctx.lineWidth = 2

        ctx.beginPath()
        ctx.moveTo(s.x, s.y)
        ctx.lineTo(s.x + tailLength, s.y - tailLength * 0.7)
        ctx.stroke()

        ctx.beginPath()
        ctx.fillStyle = `rgba(248, 250, 252, ${0.95 * twinkleFactor})`
        ctx.arc(s.x, s.y, s.r * 1.6, 0, Math.PI * 2)
        ctx.fill()
      } else {
        ctx.beginPath()
        ctx.fillStyle = `rgba(191, 219, 254, ${s.alpha * twinkleFactor})`
        ctx.arc(s.x, s.y, s.r * twinkleFactor, 0, Math.PI * 2)
        ctx.fill()
      }
    })

    animationId = requestAnimationFrame(draw)
  }

  draw()
}

onMounted(() => {
  setupStarfield()
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
})
</script>

<style scoped>
.page {
  max-width: 980px;
  margin: 24px auto;
  padding: 0 24px 32px;
  position: relative;
  min-height: calc(100vh - 160px);
}

.schedule-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.header h2 {
  margin: 0;
  font-size: 22px;
}

.sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.schedule-wrapper {
  margin-top: 16px;
  position: relative;
  z-index: 1;
}

.schedule-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(260px, 0.9fr);
  gap: 16px;
  align-items: flex-start;
}

.schedule-main {
  min-width: 0;
}

.legend {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  margin-right: 4px;
}

.legend-today {
  background-color: rgba(59, 130, 246, 0.8);
}

.legend-course {
  background-color: rgba(56, 189, 248, 0.8);
}

.table-scroll {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.22), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(37, 99, 235, 0.25), transparent 60%),
    var(--glass-70);
  backdrop-filter: blur(16px);
}

.schedule-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  background-color: transparent;
  color: #e5e7eb;
}

.schedule-table th,
.schedule-table td {
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
  border-right: 1px solid rgba(148, 163, 184, 0.25);
}

.schedule-table th:last-child,
.schedule-table td:last-child {
  border-right: none;
}

.time-col {
  width: 96px;
  text-align: left;
  background-color: var(--glass-90);
  padding: 8px;
  font-size: 12px;
  color: #e5e7eb;
}

.day-col {
  text-align: center;
  padding: 8px;
  font-size: 13px;
  background-color: transparent;
}

.day-col.today {
  background-color: rgba(37, 99, 235, 0.3);
  color: #bfdbfe;
}

.day-name {
  font-weight: 500;
}

.slot-name {
  font-weight: 500;
}

.slot-time {
  margin-top: 2px;
  font-size: 11px;
}

.cell {
  position: relative;
  height: 72px;
  padding: 4px;
  background-color: var(--glass-50);
  vertical-align: top;
  cursor: pointer;
}

.cell.today {
  background-color: rgba(37, 99, 235, 0.25);
}

.course-card {
  height: 100%;
  border-radius: 8px;
  padding: 4px 6px;
  color: #f9fafb;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(var(--neon-accent-rgb), 0.22);
}

.course-name {
  font-weight: 600;
  line-height: 1.2;
}

.course-meta {
  margin-top: 2px;
  font-size: 11px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  opacity: 0.9;
}

.empty-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.schedule-side {
  min-width: 0;
}

.side-card {
  font-size: 13px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background:
    radial-gradient(circle at 0 0, rgba(56, 189, 248, 0.18), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(37, 99, 235, 0.22), transparent 60%),
    var(--glass-75);
  backdrop-filter: blur(18px);
  color: #e5e7eb;
}

.side-header {
  font-weight: 600;
}

.side-course-name {
  font-weight: 600;
}

.side-course-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #606266;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.stats-list {
  margin: 0;
  padding-left: 16px;
  font-size: 13px;
  color: #606266;
}

.stats-list li {
  margin: 4px 0;
}

@media (max-width: 768px) {
  .page {
    padding-inline: 12px;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .schedule-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
