<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VueDraggable } from 'vue-draggable-plus'
import { templateApi } from '@/api/template'

const route = useRoute()
const router = useRouter()

const templateId = ref(route.params.id !== '0' ? Number(route.params.id) : null)
const saving = ref(false)
const templateName = ref('新模板')
const templateDesc = ref('')
const methodType = ref('RT')
const selectedField = ref(null)

// Available field categories
const fieldCategories = [
  {
    name: '工程信息',
    fields: [
      { key: 'constructionUnit', label: '施工单位' },
      { key: 'weldNo', label: '焊口编号' },
      { key: 'instructionNo', label: '指令编号' },
      { key: 'instructionDate', label: '指令日期' },
      { key: 'inspectionMethod', label: '检测方法' },
      { key: 'projectName', label: '工程名称' },
      { key: 'unitProjectName', label: '单位工程名称' },
      { key: 'buDept', label: '所属事业部' },
    ],
  },
  {
    name: '技术参数',
    fields: [
      { key: 'specification', label: '规格' },
      { key: 'material', label: '材质' },
      { key: 'grooveType', label: '坡口形式' },
      { key: 'position', label: '部位' },
      { key: 'weldingMethod', label: '焊接方式' },
      { key: 'ratio', label: '比例' },
      { key: 'inspectionStandard', label: '检测标准' },
      { key: 'qualifiedLevel', label: '合格级别' },
      { key: 'inspectionItem', label: '检测项目' },
    ],
  },
  {
    name: '人员与工艺',
    fields: [
      { key: 'welderCode', label: '焊工代号' },
      { key: 'weldingDept', label: '焊接部门' },
      { key: 'inspectionLength', label: '检测长度' },
      { key: 'processCardNo', label: '工艺卡编号' },
      { key: 'samplingInstructionNo', label: '抽检指令编号' },
      { key: 'inspectorName', label: '检测人员' },
    ],
  },
  {
    name: '检测结果',
    fields: [
      { key: 'inspectionDate', label: '检测日期' },
      { key: 'resultLevel', label: '级别' },
      { key: 'inspectionConclusion', label: '检测结论' },
      { key: 'unqualifiedHandling', label: '不合格处理' },
      { key: 'reportDefectPosition', label: '缺陷位置' },
      { key: 'reportDefectNature', label: '缺陷性质' },
      { key: 'reportDefectLength', label: '缺陷长度' },
      { key: 'unqualifiedDefectType', label: '不合格缺陷类型' },
      { key: 'remark', label: '备注' },
    ],
  },
  {
    name: '底片信息',
    fields: [
      { key: 'boxNo', label: '箱号' },
      { key: 'filmLength', label: '底片长度' },
      { key: 'filmCount', label: '底片张数' },
    ],
  },
  {
    name: '分级统计',
    fields: [
      { key: 'levelI', label: 'Ⅰ级' },
      { key: 'levelIi', label: 'Ⅱ级' },
      { key: 'levelIii', label: 'Ⅲ级' },
      { key: 'levelIv', label: 'Ⅳ级' },
    ],
  },
  {
    name: '缺陷位置组',
    fields: Array.from({ length: 15 }, (_, i) => ({
      key: `defect_${i + 1}`,
      label: `位置${i + 1}`,
    })),
  },
]

// Canvas fields - the fields placed on the report
const canvasFields = ref([])

// Image areas on canvas
const imageAreas = ref([])

// Dragging state
const draggingField = ref(null)

