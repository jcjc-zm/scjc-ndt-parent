# 数据管理树形结构改造 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将"项目管理"模块改造为左侧组织树+右侧项目表格的"数据管理"页面

**Architecture:** 后端新增 `GET /api/projects/tree` 接口返回组织树（公司→事业部→项目），前端重建 ProjectListView.vue 为 el-tree + el-table 分栏布局

**Tech Stack:** Java 17 / Spring Boot 3.2.6 / MyBatis-Plus 3.5.7 / Vue 3 / Element Plus

---

### Task 1: 创建 TreeNode DTO

**Files:**
- Create: `src/main/java/com/scjc/ndt/dto/TreeNode.java`

- [ ] **Step 1: 编写 TreeNode 类**

```java
package com.scjc.ndt.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class TreeNode {
    private String id;
    private String label;
    private String type;        // COMPANY / BU / PROJECT
    private Long projectId;
    private String projectCode;
    private String status;
    private List<TreeNode> children = new ArrayList<>();
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/scjc/ndt/dto/TreeNode.java
git commit -m "feat: add TreeNode DTO for data management tree"
```

---

### Task 2: 更新 ProjectService 接口

**Files:**
- Modify: `src/main/java/com/scjc/ndt/service/ProjectService.java`

- [ ] **Step 1: 添加 getTree 方法签名**

```java
package com.scjc.ndt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scjc.ndt.dto.ProjectRequest;
import com.scjc.ndt.dto.TreeNode;
import com.scjc.ndt.entity.SysProject;

import java.util.List;

public interface ProjectService {
    IPage<SysProject> listProjects(Integer page, Integer size, String keyword, String projectType, Long userId);
    List<TreeNode> getTree(Long userId);                                                    // ← 新增
    SysProject create(ProjectRequest request, Long creatorId);
    SysProject update(Long id, ProjectRequest request);
    void updateStatus(Long id, String status);
    SysProject getById(Long id);
    List<SysProject> getByBuName(String buName);
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile
```

Expected: BUILD SUCCESS (ProjectServiceImpl 会因未实现新方法而报错，属于预期，Task 3 修复)

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/scjc/ndt/service/ProjectService.java
git commit -m "feat: add getTree method to ProjectService interface"
```

---

### Task 3: 实现 ProjectServiceImpl.getTree()

**Files:**
- Modify: `src/main/java/com/scjc/ndt/service/impl/ProjectServiceImpl.java`

- [ ] **Step 1: 注入 SysDeptMapper 并实现 getTree**

在 `ProjectServiceImpl` 中新增依赖和方法。找到类开头的字段声明区，添加 `SysDeptMapper`：

```java
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final SysProjectMapper projectMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper roleMapper;
    private final UserProjectRelMapper userProjectRelMapper;
    private final SysDeptMapper deptMapper;           // ← 新增
