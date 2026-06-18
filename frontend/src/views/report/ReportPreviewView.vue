<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { reportApi } from '@/api/report'
import { signatureApi } from '@/api/signature'

const route = useRoute()
const router = useRouter()

const reportId = ref(Number(route.params.id))
const loading = ref(false)
const report = ref(null)
const layout = ref(null)
const inspectionData = ref({})
const signHistory = ref([])
const reportRef = ref(null)

const methodTypes = ['RT', 'UT', 'PT', 'MT', 'AUT', 'PA', 'TOFD', 'DR']

const statusMap = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING_SIGN: { label: '待签字', type: 'warning' },
  SIGNED: { label: '已签字', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
}

async function loadData() {
  loading.value = true
  try {
    const res = await reportApi.getById(reportId.value)
    if (res?.data) {
      report.value = res.data

      // Parse report content (contains inspection data)
      if (res.data.reportContent) {
        try {
          const content = typeof res.data.reportContent === 'string'
            ? JSON.parse(res.data.reportContent)
            : res.data.reportContent
          inspectionData.value = content.inspectionData || content
          layout.value = content.layoutConfig
            ? (typeof content.layoutConfig === 'string' ? JSON.parse(content.layoutConfig) : content.layoutConfig)
            : null
        } catch {
          inspectionData.value = {}
        }
      }

      // Load signature history
      try {
        const sigRes = await signatureApi.history(reportId.value)
        if (sigRes?.data) signHistory.value = sigRes.data
      } catch { /* ignore */ }
    }
  } finally {
    loading.value = false
  }
}

// Render field value
function getFieldValue(key) {
  const val = inspectionData.value[key]
  if (val == null || val === '') return '-'
  if (typeof val === 'string' && (val.includes('T') || val.includes('-'))) {
    const d = val.substring(0, 10)
    if (d.match(/^\d{4}-\d{2}-\d{2}$/)) return d
  }
  return String(val)
}

function sectionStyle(style) {
  if (!style) return {}
  return {
    fontSize: (style.fontSize || 16) + 'px',
    fontWeight: style.fontWeight || 'bold',
    textAlign: style.textAlign || 'center',
  }
}

// Export PDF
async function exportPdf() {
  if (!reportRef.value) {
    ElMessage.warning('报告内容未加载')
    return
  }
  try {
    ElMessage.info('正在生成 PDF...')
    const { default: html2canvas } = await import('html2canvas')
    const { default: jsPDF } = await import('jspdf')

    const canvas = await html2canvas(reportRef.value, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
    })

    const imgData = canvas.toDataURL('image/png')
    const imgWidth = 210 // A4 width in mm
    const pageHeight = 297 // A4 height in mm
    const imgHeight = (canvas.height * imgWidth) / canvas.width

    const pdf = new jsPDF('p', 'mm', 'a4')
    let heightLeft = imgHeight
    let position = 0

    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
    heightLeft -= pageHeight

    while (heightLeft > 0) {
      position = heightLeft - imgHeight
      pdf.addPage()
      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
      heightLeft -= pageHeight
    }

    pdf.save(`${report.value?.reportNo || 'report'}.pdf`)
    ElMessage.success('PDF 导出成功')
  } catch (err) {
    console.error('PDF export error:', err)
    ElMessage.error('PDF 导出失败，请重试')
  }
}

function goBack() {
  router.push('/report')
}

onMounted(() => loadData())
</script>

