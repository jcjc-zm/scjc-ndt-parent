<script setup>
import Sidebar from './Sidebar.vue'
import HeaderBar from './HeaderBar.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<template>
  <div class="main-layout">
    <Sidebar />
    <div class="main-container" :class="{ collapsed: appStore.sidebarCollapsed }">
      <HeaderBar />
      <main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: var(--sidebar-width);
  transition: margin-left var(--transition-normal);
  min-width: 0;
}

.main-container.collapsed {
  margin-left: var(--sidebar-collapsed-width);
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
  background: var(--color-bg);
}
</style>
