<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

// Build menu items with visibility based on user roles
const menuItems = computed(() => {
  const items = [
    {
      index: '/dashboard',
      title: '驾驶舱',
      icon: 'Odometer',
      visible: true,
    },
    {
      index: '/project',
      title: '数据管理',
      icon: 'FolderOpened',
      visible: true,
    },
    {
      index: '/approval',
      title: '签字审批',
      icon: 'Edit',
      visible: userStore.canSign,
    },
    {
      index: '/report',
      title: '报告管理',
      icon: 'Document',
      visible: true,
    },
    {
      index: '/user',
      title: '用户管理',
      icon: 'User',
      visible: userStore.canManageUsers,
    },
    {
      index: '/system',
      title: '系统设置',
      icon: 'Setting',
      visible: userStore.isSystemAdmin,
      children: [
        { index: '/system/dept', title: '组织架构' },
        { index: '/system/role', title: '角色管理' },
        { index: '/system/images', title: '图片库' },
      ],
    },
  ]
  return items.filter((item) => item.visible !== false)
})

const activeMenu = computed(() => {
  const { path } = route
  if (path.startsWith('/project')) return '/project'
  if (path.startsWith('/inspection')) return '/project'
  if (path.startsWith('/report/design') || path.startsWith('/report/create') || path.startsWith('/report/preview'))
    return '/report'
  if (path.startsWith('/approval')) return '/approval'
  if (path.startsWith('/user')) return '/user'
  if (path.startsWith('/system')) return '/system'
  return path
})

function handleSelect(index) {
  router.push(index)
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
    <!-- Logo -->
    <div class="sidebar-logo">
      <el-icon :size="24"><Monitor /></el-icon>
      <span v-show="!appStore.sidebarCollapsed" class="logo-text">SCJC NDT</span>
    </div>

    <!-- Menu -->
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      :collapse-transition="false"
      background-color="var(--color-bg-sidebar)"
      text-color="#94a3b8"
      active-text-color="#ffffff"
      class="sidebar-menu"
      @select="handleSelect"
    >
      <template v-for="item in menuItems" :key="item.index">
        <!-- Sub-menu -->
        <el-sub-menu v-if="item.children" :index="item.index">
          <template #title>
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.index"
            :index="child.index"
          >
            {{ child.title }}
          </el-menu-item>
        </el-sub-menu>

        <!-- Single menu item -->
        <el-menu-item v-else :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>

    <!-- Collapse toggle -->
    <div class="sidebar-toggle" @click="appStore.toggleSidebar()">
      <el-icon :size="18">
        <DArrowLeft v-if="!appStore.sidebarCollapsed" />
        <DArrowRight v-else />
      </el-icon>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--color-bg-sidebar);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal);
  z-index: 1000;
  overflow: hidden;
}

.sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

.sidebar-logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: var(--sidebar-width);
}

.sidebar-toggle {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  transition: color var(--transition-fast);
  flex-shrink: 0;
}

.sidebar-toggle:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.05);
}
</style>