```

在文件末尾 `getByBuName` 方法后，新增 `getTree` 方法：

```java
    @Override
    public List<TreeNode> getTree(Long userId) {
        List<String> roles = getRoleCodes(userId);

        // 1. 查所有启用的部门
        List<SysDept> depts = deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, 1)
        );

        // 2. 找公司节点
        SysDept company = depts.stream()
            .filter(d -> "COMPANY".equals(d.getDeptType()))
            .findFirst().orElse(null);
        if (company == null) return List.of();

        // 3. 找出所有 BU 节点
        List<SysDept> buDepts = depts.stream()
            .filter(d -> "BU".equals(d.getDeptType()))
            .collect(Collectors.toList());

        // 4. 查所有未删除项目
        LambdaQueryWrapper<SysProject> projQ = new LambdaQueryWrapper<>();
        if (!roles.contains("SYSTEM_ADMIN") && !roles.contains("COMPANY_ADMIN")) {
            if (roles.contains("BU_ADMIN")) {
                // BU_ADMIN: 只看其负责的事业部的项目
                List<SysProject> userProjects = userProjectRelMapper.selectList(
                    new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
                ).stream().map(r -> projectMapper.selectById(r.getProjectId())).collect(Collectors.toList());
                List<String> buNames = userProjects.stream()
                    .map(SysProject::getBuName).filter(Objects::nonNull).distinct()
                    .collect(Collectors.toList());
                projQ.in(buNames.isEmpty() ? null : SysProject::getBuName, buNames.isEmpty() ? List.of("") : buNames);
            } else {
                // 其他角色: 只看分配的项目
                List<Long> projectIds = userProjectRelMapper.selectList(
                    new LambdaQueryWrapper<UserProjectRel>().eq(UserProjectRel::getUserId, userId)
                ).stream().map(UserProjectRel::getProjectId).collect(Collectors.toList());
                projQ.in(SysProject::getId, projectIds.isEmpty() ? Arrays.asList(-1L) : projectIds);
            }
        }
        List<SysProject> projects = projectMapper.selectList(projQ);

        // 5. 组装树
        TreeNode root = new TreeNode();
        root.setId("dept_" + company.getId());
        root.setLabel(company.getDeptName());
        root.setType("COMPANY");

        // 公司直属项目
        projects.stream()
            .filter(p -> "COMPANY_DIRECT".equals(p.getProjectType()))
            .forEach(p -> {
                TreeNode projNode = new TreeNode();
                projNode.setId("proj_" + p.getId());
                projNode.setLabel(p.getProjectName());
                projNode.setType("PROJECT");
                projNode.setProjectId(p.getId());
                projNode.setProjectCode(p.getProjectCode());
                projNode.setStatus(p.getStatus());
                root.getChildren().add(projNode);
            });

        // 各事业部及其项目
        for (SysDept bu : buDepts) {
            TreeNode buNode = new TreeNode();
            buNode.setId("dept_" + bu.getId());
            buNode.setLabel(bu.getDeptName());
            buNode.setType("BU");

            List<TreeNode> buProjects = projects.stream()
                .filter(p -> "BU_SUB".equals(p.getProjectType()) && bu.getBuName().equals(p.getBuName()))
                .map(p -> {
                    TreeNode projNode = new TreeNode();
                    projNode.setId("proj_" + p.getId());
                    projNode.setLabel(p.getProjectName());
                    projNode.setType("PROJECT");
                    projNode.setProjectId(p.getId());
                    projNode.setProjectCode(p.getProjectCode());
                    projNode.setStatus(p.getStatus());
                    return projNode;
                })
                .collect(Collectors.toList());
            buNode.setChildren(buProjects);

            root.getChildren().add(buNode);
        }

        return List.of(root);
    }
```

需要新增 import：

```java
import com.scjc.ndt.dto.TreeNode;
import com.scjc.ndt.mapper.SysDeptMapper;
import java.util.Objects;
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/scjc/ndt/service/impl/ProjectServiceImpl.java
git commit -m "feat: implement getTree with permission-aware org tree building"
```

---

### Task 4: 更新 ProjectController 新增 tree 接口

**Files:**
- Modify: `src/main/java/com/scjc/ndt/controller/ProjectController.java`

- [ ] **Step 1: 添加 getTree 端点**

在 `ProjectController` 类中新增方法：

```java
import com.scjc.ndt.dto.TreeNode;
import java.util.List;

    @GetMapping("/tree")
    public R<List<TreeNode>> tree(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(projectService.getTree(userId));
    }
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/scjc/ndt/controller/ProjectController.java
git commit -m "feat: add GET /api/projects/tree endpoint"
```

---

### Task 5: listProjects 支持 buName 参数

**Files:**
- Modify: `src/main/java/com/scjc/ndt/controller/ProjectController.java`
- Modify: `src/main/java/com/scjc/ndt/service/impl/ProjectServiceImpl.java`

- [ ] **Step 1: Controller 新增 buName 参数**

在 `ProjectController.list()` 方法签名中新增 `buName` 参数，并传入 service：

```java
    @GetMapping("/list")
    public R<IPage<SysProject>> list(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "20") Integer size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String projectType,
                                      @RequestParam(required = false) String buName,        // ← 新增
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(projectService.listProjects(page, size, keyword, projectType, buName, userId));
    }
