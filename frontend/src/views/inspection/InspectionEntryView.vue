<script setup>
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { inspectionApi } from '@/api/inspection'
import { projectApi } from '@/api/project'
import * as XLSX from 'xlsx'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const projectId = ref(Number(route.params.projectId))
const project = ref(null)
const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
// Filter
const filterMethod = ref('')
const filterKeyword = ref('')

// ─── Column configuration ───
// Each column: { field, title, width, editRender (if editable), visible }
const methodTypes = ['RT', 'UT', 'PT', 'MT', 'AUT', 'PA', 'TOFD', 'DR']
const levelOptions = ['Ⅰ', 'Ⅱ', 'Ⅲ', 'Ⅳ']
const conclusionOptions = ['合格', '不合格']
const grooveOptions = ['V型', 'X型', 'U型', 'I型', 'K型', '双V型']
const weldMethodOptions = ['GTAW', 'SMAW', 'GMAW', 'FCAW', 'SAW']
const positionOptions = ['管口', '法兰', '弯头', '三通', '直管段']

// Helper to render select editor
function selectEditRender(options) {
  return {
    name: '$select',
    props: { clearable: true },
    options: options.map((o) => ({ label: o, value: o })),
  }
}

function inputEditRender(type = '$input') {
  return { name: type, props: { clearable: true } }
}

// Editable columns (showing key ones; all are editable)
const columns = computed(() => {
  const base = [
    { type: 'seq', width: 50, fixed: 'left' },
    { field: 'weldNo', title: '焊口编号', width: 130, fixed: 'left', editRender: inputEditRender(), required: true },
    { field: 'inspectionMethod', title: '检测方法', width: 90, editRender: selectEditRender(methodTypes), fixed: 'left' },
    { field: 'constructionUnit', title: '施工单位', width: 140, editRender: inputEditRender() },
    { field: 'instructionNo', title: '指令编号', width: 130, editRender: inputEditRender() },
    { field: 'instructionDate', title: '指令日期', width: 120, editRender: inputEditRender('$input') },
    { field: 'projectName', title: '工程名称', width: 150, editRender: inputEditRender() },
    { field: 'unitProjectName', title: '单位工程名称', width: 150, editRender: inputEditRender() },
    { field: 'buDept', title: '所属事业部', width: 120, editRender: inputEditRender() },
    { field: 'specification', title: '规格', width: 100, editRender: inputEditRender() },
    { field: 'material', title: '材质', width: 100, editRender: inputEditRender() },
    { field: 'grooveType', title: '坡口形式', width: 100, editRender: selectEditRender(grooveOptions) },
    { field: 'position', title: '部位', width: 100, editRender: selectEditRender(positionOptions) },
    { field: 'weldingMethod', title: '焊接方式', width: 100, editRender: selectEditRender(weldMethodOptions) },
    { field: 'ratio', title: '比例', width: 80, editRender: inputEditRender() },
    { field: 'inspectionStandard', title: '检测标准', width: 120, editRender: inputEditRender() },
    { field: 'qualifiedLevel', title: '合格级别', width: 100, editRender: inputEditRender() },
    { field: 'inspectionItem', title: '检测项目', width: 120, editRender: inputEditRender() },
    { field: 'welderCode', title: '焊工代号', width: 100, editRender: inputEditRender() },
    { field: 'weldingDept', title: '焊接部门', width: 120, editRender: inputEditRender() },
    { field: 'inspectionLength', title: '检测长度(m)', width: 110, editRender: { name: '$input', props: { type: 'number', clearable: true } } },
    { field: 'processCardNo', title: '工艺卡编号', width: 130, editRender: inputEditRender() },
    { field: 'samplingInstructionNo', title: '抽检指令编号', width: 140, editRender: inputEditRender() },
    { field: 'inspectionDate', title: '检测日期', width: 120, editRender: inputEditRender() },
    { field: 'resultLevel', title: '级别', width: 75, editRender: selectEditRender(levelOptions) },
    { field: 'inspectionConclusion', title: '检测结论', width: 100, editRender: selectEditRender(conclusionOptions) },
    { field: 'unqualifiedHandling', title: '不合格处理', width: 140, editRender: inputEditRender() },
    { field: 'reportDefectPosition', title: '缺陷位置', width: 120, editRender: inputEditRender() },
    { field: 'reportDefectNature', title: '缺陷性质', width: 120, editRender: inputEditRender() },
    { field: 'reportDefectLength', title: '缺陷长度', width: 100, editRender: { name: '$input', props: { type: 'number' } } },
    { field: 'unqualifiedDefectType', title: '不合格缺陷类型', width: 140, editRender: inputEditRender() },
    { field: 'remark', title: '备注', width: 150, editRender: inputEditRender() },
    { field: 'inspectorName', title: '检测人员', width: 100, editRender: inputEditRender() },
    { field: 'boxNo', title: '箱号', width: 100, editRender: inputEditRender() },
    { field: 'filmLength', title: '底片长度', width: 100, editRender: { name: '$input', props: { type: 'number' } } },
    { field: 'filmCount', title: '底片张数', width: 90, editRender: { name: '$input', props: { type: 'integer', min: 0 } } },
    { field: 'levelI', title: 'Ⅰ级', width: 65, editRender: { name: '$input', props: { type: 'integer', min: 0 } } },
    { field: 'levelIi', title: 'Ⅱ级', width: 65, editRender: { name: '$input', props: { type: 'integer', min: 0 } } },
    { field: 'levelIii', title: 'Ⅲ级', width: 65, editRender: { name: '$input', props: { type: 'integer', min: 0 } } },
    { field: 'levelIv', title: 'Ⅳ级', width: 65, editRender: { name: '$input', props: { type: 'integer', min: 0 } } },
    { field: 'defectAction', title: '缺陷位置', width: 90, fixed: 'right', slots: { default: 'defect_edit' } },
    { field: 'operate', title: '操作', width: 80, fixed: 'right', slots: { default: 'operate' } },
  ]
  return base
})

