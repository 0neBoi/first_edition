<template>
  <div class="landing">加载中...</div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  const token = localStorage.getItem('token')
  if (token) {
    router.replace('/home')
  } else {
    router.replace('/login')
  }
})
</script>

<style scoped>
.landing {
  padding: 48px;
  text-align: center;
  color: #999;
}
</style>
