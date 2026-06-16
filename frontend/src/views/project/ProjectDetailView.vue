<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { projectApi } from '@/api/project'

const route = useRoute()
const router = useRouter()

const projectId = ref(route.params.id)
const loading = ref(false)
const project = ref(null)
const activeTab = ref('basic')

const statusMap = {
  PENDING: { label: '待启动', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
}

const projectTypeMap = {
  COMPANY_DIRECT: '公司直属项目',
  BU_SUB: '事业部下属项目',
}

async function loadData() {
  loading.value = true
  try {
    const res = await projectApi.getById(projectId.value)
    if (res?.data) project.value = res.data
  } catch {
    ElMessage.error('加载项目信息失败')
    router.push('/project')
  } finally {
    loading.value = false
  }
}

function goToInspection() {
  router.push(`/inspection/${projectId.value}/entry`)
}

function goBack() {
  router.push('/project')
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- Header with back -->
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" link @click="goBack">返回列表</el-button>
        <h2 class="page-title">{{ project?.projectName || '项目详情' }}</h2>
        <el-tag
          v-if="project"
          :type="statusMap[project.status]?.type || 'info'"
          effect="light"
          class="status-tag"
        >
          {{ statusMap[project.status]?.label || project.status }}
        </el-tag>
      </div>
      <el-button
        v-if="project"
        type="primary"
        :icon="EditPen"
        @click="goToInspection"
      >
        检测数据录入
      </el-button>
    </div>

    <!-- Content -->
    <template v-if="project">
      <el-row :gutter="16">
        <el-col :span="16">
          <!-- Basic info card -->
          <el-card shadow="never" class="detail-card">
            <template #header>
              <span class="card-title">基本信息</span>
            </template>
            <el-descriptions :column="2" border size="default">
              <el-descriptions-item label="项目编号">
                <span class="text-mono">{{ project.projectCode }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="项目名称">{{ project.projectName }}</el-descriptions-item>
              <el-descriptions-item label="项目类型">
                <el-tag :type="project.projectType === 'COMPANY_DIRECT' ? 'primary' : 'success'" size="small">
                  {{ projectTypeMap[project.projectType] || project.projectType }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="所属事业部">
                {{ project.buName || '公司直属' }}
              </el-descriptions-item>
              <el-descriptions-item label="施工单位">{{ project.constructionUnit || '-' }}</el-descriptions-item>
              <el-descriptions-item label="单位工程名称">{{ project.unitProjectName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ project.createTime?.replace('T', ' ').substring(0, 19) || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="更新时间">
                {{ project.updateTime?.replace('T', ' ').substring(0, 19) || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <el-col :span="8">
          <!-- Quick actions -->
          <el-card shadow="never" class="detail-card">
            <template #header>
              <span class="card-title">操作</span>
            </template>
            <div class="action-list">
              <el-button
                type="primary"
                :icon="EditPen"
                size="large"
                class="action-btn"
                @click="goToInspection"
              >
                检测数据录入
              </el-button>
              <el-button
                :icon="Document"
                size="large"
                class="action-btn"
                @click="router.push(`/report?projectId=${project.id}`)"
              >
                查看报告
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.status-tag {
  margin-left: 4px;
}

.detail-card {
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  width: 100%;
  justify-content: flex-start;
}

.text-mono {
  font-family: var(--font-mono);
  font-size: 13px;
}
</style>
