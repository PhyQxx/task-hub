# Linear Style UI 重构实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development to implement this plan.

**Goal:** 将任务舱前端 UI 重构为 Linear 风格，包含统计栏、改进的甘特图、成员负载视图、智能排程等核心功能。

**Architecture:** 
- 全局 CSS 变量驱动深色主题，统一的颜色/间距/圆角系统
- 组件化拆分：StatsBar、MemberCard、WorkLogPanel、SmartScheduleModal 等独立组件
- 甘特图保留 dhtmlx-gantt，但重构其周围的 UI 容器和工具栏
- 状态管理保持 Pinia，新增 memberStore 工作负载计算

**Tech Stack:** Vue3 + TypeScript + Vite + Element Plus + dhtmlx-gantt + Pinia

---

## 文件变更总览

| 文件 | 操作 | 职责 |
|------|------|------|
| `frontend/src/App.vue` | 重构 | Header、Sidebar 样式重构，添加统计栏 |
| `frontend/src/views/GanttView.vue` | 重构 | 统计栏、时间粒度切换、按人维度、区块分组 |
| `frontend/src/views/KanbanView.vue` | 重构 | 时间粒度切换、按人维度 |
| `frontend/src/views/MemberView.vue` | 重构 | 成员工作负载卡片视图 |
| `frontend/src/views/WorkLogView.vue` | 调整 | 适配新样式 |
| `frontend/src/components/StatsBar.vue` | 新建 | 顶部统计数字栏 |
| `frontend/src/components/MemberCard.vue` | 新建 | 成员工作负载卡片 |
| `frontend/src/components/WorkLogPanel.vue` | 新建 | 甘特图右侧工作日志浮层面板 |
| `frontend/src/components/SmartScheduleModal.vue` | 新建 | 智能排程推荐弹窗 |
| `frontend/src/stores/index.ts` | 修改 | 导出新增 store |

---

## Chunk 1: 全局样式变量 + App.vue 重构

### 任务 1: 创建全局 CSS 变量定义

**Files:**
- Modify: `frontend/src/App.vue` — 在 `<style>` 顶部添加 CSS 变量块

**Step 1: 添加 Linear 风格 CSS 变量**

```css
/* 在现有 var() 定义后追加，替换或增强已有变量 */
:root {
  /* Linear 风格配色 */
  --bg: #0d0d0f;
  --surface-1: #131316;
  --surface-2: #1a1a1f;
  --surface-3: #222228;
  --surface-4: #2a2a32;
  --surface-5: #32323c;
  --surface-elevated: #38383f;
  
  --border: #2e2e38;
  --border-subtle: #222228;
  --border-strong: #3a3a45;
  
  --text: #ececf1;
  --text-secondary: #a0a0b0;
  --text-faint: #5c5c6d;
  --text-muted: #6e6e80;
  
  --primary: #5B8DEF;
  --primary-hover: #4a7de0;
  --primary-bg: rgba(91, 141, 239, 0.12);
  
  --accent: #5B8DEF;
  --accent-hover: #4a7de0;
  --accent-bg: rgba(91, 141, 239, 0.12);
  
  --success: #32d583;
  --success-bg: rgba(50, 213, 131, 0.12);
  --warning: #f5a623;
  --warning-bg: rgba(245, 166, 35, 0.12);
  --danger: #ec5f5f;
  --danger-bg: rgba(236, 95, 95, 0.12);
  
  --radius-xs: 4px;
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.4);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.4);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.5);
  
  /* 字体 */
  --font: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
}
```

**Step 2: 验证**

运行 `npm run dev` 确认无语法错误。

---

### 任务 2: 重构 App.vue Header

**Files:**
- Modify: `frontend/src/App.vue:339-391` — `.header` 相关样式

**Step 1: 更新 Header 样式**

替换现有 `.header` 样式为：

