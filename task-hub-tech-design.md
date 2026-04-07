# 任务舱 — 技术设计文档

> **项目名：** 任务舱（Team Task Tracker）
> **版本：** v1.0
> **技术负责人：** PD（项目总监）
> **日期：** 2026-04-07
> **参考PRD：** `/root/.openclaw/workspace-pd/任务舱/task-tracker-prd.md`（1892行）

---

## 一、技术栈选型

| 层级 | 技术选型 | 选型理由 |
|------|----------|----------|
| **前端框架** | Vue3 + Element Plus + Pinia + dayjs | 团队技术栈成熟，与控制台 v1.5 一致；Element Plus 组件丰富，Pinia 状态管理轻量，dayjs 日期处理无重 |
| **后端框架** | Spring Boot 3.x + MyBatis-Plus + MySQL + Redis | 团队熟悉，与标帆 SOP 后端技术栈一致；Spring Boot 3.x 性能最优 |
| **甘特图** | dhtmlxGantt（MIT 开源） | 依赖箭头/泳道/虚拟滚动开箱即用，Vue3 官方集成，学习成本低 |
| **实时协作** | WebSocket（Spring WebSocket） | 甘特图多人同时编辑需实时同步，WebSocket 是最低延迟方案 |
| **任务ID生成** | Redis `INCR TT:{YYYYMMDD}:seq` | 高并发唯一 ID，支持 Redis 集群，无重复风险 |
| **泳道视图** | 自研 CSS Grid + 虚拟滚动 | 深度定制，泳道分组/折叠/拖拽需自研逻辑 |
| **拖拽** | HTML5 Drag & Drop API + 坐标计算 | 原生 API，无需引入 draggable 库，精度可控 |
| **部署** | Docker（同 biaofan-backend） | 复用现有容器化基础设施，运维统一 |
| **缓存** | Redis（Cache-Aside 策略） | 甘特图数据/成员负载缓存，减少 DB 查询压力 |

---

## 二、系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    浏览器（Vue3 SPA）                   │
│         甘特图 / 任务列表 / 成员视图 / 里程碑            │
└────────────────────────────┬────────────────────────────┘
                             │ HTTPS / WSS
                             ▼
┌─────────────────────────────────────────────────────────┐
│              Nginx（反向代理 + 静态资源）                 │
│        监听 80/443，反向代理到后端 + 前端静态资源          │
└────────────────────────────┬────────────────────────────┘
                             │ HTTP / WebSocket
                             ▼
┌─────────────────────────────────────────────────────────┐
│            Spring Boot 3.x（API Server）                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │ task-api │  │task-core │  │task-sched│  │ task-ws │ │
│  │ (REST)   │  │(甘特图)  │  │(智能排程)│  │(实时推送)│ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
└────────────────────────────┬────────────────────────────┘
                             │
              ┌──────────────┴──────────────┐
              ▼                              ▼
