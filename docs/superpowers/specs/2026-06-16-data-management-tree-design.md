# 数据管理 - 树形结构改造设计

**日期**: 2026-06-16  
**状态**: 已确认  
**范围**: 将"项目管理"模块改造为树形结构的"数据管理"

## 一、需求概述

1. 模块重命名："项目管理" → "数据管理"
2. 页面布局由平铺表格改为左侧组织树 + 右侧项目表格
3. 树结构：佳诚公司 → 公司直属项目 + 四个事业部（成都/重庆/新疆/长庆） → 各事业部项目
4. 点击树节点，右侧表格过滤显示对应项目
5. 右侧表格列与当前一致（项目编号、名称、类型、施工单位、状态、创建时间、操作）

## 二、树结构定义

```
佳诚公司 (COMPANY, dept_1)
├── 项目：公司直属项目A (PROJECT, proj_x)
├── 项目：公司直属项目B (PROJECT, proj_y)
├── 成都事业部 (BU, dept_2)
│   ├── 项目：成都某项目 (PROJECT, proj_z)
│   └── ...
├── 重庆事业部 (BU, dept_3)
├── 新疆事业部 (BU, dept_4)
└── 长庆事业部 (BU, dept_5)
```

- 公司节点下挂：公司直属项目（`projectType = COMPANY_DIRECT`）
- 每个事业部节点下挂：该事业部的项目（`projectType = BU_SUB`, `buName` 匹配）
- 树节点 id 格式：`dept_{id}` 或 `proj_{id}` 区分类型

## 三、后端改动

### 3.1 新增接口：`GET /api/projects/tree`

返回整个组织树结构，含项目叶子节点。

**响应示例**：
```json
[
  {
    "id": "dept_1",
    "label": "佳诚公司",
    "type": "COMPANY",
    "children": [
      { "id": "proj_1", "label": "公司直属项目A", "type": "PROJECT", "projectId": 1, "projectCode": "PRJ-001", "status": "IN_PROGRESS" },
      { "id": "dept_2", "label": "成都事业部", "type": "BU", "children": [...] },
      { "id": "dept_3", "label": "重庆事业部", "type": "BU", "children": [...] },
      { "id": "dept_4", "label": "新疆事业部", "type": "BU", "children": [...] },
      { "id": "dept_5", "label": "长庆事业部", "type": "BU", "children": [...] }
    ]
  }
]
```

**实现逻辑**（`ProjectServiceImpl.getTree()`）：
1. 查询 `SysDept` 公司节点（`deptType = COMPANY`）作为根
2. 查询 `SysDept` 中所有 BU 节点（`deptType = BU`）
3. 查询 `SysProject` 所有未删除项目
4. 组装：
   - 公司直属项目（`projectType = COMPANY_DIRECT`）挂到公司节点 children
   - 各 BU 事业部作为 children 挂到公司节点
   - 事业部项目按 `buName` 挂到对应 BU 的 children

### 3.2 现有列表接口补充参数

`GET /api/projects/list` 增加可选参数 `buName`，用于树节点点击时按事业部过滤。

### 3.3 新增 DTO：`TreeNode`

```java
@Data
public class TreeNode {
    private String id;          // "dept_1" 或 "proj_1"
    private String label;       // 显示名称
    private String type;        // COMPANY / BU / PROJECT
    private Long projectId;     // 仅 type=PROJECT 时有值
    private String projectCode; // 仅 type=PROJECT 时有值
    private String status;      // 仅 type=PROJECT 时有值
    private List<TreeNode> children;
}
```

## 四、前端改动

### 4.1 文件清单

| 文件 | 改动 |
|------|------|
| `router/index.js` | 路由 meta title 改为"数据管理" |
| `Sidebar.vue` | 菜单文字改为"数据管理" |
| `ProjectListView.vue` | 重建为树+表格布局，文件可保留原名或重命名 |
| `api/project.js` | 新增 `getTree()` 调用 |

### 4.2 页面布局（ProjectListView.vue 重建）

```
┌─────────────────────────────────────────────────┐
│ 数据管理                    [创建立项] 按钮       │
├────────────┬────────────────────────────────────┤
│ 搜索框     │ 筛选栏 + 表格区域                   │
│ (过滤树)   │                                    │
│            │ ┌──────────────────────────────┐   │
│ 🌳 树      │ │ 项目编号 │ 名称 │ ... │ 操作 │   │
│            │ ├──────────────────────────────┤   │
│ 佳诚公司   │ │ PRJ-001  │ xxx  │ ... │ ...  │   │
│  ├项目A    │ │ PRJ-002  │ yyy  │ ... │ ...  │   │
│  ├成都事业部│ └──────────────────────────────┘   │
│  │├项目B   │                                    │
│  │└项目C   │          分页器                     │
│  ├重庆事业部│                                    │
│  ...       │                                    │
└────────────┴────────────────────────────────────┘
```

- 左侧宽度约 260px，可拖拽调整
- 树默认展开第一层（佳诚公司）
- 树节点点击 → 更新右侧表格数据：
  - 点击公司节点 → 显示所有项目
  - 点击事业部节点 → 显示该 BU 的项目
  - 点击项目节点 → 跳转到项目详情
- 树上方搜索框可过滤树节点关键字
- 右侧表格保留现有分页、搜索、操作按钮

### 4.3 交互细节

- 树节点点击非项目节点（公司/事业部）→ 右侧表格按 `buName` 或 `projectType` 过滤，显示分页列表
- 树节点点击项目节点 → 直接跳转 `/project/{id}/detail`
- 项目节点旁显示状态小圆点（绿=进行中，灰=待启动，蓝=已完成）
- 创建立项成功后 → 刷新树和表格

## 五、不受影响的部分

- 项目创建流程（ProjectCreateView）不变
- 项目详情页（ProjectDetailView）不变
- 检测数据录入（InspectionEntryView）不变
- 报告管理、签字审批等其他模块不变
- `SysProject` 表结构不变
- 现有 API（CRUD）保持不变

## 六、边界情况

- 无项目的事业部仍然展示在树中，children 为空数组
- 逻辑删除的项目（`deleted=1`）不出现在树中
- 树 API 需尊重现有权限规则：不同角色看到的项目范围不同（如 PROJECT_ADMIN 只能看到自己被分配的项目）
- 公司直属项目可能为空，公司节点 children 中仅有 BU 节点

## 七、测试要点

1. 树结构正确渲染：公司 → BU → 项目
2. 公司直属项目出现在公司节点下
3. 点击不同 BU 节点，表格正确过滤
4. 点击项目节点，正确跳转详情页
5. 创建新项目后，树和表格正确刷新
6. 权限控制：不同角色看到的树结构应符合其权限范围
