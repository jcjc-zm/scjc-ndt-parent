# SCJC NDT 无损检测平台 - 设计方案

> 版本: v1.0 | 日期: 2026-06-16 | 状态: 已确认

---

## 一、系统架构与技术栈

### 整体架构

```
┌─────────────────────────────────────────────────┐
│                   前端 (Vue 3)                    │
│   Element Plus + Vxe Table + vue-draggable +     │
│   ECharts + Axios + Vue Router + Pinia           │
│   端口: 5173 (dev)                                │
├─────────────────────────────────────────────────┤
│                  Nginx (生产)                     │
│         静态资源 + API 反向代理 :8088             │
├─────────────────────────────────────────────────┤
│              后端 (Spring Boot 3.2.6)             │
│  controller → service → mapper (MyBatis-Plus)    │
│  端口: 8088                                       │
│  JWT 无状态认证 + BCrypt 密码加密                 │
│  统一响应 R<T>                                    │
├─────────────────────────────────────────────────┤
│                  MySQL 8.0+                       │
│              数据库: scjc_ndt                     │
└─────────────────────────────────────────────────┘
```

### 技术栈明细

| 层 | 技术 | 用途 |
|----|------|------|
| 前端框架 | Vue 3.4+ | Composition API + `<script setup>` |
| UI 组件库 | Element Plus 2.7+ | 表单/表格/弹窗/导航 |
| Excel 表格 | Vxe Table 4.5+ | 仿 Excel 在线编辑 |
| 拖拽 | vue-draggable-plus 0.4+ | 报告设计器 |
| 图表 | ECharts 5.5+ | 仪表盘 |
| 路由 | Vue Router 4.3+ | 权限路由守卫 |
| 状态 | Pinia 2.1+ | 用户/权限缓存 |
| HTTP | Axios 1.7+ | 请求拦截 JWT |
| PDF | html2canvas + jsPDF | 报告导出 |
| Excel | xlsx (SheetJS) 0.20+ | 导入导出 |
| 构建 | Vite 5+ | 开发/构建 |

### 前端项目结构

```
frontend/src/
├── api/                    # API 接口
├── assets/                 # 静态资源
├── components/             # 通用组件
│   ├── ExcelTable/         # 仿Excel表格
│   ├── ReportDesigner/     # 拖拽报告设计器
│   ├── SignaturePad/       # 签字组件
│   └── Layout/             # 布局组件
├── composables/            # 组合式函数
├── router/                 # 路由配置
├── stores/                 # Pinia 状态
├── utils/                  # 工具函数
├── views/                  # 页面视图
│   ├── dashboard/          # 驾驶舱
│   ├── project/            # 项目管理
│   ├── inspection/         # 检测数据录入
│   ├── report/             # 报告管理
│   ├── approval/           # 签字审批
│   ├── user/               # 用户管理
│   └── system/             # 系统设置
├── App.vue
└── main.js
```

### 后端项目结构

```
src/main/java/com/scjc/ndt/
├── common/          # R<T>, JwtUtils, GlobalExceptionHandler
├── config/          # SecurityConfig, MyBatisPlusConfig, CorsConfig
├── controller/      # 各模块 Controller
├── service/         # 接口
│   └── impl/        # 实现类
├── mapper/          # MyBatis-Plus BaseMapper
├── entity/          # 实体类
└── dto/             # request/response POJOs
```

---

## 二、数据库核心设计

### 组织与用户体系

**sys_dept**（部门/组织）
- id, parent_id, dept_name, dept_type (COMPANY/BU/PROJECT)
- bu_name（所属事业部：成都/重庆/新疆/长庆）
- 树形结构: 公司 → 事业部/直属项目 → 子项目

**sys_role**（6种固定角色，系统初始化写入，不可删除）
- SYSTEM_ADMIN（系统管理员）— 全局最高权限
- COMPANY_ADMIN（公司级管理员）— 可创建事业部管理员
- BU_ADMIN（事业部管理员）— 可立项 + 创建项目管理员
- PROJECT_ADMIN（项目管理员）— 仅操作指定项目
- TECHNICAL_LEADER（技术负责人）— 报告第一道签字
- PROJECT_MANAGER（项目经理）— 报告最终签字

**sys_user**（用户）
- id, username, password(BCrypt), real_name, phone, email, status
- dept_id → sys_dept.id

**user_role_rel**（用户角色多对多）
- user_id, role_id（唯一约束）

**user_project_rel**（用户-项目绑定，项目管理员专用）
- user_id, project_id

### 权限层级

```
系统管理员 → 公司管理员 → 事业部管理员 → 项目管理员/技术负责人/项目经理
逐级创建，一人可多角色，项目管理员数据按 user_project_rel 隔离
```

