<template>
  <div class="admin-users">
    <div class="head">
      <h2>用户管理</h2>
      <p class="hint">
        查看已注册学生账号数量与列表；可为其重置登录密码，或禁用账号（逻辑删除，对方无法继续登录）。管理员账号由服务器配置，不在此列表中。
      </p>
      <p class="stat-line">
        当前共 <strong>{{ total }}</strong> 个注册用户
      </p>
    </div>

    <el-table v-loading="loading" :data="list" stripe class="table">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
      <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="school" label="学校" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openPwd(row)">重置密码</el-button>
          <el-button type="danger" link @click="remove(row)">禁用账号</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog v-model="pwdDialog" title="重置密码" width="420px" destroy-on-close @closed="pwdTarget = null">
      <p v-if="pwdTarget" class="pwd-hint">用户：{{ pwdTarget.username }}</p>
      <el-input v-model="newPwd" type="password" placeholder="新密码（至少 6 位）" show-password clearable />
      <template #footer>
        <el-button @click="pwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="savePwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUserApi } from '../../api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const pwdDialog = ref(false)
const pwdSaving = ref(false)
const pwdTarget = ref(null)
const newPwd = ref('')

async function load() {
  loading.value = true
  try {
    const data = await adminUserApi.page({ page: page.value, size: pageSize.value })
    list.value = data.records || []
    total.value = data.total ?? 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onSizeChange() {
  page.value = 1
  load()
}

function openPwd(row) {
  pwdTarget.value = row
  newPwd.value = ''
  pwdDialog.value = true
}

async function savePwd() {
  if (!newPwd.value || newPwd.value.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  pwdSaving.value = true
  try {
    await adminUserApi.resetPassword(pwdTarget.value.id, newPwd.value)
    ElMessage.success('密码已重置')
    pwdDialog.value = false
  } catch (e) {
    ElMessage.error(e.message || '失败')
  } finally {
    pwdSaving.value = false
  }
}

function remove(row) {
  ElMessageBox.confirm(
    `将禁用用户「${row.username}」：对方将无法登录（数据为逻辑删除）。确定？`,
    '禁用账号',
    { type: 'warning', confirmButtonText: '确定禁用', cancelButtonText: '取消' },
  )
    .then(async () => {
      await adminUserApi.delete(row.id)
      ElMessage.success('已禁用')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.admin-users {
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
  margin: 0 0 10px;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.55;
}
.stat-line {
  margin: 0;
  font-size: 14px;
  color: #cbd5e1;
}
.stat-line strong {
  color: var(--el-color-primary);
  font-size: 18px;
}
.table {
  border-radius: 8px;
}
.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.pwd-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #94a3b8;
}
</style>