```css
.header {
  height: 52px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 6px;
  flex-shrink: 0;
  z-index: 100;
}
.header-logo {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  letter-spacing: -0.24px;
}
.version-badge {
  font-size: 10px;
  font-weight: 500;
  color: var(--text-faint);
  background: var(--surface-3);
  padding: 1px 5px;
  border-radius: 4px;
  margin-left: 4px;
}
.header-tabs {
  display: flex;
  gap: 2px;
  margin-left: 20px;
  overflow-x: auto;
}
.header-tab {
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-faint);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.12s;
  border: 1px solid transparent;
  background: transparent;
  font-family: inherit;
}
.header-tab:hover { color: var(--text-secondary); background: var(--surface-3); }
.header-tab.active {
  color: var(--text);
  background: var(--surface-3);
  border-color: var(--border-strong);
}
.header-spacer { flex: 1; }
.header-actions { display: flex; gap: 8px; align-items: center; }
```

**Step 2: 更新 btn 样式（追加到 style 底部）**

```css
/* Linear 风格按钮 */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.12s;
  border: 1px solid transparent;
  white-space: nowrap;
}
.btn-primary {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}
.btn-primary:hover { background: var(--primary-hover); }
.btn-ghost {
  background: transparent;
  color: var(--text-secondary);
  border-color: var(--border-strong);
}
.btn-ghost:hover { background: var(--surface-3); color: var(--text); }
```

**Step 3: 验证**

确认 dev server 正常启动，Header 样式变化符合 Linear 风格。

---

### 任务 3: 重构 App.vue Sidebar

**Files:**
- Modify: `frontend/src/App.vue:401-434` — `.sidebar` 相关样式

**Step 1: 更新 Sidebar 样式**

替换 `.sidebar` 相关样式：

```css
.sidebar {
  width: 220px;
  background: var(--surface-2);
  border-right: 1px solid var(--border);
  padding: 0;
  flex-shrink: 0;
  overflow-y: auto;
}
.sidebar-section {
  padding: 16px 12px 8px;
  border-bottom: 1px solid var(--border-subtle);
}
.sidebar-section:last-child { border-bottom: none; }
.sidebar-title {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--text-faint);
  text-transform: uppercase;
  padding: 0 8px 8px;
}
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 400;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.12s;
}
.sidebar-item:hover { background: var(--surface-3); color: var(--text); }
.sidebar-item.active { background: var(--primary-bg); color: var(--primary); }
.sidebar-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.dot-green { background: var(--success); }
.dot-yellow { background: var(--warning); }
.dot-red { background: var(--danger); }
.dot-gray { background: var(--text-faint); }
.project-status {
  margin-left: auto;
  font-size: 10px;
  color: var(--text-faint);
}
```

**Step 2: 更新模板中的项目列表渲染，添加状态点颜色和状态文字**

在 `App.vue` 模板中，将项目列表从：
```vue
<div v-for="p in projectStore.projects" ...>{{ p.name }}</div>
```
改为（如果 project 数据中有 status 字段）：
```vue
<div v-for="p in projectStore.projects" ...>
  <span class="sidebar-dot" :class="getProjectDotClass(p.status)"></span>
  <span style="flex:1">{{ p.name }}</span>
  <span class="project-status">{{ p.status }}</span>
</div>
```

**Step 3: 验证**

Sidebar 样式更新，分组明显，状态点颜色区分。

---

## Chunk 2: StatsBar 组件 + GanttView 重构

### 任务 4: 创建 StatsBar 组件

**Files:**
- Create: `frontend/src/components/StatsBar.vue`

**Step 1: 创建 StatsBar.vue**

