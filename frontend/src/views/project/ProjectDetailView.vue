<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { projectApi } from '@/api/project'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const projectId = ref(route.params.id)
const loading = ref(false)
const submitting = ref(false)
const editing = ref(false)
const project = ref(null)
const activeTab = ref('basic')
const formRef = ref(null)

const statusMap = {
  PENDING: { label: '待启动', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
}

const projectTypeMap = {
  COMPANY_DIRECT: '公司直属项目',
  BU_SUB: '事业部下属项目',
}

const editForm = reactive({
  projectName: '',
  unitProjectName: '',
  constructionUnit: '',
  designUnit: '',
  supervisionUnit: '',
  contractNo: '',
  contractAmount: null,
  projectManager: '',
  projectLocation: '',
  projectDescription: '',
})

const rules = {
  projectName: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { min: 2, max: 100, message: '项目名称长度为 2-100 个字符', trigger: 'blur' },
  ],
  constructionUnit: [{ required: true, message: '请输入施工单位', trigger: 'blur' }],
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

function startEdit() {
  if (!project.value) return
  editForm.projectName = project.value.projectName || ''
  editForm.unitProjectName = project.value.unitProjectName || ''
  editForm.constructionUnit = project.value.constructionUnit || ''
  editForm.designUnit = project.value.designUnit || ''
  editForm.supervisionUnit = project.value.supervisionUnit || ''
  editForm.contractNo = project.value.contractNo || ''
  editForm.contractAmount = project.value.contractAmount ?? null
  editForm.projectManager = project.value.projectManager || ''
  editForm.projectLocation = project.value.projectLocation || ''
  editForm.projectDescription = project.value.projectDescription || ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await projectApi.update(projectId.value, editForm)
      ElMessage.success('项目信息已更新')
      editing.value = false
      await loadData()
    } catch {
      // Error handled by interceptor
    } finally {
      submitting.value = false
    }
  })
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
    <!-- Header -->
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
      <div class="header-right">
        <el-button
          v-if="userStore.canEditProject && !editing"
          :icon="Edit"
          type="primary"
          @click="startEdit"
        >
          编辑项目
        </el-button>
        <el-button
          v-if="project"
          type="primary"
          :icon="EditPen"
          @click="goToInspection"
        >
          检测数据录入
        </el-button>
      </div>
    </div>

    <!-- Content -->
    <template v-if="project">
      <el-row :gutter="16">
        <el-col :span="16">
          <!-- Edit mode -->
          <el-card v-if="editing" shadow="never" class="detail-card">
            <template #header>
              <span class="card-title">编辑项目信息</span>
            </template>
            <el-form
              ref="formRef"
              :model="editForm"
              :rules="rules"
              label-width="120px"
              label-position="right"
              size="default"
            >
              <!-- Read-only identity fields -->
              <el-form-item label="项目编号">
                <el-input :model-value="project.projectCode" disabled style="width: 300px" />
              </el-form-item>
              <el-form-item label="项目类型">
                <el-input
                  :model-value="projectTypeMap[project.projectType] || project.projectType"
                  disabled
                  style="width: 300px"
                />
              </el-form-item>
              <el-form-item label="所属事业部">
                <el-input :model-value="project.buName || '公司直属'" disabled style="width: 300px" />
              </el-form-item>

              <el-divider content-position="left">可编辑信息</el-divider>

              <el-form-item label="项目名称" prop="projectName">
                <el-input v-model="editForm.projectName" placeholder="请输入项目名称" style="width: 400px" clearable />
              </el-form-item>
              <el-form-item label="单位工程名称" prop="unitProjectName">
                <el-input v-model="editForm.unitProjectName" placeholder="请输入单位工程名称" style="width: 400px" clearable />
              </el-form-item>
              <el-form-item label="施工单位" prop="constructionUnit">
                <el-input v-model="editForm.constructionUnit" placeholder="请输入施工单位" style="width: 400px" clearable />
              </el-form-item>
              <el-form-item label="设计单位" prop="designUnit">
                <el-input v-model="editForm.designUnit" placeholder="请输入设计单位" style="width: 400px" clearable />
              </el-form-item>
              <el-form-item label="监理单位" prop="supervisionUnit">
                <el-input v-model="editForm.supervisionUnit" placeholder="请输入监理单位" style="width: 400px" clearable />
              </el-form-item>
              <el-form-item label="合同编号" prop="contractNo">
                <el-input v-model="editForm.contractNo" placeholder="请输入合同编号" style="width: 400px" clearable />
              </el-form-item>
              <el-form-item label="合同金额" prop="contractAmount">
                <el-input-number
                  v-model="editForm.contractAmount"
                  :precision="2"
                  :min="0"
                  :controls="true"
                  placeholder="请输入合同金额"
                  style="width: 300px"
                />
              </el-form-item>
              <el-form-item label="项目经理" prop="projectManager">
                <el-input v-model="editForm.projectManager" placeholder="请输入项目经理" style="width: 300px" clearable />
              </el-form-item>
              <el-form-item label="工程地点" prop="projectLocation">
                <el-input v-model="editForm.projectLocation" placeholder="请输入工程地点" style="width: 400px" clearable />
              </el-form-item>
              <el-form-item label="项目描述" prop="projectDescription">
                <el-input
                  v-model="editForm.projectDescription"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入项目描述"
                  style="width: 500px"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="submitting" :icon="Check" @click="handleSave">
                  {{ submitting ? '保存中...' : '保存' }}
                </el-button>
                <el-button :icon="Close" @click="cancelEdit">取消</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- View mode -->
          <el-card v-else shadow="never" class="detail-card">
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
              <el-descriptions-item label="设计单位">{{ project.designUnit || '-' }}</el-descriptions-item>
              <el-descriptions-item label="监理单位">{{ project.supervisionUnit || '-' }}</el-descriptions-item>
              <el-descriptions-item label="合同编号">{{ project.contractNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="合同金额">
                {{ project.contractAmount != null ? project.contractAmount : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="项目经理">{{ project.projectManager || '-' }}</el-descriptions-item>
              <el-descriptions-item label="工程地点">{{ project.projectLocation || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目描述" :span="2">
                {{ project.projectDescription || '-' }}
              </el-descriptions-item>
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

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-2);
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
