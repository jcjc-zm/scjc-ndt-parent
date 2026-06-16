<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { imageApi } from '@/api/image'

const loading = ref(false)
const images = ref([])
const filterTechnique = ref('')

const techniqueOptions = ['双壁双影', '双壁双影纵', '双壁单影', '单壁透照', '中心透照']

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (filterTechnique.value) params.techniqueType = filterTechnique.value
    const res = await imageApi.list(params)
    if (res?.data) images.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除图片「${row.imageName}」？`, '删除确认', { type: 'warning' })
    await imageApi.remove(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancelled */ }
}

function handleSearch() {
  loadData()
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">系统图片库</h2>
        <p class="page-subtitle">管理检测底片、透照示意图等图片资源</p>
      </div>
    </div>

    <!-- Filter -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-select v-model="filterTechnique" placeholder="透照方式" clearable style="width: 160px" @change="handleSearch">
          <el-option v-for="t in techniqueOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button :icon="Search" @click="handleSearch">查询</el-button>
      </div>
    </el-card>

    <!-- Image grid -->
    <el-card shadow="never" class="content-card" v-loading="loading">
      <div v-if="images.length" class="image-grid">
        <div v-for="img in images" :key="img.id" class="image-card">
          <div class="image-thumb">
            <img v-if="img.imageUrl" :src="img.imageUrl" :alt="img.imageName" />
            <el-icon v-else :size="48"><Picture /></el-icon>
          </div>
          <div class="image-info">
            <strong class="image-name">{{ img.imageName }}</strong>
            <span class="text-secondary" style="font-size: 12px">{{ img.techniqueType || '通用' }}</span>
            <span class="text-secondary" style="font-size: 11px">
              {{ img.uploadTime?.replace('T', ' ').substring(0, 16) || '-' }}
            </span>
          </div>
          <el-button
            link
            type="danger"
            :icon="Delete"
            size="small"
            class="delete-btn"
            @click="handleDelete(img)"
          />
        </div>
      </div>
      <el-empty v-else description="暂无图片" :image-size="80" />
    </el-card>
  </div>
</template>

<style scoped>
.page-container { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.page-header { margin-bottom: var(--space-6); }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-secondary); margin: 0; }

.filter-card { margin-bottom: 16px; border-radius: var(--radius-md); }
.filter-row { display: flex; align-items: center; gap: 12px; }
.content-card { border-radius: var(--radius-md); }

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.image-card {
  position: relative;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: box-shadow var(--transition-fast);
}
.image-card:hover { box-shadow: var(--shadow-md); }
.image-thumb {
  height: 140px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.image-thumb img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.image-info {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.image-name { font-size: 13px; }
.delete-btn { position: absolute; top: 4px; right: 4px; opacity: 0; }
.image-card:hover .delete-btn { opacity: 1; }
</style>
