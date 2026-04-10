# 任务舱 — 需求文档（PRD）

> **产品名：** 任务舱（Team Task Tracker）
> **版本：** v1.0（初版）
> **定位：** 研发团队的可视化任务分配与进度跟踪工具，以甘特图为核心，提供智能排程和动态调整能力
> **需求来源：** 裴浩宇（2026-04-07 晚）
> **编写角色：** PM（产品经理）
> **日期：** 2026-04-07

---

## 一、版本概述

### 1.1 产品定位

任务舱是研发团队的任务协作平台，以**甘特图（Gantt Chart）** 为核心视图，解决"任务分配不透明、进度不可见、计划难调整"三大痛点。系统支持按成员负载自动推荐负责人，并提供里程碑和依赖箭头等高级功能，帮助团队管理者（PD）一眼掌握全局。

### 1.2 核心价值

| 价值点 | 说明 |
|--------|------|
| **可视化全局** | 甘特图一览所有任务时间轴，跨成员/跨项目一目了然 |
| **智能省心** | 根据成员当前负载自动推荐最优负责人，减少人工分配 |
| **动态调整** | 任务时间、负责人均可拖拽修改，实时同步 |
| **进度透明** | 成员每日维护工作内容，PD 无需追问即可看到真实进展 |

### 1.3 与现有系统的关系

- **独立系统**：独立部署，不依赖标帆 SOP / 控制台，可独立运行
- **数据互通预留**：未来可与控制台 Hall / Tasks 模块打通（通过统一 member_id）
- **移动端**：v1.0 专注桌面端，移动端本次不做

### 1.4 版本路线图

```
v1.0 🔄  本迭代    — 甘特图 + 任务分配 + 智能排程 + 进度跟踪 + 里程碑
     → Des: UI设计
     → Dev: 前后端实现
     → QA: 验收测试
```

---

## 二、功能详述

### 2.1 功能总览

| 模块 | 功能 | 优先级 |
|------|------|--------|
| **甘特图** | 可视化时间轴、任务条、依赖箭头、里程碑 | P0 |
| **任务管理** | 创建/编辑/删除任务、分配负责人、设置时间 | P0 |
| **智能排程** | 根据成员负载自动推荐负责人 | P0 |
| **动态调整** | 拖拽修改任务时间/负责人 | P0 |
| **进度跟踪** | 成员每日填报工作内容、任务状态维护 | P0 |
| **成员管理** | 成员列表、负载视图、工作量统计 | P1 |
| **项目视图** | 多项目隔离，不同项目成员互不可见 | P1 |

---

### 2.2 甘特图（核心视图）

#### 2.2.1 用户故事

| # | 用户故事 | 角色 | 场景 |
|---|---------|------|------|
| US-G1 | 作为 PD，我希望在甘特图上一眼看懂所有任务的时间分布，不用逐一点击详情 | PD | 每天早会前扫一眼甘特图，确认今日重点 |
| US-G2 | 作为 PD，我希望在甘特图上看到任务之间的依赖关系，用箭头连接 | PD | 功能A必须在功能B完成后才能开始，箭头清晰展示 |
| US-G3 | 作为 PD，我希望为重要节点设置里程碑，到达时间自动高亮提醒 | PD | v1.0 发布日设为里程碑，接近时甘特图顶部显示红色标记 |
| US-G4 | 作为 PD，我希望拖拽任务条直接修改开始/结束时间，不用打开编辑弹窗 | PD | 某任务延期2天，直接拖拽调整 |

#### 2.2.2 甘特图功能点

| 功能 | 描述 |
|------|------|
| 时间轴头部 | 显示日/周视图切换，时间粒度：天（默认）/ 周 |
| 任务条 | 每个任务显示为彩色条形，宽度 = 持续天数，左边起点 = 开始日期 |
| 负责人轨道 | 甘特图左侧（y轴）为成员名，每位成员一行任务条 |
| 今日线 | 红色竖线标记当前日期，方便判断任务是否延误 |
| 依赖箭头 | FS（Finish-to-Start）箭头，从前置任务右端指向后续任务左端 |
| 里程碑 | 菱形图标，标记重要节点（如"发布"），颜色可自定义 |
| 进度填充 | 任务条内用深色填充表示已完成百分比 |
| 拖拽调整 | 水平拖拽任务条改变开始/结束时间，拖拽边缘调整时长 |
| 缩放 | 支持日视图（每格=天）/ 周视图（每格=周）切换 |
| 滚动 | 甘特图可横向滚动，时间轴固定在顶部 |
| 任务悬停 | 鼠标悬停任务条显示任务摘要浮层（标题/负责人/状态/进度）|
| 任务点击 | 点击任务条打开任务详情侧边栏 |

#### 2.2.3 甘特图字段

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | string | 任务唯一ID，格式 `TT-XXX` |
| title | string | 任务标题 |
| assignee | string | 负责人 member_id |
| startDate | date | 开始日期（可不填，仅时长） |
| endDate | date | 结束日期（可不填，仅时长） |
| duration | int | 持续天数（可不填，根据开始/结束自动算）|
| progress | int | 完成百分比，0-100 |
| status | enum | pending / in_progress / completed / blocked |
| dependencies | array | 依赖任务ID数组 |
| isMilestone | bool | 是否为里程碑 |
| color | string | 任务条颜色（可选，用于区分类型）|
| projectId | string | 所属项目ID |

#### 2.2.4 甘特图交互描述

**新建任务（在甘特图上）：**
1. 双击甘特图空白区域，弹出「新建任务」弹窗
2. 填写标题、时间、负责人，保存后任务条出现在甘特图上
3. 若时间留空，任务以"无时长"方式显示在甘特图最底部一行

**拖拽任务时间：**
1. 鼠标按住任务条，整体拖拽 → 移动开始+结束日期
2. 鼠标悬停在任务条左/右边缘 → 光标变为水平双箭头 → 拖拽 → 仅改变开始/结束日期
3. 拖拽后实时保存，后端自动更新

**创建依赖：**
1. 点击任务A的右端锚点（一个小圆圈）
2. 拖出一条虚线，连接到任务B的左端锚点
3. 释放后自动创建 FS 依赖：A 完成 → B 才能开始

**删除依赖：**
1. 点击依赖箭头（箭头高亮）
2. 按 `Delete` 键，或点击出现的 × 按钮

---

### 2.3 任务管理

#### 2.3.1 用户故事

| # | 用户故事 | 角色 | 场景 |
|---|---------|------|------|
| US-T1 | 作为 PD，我希望创建任务时设置负责人，系统根据负载自动推荐最合适的人 | PD | 新功能需要分配给 Dev，看到系统推荐 Dev-DQ（当前负载 40%，最低）|
| US-T2 | 作为成员，我希望每天在任务卡片上更新工作内容和进度，不用额外汇报 | Dev/QA | 每天下班前更新「今日完成」「明日计划」「当前状态」|
| US-T3 | 作为 PD，当我不在甘特图上时，我希望有一个任务列表视图，按状态/成员筛选 | PD | 快速查看所有 blocked 任务，逐个解决 |
| US-T4 | 作为成员，我希望任务可以标记 blocked 并填写原因，让 PD 知道卡在哪里 | Dev | 等待 UI 设计稿，在任务上标记 blocked + 原因 |

#### 2.3.2 任务字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | string | 自动 | 任务唯一ID，格式 `TT-XXX`，系统自动生成 |
| title | string | 是 | 任务标题（最长100字符）|
| description | text | 否 | 任务详细描述 |
| projectId | string | 是 | 所属项目ID |
| assignee | string | 是 | 负责人 member_id |
| startDate | date | 否 | 开始日期，不填则仅记录时长 |
| endDate | date | 否 | 结束日期，不填则仅记录时长 |
| duration | int | 否 | 持续天数（优先级低于起止日期）|
| estimatedHours | float | 否 | 预估工时（小时）|
| actualHours | float | 否 | 实际工时（小时）|
| progress | int | 否 | 完成百分比，0-100，默认0 |
| status | enum | 是 | `pending` / `in_progress` / `completed` / `blocked`，默认 `pending` |
| priority | enum | 否 | `P0`（紧急重要）/ `P1`（重要）/ `P2`（普通）/ `P3`（低优），默认 P2 |
| blockedReason | string | 否 | blocked 时填写原因 |
| dependencies | array | 否 | 依赖任务ID数组 |
| isMilestone | bool | 否 | 是否为里程碑，默认 false |
| milestoneDate | date | 否 | 里程碑日期（isMilestone=true 时必填）|
| tags | array | 否 | 标签数组，如 ["前端", "API", "设计"] |
| createdBy | string | 自动 | 创建人 member_id |
| createdAt | datetime | 自动 | 创建时间 |
| updatedAt | datetime | 自动 | 更新时间 |