```vue
<template>
  <div class="stats-bar">
    <div class="stat-item">
      <span class="stat-label">总任务</span>
      <span class="stat-value">{{ total }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">本周新增</span>
      <span class="stat-value" style="color:var(--success)">+{{ weeklyNew }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">进行中</span>
      <span class="stat-value" style="color:var(--primary)">{{ inProgress }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">环比</span>
      <span class="stat-value" :style="weekOverWeek >= 0 ? 'color:var(--success)' : 'color:var(--danger)'">
        {{ weekOverWeek >= 0 ? '+' : '' }}{{ weekOverWeek }}
      </span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">已完成</span>
      <span class="stat-value" style="color:var(--success)">{{ done }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">完成率</span>
      <span class="stat-value">{{ completionRate }}%</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">已阻塞</span>
      <span class="stat-value" style="color:var(--danger)">{{ blocked }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">需关注</span>
      <span class="stat-value" style="color:var(--warning)">{{ attention }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">逾期</span>
      <span class="stat-value" style="color:var(--danger)">{{ overdue }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">本周到期</span>
      <span class="stat-value">{{ weeklyDue }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTaskStore } from '@/stores'

const taskStore = useTaskStore()

const tasks = computed(() => taskStore.tasks || [])
const total = computed(() => tasks.value.length)
const weeklyNew = computed(() => {
  const weekAgo = new Date()
  weekAgo.setDate(weekAgo.getDate() - 7)
  return tasks.value.filter(t => new Date(t.createdAt) >= weekAgo).length
})
const inProgress = computed(() => tasks.value.filter(t => t.status === 'IN_PROGRESS').length)
const done = computed(() => tasks.value.filter(t => t.status === 'DONE').length)
const blocked = computed(() => tasks.value.filter(t => t.status === 'BLOCKED').length)
const completionRate = computed(() => total.value ? Math.round((done.value / total.value) * 100) : 0)
const weekOverWeek = computed(() => 2) // TODO: 计算环比
const attention = computed(() => blocked.value)
const overdue = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  return tasks.value.filter(t => t.endDate && t.endDate < today && t.status !== 'DONE').length
})
const weeklyDue = computed(() => {
  const now = new Date()
  const endOfWeek = new Date(now)
  endOfWeek.setDate(now.getDate() + (7 - now.getDay()))
  return tasks.value.filter(t => {
    if (!t.endDate) return false
    return t.endDate >= now.toISOString().split('T')[0] && t.endDate <= endOfWeek.toISOString().split('T')[0]
  }).length
})
</script>

<style scoped>
.stats-bar {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 10px 20px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border);
  overflow-x: auto;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 0 16px;
  min-width: fit-content;
}
.stat-label {
  font-size: 11px;
  color: var(--text-faint);
  white-space: nowrap;
}
.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  line-height: 1;
}
.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border);
  flex-shrink: 0;
}
</style>
```

**Step 2: 验证**

StatsBar 渲染正常，数字计算正确。

---

### 任务 5: 重构 GanttView.vue

**Files:**
- Modify: `frontend/src/views/GanttView.vue`

**Step 1: 重构 GanttView 模板结构**

将 `<template>` 部分替换为：

```vue
<template>
  <div class="gantt-view">
    <!-- Stats Bar -->
    <StatsBar />

    <!-- Toolbar -->
    <div class="gantt-toolbar">
      <div class="toolbar-left">
        <span class="project-name">{{ projectStore.currentProject?.name || '全部项目' }}</span>
      </div>
      <div class="toolbar-right">
        <!-- 时间粒度 -->
        <div class="time-scale-group">
          <button
            v-for="scale in timeScales"
            :key="scale.key"
            class="scale-btn"
            :class="{ active: timeScale === scale.key }"
            @click="timeScale = scale.key; initGantt()"
          >{{ scale.label }}</button>
        </div>
        <div class="toolbar-sep"></div>
        <!-- 维度切换 -->
        <el-button-group>
          <el-button size="small" :type="ganttDim === 'task' ? 'primary' : 'default'" @click="ganttDim = 'task'">📋 按任务</el-button>
          <el-button size="small" :type="ganttDim === 'member' ? 'primary' : 'default'" @click="ganttDim = 'member'">👤 按人</el-button>
        </el-button-group>
        <el-button size="small" @click="initGantt">刷新</el-button>
        <el-button type="primary" size="small" @click="showCreateTask = true">+ 新建任务</el-button>
      </div>
    </div>

    <!-- 任务分类区块（待处理/进行中/已完成/已阻塞） -->
    <div class="gantt-sections">
      <div v-for="section in taskSections" :key="section.key" class="gantt-section">
        <div class="section-header" @click="section.collapsed = !section.collapsed">
          <span class="section-arrow">{{ section.collapsed ? '▶' : '▼' }}</span>
          <span class="status-dot" :class="'dot-' + section.dotClass"></span>
          <span class="section-title">{{ section.label }}</span>
          <span class="section-count">{{ section.tasks.length }}</span>
        </div>
        <div v-if="!section.collapsed" class="section-content">
          <div class="gantt-container" :ref="el => section.ganttRef = el"></div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑任务弹窗 -->
    <el-dialog v-model="showCreateTask" :title="editingTaskId ? '编辑任务' : '+ 新建任务'" width="560px" :append-to-body="true">
      <!-- 表单内容保持原有逻辑不变 -->
    </el-dialog>
  </div>
</template>
```

