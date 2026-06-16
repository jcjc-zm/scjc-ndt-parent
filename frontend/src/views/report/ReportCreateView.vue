<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { reportApi } from '@/api/report'
import { templateApi } from '@/api/template'
import { inspectionApi } from '@/api/inspection'
import { projectApi } from '@/api/project'
import { imageApi } from '@/api/image'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const projects = ref([])
const inspections = ref([])
const templates = ref([])
const images = ref([])

const selectedProjectId = ref(null)
const selectedInspectionId = ref(null)
const selectedTemplateId = ref(null)
const selectedImages = ref([])

// Step tracking
const step = ref(1) // 1=select inspection, 2=select template+images, 3=confirm

async function loadProjects() {
  try {
    const res = await projectApi.list({ size: 1000 })
    if (res?.data) projects.value = res.data.records || []
  } catch { /* ignore */ }
}

async function loadInspections() {
  if (!selectedProjectId.value) return
  loading.value = true
  try {
    const res = await inspectionApi.list({ projectId: selectedProjectId.value, size: 10000 })
    if (res?.data) inspections.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  try {
    const res = await templateApi.list({})
    if (res?.data) templates.value = res.data || []
  } catch { /* ignore */ }
}

async function loadImages() {
  try {
    const res = await imageApi.list({})
    if (res?.data) images.value = res.data || []
  } catch { /* ignore */ }
}

function selectInspection(row) {
  selectedInspectionId.value = row.id
  step.value = 2
}

function toggleImageSelection(image) {
  const idx = selectedImages.value.findIndex((s) => s.imageId === image.id)
  if (idx > -1) {
    selectedImages.value.splice(idx, 1)
  } else {
    selectedImages.value.push({ areaId: `img_${Date.now()}`, imageId: image.id })
  }
}

async function handleGenerate() {
  if (!selectedInspectionId.value || !selectedTemplateId.value) {
    ElMessage.warning('请选择检测记录和报告模板')
    return
  }
  submitting.value = true
  try {
    const res = await reportApi.generate({
      inspectionId: selectedInspectionId.value,
      templateId: selectedTemplateId.value,
      imageSelections: selectedImages.value,
    })
    ElMessage.success('报告生成成功')
    router.push(`/report/preview/${res.data.id}`)
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

function goBack() {
  if (step.value > 1) {
    step.value--
  } else {
    router.push('/report')
  }
}

onMounted(() => {
  loadProjects()
  loadTemplates()
  loadImages()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <el-button :icon="ArrowLeft" link @click="goBack">返回</el-button>
        <h2 class="page-title">生成报告</h2>
      </div>
    </div>

    <!-- Steps -->
    <el-steps :active="step" finish-status="success" align-center style="margin-bottom: 24px">
      <el-step title="选择检测记录" />
      <el-step title="选择模板与图片" />
      <el-step title="确认生成" />
    </el-steps>

    <!-- Step 1: Select inspection record -->
    <el-card v-if="step === 1" shadow="never">
      <div class="step-section">
        <h3 class="step-title">选择项目</h3>
        <el-select v-model="selectedProjectId" placeholder="选择项目" style="width: 300px" @change="loadInspections">
          <el-option v-for="p in projects" :key="p.id" :label="`${p.projectCode} - ${p.projectName}`" :value="p.id" />
        </el-select>
      </div>

      <el-divider />

      <div class="step-section">
        <h3 class="step-title">选择检测记录</h3>
        <el-table
          :data="inspections"
          v-loading="loading"
          border
          stripe
          size="small"
          max-height="400"
          highlight-current-row
          :empty-text="selectedProjectId ? '暂无检测数据' : '请先选择项目'"
          @row-click="selectInspection"
        >
          <el-table-column prop="weldNo" label="焊口编号" width="130" />
          <el-table-column prop="inspectionMethod" label="检测方法" width="90" />
          <el-table-column prop="specification" label="规格" width="100" />
          <el-table-column prop="material" label="材质" width="100" />
          <el-table-column label="检测日期" width="120">
            <template #default="{ row }">
              {{ row.inspectionDate?.substring(0, 10) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="结果" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.inspectionConclusion === '合格' ? 'success' : 'danger'" size="small">
                {{ row.inspectionConclusion || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="inspectorName" label="检测人员" width="100" />
          <el-table-column label="操作" width="80" align="center">
            <template #default>
              <el-button link type="primary" size="small">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- Step 2: Select template & images -->
    <el-card v-if="step === 2" shadow="never">
      <el-row :gutter="24">
        <el-col :span="12">
          <div class="step-section">
            <h3 class="step-title">选择报告模板</h3>
            <div class="template-grid">
              <div
                v-for="tpl in templates"
                :key="tpl.id"
                class="template-card"
                :class="{ selected: selectedTemplateId === tpl.id }"
                @click="selectedTemplateId = tpl.id"
              >
                <div class="template-icon">
                  <el-icon :size="32"><Document /></el-icon>
                </div>
                <div class="template-info">
                  <strong>{{ tpl.templateName }}</strong>
                  <span class="text-secondary">{{ tpl.templateType === 'SYSTEM' ? '系统预设' : '自定义' }}</span>
                </div>
                <el-icon v-if="selectedTemplateId === tpl.id" class="check-icon" color="#059669"><CircleCheckFilled /></el-icon>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="step-section">
            <h3 class="step-title">选择图片（可选）</h3>
            <div class="image-grid">
              <div
                v-for="img in images"
                :key="img.id"
                class="image-card"
                :class="{ selected: selectedImages.some((s) => s.imageId === img.id) }"
                @click="toggleImageSelection(img)"
              >
                <div class="image-thumb">
                  <img v-if="img.imageUrl" :src="img.imageUrl" :alt="img.imageName" />
                  <el-icon v-else :size="40"><Picture /></el-icon>
                </div>
                <span class="image-name">{{ img.imageName }}</span>
                <el-icon v-if="selectedImages.some((s) => s.imageId === img.id)" class="check-icon" color="#059669"><CircleCheckFilled /></el-icon>
              </div>
            </div>
            <el-empty v-if="!images.length" description="暂无图片" :image-size="60" />
          </div>
        </el-col>
      </el-row>

      <el-divider />
      <div class="step-actions">
        <el-button @click="step = 1">上一步</el-button>
        <el-button type="primary" :disabled="!selectedTemplateId" @click="step = 3">下一步</el-button>
      </div>
    </el-card>

    <!-- Step 3: Confirm -->
    <el-card v-if="step === 3" shadow="never">
      <div class="step-section">
        <h3 class="step-title">确认生成报告</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="检测记录">
            {{ inspections.find((r) => r.id === selectedInspectionId)?.weldNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="报告模板">
            {{ templates.find((t) => t.id === selectedTemplateId)?.templateName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="选择图片">{{ selectedImages.length }} 张</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="step-actions" style="margin-top: 24px">
        <el-button @click="step = 2">上一步</el-button>
        <el-button type="primary" :loading="submitting" :icon="Check" @click="handleGenerate">
          {{ submitting ? '生成中...' : '确认生成' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-container { animation: fadeIn 0.3s ease; max-width: 1000px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.page-header { margin-bottom: var(--space-6); }
.page-title { font-size: 22px; font-weight: 700; margin: 8px 0 0; }

.step-section { padding: 4px 0; }
.step-title { font-size: 16px; font-weight: 600; margin: 0 0 16px; color: var(--color-text-primary); }

/* Template grid */
.template-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.template-card {
  display: flex; align-items: center; gap: 12px;
  padding: 16px; border: 2px solid var(--color-border); border-radius: var(--radius-md);
  cursor: pointer; transition: all var(--transition-fast); position: relative;
}
.template-card:hover { border-color: var(--color-accent); background: var(--color-bg); }
.template-card.selected { border-color: var(--color-accent); background: #ecfdf5; }
.template-icon { width: 48px; height: 48px; background: var(--color-bg); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: var(--color-primary); }
.template-info { display: flex; flex-direction: column; }
.check-icon { position: absolute; top: 8px; right: 8px; }

/* Image grid */
.image-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.image-card {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 12px; border: 2px solid var(--color-border); border-radius: var(--radius-md);
  cursor: pointer; transition: all var(--transition-fast); position: relative;
}
.image-card:hover { border-color: var(--color-accent); }
.image-card.selected { border-color: var(--color-accent); background: #ecfdf5; }
.image-thumb { width: 80px; height: 60px; background: var(--color-bg); border-radius: 4px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.image-thumb img { max-width: 100%; max-height: 100%; object-fit: contain; }
.image-name { font-size: 12px; color: var(--color-text-secondary); text-align: center; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 80px; }

.step-actions { display: flex; justify-content: center; gap: 12px; }
</style>