#### 2.3.3 每日工作内容（成员进度填报）

| 字段 | 类型 | 说明 |
|------|------|------|
| workLogId | string | 日志ID |
| taskId | string | 关联任务ID |
| userId | string | 填报人 |
| date | date | 填报日期 |
| todayDone | text | 今日完成内容 |
| tomorrowPlan | text | 明日计划 |
| currentStatus | enum | `正常` / `有风险` / `已阻塞` |
| blockedReason | string | 阻塞原因（currentStatus=已阻塞时必填）|
| hoursSpent | float | 今日花费工时 |
| createdAt | datetime | 填报时间 |

#### 2.3.4 任务列表视图交互

- **筛选栏**：`全状态 ▼` `全成员 ▼` `全项目 ▼` `关键字搜索`
- **排序**：按「创建时间」「截止日期」「优先级」「状态」升序/降序
- **行操作**：点击展开任务详情 / 快速修改状态 / 快速指派
- **批量操作**：勾选多个任务 → 批量修改状态 / 批量指派 / 批量删除

---

### 2.4 智能排程

#### 2.4.1 用户故事

| # | 用户故事 | 角色 | 场景 |
|---|---------|------|------|
| US-S1 | 作为 PD，创建任务时系统自动推荐负载最低的成员，一键采纳 | PD | 新任务分配时，推荐列表显示各成员当前负载，一键确认 |
| US-S2 | 作为 PD，我希望看到每位成员的未来工作量分布，避免过度分配 | PD | 甘特图右侧显示成员负载热力图，红色表示超载 |
| US-S3 | 作为 PD，系统在任务延期时自动推荐可调整方案（缩短工期/换人/拆分）| PD | 某任务无法按时完成，系统给出三个调整建议 |

#### 2.4.2 推荐算法输入/输出

**输入：**
- 新任务预估时长（duration / estimatedHours）
- 所有成员当前负载（本周已分配工时 + 已安排任务数）
- 成员技能标签（可选，用于匹配任务类型）

**输出：**
- 推荐成员排名列表：`[{memberId, score, currentLoad, reason}]`
- score 越高越推荐（0-100）
- reason 说明推荐原因（"当前负载最低（32%）""技能标签匹配：前端"）

**推荐逻辑：**
```
score = 100 - currentLoadPct × 0.7 - skillMismatchPenalty × 0.3
```
- `currentLoadPct`：本周已分配工时占标准容量（40h/周）的百分比
- `skillMismatchPenalty`：技能标签不匹配程度（0=完全匹配，100=完全不匹配）
- 若多个成员 score 相近，优先推荐正在执行同项目任务的成员（上下文连贯）

---

### 2.5 成员管理

#### 2.5.1 用户故事

| # | 用户故事 | 角色 | 场景 |
|---|---------|------|------|
| US-M1 | 作为 PD，我希望能在一个页面看到所有成员的任务负载，一目了然 | PD | 打开成员视图，看到每个人名旁的工作量条（%），红色=超载 |
| US-M2 | 作为成员，我希望看到自己被分配的所有任务列表，了解当前职责 | Dev/QA | 个人工作台看到本周所有任务及其状态 |

#### 2.5.2 成员字段

| 字段 | 类型 | 说明 |
|------|------|------|
| memberId | string | 成员唯一ID，与 Agent ID 对应 |
| nickname | string | 成员昵称 |
| role | string | 角色：admin/user |
| skills | array | 技能标签，如 ["前端", "React", "Python"] |
| weeklyCapacity | float | 周标准工时，默认40小时 |
| isActive | bool | 是否在职，默认 true |
| avatar | string | 头像URL |
| email | string | 邮箱（用于通知）|

---

### 2.6 项目管理

| 字段 | 类型 | 说明 |
|------|------|------|
| projectId | string | 项目ID |
| name | string | 项目名称 |
| description | text | 项目描述 |
| owner | string | 项目负责人 PD |
| memberIds | array | 参与成员ID列表 |
| startDate | date | 项目开始日期 |
| endDate | date | 项目截止日期 |
| status | enum | planning / active / completed / archived |
| createdAt | datetime | 创建时间 |

---

## 三、甘特图设计（详细说明）

### 3.1 时间轴设计

**日视图（默认）：**
- 横向：日期行，显示"04-07 周一"格式，每格宽度 = 60px（可拖拽调整）
- 竖向：成员行，每行高度 = 48px（可展开详情）
- 今日线：红色竖线（`#FF4757`），宽度 2px，带小三角箭头指向上方

**周视图：**
- 横向：周数 + 该周周一日期，每格宽度 = 280px
- 任务条宽度 = 跨越天数 × (280px / 7)

**时间轴顶部固定，横向滚动时跟随滚动**

### 3.2 任务条设计

**普通任务条：**
```
┌────────────────────────────┐
│ ■ 任务标题          60% ████░░░░ │  ← 左侧：彩色标签  右侧：进度条
└────────────────────────────┘
高度：32px，圆角 4px，左侧 4px 颜色条（按优先级）
背景色：按状态—— pending=#E8F0FE，in_progress=#E6F4EA，completed=#D4EDDA，blocked=#FFF3E0
```

**里程碑：**
```
    ◆
  发布
```
菱形图标（20×20px），金色（里程碑颜色可配置），悬停显示里程碑名称

### 3.3 依赖箭头

- **样式**：贝塞尔曲线，从前置任务条右端（圆点锚点）出发，指向后续任务条左端
- **颜色**：默认 `#999`，悬停高亮 `#2563EB`
- **箭头**：实心三角形，指向被依赖任务
- **冲突提示**：若依赖箭头形成循环（环路），箭头显示红色虚线 + 警告图标
- **绘制方式**：SVG `<path>` 叠加在甘特图画布上

### 3.4 拖拽交互规范

| 操作 | 鼠标行为 | 保存时机 |
|------|----------|---------|
| 拖拽任务整体 | 按住任务条拖拽 → 水平移动 | 拖拽结束时（mouseup）自动保存 |
| 调整开始日期 | 悬停左边缘 → 光标变为 `ew-resize` → 拖拽 | 同上 |
| 调整结束日期 | 悬停右边缘 → 光标变为 `ew-resize` → 拖拽 | 同上 |
| 拖拽创建依赖 | 点击右锚点 → 拖出虚线 → 连接到目标左锚点 | 连接建立时自动保存 |
| 删除依赖 | 点击箭头 → 按 Delete | 立即删除 |

**拖拽时实时显示日期预览（浮层），避免误操作**

### 3.4.1 拖拽权限与原因填写

**三级拖拽权限（按项目设置，默认普通模式）：**

| 模式 | Owner（PD）| Member | Viewer |
|------|:---:|:---:|:---:|
| **严格模式** | ✅ 随意拖 | ❌ 不可拖 | ❌ 不可拖 |
| **普通模式**（默认）| ✅ 随意拖 | ✅ 可拖拽自己的任务，需填原因 | ❌ 不可拖 |
| **开放模式** | ✅ 随意拖 | ✅ 随意拖，系统自动重排下游 | ❌ 不可拖 |

**普通模式 — 拖拽原因必填（Member）：**

调整任务时间时，弹出原因选择浮层：

```
调整原因（Member 必填，Owner 可跳过）：
○ 需求变更    ○ 预估不足
○ 等待上游    ○ 个人安排
○ 其他：____（选填）
```

**记录到操作日志：**
```json
{
  "action": "drag_reorder",
  "taskId": "TT-2026040001",
  "operator": "Dev",
  "detail": {
    "oldDate": "2026-04-10",
    "newDate": "2026-04-12",
    "reason": "等待上游",
    "note": "等 UI 设计稿"  // 可选
  }
}
```

### 3.5 里程碑

