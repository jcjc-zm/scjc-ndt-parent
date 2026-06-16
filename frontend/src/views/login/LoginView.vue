<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({
        username: loginForm.username,
        password: loginForm.password,
      })
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/dashboard'
      router.push(redirect)
    } catch (error) {
      // Error handled by request interceptor
    } finally {
      loading.value = false
    }
  })
}

// Quick-fill for development
function quickFill(account) {
  loginForm.username = account
  loginForm.password = 'admin123'
}
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <!-- Brand -->
      <div class="login-brand">
        <div class="brand-icon">
          <el-icon :size="48"><Monitor /></el-icon>
        </div>
        <h1 class="brand-title">SCJC NDT</h1>
        <p class="brand-subtitle">无损检测管理系统</p>
      </div>

      <!-- Form -->
      <div class="login-card">
        <h2 class="login-title">用户登录</h2>
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- Dev quick access -->
        <div class="quick-accounts">
          <span class="quick-label">快速登录:</span>
          <el-tag
            v-for="acc in ['admin', 'zhangsan', 'lisi']"
            :key="acc"
            class="quick-tag"
            size="small"
            @click="quickFill(acc)"
          >
            {{ acc }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <p class="login-footer">© 2026 SCJC 四川川际工程技术有限公司</p>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #334155 50%, #1e293b 100%);
  padding: var(--space-6);
}

.login-container {
  display: flex;
  align-items: stretch;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  max-width: 800px;
  width: 100%;
}

.login-brand {
  flex: 1;
  background: linear-gradient(135deg, #1e293b, #334155);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-8);
  color: #fff;
}

.brand-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-6);
  color: var(--color-accent-light);
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 2px;
  margin-bottom: var(--space-2);
}

.brand-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.login-card {
  flex: 1;
  padding: var(--space-12) var(--space-8);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-8);
  text-align: center;
}

.login-btn {
  width: 100%;
}

.quick-accounts {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  margin-top: var(--space-4);
  flex-wrap: wrap;
}

.quick-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.quick-tag {
  cursor: pointer;
}

.login-footer {
  margin-top: var(--space-8);
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}

/* Responsive: stack on small screens */
@media (max-width: 640px) {
  .login-container {
    flex-direction: column;
    max-width: 400px;
  }

  .login-brand {
    padding: var(--space-8);
  }

  .login-card {
    padding: var(--space-8) var(--space-6);
  }
}
</style>
