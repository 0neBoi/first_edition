<template>
  <div class="sh-page" v-loading="loading">
    <div class="sh-page-head">
      <el-page-header @back="goBack" title="返回列表" />
      <h2>{{ isNew ? '新建笔记' : '编辑笔记' }}</h2>
    </div>
    <el-form label-position="top" class="form">
      <el-form-item label="标题">
        <el-input v-model="form.title" placeholder="标题" maxlength="200" show-word-limit />
      </el-form-item>
      <el-form-item label="标签（逗号或空格分隔）">
        <el-input v-model="form.tags" placeholder="如：高数,期末" maxlength="255" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input v-model="form.content" type="textarea" :rows="16" placeholder="写点什么…" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { noteApi } from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const form = ref({ title: '', content: '', tags: '' })

const isNew = computed(() => route.params.id === 'new')

function goBack() {
  router.push('/notes')
}

async function load() {
  if (isNew.value) {
    form.value = { title: '', content: '', tags: '' }
    return
  }
  loading.value = true
  try {
    const n = await noteApi.get(Number(route.params.id))
    form.value = {
      title: n.title || '',
      content: n.content || '',
      tags: n.tags || '',
    }
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
    router.push('/notes')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    if (isNew.value) {
      const created = await noteApi.create({
        title: form.value.title,
        content: form.value.content,
        tags: form.value.tags,
      })
      ElMessage.success('已创建')
      router.replace(`/notes/${created.id}`)
    } else {
      await noteApi.update(Number(route.params.id), {
        title: form.value.title,
        content: form.value.content,
        tags: form.value.tags,
      })
      ElMessage.success('已保存')
    }
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
watch(
  () => route.params.id,
  () => load(),
)
</script>

<style scoped>
.sh-page {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
  color: #e5e7eb;
}
.sh-page-head h2 {
  margin: 12px 0 0;
  font-size: 20px;
}
.form {
  margin-top: 16px;
}
</style>