- 甘特图顶部单独一行「里程碑轨道」，用菱形标记
- 里程碑hover显示：名称 + 日期 + 关联任务数
- 接近里程碑（≤3天）：甘特图顶部显示红色警告条「⚠️ 里程碑临近」
- 里程碑可拖拽调整日期

### 3.6 甘特图 UI 布局

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 📊 任务甘特图                        [日视图▼] [周视图] [+ 新建任务]      │
├──────────────────────────────────────────────────────────────────────────┤
│ 筛选：全项目▼  全成员▼  全状态▼  🔍 搜索任务...                        │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  里程碑 │ ◆ 产品发布（04-18）                                          │
│ ─────────────────────────────────────────────────────────────────────── │
│          │ 04-07  04-08  04-09  04-10  04-11  04-12  04-13  04-14  04-15│
│  ───────┼────────────────────────────────────────────────────────────  │
│  👔 PD  │                                                    ████发布  │
│          │                                                    ◆        │
│  ───────┼────────────────────────────────────────────────────────────  │
│  👤 PM  │ ████需求分析████          ████PRD评审████                      │
│          │■■■■■■■■■■■░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│  ───────┼────────────────────────────────────────────────────────────  │
│  💻 Dev │         ████技术方案████      ████开发████████████            │
│          │         → → → → → → → → → → → →■■■■■■■■■■■■■■■■■■■■■■■■■   │
│  ───────┼────────────────────────────────────────────────────────────  │
│  🎨 Des │      ████UI设计████                                     │     │
│          │      ●━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━●     │
│  ───────┼────────────────────────────────────────────────────────────  │
│  🧪 QA  │                                              ████测试████    │
│          │                                              → → → → →■■■■  │
│          │                                                      │     │
│  ───────┼────────────────────────────────────────────────────────│───── │
│          │  ↑ 今日线 04-07                                        │     │
│ ────────────────────────────────────────────────────────────────────── │
│  图例：████ 任务条  ◆里程碑  → 依赖箭头  ████░░░ 进度填充            │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 四、智能排程逻辑

### 4.1 推荐算法说明

**目标：** 给定一个新任务（含预估时长/工期），系统推荐最合适的成员。

**核心公式：**
```
推荐得分(member) = w_load × (100 - 负载率) + w_skill × 技能匹配分 + w_context × 上下文连贯分
```

**权重设置：**
| 权重 | 值 | 说明 |
|------|---|------|
| w_load | 0.55 | 负载权重（最重要）|
| w_skill | 0.30 | 技能匹配权重 |
| w_context | 0.15 | 项目上下文连贯权重 |

**负载率计算：**
```
负载率 = 本周已分配工时 / 成员周标准容量（默认40h）
已分配工时 = SUM(estimatedHours) of 所有 in_progress + pending 任务
```

**技能匹配分：**
```
技能匹配分 = 100 - 不匹配标签数 × 25
若任务有技能标签["React"]，成员技能含"React"→100分，含"Vue"不含"React"→75分，含"Python"不含前端框架→25分
```

**上下文连贯分：**
```
上下文连贯分 = 成员在该项目已有任务数 × 20（最高100分）
```
→ 已在同一项目工作的成员有更高的连贯分，减少交接成本

### 4.2 超载检测

- 负载率 ≥ 100%：标记为红色「超载」
- 负载率 ≥ 80%：标记为黄色「高负载」
- 甘特图成员行右侧显示负载热力条

### 4.3 冲突解决建议

当任务因依赖链导致无法按时完成时，系统自动给出以下建议（由 PD 选择）：
1. **缩短工期**：建议减少非关键路径任务的缓冲时间
2. **增加人力**：推荐可调配的成员（负载<60%）
3. **拆分任务**：将大任务拆分为可并行的小任务
4. **延迟发布**：调整里程碑日期（如技术上无法压缩）

---

## 五、数据库设计

### 5.1 项目表
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
);
```

### 5.2 项目成员关联表
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
);
```

### 5.3 成员表
```sql
CREATE TABLE members (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       VARCHAR(64) NOT NULL UNIQUE COMMENT '成员ID',
    nickname        VARCHAR(64) NOT NULL COMMENT '成员昵称',
    role            VARCHAR(32) NOT NULL COMMENT '角色：PD/PM/Dev/Des/QA/Ops',
    skills          JSON COMMENT '技能标签数组，如["React","Python"]',
    weekly_capacity  FLOAT DEFAULT 40.0 COMMENT '周标准工时（小时）',
    is_active       TINYINT DEFAULT 1 COMMENT '是否在职',
    avatar          VARCHAR(256) COMMENT '头像URL',
    email           VARCHAR(128) COMMENT '邮箱',
    password_hash   VARCHAR(256) COMMENT '登录密码哈希（未来扩展）',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (role),
    INDEX idx_is_active (is_active)
);
```

### 5.4 任务表
```sql
CREATE TABLE tasks (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         VARCHAR(64) NOT NULL UNIQUE COMMENT '任务ID，格式TT-XXX',
    title           VARCHAR(256) NOT NULL COMMENT '任务标题',
    description     TEXT COMMENT '任务描述',
    project_id      VARCHAR(64) NOT NULL COMMENT '所属项目ID',
    assignee_id     VARCHAR(64) COMMENT '负责人member_id（可为空，未分配时）',
    start_date      DATE COMMENT '开始日期',
    end_date        DATE COMMENT '结束日期',
    duration        INT COMMENT '持续天数（可不填，根据起止日计算）',
    estimated_hours FLOAT COMMENT '预估工时（小时）',
    actual_hours    FLOAT DEFAULT 0 COMMENT '实际工时',
    progress        INT DEFAULT 0 COMMENT '完成百分比，0-100',
    status          VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/in_progress/completed/blocked',
    priority        VARCHAR(8) DEFAULT 'P2' COMMENT 'P0/P1/P2/P3',
    blocked_reason  VARCHAR(512) COMMENT '阻塞原因',
    is_milestone    TINYINT DEFAULT 0 COMMENT '是否里程碑',
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
    INDEX idx_is_milestone (is_milestone)
);
```

### 5.5 任务依赖关系表
```sql
CREATE TABLE task_dependencies (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         VARCHAR(64) NOT NULL COMMENT '任务ID（被依赖者/后续任务）',
    depends_on      VARCHAR(64) NOT NULL COMMENT '依赖的任务ID（前置任务）',
    dependency_type VARCHAR(16) DEFAULT 'FS' COMMENT 'FS=Finish-to-Start',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dependency (task_id, depends_on),
    INDEX idx_task (task_id),
    INDEX idx_depends_on (depends_on)
);
```

### 5.6 每日工作日志表
```sql
CREATE TABLE task_work_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id          VARCHAR(64) NOT NULL UNIQUE COMMENT '日志ID',
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
);
```

### 5.7 里程碑表
```sql
CREATE TABLE milestones (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    milestone_id    VARCHAR(64) NOT NULL UNIQUE COMMENT '里程碑ID',
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
);
```

---

## 六、API 设计

### 6.1 项目接口

#### 6.1.1 获取项目列表
```
GET /api/projects
```

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | 筛选状态：planning/active/completed/archived |
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20 |

**响应：**
```json
{
  "code": 0,
  "data": {
    "total": 5,
    "projects": [
      {
        "projectId": "PRJ-001",
        "name": "AI 笔记 v2.0",
        "description": "下一代笔记系统重构",
        "owner": "pd",
        "startDate": "2026-04-01",
        "endDate": "2026-04-30",
        "status": "active",
        "memberCount": 5
      }
    ]
  }
}
```

#### 6.1.2 创建项目
```
POST /api/projects
```
```json
{
  "name": "AI 笔记 v2.0",
  "description": "下一代笔记系统重构",
  "ownerId": "pd",
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "memberIds": ["pd", "pm", "dev", "des", "qa"]
}
```

#### 6.1.3 获取项目详情
```
GET /api/projects/:projectId
```

#### 6.1.4 更新项目
```
PUT /api/projects/:projectId
```

#### 6.1.5 删除项目（软删除）
```
DELETE /api/projects/:projectId
```

---

### 6.2 任务接口

#### 6.2.1 获取任务列表
```
GET /api/projects/:projectId/tasks
```

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | 筛选状态 |
| assigneeId | string | 否 | 筛选负责人 |
| priority | string | 否 | 筛选优先级 |
| keyword | string | 否 | 搜索标题 |
| page | int | 否 | 默认1 |
| pageSize | int | 否 | 默认50 |

