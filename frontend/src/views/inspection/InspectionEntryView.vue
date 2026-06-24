<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Upload, Download, Search, Delete, Grid, Loading } from '@element-plus/icons-vue'
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
const dirtyCount = ref(0)  // number of rows with unsaved changes

// ─── Column definitions ───
const methodTypes = ['RT', 'UT', 'PT', 'MT', 'AUT', 'PA', 'TOFD', 'DR']
const levelOptions = ['Ⅰ', 'Ⅱ', 'Ⅲ', 'Ⅳ']
const conclusionOptions = ['合格', '不合格']
const grooveOptions = ['V型', 'X型', 'U型', 'I型', 'K型', '双V型']
const weldMethodOptions = ['GTAW', 'SMAW', 'GMAW', 'FCAW', 'SAW']
const positionOptions = ['管口', '法兰', '弯头', '三通', '直管段']

const columnDefs = [
  { field: 'weldNo', title: '焊口编号', width: 120, type: 'text', required: true },
  { field: 'inspectionMethod', title: '检测方法', width: 90, type: 'text' },
  { field: 'constructionUnit', title: '施工单位', width: 130, type: 'text' },
  { field: 'instructionNo', title: '指令编号', width: 120, type: 'text' },
  { field: 'instructionDate', title: '指令日期', width: 110, type: 'text' },
  { field: 'projectName', title: '工程名称', width: 140, type: 'text' },
  { field: 'unitProjectName', title: '单位工程名称', width: 140, type: 'text' },
  { field: 'buDept', title: '所属事业部', width: 110, type: 'text' },
  { field: 'specification', title: '规格', width: 90, type: 'text' },
  { field: 'material', title: '材质', width: 90, type: 'text' },
  { field: 'grooveType', title: '坡口形式', width: 95, type: 'text' },
  { field: 'position', title: '部位', width: 90, type: 'text' },
  { field: 'weldingMethod', title: '焊接方式', width: 95, type: 'text' },
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
  { field: 'resultLevel', title: '级别', width: 65, type: 'text' },
  { field: 'inspectionConclusion', title: '检测结论', width: 95, type: 'text' },
  { field: 'unqualifiedHandling', title: '不合格处理', width: 130, type: 'text' },
  { field: 'reportDefectPosition', title: '缺陷位置', width: 110, type: 'text' },
  { field: 'reportDefectNature', title: '缺陷性质', width: 110, type: 'text' },
  { field: 'reportDefectLength', title: '缺陷长度', width: 90, type: 'numeric', mask: '#.##' },
  { field: 'unqualifiedDefectType', title: '不合格缺陷类型', width: 140, type: 'text' },
  { field: 'remark', title: '备注', width: 140, type: 'text' },
  { field: 'inspectorName', title: '检测人员', width: 90, type: 'text' },
  { field: 'boxNo', title: '箱号', width: 90, type: 'text' },
  { field: 'filmLength', title: '底片长度', width: 90, type: 'text' },
  { field: 'filmCount', title: '底片张数', width: 85, type: 'numeric' },
  { field: 'levelI', title: 'Ⅰ级', width: 60, type: 'numeric' },
  { field: 'levelIi', title: 'Ⅱ级', width: 60, type: 'numeric' },
  { field: 'levelIii', title: 'Ⅲ级', width: 60, type: 'numeric' },
  { field: 'levelIv', title: 'Ⅳ级', width: 60, type: 'numeric' },
  // Defect position display columns (15 positions, read-only summary)
  ...Array.from({ length: 15 }, (_, i) => ({
    field: `_defectPos${i + 1}`,
    title: `缺陷${i + 1}`,
    width: 95,
    type: 'text',
    displayOnly: true,
  })),
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
let dirtyRowIndices = new Set()
let isInternalUpdate = false  // flag to prevent onchange loops during programmatic updates
let lastSelectedRow = -1  // track row clicked by user (survives focus loss)
let lastSelectedCol = -1  // track col clicked by user

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
    // Defect position display columns — derive from _defectPositions array
    if (col.field.startsWith('_defectPos')) {
      const posIdx = parseInt(col.field.replace('_defectPos', '')) - 1
      const defs = rowObj._defectPositions || []
      const d = defs.find((dp) => dp && (dp.pos === posIdx + 1))
      if (!d) return ''
      const parts = [d.defect, d.level, d.length].filter(Boolean)
      return parts.join(' / ')
    }
    const val = rowObj[col.field]
    if (val == null) return ''
    if (col.field === 'inspectionDate' || col.field === 'instructionDate') {
      return typeof val === 'string' ? val.substring(0, 10) : val
    }
    return val
  })
}

