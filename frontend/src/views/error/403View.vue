<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <h1 class="error-code">403</h1>
    <p class="error-message">抱歉，您没有权限访问此页面</p>
    <el-button type="primary" @click="router.push('/dashboard')">返回首页</el-button>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  text-align: center;
}

.error-code {
  font-size: 96px;
  font-weight: 700;
  color: var(--color-text-placeholder);
  line-height: 1;
  margin-bottom: var(--space-4);
}

.error-message {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin-bottom: var(--space-8);
}
</style>