**响应：**
```json
{
  "code": 0,
  "data": {
    "total": 23,
    "tasks": [
      {
        "taskId": "TT-001",
        "title": "需求分析",
        "projectId": "PRJ-001",
        "assigneeId": "pm",
        "assigneeName": "PM",
        "startDate": "2026-04-07",
        "endDate": "2026-04-10",
        "duration": 4,
        "progress": 30,
        "status": "in_progress",
        "priority": "P1",
        "isMilestone": false,
        "dependencies": [],
        "tags": ["需求"]
      }
    ]
  }
}
```

#### 6.2.2 获取甘特图数据
```
GET /api/projects/:projectId/gantt
```

**响应：**
```json
{
  "code": 0,
  "data": {
    "tasks": [
      {
        "taskId": "TT-001",
        "title": "需求分析",
        "assigneeId": "pm",
        "startDate": "2026-04-07",
        "endDate": "2026-04-10",
        "progress": 30,
        "status": "in_progress",
        "priority": "P1",
        "dependencies": ["TT-002"],
        "isMilestone": false,
        "left": 0,
        "width": 4
      }
    ],
    "milestones": [
      {
        "milestoneId": "MS-001",
        "name": "产品发布",
        "targetDate": "2026-04-18",
        "color": "#FFD700"
      }
    ],
    "members": [
      { "memberId": "pm", "nickname": "PM" },
      { "memberId": "dev", "nickname": "Dev" }
    ]
  }
}
```

#### 6.2.3 创建任务
```
POST /api/projects/:projectId/tasks
```
```json
{
  "title": "UI 设计",
  "description": "完成所有页面的 UI 设计稿",
  "assigneeId": "des",
  "startDate": "2026-04-08",
  "endDate": "2026-04-14",
  "estimatedHours": 24,
  "priority": "P1",
  "tags": ["UI", "设计"],
  "dependencies": ["TT-001"]
}
```

**响应：**
```json
{
  "code": 0,
  "data": {
    "taskId": "TT-002",
    "status": "created"
  }
}
```

#### 6.2.4 更新任务
```
PUT /api/projects/:projectId/tasks/:taskId
```
```json
{
  "title": "UI 设计稿",
  "assigneeId": "des",
  "startDate": "2026-04-09",
  "endDate": "2026-04-15",
  "progress": 50,
  "status": "in_progress"
}
```

#### 6.2.5 拖拽更新任务时间（甘特图专用）
```
PUT /api/projects/:projectId/tasks/:taskId/drag
```
```json
{
  "startDate": "2026-04-10",
  "endDate": "2026-04-16"
}
```
**说明：** 此接口专为甘特图拖拽设计，响应时间需 < 100ms

#### 6.2.6 删除任务
```
DELETE /api/projects/:projectId/tasks/:taskId
```

#### 6.2.7 批量更新任务状态
```
PUT /api/projects/:projectId/tasks/batch-status
```
```json
{
  "taskIds": ["TT-001", "TT-002"],
  "status": "completed"
}
```

#### 6.2.8 批量指派任务
```
PUT /api/projects/:projectId/tasks/batch-assign
```
```json
{
  "taskIds": ["TT-001", "TT-002"],
  "assigneeId": "dev-dq"
}
```

---

### 6.3 依赖管理

#### 6.3.1 添加任务依赖
```
POST /api/projects/:projectId/tasks/:taskId/dependencies
```
```json
{
  "dependsOn": "TT-001"
}
```

**错误响应（环路检测）：**
```json
{
  "code": 40010,
  "message": "检测到循环依赖：TT-001 → TT-002 → TT-003 → TT-001，无法创建"
}
```

#### 6.3.2 删除任务依赖
```
DELETE /api/projects/:projectId/tasks/:taskId/dependencies/:dependsOnTaskId
```

---

### 6.4 智能排程

#### 6.4.1 获取成员负载
```
GET /api/projects/:projectId/members/loads
```

**响应：**
```json
{
  "code": 0,
  "data": {
    "projectId": "PRJ-001",
    "weekStart": "2026-04-07",
    "weekEnd": "2026-04-13",
    "members": [
      {
        "memberId": "dev",
        "nickname": "Dev",
        "role": "Dev",
        "weeklyCapacity": 40.0,
        "allocatedHours": 32.0,
        "loadPct": 80,
        "loadLevel": "high",
        "taskCount": 4
      },
      {
        "memberId": "dev-dq",
        "nickname": "Dev-DQ",
        "role": "Dev",
        "weeklyCapacity": 40.0,
        "allocatedHours": 16.0,
        "loadPct": 40,
        "loadLevel": "normal",
        "taskCount": 2
      }
    ]
  }
}
```

#### 6.4.2 获取任务分配推荐
```
GET /api/projects/:projectId/tasks/recommend?duration=3&estimatedHours=24&requiredSkills=["React"]
```

**响应：**
```json
{
  "code": 0,
  "data": {
    "recommendations": [
      {
        "rank": 1,
        "memberId": "dev-dq",
        "nickname": "Dev-DQ",
        "score": 92,
        "currentLoad": 40,
        "reason": "当前负载最低（40%），技能标签匹配"
      },
      {
        "rank": 2,
        "memberId": "dev",
        "nickname": "Dev",
        "score": 74,
        "currentLoad": 80,
        "reason": "当前负载较高（80%），技能标签匹配"
      },
      {
        "rank": 3,
        "memberId": "pm",
        "nickname": "PM",
        "score": 55,
        "currentLoad": 30,
        "reason": "当前负载最低，但技能标签不匹配（缺少：React）"
      }
    ]
  }
}
```

---

### 6.5 每日工作日志

#### 6.5.1 填报工作日志
```
POST /api/tasks/:taskId/worklogs
```
```json
{
  "logDate": "2026-04-07",
  "todayDone": "完成了模块A的接口对接，调试通过",
  "tomorrowPlan": "继续完成模块B的对接，开始集成测试",
  "currentStatus": "正常",
  "hoursSpent": 6.5
}
```

#### 6.5.2 获取任务工作日志列表
```
GET /api/tasks/:taskId/worklogs
```

**响应：**
```json
{
  "code": 0,
  "data": {
    "logs": [
      {
        "logId": "WL-001",
        "taskId": "TT-001",
        "userId": "dev",
        "logDate": "2026-04-07",
        "todayDone": "完成了模块A的接口对接",
        "tomorrowPlan": "继续完成模块B的对接",
        "currentStatus": "正常",
        "hoursSpent": 6.5,
        "createdAt": "2026-04-07T18:30:00+08:00"
      }
    ]
  }
}
```

#### 6.5.3 获取成员工作日志（按日聚合）
```
GET /api/projects/:projectId/worklogs/daily?date=2026-04-07
```

**响应：**
```json
{
  "code": 0,
  "data": {
    "date": "2026-04-07",
    "logs": [
      {
        "userId": "dev",
        "nickname": "Dev",
        "taskLogs": [
          {
            "taskId": "TT-001",
            "title": "API 开发",
            "todayDone": "完成了接口对接",
            "currentStatus": "正常",
            "progress": 60
          }
        ]
      }
    ]
  }
}
```

---

### 6.6 里程碑

#### 6.6.1 获取里程碑列表
```
GET /api/projects/:projectId/milestones
```

#### 6.6.2 创建里程碑
```
POST /api/projects/:projectId/milestones
```
```json
{
  "name": "产品发布",
  "targetDate": "2026-04-18",
  "description": "v1.0 正式发布",
  "color": "#FFD700"
}
```

#### 6.6.3 更新里程碑
```
PUT /api/projects/:projectId/milestones/:milestoneId
```

#### 6.6.4 删除里程碑
```
DELETE /api/projects/:projectId/milestones/:milestoneId
```

---

### 6.7 成员管理

#### 6.7.1 获取成员列表
```
GET /api/members
```

#### 6.7.2 获取成员详情（含负载）
```
GET /api/members/:memberId?projectId=PRJ-001
```

#### 6.7.3 创建成员
```
POST /api/members
```
```json
{
  "memberId": "dev",
  "nickname": "Dev",
  "role": "Dev",
  "skills": ["React", "Node.js"],
  "weeklyCapacity": 40.0,
  "email": "dev@example.com"
}
```

#### 6.7.4 更新成员
```
PUT /api/members/:memberId
```