<template>
  <div class="preview-page" v-loading="loading">
    <!-- Top actions -->
    <div class="preview-toolbar">
      <div class="toolbar-left">
        <el-button :icon="ArrowLeft" @click="goBack">返回列表</el-button>
        <el-divider direction="vertical" />
        <span class="report-no text-mono" v-if="report">{{ report.reportNo }}</span>
        <el-tag v-if="report" :type="statusMap[report.status]?.type || 'info'" size="small" effect="light">
          {{ statusMap[report.status]?.label || report.status }}
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-button :icon="Printer" @click="window.print()">打印</el-button>
        <el-button type="primary" :icon="Download" @click="exportPdf">导出 PDF</el-button>
      </div>
    </div>

    <!-- A4 Report Content -->
    <div class="preview-body">
      <div ref="reportRef" class="a4-page">
        <!-- Report header -->
        <div class="report-header">
          <h1 class="report-title">无损检测报告</h1>
          <div class="report-meta">
            <span>报告编号：{{ report?.reportNo || '-' }}</span>
            <span>检测方法：{{ inspectionData.inspectionMethod || '-' }}</span>
            <span>检测日期：{{ getFieldValue('inspectionDate') }}</span>
          </div>
        </div>

        <el-divider />

        <!-- Sections-based rendering (pages with tables) -->
        <template v-if="layout?.pages">
          <div v-for="(page, pi) in layout.pages" :key="pi" class="report-page">
            <div v-if="pi > 0" class="page-break"></div>
            <h2 v-if="page.name" class="page-name">{{ page.name }}</h2>

            <div v-for="(section, si) in page.sections" :key="si" :class="'section-' + section.type">

              <!-- header -->
              <div v-if="section.type === 'header'" class="section-header" :style="sectionStyle(section.style)">
                <div class="header-text">{{ section.text }}</div>
                <div v-if="section.subtext" class="header-subtext">{{ section.subtext.replace('{reportNo}', report?.reportNo || '-') }}</div>
              </div>

              <!-- table -->
              <div v-else-if="section.type === 'table'" class="section-table">
                <div v-if="section.label" class="table-label">{{ section.label }}</div>
                <!-- row-based table -->
                <table v-if="section.rows" class="info-table">
                  <tbody>
                    <tr v-for="(row, ri) in section.rows" :key="ri">
                      <td v-for="(cell, ci) in row" :key="ci"
                          :colspan="cell.colspan || 1"
                          :class="{ 'td-label': cell.label, 'td-value': !cell.label }">
                        <template v-if="cell.label">{{ cell.label }}</template>
                        <template v-if="cell.bind && cell.label">：{{ getFieldValue(cell.bind) }}</template>
                        <template v-if="!cell.label">{{ cell.bind ? getFieldValue(cell.bind) : '' }}</template>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <!-- column-based table (with optional headerRows) -->
                <table v-else-if="section.columns" class="data-table">
                  <colgroup>
                    <col v-for="col in section.columns" :key="col.field"
                         :style="{ width: col.width ? col.width + 'px' : 'auto' }">
                  </colgroup>
                  <thead>
                    <tr v-for="(hrow, hi) in section.headerRows" :key="'hr'+hi">
                      <th v-for="(hcell, hci) in hrow" :key="'hc'+hci"
                          :colspan="hcell.colspan || 1"
                          :rowspan="hcell.rowspan || 1"
                          style="text-align:center;vertical-align:middle">
                        {{ hcell.label }}<template v-if="hcell.bind">：{{ getFieldValue(hcell.bind) }}</template>
                      </th>
                    </tr>
                  </thead>
                  <thead v-if="!section.headerRows?.some(r => r.some(c => c.rowspan > 1))">
                    <tr>
                      <th v-for="col in section.columns" :key="col.field">
                        {{ col.label }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="n in (section.dataRows || section.rows || 5)" :key="n">
                      <td v-for="col in section.columns" :key="col.field">&nbsp;</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- image-area -->
              <div v-else-if="section.type === 'image-area'" class="section-image"
                   :style="{ minHeight: (section.height || 200) + 'px' }">
                <div class="image-label">{{ section.label }}</div>
                <div class="image-placeholder-box">
                  <el-icon :size="48"><Picture /></el-icon>
                  <span>（底片布置图 / 检测部位示意图）</span>
                </div>
              </div>

              <!-- text -->
              <div v-else-if="section.type === 'text'" class="section-text">
                {{ section.content }}
              </div>

              <!-- signature -->
              <div v-else-if="section.type === 'signature'" class="section-signature">
                <div class="signature-row">
                  <div v-for="sig in section.signatories" :key="sig.label" class="signature-item">
                    <div class="sig-label">{{ sig.label }}</div>
                    <div class="sig-value">{{ getFieldValue(sig.bind) || '　' }}</div>
                    <div class="sig-line"></div>
                    <div class="sig-date">年　月　日</div>
                  </div>
                </div>
              </div>

            </div>
          </div>
        </template>

        <!-- Layout-based rendering (if template has layout config) -->
        <div v-else-if="layout?.fields?.length" class="report-layout" :style="{ position: 'relative', minHeight: '700px' }">
          <div
            v-for="field in layout.fields"
            :key="field.id"
            class="layout-field"
            :style="{
              position: 'absolute',
              left: field.x + 'px',
              top: field.y + 'px',
              width: (field.width || 120) + 'px',
              fontSize: (field.fontSize || 13) + 'px',
              fontWeight: field.fontWeight || 'normal',
              color: field.color || '#1e293b',
            }"
          >
            <label class="field-label">{{ field.label }}</label>
            <span class="field-value">{{ getFieldValue(field.key) }}</span>
          </div>

          <!-- Image areas -->
          <div
            v-for="area in (layout.imageAreas || [])"
            :key="area.id"
            class="layout-image-area"
            :style="{
              position: 'absolute',
              left: area.x + 'px',
              top: area.y + 'px',
              width: area.width + 'px',
              height: area.height + 'px',
            }"
          >
            <div class="image-placeholder">
              <el-icon :size="32"><Picture /></el-icon>
              <span>{{ area.label }}</span>
            </div>
          </div>
        </div>

        <!-- Default table layout (fallback when no custom layout) -->
        <div v-else class="report-default">
          <table class="report-table">
            <tbody>
              <tr><td class="label" width="140">施工单位</td><td>{{ getFieldValue('constructionUnit') }}</td><td class="label" width="140">焊口编号</td><td>{{ getFieldValue('weldNo') }}</td></tr>
              <tr><td class="label">指令编号</td><td>{{ getFieldValue('instructionNo') }}</td><td class="label">指令日期</td><td>{{ getFieldValue('instructionDate') }}</td></tr>
              <tr><td class="label">检测方法</td><td>{{ getFieldValue('inspectionMethod') }}</td><td class="label">工程名称</td><td>{{ getFieldValue('projectName') }}</td></tr>
              <tr><td class="label">规格</td><td>{{ getFieldValue('specification') }}</td><td class="label">材质</td><td>{{ getFieldValue('material') }}</td></tr>
              <tr><td class="label">坡口形式</td><td>{{ getFieldValue('grooveType') }}</td><td class="label">部位</td><td>{{ getFieldValue('position') }}</td></tr>
              <tr><td class="label">焊接方式</td><td>{{ getFieldValue('weldingMethod') }}</td><td class="label">检测标准</td><td>{{ getFieldValue('inspectionStandard') }}</td></tr>
              <tr><td class="label">合格级别</td><td>{{ getFieldValue('qualifiedLevel') }}</td><td class="label">检测长度(m)</td><td>{{ getFieldValue('inspectionLength') }}</td></tr>
              <tr><td class="label">焊工代号</td><td>{{ getFieldValue('welderCode') }}</td><td class="label">检测人员</td><td>{{ getFieldValue('inspectorName') }}</td></tr>
              <tr><td class="label">检测结论</td><td colspan="3">
                <el-tag :type="inspectionData.inspectionConclusion === '合格' ? 'success' : 'danger'" size="small" effect="dark">
                  {{ getFieldValue('inspectionConclusion') }}
                </el-tag>
              </td></tr>
              <tr><td class="label">备注</td><td colspan="3">{{ getFieldValue('remark') }}</td></tr>
            </tbody>
          </table>
        </div>

        <!-- Signature section -->
        <div class="signature-section">
          <el-divider />
          <h3 class="section-title">签字栏</h3>
          <div class="signature-grid">
            <div class="signature-box">
              <div class="sign-label">技术负责人</div>
              <div class="sign-status">
                <template v-if="signHistory.length > 0">
                  <el-tag
                    :type="signHistory[0]?.signStatus === 'SIGNED' ? 'success' : 'warning'"
                    size="small"
                  >
                    {{ signHistory[0]?.signStatus === 'SIGNED' ? '已签' : '待签' }}
                  </el-tag>
                  <div v-if="signHistory[0]?.signTime" class="sign-time">
                    {{ signHistory[0].signTime?.replace('T', ' ').substring(0, 19) }}
                  </div>
                </template>
                <span v-else class="text-secondary">-</span>
              </div>
            </div>
            <div class="signature-box">
              <div class="sign-label">项目经理</div>
              <div class="sign-status">
                <template v-if="signHistory.length > 1">
                  <el-tag
                    :type="signHistory[1]?.signStatus === 'SIGNED' ? 'success' : 'warning'"
                    size="small"
                  >
                    {{ signHistory[1]?.signStatus === 'SIGNED' ? '已签' : '待签' }}
                  </el-tag>
                  <div v-if="signHistory[1]?.signTime" class="sign-time">
                    {{ signHistory[1].signTime?.replace('T', ' ').substring(0, 19) }}
                  </div>
                </template>
                <span v-else class="text-secondary">-</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.preview-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--header-height) - 48px);
  margin: -24px;
  background: #e2e8f0;
}

