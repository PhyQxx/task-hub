<template>
  <LoginView v-if="!authStore.isLoggedIn" />
  <div v-else class="app-layout">
    <!-- Header -->
    <header class="header">
      <div class="header-logo">📋 任务舱 <span class="version-badge">v2</span></div>

      <select class="project-select" v-model="currentProjectId" @change="onProjectChange">
        <option value="">🌐 所有项目概览</option>
        <option v-for="p in projectStore.projects" :key="p.id" :value="p.id">{{ p.name }}</option>
      </select>

      <div class="header-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="header-tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.icon }} {{ tab.label }}
        </button>
      </div>

      <div class="header-spacer"></div>

      <div class="header-actions">
        <button v-if="isAdmin" class="btn btn-ghost" @click="showSmartSchedule = true">🧠 智能排程</button>
        <button class="btn btn-primary" @click="showCreateTask = true">+ 新建任务</button>
        <el-dropdown trigger="click">
          <el-avatar :style="{ background: 'var(--primary)', fontSize: '12px', cursor: 'pointer' }" size="small">
            {{ userName.slice(0, 1) }}
          </el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>{{ userName }}</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="layout">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="sidebar-section">
          <div class="sidebar-title">快捷筛选</div>
          <div
            v-for="f in filters"
            :key="f.key"
            class="sidebar-item"
            :class="{ active: activeFilter === f.key }"
            @click="activeFilter = f.key"
          >
            {{ f.icon }} {{ f.label }}
          </div>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-title">📁 项目列表</div>
          <div
            v-for="p in projectStore.projects"
            :key="p.id"
            class="sidebar-item project-item"
            :class="{ active: currentProjectId === p.id }"
            @click="selectProject(p.id)"
          >
            <span class="sidebar-dot dot-green"></span>
            {{ p.name }}
          </div>
          <div v-if="isAdmin" class="sidebar-item" style="color: var(--primary)" @click="showCreateProject = true">
            + 新建项目
          </div>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-title">🎯 里程碑</div>
          <div class="sidebar-item">🎯 v1.0 发布 <span class="role-badge" style="margin-left:4px;font-size:10px;color:var(--success)">4/30</span></div>
          <div class="sidebar-item">🎯 Beta 验收 <span class="role-badge" style="margin-left:4px;font-size:10px;color:var(--text-faint)">5/15</span></div>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-title">👤 当前角色</div>
          <div style="padding:8px 10px;font-size:11px;line-height:1.6;color:var(--text-muted)">
            <div style="margin-bottom:6px">
              <span class="role-badge" :class="isAdmin ? 'role-admin' : 'role-member'" style="font-size:9px">
                {{ isAdmin ? '👑 管理员' : '👤 用户' }}
              </span>
              {{ isAdmin ? ' 全部操作权限' : ' 查看/更新任务' }}
            </div>
          </div>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="main">
        <GanttView  v-show="activeTab === 'gantt'" />
        <KanbanView v-show="activeTab === 'kanban'" />
        <SwimLaneView v-show="activeTab === 'swimlane'" />
        <MemberView v-show="activeTab === 'member'" />
        <MilestoneView v-show="activeTab === 'milestone'" />
        <WorkLogView v-show="activeTab === 'worklog'" />
      </main>
    </div>

    <!-- Smart Schedule Modal -->
    <SmartScheduleModal :visible="showSmartSchedule" @close="showSmartSchedule = false" />

    <!-- New Task Modal -->
    <div v-if="showCreateTask" class="modal-overlay" @click.self="showCreateTask = false">
      <div class="modal" style="width:520px">
        <div class="modal-title">+ 新建任务</div>
        <div class="modal-field">
          <label class="form-label">任务名称 *</label>
          <input class="form-input" v-model="taskForm.title" placeholder="请输入任务名称" />
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
          <div class="modal-field">
            <label class="form-label">负责人</label>
            <select class="form-input" v-model="taskForm.assigneeId">
              <option value="">未分配</option>
              <option v-for="m in memberStore.members" :key="m.id" :value="m.id">{{ m.name }}</option>
            </select>
          </div>
          <div class="modal-field">
            <label class="form-label">优先级</label>
            <select class="form-input" v-model="taskForm.priority">
              <option value="LOW">🔵 低</option>
              <option value="MEDIUM">🟡 中</option>
              <option value="HIGH">🟠 高</option>
              <option value="URGENT">🔴 紧急</option>
            </select>
          </div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px">
          <div class="modal-field">
            <label class="form-label">开始日期</label>
            <input class="form-input" type="date" v-model="taskForm.startDate" />
          </div>
          <div class="modal-field">
            <label class="form-label">工期（天）</label>
            <input class="form-input" type="number" v-model="taskForm.days" min="1" placeholder="填天数" />
          </div>
          <div class="modal-field">
            <label class="form-label">截止日期</label>
            <input class="form-input" type="date" v-model="taskForm.endDate" />
          </div>
        </div>
        <div class="modal-field">
          <label class="form-label">描述</label>
          <textarea class="form-textarea" v-model="taskForm.description" style="height:60px" placeholder="补充任务详情..."></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="showCreateTask = false">取消</button>
          <button class="btn btn-primary" @click="handleCreateTask">创建</button>
        </div>
      </div>
    </div>

    <!-- New Project Modal -->
    <div v-if="showCreateProject" class="modal-overlay" @click.self="showCreateProject = false">
      <div class="modal" style="width:420px">
        <div class="modal-title">+ 新建项目</div>
        <div class="modal-field">
          <label class="form-label">项目名称 *</label>
          <input class="form-input" v-model="projectForm.name" placeholder="请输入项目名称" />
        </div>
        <div class="modal-field">
          <label class="form-label">描述</label>
          <textarea class="form-textarea" v-model="projectForm.description" style="height:60px" placeholder="项目描述..."></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="showCreateProject = false">取消</button>
          <button class="btn btn-primary" @click="handleCreateProject">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useProjectStore, useMemberStore, useAuthStore, useTaskStore } from '@/stores'
