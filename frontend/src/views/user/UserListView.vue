<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { roleApi } from '@/api/role'
import { deptApi } from '@/api/dept'
import { projectApi } from '@/api/project'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const allRoles = ref([])
const allDepts = ref([])
const allProjects = ref([])

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
})

// Create / Edit dialog
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const editingUser = ref(null)
const submitting = ref(false)
const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  deptId: null,
  status: 1,
  roleIds: [],
  projectIds: [],
})

// Role assignment dialog
const roleDialogVisible = ref(false)
const roleDialogUser = ref(null)
const selectedRoleIds = ref([])
const selectedProjectIds = ref([])
const selectedBuId = ref(null)

// Computed role IDs
const companyAdminRoleId = computed(() => allRoles.value.find(r => r.roleCode === 'COMPANY_ADMIN')?.id)
const buAdminRoleId = computed(() => allRoles.value.find(r => r.roleCode === 'BU_ADMIN')?.id)
const projectAdminRoleId = computed(() => allRoles.value.find(r => r.roleCode === 'PROJECT_ADMIN')?.id)

// BU depts for BU_ADMIN selector
const buDepts = computed(() => allDepts.value.filter(d => d.deptType === 'BU'))

const statusMap = { 1: { label: '正常', type: 'success' }, 0: { label: '禁用', type: 'danger' } }