// Data columns (excluding action columns)
const dataColumns = computed(() =>
  columns.value.filter((c) => c.field && c.field !== 'defectAction' && c.field !== 'operate')
)

// Visible column keys for column customization (default: show most-used)
const visibleColumns = ref(dataColumns.value.map((c) => c.field))

function updateColumnVisibility(keys) {
  columns.value.forEach((col) => {
    if (col.field && col.field !== 'defectAction' && col.field !== 'operate') {
      col.visible = keys.includes(col.field)
    }
  })
}

// Edit config — row mode for Excel-like experience
const editConfig = reactive({
  trigger: 'click',
  mode: 'row',
  showIcon: false,
  autoClear: false,
  showStatus: true,
})

// Row-level validation
const editRules = ref({
  weldNo: [{ required: true, message: '焊口编号不能为空' }],
  inspectionMethod: [{ required: true, message: '请选择检测方法' }],
})

// ─── Data loading ───
async function loadProject() {
  try {
    const res = await projectApi.getById(projectId.value)
    if (res?.data) project.value = res.data
  } catch {
    ElMessage.error('项目信息加载失败')
    router.push('/project')
  }
}

async function loadData() {
  loading.value = true
  try {
    const params = { projectId: projectId.value, size: 10000 }
    if (filterMethod.value) params.method = filterMethod.value
    if (filterKeyword.value) params.keyword = filterKeyword.value
    const res = await inspectionApi.list(params)
    if (res?.data) {
      tableData.value = (res.data.records || []).map((row) => ({
        ...row,
        // Parse defectPositions if it's a JSON string
        _defectPositions: tryParseJson(row.defectPositions),
      }))
    }
  } finally {
    loading.value = false
  }
  ensureEmptyRow()
}

function tryParseJson(val) {
  if (!val) return []
  if (typeof val === 'object') return val
  try { return JSON.parse(val) } catch { return [] }
}

// ─── CRUD operations (row mode: save on row leave) ───
async function handleEditActived({ row }) {
  if (!row) return
  // Set defaults for new rows
  if (!row.projectId) row.projectId = projectId.value
  if (!row.buDept) row.buDept = project.value?.buName || ''
  if (!row.projectName) row.projectName = project.value?.projectName || ''
  if (!row.unitProjectName) row.unitProjectName = project.value?.unitProjectName || ''
  if (!row.constructionUnit) row.constructionUnit = project.value?.constructionUnit || ''
}