┌─────────────────────┐          ┌─────────────────────────┐
│        MySQL 8.x     │          │        Redis 7.x         │
│  业务数据持久化       │          │ 缓存 + 任务ID生成 + Session│
│  （projects/tasks/   │          │                         │
│   members/logs）    │          │                         │
└─────────────────────┘          └─────────────────────────┘
```

### 模块说明

| 模块 | 职责 | 技术要点 |
|------|------|---------|
| `task-api` | REST API 层，接收请求、参数校验、权限判断、响应封装 | Spring MVC + `@RestController`，统一响应结构 `{code, data, message}` |
| `task-core` | 甘特图核心计算：依赖拓扑排序、负载计算、时长推算 | Kahn 算法拓扑排序、甘特图配置、数据转换适配层 |
| `task-schedule` | 智能排程：负载计算、推荐打分、环路检测、重排策略 | 得分公式 + Redis 缓存 + Kahn 环路检测 |
| `task-ws` | WebSocket 实时推送：任务变更广播、成员在线状态 | STOMP 协议、Session 管理、广播推送 |
| `task-schedule` | 定时任务：阻塞超时检测（每60分钟）、里程碑预警 | `@Scheduled` + 飞书Webhook通知 |

---

## 三、数据库设计（DDL）

> MySQL 8.x，字符集 `utf8mb4`，排序规则 `utf8mb4_unicode_ci`

### 3.1 项目表 `projects`

```sql
CREATE TABLE projects (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      VARCHAR(64) NOT NULL UNIQUE COMMENT '项目ID，格式PRJ-XXX',
    name            VARCHAR(128) NOT NULL COMMENT '项目名称',
    description     TEXT COMMENT '项目描述',
    owner_id        VARCHAR(64) NOT NULL COMMENT '项目负责人PD的member_id',
    start_date      DATE COMMENT '项目开始日期',
    end_date        DATE COMMENT '项目截止日期',
    status          VARCHAR(32) DEFAULT 'planning' COMMENT 'planning/active/completed/archived',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';
```

### 3.2 项目成员关联表 `project_members`

```sql
CREATE TABLE project_members (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      VARCHAR(64) NOT NULL COMMENT '项目ID',
    member_id       VARCHAR(64) NOT NULL COMMENT '成员ID',
    role            VARCHAR(32) DEFAULT 'member' COMMENT '在项目中的角色：owner/member/viewer',
    joined_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_member (project_id, member_id),
    INDEX idx_project (project_id),
    INDEX idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员关联表';
```

### 3.3 成员表 `members`

```sql
CREATE TABLE members (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       VARCHAR(64) NOT NULL UNIQUE COMMENT '成员ID',
    nickname        VARCHAR(64) NOT NULL COMMENT '成员昵称',
    role            VARCHAR(32) NOT NULL COMMENT '角色：PD/PM/Dev/Des/QA/Ops',
    skills          JSON COMMENT '技能标签数组，如["React","Python"]',
    weekly_capacity FLOAT DEFAULT 40.0 COMMENT '周标准工时（小时）',
    is_active       TINYINT DEFAULT 1 COMMENT '是否在职，1=在职，0=离职',
    avatar          VARCHAR(256) COMMENT '头像URL',
    email           VARCHAR(128) COMMENT '邮箱',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (role),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员表';
```

### 3.4 任务表 `tasks`

```sql
CREATE TABLE tasks (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         VARCHAR(64) NOT NULL UNIQUE COMMENT '任务ID，格式TT-YYYYMMDD-seq',
    title           VARCHAR(256) NOT NULL COMMENT '任务标题',
    description     TEXT COMMENT '任务描述',
    project_id      VARCHAR(64) NOT NULL COMMENT '所属项目ID',
    assignee_id     VARCHAR(64) COMMENT '负责人member_id',
    start_date      DATE COMMENT '开始日期',
    end_date        DATE COMMENT '结束日期',
    duration        INT COMMENT '持续天数',
    estimated_hours FLOAT COMMENT '预估工时（小时）',
    actual_hours    FLOAT DEFAULT 0 COMMENT '实际工时',
    progress        INT DEFAULT 0 COMMENT '完成百分比，0-100',
    status          VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/in_progress/completed/blocked',
    priority        VARCHAR(8) DEFAULT 'P2' COMMENT 'P0/P1/P2/P3',
    blocked_reason  VARCHAR(512) COMMENT '阻塞原因',
    blocked_at      DATETIME COMMENT '进入阻塞状态的时间',
    is_milestone    TINYINT DEFAULT 0 COMMENT '是否里程碑，1=是',
    milestone_date  DATE COMMENT '里程碑日期',
    tags            JSON COMMENT '标签数组',
    created_by      VARCHAR(64) NOT NULL COMMENT '创建人',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project (project_id),
    INDEX idx_assignee (assignee_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_is_milestone (is_milestone),
    INDEX idx_project_status_assignee (project_id, status, assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';
```

### 3.5 任务依赖关系表 `task_dependencies`

```sql
CREATE TABLE task_dependencies (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         VARCHAR(64) NOT NULL COMMENT '任务ID（被依赖者/后续任务）',
    depends_on      VARCHAR(64) NOT NULL COMMENT '依赖的任务ID（前置任务）',
    dependency_type VARCHAR(16) DEFAULT 'FS' COMMENT 'FS=Finish-to-Start, SS=Start-to-Start, FF=Finish-to-Finish',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dependency (task_id, depends_on),
    INDEX idx_task (task_id),
    INDEX idx_depends_on (depends_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖关系表';
```

### 3.6 每日工作日志表 `task_work_logs`

```sql
CREATE TABLE task_work_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id          VARCHAR(64) NOT NULL UNIQUE COMMENT '日志ID，格式WL-YYYYMMDD-seq',
    task_id         VARCHAR(64) NOT NULL COMMENT '关联任务ID',
    user_id         VARCHAR(64) NOT NULL COMMENT '填报人member_id',
    log_date        DATE NOT NULL COMMENT '填报日期',
    today_done      TEXT COMMENT '今日完成内容',
    tomorrow_plan   TEXT COMMENT '明日计划',
    current_status  VARCHAR(16) DEFAULT '正常' COMMENT '正常/有风险/已阻塞',
    blocked_reason  VARCHAR(512) COMMENT '阻塞原因',
    hours_spent     FLOAT DEFAULT 0 COMMENT '今日花费工时',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_date (task_id, log_date),
    INDEX idx_task (task_id),
    INDEX idx_user (user_id),
    INDEX idx_log_date (log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日工作日志表';
```

### 3.7 里程碑表 `milestones`

```sql
CREATE TABLE milestones (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    milestone_id    VARCHAR(64) NOT NULL UNIQUE COMMENT '里程碑ID，格式MS-YYYYMMDD-seq',
    project_id      VARCHAR(64) NOT NULL COMMENT '所属项目ID',
    name            VARCHAR(128) NOT NULL COMMENT '里程碑名称',
    target_date     DATE NOT NULL COMMENT '目标日期',
    description     TEXT COMMENT '里程碑说明',
    color           VARCHAR(16) DEFAULT '#FFD700' COMMENT '颜色',
    created_by      VARCHAR(64) NOT NULL COMMENT '创建人',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project (project_id),
    INDEX idx_target_date (target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='里程碑表';
```

### 3.8 操作历史表 `task_history`（v2 扩展，提前建表）

```sql
CREATE TABLE task_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         VARCHAR(64) NOT NULL COMMENT '任务ID',
    operator        VARCHAR(64) NOT NULL COMMENT '操作人',
    action          VARCHAR(32) NOT NULL COMMENT '操作类型：create/update/delete/status_change/assign/reorder',
    detail          JSON COMMENT '变更详情',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_created_at (created_at),
    INDEX idx_task_time (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务操作历史表';
```

### 3.9 索引汇总

| 索引名 | 表 | 字段 | 类型 | 用途 |
|--------|-----|------|------|------|
| `idx_project_status_assignee` | tasks | `(project_id, status, assignee_id)` | BTREE | 甘特图/列表联合筛选 |
| `idx_task_time` | task_history | `(task_id, created_at)` | BTREE | 操作历史查询 |
| `idx_project` | project_members | `project_id` | BTREE | 项目成员查询 |
| `idx_member` | project_members | `member_id` | BTREE | 成员项目查询 |
| `uk_task_date` | task_work_logs | `(task_id, log_date)` | BTREE | 唯一约束，防止重复填报 |

---

## 四、核心 API 设计

### 4.1 任务接口

#### `POST /api/tasks/reorder` — 批量重排下游依赖

> 拖拽任务时间后，自动重排所有下游依赖任务。**响应时间 < 100ms**

**请求：**
```json
{
  "taskId": "TT-20260407001",
  "startDate": "2026-04-10",
  "endDate": "2026-04-16",
  "autoReschedule": true
}
```

**响应：**
```json
{
  "code": 0,
  "data": {
    "rescheduled": [
      { "taskId": "TT-20260407002", "oldEndDate": "2026-04-12", "newStartDate": "2026-04-17" }
    ],
    "cascaded": 1
  }
}
```

**错误响应（环路检测）：**
```json
{ "code": 40021, "message": "重排后产生循环依赖：TT-001 → TT-002 → TT-003 → TT-001" }
```

---

#### `POST /api/tasks/batch-schedule` — 批量智能分配

> 选中多个任务，一键按负载均分推荐负责人

**请求：**
```json
{
  "taskIds": ["TT-20260407001", "TT-20260407002", "TT-20260407003"],
  "projectId": "PRJ-2026040001",
  "mode": "balance_load"
}
```

**响应：**
```json
{
  "code": 0,
  "data": {
    "results": [
      { "taskId": "TT-20260407001", "assigneeId": "dev-dq", "nickname": "Dev-DQ", "score": 92, "reason": "当前负载最低（40%）" },
      { "taskId": "TT-20260407002", "assigneeId": "qa",     "nickname": "QA",     "score": 88, "reason": "负载41%，技能标签匹配" }
    ]
  }
}
```

---

#### `GET /api/members/{id}/load-trend` — 成员负载趋势

> 悬停推荐成员时显示本周+下周负载曲线

**响应：**
```json
{
  "code": 0,
  "data": {
    "memberId": "dev",
    "weeks": [
      { "week": "2026-W14", "weekLabel": "4/7~4/13", "loadPercent": 72, "taskCount": 5, "loadLevel": "normal" },
      { "week": "2026-W15", "weekLabel": "4/14~4/20", "loadPercent": 88, "taskCount": 7, "loadLevel": "high" }
    ]
  }
}
```

---

#### `GET /api/tasks/{id}/history` — 任务操作历史

> 任务详情 → 操作日志 Tab

**响应：**
```json
{
  "code": 0,
  "data": {
    "history": [
      { "action": "create",        "operator": "PD",    "detail": {},                    "createdAt": "2026-04-07T10:00:00" },
      { "action": "assign",       "operator": "PD",    "detail": {"assignee": "Dev"},    "createdAt": "2026-04-07T10:05:00" },
      { "action": "status_change","operator": "Dev",    "detail": {"from": "pending", "to": "in_progress"}, "createdAt": "2026-04-07T14:00:00" }
    ]
  }
}
```

---

### 4.2 WebSocket 协议

**连接路径：** `ws://host/api/ws?token={jwt}`

**心跳：** 客户端每 30s 发送 `ping`，服务端响应 `pong`

**服务端推送事件：**

| 事件类型 | payload | 触发时机 |
|---------|---------|---------|
| `TASK_UPDATED` | `{taskId, fields, actor, ts}` | 任务任一字段变更 |
| `TASK_CREATED` | `{task, actor, ts}` | 新建任务 |
| `TASK_DELETED` | `{taskId, actor, ts}` | 删除任务 |
| `TASK_ASSIGNED` | `{taskId, assigneeId, actor, ts}` | 任务指派变更 |
| `MILESTONE_UPDATED` | `{milestone, actor, ts}` | 里程碑变更 |
| `MEMBER_LOAD_ALERT` | `{memberId, loadPct, ts}` | 负载超载预警 |

**客户端请求示例：**
```javascript
// 连接
const ws = new WebSocket('ws://api.example.com/api/ws?token=xxx')

// 订阅项目甘特图变更
ws.send(JSON.stringify({ type: 'SUBSCRIBE_PROJECT', projectId: 'PRJ-001' }))

// 接收
ws.onmessage = (e) => {
  const { type, payload } = JSON.parse(e.data)
  if (type === 'TASK_UPDATED') refreshGanttTask(payload.taskId)
}
```

---

## 五、甘特图实现方案

### 5.1 技术选型：dhtmlxGantt

**结论：** 选用 dhtmlxGantt（MIT 开源，免费版本功能完整），原因如下：

| 对比项 | 自研 SVG | dhtmlxGantt（已选）|
|--------|---------|-------------------|
| 依赖箭头 | ✅ 完整支持 FS/SS/FF | ✅ 支持 FS/FF，需二次开发 SS |
| 泳道视图 | ✅ 原生支持 | ✅ 通过 `layer` 配置实现 |
| 虚拟滚动 | ✅ 按需渲染 | ✅ 内置虚拟滚动（大数据量优化）|
| 拖拽交互 | ✅ 完全可控 | ✅ 支持但需二次开发定制 |
| 与 Vue3 集成 | 手动绑定 | ✅ `dhtmlx-gantt` Vue3 封装组件 |
| 学习成本 | 高（从0实现）| 中（官方文档完善）|
| 包体积 | 0 | ~2MB（可 tree-shaking）|

**dhtmlxGantt 免费版能力：**
- 水平/垂直任务条拖拽
- 依赖箭头（FS/完成-完成 FF）
- 关键路径高亮
- 虚拟滚动（>1000任务无压力）
- REST API 数据加载
- 自定义 CSS 样式覆盖

**需要二次开发的功能：**
- SS（开始-开始）依赖类型
- 泳道视图（Swimlane layer）
- 拖拽指派负责人（需要自定义事件）

### 5.2 dhtmlxGantt Vue3 集成方案

**安装：**
```bash
npm install dhtmlx-gantt
```

**基础配置：**
```javascript
import gantt from 'dhtmlx-gantt'
import 'dhtmlx-gantt/codebase/dhtmlxgantt.css'

// 注册 Vue3 组件
export default {
  name: 'GanttChart',
  mounted() {
    gantt.config.date_format = "%Y-%m-%d"
    gantt.init(this.$refs.ganttContainer)
    gantt.load('/api/gantt/project/{projectId}')  // REST 数据加载
  },
  beforeDestroy() {
    gantt.clear()
  }
}
```

**泳道视图实现（dhtmlxGantt 7.0+）：**
```javascript
gantt.config.show_task_cells = true
gantt.config.fit_tasks = true
// 使用 grid 层自定义泳道列
gantt.config.columns = [
  { name: "text", label: "任务名称", tree: true, width: 200 },
  { name: "start_date", label: "开始", align: "center", width: 80 },
  { name: "duration", label: "工期", align: "center", width: 60 },
  { name: "assignee", label: "负责人", width: 100, template: (task) => task.assignee_name }
]
```

**拖拽指派负责人（自定义）：**
```javascript
gantt.attachEvent("onBeforeTaskDrag", function(id, mode, e) {
  if (mode === "move") {
    // 拖拽任务条时触发负责人切换
    return true
  }
  return true
})

gantt.attachEvent("onAfterTaskDrag", function(id, mode, e) {
  const task = gantt.getTask(id)
  // 调用 API 更新负责人
  PUT /api/tasks/{id}/assignee { assigneeId: task.assignee_id }
})
```

**性能优化：**
```javascript
gantt.config.smart_rendering = true   // 虚拟滚动
gantt.config.row_height = 40       // 固定行高
gantt.config.bar_height = 28      // 任务条高度
```

### 5.2 SVG 甘特图结构

```xml
<svg id="gantt-svg" width="100%" height="100%">
  <!-- ① 时间轴头部（sticky top） -->
  <g id="timeline-header">
    <rect x="0" y="0" width="120" height="40" />  <!-- 成员列头 -->
    <!-- 日期格子 -->
    <rect v-for="day in days" :x="120 + day.index * CELL_WIDTH" y="0"
          :width="CELL_WIDTH" height="40" />
    <text v-for="day in days" :x="120 + day.index * CELL_WIDTH + CELL_WIDTH/2" y="25"
          text-anchor="middle">{{ day.label }}</text>
  </g>

  <!-- ② 泳道行（成员行） -->
  <g v-for="member in members" :id="'lane-' + member.id"
     :transform="'translate(0, ' + (40 + member.index * ROW_HEIGHT) + ')'">
    <!-- 成员名（sticky left） -->
    <rect x="0" y="0" width="120" :height="ROW_HEIGHT" class="lane-label" />
    <text x="10" y="24">{{ member.nickname }}</text>
    <!-- 任务条 -->
    <g v-for="task in member.tasks"
       :transform="'translate(' + task.left + ', 8)'"
       class="task-bar"
       :data-task-id="task.taskId">
      <rect :width="task.width" height="32" rx="4" />
      <!-- 进度填充 -->
      <rect :width="task.width * task.progress / 100" height="32" rx="4" class="progress-fill" />
      <text x="8" y="20">{{ task.title }}</text>
    </g>
  </g>

  <!-- ③ 今日线 -->
  <line id="today-line" x1="TODAY_X" y1="0" x2="TODAY_X" :y2="TOTAL_HEIGHT"
        stroke="#FF4757" stroke-width="2" />

  <!-- ④ 依赖箭头（叠加层，bezier曲线） -->
  <g id="dependency-arrows">
    <path v-for="dep in dependencies"
          :d="getBezierPath(dep)"
          stroke="#999" stroke-width="1.5" fill="none"
          marker-end="url(#arrowhead)" />
  </g>

  <!-- ⑤ 里程碑（菱形） -->
  <g v-for="ms in milestones" :transform="'translate(' + ms.x + ', ' + ms.y + ')'">
    <polygon points="0,-12 12,0 0,12 -12,0" fill="#FFD700" />
    <text y="28" text-anchor="middle" font-size="11">{{ ms.name }}</text>
  </g>

  <!-- SVG Defs -->
  <defs>
    <marker id="arrowhead" markerWidth="8" markerHeight="8" refX="8" refY="4" orient="auto">
      <polygon points="0,0 8,4 0,8" fill="#999" />
    </marker>
  </defs>
</svg>
```

### 5.3 拖拽实现

**拖拽类型：**

| 拖拽类型 | 触发区域 | 鼠标样式 | 效果 |
|---------|---------|---------|------|
| 整体拖拽 | 任务条主体 | `move` | 同步移动 startDate + endDate |
| 调整开始 | 左边缘 8px | `ew-resize` | 仅改变 startDate |
| 调整结束 | 右边缘 8px | `ew-resize` | 仅改变 endDate |
| 创建依赖 | 任务右锚点（圆圈） | `crosshair` | 拖出虚线，连接建立依赖 |
| 拖拽指派 | 任务条拖入成员泳道 | `move` | 弹出确认更换负责人 |

**拖拽流程：**

```
mousedown(任务条)
  ↓
dragstart → 记录原始 startDate/endDate → 添加拖拽样式
  ↓
mousemove → 计算相对位移 → 更新任务条 visual position → 实时显示日期预览浮层
  ↓
mouseup → 计算新日期 → 100ms 防抖 → PUT /api/tasks/:id/drag
  ↓
成功 → 更新本地状态
失败 → 回滚到原始位置 → 提示错误
```

**日期预览浮层（Drag Preview）：**
```
┌──────────────────────┐
│ 📅 04-10 → 04-16（7天）│  ← 拖拽时跟随鼠标显示
│ 负责人：Dev           │
└──────────────────────┘
```

### 5.4 依赖箭头（Bezier 曲线）

```javascript
// 依赖箭头 SVG path 计算
function getBezierPath(dep) {
  const startX = dep.fromTask.left + dep.fromTask.width  // 前置任务右端
  const startY = dep.fromTask.y + ROW_HEIGHT / 2
  const endX   = dep.toTask.left                          // 后置任务左端
  const endY   = dep.toTask.y + ROW_HEIGHT / 2
  const midX   = (startX + endX) / 2

  // 水平依赖：直接贝塞尔曲线
  // 跨行依赖：先水平再垂直再水平
  return `M ${startX} ${startY} C ${midX} ${startY}, ${midX} ${endY}, ${endX} ${endY}`
}
```

### 5.5 虚拟滚动

**策略：** 任务数 > 50 时启用，仅渲染可视区域 ±5 行

```javascript
// 可视区域计算
const visibleStart = Math.floor(scrollLeft / CELL_WIDTH) - 5
const visibleEnd   = visibleStart + Math.ceil(viewportWidth / CELL_WIDTH) + 5

// 仅渲染可见任务
const visibleTasks = allTasks.filter(t =>
  t.startDate <= visibleEndDate && t.endDate >= visibleStartDate
)
```

### 5.6 性能指标

| 指标 | 目标 | 实现方案 |
|------|------|---------|
| 初始加载 | < 200ms | Redis 缓存甘特图数据 + 联合索引 |
| 拖拽响应 | < 16ms（60fps）| 前端乐观更新，后端异步保存 |
| 依赖箭头渲染 | < 50ms（100条）| SVG path 批量生成，减少重绘 |
| 批量重排 API | < 100ms | Kahn 拓扑排序 + 批量 UPDATE |

---

## 六、智能排程实现

### 6.1 推荐得分公式

```
score = 0.55 × loadFactor + 0.30 × skillFactor + 0.15 × contextFactor
```

| 因子 | 计算方式 | 说明 |
|------|---------|------|
| `loadFactor` | `(100 - currentLoadPct) / 100` | 负载越低得分越高 |
| `skillFactor` | `100 - skillMismatchPenalty` | 技能标签不匹配扣25分/项 |
| `contextFactor` | `min(memberTaskCountInProject × 20, 100) / 100` | 同项目已有任务越多，上下文连贯性越高 |

**阈值：**
- `loadPct ≥ 100%` → 标记红色「超载」，排除推荐（除非无其他人选）
- `loadPct ≥ 80%` → 标记黄色「高负载」，推荐时显示警告

### 6.2 环路检测算法（Kahn 算法）

```java
public boolean hasCycle(List<TaskDependency> deps) {
    // 构建入度表
    Map<String, Integer> inDegree = new HashMap<>();
    Map<String, List<String>> adj = new HashMap<>();

    for (TaskDependency dep : deps) {
        adj.computeIfAbsent(dep.getDependsOn(), k -> new ArrayList<>()).add(dep.getTaskId());
        inDegree.merge(dep.getTaskId(), 1, Integer::sum);
    }

    // BFS 拓扑排序
    Queue<String> queue = new LinkedList<>();
    inDegree.forEach((k, v) -> { if (v == 0) queue.offer(k); });

    int count = 0;
    while (!queue.isEmpty()) {
        String node = queue.poll();
        count++;
        for (String next : adj.getOrDefault(node, List.of())) {
            int newDeg = inDegree.merge(next, -1, Integer::sum);
            if (newDeg == 0) queue.offer(next);
        }
    }

    // 若拓扑排序后节点数 < 总节点数，说明有环路
    return count < inDegree.size();
}
```

### 6.3 批量重排下游

```java
public List<Task> cascadeReschedule(String taskId, LocalDate newEndDate) {
    // 1. 获取所有直接下游依赖（FS类型：前置结束 → 后置开始）
    List<Task> downstream = findDownstream(taskId, DependencyType.FS);

    List<Task> rescheduled = new ArrayList<>();
    LocalDate currentDate = newEndDate.plusDays(1);

    for (Task t : downstream) {
        long duration = DAYS.between(t.getStartDate(), t.getEndDate());
        t.setStartDate(currentDate);
        t.setEndDate(currentDate.plusDays(duration));
        taskMapper.updateById(t);
        rescheduled.add(t);
        // 递归重排下游
        rescheduled.addAll(cascadeReschedule(t.getTaskId(), t.getEndDate()));
    }

    return rescheduled;
}
```

### 6.4 超载预警

**触发时机：**
- 创建/指派任务时，实时检测
- 每小时 Cron 扫描所有成员负载

**飞书通知格式：**
```
⚠️ 任务阻塞超过 24 小时未解决
项目：{{projectName}}
任务：{{taskTitle}}
负责人：{{assigneeName}}
阻塞原因：{{blockedReason}}
阻塞时间：{{blockedAt}}（已 {{hours}} 小时）
[查看任务] [联系负责人]
```

---

## 七、安全设计

### 7.1 认证与授权

**JWT 认证：**
- Access Token：有效期 2h，存储在内存
- Refresh Token：有效期 7d，存储在 HttpOnly Cookie
- Token 结构：`{sub: memberId, role, projectRoles: {PRJ-001: 'owner', PRJ-002: 'member'}, exp, iat}`

**三级项目权限：**

| 权限 | Owner（PD）| Member | Viewer |
|------|:---:|:---:|:---:|
| 查看甘特图/任务列表 | ✅ | ✅ | ✅ |
| 创建/编辑任务 | ✅ | ✅（仅自己的）| ❌ |
| 拖拽调整时间 | ✅ | ✅（需填原因）| ❌ |
| 删除任务 | ✅（自己的）| ❌ | ❌ |
| 管理里程碑 | ✅ | ❌ | ❌ |
| 添加/移除成员 | ✅ | ❌ | ❌ |
| 删除项目 | ✅ | ❌ | ❌ |

### 7.2 接口权限装饰器

```java
@PreAuthorize("@projectAuth.canAccessProject(#projectId, 'member')")
@GetMapping("/projects/{projectId}/gantt")
public Result<GanttVO> getGantt(@PathVariable String projectId) { ... }

@PreAuthorize("@projectAuth.canModify(#projectId)")
@PutMapping("/projects/{projectId}/tasks/{taskId}")
public Result<Void> updateTask(...) { ... }
```

### 7.3 SQL 注入防护

- 全局：MyBatis-Plus `#{}` 参数化查询，禁止 `${}` 拼接
- 搜索：Elasticsearch 全文检索，或 MySQL `LIKE ?` 参数化
- 日志：脱敏后记录，防止敏感信息泄露

### 7.4 操作审计

- 全量记录：`task_history` 表，每次变更一条
- 变更内容：JSON diff，记录变更前后值
- 不可删除：审计日志不提供物理删除接口

---

## 八、部署方案

### 8.1 Docker 容器化

与 `biaofan-backend` 共用镜像体系，`Dockerfile` 复用：

```dockerfile
FROM openjdk:17-slim
COPY target/task-hub.jar /app/task-hub.jar
COPY application-docker.yml /app/config/application-docker.yml
WORKDIR /app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s \
  CMD pgrep -f "task-hub.jar" || exit 1
ENTRYPOINT ["java", "-jar", "task-hub.jar", "--spring.config.location=/app/config/"]
```

### 8.2 Docker Compose

```yaml
services:
  task-hub:
    image: registry.example.com/task-hub:latest
    container_name: task-hub
    ports:
      - "8015:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - MYSQL_HOST=mysql
      - REDIS_HOST=redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "pgrep", "-f", "task-hub.jar"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### 8.3 application-docker.yml 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST}:3306/task_hub?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    port: 6379
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

server:
  port: 8080

logging:
  level: INFO
  pattern: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 8.4 Nginx 配置

```nginx
server {
    listen 80;
    server_name task-hub.example.com;

    # 前端静态资源
    location / {
        root /var/www/task-hub/dist;
        try_files $uri $uri/ /index.html;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://task-hub:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 60s;
    }

    # WebSocket 代理
    location /api/ws {
        proxy_pass http://task-hub:8080/api/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }
}
```

---

## 九、任务 ID 生成方案

### Redis INCR 方案

```java
public String generateTaskId() {
    String key = "TT:" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // TT:20260407
    Long seq = redisTemplate.opsForValue().increment(key, 1);
    redisTemplate.expire(key, Duration.ofDays(7)); // 7天后自动过期
    return String.format("TT-%s-%04d",
        LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),
        seq);
}
```

**示例：** `TT-20260407-0001`

**Redis 集群支持：** 使用 `INCR` 原子操作，天然支持集群，无竞争条件。

---

## 十、v1.5 迭代关键技术点

| 功能 | 技术实现要点 | 优先级 |
|------|------------|-------|
| 拖拽指派负责人 | dragover 事件监听泳道行，drop 后弹出确认 | P0 |
| 右键快捷菜单 | 原生 `contextmenu` 事件 + 动态定位浮层 | P1 |
| 多依赖类型（SS/FF）| SVG path 分支逻辑：SS=水平虚线，FF=水平点线 | P1 |
| 虚拟滚动 | IntersectionObserver + 可视区域计算，任务数>50启用 | P1 |
| 阻塞24h自动升级 | `@Scheduled(fixedRate=3600000)` + 飞书Webhook | P0 |
| 负载趋势图 | `GET /api/members/{id}/load-trend` + ECharts迷你图 | P1 |
| 批量排程 | `POST /api/tasks/batch-schedule` + 得分公式批量计算 | P1 |
| 超载预警前置 | 任务指派/创建时实时检测，拦截并弹窗 | P0 |
| 任务评论 | 独立表 `task_comments`，WebSocket实时推送 | P1 |
| 操作历史 | `task_history` 记录 + JSON diff 差异存储 | P1 |
| 进度自动计算 | Cron 每日凌晨计算：progress = 已填报天数 / 总工期 | P1 |

---

## 十一、目录结构

```
task-hub/
├── backend/                        # Spring Boot 后端
│   └── src/main/java/com/taskhub/
│       ├── TaskHubApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   ├── WebSocketConfig.java
│       │   └── RedisConfig.java
│       ├── controller/
│       │   ├── ProjectController.java
│       │   ├── TaskController.java
│       │   ├── MemberController.java
│       │   └── MilestoneController.java
│       ├── service/
│       │   ├── TaskService.java
│       │   ├── GanttService.java       # 甘特图核心计算
│       │   ├── ScheduleService.java    # 智能排程
│       │   └── WebSocketService.java
│       ├── mapper/
│       │   ├── TaskMapper.java
│       │   ├── ProjectMapper.java
│       │   └── MemberMapper.java
│       ├── model/
│       │   ├── entity/                 # 数据库实体
│       │   ├── dto/                    # 请求/响应 DTO
│       │   └── vo/                     # 视图对象
│       ├── schedule/
│       │   ├── BlockedTaskCheckJob.java
│       │   └── MilestoneAlertJob.java
│       └── util/
│           ├── TaskIdGenerator.java     # Redis INCR 生成器
│           └── CycleDetector.java       # Kahn 环路检测
├── frontend/                      # Vue3 前端
│   └── src/
│       ├── views/
│       │   ├── GanttView.vue           # 甘特图主视图
│       │   ├── TaskListView.vue        # 任务列表
│       │   ├── MemberView.vue          # 成员视图
│       │   └── MilestoneView.vue       # 里程碑
│       ├── components/
│       │   ├── gantt/
│       │   │   ├── GanttChart.vue      # SVG 甘特图主组件
│       │   │   ├── GanttTaskBar.vue    # 任务条
│       │   │   ├── GanttDependency.vue # 依赖箭头
│       │   │   └── GanttTodayLine.vue  # 今日线
│       │   ├── TaskDetail.vue          # 任务详情侧边栏
│       │   └── ContextMenu.vue         # 右键菜单
│       ├── stores/
│       │   ├── gantt.ts                # 甘特图状态
│       │   └── member.ts                # 成员状态
│       ├── composables/
│       │   ├── useDragDrop.ts          # 拖拽逻辑
│       │   ├── useWebSocket.ts         # WebSocket 连接
│       │   └── useVirtualScroll.ts     # 虚拟滚动
│       └── styles/
│           └── gantt.scss
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── nginx.conf
├── sql/
│   └── ddl.sql                         # 完整建表语句
└── README.md
```

---

*文档版本：v1.0 | 创建日期：2026-04-07 | 作者：PD*

---

## 十二、dhtmlxGantt 参考资料

> 收到生产环境参考代码 `taskGantt.vue`（Hussar系统，2501行），已存入：
> `/root/.openclaw/workspace-pd/任务舱/reference/taskGantt.vue`
> 快速参考：`/root/.openclaw/workspace-pd/任务舱/reference/dhtmlxgantt-snippets.md`

### 参考实现亮点

| 功能 | 参考代码位置 |
|------|------------|
| `Gantt.getGanttInstance()` 初始化 | mounted() |
| `ganttInstance.destructor()` 销毁 | beforeDestroy() |
| `drag_timeline` / `undo` / `tooltip` 插件 | initGantt() |
| `onBeforeTaskDrag` / `onAfterTaskDrag` | 拖拽保存 |
| `gantt.parse({data, links})` 数据加载 | — |
| `ganttInstance.serialize()` 数据序列化 | — |
| `row_height: 40` / `bar_height: 39` | 固定行高 |
| `layout: { grid + timeline + scrollbar }` | 双栏布局 |

### 关键配置（来自生产代码）

```javascript
// 日期格式（与后端交互）
xml_date: '%Y-%m-%d %H:%i:%s'
// 步长（分钟）
time_step: 10
// 禁止默认行为
dblclick_create: false
drag_links: false
drag_progress: false
// 拖拽移动（开启）
drag_move: this.dragMove
// 固定行高
row_height: 40
bar_height: 39
```