// Add a field to canvas
function addFieldToCanvas(field) {
  const newField = {
    id: `f_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
    key: field.key,
    label: field.label,
    x: 30 + (canvasFields.value.length % 3) * 180,
    y: 30 + Math.floor(canvasFields.value.length / 3) * 36,
    width: 160,
    fontSize: 13,
    fontWeight: 'normal',
    color: '#1e293b',
  }
  canvasFields.value.push(newField)
  selectedField.value = newField
}

// Add image area
function addImageArea() {
  const area = {
    id: `img_${Date.now()}`,
    label: `图片区 ${imageAreas.value.length + 1}`,
    x: 100,
    y: 100 + imageAreas.value.length * 150,
    width: 300,
    height: 200,
  }
  imageAreas.value.push(area)
  selectedField.value = area
}

// Remove selected field from canvas
function removeSelectedField() {
  if (!selectedField.value) return
  const f = selectedField.value
  if ('key' in f) {
    canvasFields.value = canvasFields.value.filter((item) => item.id !== f.id)
  } else {
    imageAreas.value = imageAreas.value.filter((item) => item.id !== f.id)
  }
  selectedField.value = null
}

function handleCanvasDrop(event) {
  if (draggingField.value) {
    addFieldToCanvas({ ...draggingField.value })
  }
}
function updateFieldPosition(id, x, y) {
  const f = canvasFields.value.find((item) => item.id === id)
  if (f) {
    f.x = Math.round(x)
    f.y = Math.round(y)
  }
}

// ─── Save template ───
async function saveTemplate() {
  if (!templateName.value.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }

  const layoutConfig = {
    fields: canvasFields.value.map((f) => ({
      id: f.id,
      key: f.key,
      label: f.label,
      x: f.x,
      y: f.y,
      width: f.width,
      fontSize: f.fontSize,
      fontWeight: f.fontWeight,
      color: f.color,
    })),
    imageAreas: imageAreas.value.map((a) => ({
      id: a.id,
      label: a.label,
      x: a.x,
      y: a.y,
      width: a.width,
      height: a.height,
    })),
    pageSize: 'A4',
  }

  saving.value = true
  try {
    if (templateId.value) {
      await templateApi.update(templateId.value, {
        id: templateId.value,
        templateName: templateName.value,
        templateType: 'CUSTOM',
        methodType: methodType.value,
        description: templateDesc.value,
        layoutConfig: JSON.stringify(layoutConfig),
      })
      ElMessage.success('模板已更新')
    } else {
      const res = await templateApi.create({
        templateName: templateName.value,
        templateType: 'CUSTOM',
        methodType: methodType.value,
        description: templateDesc.value,
        layoutConfig: JSON.stringify(layoutConfig),
      })
      if (res?.data) {
        templateId.value = res.data.id
        ElMessage.success('模板已保存')
        router.replace(`/report/design/${res.data.id}`)
      }
    }
  } catch {
    // Error handled by interceptor
  } finally {
    saving.value = false
  }
}

// ─── Load existing template ───
async function loadTemplate() {
  if (!templateId.value) return
  try {
    const res = await templateApi.getById(templateId.value)
    if (res?.data) {
      const tpl = res.data
      templateName.value = tpl.templateName || ''
      templateDesc.value = tpl.description || ''
      methodType.value = tpl.methodType || 'RT'
      if (tpl.layoutConfig) {
        try {
          const layout = typeof tpl.layoutConfig === 'string' ? JSON.parse(tpl.layoutConfig) : tpl.layoutConfig
          canvasFields.value = layout.fields || []
          imageAreas.value = layout.imageAreas || []
        } catch { /* ignore */ }
      }
    }
  } catch { /* ignore */ }
}

function goBack() {
  router.push('/report')
}

onMounted(() => loadTemplate())
</script>

<template>
  <div class="designer-page">
    <!-- Top toolbar -->
    <div class="designer-toolbar">
      <div class="toolbar-left">
        <el-button :icon="ArrowLeft" link @click="goBack">返回</el-button>
        <el-divider direction="vertical" />
        <el-input v-model="templateName" placeholder="模板名称" style="width: 180px" size="default" />
        <el-select v-model="methodType" style="width: 100px" size="default">
          <el-option v-for="m in ['RT','UT','PT','MT','AUT','PA','TOFD','DR']" :key="m" :label="m" :value="m" />
        </el-select>
        <el-input v-model="templateDesc" placeholder="模板描述（可选）" style="width: 200px" size="default" />
      </div>
      <div class="toolbar-right">
        <el-button @click="addImageArea" :icon="Picture">添加图片区</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="saveTemplate">
          {{ saving ? '保存中...' : '保存模板' }}
        </el-button>
      </div>
    </div>

    <!-- Main 3-panel layout -->
    <div class="designer-body">
      <!-- Left: Field panel -->
      <aside class="field-panel">
        <div class="panel-header">字段组件</div>
        <div class="panel-body">
          <div v-for="cat in fieldCategories" :key="cat.name" class="field-category">
            <p class="category-title">{{ cat.name }}</p>
            <div class="field-list">
              <div
                v-for="field in cat.fields"
                :key="field.key"
                class="field-item"
                draggable="true"
                @dragstart="draggingField = field"
                @dragend="draggingField = null"
                @click="addFieldToCanvas(field)"
              >
                <el-icon :size="14"><Plus /></el-icon>
                <span>{{ field.label }}</span>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- Center: A4 Canvas -->
      <main
        class="canvas-area"
        @dragover.prevent
        @drop.prevent="handleCanvasDrop"
      >
        <div class="canvas-scroll">
          <div class="a4-canvas" :style="{ width: '595px', minHeight: '842px' }">
            <!-- Rendered fields -->
            <div
              v-for="field in canvasFields"
              :key="field.id"
              class="canvas-field"
              :class="{ selected: selectedField?.id === field.id }"
              :style="{
                left: field.x + 'px',
                top: field.y + 'px',
                width: field.width + 'px',
                fontSize: field.fontSize + 'px',
                fontWeight: field.fontWeight,
                color: field.color,
              }"
              @mousedown.stop="selectedField = field"
            >
              {{ field.label }}
              <span class="field-preview text-secondary" style="font-size: 10px">&#123;&#123;{{ field.key }}&#125;&#125;</span>
            </div>

            <!-- Image areas -->
            <div
              v-for="area in imageAreas"
              :key="area.id"
              class="canvas-image-area"
              :class="{ selected: selectedField?.id === area.id }"
              :style="{
                left: area.x + 'px',
                top: area.y + 'px',
                width: area.width + 'px',
                height: area.height + 'px',
              }"
              @mousedown.stop="selectedField = area"
            >
              <el-icon :size="24"><Picture /></el-icon>
              <span>{{ area.label }}</span>
            </div>

            <!-- Empty state -->
            <div v-if="!canvasFields.length && !imageAreas.length" class="canvas-empty">
              <el-icon :size="48"><Plus /></el-icon>
              <p>从左侧拖拽或点击字段添加到画布</p>
              <p class="text-secondary">或点击"添加图片区"放置图片</p>
            </div>
          </div>
        </div>
      </main>

      <!-- Right: Properties panel -->
      <aside class="props-panel">
        <div class="panel-header">属性设置</div>
        <div class="panel-body">
          <template v-if="selectedField">
            <!-- Field properties -->
            <template v-if="'key' in selectedField">
              <div class="prop-group">
                <label>标签名</label>
                <el-input v-model="selectedField.label" size="small" />
              </div>
              <div class="prop-group">
                <label>字段绑定</label>
                <el-input :model-value="selectedField.key" size="small" disabled />
              </div>
              <div class="prop-group">
                <label>字体大小</label>
                <el-input-number v-model="selectedField.fontSize" :min="8" :max="48" size="small" style="width:100%" />
              </div>
              <div class="prop-group">
                <label>字重</label>
                <el-select v-model="selectedField.fontWeight" size="small" style="width:100%">
                  <el-option label="常规" value="normal" />
                  <el-option label="加粗" value="bold" />
                </el-select>
              </div>
              <div class="prop-group">
                <label>颜色</label>
                <el-color-picker v-model="selectedField.color" size="small" />
              </div>
              <div class="prop-group">
                <label>宽度</label>
                <el-input-number v-model="selectedField.width" :min="60" :max="400" size="small" style="width:100%" />
              </div>
              <div class="prop-group">
                <label>X 坐标</label>
                <el-input-number v-model="selectedField.x" :min="0" :max="550" size="small" style="width:100%" />
              </div>
              <div class="prop-group">
                <label>Y 坐标</label>
                <el-input-number v-model="selectedField.y" :min="0" :max="800" size="small" style="width:100%" />
              </div>
            </template>

            <!-- Image area properties -->
            <template v-else>
              <div class="prop-group">
                <label>区域名称</label>
                <el-input v-model="selectedField.label" size="small" />
              </div>
              <div class="prop-group">
                <label>宽度</label>
                <el-input-number v-model="selectedField.width" :min="50" :max="500" size="small" style="width:100%" />
              </div>
              <div class="prop-group">
                <label>高度</label>
                <el-input-number v-model="selectedField.height" :min="50" :max="500" size="small" style="width:100%" />
              </div>
              <div class="prop-group">
                <label>X 坐标</label>
                <el-input-number v-model="selectedField.x" :min="0" :max="550" size="small" style="width:100%" />
              </div>
              <div class="prop-group">
                <label>Y 坐标</label>
                <el-input-number v-model="selectedField.y" :min="0" :max="800" size="small" style="width:100%" />
              </div>
            </template>

            <el-divider />
            <el-button type="danger" :icon="Delete" size="small" @click="removeSelectedField">
              删除此元素
            </el-button>
          </template>
          <el-empty v-else description="点击画布上的元素进行编辑" :image-size="60" />
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.designer-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--header-height) - 48px);
  margin: -24px;
  background: #f1f5f9;
}

/* Toolbar */
.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 3-panel body */
.designer-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* Left panel */
.field-panel {
  width: 240px;
  background: #fff;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.panel-header {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.field-category {
  margin-bottom: 12px;
}
.category-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 4px 8px;
  margin: 0;
}
.field-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.field-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  font-size: 12px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  cursor: grab;
  transition: all var(--transition-fast);
  user-select: none;
}
.field-item:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: #ecfdf5;
}
.field-item:active {
  cursor: grabbing;
}

/* Center canvas */
.canvas-area {
  flex: 1;
  overflow: auto;
  display: flex;
  justify-content: center;
  padding: 24px;
  background: #e2e8f0;
}
.a4-canvas {
  background: #fff;
  box-shadow: var(--shadow-lg);
  position: relative;
  border-radius: 2px;
}
.canvas-field {
  position: absolute;
  padding: 4px 6px;
  border: 1px dashed transparent;
  border-radius: 3px;
  cursor: move;
  transition: border-color var(--transition-fast);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.canvas-field:hover { border-color: var(--color-accent); background: rgba(5, 150, 105, 0.04); }
.canvas-field.selected { border-color: var(--color-accent); background: rgba(5, 150, 105, 0.08); border-style: solid; }

.canvas-image-area {
  position: absolute;
  border: 2px dashed var(--color-text-placeholder);
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: border-color var(--transition-fast);
}
.canvas-image-area:hover { border-color: var(--color-accent); }
.canvas-image-area.selected { border-color: var(--color-accent); border-style: solid; background: rgba(5, 150, 105, 0.04); }

.canvas-empty {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: var(--color-text-placeholder);
}
.canvas-empty p { margin: 8px 0 0; font-size: 14px; }

/* Right panel */
.props-panel {
  width: 240px;
  background: #fff;
  border-left: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.prop-group {
  margin-bottom: 12px;
}
.prop-group label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}
</style>
