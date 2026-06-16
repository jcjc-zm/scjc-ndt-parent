<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deptApi } from '@/api/dept'

const loading = ref(false)
const treeData = ref([])
const defaultExpandedKeys = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const editingNode = ref(null)
const submitting = ref(false)

const form = ref({
  parentId: null,
  deptName: '',
  deptType: 'BU',
  buName: '',
  sort: 0,
})

const deptTypeOptions = [
  { label: '事业部', value: 'BU' },
  { label: '项目部', value: 'PROJECT' },
]

const buOptions = ['成都', '重庆', '新疆', '长庆']

async function loadTree() {
  loading.value = true
  try {
    const res = await deptApi.tree()
    if (res?.data) {
      treeData.value = res.data
      defaultExpandedKeys.value = res.data.map((d) => d.id)
    }
  } finally {
    loading.value = false
  }
}

function openCreateDialog(parentNode) {
  dialogTitle.value = parentNode ? `在「${parentNode.deptName}」下新增` : '新增部门'
  editingNode.value = null
  form.value = {
    parentId: parentNode?.id || null,
    deptName: '',
    deptType: parentNode?.deptType === 'BU' ? 'PROJECT' : 'BU',
    buName: parentNode?.buName || '',
    sort: 0,
  }
  dialogVisible.value = true
}

function openEditDialog(node) {
  dialogTitle.value = `编辑「${node.deptName}」`
  editingNode.value = node
  form.value = {
    parentId: node.parentId,
    deptName: node.deptName,
    deptType: node.deptType,
    buName: node.buName || '',
    sort: node.sort || 0,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.deptName.trim()) {
    ElMessage.warning('请输入部门名称')
    return
  }
  submitting.value = true
  try {
    if (editingNode.value) {
      await deptApi.update(editingNode.value.id, { ...form.value, id: editingNode.value.id })
      ElMessage.success('部门已更新')
    } else {
      await deptApi.create(form.value)
      ElMessage.success('部门已创建')
    }
    dialogVisible.value = false
    loadTree()
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(node) {
  try {
    await ElMessageBox.confirm(`确认删除部门「${node.deptName}」？如有子部门将无法删除。`, '删除确认', {
      type: 'warning',
    })
    await deptApi.remove(node.id)
    ElMessage.success('已删除')
    loadTree()
  } catch { /* cancelled */ }
}

function getDeptTypeTag(type) {
  if (type === 'COMPANY') return { label: '公司', type: 'danger' }
  if (type === 'BU') return { label: '事业部', type: 'primary' }
  if (type === 'PROJECT') return { label: '项目部', type: 'success' }
  return { label: type, type: 'info' }
}

onMounted(() => loadTree())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">组织架构</h2>
        <p class="page-subtitle">管理公司、事业部与项目部层级</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog(null)">新增部门</el-button>
    </div>

    <el-card shadow="never" class="content-card" v-loading="loading">
      <el-tree
        :data="treeData"
        :default-expanded-keys="defaultExpandedKeys"
        node-key="id"
        :props="{ label: 'deptName', children: 'children' }"
        highlight-current
      >
        <template #default="{ node, data }">
          <div class="tree-node-row">
            <span class="node-name">{{ data.deptName }}</span>
            <el-tag :type="getDeptTypeTag(data.deptType).type" size="small" effect="plain">
              {{ getDeptTypeTag(data.deptType).label }}
            </el-tag>
            <span v-if="data.buName" class="text-secondary bu-name">{{ data.buName }}</span>
            <span class="node-actions">
              <el-button link type="primary" :icon="Plus" size="small" @click.stop="openCreateDialog(data)">子部门</el-button>
              <el-button link type="warning" :icon="EditPen" size="small" @click.stop="openEditDialog(data)">编辑</el-button>
              <el-button link type="danger" :icon="Delete" size="small" @click.stop="handleDelete(data)">删除</el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="上级部门">
          <el-input :model-value="editingNode?.parentId || form.parentId || '顶级'" disabled />
        </el-form-item>
        <el-form-item label="部门名称" required>
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门类型">
          <el-select v-model="form.deptType" style="width: 100%">
            <el-option v-for="opt in deptTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属事业部" v-if="form.deptType !== 'COMPANY'">
          <el-select v-model="form.buName" style="width: 100%" clearable placeholder="选择事业部">
            <el-option v-for="bu in buOptions" :key="bu" :label="bu + '事业部'" :value="bu" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: var(--space-6); }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-secondary); margin: 0; }
.content-card { border-radius: var(--radius-md); }

.tree-node-row {
  display: flex; align-items: center; gap: 8px;
  padding: 4px 0; flex: 1;
}
.node-name { font-size: 14px; font-weight: 500; }
.bu-name { font-size: 12px; }
.node-actions { margin-left: auto; display: flex; gap: 4px; opacity: 0; transition: opacity var(--transition-fast); }
.tree-node-row:hover .node-actions { opacity: 1; }
</style>
