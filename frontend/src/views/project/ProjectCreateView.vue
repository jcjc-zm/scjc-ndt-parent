<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { projectApi } from '@/api/project'
import { deptApi } from '@/api/dept'

const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)
const buList = ref([])

const form = reactive({
  projectCode: '',
  projectName: '',
  unitProjectName: '',
  projectType: 'COMPANY_DIRECT',
  parentId: null,
  buName: '',
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
  projectCode: [
    { required: true, message: '请输入项目编号', trigger: 'blur' },
    { min: 2, max: 50, message: '项目编号长度为 2-50 个字符', trigger: 'blur' },
  ],
  projectName: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { min: 2, max: 100, message: '项目名称长度为 2-100 个字符', trigger: 'blur' },
  ],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  buName: [{
    validator: (_rule, value, callback) => {
      if (form.projectType === 'BU_SUB' && !value) {
        callback(new Error('请选择所属事业部'))
      } else {
        callback()
      }
    },
    trigger: 'change',
  }],
  constructionUnit: [{ required: true, message: '请输入施工单位', trigger: 'blur' }],
}

async function loadBuList() {
  try {
    const res = await deptApi.list()
    if (res?.data) buList.value = res.data.filter((d) => d.deptType === 'BU')
  } catch {
    // ignore
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await projectApi.create(form)
      ElMessage.success('项目创建成功')
      router.push('/project')
    } catch {
      // Error handled by interceptor
    } finally {
      submitting.value = false
    }
  })
}

function handleCancel() {
  router.back()
}

onMounted(() => loadBuList())

// Clear BU selection when project type changes
watch(() => form.projectType, () => {
  form.buName = ''
  formRef.value?.validateField('buName')
})
</script>

<template>
  <div class="page-container">
    <!-- Page header -->
    <div class="page-header">
      <div>
        <h2 class="page-title">创建立项</h2>
        <p class="page-subtitle">创建新的检测工程项目</p>
      </div>
    </div>

    <!-- Form card -->
    <div class="form-card">
      <div class="form-card-header">
        <el-icon :size="18"><Edit /></el-icon>
        <span class="form-card-title">项目信息</span>
      </div>
      <div class="form-card-body">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
          label-position="right"
          size="default"
          class="project-form"
        >
          <!-- Project type selector -->
          <div class="form-section-label">项目归属</div>

          <el-form-item label="项目类型" prop="projectType">
            <div class="type-selector">
              <label
                class="type-card"
                :class="{ 'type-card--active': form.projectType === 'COMPANY_DIRECT' }"
              >
                <input
                  type="radio"
                  value="COMPANY_DIRECT"
                  v-model="form.projectType"
                  class="type-radio"
                />
                <div class="type-card-icon company-icon">
                  <el-icon :size="22"><OfficeBuilding /></el-icon>
                </div>
                <div class="type-card-body">
                  <div class="type-card-title">公司直属项目</div>
                  <div class="type-card-desc">由公司直接管理的项目</div>
                </div>
              </label>

              <label
                class="type-card"
                :class="{ 'type-card--active': form.projectType === 'BU_SUB' }"
              >
                <input
                  type="radio"
                  value="BU_SUB"
                  v-model="form.projectType"
                  class="type-radio"
                />
                <div class="type-card-icon bu-icon">
                  <el-icon :size="22"><Connection /></el-icon>
                </div>
                <div class="type-card-body">
                  <div class="type-card-title">事业部下属项目</div>
                  <div class="type-card-desc">归属于特定事业部的项目</div>
                </div>
              </label>
            </div>
          </el-form-item>

          <!-- BU selector (conditional) -->
          <transition name="expand">
            <el-form-item v-if="form.projectType === 'BU_SUB'" label="所属事业部" prop="buName" class="bu-field">
              <el-select v-model="form.buName" placeholder="请选择事业部" class="bu-select" size="large">
                <el-option
                  v-for="bu in buList"
                  :key="bu.buName"
                  :label="bu.deptName"
                  :value="bu.buName"
                />
              </el-select>
            </el-form-item>
          </transition>

          <!-- Basic info -->
          <div class="form-section-label">基本信息</div>

          <el-form-item label="项目编号" prop="projectCode">
            <el-input
              v-model="form.projectCode"
              placeholder="请输入项目编号，如 XJ-2026-001"
              class="form-input"
              clearable
              size="large"
            />
          </el-form-item>

          <el-form-item label="项目名称" prop="projectName">
            <el-input
              v-model="form.projectName"
              placeholder="请输入项目名称"
              class="form-input-wide"
              clearable
              size="large"
            />
          </el-form-item>

          <el-form-item label="单位工程名称" prop="unitProjectName">
            <el-input
              v-model="form.unitProjectName"
              placeholder="请输入单位工程名称（可选）"
              class="form-input-wide"
              clearable
            />
          </el-form-item>

          <el-form-item label="施工单位" prop="constructionUnit">
            <el-input
              v-model="form.constructionUnit"
              placeholder="请输入施工单位名称"
              class="form-input-wide"
              clearable
            />
          </el-form-item>

          <!-- Extended info -->
          <div class="form-section-label">
            扩展信息
            <span class="section-label-hint">（选填）</span>
          </div>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="设计单位" prop="designUnit" class="compact-label">
                <el-input
                  v-model="form.designUnit"
                  placeholder="请输入设计单位"
                  clearable
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="监理单位" prop="supervisionUnit" class="compact-label">
                <el-input
                  v-model="form.supervisionUnit"
                  placeholder="请输入监理单位"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="合同编号" prop="contractNo" class="compact-label">
                <el-input
                  v-model="form.contractNo"
                  placeholder="请输入合同编号"
                  clearable
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同金额" prop="contractAmount" class="compact-label">
                <el-input-number
                  v-model="form.contractAmount"
                  :precision="2"
                  :min="0"
                  :controls="true"
                  placeholder="请输入合同金额"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="项目经理" prop="projectManager" class="compact-label">
                <el-input
                  v-model="form.projectManager"
                  placeholder="请输入项目经理姓名"
                  clearable
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="工程地点" prop="projectLocation" class="compact-label">
                <el-input
                  v-model="form.projectLocation"
                  placeholder="请输入工程地点"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="项目描述" prop="projectDescription">
            <el-input
              v-model="form.projectDescription"
              type="textarea"
              :rows="4"
              placeholder="请输入项目描述"
              class="form-textarea"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <!-- Actions -->
          <div class="form-actions">
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              class="submit-btn"
              @click="handleSubmit"
            >
              <el-icon><Check /></el-icon>
              {{ submitting ? '创建中...' : '确 定' }}
            </el-button>
            <el-button size="large" @click="handleCancel">
              <el-icon><Close /></el-icon>
              取 消
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Page container ── */
.page-container {
  animation: fadeSlideIn 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 880px;
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  margin-bottom: var(--space-6);
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 4px;
  letter-spacing: -0.3px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* ── Form card ── */
.form-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: box-shadow var(--transition-fast);
}