**Step 2: 添加 GanttView script setup 逻辑**

在 `<script setup>` 中：

```typescript
import StatsBar from '@/components/StatsBar.vue'

const timeScale = ref<'day' | 'week' | 'month' | 'quarter'>('week')
const ganttDim = ref<'task' | 'member'>('task')

const timeScales = [
  { key: 'day', label: '今天' },
  { key: 'week', label: '周' },
  { key: 'month', label: '月' },
  { key: 'quarter', label: '季' },
]

const taskSections = computed(() => {
  const tasks = ganttStore.ganttData.tasks || []
  return [
    { key: 'TODO', label: '待处理', dotClass: 'pending', collapsed: false, tasks: tasks.filter(t => t.status === 'TODO'), ganttRef: null },
    { key: 'IN_PROGRESS', label: '进行中', dotClass: 'progress', collapsed: false, tasks: tasks.filter(t => t.status === 'IN_PROGRESS'), ganttRef: null },
    { key: 'DONE', label: '已完成', dotClass: 'done', collapsed: false, tasks: tasks.filter(t => t.status === 'DONE'), ganttRef: null },
    { key: 'BLOCKED', label: '已阻塞', dotClass: 'blocked', collapsed: false, tasks: tasks.filter(t => t.status === 'BLOCKED'), ganttRef: null },
  ]
})
```

**Step 3: 更新样式**

替换 `.gantt-view` 相关样式：

```css
.gantt-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg);
}
.gantt-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border);
  gap: 12px;
}
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }
.project-name { font-size: 15px; font-weight: 600; color: var(--text); }
.toolbar-sep { width: 1px; height: 20px; background: var(--border); }
.time-scale-group { display: flex; gap: 2px; background: var(--surface-3); padding: 3px; border-radius: var(--radius-sm); }
.scale-btn {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-faint);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.12s;
}
.scale-btn:hover { color: var(--text-secondary); }
.scale-btn.active { background: var(--surface-5); color: var(--text); }
.gantt-sections { flex: 1; overflow: auto; }
.gantt-section { border-bottom: 1px solid var(--border-subtle); }
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  cursor: pointer;
  background: var(--surface-1);
  user-select: none;
}
.section-header:hover { background: var(--surface-2); }
.section-arrow { font-size: 10px; color: var(--text-faint); }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.dot-pending { background: var(--text-faint); }
.dot-progress { background: var(--primary); }
.dot-done { background: var(--success); }
.dot-blocked { background: var(--danger); }
.section-title { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.section-count { font-size: 11px; color: var(--text-faint); background: var(--surface-3); padding: 2px 8px; border-radius: 10px; }
.section-content { padding: 0; }
.gantt-container { height: 100%; min-height: 200px; }
```

**Step 4: 验证**

`npm run dev` 确认 GanttView 编译通过，StatsBar 和工具栏显示正确。

---

## Chunk 3: MemberCard 组件 + MemberView 重构

### 任务 6: 创建 MemberCard 组件

**Files:**
- Create: `frontend/src/components/MemberCard.vue`

