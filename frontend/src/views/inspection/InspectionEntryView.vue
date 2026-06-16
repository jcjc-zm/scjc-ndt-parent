<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Upload, Download, Search, Delete, Grid, Loading, Check } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { inspectionApi } from '@/api/inspection'
import { projectApi } from '@/api/project'
import jspreadsheet from 'jspreadsheet-ce'
import 'jsuites/dist/jsuites.css'
import 'jspreadsheet-ce/dist/jspreadsheet.css'
import * as XLSX from 'xlsx'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const projectId = ref(Number(route.params.projectId))
const project = ref(null)
const loading = ref(false)
const saving = ref(false)

// ─── Column definitions ───
const methodTypes = ['RT', 'UT', 'PT', 'MT', 'AUT', 'PA', 'TOFD', 'DR']
const levelOptions = ['Ⅰ', 'Ⅱ', 'Ⅲ', 'Ⅳ']
const conclusionOptions = ['合格', '不合格']
const grooveOptions = ['V型', 'X型', 'U型', 'I型', 'K型', '双V型']
const weldMethodOptions = ['GTAW', 'SMAW', 'GMAW', 'FCAW', 'SAW']
const positionOptions = ['管口', '法兰', '弯头', '三通', '直管段']

const columnDefs = [
  { field: 'weldNo', title: '焊口编号', width: 120, type: 'text', required: true },
  { field: 'inspectionMethod', title: '检测方法', width: 90, type: 'dropdown', source: methodTypes },
  { field: 'constructionUnit', title: '施工单位', width: 130, type: 'text' },
  { field: 'instructionNo', title: '指令编号', width: 120, type: 'text' },
  { field: 'instructionDate', title: '指令日期', width: 110, type: 'text' },
  { field: 'projectName', title: '工程名称', width: 140, type: 'text' },
  { field: 'unitProjectName', title: '单位工程名称', width: 140, type: 'text' },
  { field: 'buDept', title: '所属事业部', width: 110, type: 'text' },
  { field: 'specification', title: '规格', width: 90, type: 'text' },
  { field: 'material', title: '材质', width: 90, type: 'text' },
  { field: 'grooveType', title: '坡口形式', width: 95, type: 'dropdown', source: grooveOptions },
  { field: 'position', title: '部位', width: 90, type: 'dropdown', source: positionOptions },
  { field: 'weldingMethod', title: '焊接方式', width: 95, type: 'dropdown', source: weldMethodOptions },
  { field: 'ratio', title: '比例', width: 75, type: 'text' },
  { field: 'inspectionStandard', title: '检测标准', width: 110, type: 'text' },
  { field: 'qualifiedLevel', title: '合格级别', width: 90, type: 'text' },
  { field: 'inspectionItem', title: '检测项目', width: 110, type: 'text' },
  { field: 'welderCode', title: '焊工代号', width: 90, type: 'text' },
  { field: 'weldingDept', title: '焊接部门', width: 110, type: 'text' },
  { field: 'inspectionLength', title: '检测长度(m)', width: 105, type: 'numeric', mask: '#.##' },
  { field: 'processCardNo', title: '工艺卡编号', width: 120, type: 'text' },
  { field: 'samplingInstructionNo', title: '抽检指令编号', width: 130, type: 'text' },
  { field: 'inspectionDate', title: '检测日期', width: 110, type: 'text' },
  { field: 'resultLevel', title: '级别', width: 65, type: 'dropdown', source: levelOptions },
  { field: 'inspectionConclusion', title: '检测结论', width: 95, type: 'dropdown', source: conclusionOptions },
  { field: 'unqualifiedHandling', title: '不合格处理', width: 130, type: 'text' },
  { field: 'reportDefectPosition', title: '缺陷位置', width: 110, type: 'text' },
  { field: 'reportDefectNature', title: '缺陷性质', width: 110, type: 'text' },
  { field: 'reportDefectLength', title: '缺陷长度', width: 90, type: 'numeric', mask: '#.##' },
  { field: 'unqualifiedDefectType', title: '不合格缺陷类型', width: 140, type: 'text' },
  { field: 'remark', title: '备注', width: 140, type: 'text' },
  { field: 'inspectorName', title: '检测人员', width: 90, type: 'text' },
  { field: 'boxNo', title: '箱号', width: 90, type: 'text' },
  { field: 'filmLength', title: '底片长度', width: 90, type: 'numeric', mask: '#.##' },
  { field: 'filmCount', title: '底片张数', width: 85, type: 'numeric', mask: '#' },
  { field: 'levelI', title: 'Ⅰ级', width: 60, type: 'numeric', mask: '#' },
  { field: 'levelIi', title: 'Ⅱ级', width: 60, type: 'numeric', mask: '#' },
  { field: 'levelIii', title: 'Ⅲ级', width: 60, type: 'numeric', mask: '#' },
  { field: 'levelIv', title: 'Ⅳ级', width: 60, type: 'numeric', mask: '#' },
]

