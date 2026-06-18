<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { templateApi } from '@/api/template'

const router = useRouter()

const loading = ref(false)
const templates = ref([])
const searchKeyword = ref('')
const activeMethod = ref('')

const methodTypes = ['RT', 'UT', 'PT', 'MT', 'AUT', 'PA', 'TOFD', 'DR']

const methodColors = {
  RT: '#ef4444',
  UT: '#3b82f6',
  PT: '#f59e0b',
  MT: '#8b5cf6',
  AUT: '#06b6d4',
  PA: '#10b981',
  TOFD: '#ec4899',
  DR: '#6366f1',
}

// Filtered templates by method type and search keyword
const filteredTemplates = computed(() => {
  let list = templates.value
  if (activeMethod.value) {
    list = list.filter((t) => t.methodType === activeMethod.value)
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(
      (t) =>
        (t.templateName || '').toLowerCase().includes(kw) ||
        (t.description || '').toLowerCase().includes(kw),
    )
  }
  return list
})

async function loadTemplates() {
  loading.value = true
  try {
    const res = await templateApi.list({})
    if (res?.data) templates.value = res.data || []
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
}

function goToCreate() {
  router.push('/report/design/0')
}

function goToEdit(template) {
  router.push(`/report/design/${template.id}`)
}

async function handleDelete(template) {
  try {
    await ElMessageBox.confirm(`确认删除模板「${template.templateName}」？`, '删除确认', {
      type: 'warning',
    })
    await templateApi.remove(template.id)
    templates.value = templates.value.filter((t) => t.id !== template.id)
    ElMessage.success('模板已删除')
  } catch {
    /* cancelled */
  }
}

onMounted(() => loadTemplates())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">模板管理</h2>
        <p class="page-subtitle">管理报告模板，创建和编辑自定义模板</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="goToCreate">新增模板</el-button>
      </div>
    </div>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <div class="method-chips">
          <el-tag
            :type="activeMethod === '' ? 'primary' : 'info'"
            :effect="activeMethod === '' ? 'dark' : 'plain'"
            class="method-chip"
            @click="activeMethod = ''"
          >
            全部
          </el-tag>
          <el-tag
            v-for="m in methodTypes"
            :key="m"
            :effect="activeMethod === m ? 'dark' : 'plain'"
            :type="activeMethod === m ? 'primary' : 'info'"
            class="method-chip"
            @click="activeMethod = activeMethod === m ? '' : m"
          >
            {{ m }}
          </el-tag>
        </div>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索模板名称或描述..."
          :prefix-icon="Search"
          clearable
          style="width: 260px"
        />
        <span class="total-count">共 {{ filteredTemplates.length }} 个模板</span>
      </div>
    </el-card>

    <!-- Card Grid -->
    <div v-loading="loading" class="template-grid-container">
      <div v-if="!loading && !filteredTemplates.length" class="empty-state">
        <el-empty :description="activeMethod || searchKeyword ? '没有匹配的模板' : '暂无模板'">
          <el-button v-if="!activeMethod && !searchKeyword" type="primary" @click="goToCreate">
            创建第一个模板
          </el-button>
        </el-empty>
      </div>

      <div v-else class="template-grid">
        <div
          v-for="tpl in filteredTemplates"
          :key="tpl.id"
          class="template-card"
          @click="goToEdit(tpl)"
        >
          <div class="card-cover" :style="{ background: `linear-gradient(135deg, ${methodColors[tpl.methodType] || '#059669'}22, ${methodColors[tpl.methodType] || '#059669'}08)` }">
            <div class="cover-icon" :style="{ color: methodColors[tpl.methodType] || '#059669' }">
              <el-icon :size="40"><Document /></el-icon>
            </div>
            <div class="cover-badge" :style="{ background: methodColors[tpl.methodType] || '#059669' }">
              {{ tpl.methodType || '-' }}
            </div>
          </div>
          <div class="card-body">
            <h4 class="card-title">{{ tpl.templateName }}</h4>
            <p class="card-desc">{{ tpl.description || '暂无描述' }}</p>
            <div class="card-meta">
              <el-tag :type="tpl.templateType === 'SYSTEM' ? '' : 'success'" size="small" effect="light">
                {{ tpl.templateType === 'SYSTEM' ? '系统预设' : '自定义' }}
              </el-tag>
              <span class="card-time">{{ (tpl.createTime || '').replace('T', ' ').substring(0, 10) || '-' }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button text size="small" :icon="Edit" @click.stop="goToEdit(tpl)">编辑</el-button>
            <el-button text size="small" type="danger" :icon="Delete" @click.stop="handleDelete(tpl)">删除</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-6);
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 4px;
}
.page-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}
.header-actions {
  display: flex;
  gap: 8px;
}

/* Filters */
.filter-card {
  margin-bottom: 20px;
  border-radius: var(--radius-md);
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.method-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.method-chip {
  cursor: pointer;
  user-select: none;
  transition: transform 0.15s ease;
}
.method-chip:hover {
  transform: translateY(-1px);
}
.total-count {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-left: auto;
}

/* Grid */
.template-grid-container {
  min-height: 200px;
}
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.empty-state {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

/* Card */
.template-card {
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: all 0.25s ease;
}
.template-card:hover {
  border-color: var(--color-accent);
  box-shadow: 0 4px 16px rgba(5, 150, 105, 0.10);
  transform: translateY(-2px);
}

.card-cover {
  position: relative;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover-icon {
  opacity: 0.7;
}
.cover-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 12px;
  letter-spacing: 0.5px;
}

.card-body {
  padding: 16px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
}
.card-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 0 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-time {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding: 0 12px 12px;
}
</style>
