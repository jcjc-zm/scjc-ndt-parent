<script setup>
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
}
</script>

<template>
  <header class="header-bar">
    <div class="header-left">
      <!-- Breadcrumb -->
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.meta.title && route.path !== '/dashboard'">
          {{ route.meta.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <!-- User info -->
      <el-dropdown trigger="click" placement="bottom-end">
        <div class="user-info">
          <el-icon :size="20"><UserFilled /></el-icon>
          <span class="username">{{ userStore.realName || userStore.username }}</span>
          <el-icon :size="14"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              <div class="dropdown-user-detail">
                <div>{{ userStore.realName || userStore.username }}</div>
                <div class="text-secondary" style="font-size: 12px">
                  {{ userStore.deptName }} · {{ userStore.roles.join(' / ') }}
                </div>
              </div>
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped>
.header-bar {
  height: var(--header-height);
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
  color: var(--color-text-regular);
}

.user-info:hover {
  background: var(--color-bg-hover);
}

.username {
  font-size: 14px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-user-detail {
  line-height: 1.5;
}
</style>