// Convert to jspreadsheet column format
const jsColumns = computed(() =>
  columnDefs.map((col) => ({
    name: col.field,
    title: col.title,
    width: col.width,
    type: col.type === 'dropdown' ? 'dropdown' :
          col.type === 'numeric' ? 'numeric' : 'text',
    source: col.source || undefined,
    mask: col.mask || undefined,
  }))
)

// ─── Filter state ───
const filterMethod = ref('')
const filterKeyword = ref('')

// ─── jspreadsheet instance ───
const spreadsheetEl = ref(null)
let worksheet = null
let tableData = []       // our source of truth: array of row objects
let saveTimer = null
let dirtyRowIndices = new Set()
let isInternalUpdate = false  // flag to prevent onchange loops during setData
let ignoreNextChanges = false // skip onchange during programmatic updates

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

async function loadData(silent = false) {
  if (!silent) loading.value = true
  try {
    const params = { projectId: projectId.value, size: 100000 }
    if (filterMethod.value) params.method = filterMethod.value
    if (filterKeyword.value) params.keyword = filterKeyword.value
    const res = await inspectionApi.list(params)
    if (res?.data) {
      tableData = (res.data.records || []).map((row) => ({
        ...row,
        _defectPositions: tryParseJson(row.defectPositions),
      }))
    } else {
      tableData = []
    }
    refreshSheet()
  } finally {
    if (!silent) loading.value = false
  }
}

function tryParseJson(val) {
  if (!val) return []
  if (typeof val === 'object') return val
  try { return JSON.parse(val) } catch { return [] }
}

// ─── Initialize / refresh the spreadsheet ───
function getRowData(rowObj) {
  return columnDefs.map((col) => {
    const val = rowObj[col.field]
    if (val == null) return ''
    if (col.field === 'inspectionDate' || col.field === 'instructionDate') {
      return typeof val === 'string' ? val.substring(0, 10) : val
    }
    return val
  })
}

const ROW_HEIGHT = 26

// Only pass real data — minSpareRows auto-fills empty rows below
function buildSheetData() {
  return tableData.map((row) => getRowData(row))
}

function refreshSheet() {
  if (!worksheet) return
  isInternalUpdate = true
  ignoreNextChanges = true
  worksheet.setData(buildSheetData())
  dirtyRowIndices.clear()
  nextTick(() => {
    isInternalUpdate = false
    ignoreNextChanges = false
  })
}

// ─── Infinite scroll: auto-append rows when user scrolls near bottom ───
let scrollContainer = null
let infiniteScrollPending = false

function onSheetScroll() {
  if (!scrollContainer || !worksheet || infiniteScrollPending) return
  const { scrollTop, scrollHeight, clientHeight } = scrollContainer
  // When within 150px of the bottom, add more rows
  if (scrollTop + clientHeight >= scrollHeight - 150) {
    infiniteScrollPending = true
    isInternalUpdate = true
    try {
      worksheet.insertRow(200)
    } catch { /* ignore */ }
    isInternalUpdate = false
    infiniteScrollPending = false
  }
}