const ROW_HEIGHT = 26
const MIN_TOTAL_ROWS = 2000   // pre-pad data for Excel-like scroll range
const SCROLL_EXTEND_ROWS = 200  // rows to add when nearing bottom
const SCROLL_THRESHOLD = 400    // px from bottom to trigger extension

function buildSheetData() {
  const data = tableData.map((row) => getRowData(row))
  // Pad with empty rows to create the illusion of infinite scroll.
  // The spreadsheet always has at least MIN_TOTAL_ROWS rows so the
  // vertical scrollbar has enough range from the very beginning.
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
  nextTick(() => {
    isInternalUpdate = false
  })
}

// ─── Infinite scroll — poll-based (most reliable) + scroll event ───
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

// scroll event handler (fast response)
function onSheetScroll() {
  tryExtendSheet()
}

// polling fallback (guaranteed to work regardless of event quirks)
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
    // jspreadsheet-ce v5 returns the instance directly; capture it
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
        tableOverflow: false,   // let .sheet-wrap handle scrolling natively
        defaultRowHeight: ROW_HEIGHT,
        defaultColWidth: 90,
        editable: true,
        allowComments: false,
        onchange: handleCellChange,
        ondeleterow: handleDeleteRow,
        oninsertrow: handleInsertRow,
      }]
    })

    // jspreadsheet-ce v5: find the real worksheet instance.
    // Priority: return value → .jss_container child → direct element property
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

    // Attach scroll listener + start polling on .sheet-wrap
    // Also listen for user interaction to mark sheet as modified
    nextTick(() => {
      const wrapEl = spreadsheetEl.value?.parentElement
      if (wrapEl) {
        wrapEl.addEventListener('scroll', onSheetScroll, { passive: true })
      }
      // DOM-level interaction detection (onchange unreliable with tableOverflow:false)
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

// ─── Cell change handler — track that the sheet has been modified ───
function handleCellChange(instance, cell, colIndex, rowIndex, newValue, oldValue) {
  if (isInternalUpdate) return
  const rowIdx = typeof rowIndex === 'string' ? parseInt(rowIndex) : rowIndex
  dirtyRowIndices.add(rowIdx)
  dirtyCount.value = dirtyRowIndices.size
}

// ─── Track selected cell from DOM click (survives focus loss to toolbar buttons) ───
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
  lastSelectedCol = cells.indexOf(td) - 1  // -1 to skip row-number column

  // Center viewport on the clicked cell (except near edges)
  setTimeout(() => {
    ensureActiveCellVisible()
  }, 60)
}
const touched = ref(false)

// ─── Auto-scroll to center active cell in viewport (like Excel) ───
function ensureActiveCellVisible() {
  if (!spreadsheetEl.value) return

  // Try to get cell coordinates: first from worksheet selection, then from tracked click
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

  // 不能滚出内容区域边界（开头/结尾自然靠边）
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
    // Delay to let jspreadsheet update cell position before we query DOM
    setTimeout(() => {
      ensureActiveCellVisible()
    }, 60)
  }
}

