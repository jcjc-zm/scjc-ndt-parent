<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Download, Search, Delete, Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { processCardApi } from '@/api/processCard'
import { projectApi } from '@/api/project'
import jspreadsheet from 'jspreadsheet-ce'
import 'jsuites/dist/jsuites.css'
import 'jspreadsheet-ce/dist/jspreadsheet.css'
import * as XLSX from 'xlsx'

const userStore = useUserStore()

// ─── Project selector ───
const selectedProjectId = ref(null)
const projectList = ref([])
const projectsLoading = ref(false)

async function loadProjects() {
  projectsLoading.value = true
  try {
    const res = await projectApi.list({ page: 1, size: 1000 })
    if (res?.data) {
      projectList.value = res.data.records || []
    }
  } catch { /* ignore */ }
  finally { projectsLoading.value = false }
}

function onProjectChange(projectId) {
  selectedProjectId.value = projectId
  if (projectId) {
    loadData()
  } else {
    tableData = []
    refreshSheet()
  }
}

const project = computed(() =>
  projectList.value.find((p) => p.id === selectedProjectId.value) || null
)

// ─── Column definitions (33 RT process card columns) ───
const columnDefs = [
  { field: 'processCardNo', title: '工艺卡编号', width: 130, type: 'text' },
  { field: 'specification', title: '规格', width: 100, type: 'text' },
  { field: 'techniqueType', title: '透照方式', width: 100, type: 'text' },
  { field: 'iqiValue', title: '像质指数', width: 80, type: 'text' },
  { field: 'iqiModel', title: '像质计型号', width: 110, type: 'text' },
  { field: 'equipmentModel', title: '射线机型号', width: 110, type: 'text' },
  { field: 'equipmentNo', title: '射线机编号', width: 110, type: 'text' },
  { field: 'focalSpotSize', title: '焦点尺寸(mm)', width: 105, type: 'text' },
  { field: 'iqiPosition', title: '像质计位置', width: 110, type: 'text' },
  { field: 'focalDistance', title: '焦距(mm)', width: 85, type: 'text' },
  { field: 'penetrationThickness', title: '透照厚度(mm)', width: 110, type: 'text' },
  { field: 'tubeVoltage', title: '管电压(KV)', width: 95, type: 'text' },
  { field: 'exposureTime', title: '曝光时间(min)', width: 105, type: 'text' },
  { field: 'transilluminationLength', title: '一次透照长度(mm)', width: 135, type: 'text' },
  { field: 'exposureCount', title: '曝光次数', width: 85, type: 'numeric' },
  { field: 'filmSpec', title: '胶片规格(mm)', width: 105, type: 'text' },
  { field: 'densityRange', title: '黑度范围', width: 95, type: 'text' },
  { field: 'tubeCurrent', title: '管电流', width: 80, type: 'text' },
  { field: 'filmType', title: '胶片类型', width: 90, type: 'text' },
  { field: 'workFilmDistance', title: '工件-胶片距离', width: 115, type: 'text' },
  { field: 'techLevel', title: '技术等级', width: 90, type: 'text' },
  { field: 'heatTreatmentStatus', title: '热处理状态', width: 100, type: 'text' },
  { field: 'leadScreen', title: '铅增感屏', width: 90, type: 'text' },
  { field: 'sourceType', title: '源的种类', width: 90, type: 'text' },
  { field: 'exposureParam', title: '曝光参数(mA)', width: 110, type: 'text' },
  { field: 'transilluminationParam', title: '透照参数', width: 120, type: 'text' },
  { field: 'filmProcessing', title: '胶片处理方式', width: 110, type: 'text' },
  { field: 'developmentTime', title: '显影时间', width: 85, type: 'text' },
  { field: 'developmentTemperature', title: '显影温度', width: 85, type: 'text' },
  { field: 'sourceActivity', title: '源活度', width: 80, type: 'text' },
  { field: 'filmCount', title: '片子张数', width: 85, type: 'numeric' },
  { field: 'remark', title: '备注', width: 150, type: 'text' },
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
const filterKeyword = ref('')

// ─── jspreadsheet instance ───
const spreadsheetEl = ref(null)
let worksheet = null
let tableData = []
let dirtyRowIndices = new Set()
let isInternalUpdate = false
let lastSelectedRow = -1
let lastSelectedCol = -1

// ─── Data loading ───
const loading = ref(false)

async function loadData(silent = false) {
  if (!selectedProjectId.value) return
  if (!silent) loading.value = true
  try {
    const params = { projectId: selectedProjectId.value, size: 100000 }
    if (filterKeyword.value) params.keyword = filterKeyword.value
    const res = await processCardApi.list(params)
    if (res?.data) {
      tableData = (res.data.records || []).map((row) => ({ ...row }))
    } else {
      tableData = []
    }
    refreshSheet()
  } finally {
    if (!silent) loading.value = false
  }
}

// ─── Initialize / refresh the spreadsheet ───
function getRowData(rowObj) {
  return columnDefs.map((col) => {
    const val = rowObj[col.field]
    if (val == null) return ''
    return val
  })
}

const ROW_HEIGHT = 26
const MIN_TOTAL_ROWS = 2000
const SCROLL_EXTEND_ROWS = 200
const SCROLL_THRESHOLD = 400

function buildSheetData() {
  const data = tableData.map((row) => getRowData(row))
  while (data.length < MIN_TOTAL_ROWS) {
    data.push(Array(columnDefs.length).fill(''))
  }
  return data
}

function refreshSheet() {
  if (!worksheet) return
  isInternalUpdate = true
  worksheet.setData(buildSheetData())
  dirtyRowIndices.clear()
  dirtyCount.value = 0
  nextTick(() => {
    isInternalUpdate = false
  })
}

// ─── Infinite scroll ───
let infiniteScrollPending = false
let scrollPollTimer = null

function tryExtendSheet() {
  if (!worksheet || infiniteScrollPending) return
  const wrapEl = spreadsheetEl.value?.parentElement
  if (!wrapEl) return
  if (wrapEl.scrollHeight - wrapEl.scrollTop - wrapEl.clientHeight < SCROLL_THRESHOLD) {
    infiniteScrollPending = true
    isInternalUpdate = true
    try {
      worksheet.insertRow(SCROLL_EXTEND_ROWS)
    } catch { /* ignore */ }
    isInternalUpdate = false
    infiniteScrollPending = false
  }
}

function onSheetScroll() {
  tryExtendSheet()
}

function startScrollPolling() {
  stopScrollPolling()
  scrollPollTimer = setInterval(tryExtendSheet, 400)
}

function stopScrollPolling() {
  if (scrollPollTimer) {
    clearInterval(scrollPollTimer)
    scrollPollTimer = null
  }
}

function initSpreadsheet() {
  if (!spreadsheetEl.value || worksheet) return

  try {
    const spreadsheet = jspreadsheet(spreadsheetEl.value, {
      worksheets: [{
        data: buildSheetData(),
        columns: jsColumns.value,
        freezeColumns: 2,
        minSpareRows: 0,
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
      }]
    })

    let instance = null
    if (spreadsheet && typeof spreadsheet.insertRow === 'function') {
      instance = spreadsheet
    } else if (spreadsheet?.current) {
      instance = Array.isArray(spreadsheet.current) ? spreadsheet.current[0] : spreadsheet.current
    }
    if (!instance || typeof instance.insertRow !== 'function') {
      const childInst = spreadsheetEl.value?.querySelector?.('.jss_container')?.jspreadsheet
      if (childInst && typeof childInst.insertRow === 'function') {
        instance = childInst
      }
    }
    if (!instance || typeof instance.insertRow !== 'function') {
      instance = spreadsheetEl.value?.jspreadsheet
    }

    if (instance && typeof instance.insertRow === 'function') {
      worksheet = instance
    }

    nextTick(() => {
      const wrapEl = spreadsheetEl.value?.parentElement
      if (wrapEl) {
        wrapEl.addEventListener('scroll', onSheetScroll, { passive: true })
      }
      const container = spreadsheetEl.value
      if (container) {
        container.addEventListener('click', onSheetClick, { passive: true })
        container.addEventListener('keydown', onSheetKeyDown, { passive: true })
      }
      startScrollPolling()
    })
  } catch (err) {
    console.error('jspreadsheet init failed:', err)
  }
}

// ─── Cell change handler ───
function handleCellChange(instance, cell, colIndex, rowIndex, newValue, oldValue) {
  if (isInternalUpdate) return
  const rowIdx = typeof rowIndex === 'string' ? parseInt(rowIndex) : rowIndex
  dirtyRowIndices.add(rowIdx)
  dirtyCount.value = dirtyRowIndices.size
}

// ─── Track selected cell ───
function onSheetClick(e) {
  if (!touched.value) touched.value = true
  const td = e.target.closest('td')
  if (!td) return
  const tr = td.closest('tr')
  if (!tr) return
  const tbody = tr.closest('tbody')
  if (!tbody) return
  const rows = Array.from(tbody.querySelectorAll('tr'))
  const cells = Array.from(tr.querySelectorAll('td'))
  lastSelectedRow = rows.indexOf(tr)
  lastSelectedCol = cells.indexOf(td) - 1

  setTimeout(() => {
    ensureActiveCellVisible()
  }, 60)
}
const touched = ref(false)

function ensureActiveCellVisible() {
  if (!spreadsheetEl.value) return

  let x, y
  if (worksheet) {
    const selected = worksheet.getSelected()
    if (selected && selected.length > 0) {
      x = selected[0].x
      y = selected[0].y
    }
  }
  if (x == null || y == null) {
    x = lastSelectedCol
    y = lastSelectedRow
  }
  if (x < 0 || y < 0) return

  const table = spreadsheetEl.value.querySelector('table')
  if (!table) return
  const tbody = table.querySelector('tbody')
  if (!tbody) return
  const rows = tbody.querySelectorAll('tr')
  if (y >= rows.length) return
  const cells = rows[y].querySelectorAll('td')
  if (x + 1 >= cells.length) return

  const cell = cells[x + 1]
  const wrapEl = spreadsheetEl.value.parentElement
  if (!wrapEl) return

  const cellRect = cell.getBoundingClientRect()
  const wrapRect = wrapEl.getBoundingClientRect()

  const cellCenterY = cellRect.top + cellRect.height / 2
  const cellCenterX = cellRect.left + cellRect.width / 2
  const wrapCenterY = wrapRect.top + wrapRect.height / 2
  const wrapCenterX = wrapRect.left + wrapRect.width / 2

  let targetTop = wrapEl.scrollTop + (cellCenterY - wrapCenterY)
  let targetLeft = wrapEl.scrollLeft + (cellCenterX - wrapCenterX)

  const maxTop = wrapEl.scrollHeight - wrapEl.clientHeight
  const maxLeft = wrapEl.scrollWidth - wrapEl.clientWidth
  targetTop = Math.max(0, Math.min(targetTop, maxTop))
  targetLeft = Math.max(0, Math.min(targetLeft, maxLeft))

  wrapEl.scrollTop = targetTop
  wrapEl.scrollLeft = targetLeft
}

function onSheetKeyDown(e) {
  if (!touched.value) touched.value = true
  const navKeys = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Tab', 'Enter']
  if (navKeys.includes(e.key) && !e.ctrlKey && !e.metaKey && !e.altKey) {
    setTimeout(() => {
      ensureActiveCellVisible()
    }, 60)
  }
}

// ─── Build row object from spreadsheet row ───
function buildRowFromSheet(rowIndex) {
  if (!worksheet) return null
  const rowData = worksheet.getRowData(rowIndex)
  if (!rowData) return null
  const obj = { projectId: selectedProjectId.value }
  const intFields = ['exposureCount', 'filmCount']
  columnDefs.forEach((col, i) => {
    let val = rowData[i]
    if (val === '' || val === undefined || val === null) {
      obj[col.field] = intFields.includes(col.field) ? null : ''
    } else if (col.type === 'numeric') {
      let num = Number(val)
      if (intFields.includes(col.field)) num = Math.round(num)
      obj[col.field] = isNaN(num) ? null : num
    } else {
      obj[col.field] = val
    }
  })
  return obj
}

// ─── Save ───
const saving = ref(false)
const dirtyCount = ref(0)

async function handleSave() {
  if (!worksheet || saving.value || !selectedProjectId.value) return

  saving.value = true
  let saved = 0
  let errors = 0

  try {
    const allData = worksheet.getData(false)
    if (!allData || allData.length === 0) {
      ElMessage.info('没有需要保存的数据')
      return
    }

    for (let rowIdx = 0; rowIdx < allData.length; rowIdx++) {
      const rowData = buildRowFromSheet(rowIdx)
      if (!rowData) continue

      const hasContent = rowData.processCardNo
      if (!hasContent) continue

      try {
        const existingRow = rowIdx < tableData.length ? tableData[rowIdx] : null

        if (existingRow?.id) {
          await processCardApi.update(existingRow.id, { ...rowData, projectId: selectedProjectId.value })
          Object.assign(existingRow, rowData)
          saved++
        } else {
          const res = await processCardApi.create({ ...rowData, projectId: selectedProjectId.value })
          if (res?.data) {
            const newRow = { ...res.data }
            if (rowIdx < tableData.length) {
              tableData[rowIdx] = newRow
            } else {
              while (tableData.length <= rowIdx) tableData.push({})
              tableData[rowIdx] = newRow
            }
            saved++
          }
        }
      } catch (err) {
        console.error('Save row ' + rowIdx + ' failed:', err)
        errors++
      }
    }

    dirtyRowIndices.clear()
    dirtyCount.value = 0
    touched.value = false

    if (errors > 0) {
      ElMessage.warning(`保存完成：${saved} 条成功，${errors} 条失败`)
    } else if (saved > 0) {
      ElMessage.success(`已保存 ${saved} 条数据`)
    } else {
      ElMessage.info('数据无变化')
    }
  } finally {
    saving.value = false
  }
}

// ─── Row insert / delete ───
function handleInsertRow(instance, rows) {
  if (isInternalUpdate) return
  for (const r of rows) {
    if (r.row <= tableData.length) {
      tableData.splice(r.row, 0, { projectId: selectedProjectId.value })
    }
  }
}

async function handleDeleteRow(instance, removedRows) {
  if (isInternalUpdate) return
  const sorted = [...removedRows].sort((a, b) => b - a)
  for (const rowIdx of sorted) {
    if (rowIdx < tableData.length) {
      const existingRow = tableData[rowIdx]
      if (existingRow?.id) {
        try { await processCardApi.remove(existingRow.id) } catch { /* ignore */ }
      }
      tableData.splice(rowIdx, 1)
    }
  }
  ElMessage.success('删除成功')
}

// ─── Filter ───
async function applyFilter() {
  if (touched.value) await handleSave()
  loadData()
}

// ─── Export ───
async function handleExport() {
  if (!selectedProjectId.value) { ElMessage.warning('请先选择项目'); return }
  try {
    loading.value = true
    const res = await processCardApi.exportData({ projectId: selectedProjectId.value })
    const url = window.URL.createObjectURL(res)
    const link = document.createElement('a')
    link.href = url
    link.download = `工艺卡_${project.value?.projectCode || selectedProjectId.value}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    const res = await processCardApi.list({ projectId: selectedProjectId.value, size: 100000 })
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
    fields.map((f) => row[f] ?? '')
  )
  const ws = XLSX.utils.aoa_to_sheet([headers, ...data])
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '工艺卡')
  XLSX.writeFile(wb, `工艺卡_${project.value?.projectCode || selectedProjectId.value}.xlsx`)
  ElMessage.success('导出成功')
}

// ─── Import ───
const importRef = ref(null)

function handleImportClick() {
  if (!selectedProjectId.value) { ElMessage.warning('请先选择项目'); return }
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
        const record = { projectId: selectedProjectId.value }
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

    await processCardApi.batchImport(selectedProjectId.value, records)
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

// ─── Delete selected row ───
async function handleDeleteSelected() {
  if (!worksheet) return

  let rowIdx = lastSelectedRow
  if (rowIdx < 0) {
    const selected = worksheet.getSelected()
    if (selected && selected.length > 0) {
      rowIdx = selected[0].y
    }
  }
  if (rowIdx < 0) { ElMessage.warning('请先点击目标行中的任意单元格'); return }
  if (rowIdx >= tableData.length) { ElMessage.warning('无法删除空行'); return }
  const row = tableData[rowIdx]
  if (!row?.id) { ElMessage.warning('该行尚未保存'); return }

  try {
    await ElMessageBox.confirm(`确认删除工艺卡「${row.processCardNo}」？`, '删除确认', {
      confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning',
    })
    await processCardApi.remove(row.id)
    ElMessage.success('删除成功')
    tableData.splice(rowIdx, 1)
    refreshSheet()
  } catch { /* cancelled */ }
}

// ─── Ctrl+S handler ───
function onKeyDown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSave()
  }
}

// ─── Beforeunload ───
function onBeforeUnload(e) {
  if (touched.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

// ─── Lifecycle ───
onMounted(async () => {
  await loadProjects()
  await nextTick()
  await nextTick()
  initSpreadsheet()
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('beforeunload', onBeforeUnload)
})

onBeforeUnmount(() => {
  stopScrollPolling()
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('beforeunload', onBeforeUnload)
  const wrapEl = spreadsheetEl.value?.parentElement
  if (wrapEl) {
    wrapEl.removeEventListener('scroll', onSheetScroll)
  }
  const container = spreadsheetEl.value
  if (container) {
    container.removeEventListener('click', onSheetClick)
    container.removeEventListener('keydown', onSheetKeyDown)
  }
  worksheet = null
  if (spreadsheetEl.value) {
    spreadsheetEl.value.innerHTML = ''
  }
})

</script>

<template>
  <div class="inspection-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <div>
          <h2 class="page-title">工艺卡管理
            <el-tag v-if="project" type="primary" size="small" class="project-tag">
              {{ project.projectCode }}
            </el-tag>
          </h2>
          <p class="page-subtitle">点击单元格直接编辑 · Tab/Enter 导航 · Ctrl+S 保存 · Ctrl+C/V 复制粘贴</p>
        </div>
      </div>
      <div class="header-actions">
        <span class="save-indicator saving" v-if="saving">
          <el-icon class="is-loading"><Loading /></el-icon> 保存中...
        </span>
        <span class="save-indicator unsaved" v-else-if="touched">已修改，请保存</span>
        <span class="save-indicator saved" v-else>已保存</span>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="selectedProjectId"
          placeholder="选择项目"
          clearable
          filterable
          style="width: 220px"
          :loading="projectsLoading"
          @change="onProjectChange"
        >
          <el-option
            v-for="p in projectList"
            :key="p.id"
            :label="`${p.projectName} (${p.projectCode})`"
            :value="p.id"
          />
        </el-select>
        <el-input
          v-model="filterKeyword"
          placeholder="搜索工艺卡编号..."
          :prefix-icon="Search"
          clearable
          style="width: 200px"
          @keyup.enter="applyFilter"
          @clear="loadData"
        />
        <el-button :icon="Search" @click="applyFilter">查询</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :loading="saving" @click="handleSave">保存 (Ctrl+S)</el-button>
        <el-button :icon="Upload" @click="handleImportClick">导入Excel</el-button>
        <input ref="importRef" type="file" accept=".xlsx,.xls" style="display: none" @change="handleImportFile" />
        <el-button :icon="Download" @click="handleExport">导出Excel</el-button>
        <el-tooltip content="先点击表格中目标行的任意单元格，再点此按钮" placement="top">
          <el-button :icon="Delete" type="danger" plain @click="handleDeleteSelected">删除行</el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- Jspreadsheet container -->
    <div class="sheet-wrap">
      <div v-if="loading" class="sheet-loading-overlay">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>加载数据中...</p>
      </div>
      <div v-if="!selectedProjectId && !loading" class="sheet-placeholder">
        <el-icon :size="48"><Search /></el-icon>
        <p>请先从上方选择一个项目开始录入工艺卡</p>
      </div>
      <div ref="spreadsheetEl" class="spreadsheet-container"></div>
    </div>
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
}

.save-indicator.saving { color: var(--color-warning); }
.save-indicator.unsaved { color: var(--color-danger); font-weight: 500; }
.save-indicator.saved { color: var(--color-success); }

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

/* Sheet wrap */
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

.sheet-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-placeholder);
  gap: 12px;
  z-index: 5;
}

.spreadsheet-container {
  min-height: 300px;
}

/* Jspreadsheet overrides */
.sheet-wrap :deep(table) {
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
}

.sheet-wrap :deep(td) {
  padding: 2px 4px;
}

.sheet-wrap :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 5;
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

/* Scrollbar */
.sheet-wrap::-webkit-scrollbar { width: 12px; height: 18px; }
.sheet-wrap::-webkit-scrollbar-track { background: #f1f1f1; }
.sheet-wrap::-webkit-scrollbar-thumb { background: #b0b0b0; border-radius: 9px; }
.sheet-wrap::-webkit-scrollbar-thumb:hover { background: #888; }
.sheet-wrap::-webkit-scrollbar-corner { background: #f1f1f1; }
</style>
