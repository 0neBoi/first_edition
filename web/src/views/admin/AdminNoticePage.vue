<template>
  <div class="admin-notice">
    <div class="head">
      <h2>校园公告管理</h2>
      <p class="hint">使用 root 账号登录后可在此发布、编辑、删除公告；师生在「校园公告」菜单查看公开列表。</p>
      <el-button type="primary" @click="openCreate">发布公告</el-button>
    </div>

    <el-table v-loading="loading" :data="list" stripe class="table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="noticeType" label="类型" width="100" />
      <el-table-column prop="publisher" label="发布单位" width="120" />
      <el-table-column label="置顶" width="70">
        <template #default="{ row }">{{ row.pinned ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="publishDate" label="发布日期" width="120" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑公告' : '发布公告'" width="640px" destroy-on-close>
      <el-form :model="form" label-width="88px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="公告标题" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.noticeType" placeholder="选择类型" style="width: 100%">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布单位">
          <el-input v-model="form.publisher" placeholder="如：教务处" />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="form.publishDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" style="width: 100%" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" />
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input v-model="form.content" type="textarea" :rows="12" placeholder="支持多段，换行将在公告页显示为分段" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { campusAnnouncementApi } from '../../api'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)

const types = [
  { value: 'teaching', label: '教务教学' },
  { value: 'calendar', label: '校历放假' },
  { value: 'activity', label: '活动赛事' },
  { value: 'service', label: '服务提醒' },
  { value: 'security', label: '安全提示' },
]

const form = reactive({
  title: '',
  content: '',
  noticeType: 'teaching',
  publisher: '教务处',
  pinned: false,
  publishDate: '',
})

function resetForm() {
  form.title = ''
  form.content = ''
  form.noticeType = 'teaching'
  form.publisher = '教务处'
  form.pinned = false
  form.publishDate = ''
}

async function load() {
  loading.value = true
  try {
    const data = await campusAnnouncementApi.publicList()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.title = row.title || ''
  form.content = row.content || ''
  form.noticeType = row.noticeType || 'teaching'
  form.publisher = row.publisher || '教务处'
  form.pinned = !!row.pinned
  form.publishDate = row.publishDate || ''
  dialogVisible.value = true
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写正文')
    return
  }
  const body = {
    title: form.title.trim(),
    content: form.content,
    noticeType: form.noticeType,
    publisher: form.publisher || '教务处',
    pinned: form.pinned ? 1 : 0,
    publishDate: form.publishDate || null,
  }
  saving.value = true
  try {
    if (editingId.value) {
      await campusAnnouncementApi.update(editingId.value, body)
      ElMessage.success('已保存')
    } else {
      await campusAnnouncementApi.create(body)
      ElMessage.success('已发布')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function remove(row) {
  ElMessageBox.confirm(`确定删除「${row.title}」？`, '确认删除', { type: 'warning' })
    .then(async () => {
      await campusAnnouncementApi.delete(row.id)
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.admin-notice {
  padding: 24px;
  max-width: 1100px;
  margin: 0 auto;
  color: #e5e7eb;
}
.head {
  margin-bottom: 20px;
}
.head h2 {
  margin: 0 0 8px;
  font-size: 22px;
}
.hint {
  margin: 0 0 16px;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.5;
}
.table {
  border-radius: 8px;
}
</style>