import { projectApi } from '@/api'
import { ElMessage } from 'element-plus'
import GanttView from '@/views/GanttView.vue'
import KanbanView from '@/views/KanbanView.vue'
import SwimLaneView from '@/views/SwimLaneView.vue'
import WorkLogView from '@/views/WorkLogView.vue'
import MemberView from '@/views/MemberView.vue'
import MilestoneView from '@/views/MilestoneView.vue'
import LoginView from '@/views/LoginView.vue'
import SmartScheduleModal from '@/components/SmartScheduleModal.vue'

const projectStore = useProjectStore()
const memberStore = useMemberStore()
const authStore = useAuthStore()
const taskStore = useTaskStore()

const activeTab = ref('gantt')
const activeFilter = ref('mine')
const showSmartSchedule = ref(false)
const showCreateTask = ref(false)
const showCreateTaskModal = ref(false)
const showCreateProject = ref(false)

const tabs = [
  { key: 'gantt', label: '甘特图', icon: '📊' },
  { key: 'kanban', label: '看板', icon: '📋' },
  { key: 'swimlane', label: '泳道', icon: '🌊' },
  { key: 'member', label: '成员', icon: '👥' },
  { key: 'milestone', label: '里程碑', icon: '🎯' },
  { key: 'worklog', label: '日志', icon: '📝' },
]

const filters = [
  { key: 'mine', label: '我的任务', icon: '🐛' },
  { key: 'today', label: '今日到期', icon: '⏰' },
  { key: 'blocked', label: '已阻塞', icon: '⚠️' },
  { key: 'owner', label: '我负责的', icon: '🎯' },
]

const currentProjectId = computed({
  get: () => projectStore.currentProjectId,
  set: (v) => projectStore.selectProject(v),
})

const userName = computed(() => authStore.nickname || '用户')
const isAdmin = computed(() => authStore.role === 'admin')

const taskForm = ref({
  title: '',
  assigneeId: '',
  priority: 'MEDIUM',
  startDate: '',
  days: 5,
  endDate: '',
  description: '',
})

const projectForm = ref({ name: '', description: '' })

function selectProject(id: string) {
  projectStore.selectProject(id)
  currentProjectId.value = id
}

function onProjectChange() {
  projectStore.selectProject(currentProjectId.value)
}

async function handleCreateTask() {
  if (!taskForm.value.title) {
    ElMessage.warning('请输入任务名称')
    return
  }
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  try {
    const { taskApi: ta } = await import('@/api')
    await ta.create({
      projectId,
      title: taskForm.value.title,
      assigneeId: taskForm.value.assigneeId || undefined,
      priority: taskForm.value.priority,
      startDate: taskForm.value.startDate || undefined,
      endDate: taskForm.value.endDate || undefined,
      description: taskForm.value.description,
    })
    ElMessage.success('任务创建成功')
    showCreateTask.value = false
    taskForm.value = { title: '', assigneeId: '', priority: 'MEDIUM', startDate: '', days: 5, endDate: '', description: '' }
    // Refresh gantt/kanban
    activeTab.value = activeTab.value // trigger reload
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

async function handleCreateProject() {
  if (!projectForm.value.name) {
    ElMessage.warning('请输入项目名称')
    return
  }
  try {
    await projectApi.create({
      name: projectForm.value.name,
      description: projectForm.value.description,
    })
    ElMessage.success('项目创建成功')
    showCreateProject.value = false
    projectForm.value = { name: '', description: '' }
    await projectStore.fetchProjects()
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

function handleLogout() {
  authStore.logout()
}

onMounted(async () => {
  await authStore.fetchMe()
  if (!authStore.isLoggedIn) return
  await projectStore.fetchProjects()
  await memberStore.fetchMembers()
  // Load tasks once for the initial active tab (gantt)
  await taskStore.fetchTasks(projectStore.currentProjectId || '')
})

// Fetch tasks when switching tabs to avoid duplicate calls from hidden views
watch(activeTab, async (tab) => {
  if (!projectStore.currentProjectId) return
  if (tab === 'kanban' || tab === 'swimlane') {
    await taskStore.fetchTasks(projectStore.currentProjectId)
  }
})
</script>

<style>
:root {
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
  --font: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: var(--bg);
}

/* ── Header ── */
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

/* ── Layout ── */
.layout {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ── Sidebar ── */
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

/* ── Main Content ── */
.main {
  flex: 1;
  overflow: auto;
  min-width: 0;
}

/* Role badges */
.role-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 6px;
  font-weight: 590;
}
.role-admin { background: rgba(167,139,250,0.15); color: #7c5af5; }
.role-member { background: var(--primary-bg); color: var(--primary); }
.role-viewer { background: var(--surface-4); color: var(--text-muted); }

/* Modal fields */
.modal-field { margin-bottom: 10px; }

/* Log entry (used in smart schedule) */
.log-entry {
  background: var(--surface-4);
  border-radius: var(--radius-md);
  padding: 10px;
  margin-bottom: 8px;
  border-left: 3px solid var(--primary);
}

.project-select {
  background: var(--surface-3);
  border: 1px solid var(--border-strong);
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  padding: 5px 10px;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}
.project-select:focus { outline: none; border-color: var(--border-strong); background: var(--surface-5); }
</style>