---

### 6.8 API 错误码

| 错误码 | 说明 |
|--------|------|
| 40001 | 参数错误 |
| 40002 | 资源不存在 |
| 40003 | 权限不足 |
| 40010 | 循环依赖检测 |
| 40011 | 任务时间冲突 |
| 40012 | 成员不在项目中 |
| 50001 | 服务器内部错误 |

---

## 七、UI 布局

### 7.1 页面结构

整体布局采用**侧边栏 + 主内容区**结构，参考控制台 v1.5 的 Tab 设计：

```
┌────────────────────────────────────────────────────────────────────┐
│ 👔 任务舱              [🔔 通知] [👤 PD] [⚙️ 设置]       │
├─────────┬──────────────────────────────────────────────────────────┤
│         │                                                          │
│ 📁 项目 │  主内容区（甘特图 / 列表 / 成员 / 设置）                   │
│ ────── │                                                          │
│ AI笔记 │                                                          │
│ 控制台 │                                                          │
│ SOP系统│                                                          │
│         │                                                          │
│ 📊 视图 │                                                          │
│ ────── │                                                          │
│ 甘特图 │                                                          │
│ 任务列表│                                                          │
│ 成员视图│                                                          │
│         │                                                          │
│ ➕ 新建  │                                                          │
└─────────┴──────────────────────────────────────────────────────────┘
```

### 7.2 甘特图 Tab（默认视图）

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 📊 甘特图                                    [日视图▼] [+ 新建任务]     │
├──────────────────────────────────────────────────────────────────────────┤
│ 筛选：全项目▼  全成员▼  全状态▼  全优先级▼   🔍 搜索...  [重置]        │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ◆ 产品发布 04-18        ◆ 测试验收 04-25                               │
│ ─────────────────────────────────────────────────────────────────────  │
│                                                                        │
│   04-07   04-08   04-09   04-10   04-11   04-12   04-13   04-14  04-15 │
│                                                                        │
│ PM  ████████████████                    ████████████                   │
│     需求分析 100%                        PRD评审 40%                     │
│                                                                        │
│ Dev      ████████████████      ████████████████████████████████       │
│          技术方案 50%             开发 0%                                │
│                                        ↑依赖箭头→→→→→→→→→→→→→→→→→→→→│
│                                                                        │
│ Des   ██████████████████████                                        │ │
│       UI设计 80%                                                        │
│                                                                        │
│ QA                                      ██████████████████████         │
│                                        测试验收 0%                       │
│                                                            │            │
│ ───────────────────────────────────────────────────────────┼────────── │
│                                                     今日→│            │
│ ────────────────────────────────────────────────────────────────────── │
│  [图例] ████ 任务条  ◆里程碑  ──→ 依赖箭头  ████░░ 进度填充  🔴今日线 │
└──────────────────────────────────────────────────────────────────────────┘
```

**任务条悬停浮层：**
```
┌─────────────────────────────┐
│ 📋 TT-001 需求分析          │
│ 负责人：PM                  │
│ 时间：04-07 ~ 04-10（4天）  │
│ 进度：██████████░░░░░░░ 60% │
│ 状态：🔄 进行中             │
│ 依赖：无                    │
│ ─────────────────────────── │
│ [📝 更新进度] [✏️ 编辑]     │
└─────────────────────────────┘
```

### 7.3 任务列表 Tab

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 📋 任务列表                                    [+ 新建任务] [导出CSV]     │
├──────────────────────────────────────────────────────────────────────────┤
│  全项目▼  全成员▼  全状态▼  全优先级▼   排序：最近▼   🔍 搜索...       │
├──────────────────────────────────────────────────────────────────────────┤
│ ☐ │ 任务ID  │ 标题         │ 负责人  │ 时间        │ 优先级 │ 状态   │ 进度 │
│───┼─────────┼──────────────┼─────────┼─────────────┼────────┼────────┼─────│
│ ☐ │ TT-001  │ 需求分析     │ PM      │ 04-07~04-10 │ P1     │ 🔄进行中│ 60% │
│ ☐ │ TT-002  │ UI设计       │ Des     │ 04-08~04-14 │ P1     │ 🔄进行中│ 80% │
│ ☐ │ TT-003  │ 技术方案     │ Dev     │ 04-09~04-12 │ P2     │ ⏳待开始│  0% │
│ ☐ │ TT-004  │ 开发         │ Dev     │ 04-13~04-20 │ P1     │ ⏳待开始│  0% │
│ ☐ │ TT-005  │ 测试验收     │ QA      │ 04-21~04-25 │ P1     │ ⏳待开始│  0% │
├──────────────────────────────────────────────────────────────────────────┤
│ 已选中 0 项  │  批量：☑️ 完成任务  ☑️ 指派给  ▼  ☑️ 删除             │
└──────────────────────────────────────────────────────────────────────────┘
```

### 7.4 成员视图 Tab

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 👥 成员视图                                                    [添加成员] │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  👔 PD       张三     负载：██████░░░░░░░░░░░  60%  任务数：3          │
│  ────────────────────────────────────────────────────────────────────── │
│  本周任务：项目规划 v1.0 (30%)  ·  周会主持 (100%)  ·  代码评审 (0%)    │
│                                                                          │
│  💻 Dev      李四     负载：████████████░░░░░  85%🟡 任务数：4          │
│  ────────────────────────────────────────────────────────────────────── │
│  本周任务：API开发 (60%)  ·  数据库设计 (100%)  ·  Bug修复 (20%)  ...   │
│                                                                          │
│  🧪 QA       王五     负载：████████░░░░░░░░░  40%  任务数：2           │
│  ────────────────────────────────────────────────────────────────────── │
│  本周任务：测试用例编写 (50%)  ·  集成测试 (10%)                        │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 7.5 新建/编辑任务弹窗

```
┌──────────────────────────────────────────────────────────────────────────┐
│  新建任务                                              [×]               │
├──────────────────────────────────────────────────────────────────────────┤
│  任务标题 *                                                           │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                                                                   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  所属项目 *  [AI笔记 v2.0 ▼]                                           │
│                                                                          │
│  负责人 *     [PM ▼]  [🤖 查看推荐]                                   │
│  ──────────────────────────────────────────────────────────────────   │
│  💡 推荐排序：                                                        │
│   1. Dev-DQ（负载40%）— 推荐采纳 [采用]                               │
│   2. PM（负载30%）                                                    │
│   3. Dev（负载80%）                                                   │
│                                                                          │
│  开始日期     结束日期       预估工时（小时）                          │
│  [04-08   ] ~ [04-14   ]   [24              ]                          │
│                                                                          │
│  优先级   [P1 ▼]   标签 [添加标签...]                                  │
│                                                                          │
│  依赖任务   [+ 添加依赖]                                               │
│                                                                          │
│  任务描述                                                           │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                                                                   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│                                                    [取消]  [保存]       │
└──────────────────────────────────────────────────────────────────────────┘
```

### 7.6 任务详情侧边栏（右侧滑出）

```
┌──────────────────────────────────────────────┐
│ ← 返回          TT-001  需求分析      [P1]   │
├──────────────────────────────────────────────┤
│                                              │
│  负责人：PM                                  │
│  时间：04-07 ~ 04-10（4天）                  │
│  预估：24h    实际：16h                       │
│  进度：██████████░░░░░░░░░░░░░░  60%        │
│  状态：[🔄 进行中 ▼]                         │
│                                              │
│  📝 今日工作（Dev 04-07 18:30 填报）          │
│  ──────────────────────────────────────────── │
│  已完成：接口文档整理、模块划分               │
│ 明日计划：继续完成详细需求文档                │
│ 当前状态：正常    耗时：6.5h                  │
│                                              │
│  📊 工作日志（近7天）                        │
│  ──────────────────────────────────────────── │
│  04-07 Dev  🔄 进行中  今日完成60%           │
│  04-06 Dev  🔄 进行中  今日完成30%           │
│  04-05 PM   ✅ 已完成  需求初稿完成          │
│                                              │
│  🔗 依赖关系                                  │
│  ──────────────────────────────────────────── │
│  前置依赖：无                                 │
│  后续任务：TT-002 UI设计                      │
│                                              │
│  📎 操作日志                                  │
│  ──────────────────────────────────────────── │
│  04-07 10:00  PM 创建任务                     │
│  04-07 10:05  PM 指派给 Dev                  │
│  04-07 14:00  Dev 开始执行                   │
│                                              │
│  [✏️ 编辑]  [🔔 催办]  [📌 标记里程碑]       │
└──────────────────────────────────────────────┘
```

