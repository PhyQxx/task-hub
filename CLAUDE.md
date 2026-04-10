# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目简介

任务舱（Task Hub）— 团队任务甘特图管理系统。前后端分离 monorepo，支持甘特图、看板、泳道、里程碑、成员负载、工作日志六大视图。

## 常用命令

### 前端（`frontend/`）

```bash
npm install        # 安装依赖
npm run dev        # Vite 开发服务器，端口 5173，/api 代理到 localhost:8096
npm run build      # 生产构建，输出到 dist/
```

无 lint / typecheck / test 脚本，无 ESLint / Prettier 配置。

### 后端（`backend/`）

```bash
./mvnw spring-boot:run              # 本地启动（需 MySQL + Redis），端口 8096
./mvnw clean package -DskipTests    # 打包 JAR（无测试可跳过）
./mvnw test                         # 运行测试（JUnit 5，但当前无测试文件）
```

- Java 17，Spring Boot 3.2.5，MyBatis-Plus 3.5.6
- 数据库连接配置在 `application.yml`，默认连 `mysql.pnkx.top:13306/task_hub`

### 部署

```bash
cd deploy && docker-compose up -d        # 生产部署（后端 8096 + Nginx 8097）
cd backend && docker-compose up -d       # 开发环境（含 MySQL + Redis 容器）
```

生产 Dockerfile 直接 COPY 预编译 JAR，不在容器内构建。

## 架构概览

### 前端

- **框架**：Vue 3.5 + TypeScript + Vite 8 + Pinia 3 + Element Plus
- **无 vue-router**：通过 App.vue 中的 `activeTab` ref + `v-show` 切换视图
- **甘特图**：dhtmlx-gantt v9（MIT），CSS 在 `public/assets/dhtmlxgantt.css`
- **API 客户端**：`src/api/client.ts` 封装 axios，JWT token 拦截器自动注入
- **Pinia stores**（`src/stores/`）：auth、project、task、gantt、member、workLog
- **设计系统**：`src/styles/global.css`，飞书风格主题
- **路径别名**：`@` → `src/`
- **构建 base**：`''`（相对路径），部署在 Nginx 子路径

关键文件：
- `App.vue`（527行）：应用外壳 + 侧边栏 + 标签导航 + 创建任务/项目弹窗
- `views/GanttView.vue`：甘特图，任务 CRUD / 拖拽排程 / 依赖连线 / 时间刻度切换
- `views/KanbanView.vue`：看板四列，支持右键菜单和批量操作
- `api/index.ts`：所有 API 端点定义（project / task / gantt / member / workLog / milestone / schedule）

### 后端

- **包结构**：`com.taskhub` — controller / service / mapper / entity / dto / vo / config / ws / util
- **认证**：Spring Security + JWT（`JwtAuthenticationFilter`），ADMIN 角色才能执行删除/排程等操作
- **ORM**：MyBatis-Plus，下划线自动转驼峰，逻辑删除字段 `deleted`，ID 自增
- **ID 生成**：Redis `INCR TT:{YYYYMMDD}:seq` 生成业务 ID
- **实时推送**：WebSocket STOMP，endpoint `/ws`，按项目广播到 `/topic/project/{projectId}`
- **无 mapper XML**：所有查询用 `@Select` 注解或 MyBatis-Plus 内置方法
- **统一响应**：`ApiResponse<T>` 包装，code=0 表示成功
- **Lombok**：所有 entity / DTO / VO 使用 `@Data`

### 数据库（MySQL 8.x，库名 `task_hub`）

8 张表：`projects`、`project_members`、`tasks`、`task_dependencies`、`members`、`milestones`、`task_work_logs`、`task_history`。初始化脚本：`backend/src/main/resources/schema.sql`。

### Nginx 反向代理

- `/api/` → 后端 8096
- `/ws/` → WebSocket 8096
- `/` → SPA 静态资源（try_files fallback）

## gstack

使用 `/browse` skill 进行所有网页浏览操作，不使用 `mcp__claude-in-chrome__*` 工具。

可用的 gstack skills：`/browse`、`/qa`、`/qa-only`、`/design-review`、`/review`、`/ship`、`/investigate`、`/benchmark`、`/canary`

## 注意事项

- 使用中文注释和中文 UI 文案
- TypeScript strict mode 关闭
- 前端无路由，所有视图在 App.vue 中通过 v-show 切换
- `vite.config.ts` 中 `base: ''`，构建产物使用相对路径
- 甘特图空白数据条 bug：`TaskCreateDTO.planStartDate` 到 `startDate` 的字段映射丢失
- WebSocket 广播方法已定义但尚未从 service 层调用
- `SmartScheduleModal` 使用硬编码 mock 数据
