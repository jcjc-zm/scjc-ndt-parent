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
    <!-- Header breadcrumb -->
    <div class="breadcrumb-row">
      <el-button :icon="ArrowLeft" link class="back-link" @click="goBack">
        返回列表
      </el-button>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-current">项目详情</span>
    </div>

    <!-- Title row -->
    <div class="title-row">
      <div class="title-left">
        <h2 class="page-title">{{ project?.projectName || '项目详情' }}</h2>
        <el-tag
          v-if="project"
          :type="statusMap[project.status]?.type || 'info'"
          effect="plain"
          size="default"
          class="status-tag"
        >
          <span class="status-dot" :class="`status-dot--${project.status?.toLowerCase()}`" />
          {{ statusMap[project.status]?.label || project.status }}
        </el-tag>
      </div>
      <div class="title-actions">
        <el-button
          v-if="userStore.canEditProject && !editing"
          type="primary"
          @click="startEdit"
        >
          <el-icon><Edit /></el-icon>
          编辑项目
        </el-button>
        <el-button
          v-if="project"
          type="success"
          @click="goToInspection"
        >
          <el-icon><EditPen /></el-icon>
          检测数据录入
        </el-button>
      </div>
    </div>

    <!-- Content -->
    <template v-if="project">
      <el-row :gutter="20">
        <!-- Main column -->
        <el-col :span="17">
          <!-- Edit mode -->
          <div v-if="editing" class="section-card">
            <div class="section-header">
              <el-icon :size="18"><Edit /></el-icon>
              <span class="section-title">编辑项目信息</span>
            </div>
            <div class="section-body">
              <el-form
                ref="formRef"
                :model="editForm"
                :rules="rules"
                label-width="120px"
                label-position="right"
                size="default"
              >
                <!-- Read-only identity fields -->
                <div class="form-section-label">项目标识（不可编辑）</div>
                <el-form-item label="项目编号">
                  <el-input :model-value="project.projectCode" disabled class="readonly-input" />
                </el-form-item>
                <el-form-item label="项目类型">
                  <el-input
                    :model-value="projectTypeMap[project.projectType] || project.projectType"
                    disabled
                    class="readonly-input"
                  />
                </el-form-item>
                <el-form-item label="所属事业部">
                  <el-input :model-value="project.buName || '公司直属'" disabled class="readonly-input" />
                </el-form-item>

                <div class="form-section-label">可编辑信息</div>

                <el-form-item label="项目名称" prop="projectName">
                  <el-input v-model="editForm.projectName" placeholder="请输入项目名称" clearable class="form-input" />
                </el-form-item>
                <el-form-item label="单位工程名称" prop="unitProjectName">
                  <el-input v-model="editForm.unitProjectName" placeholder="请输入单位工程名称" clearable class="form-input" />
                </el-form-item>
                <el-form-item label="施工单位" prop="constructionUnit">
                  <el-input v-model="editForm.constructionUnit" placeholder="请输入施工单位" clearable class="form-input" />
                </el-form-item>
                <el-form-item label="设计单位" prop="designUnit">
                  <el-input v-model="editForm.designUnit" placeholder="请输入设计单位" clearable class="form-input" />
                </el-form-item>
                <el-form-item label="监理单位" prop="supervisionUnit">
                  <el-input v-model="editForm.supervisionUnit" placeholder="请输入监理单位" clearable class="form-input" />
                </el-form-item>
                <el-form-item label="合同编号" prop="contractNo">
                  <el-input v-model="editForm.contractNo" placeholder="请输入合同编号" clearable class="form-input" />
                </el-form-item>
                <el-form-item label="合同金额" prop="contractAmount">
                  <el-input-number
                    v-model="editForm.contractAmount"
                    :precision="2"
                    :min="0"
                    :controls="true"
                    placeholder="请输入合同金额"
                    class="form-input-number"
                  />
                </el-form-item>
                <el-form-item label="项目经理" prop="projectManager">
                  <el-input v-model="editForm.projectManager" placeholder="请输入项目经理" clearable class="form-input-sm" />
                </el-form-item>
                <el-form-item label="工程地点" prop="projectLocation">
                  <el-input v-model="editForm.projectLocation" placeholder="请输入工程地点" clearable class="form-input" />
                </el-form-item>
                <el-form-item label="项目描述" prop="projectDescription">
                  <el-input
                    v-model="editForm.projectDescription"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入项目描述"
                    class="form-textarea"
                    maxlength="500"
                    show-word-limit
                  />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" :loading="submitting" @click="handleSave">
                    <el-icon><Check /></el-icon>
                    {{ submitting ? '保存中...' : '保存' }}
                  </el-button>
                  <el-button @click="cancelEdit">
                    <el-icon><Close /></el-icon>
                    取消
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <!-- View mode -->
          <div v-else class="section-card">
            <div class="section-header">
              <el-icon :size="18"><InfoFilled /></el-icon>
              <span class="section-title">基本信息</span>
            </div>
            <div class="section-body">
              <el-descriptions :column="2" border size="default" class="info-descriptions">
                <el-descriptions-item label="项目编号" label-class-name="desc-label">
                  <span class="text-mono text-primary">{{ project.projectCode }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="项目名称" label-class-name="desc-label">
                  <span class="text-strong">{{ project.projectName }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="项目类型" label-class-name="desc-label">
                  <el-tag :type="project.projectType === 'COMPANY_DIRECT' ? 'primary' : 'success'" size="small" effect="plain">
                    {{ projectTypeMap[project.projectType] || project.projectType }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="所属事业部" label-class-name="desc-label">
                  {{ project.buName || '公司直属' }}
                </el-descriptions-item>
                <el-descriptions-item label="施工单位" label-class-name="desc-label">{{ project.constructionUnit || '-' }}</el-descriptions-item>
                <el-descriptions-item label="单位工程名称" label-class-name="desc-label">{{ project.unitProjectName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="设计单位" label-class-name="desc-label">{{ project.designUnit || '-' }}</el-descriptions-item>
                <el-descriptions-item label="监理单位" label-class-name="desc-label">{{ project.supervisionUnit || '-' }}</el-descriptions-item>
                <el-descriptions-item label="合同编号" label-class-name="desc-label">{{ project.contractNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="合同金额" label-class-name="desc-label">
                  {{ project.contractAmount != null ? project.contractAmount.toLocaleString() : '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="项目经理" label-class-name="desc-label">{{ project.projectManager || '-' }}</el-descriptions-item>
                <el-descriptions-item label="工程地点" label-class-name="desc-label">{{ project.projectLocation || '-' }}</el-descriptions-item>
                <el-descriptions-item label="项目描述" :span="2" label-class-name="desc-label">
                  {{ project.projectDescription || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="创建时间" label-class-name="desc-label">
                  <span class="text-mono text-sm">{{ project.createTime?.replace('T', ' ').substring(0, 19) || '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="更新时间" label-class-name="desc-label">
                  <span class="text-mono text-sm">{{ project.updateTime?.replace('T', ' ').substring(0, 19) || '-' }}</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </el-col>

        <!-- Sidebar column -->
        <el-col :span="7">
          <div class="section-card sidebar-card">
            <div class="section-header">
              <el-icon :size="18"><Operation /></el-icon>
              <span class="section-title">快捷操作</span>
            </div>
            <div class="section-body">
              <div class="action-list">
                <button class="action-card action-card--primary" @click="goToInspection">
                  <div class="action-card-icon">
                    <el-icon :size="22"><EditPen /></el-icon>
                  </div>
                  <div class="action-card-body">
                    <div class="action-card-title">检测数据录入</div>
                    <div class="action-card-desc">为该项目的检测数据进行录入管理</div>
                  </div>
                  <el-icon :size="16" class="action-card-arrow"><ArrowRight /></el-icon>
                </button>

                <button class="action-card action-card--default" @click="router.push(`/report?projectId=${project.id}`)">
                  <div class="action-card-icon">
                    <el-icon :size="22"><Document /></el-icon>
                  </div>
                  <div class="action-card-body">
                    <div class="action-card-title">查看报告</div>
                    <div class="action-card-desc">浏览和导出该项目相关的检测报告</div>
                  </div>
                  <el-icon :size="16" class="action-card-arrow"><ArrowRight /></el-icon>
                </button>

                <button class="action-card action-card--default" @click="router.push(`/process-card?projectId=${project.id}`)">
                  <div class="action-card-icon">
                    <el-icon :size="22"><Notebook /></el-icon>
                  </div>
                  <div class="action-card-body">
                    <div class="action-card-title">工艺卡</div>
                    <div class="action-card-desc">查看工艺卡及技术规范</div>
                  </div>
                  <el-icon :size="16" class="action-card-arrow"><ArrowRight /></el-icon>
                </button>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<style scoped>
/* ── Page container ── */
.page-container {
  animation: fadeSlideIn 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 1200px;
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── Breadcrumb ── */
.breadcrumb-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.back-link {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  padding: 0;
}

.back-link:hover {
  color: var(--color-primary);
}

.breadcrumb-sep {
  color: var(--color-text-placeholder);
  font-size: 13px;
}

.breadcrumb-current {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* ── Title row ── */
.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);
}

.title-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
  letter-spacing: -0.3px;
}

.status-tag {
  font-weight: 600;
  padding: 4px 12px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-dot--pending { background: #94a3b8; }
.status-dot--in_progress { background: #3b82f6; }
.status-dot--completed { background: #10b981; }

.title-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

/* ── Section cards ── */
.section-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-5);
  overflow: hidden;
  transition: box-shadow var(--transition-fast);
}

.section-card:hover {
  box-shadow: var(--shadow-sm);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-5);
  background: var(--color-bg-hover);
  border-bottom: 1px solid var(--color-border-light);
  color: var(--color-text-primary);
}

.section-title {
  font-size: 15px;
  font-weight: 600;
}

.section-body {
  padding: var(--space-5);
}

/* ── Form styling ── */
.form-section-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: var(--space-4) 0 var(--space-3);
  padding-bottom: var(--space-2);
  border-bottom: 1px dashed var(--color-border-light);
}

.form-section-label:first-child {
  margin-top: 0;
}

.readonly-input {
  width: 320px;
}

.form-input {
  width: 420px;
}

.form-input-sm {
  width: 320px;
}

.form-input-number {
  width: 320px;
}

.form-textarea {
  width: 520px;
}

.readonly-input :deep(.el-input__wrapper) {
  background: var(--color-bg-hover);
  cursor: not-allowed;
}

/* ── Descriptions styling ── */
.info-descriptions :deep(.desc-label) {
  font-weight: 500;
  color: var(--color-text-secondary);
}

.text-primary {
  color: var(--color-primary);
}

.text-strong {
  font-weight: 500;
}

.text-sm {
  font-size: 12px;
}

/* ── Action cards ── */
.action-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.action-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: left;
  width: 100%;
  font-family: inherit;
}

.action-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary-light);
}

.action-card--primary {
  border-color: var(--color-accent);
  background: linear-gradient(135deg, rgba(5, 150, 105, 0.04), transparent);
}

.action-card--primary:hover {
  border-color: var(--color-accent);
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.15);
}

.action-card-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-hover);
  color: var(--color-text-secondary);
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.action-card--primary .action-card-icon {
  background: linear-gradient(135deg, #059669, #10b981);
  color: #fff;
}

.action-card:hover .action-card-icon {
  transform: scale(1.05);
}

.action-card-body {
  flex: 1;
  min-width: 0;
}

.action-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.action-card-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-card-arrow {
  color: var(--color-text-placeholder);
  flex-shrink: 0;
  transition: transform var(--transition-fast);
}

.action-card:hover .action-card-arrow {
  transform: translateX(3px);
  color: var(--color-primary);
}

/* ── Sidebar card ── */
.sidebar-card {
  position: sticky;
  top: var(--space-6);
}
</style>