---

## 八、验收标准

### 8.1 甘特图

| # | 验收项 | 通过标准 |
|---|--------|---------|
| G-1 | 甘特图渲染 | 打开项目甘特图，所有任务正确显示为任务条，位置与日期对应 |
| G-2 | 任务条颜色 | pending=蓝色，in_progress=绿色，completed=灰色，blocked=橙色，优先级颜色条左侧显示 |
| G-3 | 今日线 | 红色竖线准确显示在当前日期位置 |
| G-4 | 拖拽改时间 | 拖拽任务条，日期实时更新，刷新页面后保存正确 |
| G-5 | 拖拽边缘调整时长 | 拖拽任务条左/右边缘，仅改变开始/结束日期 |
| G-6 | 依赖箭头 | 两个有依赖关系的任务之间显示箭头，前置完成前后续任务条显示灰色锁定 |
| G-7 | 循环依赖检测 | 创建依赖时若形成循环，后端返回 40010 错误，前端提示"检测到循环依赖" |
| G-8 | 里程碑 | 甘特图顶部里程碑行显示菱形标记，hover 显示详情 |
| G-9 | 里程碑临近预警 | 距里程碑 ≤3 天时，甘特图顶部显示红色警告条 |
| G-10 | 缩放切换 | 日视图/周视图切换，甘特图时间轴和任务条宽度正确重算 |
| G-11 | 任务悬停浮层 | 鼠标悬停任务条 300ms 后显示浮层，包含标题/负责人/状态/进度/依赖信息 |
| G-12 | 横向滚动 | 当任务时间跨度超过视图宽度时，可横向滚动，时间轴固定跟随 |

### 8.2 任务管理

| # | 验收项 | 通过标准 |
|---|--------|---------|
| T-1 | 创建任务 | 填写标题+负责人后保存，任务出现在甘特图和列表中 |
| T-2 | 任务字段完整 | 创建任务后，所有字段（时间/预估工时/标签/优先级）正确保存 |
| T-3 | 编辑任务 | 修改任意字段后保存，刷新页面数据正确 |
| T-4 | 删除任务 | 删除任务后，甘特图和列表中均消失，关联依赖箭头一并清除 |
| T-5 | 批量指派 | 勾选多个任务 → 选择负责人 → 批量指派，所有任务负责人同步更新 |
| T-6 | 批量修改状态 | 勾选多个任务 → 修改状态 → 所有任务状态同步更新 |
| T-7 | 筛选 | 按成员/状态/优先级筛选后，列表和甘特图同步过滤 |
| T-8 | 搜索 | 输入关键词搜索，列表返回包含关键词的任务（模糊匹配标题）|

### 8.3 智能排程

| # | 验收项 | 通过标准 |
|---|--------|---------|
| S-1 | 负载统计 | 成员负载百分比 = 本周已分配工时 / 40h，计算正确 |
| S-2 | 推荐排序 | 推荐列表按 score 降序排列，负载最低且技能匹配的成员排第一 |
| S-3 | 推荐采纳 | 点击「采用」按钮，负责人自动填充为推荐成员 |
| S-4 | 超载标识 | 负载≥100% 的成员在成员视图中显示红色「超载」标签 |
| S-5 | 高负载预警 | 负载≥80% 的成员在成员视图中显示黄色标签 |

### 8.4 进度跟踪

| # | 验收项 | 通过标准 |
|---|--------|---------|
| P-1 | 填报工作日志 | 成员填写今日完成+明日计划+状态后提交，日志出现在任务详情中 |
| P-2 | 进度更新 | 填报日志后，任务进度自动更新（或手动填写进度）|
| P-3 | 阻塞标记 | currentStatus=已阻塞 时，blockedReason 必填，任务甘特图条显示橙色 |
| P-4 | 每日汇总 | 成员视图按日聚合显示所有成员当日工作内容 |
| P-5 | 历史日志 | 任务详情展示近7天工作日志，按时间倒序排列 |

### 8.5 里程碑

| # | 验收项 | 通过标准 |
|---|--------|---------|
| M-1 | 创建里程碑 | 填写名称+日期后保存，里程碑出现在甘特图顶部 |
| M-2 | 拖拽调整日期 | 拖拽里程碑菱形到新日期，保存后日期正确更新 |
| M-3 | 删除里程碑 | 删除里程碑后，甘特图顶部标记消失 |
| M-4 | 临近预警 | 距里程碑 ≤3 天时，顶部显示警告条「⚠️ [里程碑名] 临近（还剩X天）」|

### 8.6 项目与成员

| # | 验收项 | 通过标准 |
|---|--------|---------|
| A-1 | 创建项目 | 填写项目名+负责人+成员后，项目出现在侧边栏列表 |
| A-2 | 项目隔离 | 切换到项目A，只能看到项目A的任务，成员列表仅包含项目A成员 |
| A-3 | 添加成员 | 在项目设置中添加成员，新成员可看到并操作该项目任务 |
| A-4 | 成员技能 | 成员技能标签正确保存，并在智能推荐时作为技能匹配依据 |

---

## 九、技术实现建议

### 9.1 前端技术

- **框架**：Vue3 + Element Plus
- **甘特图**：dhtmlxGantt（MIT 开源，依赖箭头/虚拟滚动开箱即用）
- **拖拽**：Vue Draggable Plus（轻量，支持任务条拖拽）
- **状态管理**：Pinia
- **日期处理**：dayjs

### 9.2 后端技术

- **框架**：Node.js + Express（或 Koa）
- **数据库**：MySQL（如本项目已有），或 SQLite（轻量独立部署）
- **实时**：WebSocket（Socket.io）用于甘特图多人协作同步
- **任务ID生成**：Redis INCR `taskIdCounter`，格式 `TT-{year}{month}{seq}`

### 9.3 性能考虑

- 甘特图数据首次加载 < 200ms（前端缓存 + 后端索引）
- 拖拽操作响应 < 100ms（前端乐观更新，后端异步保存）
- 成员负载统计使用 SQL 聚合，不实时计算（页面加载时计算）

### 9.4 安全考虑

- 成员只能操作自己所属项目的任务
- 只有 PD（owner）可以删除项目/里程碑
- 任务操作记录完整审计日志（操作人/时间/变更内容）

---

## 十、v2 新增功能（P0/P1 优先级）

> 基于 Linear/Notion 设计系统分析 + 业务逻辑优化，新增以下功能。

### 10.1 泳道视图（P1）

**描述：** 按负责人分列，每列一个泳道，展示该成员在一段时间内的任务安排。

**交互：**
- 横向时间轴（天/周视图），可左右滑动
- 每个泳道一行，成员头像 + 名字固定在左侧（sticky）
- 任务条覆盖对应时间段，支持拖拽移动
- 点击任务条展开详情

**与看板的区别：** 看板按任务状态分列，泳道按人分列。泳道更适合查看「谁在什么时间做什么」。

**UI 布局：**
```
[成员] | 4/7周一 | 4/8周二 | 4/9周三 | 4/10周四 | 4/11周五
Dev    | ████甘特图前端██████ |      | ███任务依赖██ | ...
Des    |      | ████UI设计██████ |      |      | ...
QA     |      |      | ███测试███ |      |      | ...
```

### 10.2 时长自动推算（P0）

**描述：** 任务创建/编辑时，只需填写「开始日期」+「工期（天）」，截止日期由系统自动计算。

**规则：**
- 截止日期 = 开始日期 + 工期（自然日）
- 开启「工作日模式」后，跳过周末自动计算工作日
- 修改开始日期或工期，任一字段均自动重算另一字段

**字段设计：**
| 字段 | 类型 | 说明 |
|------|------|------|
| start_date | DATE | 开始日期（必填） |
| duration_days | INT | 工期天数（必填，默认 1） |
| end_date | DATE | 截止日期（自动计算，只读） |
| workdays_only | BOOLEAN | 工作日模式（默认 false） |

### 10.3 自动重排下游依赖（P0）

**描述：** 开启后，当任务 A 的时间发生变化（拖拽/修改），所有依赖 A 的任务自动顺延。

