# AGENTS.md

## 项目概况

任务舱（Task Hub）— 团队任务甘特图管理系统。前后端分离，monorepo 结构。

## 关键命令

### 前端（`frontend/`）

```bash
npm install          # 安装依赖
npm run dev          # Vite 开发服务器，端口 5173，自动代理 /api → localhost:8096
npm run build        # 生产构建，输出到 dist/
```

无 lint/typecheck/test 脚本。无 ESLint/Prettier 配置。

### 后端（`backend/`）

```bash
./mvnw spring-boot:run          # 本地启动（需 MySQL + Redis）
./mvnw clean package -DskipTests # 打包 JAR
./mvnw test                      # 运行测试（JUnit 5）
```

- Java 17（amazoncorretto:17-alpine）
- Spring Boot 3.2.5
- 本地默认端口 8080；Docker 部署端口 8096

## 架构要点

- **后端包结构**：`com.taskhub` — controller / service / mapper / entity / dto / vo / config / ws / util
- **ORM**：MyBatis-Plus，下划线自动转驼峰，逻辑删除字段 `deleted`，ID 自增
- **认证**：Spring Security + JWT（`JwtAuthenticationFilter`），JWT secret 在 `application.yml` 中
- **实时推送**：WebSocket（`/ws/` 路径），`GanttWebSocketHandler`
- **前端状态管理**：Pinia（`stores/auth.ts`）
- **甘特图组件**：dhtmlx-gantt v9（MIT 版本），参考代码在 `reference/`
- **API 客户端**：axios，封装在 `src/api/client.ts`，baseURL 通过 Vite 代理

## 数据库

- MySQL 8.x，库名 `task_hub`
- 初始化脚本：`backend/src/main/resources/schema.sql`
- 非标准端口：本地 3306，部署环境 3307
- Redis：本地 6379，部署环境 6380

## 部署

```bash
cd deploy && docker-compose up -d    # 生产部署（后端 8096 + Nginx 8097）
cd backend && docker-compose up -d   # 开发环境（含 MySQL + Redis 容器）
```

- 生产 Dockerfile 直接 COPY 预编译 JAR，不在容器内构建
- Nginx 反向代理：`/api/` → 后端，`/ws/` → WebSocket，SPA fallback `try_files`

## 已知问题

- 甘特图空白数据条：`TaskCreateDTO.planStartDate` 到 `startDate` 的字段映射丢失，待修复

## 注意事项
- 使用中文注释
- 前端 `vite.config.ts` 中 `base: ''`（非默认 `/`），构建后资源路径为相对路径
- 前端无测试框架，无代码检查工具
- 后端使用 Lombok，实体类通过注解生成 getter/setter
- MyBatis-Plus mapper XML 在 `resources/mapper/` 下（如果有自定义 SQL）
- 系统角色：admin（管理员）/ user（普通用户）
- 项目角色：owner（所有者）/ member（成员）/ viewer（查看者）