**Step 1: 创建 MemberCard.vue**

```vue
<template>
  <div class="member-card">
    <div class="card-header">
      <div class="member-avatar" :style="{ background: member.color }">
        {{ member.name.slice(0, 1) }}
      </div>
      <div class="member-info">
        <div class="member-name">{{ member.name }}</div>
        <div class="member-role">{{ member.role }}</div>
      </div>
      <div class="member-status" :class="'status-' + member.status">
        {{ statusLabel(member.status) }}
      </div>
    </div>
    <div class="card-stats">
      <div class="card-stat">
        <span class="card-stat-value">{{ member.inProgressCount }}</span>
        <span class="card-stat-label">进行中</span>
      </div>
      <div class="card-stat">
        <span class="card-stat-value">{{ member.weeklyDoneCount }}</span>
        <span class="card-stat-label">本周完成</span>
      </div>
      <div class="card-stat">
        <span class="card-stat-value" :class="loadClass(member.load)">{{ member.load }}%</span>
        <span class="card-stat-label">工作负载</span>
      </div>
    </div>
    <div class="load-bar">
      <div class="load-bar-fill" :class="loadClass(member.load)" :style="{ width: member.load + '%' }"></div>
    </div>
    <div v-if="member.load >= 80" class="load-warning">
      ⚠ 超载预警
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Member {
  id: string
  name: string
  role: string
  status: 'idle' | 'working' | 'overloaded'
  inProgressCount: number
  weeklyDoneCount: number
  load: number
  color: string
}

const props = defineProps<{ member: Member }>()

function statusLabel(status: string) {
  return { idle: '空闲', working: '工作中', overloaded: '超载' }[status] || '未知'
}
function loadClass(load: number) {
  if (load >= 90) return 'load-critical'
  if (load >= 80) return 'load-warning'
  return 'load-normal'
}
</script>

<style scoped>
.member-card {
  background: var(--surface-3);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-lg);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card-header { display: flex; align-items: center; gap: 12px; }
.member-avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; color: #fff;
  flex-shrink: 0;
}
.member-info { flex: 1; }
.member-name { font-size: 14px; font-weight: 600; color: var(--text); }
.member-role { font-size: 12px; color: var(--text-faint); margin-top: 2px; }
.member-status {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.status-idle { background: var(--surface-4); color: var(--text-faint); }
.status-working { background: var(--success-bg); color: var(--success); }
.status-overloaded { background: var(--danger-bg); color: var(--danger); }
.card-stats { display: flex; gap: 16px; }
.card-stat { display: flex; flex-direction: column; gap: 2px; }
.card-stat-value { font-size: 18px; font-weight: 700; color: var(--text); }
.card-stat-label { font-size: 11px; color: var(--text-faint); }
.load-bar { height: 4px; background: var(--surface-5); border-radius: 2px; overflow: hidden; }
.load-bar-fill { height: 100%; border-radius: 2px; transition: width 0.3s; }
.load-normal { background: var(--success); }
.load-warning { background: var(--warning); }
.load-critical { background: var(--danger); }
.load-warning { color: var(--warning); font-size: 11px; }
</style>
```

---

### 任务 7: 重构 MemberView.vue

**Files:**
- Modify: `frontend/src/views/MemberView.vue`

**Step 1: 更新 MemberView 模板**

替换 `<template>` 部分：

```vue
<template>
  <div class="member-view">
    <div class="view-header">
      <h2 class="view-title">👥 成员负载</h2>
    </div>
    <div class="member-grid">
      <MemberCard
        v-for="m in enrichedMembers"
        :key="m.id"
        :member="m"
      />
    </div>
  </div>
</template>
```

**Step 2: 更新 script setup**