**触发时机：**
- 任务开始时间提前 → 下游任务不变
- 任务开始时间延后 → 下游任务自动顺延相同天数
- 任务工期变化 → 下游任务相应调整

**冲突检测：**
- 若重排后超过项目截止日期，弹窗提示用户确认
- 环路检测（同 v1.0）：重排后若产生环路，回滚并报错

**UI 开关：** 任务详情 → 高级设置 → ☑️ 自动重排下游

### 10.4 操作日志（P1）

**描述：** 每个任务的完整变更历史，谁在什么时间改了什么都记录。

**记录内容：**
| 操作类型 | 记录字段 |
|----------|----------|
| 创建 | 创建人、创建时间 |
| 状态变更 | 旧状态 → 新状态、操作人、时间 |
| 时间调整 | 旧时间 → 新时间、操作人、时间 |
| 负责人变更 | 旧负责人 → 新负责人、操作人、时间 |
| 前置任务变更 | 变更内容、操作人、时间 |
| 阻塞原因变更 | 变更内容、操作人、时间 |

**表结构扩展（tasks_history）：**
```sql
CREATE TABLE task_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(32) NOT NULL,
  operator VARCHAR(64) NOT NULL,
  action ENUM('create','update','delete','status_change','assign','reorder') NOT NULL,
  detail JSON,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task_id (task_id),
  INDEX idx_created_at (created_at)
);
```

### 10.5 任务评论（P1）

**描述：** 每个任务详情页可评论，@成员触发通知。

**功能：**
- 评论支持 @成员，自动解析并推送通知
- 支持上传截图/附件
- 评论后任务负责人收到实时推送（WebSocket）
- 支持引用其他任务（`#TT-2026040001` 自动高亮跳转）

**通知内容：** `Dev 评论了你的任务 [甘特图前端开发]：这个依赖需要调整顺序`

### 10.6 工作日模式（P1）

**描述：** 智能排程和时长计算可选择跳过周末/节假日。

**配置项：**
- 全局工作日设置：周一至周五 / 自定义
- 节假日白名单：手动维护节假日列表
- 自动跳过：排程、甘特图时长计算均受此影响

**UI：** 设置 → 工作日历 → 配置工作日 + 导入节假日

### 10.7 关键路径高亮（P2）

**描述：** 甘特图自动标注项目最长链路（关键路径），高亮显示。

**算法：** 从终点里程碑倒推，找出耗时最长的任务链，标记为关键路径。

**UI：** 甘特图工具栏 → ☑️ 关键路径高亮 → 相关任务条边框加粗 + 红色标记

---

## 十一、v2 API 扩展

### 11.1 新增接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/tasks/:id/history` | 获取任务操作历史 |
| POST | `/api/tasks/:id/comments` | 添加评论 |
| GET | `/api/tasks/:id/comments` | 获取评论列表 |
| PUT | `/api/projects/:id/settings` | 更新项目设置（含工作日配置）|
| GET | `/api/projects/:id/critical-path` | 获取关键路径 |
| POST | `/api/tasks/reorder` | 批量重排下游依赖 |

### 11.2 新增错误码

| 错误码 | 说明 |
|--------|------|
| 40020 | 任务重排超出项目截止日期 |
| 40021 | 环路检测：重排后产生循环依赖 |
| 40022 | 节假日配置冲突 |

---

## 十二、v2 验收标准

### G 类（Gantt）
| # | 标准 | 可测试条件 |
|---|------|-----------|
| G1 | 泳道视图正确显示每个成员的任务条 | 切换泳道视图，数据与甘特图一致 |
| G2 | 时长自动计算截止日期 | 修改工期，自动更新截止日期 |
| G3 | 工作日模式下跳过周末 | 设置周一任务工期3天，正确显示周四完成 |

### P 类（Project）
| # | 标准 | 可测试条件 |
|---|------|-----------|
| P1 | 操作日志完整记录 | 改状态/改时间/改人，history API 返回完整记录 |
| P2 | 自动重排下游生效 | 修改任务开始时间，依赖任务自动顺延 |
| P3 | 关键路径高亮 | 甘特图工具栏打开后，路径正确标注 |

### C 类（Collaboration）
| # | 标准 | 可测试条件 |
|---|------|-----------|
| C1 | 任务评论 @成员 | 评论输入 @Dev，Dev 收到通知 |
| C2 | 评论列表正确返回 | GET /comments 返回按时间正序 |

---

## 十三、v1.5 迭代优化建议

> 基于产品体验反馈，整理 v1.5 迭代功能清单。标记说明：🔴 P0 = v1.5 必做，🟡 P1 = v1.5 尽量做，⚪ P2 = 后续迭代。

### 13.1 甘特图交互体验

#### 🔴 P0 — 拖拽指派负责人
**描述：** 将任务条拖拽到左侧成员轨道，直接更换负责人，减少 PD 手动编辑操作。

**交互：**
1. 鼠标悬停在任务条 → 左侧成员列表高亮
2. 拖拽任务条 → 移入某成员行 → 该行边框高亮
3. 松开 → 弹出确认：「确认将 [任务名] 指派给 [成员]？」
4. 确认后自动保存，同时操作日志记录

**前端事件：**
```javascript
// 拖拽到成员行触发
ondragmember = (taskId, memberId) => {
  openAssignConfirm(taskId, memberId) // 确认后 PUT /api/tasks/:id/assignee
}
```

#### 🟡 P1 — 右键快捷操作面板
**描述：** 任务条右键弹出快捷菜单，无需打开侧边栏。

**菜单项：**
```
🖊 修改状态        → 子菜单：待办 / 进行中 / 已完成 / 已阻塞
📊 修改进度        → 滑块 0-100%
⚠ 标记阻塞        → 输入阻塞原因
📝 添加备注        → 文本框
👤 更换负责人      → 成员选择
🔗 设置依赖        → 弹出依赖选择器
🗑 删除任务        → 二次确认
```

**实现：** 使用原生 `contextmenu` 事件，自定义 `.context-menu` 浮层。

#### 🟡 P1 — 任务条层级置顶
**描述：** 任务条重叠时，右键菜单增加「置顶 / 置底」功能，避免关键任务被遮挡。

#### ⚪ P2 — 离线操作支持
**描述：** 拖拽 / 编辑操作写入 LocalStorage，联网后自动同步。

**数据结构：**
```javascript
// 离线队列
offlineQueue = [
  { action: 'UPDATE_TASK', taskId: 'TT-xxx', changes: {startDate: '2026-04-10'}, ts: 1712500000 },
  { action: 'ASSIGN', taskId: 'TT-yyy', assigneeId: 'dev-02', ts: 1712500001 }
]
// 联网时批量 POST /api/sync
```

---

### 13.2 智能排程体验

#### 🟡 P1 — 推荐理由可视化（负载趋势图）
**描述：** 悬停推荐成员时，显示本周 / 下周负载曲线图。

**交互：**
- 悬停任一推荐成员 → 浮层显示折线图（本周负载 + 下周预测）
- 峰值用红色标注，辅助 PD 判断是否适合分配

**负载数据 API：**
```json
GET /api/members/{id}/load-trend
Response: {
  "memberId": "dev-01",
  "weeks": [
    { "week": "2026-W14", "loadPercent": 72, "taskCount": 5 },
    { "week": "2026-W15", "loadPercent": 88, "taskCount": 7 }
  ]
}
```

#### 🟡 P1 — 批量排程
**描述：** 选中多个任务，一键触发「智能分配」，按负载均分。

**交互：**
1. 甘特图勾选多个任务（任务条左侧复选框）
2. 点击工具栏「🧠 批量智能分配」
3. 系统自动分配，结果预览 → PD 确认后保存

**API：**
```json
POST /api/tasks/batch-schedule
Body: { taskIds: ["TT-001","TT-002","TT-003"], mode: "balance_load" }
Response: { results: [{taskId, assigneeId, score, reason}] }
```

#### 🔴 P0 — 超载预警前置
**描述：** 创建 / 指派任务时，若负责人负载 ≥ 80%，**实时弹出黄色预警**。

**预警时机：**
- 创建任务选择了负责人 → 实时检测
- 指派任务更换负责人 → 实时检测
- 智能排程推荐时 → 过滤掉 ≥ 80% 的成员（标注"负载过高"）

