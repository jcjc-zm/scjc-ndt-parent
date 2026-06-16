<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reportApi } from '@/api/report'
import { templateApi } from '@/api/template'
import { projectApi } from '@/api/project'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const projects = ref([])
const templates = ref([])

const query = reactive({
  page: 1,
  size: 20,
  projectId: '',
  status: '',
})

const statusMap = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING_SIGN: { label: '待签字', type: 'warning' },
  SIGNED: { label: '已签字', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
}

async function loadProjects() {
  try {
    const res = await projectApi.list({ size: 1000 })
    if (res?.data) projects.value = res.data.records || []
  } catch { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.projectId) delete params.projectId
    if (!params.status) delete params.status
    const res = await reportApi.list(params)
    if (res?.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除报告「${row.reportNo}」？`, '删除确认', {
      type: 'warning',
    })
    await reportApi.remove(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancelled */ }
}

function goToCreate() {
  router.push('/report/create/0')
}

function goToPreview(row) {
  router.push(`/report/preview/${row.id}`)
}

function goToDesign() {
  router.push('/report/design/0')
}

function handleSearch() {
  query.page = 1
  loadData()
}

onMounted(() => {
  loadProjects()
  loadData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">报告管理</h2>
        <p class="page-subtitle">生成、预览和管理检测报告</p>
      </div>
      <div class="header-actions">
        <el-button :icon="EditPen" @click="goToDesign">设计模板</el-button>
        <el-button type="primary" :icon="Plus" @click="goToCreate">生成报告</el-button>
      </div>
    </div>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-select v-model="query.projectId" placeholder="选择项目" clearable style="width: 200px" @change="handleSearch">
          <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="报告状态" clearable style="width: 140px" @change="handleSearch">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="待签字" value="PENDING_SIGN" />
          <el-option label="已签字" value="SIGNED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button :icon="Search" @click="handleSearch">查询</el-button>
        <span class="total-count">共 {{ total }} 份报告</span>
      </div>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe :empty-text="'暂无报告'">
        <el-table-column prop="reportNo" label="报告编号" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-mono">{{ row.reportNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
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
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="goToPreview(row)">预览</el-button>
            <el-button link type="success" :icon="Download" @click="goToPreview(row)">PDF</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > query.size">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-container { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: var(--space-6); }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-secondary); margin: 0; }
.header-actions { display: flex; gap: 8px; }

.filter-card { margin-bottom: 16px; border-radius: var(--radius-md); }
.filter-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.total-count { font-size: 13px; color: var(--color-text-secondary); margin-left: auto; }
.table-card { border-radius: var(--radius-md); }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.text-mono { font-family: var(--font-mono); font-size: 13px; }
</style>
