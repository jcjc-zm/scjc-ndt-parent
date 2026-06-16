<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { dashboardApi } from '@/api/dashboard'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const stats = ref({
  totalInspections: 0,
  qualifiedRate: 100,
  pendingCount: 0,
  activeProjects: 0,
  weeklyNew: 0,
  methodDistribution: [],
  weeklyWorkload: [],
})

// ECharts trend option (30-day inspection trend)
const trendOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
  },
  grid: { top: 20, right: 20, bottom: 30, left: 50 },
  xAxis: {
    type: 'category',
    data: Array.from({ length: 30 }, (_, i) => {
      const d = new Date()
      d.setDate(d.getDate() - 29 + i)
      return `${d.getMonth() + 1}/${d.getDate()}`
    }),
    axisLabel: { fontSize: 11, color: '#94a3b8' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { fontSize: 11, color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#f1f5f9' } },
  },
  series: [
    {
      name: '检测数量',
      type: 'bar',
      data: Array.from({ length: 30 }, () => Math.floor(Math.random() * 50) + 10),
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#10b981' },
            { offset: 1, color: '#059669' },
          ],
        },
        borderRadius: [4, 4, 0, 0],
      },
      barWidth: '60%',
    },
  ],
}))

// Method distribution chart option
const methodChartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)',
  },
  legend: {
    orient: 'vertical',
    left: 'left',
    top: 'center',
    textStyle: { fontSize: 12, color: '#64748b' },
  },
  series: [
    {
      type: 'pie',
      radius: ['55%', '80%'],
      center: ['60%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#fff',
        borderWidth: 2,
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' },
      },
      data: (stats.value.methodDistribution || []).map((m) => ({
        name: m.method,
        value: m.count,
      })),
      color: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#84cc16'],
    },
  ],
}))

// KPI card config
const kpiCards = computed(() => [
  {
    label: '检测总任务',
    value: stats.value.totalInspections || 0,
    color: '#3b82f6',
    bg: '#eff6ff',
    icon: 'Document',
  },
  {
    label: '合格率',
    value: (stats.value.qualifiedRate || 100).toFixed(1) + '%',
    color: '#10b981',
    bg: '#ecfdf5',
    icon: 'CircleCheck',
  },
  {
    label: '待处理任务',
    value: stats.value.pendingCount || 0,
    color: '#f59e0b',
    bg: '#fffbeb',
    icon: 'Clock',
  },
  {
    label: '本周新增',
    value: stats.value.weeklyNew || 0,
    color: '#8b5cf6',
    bg: '#f5f3ff',
    icon: 'Plus',
  },
  {
    label: '活跃项目',
    value: stats.value.activeProjects || 0,
    color: '#059669',
    bg: '#ecfdf5',
    icon: 'FolderOpened',
  },
])

async function loadData() {
  loading.value = true
  try {
    const res = await dashboardApi.overview()
    if (res?.data) stats.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <!-- Page header -->
    <div class="page-header">
      <div>
        <h2 class="page-title">驾驶舱</h2>
        <p class="page-subtitle">无损检测数据总览</p>
      </div>
      <el-button :icon="Refresh" @click="loadData">刷新数据</el-button>
    </div>

    <!-- KPI Cards -->
    <div class="kpi-grid">
      <div
        v-for="card in kpiCards"
        :key="card.label"
        class="kpi-card"
        :style="{ '--kpi-color': card.color, '--kpi-bg': card.bg }"
      >
        <div class="kpi-icon-box">
          <el-icon :size="24"><component :is="card.icon" /></el-icon>
        </div>
        <div class="kpi-body">
          <span class="kpi-value">{{ card.value }}</span>
          <span class="kpi-label">{{ card.label }}</span>
        </div>
      </div>
    </div>

    <!-- Charts row -->
    <el-row :gutter="16" class="charts-row">
      <!-- Trend chart -->
      <el-col :span="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span class="card-title">近30天检测趋势</span>
          </template>
          <v-chart :option="trendOption" style="height: 320px" autoresize />
        </el-card>
      </el-col>

      <!-- Method distribution -->
      <el-col :span="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span class="card-title">检测方法分布</span>
          </template>
          <div v-if="stats.methodDistribution?.length">
            <v-chart :option="methodChartOption" style="height: 280px" autoresize />
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Weekly workload + Quick actions -->
    <el-row :gutter="16" class="bottom-row">
      <el-col :span="18">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header-row">
              <span class="card-title">本周工作量统计</span>
              <span class="card-subtitle">按事业部 × 检测方法</span>
            </div>
          </template>
          <el-table
            :data="stats.weeklyWorkload || []"
            border
            stripe
            size="small"
            :empty-text="'暂无数据'"
          >
            <el-table-column prop="deptName" label="事业部" fixed width="130" />
            <el-table-column label="RT" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.rt }">{{ row.rt || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="UT" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.ut }">{{ row.ut || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="PT" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.pt }">{{ row.pt || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="MT" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.mt }">{{ row.mt || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="AUT" width="70" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.aut }">{{ row.aut || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="PA" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.pa }">{{ row.pa || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="TOFD" width="75" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.tofd }">{{ row.tofd || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="DR" width="65" align="center">
              <template #default="{ row }">
                <span :class="{ 'zero-count': !row.dr }">{{ row.dr || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="合计" width="75" align="center" fixed="right">
              <template #default="{ row }">
                <el-tag :type="row.total > 20 ? 'danger' : row.total > 10 ? 'warning' : 'primary'" size="small">
                  {{ row.total || 0 }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="never" class="chart-card quick-actions-card">
          <template #header>
            <span class="card-title">快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button
              v-if="userStore.canCreateProject"
              type="primary"
              :icon="Plus"
              size="large"
              class="action-btn"
              @click="router.push('/project/create')"
            >
              创建立项
            </el-button>
            <el-button
              :icon="Edit"
              size="large"
              class="action-btn"
              @click="router.push('/approval')"
            >
              签字审批
            </el-button>
            <el-button
              :icon="Document"
              size="large"
              class="action-btn"
              @click="router.push('/report')"
            >
              报告管理
            </el-button>
            <el-button
              v-if="userStore.canManageUsers"
              :icon="User"
              size="large"
              class="action-btn"
              @click="router.push('/user')"
            >
              用户管理
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Page header */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-6);
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

/* KPI cards grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: #fff;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
  cursor: default;
}

.kpi-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.kpi-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--kpi-bg, #f1f5f9);
  color: var(--kpi-color, #3b82f6);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.kpi-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.kpi-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

/* Chart cards */
.charts-row {
  margin-bottom: 16px;
}

.chart-card {
  border-radius: var(--radius-md);
  height: 100%;
}

.chart-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.card-header-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.card-subtitle {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.bottom-row {
  margin-bottom: 16px;
}

/* Table zero values */
.zero-count {
  color: var(--color-text-placeholder);
}

/* Quick actions */
.quick-actions-card {
  height: 100%;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  width: 100%;
  justify-content: flex-start;
}

/* Responsive */
@media (max-width: 1400px) {
  .kpi-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