```

- [ ] **Step 2: ServiceImpl 新增 buName 过滤**

在 `listProjects` 方法中增加 `buName` 参数和过滤条件：

```java
    @Override
    public IPage<SysProject> listProjects(Integer page, Integer size, String keyword,
                                           String projectType, String buName, Long userId) {
        Page<SysProject> p = new Page<>(page, size);
        LambdaQueryWrapper<SysProject> q = new LambdaQueryWrapper<>();

        // ... 现有的角色权限过滤代码保持不变 ...

        q.like(StringUtils.hasText(keyword), SysProject::getProjectName, keyword)
         .eq(StringUtils.hasText(projectType), SysProject::getProjectType, projectType)
         .eq(StringUtils.hasText(buName), SysProject::getBuName, buName)              // ← 新增
         .orderByDesc(SysProject::getCreateTime);
        return projectMapper.selectPage(p, q);
    }
```

同步更新 `ProjectService` 接口签名：

```java
IPage<SysProject> listProjects(Integer page, Integer size, String keyword, String projectType, String buName, Long userId);
```

- [ ] **Step 3: 编译验证**

```bash
mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/scjc/ndt/controller/ProjectController.java \
        src/main/java/com/scjc/ndt/service/impl/ProjectServiceImpl.java \
        src/main/java/com/scjc/ndt/service/ProjectService.java
git commit -m "feat: add buName filter to project list API"
```

---

### Task 6: 前端 API 模块新增 getTree

**Files:**
- Modify: `frontend/src/api/project.js`

- [ ] **Step 1: 添加 getTree 方法**

在 `projectApi` 对象中新增：

```js
import request from '@/utils/request'

export const projectApi = {
  list(params) {
    return request.get('/projects/list', { params })
  },
  getTree() {                                          // ← 新增
    return request.get('/projects/tree')
  },
  getById(id) {
    return request.get(`/projects/${id}`)
  },
  create(data) {
    return request.post('/projects', data)
  },
  update(id, data) {
    return request.put(`/projects/${id}`, data)
  },
  updateStatus(id, status) {
    return request.put(`/projects/${id}/status`, { status })
  },
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/api/project.js
git commit -m "feat: add getTree to project API"
```

---

### Task 7: 更新路由 title

**Files:**
- Modify: `frontend/src/router/index.js`

- [ ] **Step 1: 修改路由 meta**

找到项目管理路由定义（`path: 'project'`），将 `title` 改为 "数据管理"：

```js
        {
          path: 'project',
          name: 'ProjectList',
          component: () => import('@/views/project/ProjectListView.vue'),
          meta: { title: '数据管理', icon: 'FolderOpened' },    // ← 改 title
        },
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/router/index.js
git commit -m "feat: rename project management to data management in router"
```

---

### Task 8: 更新侧边栏菜单文字

**Files:**
- Modify: `frontend/src/components/Layout/Sidebar.vue`

- [ ] **Step 1: 修改菜单项 title**

找到 `menuItems` 中的项目管理项，改为 "数据管理"：

```js
    {
      index: '/project',
      title: '数据管理',          // ← 改 title
      icon: 'FolderOpened',
      visible: true,
    },
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/Layout/Sidebar.vue
git commit -m "feat: rename project management to data management in sidebar"
```

---

### Task 9: 重建 ProjectListView.vue 为树+表格布局

**Files:**
- Modify: `frontend/src/views/project/ProjectListView.vue`

这是本次改动最大的文件，用完整的 `<script setup>` + `<template>` + `<style scoped>` 替换整个文件。

- [ ] **Step 1: 替换为新的树+表格布局**

完整文件内容：

```vue
<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened, Document } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { projectApi } from '@/api/project'

const router = useRouter()
const userStore = useUserStore()

// --- Tree state ---
const treeData = ref([])
const treeLoading = ref(false)
const treeFilterText = ref('')
const currentNodeKey = ref('')
const defaultExpandedKeys = ref([])
const treeRef = ref(null)