async function handleEditClosed({ row }) {
  if (!row) return

  // Skip if row is completely empty
  const hasData = row.weldNo || row.inspectionMethod || row.inspectionDate
  if (!hasData) {
    // Remove empty row if it's not the only row
    if (tableData.value.length > 1) {
      const idx = tableData.value.indexOf(row)
      if (idx > -1) tableData.value.splice(idx, 1)
    }
    return
  }

  saving.value = true
  try {
    if (row.id) {
      // Update existing
      await inspectionApi.update(row.id, { ...row, projectId: projectId.value })
    } else if (row.weldNo) {
      // Create new
      const res = await inspectionApi.create({ ...row, projectId: projectId.value })
      if (res?.data) {
        row.id = res.data.id
        row.createTime = res.data.createTime
      }
    }
  } catch {
    // Error handled by interceptor
  } finally {
    saving.value = false
  }

  // Auto-append: if this was the last row, add a new empty row
  const idx = tableData.value.indexOf(row)
  if (idx === tableData.value.length - 1) {
    ensureEmptyRow()
  }
}

// Ensure at least one empty row at the bottom
function ensureEmptyRow() {
  const lastRow = tableData.value[tableData.value.length - 1]
  if (!lastRow || lastRow.id || lastRow.weldNo) {
    tableData.value.push({
      projectId: projectId.value,
      buDept: project.value?.buName || '',
      projectName: project.value?.projectName || '',
      unitProjectName: project.value?.unitProjectName || '',
      constructionUnit: project.value?.constructionUnit || '',
    })
  }
}

// ─── Delete ───
async function handleDelete(row) {
  if (!row.id) {
    const idx = tableData.value.indexOf(row)
    if (idx > -1) tableData.value.splice(idx, 1)
    ensureEmptyRow()
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除焊口「${row.weldNo}」的检测数据？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await inspectionApi.remove(row.id)
    ElMessage.success('删除成功')
    const idx = tableData.value.indexOf(row)
    if (idx > -1) tableData.value.splice(idx, 1)
    ensureEmptyRow()
  } catch { /* cancelled */ }
}

