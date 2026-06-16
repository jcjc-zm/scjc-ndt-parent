# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

### Backend (Spring Boot)

```bash
# Build
mvn clean compile          # compile only
mvn clean package          # compile + package (skips tests by default in this config)

# Run
mvn spring-boot:run        # development server on port 8088
java -jar target/scjc-ndt-parent-1.0-SNAPSHOT.jar

# Tests
mvn test                          # run all tests
mvn test -Dtest=UserServiceTest   # run single test class
```

### Frontend (Vue 3 + Vite)

```bash
cd frontend
npm install               # install dependencies
npm run dev               # dev server on port 5173, proxies /api → localhost:8088
npm run build             # production build
```

Full-stack dev: start backend (`mvn spring-boot:run`) and frontend (`npm run dev`) concurrently, then open `http://localhost:5173`.

## Architecture

**SCJC NDT 无损检测管理系统** — Spring Boot 3.2.6 + Vue 3 monorepo with stateless JWT authentication.

### Tech stack
**Backend:** Java 17 · Spring Boot 3.2.6 · Spring Security · MyBatis-Plus 3.5.7 · MySQL · JWT (jjwt 0.12.6) · Lombok
**Frontend:** Vue 3.5 · Vite 8 · Element Plus 2.14 · Pinia 3 · Vue Router 4 · ECharts 6 · Axios · VXE Table

### Backend layer structure (standard MVC)
```
controller/  →  service/  →  mapper/ (MyBatis-Plus BaseMapper)
                          →  entity/ (JPA-annotated POJOs with @TableName)
common/       —  R<T> (unified response), JwtUtils, GlobalExceptionHandler, BusinessException
config/       —  SecurityConfig, JwtAuthenticationFilter, CorsConfig, MyBatisPlusConfig
dto/          —  request/response POJOs with jakarta.validation
```

### Business modules (8 domain areas)

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
| Image | `ImageController` | `ImageService` | `SystemImageMapper` | Image library (films/diagrams) |

### Key design decisions

- **Unified response wrapper**: All controllers return `R<T>` (fields: `code`, `message`, `data`). Success = 200; `BusinessException` thrown in services is caught by `GlobalExceptionHandler` and converted to `R.error(code, message)`. Validation errors return code 400. Unexpected exceptions return code 500.
- **Stateless JWT auth**: `SecurityConfig` disables CSRF, sets `SessionCreationPolicy.STATELESS`. `JwtAuthenticationFilter` (a `OncePerRequestFilter`) skips `/api/auth/**` and OPTIONS; for all other requests it extracts `Authorization: Bearer <token>`, validates via `JwtUtils`, sets `request.setAttribute("userId"/"username")`, and builds a Spring Security `UsernamePasswordAuthenticationToken` into `SecurityContextHolder`.
- **Public endpoints**: `POST /api/auth/login`, all `/api/auth/**`, `/api/public/**`, and OPTIONS are unauthenticated. Everything else requires a valid JWT.
- **Password encoding**: BCrypt via `PasswordEncoder` bean. Seed accounts (admin) in `doc/sql/init.sql` use `admin123`.
- **MyBatis-Plus conventions**: `BaseMapper<T>` + `IService<T>`/`ServiceImpl<M, T>` for CRUD. Logical delete on `deleted` column (0/1). Auto-fill on `createTime`/`updateTime` via `MetaObjectHandler`. Pagination via `PaginationInnerInterceptor(DbType.MYSQL)`. Underscore-to-camelCase mapping enabled. SQL logging to stdout.
- **Entity → DTO conversion**: `UserInfo` is the safe-to-expose user representation (no password). Conversion happens in service layer via `toUserInfo()`.
- **CORS**: `CorsConfig` allows all origins (`*`) with credentials, all methods (GET/POST/PUT/DELETE/OPTIONS), and all headers.
- **User identity in controllers**: Retrieve via `request.getAttribute("userId")` (Long) and `request.getAttribute("username")` (String).

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
- **Auth guard**: `router.beforeEach` checks `localStorage.getItem('token')`, redirects to `/login?redirect=...` if missing on protected routes.
- **State**: Pinia stores. `user.js` holds token + userInfo in localStorage.
- **UI library**: Element Plus (primary). VXE Table for complex tables (inspection data). ECharts for dashboard charts. `vue-draggable-plus` for template layout drag & drop.

### Database
- Database: `scjc_ndt` (MySQL, utf8mb4)
- Initial schema & seed data: `doc/sql/init.sql`
- Seed account: `admin` / `admin123` (SYSTEM_ADMIN)
- 10 tables: `sys_dept`, `sys_role`, `sys_user`, `user_role_rel`, `user_project_rel`, `sys_project`, `inspection_record`, `system_image`, `report_template`, `report_record`, `signature_record`

### Default config (application.yml)
- Port: `8088`
- MySQL: `jdbc:mysql://localhost:3306/scjc_ndt`, user `root`, password `123456`
- JWT: `scjc-ndt-jwt-secret-key-2026-very-long-and-secure-string`, expiration 86400000 ms (24h)
- Jackson: `yyyy-MM-dd HH:mm:ss`, Asia/Shanghai timezone, non-null serialization
