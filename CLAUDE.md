# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

### Backend (Spring Boot)

```bash
# Build
mvn clean compile          # compile only
mvn clean package          # compile + package + run any tests

# Run
mvn spring-boot:run        # development server on port 8088
java -jar target/scjc-ndt-parent-1.0-SNAPSHOT.jar

# Tests (spring-boot-starter-test is configured, uses JUnit 5 + Mockito)
mvn test                                          # run all tests
mvn test -Dtest=ClassName                         # run single test class
mvn test -Dtest=ClassName#methodName              # run single test method
```

### Frontend (Vue 3 + Vite)

```bash
cd frontend
npm install               # install dependencies
npm run dev               # dev server on port 5173, proxies /api → localhost:8088
npm run build             # production build

# Browser testing (Playwright)
npx playwright test        # run Playwright tests
```

Full-stack dev: start backend (`mvn spring-boot:run`) and frontend (`npm run dev`) concurrently, then open `http://localhost:5173`.

**Note:** No Maven wrapper (`mvnw`) is included — Maven must be installed and on `PATH`. Tests use JUnit 5 (`@Test`, `@DisplayName`) + Mockito (`mock()`, `when()`) — see `FlexibleLocalDateDeserializerTest` for the pattern.

### Vite dev proxy

The Vite dev server proxies `/api` → `http://localhost:8088` (see `frontend/vite.config.js`). Path alias `@` resolves to `frontend/src/` for imports.

## Architecture

**SCJC NDT 无损检测管理系统** — Spring Boot 3.2.6 + Vue 3 monorepo with stateless JWT authentication.

### Tech stack
**Backend:** Java 17 · Spring Boot 3.2.6 · Spring Security · MyBatis-Plus 3.5.7 · MySQL · JWT (jjwt 0.12.6) · Lombok
**Frontend:** Vue 3.5 · Vite 8 · Element Plus 2.14 · Pinia 3 · Vue Router 4 · ECharts 6 · Axios · VXE Table · html2canvas + jsPDF (report export) · SheetJS/xlsx (Excel) · @vueuse/core (composition utilities)

### Backend layer structure (standard MVC)

Base package: `com.scjc.ndt`. Entry point: `NdtApplication` — `@SpringBootApplication` + `@MapperScan("com.scjc.ndt.mapper")` (required for MyBatis-Plus to discover mapper interfaces).

```
controller/  →  service/  →  mapper/ (MyBatis-Plus BaseMapper)
                          →  entity/ (MyBatis-Plus-annotated POJOs with @TableName)
common/       —  R<T> (unified response), JwtUtils, GlobalExceptionHandler, BusinessException, FlexibleLocalDateDeserializer
config/       —  SecurityConfig, JwtAuthenticationFilter, CorsConfig, MyBatisPlusConfig
dto/          —  request/response POJOs with jakarta.validation (UserInfo, TreeNode, PageQuery, etc.)
```

### Business modules (12 domain areas)

| Module | Controller | Service | Mapper(s) | Description |
|--------|-----------|---------|-----------|-------------|
| Auth | `AuthController` | `AuthService` | — | Login + `/me` endpoint |
| User | `UserController` | `UserService` | `SysUserMapper`, `UserRoleRelMapper`, `UserProjectRelMapper` | CRUD + role/project assignment |
| Dept | `DeptController` | `DeptService` | `SysDeptMapper` | Org tree (COMPANY/BU/PROJECT) |
| Role | `RoleController` | `RoleService` | `SysRoleMapper` | Fixed 6 roles |
| Project | `ProjectController` | `ProjectService` | `SysProjectMapper` | Project CRUD |
| Inspection | `InspectionController` | `InspectionService` | `InspectionRecordMapper` | Inspection data entry |
| Report | `ReportController` | `ReportService` | `ReportRecordMapper`, `ReportTemplateMapper` | Report generation + export |
| Signature | `SignatureController` | `SignatureService` | `SignatureRecordMapper` | Two-step approval workflow |
| Template | `TemplateController` | `TemplateService` | `ReportTemplateMapper` | Report template design |
| Dashboard | `DashboardController` | `DashboardService` | — | Aggregated statistics |
| ProcessCard | `ProcessCardController` | `ProcessCardService` | `ProcessCardMapper` | Process card CRUD (工艺卡管理) |
| Image | `ImageController` | `ImageService` | `SystemImageMapper` | Image library (films/diagrams) |

### Key design decisions