### 项目表 sys_project

```sql
sys_project (
  id, project_code, project_name, unit_project_name,
  parent_id,           -- null=直属公司 / 指向事业部
  bu_name,             -- 所属事业部
  project_type,        -- COMPANY_DIRECT / BU_SUB
  construction_unit,   -- 施工单位
  status,              -- PENDING/IN_PROGRESS/COMPLETED
  ...
)
```

### 检测数据表 inspection_record

核心字段标准列存储，15组缺陷位置用 JSON 列：

```sql
inspection_record (
  id, project_id,
  -- 工程信息
  construction_unit, weld_no, instruction_no, instruction_date,
  inspection_method,    -- RT/UT/PT/MT/AUT/PA/TOFD/DR
  project_name, unit_project_name, bu_dept,
  specification, material, groove_type, position, welding_method,
  ratio, inspection_standard, qualified_level, inspection_item,
  welder_code, welding_dept, inspection_length, process_card_no,
  sampling_instruction_no, inspection_date,
  -- 结果
  result_level,        -- Ⅰ/Ⅱ/Ⅲ/Ⅳ
  inspection_conclusion,
  unqualified_handling,
  -- 缺陷报告信息
  report_defect_position, report_defect_nature, report_defect_length,
  unqualified_defect_type,
  remark, inspector_name,
  -- 底片 (RT专用)
  box_no, film_length, film_count,
  -- 文件
  image_url, inspection_report_url, original_record_url,
  -- 缺陷位置 (最多15组, JSON)
  defect_positions JSON,
  ...
)
```

### 报告与签字

**report_template**（报告模板）
- id, project_id, template_name, method_type
- template_type (SYSTEM/CUSTOM) — 系统预设 or 自定义
- layout_config JSON — 拖拽生成的字段布局
- image_area_config JSON — 图片放置区配置（位置、尺寸、图片来源）

**report_record**（生成报告）
- id, inspection_id, template_id
- report_no, report_content JSON
- image_selections JSON — 用户选择的图片（从系统图片库中选取）
- status (DRAFT/PENDING_SIGN/SIGNED/REJECTED)
- pdf_url

**signature_record**（签字记录）
- id, report_id, signatory_id, signatory_role
- sign_order (1=技术负责人, 2=项目经理)
- sign_status, sign_time, comment
- signature_image_url

**system_image**（系统图片库）
- id, image_name, image_url, image_type
- technique_type — 关联透照方式（双壁双影/单壁透照/中心透照...）
- project_id (可选，项目专属图片)
- upload_by, upload_time

---

## 三、权限与角色系统

### 数据权限规则

| 角色 | 数据可见范围 |
|------|------------|
| 系统管理员 | 全部项目 |
| 公司管理员 | 公司直属项目 + 所有事业部项目 |
| 事业部管理员 | 本事业部下属项目 |
| 项目管理员 | 仅 user_project_rel 绑定的项目 |
| 技术负责人 | 绑定的项目（签字权） |
| 项目经理 | 绑定的项目（签字权） |

### 操作权限

- 立项 → 系统管理员 + 事业部管理员
- 数据录入 → 项目管理员 + 事业部管理员
- 报告设计 → 项目管理员（系统模板 or 自定义）
- 报告生成 → 项目管理员 + 事业部管理员
- 签字 → 技术负责人 → 项目经理（串行）
- 用户管理 → 逐级创建（系统→公司→事业部→项目）
- 系统配置 → 系统管理员

### 后端实现

```java
@RequireRole({"SYSTEM_ADMIN", "BU_ADMIN"})     // 角色级
@RequireProjectData                              // 数据过滤
// MyBatis-Plus 拦截器自动注入 WHERE project_id IN (...)
```

---

## 四、API 设计

```
/api/auth
  POST   /login, /logout
  GET    /me

/api/users
  GET    /list
  POST   /                    # 逐级创建
  PUT    /{id}, /{id}/status
  POST   /{id}/roles, /{id}/projects

/api/projects
  GET    /list, /{id}
  POST   /                    # 立项
  PUT    /{id}, /{id}/status

/api/inspections
  GET    /list, /{id}
  POST   /, PUT /{id}, DELETE /{id}
  POST   /batch-import        # Excel导入
  GET    /export               # Excel导出
  GET    /project/{id}/template

/api/reports
  GET    /list, /{id}, /{id}/pdf
  POST   /                    # 生成报告
  DELETE /{id}

/api/templates
  GET    /list, /{id}
  POST   /, PUT /{id}, DELETE /{id}

/api/signatures
  GET    /pending, /{id}/history
  POST   /{id}/sign

/api/dashboard
  GET    /overview, /project-distribution
  GET    /weekly-workload, /inspection-stats, /my-todos

/api/images
  GET    /list                 # 系统图片库
  POST   /upload               # 上传图片
  DELETE /{id}

/api/depts
  GET    /tree, /list
  POST   /, PUT /{id}
```

