<script setup>
import { ref, reactive, onMounted } from 'vue'
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
  buName: [{ required: true, message: '请选择所属事业部', trigger: 'change' }],
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

    <!-- Form -->
    <el-card shadow="never" class="form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="right"
        size="default"
        class="project-form"
      >
        <!-- Project type -->
        <el-form-item label="项目类型" prop="projectType">
          <el-radio-group v-model="form.projectType">
            <el-radio-button value="COMPANY_DIRECT">
              <el-icon><OfficeBuilding /></el-icon>
              公司直属项目
            </el-radio-button>
            <el-radio-button value="BU_SUB">
              <el-icon><Connection /></el-icon>
              事业部下属项目
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- BU selector (required for BU_SUB) -->
        <el-form-item label="所属事业部" prop="buName">
          <el-select v-model="form.buName" placeholder="请选择事业部" style="width: 300px">
            <el-option
              v-for="bu in buList"
              :key="bu.buName"
              :label="bu.deptName"
              :value="bu.buName"
            />
          </el-select>
        </el-form-item>

        <!-- Basic info -->
        <el-divider content-position="left">基本信息</el-divider>

        <el-form-item label="项目编号" prop="projectCode">
          <el-input
            v-model="form.projectCode"
            placeholder="请输入项目编号，如 XJ-2026-001"
            style="width: 300px"
            clearable
          />
        </el-form-item>

        <el-form-item label="项目名称" prop="projectName">
          <el-input
            v-model="form.projectName"
            placeholder="请输入项目名称"
            style="width: 400px"
            clearable
          />
        </el-form-item>

        <el-form-item label="单位工程名称" prop="unitProjectName">
          <el-input
            v-model="form.unitProjectName"
            placeholder="请输入单位工程名称（可选）"
            style="width: 400px"
            clearable
          />
        </el-form-item>

        <el-form-item label="施工单位" prop="constructionUnit">
          <el-input
            v-model="form.constructionUnit"
            placeholder="请输入施工单位名称"
            style="width: 400px"
            clearable
          />
        </el-form-item>

        <!-- Actions -->
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            :icon="Check"
            @click="handleSubmit"
          >
            {{ submitting ? '创建中...' : '确 定' }}
          </el-button>
          <el-button :icon="Close" @click="handleCancel">取 消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease;
  max-width: 800px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
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
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}

.form-card {
  border-radius: var(--radius-md);
}

.project-form {
  padding: var(--space-4) 0;
}

.project-form :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