// ─── Build row object from spreadsheet row ───
// Only includes editable columns (skips displayOnly columns like defect positions)
function buildRowFromSheet(rowIndex) {
  if (!worksheet) return null
  const rowData = worksheet.getRowData(rowIndex)
  if (!rowData) return null
  const obj = { projectId: projectId.value }
  const intFields = ['filmCount', 'levelI', 'levelIi', 'levelIii', 'levelIv']
  columnDefs.forEach((col, i) => {
    if (col.displayOnly) return  // skip display-only columns
    let val = rowData[i]
    if (val === '' || val === undefined || val === null) {
      obj[col.field] = (col.field === 'inspectionLength' ||
        col.field === 'filmCount' || col.field.startsWith('level')) ? null : ''
    } else if (col.type === 'numeric') {
      let num = Number(val)
      if (intFields.includes(col.field)) num = Math.round(num)
      obj[col.field] = isNaN(num) ? null : num
    } else {
      obj[col.field] = val
    }
  })
  // Auto-fill project-derived fields when empty
  if (project.value) {
    if (!obj.buDept) obj.buDept = project.value.buName || ''
    if (!obj.projectName) obj.projectName = project.value.projectName || ''
    if (!obj.unitProjectName) obj.unitProjectName = project.value.unitProjectName || ''
  }
  // Normalize date fields: users may type "2026.06.17" or "2026/06/17"
  const dateFields = ['instructionDate', 'inspectionDate']
  dateFields.forEach((f) => {
    if (obj[f] && typeof obj[f] === 'string') {
      obj[f] = obj[f].replace(/\./g, '-').replace(/\//g, '-')
    }
  })
  return obj
}

// ─── Save all data (like Excel — save everything on demand) ───
// silent=true 时不弹出成功提示（用于自动保存场景），但错误仍会提示
async function handleSave(silent = false) {
  if (!worksheet || saving.value) return

  saving.value = true
  let saved = 0
  let errors = 0

  try {
    // Get ALL rows from the worksheet
    const allData = worksheet.getData(false)
    if (!allData || allData.length === 0) {
      ElMessage.info('没有需要保存的数据')
      return
    }

    for (let rowIdx = 0; rowIdx < allData.length; rowIdx++) {
      const rowData = buildRowFromSheet(rowIdx)
      if (!rowData) continue

      const hasContent = rowData.weldNo || rowData.inspectionMethod
      if (!hasContent) continue
      if (!rowData.weldNo) continue

      try {
        const existingRow = rowIdx < tableData.length ? tableData[rowIdx] : null

        if (existingRow?.id) {
          // PROJECT_ADMIN can only create, not modify
          if (userStore.isProjectAdmin) continue
          await inspectionApi.update(existingRow.id, { ...rowData, projectId: projectId.value })
          Object.assign(existingRow, rowData)
          saved++
        } else {
          const res = await inspectionApi.create({ ...rowData, projectId: projectId.value })
          if (res?.data) {
            const newRow = { ...res.data, _defectPositions: [] }
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
      if (!silent) ElMessage.success(`已保存 ${saved} 条数据`)
    } else {
      if (!silent) ElMessage.info('数据无变化')
    }
  } finally {
    saving.value = false
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
  if (isInternalUpdate) return

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
  if (touched.value) {
    await handleSave()
  }
  loadData()
}

// ─── Export ───
async function handleExport() {
  try {
    loading.value = true
    const res = await inspectionApi.list({ projectId: projectId.value, method: filterMethod.value, size: 100000 })
    if (res?.data?.records) {
      exportToExcel(res.data.records)
    } else {
      ElMessage.warning('没有可导出的数据')
    }
  } catch {
    ElMessage.error('导出失败，请稍后重试')
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
async function goBack() {
  if (touched.value) {
    await handleSave(true)
  }
  router.push(`/project/${projectId.value}/detail`)
}

// ─── Defect position editing ───
const defectDialogVisible = ref(false)
const defectEditRowIndex = ref(-1)
const defectForm = ref([])

function openDefectEditor() {
  if (!worksheet) return

  // Try to get selected row: first from our tracked click, then from active selection
  let rowIdx = lastSelectedRow
  if (rowIdx < 0) {
    const selected = worksheet.getSelected()
    if (selected && selected.length > 0) {
      rowIdx = selected[0].y
    }
  }
  if (rowIdx < 0) {
    ElMessage.warning('请先点击目标行中的任意单元格，再点击编辑缺陷')
    return
  }
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
  row.defectPositions = valid   // store as array, not JSON string (API expects List<DefectPosition>)
  defectDialogVisible.value = false

  // Refresh defect display columns in the spreadsheet for this row
  if (worksheet) {
    const rowIdx = defectEditRowIndex.value
    const firstDefectCol = columnDefs.findIndex((c) => c.field.startsWith('_defectPos'))
    for (let i = 0; i < 15; i++) {
      const colIdx = firstDefectCol + i
      if (colIdx < 0) continue
      const d = valid.find((dp) => dp && dp.pos === i + 1)
      const text = d ? [d.defect, d.level, d.length].filter(Boolean).join(' / ') : ''
      try { worksheet.setValue(rowIdx, colIdx, text) } catch { /* ignore */ }
    }
  }

  if (row.id) {
    saving.value = true
    try {
      await inspectionApi.update(row.id, { defectPositions: valid, projectId: projectId.value })
    } catch { /* handled by interceptor */ }
    finally { saving.value = false }
  }
}

// ─── Delete selected row ───
async function handleDeleteSelected() {
  if (!worksheet) return

  // Try to get selected row: first from our tracked click, then from active selection
  let rowIdx = lastSelectedRow
  if (rowIdx < 0) {
    const selected = worksheet.getSelected()
    if (selected && selected.length > 0) {
      rowIdx = selected[0].y
    }
  }
  if (rowIdx < 0) {
    ElMessage.warning('请先点击目标行中的任意单元格，再点击删除行')
    return
  }
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

// ─── Ctrl+S handler ───
function onKeyDown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSave()
  }
}

// ─── Warn before leaving with unsaved data ───
function onBeforeUnload(e) {
  if (touched.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

// ─── Auto-save when navigating away (router navigation) ───
onBeforeRouteLeave(async (to, from, next) => {
  if (touched.value) {
    await handleSave(true)
  }
  next()
})

// ─── Lifecycle ───
onMounted(async () => {
  await loadProject()
  await loadData()
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
        <el-button :icon="ArrowLeft" link @click="goBack">返回项目详情</el-button>
        <div>
          <h2 class="page-title">
            {{ project?.projectName || '检测数据录入' }}
            <el-tag v-if="project" :type="project?.status === 'COMPLETED' ? 'success' : 'primary'" size="small" class="project-tag">
              {{ project?.projectCode }}
            </el-tag>
          </h2>
          <p class="page-subtitle">点击单元格直接编辑 · Tab/Enter 导航 · Ctrl+S 保存 · Ctrl+C/V 复制粘贴</p>
        </div>
      </div>
      <div class="header-actions">
        <span class="save-indicator saving" v-if="saving">
          <el-icon class="is-loading"><Loading /></el-icon> 保存中...
        </span>
        <span class="save-indicator unsaved" v-else-if="touched">
          已修改，请保存
        </span>
        <span class="save-indicator saved" v-else>
          已保存
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
        <el-button type="primary" :loading="saving" @click="handleSave">保存 (Ctrl+S)</el-button>
        <el-button :icon="Upload" @click="handleImportClick">导入Excel</el-button>
        <input ref="importRef" type="file" accept=".xlsx,.xls" style="display: none" @change="handleImportFile" />
        <el-button :icon="Download" @click="handleExport">导出Excel</el-button>
        <el-tooltip content="先点击表格中目标行的任意单元格，再点此按钮" placement="top">
          <el-button :icon="Grid" @click="openDefectEditor">编辑缺陷</el-button>
        </el-tooltip>
        <el-tooltip v-if="!userStore.isProjectAdmin" content="先点击表格中目标行的任意单元格，再点此按钮" placement="top">
          <el-button :icon="Delete" type="danger" plain @click="handleDeleteSelected">删除行</el-button>
        </el-tooltip>
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
}

.save-indicator.saving {
  color: var(--color-warning);
}

.save-indicator.unsaved {
  color: var(--color-danger);
  font-weight: 500;
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
/* .sheet-wrap is the native scroll container — both scrollbars always visible. */
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

/* ─── Native scrollbar styling for .sheet-wrap (the scroll container) ─── */
.sheet-wrap::-webkit-scrollbar {
  width: 12px;   /* 垂直滚动条宽度 */
  height: 18px;  /* 水平滚动条高度 — 加粗便于拖拽 */
}

.sheet-wrap::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.sheet-wrap::-webkit-scrollbar-thumb {
  background: #b0b0b0;
  border-radius: 9px;
}

.sheet-wrap::-webkit-scrollbar-thumb:hover {
  background: #888;
}

.sheet-wrap::-webkit-scrollbar-corner {
  background: #f1f1f1;
}
</style>