function initSpreadsheet() {
  if (!spreadsheetEl.value || worksheet) return

  // Fill initial viewport: enough spare rows so table exceeds container height
  const containerH = spreadsheetEl.value.parentElement?.clientHeight || 600
  const viewportRows = Math.max(20, Math.ceil(containerH / ROW_HEIGHT) + 10)

  try {
    jspreadsheet(spreadsheetEl.value, {
      worksheets: [{
        data: buildSheetData(),
        columns: jsColumns.value,
        freezeColumns: 2,
        minSpareRows: viewportRows,
        allowInsertRow: true,
        allowDeleteRow: true,
        allowInsertColumn: false,
        allowDeleteColumn: false,
        allowRenameColumn: false,
        columnSorting: false,
        columnDrag: false,
        tableOverflow: false,
        defaultRowHeight: ROW_HEIGHT,
        defaultColWidth: 90,
        editable: true,
        allowComments: false,
        onchange: handleCellChange,
        ondeleterow: handleDeleteRow,
        oninsertrow: handleInsertRow,
        onblur: handleTableBlur,
      }]
    })

    // Get worksheet instance
    const instance = spreadsheetEl.value.jspreadsheet
    if (instance) {
      worksheet = instance.current ? instance.current[0] : instance
    }

    // Set up infinite scroll listener on the scroll container (.sheet-wrap)
    scrollContainer = spreadsheetEl.value.parentElement
    if (scrollContainer) {
      scrollContainer.addEventListener('scroll', onSheetScroll, { passive: true })
    }
  } catch (err) {
    console.error('jspreadsheet init failed:', err)
  }
}

// ─── Table blur handler — flush all pending saves ───
function handleTableBlur(instance) {
  if (saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = null
  }
  flushDirtyRows()
}

// ─── Cell change handler ───
function handleCellChange(instance, cell, colIndex, rowIndex, newValue, oldValue) {
  if (isInternalUpdate || ignoreNextChanges) return

  const colIdx = typeof colIndex === 'string' ? parseInt(colIndex) : colIndex
  const rowIdx = typeof rowIndex === 'string' ? parseInt(rowIndex) : rowIndex
  const colDef = columnDefs[colIdx]
  if (!colDef) return

  // Mark row as dirty
  dirtyRowIndices.add(rowIdx)

  // Debounce save
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    flushDirtyRows()
  }, 600)
}

function flushDirtyRows() {
  const rows = [...dirtyRowIndices]
  dirtyRowIndices.clear()
  for (const rowIdx of rows) {
    saveRow(rowIdx)
  }
}

// ─── Build row object from spreadsheet row ───
function buildRowFromSheet(rowIndex) {
  if (!worksheet) return null
  const rowData = worksheet.getRowData(rowIndex)
  const obj = { projectId: projectId.value }
  columnDefs.forEach((col, i) => {
    let val = rowData[i]
    if (val === '' || val === undefined || val === null) {
      obj[col.field] = col.field === 'inspectionLength' || col.field === 'filmLength' ||
        col.field === 'filmCount' || col.field.startsWith('level') ? null : ''
    } else if (col.type === 'numeric') {
      obj[col.field] = val === '' ? null : Number(val)
    } else {
      obj[col.field] = val
    }
  })
  return obj
}

// ─── Save a single row ───
let savingRows = new Set()

async function saveRow(rowIndex) {
  if (savingRows.has(rowIndex)) return

  const rowData = buildRowFromSheet(rowIndex)
  if (!rowData) return

  // Skip completely empty rows
  const hasContent = rowData.weldNo || rowData.inspectionMethod || rowData.inspectionDate
  if (!hasContent) return

  // Require at least weldNo for new records
  if (!rowData.weldNo) return

  savingRows.add(rowIndex)
  saving.value = true

  try {
    const existingRow = rowIndex < tableData.length ? tableData[rowIndex] : null

    if (existingRow?.id) {
      // Update existing record
      await inspectionApi.update(existingRow.id, { ...rowData, projectId: projectId.value })
      // Update local cache
      Object.assign(existingRow, rowData)
    } else {
      // Create new record
      const res = await inspectionApi.create({ ...rowData, projectId: projectId.value })
      if (res?.data) {
        const newRow = { ...res.data, _defectPositions: [] }
        if (rowIndex < tableData.length) {
          tableData[rowIndex] = newRow
        } else {
          // Expand tableData to include this row
          while (tableData.length <= rowIndex) tableData.push({})
          tableData[rowIndex] = newRow
        }
      }
    }
  } catch (err) {
    console.error('Save failed:', err)
  } finally {
    savingRows.delete(rowIndex)
    saving.value = savingRows.size > 0
  }
}

// ─── Row insert handler ───
function handleInsertRow(instance, rows) {
  if (isInternalUpdate) return
  // Sync tableData with inserted rows
  // rows: [{ row: number, data: CellValue[] }]
  for (const r of rows) {
    if (r.row <= tableData.length) {
      tableData.splice(r.row, 0, { projectId: projectId.value })
    }
  }
}