/* Toolbar */
.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 8px; }
.report-no { font-size: 14px; font-weight: 600; }

/* Body */
.preview-body {
  flex: 1;
  overflow: auto;
  display: flex;
  justify-content: center;
  padding: 24px;
}

/* A4 page */
.a4-page {
  width: 794px; /* A4 at 96dpi */
  min-height: 1123px;
  background: #fff;
  box-shadow: var(--shadow-lg);
  padding: 60px 50px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text-primary);
}

@media print {
  .preview-toolbar { display: none; }
  .preview-body { padding: 0; overflow: visible; }
  .a4-page { box-shadow: none; width: 100%; min-height: auto; padding: 15mm; }
}

/* Header */
.report-header { text-align: center; margin-bottom: 24px; }
.report-title { font-size: 22px; font-weight: 700; margin: 0 0 12px; letter-spacing: 2px; }
.report-meta { display: flex; justify-content: center; gap: 24px; font-size: 12px; color: var(--color-text-secondary); }

/* Layout-based fields */
.layout-field { padding: 2px 0; }
.field-label { font-size: 10px; color: var(--color-text-secondary); display: block; }
.field-value { font-weight: 500; }

.layout-image-area {
  border: 1px dashed var(--color-border);
  display: flex; align-items: center; justify-content: center;
}
.image-placeholder {
  display: flex; flex-direction: column; align-items: center;
  gap: 8px; color: var(--color-text-placeholder); font-size: 12px;
}