// ─── Export ───
async function handleExport() {
  try {
    loading.value = true
    const res = await inspectionApi.exportData({ projectId: projectId.value, method: filterMethod.value })
    // The API returns blob
    const url = window.URL.createObjectURL(res)
    const link = document.createElement('a')
    link.href = url
    link.download = `检测数据_${project.value?.projectCode || projectId.value}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    // Try alternative - build xlsx from data
    const res = await inspectionApi.list({ projectId: projectId.value, method: filterMethod.value, size: 10000 })
    if (res?.data?.records) {
      exportToExcel(res.data.records)
    }
  } finally {
    loading.value = false
  }
}

function exportToExcel(records) {
  const headers = columns.value.filter((c) => c.field).map((c) => c.title)
  const fields = columns.value.filter((c) => c.field).map((c) => c.field)
  const data = records.map((row) => fields.map((f) => {
    const val = row[f]
    if (f === 'defectPositions') return val || ''
    if (f === 'inspectionDate' || f === 'instructionDate' || f === 'createTime') {
      return val ? val.substring(0, 10) : ''
    }
    return val ?? ''
  }))
  const ws = XLSX.utils.aoa_to_sheet([headers, ...data])
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '检测数据')
  XLSX.writeFile(wb, `检测数据_${project.value?.projectCode || projectId.value}.xlsx`)
  ElMessage.success('导出成功')
}

// ─── Import ───
const importRef = ref(null)
function handleImportClick() {
  importRef.value?.click()
}

async function handleImportFile(e) {
  const file = e.target.files[0]
  if (!file) return

  try {
    const data = await file.arrayBuffer()
    const wb = XLSX.read(data, { type: 'array' })
    const ws = wb.Sheets[wb.SheetNames[0]]
    const jsonData = XLSX.utils.sheet_to_json(ws, { header: 1 })

    if (jsonData.length < 2) {
      ElMessage.warning('Excel 文件为空或格式不正确')
      return
    }

    // Map headers to field names
    const headers = jsonData[0]
    const fieldMap = buildFieldMap(headers)
    const records = jsonData.slice(1)
      .filter((row) => row.some((cell) => cell != null && cell !== ''))
      .map((row) => {
        const record = { projectId: projectId.value }
        headers.forEach((h, i) => {
          const field = fieldMap[h]
          if (field && row[i] != null && row[i] !== '') {
            record[field] = row[i]
          }
        })
        return record
      })

    await ElMessageBox.confirm(
      `识别到 ${records.length} 条数据，确认导入？`,
      '导入确认',
      { confirmButtonText: '确认导入', cancelButtonText: '取消', type: 'info' }
    )

    await inspectionApi.batchImport(projectId.value, records)
    ElMessage.success(`成功导入 ${records.length} 条数据`)
    loadData()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('导入失败，请检查文件格式')
  } finally {
    e.target.value = ''
  }
}

function buildFieldMap(headers) {
  const mapping = {}
  const colDefs = columns.value.filter((c) => c.field)
  headers.forEach((h) => {
    if (!h) return
    const found = colDefs.find((c) => c.title === String(h).trim())
    if (found) mapping[h] = found.field
  })
  return mapping
}

// ─── Navigation ───
function goBack() {
  router.push(`/project/${projectId.value}/detail`)
}

// ─── Defect positions editing ───
const defectDialogVisible = ref(false)
const defectEditRow = ref(null)
const defectForm = ref([])

function openDefectEditor(row) {
  defectEditRow.value = row
  defectForm.value = Array.from({ length: 15 }, (_, i) => {
    const existing = row._defectPositions?.find((d) => d.pos === i + 1)
    return {
      pos: i + 1,
      defect: existing?.defect || '',
      length: existing?.length || '',
      level: existing?.level || '',
      other: existing?.other || '',
    }
  })
  defectDialogVisible.value = true
}

async function saveDefects() {
  if (!defectEditRow.value) return
  const valid = defectForm.value.filter((d) => d.defect || d.level || d.other)
  defectEditRow.value._defectPositions = valid
  defectEditRow.value.defectPositions = JSON.stringify(valid)
  defectDialogVisible.value = false
  // Save row via API
  saving.value = true
  try {
    if (defectEditRow.value.id) {
      await inspectionApi.update(defectEditRow.value.id, { ...defectEditRow.value, projectId: projectId.value })
    }
  } catch { /* handled by interceptor */ }
  finally { saving.value = false }
}

// ─── Init ───
onMounted(async () => {
  await loadProject()
  await loadData()
})
</script>

<template>
  <div class="inspection-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" link @click="goBack">返回项目详情</el-button>
        <div>
          <h2 class="page-title">
            {{ project?.projectName || '检测数据录入' }}
            <el-tag v-if="project" :type="project?.status === 'COMPLETED' ? 'success' : 'primary'" size="small" class="project-tag">
              {{ project?.projectCode }}
            </el-tag>
          </h2>
          <p class="page-subtitle">点击行进入编辑 · 离开行自动保存 · Tab 切换单元格 · 末行自动追加</p>
        </div>
      </div>
      <div class="header-actions">
        <span class="save-indicator" v-if="saving">
          <el-icon class="is-loading"><Loading /></el-icon> 保存中...
        </span>
        <span class="save-indicator saved" v-else>
          <el-icon><Check /></el-icon> 已自动保存
        </span>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="filterMethod"
          placeholder="检测方法"
          clearable
          style="width: 130px"
          @change="loadData"
        >
          <el-option v-for="m in methodTypes" :key="m" :label="m" :value="m" />
        </el-select>
        <el-input
          v-model="filterKeyword"
          placeholder="搜索焊口编号..."
          :prefix-icon="Search"
          clearable
          style="width: 200px"
          @keyup.enter="loadData"
          @clear="loadData"
        />
        <el-button :icon="Search" @click="loadData">查询</el-button>
      </div>
      <div class="toolbar-right">
        <el-button :icon="Upload" @click="handleImportClick">导入Excel</el-button>
        <input ref="importRef" type="file" accept=".xlsx,.xls" style="display: none" @change="handleImportFile" />
        <el-button :icon="Download" @click="handleExport">导出Excel</el-button>
        <el-popover placement="bottom-end" :width="300" trigger="click">
          <template #reference>
            <el-button :icon="Setting" circle />
          </template>
          <div class="column-settings">
            <p class="settings-title">显示列</p>
            <el-checkbox-group v-model="visibleColumns" @change="updateColumnVisibility">
              <div v-for="col in dataColumns" :key="col.field" class="column-check-item">
                <el-checkbox :label="col.field" :value="col.field">
                  {{ col.title }}
                </el-checkbox>
              </div>
            </el-checkbox-group>
          </div>
        </el-popover>
      </div>
    </div>

    <!-- Vxe Table -->
    <div class="table-wrap" v-loading="loading">
      <vxe-grid
        v-bind="$attrs"
        :data="tableData"
        :columns="columns"
        :edit-config="editConfig"
        :edit-rules="editRules"
        :column-config="{ resizable: true }"
        height="100%"
        :row-config="{ isHover: true }"
        :toolbar-config="{ enabled: false }"
        :custom-config="{ storage: true }"
        size="mini"
        stripe
        border
        show-overflow
        @edit-actived="handleEditActived"
        @edit-closed="handleEditClosed"
      >
        <!-- Action column slot -->
        <template #defect_edit_default="{ row }">
          <el-button link type="warning" :icon="Grid" size="small" @click="openDefectEditor(row)">
            编辑
          </el-button>
        </template>

        <template #operate_default="{ row }">
          <div class="action-cell">
            <el-button link type="danger" :icon="Delete" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </div>
        </template>

        <!-- Inspection method render -->
        <template #inspectionMethod_default="{ row }">
          <el-tag
            :type="row.inspectionMethod === 'RT' ? 'danger' : row.inspectionMethod === 'UT' ? 'primary' : 'success'"
            size="small"
            effect="dark"
          >
            {{ row.inspectionMethod || '-' }}
          </el-tag>
        </template>

        <!-- Conclusion render -->
        <template #inspectionConclusion_default="{ row }">
          <el-tag
            :type="row.inspectionConclusion === '合格' ? 'success' : 'danger'"
            size="small"
            effect="light"
          >
            {{ row.inspectionConclusion || '-' }}
          </el-tag>
        </template>

        <!-- Date render -->
        <template #inspectionDate_default="{ row }">
          {{ row.inspectionDate ? row.inspectionDate.substring(0, 10) : '-' }}
        </template>

        <template #instructionDate_default="{ row }">
          {{ row.instructionDate ? row.instructionDate.substring(0, 10) : '-' }}
        </template>
      </vxe-grid>
    </div>

    <!-- Defect Position Dialog -->
    <el-dialog
      v-model="defectDialogVisible"
      title="缺陷位置编辑（最多15组）"
      width="800px"
      destroy-on-close
      @close="saveDefects"
    >
      <el-table :data="defectForm" border size="small" max-height="450">
        <el-table-column label="位置" width="65" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.pos }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="缺陷" width="160">
          <template #default="{ row }">
            <el-input v-model="row.defect" size="small" placeholder="缺陷描述" clearable />
          </template>
        </el-table-column>
        <el-table-column label="长度(mm)" width="100">
          <template #default="{ row }">
            <el-input v-model="row.length" size="small" type="number" placeholder="0" />
          </template>
        </el-table-column>
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <el-select v-model="row.level" size="small" clearable placeholder="级别">
              <el-option v-for="l in levelOptions" :key="l" :label="l" :value="l" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="其他缺陷及长度" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.other" size="small" placeholder="其他缺陷描述" clearable />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="defectDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="saveDefects">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.inspection-page {
  animation: fadeIn 0.3s ease;
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--header-height) - 48px);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Header */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.project-tag {
  margin-left: 8px;
  vertical-align: middle;
}

.page-subtitle {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 2px 0 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.save-indicator {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-warning);
}

.save-indicator.saved {
  color: var(--color-success);
}

/* Toolbar */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Table — fill viewport */
.table-wrap {
  flex: 1;
  overflow: hidden;
  border-radius: var(--radius-md);
  background: #fff;
  border: 1px solid var(--color-border);
  min-height: 0;
}

.table-wrap :deep(.vxe-grid) {
  height: 100%;
}

.table-wrap :deep(.vxe-table) {
  border-radius: var(--radius-md);
}

.table-wrap :deep(.vxe-table .vxe-body--column) {
  padding: 2px 4px;
  font-size: 13px;
}

.table-wrap :deep(.vxe-table .vxe-header--column) {
  font-size: 12px;
  font-weight: 600;
  background: var(--color-bg);
  position: sticky;
  top: 0;
  z-index: 2;
}

.action-cell {
  display: flex;
  gap: 4px;
}

/* Column settings */
.column-settings {
  max-height: 400px;
  overflow-y: auto;
}

.settings-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 8px;
}

.column-check-item {
  padding: 2px 0;
}
</style>