```typescript
import MemberCard from '@/components/MemberCard.vue'

const memberColors: Record<string, string> = {
  'Dev': '#34D399',
  'Des': '#F472B6',
  'QA': '#FBBF24',
  'Ops': '#60A5FA',
  'PD': '#818CF8',
}

const enrichedMembers = computed(() => {
  return memberStore.members.map(m => {
    const memberTasks = taskStore.tasks.filter(t => t.assigneeId === m.id)
    const inProgressCount = memberTasks.filter(t => t.status === 'IN_PROGRESS').length
    const weeklyDoneCount = memberTasks.filter(t => t.status === 'DONE').length
    const totalCount = memberTasks.length
    const load = Math.min(100, Math.round((inProgressCount / 5) * 100))
    const status = load >= 90 ? 'overloaded' : load >= 50 ? 'working' : 'idle'
    return {
      ...m,
      role: m.role || '未知',
      inProgressCount,
      weeklyDoneCount,
      load,
      status,
      color: memberColors[m.role] || '#8b5cf6',
    }
  })
})
```

**Step 3: 更新样式**

```css
.member-view {
  padding: 24px;
  background: var(--bg);
  min-height: 100%;
}
.view-header {
  margin-bottom: 20px;
}
.view-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}
.member-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
```

**Step 4: 验证**

`npm run dev` 确认 MemberView 正常显示成员卡片。

---

## Chunk 4: 智能排程弹窗 + 工作日志面板

### 任务 8: 创建 SmartScheduleModal 组件

**Files:**
- Create: `frontend/src/components/SmartScheduleModal.vue`

**Step 1: 创建 SmartScheduleModal.vue**

```vue
<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal smart-schedule-modal">
      <div class="modal-header">
        <span class="modal-icon">🧠</span>
        <span class="modal-title">智能排程推荐</span>
        <button class="modal-close" @click="$emit('close')">×</button>
      </div>
      <p class="modal-desc">
        基于成员负载、技能匹配度、上下文压力智能推荐最优执行人
      </p>
      <div class="schedule-list">
        <div v-for="item in recommendations" :key="item.taskId" class="schedule-item">
          <div class="schedule-task">{{ item.taskName }}</div>
          <div class="schedule-recommend">
            推荐执行人：<strong>{{ item.assignee }}</strong>
            <span class="schedule-load">({{ item.loadDesc }})</span>
          </div>
          <div class="schedule-score">
            得分：<strong>{{ item.score }}</strong>
            <span v-if="item.flag" class="schedule-flag">{{ item.flag }}</span>
          </div>
        </div>
      </div>
      <div class="modal-actions">
        <button class="btn btn-ghost" @click="$emit('close')">关闭</button>
        <button class="btn btn-primary" @click="applyAll">应用推荐</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ visible: boolean }>()
defineEmits(['close'])

const recommendations = [
  { taskId: '1', taskName: '甘特图前端组件开发', assignee: 'Dev', loadDesc: '负载 70%，技能匹配 95%', score: '87.5 / 100', flag: '' },
  { taskId: '2', taskName: '智能排程推荐算法', assignee: 'Dev', loadDesc: '当前负载过高，建议拆分任务', score: '62.0 / 100', flag: '⚠ 风险提示' },
  { taskId: '3', taskName: '数据库建表 SQL', assignee: 'Ops', loadDesc: '负载 28%，技能完全匹配', score: '94.0 / 100', flag: '✅ 最优' },
]

function applyAll() {
  // TODO: 调用 API 应用推荐
}
</script>

<style scoped>
.smart-schedule-modal { width: 480px; }
.modal-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.modal-icon { font-size: 18px; }
.modal-title { font-size: 15px; font-weight: 700; color: var(--text); flex: 1; }
.modal-close { background: none; border: none; font-size: 20px; color: var(--text-faint); cursor: pointer; }
.modal-desc { font-size: 12px; color: var(--text-faint); margin-bottom: 16px; }
.schedule-list { display: flex; flex-direction: column; gap: 10px; }
.schedule-item {
  background: var(--surface-4);
  border-radius: var(--radius-md);
  padding: 12px;
  border-left: 3px solid var(--primary);
}
.schedule-task { font-size: 13px; font-weight: 500; color: var(--text-secondary); margin-bottom: 4px; }
.schedule-recommend { font-size: 12px; color: var(--text-faint); margin-bottom: 2px; }
.schedule-score { font-size: 12px; color: var(--text-faint); }
.schedule-load { margin-left: 4px; }
.schedule-flag { margin-left: 8px; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
</style>
```

