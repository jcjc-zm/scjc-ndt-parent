<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { signatureApi } from '@/api/signature'
import { reportApi } from '@/api/report'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('pending')
const loading = ref(false)
const pendingList = ref([])
const historyList = ref([])

// Sign dialog
const signDialogVisible = ref(false)
const signingRecord = ref(null)
const signReport = ref(null)
const signForm = ref({
  comment: '',
  signatureImageUrl: '',
})
const signing = ref(false)

// Role labels
const roleMap = {
  TECHNICAL_LEADER: '技术负责人',
  PROJECT_MANAGER: '项目经理',
}

async function loadPending() {
  loading.value = true
  try {
    const res = await signatureApi.pending()
    if (res?.data) {
      // Enrich with report info
      const enriched = await Promise.all(
        res.data.map(async (sig) => {
          try {
            const reportRes = await reportApi.getById(sig.reportId)
            return { ...sig, _report: reportRes?.data }
          } catch {
            return sig
          }
        })
      )
      pendingList.value = enriched
    }
  } finally {
    loading.value = false
  }
}

function openSignDialog(record) {
  signingRecord.value = record
  signReport.value = record._report || null
  signForm.value = { comment: '', signatureImageUrl: '' }
  signDialogVisible.value = true
}

async function handleSign() {
  if (!signingRecord.value) return

  try {
    await ElMessageBox.confirm(
      `确认签署报告「${signReport.value?.reportNo || signingRecord.value.reportId}」？`,
      '签字确认',
      {
        confirmButtonText: '确认签字',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
  } catch {
    return
  }

  signing.value = true
  try {
    await signatureApi.sign(signingRecord.value.reportId, signForm.value)
    ElMessage.success('签字成功')
    signDialogVisible.value = false
    loadPending()
  } catch {
    // Error handled by interceptor
  } finally {
    signing.value = false
  }
}

function goToReport(reportId) {
  router.push(`/report/preview/${reportId}`)
}

onMounted(() => loadPending())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">签字审批</h2>
        <p class="page-subtitle" v-if="userStore.isTechnicalLeader">技术负责人 — 审阅签字</p>
        <p class="page-subtitle" v-else-if="userStore.isProjectManagerRole">项目经理 — 最终签字</p>
        <p class="page-subtitle" v-else>查看签字记录</p>
      </div>
    </div>

    <!-- Tabs -->
    <el-card shadow="never" class="content-card">
      <el-tabs v-model="activeTab" @tab-change="activeTab === 'pending' ? loadPending() : null">
        <el-tab-pane label="待签字" name="pending">
          <el-table
            :data="pendingList"
            v-loading="loading"
            border
            stripe
            :empty-text="'暂无待签字报告'"
          >
            <el-table-column label="报告编号" width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="text-mono">{{ row._report?.reportNo || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="签字角色" width="130" align="center">
              <template #default="{ row }">
                <el-tag :type="row.signatoryRole === 'TECHNICAL_LEADER' ? 'primary' : 'warning'" size="small">
                  {{ roleMap[row.signatoryRole] || row.signatoryRole }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="签字顺序" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.signOrder === 1 ? '' : 'info'" size="small" effect="plain">
                  {{ row.signOrder === 1 ? '第一签' : '第二签' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="报告状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row._report?.status === 'PENDING_SIGN' ? 'warning' : 'info'"
                  size="small"
                  effect="light"
                >
                  {{ row._report?.status === 'PENDING_SIGN' ? '待签字' : row._report?.status || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" align="center">
              <template #default="{ row }">
                <el-button link type="primary" :icon="View" @click="goToReport(row.reportId)">
                  查看报告
                </el-button>
                <el-button link type="success" :icon="Edit" @click="openSignDialog(row)">
                  签字
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="已签字" name="history">
          <el-empty description="签字历史记录" :image-size="80">
            <template #default>
              <p class="text-secondary">已完成的签字记录将在此显示</p>
            </template>
          </el-empty>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Sign Dialog -->
    <el-dialog
      v-model="signDialogVisible"
      title="签字确认"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="sign-dialog-content">
        <!-- Report info -->
        <el-descriptions v-if="signReport" :column="1" border size="small" style="margin-bottom: 24px">
          <el-descriptions-item label="报告编号">{{ signReport.reportNo }}</el-descriptions-item>
          <el-descriptions-item label="签字角色">
            <el-tag :type="signingRecord?.signatoryRole === 'TECHNICAL_LEADER' ? 'primary' : 'warning'" size="small">
              {{ roleMap[signingRecord?.signatoryRole] }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- Comment -->
        <div class="form-group">
          <label class="form-label">审批意见</label>
          <el-input
            v-model="signForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入审批意见..."
          />
        </div>

        <!-- Signature image -->
        <div class="form-group">
          <label class="form-label">签名图片链接（可选）</label>
          <el-input
            v-model="signForm.signatureImageUrl"
            placeholder="输入签名图片URL"
            clearable
          />
        </div>

        <!-- Preview -->
        <div v-if="signForm.signatureImageUrl" class="sign-preview">
          <img :src="signForm.signatureImageUrl" alt="签名预览" />
        </div>
      </div>

      <template #footer>
        <el-button @click="signDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="signing" :icon="Check" @click="handleSign">
          {{ signing ? '签字中...' : '确认签字' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.page-header { margin-bottom: var(--space-6); }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-secondary); margin: 0; }

.content-card { border-radius: var(--radius-md); }

.text-mono { font-family: var(--font-mono); font-size: 13px; }

/* Sign dialog */
.form-group { margin-bottom: var(--space-4); }
.form-label { display: block; font-size: 13px; font-weight: 600; color: var(--color-text-regular); margin-bottom: 6px; }

.sign-preview {
  margin-top: 8px;
  padding: 12px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  text-align: center;
}
.sign-preview img {
  max-height: 80px;
  max-width: 100%;
  object-fit: contain;
}
</style>
