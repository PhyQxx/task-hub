# 任务舱 — Team Task Tracker

> 轻量级团队任务甘特图管理系统，支持甘特图、看板、泳道视图、工作日志四大视图。

---

## 🌐 访问地址

| 服务 | 地址 |
|------|------|
| **前端（原型）** | http://192.168.31.104:8890/task-hub/ |
| **后端 API** | http://192.168.31.104:8096 |
| **Swagger UI** | http://192.168.31.104:8096/swagger-ui.html |

---

## 📁 项目结构

```
任务舱/
├── backend/                    # Spring Boot 3.x 后端
│   ├── src/main/java/com/taskhub/
│   │   ├── config/            # 安全配置、CORS、WebSocket
│   │   ├── controller/        # REST API 控制器
│   │   ├── dto/               # 数据传对象
│   │   ├── entity/            # MyBatis-Plus 实体类
│   │   ├── mapper/            # MyBatis Mapper 接口
│   │   ├── service/           # 业务逻辑层
│   │   ├── vo/                # 视图对象
│   │   ├── ws/                # WebSocket 实时推送
│   │   └── TaskHubApplication.java
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/                   # Vue3 前端（Vite）
│   ├── src/
│   │   ├── views/
│   │   │   ├── GanttView.vue   # 甘特图视图
│   │   │   ├── KanbanView.vue  # 看板视图
│   │   │   ├── SwimLaneView.vue # 泳道视图
│   │   │   └── WorkLogView.vue  # 工作日志视图
│   │   ├── components/
│   │   └── App.vue
│   ├── package.json
│   └── vite.config.js
│
├── reference/                  # 参考资料
│   ├── taskGantt.vue          # 甘特图组件参考
│   └── dhtmlxgantt-snippets.md
│
├── deploy/                     # 部署配置
│   ├── docker-compose.yml     # 容器编排
│   ├── nginx.conf             # Nginx 反向代理配置
│   └── Dockerfile
│
├── task-tracker-prd.md        # 产品需求文档
├── task-hub-tech-design.md    # 技术设计文档
└── task-tracker-ui-design.md  # UI 设计文档
```

---

## 🏗 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | Vue3 + Element Plus + Pinia | 团队技术栈成熟 |
| 前端甘特图 | dhtmlxGantt（MIT） | 开源甘特图组件 |
| 后端框架 | Spring Boot 3.x | Java 17+ |
| ORM | MyBatis-Plus | CRUD 效率高 |
| 数据库 | MySQL 8.x（端口 3307） | 业务数据持久化 |
| 缓存 | Redis 7.x（端口 6380） | 缓存 + Session |
| 实时推送 | WebSocket（STOMP） | 任务变更广播 |
| 部署 | Docker + Nginx | 容器化部署 |

---

## 🗄 数据库表

| 表名 | 说明 |
|------|------|
| `projects` | 项目信息 |
| `project_members` | 项目成员关联 |
| `tasks` | 任务详情 |
| `task_dependencies` | 任务依赖关系 |
| `milestones` | 里程碑 |
| `task_work_logs` | 工作日志 |
| `task_history` | 任务变更历史 |

---

## 📦 部署架构

```
浏览器（Vue3 SPA）
    │
    │ HTTP / WebSocket
    ▼
Nginx（静态资源 + 反向代理）
    │
    ├── /api/  →  Spring Boot :8096
    └── /ws/  →  WebSocket    :8096
                    │
              ┌─────┴─────┐
              ▼           ▼
          MySQL        Redis
         :3307         :6380
```

### Docker 部署

```bash
cd 任务舱/deploy
docker-compose up -d
```

### 原型部署（Python HTTP 服务器）

```bash
# 前端部署目录
/root/.openclaw/workspace-des/prototype/

# 原型服务（已开放）
http://192.168.31.104:8890
```

---

## 🔌 API 概览

### 项目接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/projects` | 获取项目列表 |
| POST | `/api/projects` | 创建项目 |
| GET | `/api/projects/{id}` | 获取项目详情 |
| PUT | `/api/projects/{id}` | 更新项目 |
| DELETE | `/api/projects/{id}` | 删除项目 |

### 任务接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/projects/{id}/tasks` | 获取项目任务列表 |
| POST | `/api/projects/{id}/tasks` | 创建任务 |
| PUT | `/api/tasks/{id}` | 更新任务 |
| DELETE | `/api/tasks/{id}` | 删除任务 |

### 甘特图接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/projects/{id}/gantt` | 获取甘特图数据（含依赖关系） |

---

## 📋 功能清单

### ✅ 已完成

- [x] 项目管理（CRUD）
- [x] 任务管理（CRUD，含父子任务）
- [x] 甘特图视图（dhtmlxGantt）
- [x] 看板视图（Kanban）
- [x] 泳道视图（SwimLane）
- [x] 工作日志（WorkLog）
- [x] 任务依赖关系（前驱/后继）
- [x] WebSocket 实时推送
- [x] Redis 缓存
- [x] JWT 认证
- [x] Docker 部署

### 🐞 已知 Bug

- [ ] **甘特图空白数据条**：`start_date` 未能正确持久化到数据库
  - 根因：`TaskCreateDto.planStartDate → startDate` 映射丢失
  - 状态：待 Dev 修复

---

## 👥 团队角色

| 角色 | Agent ID | 职责 |
|------|----------|------|
| PM | pm | 需求分析、PRD 输出 |
| PD | pd | 项目总负责、技术架构 |
| Des | des | UI/UX 设计 |
| Dev | dev | 后端开发 |
| QA | qa | 测试验证 |
| Ops | ops | 运维部署 |

---

## 📄 相关文档

- [PRD](./task-tracker-prd.md) — 产品需求文档
- [技术设计](./task-hub-tech-design.md) — 系统架构与数据库设计
- [UI 设计](./task-tracker-ui-design.md) — 界面设计规范

---

> 最后更新：2026-04-08 by PM Agent
