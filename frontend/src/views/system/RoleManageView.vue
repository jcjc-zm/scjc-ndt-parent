<script setup>
import { ref, onMounted } from 'vue'
import { roleApi } from '@/api/role'

const loading = ref(false)
const roles = ref([])

const roleDescriptions = {
  SYSTEM_ADMIN: { desc: '全局最高权限，管理系统所有功能', color: '#ef4444' },
  COMPANY_ADMIN: { desc: '公司级管理，可创建事业部管理员', color: '#f97316' },
  BU_ADMIN: { desc: '事业部管理，可立项 + 创建项目管理员', color: '#3b82f6' },
  PROJECT_ADMIN: { desc: '项目管理，仅操作指定项目数据', color: '#10b981' },
  TECHNICAL_LEADER: { desc: '技术负责人，报告第一道签字', color: '#8b5cf6' },
  PROJECT_MANAGER: { desc: '项目经理，报告最终签字', color: '#06b6d4' },
}

async function loadData() {
  loading.value = true
  try {
    const res = await roleApi.list()
    if (res?.data) roles.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">角色管理</h2>
        <p class="page-subtitle">系统预置6种角色，不可增删</p>
      </div>
    </div>

    <el-card shadow="never" class="content-card" v-loading="loading">
      <el-table :data="roles" border stripe :empty-text="'暂无数据'">
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <div class="role-name-cell">
              <span
                class="role-dot"
                :style="{ background: roleDescriptions[row.roleCode]?.color || '#94a3b8' }"
              />
              <span>{{ row.roleName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="roleCode" label="角色编码" width="200">
          <template #default="{ row }">
            <code class="text-mono">{{ row.roleCode }}</code>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="300">
          <template #default="{ row }">
            {{ roleDescriptions[row.roleCode]?.desc || row.description || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="创建权限" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.roleCode === 'SYSTEM_ADMIN'" type="danger" size="small">全部</el-tag>
            <el-tag v-else-if="row.roleCode === 'COMPANY_ADMIN'" type="warning" size="small">事业部级</el-tag>
            <el-tag v-else-if="row.roleCode === 'BU_ADMIN'" type="primary" size="small">项目级</el-tag>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 16px"
        title="系统角色说明"
        description="6种角色在系统初始化时写入数据库，不可删除。创建用户时按层级赋权：系统管理员 → 公司管理员 → 事业部管理员 → 项目管理员/技术负责人/项目经理。签字角色（技术负责人、项目经理）由事业部管理员或系统管理员分配。"
      />
    </el-card>
  </div>
</template>

<style scoped>
.page-container { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.page-header { margin-bottom: var(--space-6); }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-secondary); margin: 0; }
.content-card { border-radius: var(--radius-md); }
.role-name-cell { display: flex; align-items: center; gap: 8px; }
.role-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.text-mono { font-family: var(--font-mono); font-size: 12px; background: var(--color-bg); padding: 2px 6px; border-radius: 3px; }
</style>