// ─── Row delete handler ───
async function handleDeleteRow(instance, removedRows) {
  if (isInternalUpdate || ignoreNextChanges) return

  // Process in descending order to maintain valid indices
  const sorted = [...removedRows].sort((a, b) => b - a)
  for (const rowIdx of sorted) {
    if (rowIdx < tableData.length) {
      const existingRow = tableData[rowIdx]
      if (existingRow?.id) {
        try {
          await inspectionApi.remove(existingRow.id)
        } catch {
          // Error handled by interceptor
        }
      }
      tableData.splice(rowIdx, 1)
    }
  }
  ElMessage.success('删除成功')
}

// ─── Filter ───
async function applyFilter() {
  // Flush pending saves before reloading
  if (saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = null
  }
  flushDirtyRows()
  // Wait for saves to complete
  await nextTick()
  loadData()
}

// ─── Export ───
async function handleExport() {
  try {
    loading.value = true
    const res = await inspectionApi.exportData({ projectId: projectId.value, method: filterMethod.value })
    const url = window.URL.createObjectURL(res)
    const link = document.createElement('a')
    link.href = url
    link.download = `检测数据_${project.value?.projectCode || projectId.value}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    // Fallback: build from loaded data
    const res = await inspectionApi.list({ projectId: projectId.value, method: filterMethod.value, size: 100000 })
    if (res?.data?.records) {
      exportToExcel(res.data.records)
    }
  } finally {
    loading.value = false
  }
}

function exportToExcel(records) {
  const headers = columnDefs.map((c) => c.title)
  const fields = columnDefs.map((c) => c.field)
  const data = records.map((row) =>
    fields.map((f) => {
      const val = row[f]
      if (f === 'defectPositions') return val || ''
      if (f === 'inspectionDate' || f === 'instructionDate' || f === 'createTime') {
        return val ? val.substring(0, 10) : ''
      }
      return val ?? ''
    })
  )
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

    const headers = jsonData[0]
    const fieldMap = buildFieldMap(headers)
    const records = jsonData
      .slice(1)
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

    if (records.length === 0) {
      ElMessage.warning('未识别到有效数据')
      return
    }

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
  headers.forEach((h) => {
    if (!h) return
    const found = columnDefs.find((c) => c.title === String(h).trim())
    if (found) mapping[h] = found.field
  })
  return mapping
}

// ─── Navigation ───
function goBack() {
  router.push(`/project/${projectId.value}/detail`)
}

// ─── Defect position editing ───
const defectDialogVisible = ref(false)
const defectEditRowIndex = ref(-1)
const defectForm = ref([])

function openDefectEditor() {
  if (!worksheet) return
  const selected = worksheet.getSelected()
  if (!selected || selected.length === 0) {
    ElMessage.warning('请先在表格中选中要编辑缺陷的行')
    return
  }
  // getSelected() returns [{ element, x, y }] — x=col, y=row
  const rowIdx = selected[0].y
  if (rowIdx >= tableData.length) {
    ElMessage.warning('请选择已有数据的行')
    return
  }
  const row = tableData[rowIdx]
  if (!row) return

  defectEditRowIndex.value = rowIdx
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
  if (defectEditRowIndex.value < 0 || defectEditRowIndex.value >= tableData.length) return
  const row = tableData[defectEditRowIndex.value]
  if (!row) return

  const valid = defectForm.value.filter((d) => d.defect || d.level || d.other)
  row._defectPositions = valid
  row.defectPositions = JSON.stringify(valid)
  defectDialogVisible.value = false

  if (row.id) {
    saving.value = true
    try {
      await inspectionApi.update(row.id, { defectPositions: row.defectPositions, projectId: projectId.value })
    } catch { /* handled by interceptor */ }
    finally { saving.value = false }
  }
}

// ─── Delete selected row ───
async function handleDeleteSelected() {
  if (!worksheet) return
  const selected = worksheet.getSelected()
  if (!selected || selected.length === 0) {
    ElMessage.warning('请先选中要删除的行')
    return
  }
  const rowIdx = selected[0].y
  if (rowIdx >= tableData.length) {
    ElMessage.warning('无法删除空行')
    return
  }
  const row = tableData[rowIdx]
  if (!row?.id) {
    ElMessage.warning('该行尚未保存')
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
    tableData.splice(rowIdx, 1)
    refreshSheet()
  } catch { /* cancelled */ }
}

// ─── Lifecycle ───
onMounted(async () => {
  await loadProject()
  await loadData()
  // Wait for DOM to fully settle before initializing spreadsheet
  await nextTick()
  await nextTick()
  initSpreadsheet()
  // Listen for resize to keep table height in sync
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  // Flush pending saves
  if (saveTimer) {
    clearTimeout(saveTimer)
    flushDirtyRows()
  }
  window.removeEventListener('resize', handleResize)
  if (scrollContainer) {
    scrollContainer.removeEventListener('scroll', onSheetScroll)
    scrollContainer = null
  }
  // Clean up
  worksheet = null
  if (spreadsheetEl.value) {
    spreadsheetEl.value.innerHTML = ''
  }
})

let resizeTimer = null
function handleResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    if (worksheet && spreadsheetEl.value?.parentElement) {
      const h = spreadsheetEl.value.parentElement.clientHeight
      if (h > 0) {
        isInternalUpdate = true
        try {
          worksheet.setData(buildSheetData())
        } catch { /* ignore */ }
        isInternalUpdate = false
      }
    }
  }, 200)
}
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
          <p class="page-subtitle">点击单元格直接编辑 · Tab/Enter 导航 · 自动保存 · Ctrl+C/V 复制粘贴</p>
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
        <el-button :icon="Grid" @click="openDefectEditor">编辑缺陷</el-button>
        <el-button :icon="Delete" type="danger" plain @click="handleDeleteSelected">删除行</el-button>
      </div>
    </div>

    <!-- Jspreadsheet container (always rendered, loading overlay on top) -->
    <div class="sheet-wrap">
      <div v-if="loading" class="sheet-loading-overlay">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>加载数据中...</p>
      </div>
      <div ref="spreadsheetEl" class="spreadsheet-container"></div>
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
  height: calc(100vh - var(--header-height) - 16px);
  padding: 0 8px;
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
  margin-bottom: 8px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.page-title {
  font-size: 18px;
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
  padding: 8px 0;
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

/* Spreadsheet — native browser scrollbars (bottom + right), fills viewport */
.sheet-wrap {
  flex: 1 1 auto;
  min-height: 300px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: auto;
  background: #fff;
  position: relative;
}

.sheet-loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  gap: 8px;
  background: rgba(255, 255, 255, 0.9);
  z-index: 10;
}

.spreadsheet-container {
  min-height: 300px;
}

/* Override jspreadsheet styles for better integration */
.sheet-wrap :deep(table) {
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
}

.sheet-wrap :deep(td) {
  padding: 2px 4px;
}

.sheet-wrap :deep(thead td) {
  background: #f0f0f0 !important;
  font-weight: 600;
  font-size: 12px;
  color: #333;
  text-align: center;
  padding: 4px 2px;
}

.sheet-wrap :deep(thead td:first-child),
.sheet-wrap :deep(tbody td:first-child) {
  text-align: center;
  background: #f5f5f5;
  color: #888;
  font-size: 11px;
  min-width: 30px;
  width: 30px;
}

.sheet-wrap :deep(tbody tr:nth-child(even) td) {
  background-color: #fafbfc;
}

.sheet-wrap :deep(.jspreadsheet) {
  border: none !important;
}

.sheet-wrap :deep(.jspreadsheet select),
.sheet-wrap :deep(.jspreadsheet input) {
  border: none !important;
  box-shadow: none !important;
  border-radius: 0 !important;
}

/* Green selection like Excel */
.sheet-wrap :deep(.jspreadsheet .highlight) {
  background-color: #e8f0fe !important;
}

.sheet-wrap :deep(.jspreadsheet .highlight-top) {
  border-top: 2px solid #217346 !important;
}

.sheet-wrap :deep(.jspreadsheet .highlight-bottom) {
  border-bottom: 2px solid #217346 !important;
}

.sheet-wrap :deep(.jspreadsheet .highlight-left) {
  border-left: 2px solid #217346 !important;
}

.sheet-wrap :deep(.jspreadsheet .highlight-right) {
  border-right: 2px solid #217346 !important;
}
</style>