/* Default table */
.report-table { width: 100%; border-collapse: collapse; }
.report-table td {
  border: 1px solid var(--color-border);
  padding: 8px 12px;
  font-size: 13px;
}
.report-table td.label {
  background: var(--color-bg);
  font-weight: 600;
  width: 140px;
  color: var(--color-text-regular);
}

/* Signature */
.signature-section { margin-top: 40px; }
.section-title { font-size: 15px; font-weight: 600; margin: 0 0 16px; }
.signature-grid { display: flex; gap: 40px; }
.signature-box { flex: 1; }
.sign-label { font-weight: 600; margin-bottom: 8px; font-size: 14px; }
.sign-status { min-height: 50px; }
.sign-time { font-size: 12px; color: var(--color-text-secondary); margin-top: 4px; }

/* ── Sections-based rendering ── */
.report-page { margin-bottom: 24px; }
.page-break { border-top: 2px dashed var(--color-border); margin: 32px 0; }
.page-name { font-size: 14px; font-weight: 600; color: var(--color-text-secondary); text-align: center; margin: 0 0 16px; }

.section-header { padding: 8px 0 16px; border-bottom: 2px solid #1e293b; margin-bottom: 16px; }
.header-text { font-size: 20px; font-weight: 700; text-align: center; }
.header-subtext { font-size: 12px; color: var(--color-text-secondary); text-align: right; margin-top: 4px; }

.section-table { margin-bottom: 16px; }
.table-label { font-size: 13px; font-weight: 600; margin-bottom: 8px; color: var(--color-text-primary); }

.info-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.info-table td {
  border: 1px solid var(--color-border);
  padding: 6px 8px;
  line-height: 1.6;
}
.info-table .td-label {
  background: #f8fafc;
  font-weight: 600;
  width: 120px;
  white-space: nowrap;
}
.info-table .td-value { color: var(--color-text-primary); }

.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th, .data-table td {
  border: 1px solid var(--color-border);
  padding: 6px 8px;
  text-align: center;
}
.data-table th { background: #f8fafc; font-weight: 600; }
.data-table td { height: 28px; }

.section-image {
  border: 2px dashed #94a3b8;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 16px 0;
}
.image-label { font-size: 13px; font-weight: 600; margin: 12px 0; }
.image-placeholder-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--color-text-placeholder);
  padding-bottom: 24px;
}

.section-text {
  font-size: 14px;
  line-height: 2;
  padding: 8px 0;
  font-weight: 500;
}

.section-signature { margin-top: 32px; }
.signature-row { display: flex; gap: 24px; }
.signature-item {
  flex: 1;
  text-align: center;
  padding: 8px;
}
.sig-label { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.sig-value { font-size: 13px; min-height: 24px; color: var(--color-text-secondary); }
.sig-line { border-bottom: 1px solid var(--color-text-primary); margin: 4px auto 8px; width: 80%; }
.sig-date { font-size: 12px; color: var(--color-text-secondary); }
</style>