- **Unified response wrapper**: All controllers return `R<T>` (fields: `code`, `message`, `data`). Success = 200. Services throw `BusinessException(code, message)` for known errors → `GlobalExceptionHandler` converts to `R.error(code, message)`. Validation errors (`MethodArgumentNotValidException`) return code 400. Unexpected exceptions return code 500. Controllers never catch — all error handling is centralized in the exception handler.
- **Stateless JWT auth**: `SecurityConfig` disables CSRF, sets `SessionCreationPolicy.STATELESS`. `JwtAuthenticationFilter` (a `OncePerRequestFilter`) skips `/api/auth/**` and OPTIONS; for all other requests it extracts `Authorization: Bearer <token>`, validates via `JwtUtils`, sets `request.setAttribute("userId"/"username")`, and builds a Spring Security `UsernamePasswordAuthenticationToken` into `SecurityContextHolder`.
- **Public endpoints**: `POST /api/auth/login`, all `/api/auth/**`, `/api/public/**`, and OPTIONS are unauthenticated. Everything else requires a valid JWT.
- **Password encoding**: BCrypt via `PasswordEncoder` bean. Seed accounts (admin) in `doc/sql/init.sql` use `admin123`.
- **MyBatis-Plus conventions**: `BaseMapper<T>` + `IService<T>`/`ServiceImpl<M, T>` for CRUD. Logical delete on `deleted` column (0/1). Auto-fill on `createTime`/`updateTime` via `MetaObjectHandler`. Pagination via `PaginationInnerInterceptor(DbType.MYSQL)`. Underscore-to-camelCase mapping enabled. SQL logging to stdout.
- **DataInitializer**: `CommandLineRunner` that seeds a default SYSTEM report template ("胶片射线报告A") on first startup. Uses WJ-11 (首页) + WJ-12 (附页) standard format with RT technical parameters, inspection result statistics, image area, signature blocks, and an appendix weld detail table.
- **InspectionRecord**: The largest entity — contains both generic inspection fields and 40+ RT射线检测 (film radiography) specific fields including film parameters, source/equipment details, exposure parameters, defect tracking, and yield statistics. Some RT fields overlap with ProcessCard fields (both describe technique parameters) but serve different purposes: ProcessCard defines the technique *specification*, InspectionRecord captures the *as-performed* values.
- **Entity → DTO conversion**: `UserInfo` is the safe-to-expose user representation (no password). Conversion happens in service layer via `toUserInfo()`.
- **CORS**: `CorsConfig` allows all origins (`*`) with credentials, all methods (GET/POST/PUT/DELETE/OPTIONS), and all headers.
- **User identity in controllers**: Retrieve via `request.getAttribute("userId")` (Long) and `request.getAttribute("username")` (String).
- **Date deserialization**: `FlexibleLocalDateDeserializer` (custom Jackson `JsonDeserializer`) handles 7 date formats (`yyyy-MM-dd`, `yyyy/M/d`, `yyyy.M.d`, `yyyyMMdd`, etc.) plus Excel serial numbers (e.g., `45735` → `2025-03-19`). Used via `@JsonDeserialize(using = ...)` on `LocalDate` fields in `InspectionRequest` and `ProcessCardRequest`.
- **Validation groups**: DTOs use `jakarta.validation` with groups for differential validation. E.g., `InspectionRequest` defines `OnCreate` interface — `@NotBlank(groups = OnCreate.class)` on `weldNo` enforces it only during create, not update. Controllers annotate with `@Validated(OnCreate.class)` or `@Valid` (no groups).
- **Org tree**: `GET /api/projects/tree` returns a permission-aware nested org tree (`TreeNode`: id, label, type, children, buName). Users see only projects in their scope (SYSTEM_ADMIN sees all, BU_ADMIN sees their BU's projects, etc.).

### Role hierarchy (6 fixed roles)

| Role | Code | Description |
|------|------|-------------|
| 系统管理员 | `SYSTEM_ADMIN` | Global admin, manages everything |
| 公司级管理员 | `COMPANY_ADMIN` | Manages company-direct projects, can create BU admins |
| 事业部管理员 | `BU_ADMIN` | Manages BU projects, can create projects and project admins |
| 项目管理员 | `PROJECT_ADMIN` | Manages assigned project data & reports |
| 技术负责人 | `TECHNICAL_LEADER` | First signatory for report approval |
| 项目经理 | `PROJECT_MANAGER` | Second (final) signatory for report approval |

### Report workflow

```
Inspection data entry → Generate report (select template) → 
  Draft → Submit for signature → 
    TECHNICAL_LEADER signs (order 1) → 
      PROJECT_MANAGER signs (order 2) → 
        SIGNED (final state)
```

Report export uses `html2canvas` (DOM → canvas) + `jsPDF` (canvas → PDF) for client-side PDF generation. Excel import/export uses `xlsx` (SheetJS).

### Frontend structure
```
frontend/src/
  api/          — per-module HTTP request modules (auth, user, project, etc.)
  stores/       — Pinia stores (user.js for auth state, app.js for app state)
  router/       — Vue Router with auth guard (beforeEach checks token)
  utils/        — Axios instance with interceptors (request.js)
  views/        — page components grouped by module
  components/   — shared components (Layout)
  styles/       — global CSS
```

- **HTTP client**: `src/utils/request.js` — Axios instance with base `/api`, JWT injected via request interceptor, `R<T>` unwrapped in response interceptor, 401 triggers redirect to `/login`.
- **API module convention**: Each `src/api/*.js` imports `request` and exports functions that call `request.get/post/put/delete(path, params)`. The response interceptor returns `response.data` (the full `R<T>` wrapper: `{ code, message, data }`), so callers access the payload via `.data` on the returned object.
- **Auth guard**: `router.beforeEach` checks `localStorage.getItem('token')`, redirects to `/login?redirect=...` if missing on protected routes.
- **State**: Pinia stores. `user.js` holds token + userInfo in localStorage.
- **UI library**: Element Plus (primary). VXE Table for complex tables (reports, user lists, etc.). ECharts for dashboard charts. `vue-draggable-plus` for template layout drag & drop. The inspection entry page uses jspreadsheet-ce (see below) instead of VXE Table.
- **Project statuses**: Defined in `src/constants/projectStatus.js` — `PENDING` (待启动), `IN_PROGRESS` (进行中), `COMPLETED` (已完成).

### Frontend routing

```
/login                          → LoginView (public)
/                               → MainLayout (auth required)
  /dashboard                    → DashboardView
  /project                      → ProjectListView
  /project/create               → ProjectCreateView
  /project/:id/detail           → ProjectDetailView
  /inspection/:projectId/entry  → InspectionEntryView (data entry grid)
  /process-card                 → ProcessCardView (process card management)
  /report                       → ReportListView
  /report/design/:id            → ReportDesignView (template designer)
  /report/preview/:id           → ReportPreviewView
  /report/create/:inspectionId  → ReportCreateView
  /template                     → TemplateManageView (template list & management)
  /approval                     → ApprovalView (signature workflow)
  /user                         → UserListView
  /system/dept                  → DeptManageView
  /system/role                  → RoleManageView
  /system/images                → ImageLibraryView
/403                            → Forbidden
/:pathMatch(.*)*                → 404
```

### Inspection data entry (jspreadsheet integration)

The inspection entry page uses **jspreadsheet-ce v5.0.4** with a specific setup:

- **`tableOverflow: false`** — jspreadsheet renders the full table without internal scroll; scrolling is handled by a wrapper `.sheet-wrap` div with `overflow: auto`. This gives browser-native scrollbars (both vertical + horizontal), sticky thead, and Excel-like behavior.
- **Instance lookup**: jspreadsheet-ce v5 injects the worksheet instance on the child `.jss_container` element (`.jss_container.jspreadsheet`), not on the container element itself (v4 behavior). The instance IS the worksheet — it has `insertRow()`, `getData()`, `setData()`, `getSelected()` directly. No `.current` wrapper.
- **`onchange` caveat**: The `onchange` callback is unreliable with `tableOverflow: false`. Use DOM-level `click`/`keydown` listeners on the spreadsheet container to detect user interaction.
- **Infinite scroll**: 2000 empty rows pre-padded via `buildSheetData()`. A 400ms `setInterval` poller + scroll event listener checks `scrollHeight - scrollTop - clientHeight < 400px` and calls `worksheet.insertRow(200)` to extend.
- **Save**: Manual save via button or Ctrl+S. Calls `worksheet.getData(false)` to collect all rows, then creates/updates via API. `touched` ref tracks unsaved modifications; `beforeunload` warns on leave.
- **Keyboard navigation**: Arrow keys/Tab/Enter scroll the active cell to center of viewport (with boundary clamping at start/end of sheet).
- **Playwright** is available (`npm install` includes it) for browser-based verification of the inspection entry page.

### Database
- Database: `scjc_ndt` (MySQL, utf8mb4)
- Initial schema & seed data: `doc/sql/init.sql`
- Seed account: `admin` / `admin123` (SYSTEM_ADMIN)
- 12 tables: `sys_dept`, `sys_role`, `sys_user`, `user_role_rel`, `user_project_rel`, `sys_project`, `inspection_record`, `process_card`, `system_image`, `report_template`, `report_record`, `signature_record`

### Default config (application.yml)
- Port: `8088`
- MySQL: `jdbc:mysql://localhost:3306/scjc_ndt`, user `root`, password `123456`
- JWT: `scjc-ndt-jwt-secret-key-2026-very-long-and-secure-string`, expiration 86400000 ms (24h)
- Jackson: `yyyy-MM-dd HH:mm:ss`, Asia/Shanghai timezone, non-null serialization