async function loadData() {
  loading.value = true
  try {
    const res = await userApi.list(query)
    if (res?.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function loadRefs() {
  try {
    const [rolesRes, deptsRes, projectsRes] = await Promise.all([
      roleApi.list(),
      deptApi.list(),
      projectApi.list({ size: 1000 }),
    ])
    if (rolesRes?.data) allRoles.value = rolesRes.data
    if (deptsRes?.data) allDepts.value = deptsRes.data
    if (projectsRes?.data) allProjects.value = projectsRes.data.records || []
  } catch { /* ignore */ }
}

// ─── Create / Edit ───
function openCreateDialog() {
  dialogTitle.value = '新增用户'
  editingUser.value = null
  Object.assign(form, {
    username: '', password: '', realName: '', phone: '', email: '',
    deptId: null, status: 1, roleIds: companyAdminRoleId.value ? [companyAdminRoleId.value] : [], projectIds: [],
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogTitle.value = `编辑用户`
  editingUser.value = row
  Object.assign(form, {
    username: row.username,
    password: '',
    realName: row.realName || '',
    phone: row.phone || '',
    email: row.email || '',
    deptId: row.deptId,
    status: row.status,
    roleIds: [],
    projectIds: [],
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.username.trim()) { ElMessage.warning('请输入用户名'); return }
  if (!editingUser.value && !form.password) { ElMessage.warning('请输入密码'); return }

  submitting.value = true
  try {
    const data = { ...form }
    if (editingUser.value && !data.password) delete data.password
    if (!editingUser.value) {
      await userApi.create(data)
      ElMessage.success('用户创建成功')
    } else {
      await userApi.update(editingUser.value.id, data)
      ElMessage.success('用户已更新')
    }
    dialogVisible.value = false
    loadData()
  } catch { /* error handled by interceptor */ }
  finally { submitting.value = false }
}

// ─── Status ───
async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await userApi.updateStatus(row.id, newStatus)
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadData()
  } catch { /* ignore */ }
}

// ─── Role Assignment ───
function openRoleDialog(row) {
  roleDialogUser.value = row
  // Map user's role codes (from backend) to role IDs for checkbox group
  const userRoleCodes = row.roles || []
  selectedRoleIds.value = allRoles.value
    .filter(r => userRoleCodes.includes(r.roleCode))
    .map(r => r.id)
  // Pre-populate project assignments
  selectedProjectIds.value = row.projectIds || []
  // Pre-populate BU if user's dept is a BU
  const dept = allDepts.value.find(d => d.id === row.deptId)
  selectedBuId.value = dept?.deptType === 'BU' ? dept.id : null
  roleDialogVisible.value = true
}

async function saveRoles() {
  if (!roleDialogUser.value) return
  try {
    const userId = roleDialogUser.value.id
    await userApi.assignRoles(userId, selectedRoleIds.value)

    // Resolve selected role codes
    const selectedRoleCodes = allRoles.value
      .filter(r => selectedRoleIds.value.includes(r.id))
      .map(r => r.roleCode)

    // Collect project IDs from PROJECT_ADMIN + BU_ADMIN selections
    let allProjectIds = []
    if (selectedRoleCodes.includes('PROJECT_ADMIN')) {
      allProjectIds.push(...selectedProjectIds.value)
    }
    if (selectedRoleCodes.includes('BU_ADMIN') && selectedBuId.value) {
      const buProjects = allProjects.value.filter(p => p.parentId === selectedBuId.value)
      allProjectIds.push(...buProjects.map(p => p.id))
      // Also update the user's deptId to the selected BU
      await userApi.update(userId, { username: roleDialogUser.value.username, deptId: selectedBuId.value })
    }
    // Only touch project assignments if a relevant role is selected
    if (selectedRoleCodes.includes('PROJECT_ADMIN') || selectedRoleCodes.includes('BU_ADMIN')) {
      await userApi.assignProjects(userId, [...new Set(allProjectIds)])
    }

    ElMessage.success('角色已分配')
    roleDialogVisible.value = false
    loadData()
  } catch { /* ignore */ }
}

// ─── Delete ───
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '删除确认', { type: 'warning' })
    await userApi.updateStatus(row.id, 0) // Soft delete via disable
    ElMessage.success('已禁用')
    loadData()
  } catch { /* cancelled */ }
}

function handleSearch() { query.page = 1; loadData() }

onMounted(() => {
  loadRefs()
  loadData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-subtitle">逐级创建与管理系统用户</p>
      </div>
      <el-button v-if="userStore.canManageUsers" type="primary" :icon="Plus" @click="openCreateDialog">
        新增用户
      </el-button>
    </div>

    <!-- Filter -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input v-model="query.keyword" placeholder="搜索用户名..." :prefix-icon="Search" clearable style="width: 220px" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-button :icon="Search" @click="handleSearch">查询</el-button>
        <span class="total-count">共 {{ total }} 人</span>
      </div>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe :empty-text="'暂无数据'">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="所属项目" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.projectNames?.join('、') || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small" effect="light">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            {{ row.createTime?.replace('T', ' ').substring(0, 19) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="EditPen" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" :icon="Setting" size="small" @click="openRoleDialog(row)">角色</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" :icon="SwitchButton" size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > query.size">
        <el-pagination
          v-model:current-page="query.page" v-model:page-size="query.size"
          :total="total" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next" background
          @current-change="loadData" @size-change="loadData"
        />
      </div>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="540px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="登录用户名" :disabled="!!editingUser" />
        </el-form-item>
        <el-form-item label="密码" :required="!editingUser">
          <el-input v-model="form.password" type="password" placeholder="留空则不修改" show-password />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱地址" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="form.deptId" placeholder="选择部门" style="width: 100%" clearable>
            <el-option v-for="d in allDepts" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Role Assignment Dialog -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="480px" destroy-on-close>
      <el-checkbox-group v-model="selectedRoleIds">
        <div v-for="role in allRoles" :key="role.id" style="margin-bottom: 12px">
          <el-checkbox :label="role.id" :value="role.id">
            <strong>{{ role.roleName }}</strong>
            <span class="text-secondary" style="font-size: 12px; margin-left: 4px">{{ role.roleCode }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>

      <!-- PROJECT_ADMIN: assign projects -->
      <el-divider v-if="selectedRoleIds.includes(projectAdminRoleId)" />
      <div v-if="selectedRoleIds.includes(projectAdminRoleId)" style="margin-bottom: 16px">
        <div style="margin-bottom: 6px; font-size: 14px; color: var(--el-text-color-regular)">所属项目</div>
        <el-select v-model="selectedProjectIds" multiple placeholder="选择项目" style="width: 100%" clearable filterable>
          <el-option v-for="p in allProjects" :key="p.id" :label="p.projectName" :value="p.id">
            <span>{{ p.projectName }}</span>
            <span style="float: right; color: var(--color-text-secondary); font-size: 12px">{{ p.projectCode }}</span>
          </el-option>
        </el-select>
      </div>

      <!-- BU_ADMIN: assign BU -->
      <el-divider v-if="selectedRoleIds.includes(buAdminRoleId)" />
      <div v-if="selectedRoleIds.includes(buAdminRoleId)" style="margin-bottom: 16px">
        <div style="margin-bottom: 6px; font-size: 14px; color: var(--el-text-color-regular)">所属事业部</div>
        <el-select v-model="selectedBuId" placeholder="选择事业部" style="width: 100%" clearable>
          <el-option v-for="bu in buDepts" :key="bu.id" :label="bu.deptName" :value="bu.id" />
        </el-select>
      </div>

      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: var(--space-6); }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-secondary); margin: 0; }

.filter-card { margin-bottom: 16px; border-radius: var(--radius-md); }
.filter-row { display: flex; align-items: center; gap: 12px; }
.total-count { font-size: 13px; color: var(--color-text-secondary); margin-left: auto; }
.table-card { border-radius: var(--radius-md); }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