**Step 2: 在 App.vue 中集成 SmartScheduleModal**

在 `App.vue` 模板中：

```vue
<SmartScheduleModal :visible="showSmartSchedule" @close="showSmartSchedule = false" />
```

并添加 import：
```typescript
import SmartScheduleModal from '@/components/SmartScheduleModal.vue'
```

---

### 任务 9: 更新 App.vue 全局弹窗样式（追加到 style）

**Step 1: 追加全局 modal 样式**

```css
/* 全局 Modal 样式 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}
.modal {
  background: var(--surface-2);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-xl);
  padding: 20px;
  box-shadow: var(--shadow-lg);
  max-height: 90vh;
  overflow-y: auto;
}
.modal-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 12px;
}
.modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 16px;
}
```

**Step 2: 验证**

SmartScheduleModal 在点击"智能排程"按钮后正常弹出，样式符合 Linear 风格。

---

## Chunk 5: KanbanView 优化 + 细节打磨

### 任务 10: 重构 KanbanView.vue 时间粒度和维度

**Files:**
- Modify: `frontend/src/views/KanbanView.vue`

**Step 1: 更新模板中的工具栏**

替换 `.toolbar` 部分：

```html
<div class="toolbar">
  <div class="time-scale-group">
    <button v-for="scale in timeScales" :key="scale.key" class="scale-btn" :class="{ active: timeScale === scale.key }" @click="timeScale = scale.key">{{ scale.label }}</button>
  </div>
  <div class="toolbar-sep"></div>
  <el-button-group>
    <el-button size="small" :type="kanbanDim === 'task' ? 'primary' : 'default'" @click="kanbanDim = 'task'">📋 按任务</el-button>
    <el-button size="small" :type="kanbanDim === 'member' ? 'primary' : 'default'" @click="kanbanDim = 'member'">👤 按人</el-button>
  </el-button-group>
  <span class="toolbar-info">{{ projectStore.currentProject?.name || '所有项目' }} · {{ currentPeriodLabel }}</span>
</div>
```

**Step 2: 添加 script setup 变量**

```typescript
const timeScale = ref<'today' | 'week' | 'month' | 'quarter'>('month')
const kanbanDim = ref<'task' | 'member'>('task')
const timeScales = [
  { key: 'today', label: '今天' },
  { key: 'week', label: '周' },
  { key: 'month', label: '月' },
  { key: 'quarter', label: '季' },
]
const currentPeriodLabel = computed(() => {
  const now = new Date()
  return `${now.getMonth() + 1}月`
})
```

**Step 3: 按人维度时动态切换列**

更新 `kanban-board` 部分，当 `kanbanDim === 'member'` 时，列切换为成员名单。

**Step 4: 更新样式**

追加：
```css
.time-scale-group { display: flex; gap: 2px; background: var(--surface-3); padding: 3px; border-radius: var(--radius-sm); }
.scale-btn { padding: 4px 12px; border-radius: 4px; font-size: 12px; font-weight: 500; color: var(--text-faint); background: transparent; border: none; cursor: pointer; transition: all 0.12s; }
.scale-btn:hover { color: var(--text-secondary); }
.scale-btn.active { background: var(--surface-5); color: var(--text); }
.toolbar-sep { width: 1px; height: 20px; background: var(--border); }
```

---

## 最终验证

每个 Chunk 完成后执行：

1. `npm run dev` — 确认无编译错误
2. 浏览器访问 http://localhost:5173 — 确认 UI 渲染正常
3. `lsp_diagnostics` on modified files — 确认无新增 TypeScript/Lint 错误

---

**Plan saved to:** `docs/superpowers/plans/2026-04-09-linear-ui-redesign.md`
