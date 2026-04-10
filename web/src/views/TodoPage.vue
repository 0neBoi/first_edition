<template>
  <div class="sh-page">
    <div class="sh-page-head">
      <h2>待办</h2>
      <p class="muted">看板按状态分列；日历查看截止日分布；今日聚焦未完成任务。</p>
    </div>
    <el-tabs v-model="tab">
      <el-tab-pane label="看板" name="kanban">
        <div class="kanban">
          <el-card class="col">
            <template #header>进行中</template>
            <div v-for="t in openTodos" :key="t.id" class="todo-card">
              <div class="todo-title">{{ t.title }}</div>
              <div class="todo-meta">
                <span v-if="t.dueDate">截止 {{ t.dueDate }}</span>
                <span>优先级 {{ t.priority ?? 0 }}</span>
              </div>
              <div class="todo-actions">
                <el-button size="small" @click="toggleDone(t, 1)">完成</el-button>
                <el-button size="small" link type="primary" @click="openEdit(t)">编辑</el-button>
              </div>
            </div>
            <el-empty v-if="!openTodos.length" description="暂无待办" />
          </el-card>
          <el-card class="col">
            <template #header>已完成</template>
            <div v-for="t in doneTodos" :key="t.id" class="todo-card done">
              <div class="todo-title">{{ t.title }}</div>
              <div class="todo-actions">
                <el-button size="small" @click="toggleDone(t, 0)">恢复</el-button>
                <el-button size="small" link type="danger" @click="remove(t)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!doneTodos.length" description="还没有完成的任务" />
          </el-card>
        </div>
      </el-tab-pane>
      <el-tab-pane label="日历" name="cal">
        <el-calendar v-model="calDate">
          <template #date-cell="{ data }">
            <div class="cal-cell">
              <span class="day">{{ data.day.split('-').slice(2).join('-') }}</span>
              <span v-if="dueMap[data.day]" class="badge">{{ dueMap[data.day] }} 项</span>
            </div>
          </template>
        </el-calendar>
      </el-tab-pane>
      <el-tab-pane label="今日" name="today">
        <el-button type="primary" @click="openCreate" style="margin-bottom: 12px">新建待办</el-button>
        <el-table :data="todayList" stripe v-loading="loadingToday">
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="dueDate" label="截止日" width="120" />
          <el-table-column prop="priority" label="优先级" width="90" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button type="primary" link @click="toggleDone(row, 1)">完成</el-button>
              <el-button link @click="openEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑待办' : '新建待办'" width="480px" @closed="resetForm">
      <el-form label-width="88px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="截止日">
          <el-date-picker v-model="editForm.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="可选" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="editForm.priority" :min="0" :max="9" />
        </el-form-item>
        <el-form-item v-if="editingId" label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="0">待办</el-radio>
            <el-radio :label="1">完成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTodo">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { todoApi } from '../api'

const tab = ref('kanban')
const allTodos = ref([])
const loadingToday = ref(false)
const todayList = ref([])
const calDate = ref(new Date())
const dueMap = ref({})

const openTodos = computed(() => allTodos.value.filter((t) => t.status === 0))
const doneTodos = computed(() => allTodos.value.filter((t) => t.status === 1).slice(0, 50))

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const editForm = ref({
  title: '',
  content: '',
  dueDate: null,
  priority: 0,
  status: 0,
})

async function loadKanban() {
  try {
    allTodos.value = (await todoApi.list({})) || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

async function loadToday() {
  loadingToday.value = true
  try {
    todayList.value = (await todoApi.today()) || []
    maybeNotify(todayList.value)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loadingToday.value = false
  }
}

function monthRange(d) {
  const y = d.getFullYear()
  const m = d.getMonth()
  const pad = (n) => String(n).padStart(2, '0')
  const from = `${y}-${pad(m + 1)}-01`
  const last = new Date(y, m + 1, 0).getDate()
  const to = `${y}-${pad(m + 1)}-${pad(last)}`
  return { from, to }
}

async function loadCalendarMonth() {
  const { from, to } = monthRange(calDate.value)
  try {
    const list = (await todoApi.list({ dueFrom: from, dueTo: to })) || []
    const map = {}
    for (const t of list) {
      if (!t.dueDate) continue
      map[t.dueDate] = (map[t.dueDate] || 0) + 1
    }
    dueMap.value = map
  } catch (e) {
    ElMessage.error(e.message || '日历加载失败')
  }
}

function maybeNotify(list) {
  if (!list.length || !('Notification' in window)) return
  if (Notification.permission === 'granted') {
    new Notification('今日待办', { body: `还有 ${list.length} 项待完成` })
  }
}

async function toggleDone(row, status) {
  try {
    await todoApi.update(row.id, { status })
    ElMessage.success(status === 1 ? '已完成' : '已恢复')
    await Promise.all([loadKanban(), loadToday(), loadCalendarMonth()])
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('删除该待办？', '提示', { type: 'warning' })
    await todoApi.delete(row.id)
    ElMessage.success('已删除')
    await Promise.all([loadKanban(), loadToday(), loadCalendarMonth()])
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

function openCreate() {
  editingId.value = null
  editForm.value = { title: '', content: '', dueDate: null, priority: 0, status: 0 }
  dialogVisible.value = true
}

function openEdit(t) {
  editingId.value = t.id
  editForm.value = {
    title: t.title,
    content: t.content || '',
    dueDate: t.dueDate || null,
    priority: t.priority ?? 0,
    status: t.status ?? 0,
  }
  dialogVisible.value = true
}

function resetForm() {
  editingId.value = null
}

async function saveTodo() {
  saving.value = true
  try {
    if (editingId.value) {
      await todoApi.update(editingId.value, {
        title: editForm.value.title,
        content: editForm.value.content,
        dueDate: editForm.value.dueDate || undefined,
        clearDueDate: !editForm.value.dueDate,
        priority: editForm.value.priority,
        status: editForm.value.status,
      })
    } else {
      await todoApi.create({
        title: editForm.value.title,
        content: editForm.value.content,
        dueDate: editForm.value.dueDate || undefined,
        priority: editForm.value.priority,
        status: 0,
      })
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await Promise.all([loadKanban(), loadToday(), loadCalendarMonth()])
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

watch(tab, (v) => {
  if (v === 'today') loadToday()
  if (v === 'cal') loadCalendarMonth()
})

watch(calDate, () => loadCalendarMonth())

onMounted(() => {
  loadKanban()
  loadCalendarMonth()
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
.kanban {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 768px) {
  .kanban {
    grid-template-columns: 1fr;
  }
}
.col {
  min-height: 200px;
}
.todo-card {
  padding: 10px;
  margin-bottom: 10px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--glass-60);
}
.todo-card.done {
  opacity: 0.85;
}
.todo-title {
  font-weight: 600;
  margin-bottom: 6px;
}
.todo-meta {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}
.todo-actions {
  display: flex;
  gap: 8px;
}
.cal-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 12px;
}
.cal-cell .badge {
  margin-top: 4px;
  color: #38bdf8;
  font-size: 11px;
}
</style>
