<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened, Document } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { projectApi } from '@/api/project'

const router = useRouter()
const userStore = useUserStore()

// --- Tree state ---
const treeData = ref([])
const treeLoading = ref(false)
const treeFilterText = ref('')
const currentNodeKey = ref('')
const defaultExpandedKeys = ref([])
const treeRef = ref(null)

// --- Table state ---
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selectedNodeType = ref('')
const selectedBuName = ref('')

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
})

// Status tag mapping
const statusMap = {
  PENDING: { label: '待启动', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
}

// Load tree
async function loadTree() {
  treeLoading.value = true
  try {
    const res = await projectApi.getTree()
    if (res?.data) {
      treeData.value = res.data
      if (res.data.length > 0) {
        defaultExpandedKeys.value = [res.data[0].id]
        currentNodeKey.value = res.data[0].id
        selectedNodeType.value = 'COMPANY'
      }
    }
  } catch {
    // handled by interceptor
  } finally {
    treeLoading.value = false
  }
}

// Tree node click
function handleNodeClick(data) {
  if (data.type === 'PROJECT') {
    router.push(`/project/${data.projectId}/detail`)
    return
  }
  currentNodeKey.value = data.id
  selectedNodeType.value = data.type
  if (data.type === 'COMPANY') {
    selectedBuName.value = ''
  } else if (data.type === 'BU') {
    selectedBuName.value = data.label
  }
  query.page = 1
  loadTable()
}

// Filter tree by text
watch(treeFilterText, (val) => {
  treeRef.value?.filter(val)
})

function filterNode(value, data) {
  if (!value) return true
  return data.label.includes(value)
}

// Load table
async function loadTable() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
    }
    if (selectedNodeType.value === 'BU') {
      params.buName = selectedBuName.value
    }
    const res = await projectApi.list(params)
    if (res?.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

// Search
function handleSearch() {
  query.page = 1
  loadTable()
}

function handleReset() {
  query.keyword = ''
  query.page = 1
  loadTable()
}

// Pagination
function handlePageChange(page) {
  query.page = page
  loadTable()
}

function handleSizeChange(size) {
  query.size = size
  query.page = 1
  loadTable()
}

// Actions
function goToCreate() {
  router.push('/project/create')
}

function goToDetail(row) {
  router.push(`/project/${row.id}/detail`)
}

function goToInspection(row) {
  router.push(`/inspection/${row.id}/entry`)
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 'IN_PROGRESS' ? 'COMPLETED' : 'IN_PROGRESS'
  const action = newStatus === 'COMPLETED' ? '完成' : '启动'
  try {
    await ElMessageBox.confirm(
      `确认将项目「${row.projectName}」标记为${action}？`,
      '状态变更',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    await projectApi.updateStatus(row.id, newStatus)
    ElMessage.success(`项目已${action}`)
    loadTable()
    loadTree()
  } catch {
    // Cancelled
  }
}

onMounted(() => {
  loadTree().then(() => loadTable())
})
</script>

<template>
  <div class="page-container">
    <!-- Page header -->
    <div class="page-header">
      <div>
        <h2 class="page-title">数据管理</h2>
        <p class="page-subtitle">按组织架构浏览和管理项目数据</p>
      </div>
      <el-button
        v-if="userStore.canCreateProject"
        type="primary"
        :icon="Plus"
        @click="goToCreate"
      >
        创建立项
      </el-button>
    </div>

    <!-- Content: tree + table -->
    <div class="content-wrapper">
      <!-- Left: Tree -->
      <div class="tree-panel">
        <div class="tree-header">
          <span class="tree-title">组织架构</span>
        </div>
        <el-input
          v-model="treeFilterText"
          placeholder="搜索节点..."
          :prefix-icon="Search"
          clearable
          size="small"
          class="tree-search"
        />
        <div class="tree-body" v-loading="treeLoading">
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="{ children: 'children', label: 'label' }"
            node-key="id"
            :default-expanded-keys="defaultExpandedKeys"
            :current-node-key="currentNodeKey"
            :filter-node-method="filterNode"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node" :class="`tree-node--${data.type.toLowerCase()}`">
                <el-icon v-if="data.type === 'COMPANY' || data.type === 'BU'" :size="16">
                  <FolderOpened />
                </el-icon>
                <el-icon v-else :size="16"><Document /></el-icon>
                <span class="tree-node-label">{{ data.label }}</span>
                <span v-if="data.type === 'PROJECT' && data.status" class="tree-node-status" :class="`status-${data.status.toLowerCase()}`" />
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- Right: Table -->
      <div class="table-panel">
        <!-- Filter bar -->
        <div class="filter-bar">
          <div class="filter-left">
            <el-input
              v-model="query.keyword"
              placeholder="搜索项目编号、名称..."
              :prefix-icon="Search"
              clearable
              style="width: 220px"
              size="default"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </div>
          <div class="filter-right">
            <span class="total-count">共 {{ total }} 个项目</span>
          </div>
        </div>

        <!-- Table -->
        <el-table
          :data="tableData"
          v-loading="loading"
          border
          stripe
          style="width: 100%"
          :empty-text="'请从左侧选择组织节点查看项目'"
        >
          <el-table-column prop="projectCode" label="项目编号" width="140" show-overflow-tooltip />
          <el-table-column prop="projectName" label="项目名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="unitProjectName" label="单位工程名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="buName" label="所属事业部" width="120" align="center">
            <template #default="{ row }">
              {{ row.buName || '公司直属' }}
            </template>
          </el-table-column>
          <el-table-column prop="constructionUnit" label="施工单位" width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusMap[row.status]?.type || 'info'" effect="light" size="small">
                {{ statusMap[row.status]?.label || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" align="center">
            <template #default="{ row }">
              {{ row.createTime?.replace('T', ' ').substring(0, 19) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click="goToDetail(row)">详情</el-button>
              <el-button link type="success" :icon="EditPen" @click="goToInspection(row)">录入</el-button>
              <el-button
                v-if="row.status !== 'COMPLETED'"
                link
                type="warning"
                :icon="SwitchButton"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 'IN_PROGRESS' ? '完成' : '启动' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination-wrap" v-if="total > query.size">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  flex-shrink: 0;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* Content wrapper */
.content-wrapper {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

/* Left tree panel */
.tree-panel {
  width: 260px;
  flex-shrink: 0;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tree-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
}

.tree-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.tree-search {
  margin: 8px 12px;
}

.tree-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

/* Tree node styling */
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.tree-node-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-left: auto;
}

.status-pending { background: #909399; }
.status-in_progress { background: #67c23a; }
.status-completed { background: #409eff; }

.tree-node--project {
  padding-left: 4px;
}

/* Right table panel */
.table-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  padding: 16px;
  overflow: hidden;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-shrink: 0;
  gap: 12px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.total-count {
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  flex-shrink: 0;
}

/* el-table inside table-panel should fill remaining space */
.table-panel :deep(.el-table) {
  flex: 1;
}
</style>