.form-card:hover {
  box-shadow: var(--shadow-sm);
}

.form-card-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-6);
  background: var(--color-bg-hover);
  border-bottom: 1px solid var(--color-border-light);
  color: var(--color-text-primary);
}

.form-card-title {
  font-size: 15px;
  font-weight: 600;
}

.form-card-body {
  padding: var(--space-6);
}

/* ── Form sections ── */
.form-section-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: var(--space-6) 0 var(--space-4);
  padding-bottom: var(--space-2);
  border-bottom: 1px dashed var(--color-border-light);
}

.form-section-label:first-child {
  margin-top: 0;
}

.section-label-hint {
  font-weight: 400;
  text-transform: none;
  letter-spacing: 0;
}

/* ── Project type selector ── */
.type-selector {
  display: flex;
  gap: var(--space-4);
}

.type-card {
  flex: 1;
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  padding: var(--space-5);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  background: var(--color-bg-card);
  position: relative;
}

.type-card:hover {
  border-color: var(--color-primary-light);
  background: var(--color-bg-hover);
}

.type-card--active {
  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(51, 65, 85, 0.03), transparent);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.type-radio {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.type-card-icon {
  width: 46px;
  height: 46px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.company-icon {
  background: linear-gradient(135deg, #475569, #334155);
  color: #fff;
}

.bu-icon {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
}

.type-card--active .type-card-icon {
  transform: scale(1.05);
}

.type-card-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.type-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.type-card-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* ── BU selector ── */
.bu-field {
  overflow: hidden;
}

.bu-select {
  width: 360px;
}

/* ── Form fields ── */
.form-input {
  width: 360px;
}

.form-input-wide {
  width: 480px;
}

.form-textarea {
  width: 540px;
}

/* Compact labels for two-column layout */
.compact-label :deep(.el-form-item__label) {
  width: 100px !important;
}

/* ── Form actions ── */
.form-actions {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  gap: var(--space-3);
}

.submit-btn {
  min-width: 120px;
  font-weight: 600;
}

/* ── Transition: expand ── */
.expand-enter-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.expand-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.expand-enter-from {
  opacity: 0;
  transform: translateY(-8px);
  max-height: 0;
}

.expand-leave-to {
  opacity: 0;
  transform: translateY(-8px);
  max-height: 0;
}
</style>