// --- Table state ---
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selectedNodeType = ref('')   // COMPANY / BU
const selectedBuName = ref('')
const selectedProjectType = ref('')

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
})

// Status tag mapping
const statusMap = {
  PENDING: { label: '待启动', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
}

// Load tree
async function loadTree() {
  treeLoading.value = true
  try {
    const res = await projectApi.getTree()
    if (res?.data) {
      treeData.value = res.data
      if (res.data.length > 0) {
        defaultExpandedKeys.value = [res.data[0].id]
        currentNodeKey.value = res.data[0].id
        selectedNodeType.value = 'COMPANY'
      }
    }
  } catch {
    // handled by interceptor
  } finally {
    treeLoading.value = false
  }
}

// Tree node click
function handleNodeClick(data) {
  if (data.type === 'PROJECT') {
    router.push(`/project/${data.projectId}/detail`)
    return
  }
  currentNodeKey.value = data.id
  selectedNodeType.value = data.type
  if (data.type === 'COMPANY') {
    selectedBuName.value = ''
    selectedProjectType.value = ''
  } else if (data.type === 'BU') {
    selectedBuName.value = data.label
    selectedProjectType.value = 'BU_SUB'
  }
  query.page = 1
  loadTable()
}

// Filter tree by text
watch(treeFilterText, (val) => {
  treeRef.value?.filter(val)
})

function filterNode(value, data) {
  if (!value) return true
  return data.label.includes(value)
}

// Load table
async function loadTable() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
    }
    if (selectedNodeType.value === 'COMPANY') {
      // 公司节点：显示所有项目，不设过滤条件
    } else if (selectedNodeType.value === 'BU') {
      params.buName = selectedBuName.value
    }
    const res = await projectApi.list(params)
    if (res?.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

// Search
function handleSearch() {
  query.page = 1
  loadTable()
}

function handleReset() {
  query.keyword = ''
  query.page = 1
  loadTable()
}

// Pagination
function handlePageChange(page) {
  query.page = page
  loadTable()
}

function handleSizeChange(size) {
  query.size = size
  query.page = 1
  loadTable()
}

// Actions
function goToCreate() {
  router.push('/project/create')
}

function goToDetail(row) {
  router.push(`/project/${row.id}/detail`)
}

function goToInspection(row) {
  router.push(`/inspection/${row.id}/entry`)
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 'IN_PROGRESS' ? 'COMPLETED' : 'IN_PROGRESS'
  const action = newStatus === 'COMPLETED' ? '完成' : '启动'
  try {
    await ElMessageBox.confirm(
      `确认将项目「${row.projectName}」标记为${action}？`,
      '状态变更',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    await projectApi.updateStatus(row.id, newStatus)
    ElMessage.success(`项目已${action}`)
    loadTable()
    loadTree()
  } catch {
    // Cancelled
  }
}

onMounted(() => {
  loadTree().then(() => loadTable())
})
</script>

<template>
  <div class="page-container">
    <!-- Page header -->
    <div class="page-header">
      <div>
        <h2 class="page-title">数据管理</h2>
        <p class="page-subtitle">按组织架构浏览和管理项目数据</p>
      </div>
      <el-button
        v-if="userStore.canCreateProject"
        type="primary"
        :icon="Plus"
        @click="goToCreate"
      >
        创建立项
      </el-button>
    </div>

    <!-- Content: tree + table -->
    <div class="content-wrapper">
      <!-- Left: Tree -->
      <div class="tree-panel">
        <div class="tree-header">
          <span class="tree-title">组织架构</span>
        </div>
        <el-input
          v-model="treeFilterText"
          placeholder="搜索节点..."
          :prefix-icon="Search"
          clearable
          size="small"
          class="tree-search"
        />
        <div class="tree-body" v-loading="treeLoading">
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="{ children: 'children', label: 'label' }"
            node-key="id"
            :default-expanded-keys="defaultExpandedKeys"
            :current-node-key="currentNodeKey"
            :filter-node-method="filterNode"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node" :class="`tree-node--${data.type.toLowerCase()}`">
                <el-icon v-if="data.type === 'COMPANY' || data.type === 'BU'" :size="16">
                  <FolderOpened />
                </el-icon>
                <el-icon v-else :size="16"><Document /></el-icon>
                <span class="tree-node-label">{{ data.label }}</span>
                <span v-if="data.type === 'PROJECT' && data.status" class="tree-node-status" :class="`status-${data.status.toLowerCase()}`" />
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- Right: Table -->
      <div class="table-panel">
        <!-- Filter bar -->
        <div class="filter-bar">
          <div class="filter-left">
            <el-input
              v-model="query.keyword"
              placeholder="搜索项目编号、名称..."
              :prefix-icon="Search"
              clearable
              style="width: 220px"
              size="default"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </div>
          <div class="filter-right">
            <span class="total-count">共 {{ total }} 个项目</span>
          </div>
        </div>

        <!-- Table -->
        <el-table
          :data="tableData"
          v-loading="loading"
          border
          stripe
          style="width: 100%"
          :empty-text="'请从左侧选择组织节点查看项目'"
        >
          <el-table-column prop="projectCode" label="项目编号" width="140" show-overflow-tooltip />
          <el-table-column prop="projectName" label="项目名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="unitProjectName" label="单位工程名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="buName" label="所属事业部" width="120" align="center">
            <template #default="{ row }">
              {{ row.buName || '公司直属' }}
            </template>
          </el-table-column>
          <el-table-column prop="constructionUnit" label="施工单位" width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusMap[row.status]?.type || 'info'" effect="light" size="small">
                {{ statusMap[row.status]?.label || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" align="center">
            <template #default="{ row }">
              {{ row.createTime?.replace('T', ' ').substring(0, 19) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click="goToDetail(row)">详情</el-button>
              <el-button link type="success" :icon="EditPen" @click="goToInspection(row)">录入</el-button>
              <el-button
                v-if="row.status !== 'COMPLETED'"
                link
                type="warning"
                :icon="SwitchButton"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 'IN_PROGRESS' ? '完成' : '启动' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination-wrap" v-if="total > query.size">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  flex-shrink: 0;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* Content wrapper */
.content-wrapper {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

/* Left tree panel */
.tree-panel {
  width: 260px;
  flex-shrink: 0;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tree-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
}

.tree-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.tree-search {
  margin: 8px 12px;
}

.tree-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

/* Tree node styling */
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.tree-node-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-left: auto;
}

.status-pending { background: #909399; }
.status-in_progress { background: #67c23a; }
.status-completed { background: #409eff; }

.tree-node--project {
  padding-left: 4px;
}

/* Right table panel */
.table-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  padding: 16px;
  overflow: hidden;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-shrink: 0;
  gap: 12px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.total-count {
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  flex-shrink: 0;
}

/* el-table inside table-panel should fill remaining space */
.table-panel :deep(.el-table) {
  flex: 1;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/project/ProjectListView.vue
git commit -m "feat: rebuild project list as tree + table data management view"
```

---

### Task 10: 端到端验证

- [ ] **Step 1: 启动后端，验证 API**

```bash
mvn spring-boot:run
```

测试接口（需要先登录获取 token）：

```bash
# 登录
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 获取树结构 (用实际 token 替换)
curl http://localhost:8088/api/projects/tree \
  -H "Authorization: Bearer <token>"
```

Expected: 返回树形 JSON，包含公司节点、事业部节点和项目叶子节点

- [ ] **Step 2: 启动前端，验证页面**

```bash
cd frontend && npm run dev
```

测试步骤：
1. 访问 `http://localhost:5173` 登录
2. 点击侧边栏「数据管理」
3. 确认左侧显示组织树（佳诚公司 → 四个事业部）
4. 点击公司节点 → 右侧表格显示项目列表
5. 点击事业部节点 → 右侧表格过滤
6. 点击项目节点 → 跳转详情页

- [ ] **Step 3: 提交**

```bash
git commit --allow-empty -m "chore: end-to-end verification passed for data management tree"
```