---

## 五、前端路由

```
/login                              # 登录
/                                   # 主布局（侧边栏 + 顶栏）

/dashboard                          # 驾驶舱首页

/project                            # 项目管理
  /project/list                     # 项目列表
  /project/create                   # 创建立项
  /project/:id/detail               # 项目详情 → 含数据录入入口

/inspection                         # 检测数据
  /inspection/:projectId/entry      # Excel式录入

/report                             # 报告管理
  /report/list                      # 报告列表
  /report/design/:id                # 拖拽设计器
  /report/preview/:id               # 报告预览
  /report/create/:inspectionId      # 选择模板 → 生成

/approval                           # 签字审批
  /approval/pending                 # 待签字
  /approval/history                 # 已签字

/user                               # 用户管理
  /user/list, /user/create

/system                             # 系统设置
  /system/dept, /system/role
```

路由守卫: 无 token → /login → 获取用户权限 → 过滤菜单 → 无权限 → 403

---

## 六、UI 设计系统

- **风格**: Data-Dense Dashboard（数据密集型仪表盘）
- **配色**: 工业灰蓝 #334155 + 翠绿 #059669 + 背景 #F8FAFC
- **字体**: Fira Sans（正文）+ Fira Code（数据/编号）
- **布局**: 经典侧边栏（深色导航 + 浅色内容区）
- **支持**: 浅色/深色双模式

---

## 七、核心页面设计

### 7.1 仪表盘

- 5 个 KPI 卡片（检测总任务、合格率、待处理、本周新增、活跃项目）
- 检测趋势柱状图（30天）
- 检测方法分布进度条
- 周工作量统计表（事业部 × 检测方法交叉统计）

### 7.2 检测数据录入（仿 Excel）

- 每个立项项目自动拥有独立数据表
- 点击单元格直接编辑，失焦自动保存
- Tab/Enter/↑↓←→ 键盘导航
- Ctrl+C/V 复制粘贴，Excel 批量导入导出
- 底部空行"点击输入..."即时新增
- 技术选型: vxe-table + xlsx (SheetJS)

### 7.3 报告设计器（三栏布局）

- **左侧**: 字段组件池（分组：基础信息/检测参数/检测结果/附加/图片）
- **中间**: A4 画布所见即所得，拖拽字段自由排版
- **右侧**: 属性面板（标签名、数据绑定、字体、宽度）
- **图片区**: 每个模板预留图片放置区，图片从系统图片库选择
- **模板来源**: 系统预设模板（基于 PDF 参考） + 用户自定义模板
- 技术选型: vue-draggable-plus + html2canvas + jsPDF

### 7.4 签字流程

- 报告生成 → 技术负责人审阅签字 → 项目经理审阅签字（串行）
- 待签字列表 + 签字历史追踪

---

## 八、报告模板系统

### 模板类型

| 类型 | 说明 | 创建方式 |
|------|------|---------|
| 系统预设模板 | 基于 PDF 参考的固定模板 | 系统初始化时写入数据库 |
| 自定义模板 | 项目管理员通过设计器创建 | 拖拽字段 + 设置布局 |

### 图片区设计

- 每个模板必须包含至少一个**图片放置区**
- 图片从系统图片库中选择（检测底片、透照示意图等）
- 系统图片库预置 5 种透照方式示意图（双壁双影/双壁双影纵/双壁单影/单壁透照/中心透照）
- 项目管理员可上传项目专属图片

### 报告生成流程

```
检测记录 → 选择模板（系统/自定义）→ 自动填充数据 → 
选择图片 → 预览 → 生成报告 → 签字流程
```

---

## 九、关键业务规则

1. **逐级创建**: 上级管理员只能创建直接下级角色的用户
2. **项目数据隔离**: 项目管理员只能查看/操作被分配的项目数据
3. **双重项目归属**: 公司级直属项目 + 事业部下属项目
4. **报告双签**: 技术负责人 → 项目经理，串行不可跳过
5. **检测方法 x 8**: RT/UT/PT/MT/AUT/PA/TOFD/DR
6. **缺陷位置 x 15**: 每条记录最多记录 15 组缺陷信息
7. **Excel 即保存**: 单元格失焦自动保存，无需逐行确认
8. **模板双来源**: 系统预设 + 用户自定义