**预警文案：**
```
⚠️ [Dev] 当前负载率 83%，即将超载
当前任务：5 个 进行中
建议人选：QA（负载 41%）
[仍要指派]  [换推荐人选]
```

---

### 13.3 进度跟踪体验

#### 🟡 P1 — 快捷填报模板
**描述：** 工作日志输入框提供快捷模板，减少重复输入。

**模板选项：**
```
今日完成：
  · 完成 XX 模块开发
  · 联调 XX 接口
  · 修复 XX Bug
  
明日计划：
  · 继续 XX 功能开发
  · 提测 XX 模块
  · 编写 XX 文档
```

**实现：** 快捷短语存 LocalStorage，用户可自定义模板。

#### 🟡 P1 — 进度自动计算
**描述：** 可选开启「按工作日志自动计算进度」。

**公式：** `progress = 已填报天数 / 总预估工期天数 × 100%`

**开关：** 任务详情 → 高级设置 → ☑️ 自动计算进度（默认关闭）

#### 🔴 P0 — 阻塞自动升级
**描述：** 任务标记「blocked」超过 **24 小时**，自动向 PD 发送飞书通知。

**触发时机：**
- 每小时 Cron 检测 `status='blocked' AND blockedAt < NOW() - 24h`
- 发送飞书消息给项目 owner

**消息格式：**
```
⚠️ 任务阻塞超过 24 小时未解决
项目：控制台 v1.5
任务：甘特图前端开发
负责人：Dev
阻塞原因：等待 UI 设计稿
阻塞时间：2026-04-07 10:00（已 26 小时）
[查看任务] [联系负责人]
```

---

### 13.4 甘特图核心功能

#### 🟡 P1 — 多依赖类型
**描述：** 支持 FS（完成→开始）、SS（开始→开始）、FF（完成→完成）三种依赖类型。

**表结构扩展：**
```sql
ALTER TABLE task_dependencies ADD COLUMN dependency_type
  ENUM('FS','SS','FF') NOT NULL DEFAULT 'FS';
```

**依赖箭头样式：**
- FS：实线箭头（当前默认）
- SS：虚线箭头
- FF：点线箭头

#### 🟡 P1 — 任务拆分
**描述：** 右键菜单「拆分任务」，将长任务拆分为多个子任务。

**交互：**
1. 右键 → 「拆分任务」
2. 输入子任务数量（默认 2）
3. 系统生成等时长子任务，自动继承原任务的依赖和负责人
4. 拆分后原任务自动转为「父任务」（折叠显示子任务）

#### ⚪ P2 — 甘特图导出
**描述：** 支持导出 PNG / PDF / Excel（含任务明细）。

#### ⚪ P2 — 历史版本回溯
**描述：** 任务变更记录历史版本，支持一键回滚到指定版本。

---

### 13.5 任务管理功能

#### 🟡 P1 — 任务模板库
**描述：** 将常用任务保存为模板，创建新项目时一键复用。

**模板字段：** 名称 / 工期 / 优先级 / 描述（不含具体时间）

**API：**
```json
POST /api/task-templates   # 保存模板
GET  /api/task-templates   # 获取模板列表
POST /api/projects/{id}/tasks/from-template  # 从模板创建任务
```

#### 🟡 P1 — 状态流转规则
**描述：** 配置任务状态流转约束。

**规则示例：**
- blocked 状态 → 必须填写 blockedReason
- completed 状态 → 必须关联至少一条工作日志
- 已完成任务不可逆转为已阻塞

**表结构：**
```sql
CREATE TABLE status_transition_rules (
  id INT PRIMARY KEY AUTO_INCREMENT,
  from_status VARCHAR(32) NOT NULL,
  to_status VARCHAR(32) NOT NULL,
  require_fields JSON,         -- 必须填写的字段
  require_logs BOOLEAN DEFAULT FALSE,  -- 必须关联日志
  UNIQUE KEY uk_transition (from_status, to_status)
);
```

#### ⚪ P2 — 工时统计维度
**描述：** 按任务类型 / 标签 / 优先级统计工时占比。

---

### 13.6 项目 / 成员管理

#### 🟡 P1 — 项目模板
**描述：** 将项目保存为模板（含默认成员 / 任务模板 / 里程碑）。

**模板内容：** 项目名称 / 成员角色 / 任务模板列表 / 里程碑列表

#### 🟡 P1 — 离职成员处理
**描述：** 一键转移离职成员的所有任务，保留原负责人记录。

**操作流程：**
1. PD 选择离职成员 → 「批量转移任务」
2. 选择接收人 → 预览转移任务列表 → 确认
3. 原负责人标注「已离职」，操作日志保留记录

---

### 13.7 数据库与性能（技术优化）

#### 🟡 P1 — 联合索引优化
```sql
-- 任务列表查询优化
CREATE INDEX idx_project_status_assignee
  ON tasks(project_id, status, assignee_id);

-- 操作日志查询优化
CREATE INDEX idx_task_time
  ON task_history(task_id, created_at);
```

#### 🟡 P1 — Redis 缓存策略
```
缓存 Key 格式：
gantt:project:{projectId}     → 甘特图数据，TTL 10min
member:load:{memberId}        → 成员负载，TTL 5min
task:template:list            → 任务模板列表，TTL 30min

写操作时删除缓存（Cache-Aside）：
PUT /api/tasks → 删除 gantt:project:{projectId}
```

#### 🟡 P1 — WebSocket 实时同步
**描述：** 甘特图页面建立 WebSocket 连接，多人协作时实时同步变更。

**事件：**
```javascript
// 客户端
ws = new WebSocket('ws://api/task-hub/ws?token=xxx')
ws.onmessage = (e) => {
  const event = JSON.parse(e.data)
  if (event.type === 'TASK_UPDATED') refreshTask(event.taskId)
  if (event.type === 'TASK_ASSIGNED') refreshGantt()
}

// 服务端广播
ws.broadcast('TASK_UPDATED', { taskId: 'TT-001', actor: 'PD' })
```

#### ⚪ P2 — API 版本控制
**描述：** `/api/v1/projects`，后续迭代 `/api/v2`。

---

### 13.8 前端性能

#### 🟡 P1 — 虚拟滚动
**描述：** 任务 / 成员数量 > 50 时，仅渲染可视区域任务条。

**实现：** 使用 `vue-virtual-scroller` 或自研虚拟列表。

#### 🟡 P1 — 拖拽防抖
**描述：** 拖拽时 100ms 防抖，避免频繁调用保存接口。

```javascript
// 拖拽结束 100ms 后才保存
let saveTimer = null
function onDragEnd(taskId, newDate) {
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    PUT /api/tasks/{id} { startDate: newDate }
  }, 100)
}
```

#### 🟡 P1 — 横向预加载
**描述：** 滚动到当前视图边缘前，提前加载下一时间段数据。

---

### 13.9 v1.5 验收标准补充

| # | 功能 | 验收标准 | 可测试条件 |
|---|------|---------|-----------|
| G5 | 拖拽指派负责人 | 任务条拖到成员行，弹出确认，确认后 assignee 更新 | 数据库 assignee_id 正确变更 |
| G6 | 右键快捷菜单 | 右键任务条，菜单出现，操作后状态正确变更 | 菜单 5 项操作全部可执行 |
| P4 | 超载预警前置 | 指派时负载≥80%，实时弹出预警 | 预警文案含当前负载率 |
| P5 | 阻塞24h自动升级 | blocked 超过24h，PD 收到飞书通知 | 模拟时间或等待24h |
| P6 | 多依赖类型 | SS/FF 依赖箭头样式与 FS 不同，环路检测覆盖三种类型 | 创建 SS/FF 依赖，预览正确 |
| T3 | 任务模板库 | 从模板创建任务，自动填充预设字段 | 模板创建的任务包含模板内容 |
| T4 | 状态流转规则 | blocked 状态未填原因，保存被拦截 | 前端提示"请填写阻塞原因" |
| M1 | 离职成员批量转移 | 转移后任务 assignee 正确，历史记录保留 | 原负责人显示"已离职"标签 |
| T5 | 批量排程 | 选中3个任务，一键分配，结果预览→确认→保存 | 3个任务全部正确分配 |
| S3 | 进度自动计算 | 开启后，填报1天日志，进度自动变为 20%（5天工期） | 任务 progress 字段自动更新 |

