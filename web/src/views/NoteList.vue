<template>
  <div class="sh-page">
    <div class="sh-page-head">
      <h2>笔记</h2>
      <p class="muted">按标签筛选，支持标题与正文关键字搜索。</p>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索标题或正文" clearable style="width: 220px" @keyup.enter="load" />
        <el-input v-model="tag" placeholder="标签" clearable style="width: 140px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="createNew">新建笔记</el-button>
      </div>
    </div>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="tags" label="标签" width="140" />
      <el-table-column prop="updateTime" label="更新" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="$router.push(`/notes/${row.id}`)">编辑</el-button>
          <el-button type="danger" link @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { noteApi } from '../api'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const keyword = ref('')
const tag = ref('')

async function load() {
  loading.value = true
  try {
    list.value = (await noteApi.list({ keyword: keyword.value || undefined, tag: tag.value || undefined })) || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function createNew() {
  router.push('/notes/new')
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该笔记？', '提示', { type: 'warning' })
    await noteApi.delete(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

onMounted(load)
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
  font-size: 22px;
}
.muted {
  margin: 0 0 16px;
  color: #94a3b8;
  font-size: 13px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
