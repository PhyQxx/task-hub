<template>
  <LoginView v-if="!authStore.isLoggedIn" />
  <div v-else class="app-layout">
    <!-- Header -->
    <header class="header">
      <div class="header-logo">
        <span class="logo-icon">📋</span>
        <span class="logo-text">任务舱</span>
        <span class="version-badge">v2.0</span>
      </div>

      <div class="header-project-switcher">
        <select class="project-select" v-model="currentProjectId" @change="onProjectChange">
          <option value="">🌐 所有项目概览</option>
          <option v-for="p in projectStore.projects" :key="p.projectId" :value="p.projectId">{{ p.name }}</option>
        </select>
      </div>

      <nav class="header-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="header-tab"
          :class="{ active: activeTab === tab.key }"
          @click="$router.push('/' + tab.key)"
        >
          <span class="tab-icon">{{ tab.icon }}</span>
          <span class="tab-label">{{ tab.label }}</span>
        </button>
      </nav>

      <div class="header-spacer"></div>

      <div class="header-actions">
        <NotificationBell />
        <button class="btn btn-ghost theme-toggle" @click="uiStore.toggleTheme" :title="uiStore.theme === 'dark' ? '切换到明亮模式' : '切换到暗黑模式'">
          <span v-if="uiStore.theme === 'dark'">☀️</span>
          <span v-else>🌙</span>
        </button>
        <el-dropdown trigger="click">
          <div class="user-avatar-wrap">
            <el-avatar class="user-avatar" :size="28">
              {{ userName.slice(0, 1) }}
            </el-avatar>
            <span class="user-name">{{ userName }}</span>
          </div>
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
        <div class="sidebar-scroll">
          <div class="sidebar-section">
            <div class="sidebar-title">快捷筛选</div>
            <div
              v-for="f in filters"
              :key="f.key"
              class="sidebar-item"
              :class="{ active: activeFilter === f.key }"
              @click="setFilter(f.key)"
            >
              <span class="item-icon">{{ f.icon }}</span>
              <span class="item-label">{{ f.label }}</span>
            </div>
          </div>

          <div class="sidebar-section">
            <div class="sidebar-title">📁 项目列表</div>
            <div
              v-for="p in projectStore.projects"
              :key="p.projectId"
              class="sidebar-item project-item"
              :class="{ active: currentProjectId === p.projectId }"
              @click="selectProject(p.projectId)"
            >
              <span class="sidebar-dot dot-green"></span>
              <span class="item-label project-name-text">{{ p.name }}</span>
              <span v-if="isAdmin" class="project-item-actions" @click.stop="openEditProject(p)">✏️</span>
            </div>
            <div v-if="isAdmin" class="sidebar-item add-project-btn" @click="openCreateProject">
              <span class="item-icon">+</span>
              <span class="item-label">新建项目</span>
            </div>
          </div>

          <div class="sidebar-section">
            <div class="sidebar-title">👤 当前角色</div>
            <div class="role-info">
              <div class="role-row">
                <span class="role-badge" :class="isAdmin ? 'role-admin' : 'role-member'">
                  {{ isAdmin ? '👑 管理员' : '👤 用户' }}
                </span>
              </div>
              <div v-if="currentProjectId && authStore.projectRole" class="role-row">
                <span class="role-badge" :class="'role-' + authStore.projectRole">
                  {{ projectRoleLabel }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="main">
        <router-view />
      </main>
    </div>

    <!-- Smart Schedule Modal -->
    <SmartScheduleModal :visible="showSmartSchedule" @close="showSmartSchedule = false" />

    <!-- Global Components -->
    <CommandPalette />
    <TaskDetailDrawer 
      v-model:visible="showDrawer" 
      :task-id="selectedTaskId" 
      @updated="onTaskUpdated" 
    />

    <!-- Modals -->
    <!-- New Task Modal -->
    <div v-if="showCreateTask" class="modal-overlay" @click.self="showCreateTask = false">
      <div class="modal" style="width:520px">
        <div class="modal-title">+ 新建任务</div>
        <div class="modal-field">
          <label class="form-label">任务名称 *</label>
          <input class="form-input" v-model="taskForm.title" placeholder="请输入任务名称" />
        </div>
        <div class="form-row">
          <div class="modal-field">
            <label class="form-label">负责人</label>
            <select class="form-input" v-model="taskForm.assigneeId">
              <option value="">未分配</option>
              <option v-for="m in memberStore.members" :key="m.memberId" :value="m.memberId">{{ m.nickname }}</option>
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
        <div class="form-row-3">
          <div class="modal-field">
            <label class="form-label">开始日期</label>
            <input class="form-input" type="date" v-model="taskForm.startDate" />
          </div>
          <div class="modal-field">
            <label class="form-label">工期（天）</label>
            <input class="form-input" type="number" v-model="taskForm.days" min="1" />
          </div>
          <div class="modal-field">
            <label class="form-label">截止日期</label>
            <input class="form-input" type="date" v-model="taskForm.endDate" />
          </div>
        </div>
        <div class="modal-field">
          <label class="form-label">描述</label>
          <textarea class="form-textarea" v-model="taskForm.description" style="height:80px" placeholder="补充任务详情..."></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="showCreateTask = false">取消</button>
          <button class="btn btn-primary" @click="handleCreateTask">创建任务</button>
        </div>
      </div>
    </div>

    <!-- New/Edit Project Modal -->
    <div v-if="showCreateProject" class="modal-overlay" @click.self="closeProjectModal">
      <div class="modal" style="width:420px">
        <div class="modal-title">{{ editingProject ? '✏️ 编辑项目' : '+ 新建项目' }}</div>
        <div class="modal-field">
          <label class="form-label">项目名称 *</label>
          <input class="form-input" v-model="projectForm.name" placeholder="请输入项目名称" />
        </div>
        <div class="modal-field">
          <label class="form-label">描述</label>
          <textarea class="form-textarea" v-model="projectForm.description" style="height:80px" placeholder="项目描述..."></textarea>
        </div>
        <div class="modal-actions">
          <button v-if="editingProject && isAdmin" class="btn btn-danger" style="margin-right:auto" @click="handleDeleteProject">删除项目</button>
          <button class="btn btn-ghost" @click="closeProjectModal">取消</button>
          <button class="btn btn-primary" @click="editingProject ? handleUpdateProject() : handleCreateProject()">
            {{ editingProject ? '保存修改' : '立即创建' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useProjectStore, useMemberStore, useAuthStore, useTaskStore, useGanttStore, useUIStore } from '@/stores'
import { projectApi, permissionApi } from '@/api'
import { ElMessage } from 'element-plus'
import { useWebSocket } from '@/composables/useWebSocket'
import LoginView from '@/views/LoginView.vue'
import SmartScheduleModal from '@/components/SmartScheduleModal.vue'
import NotificationBell from '@/components/NotificationBell.vue'
import CommandPalette from '@/components/CommandPalette.vue'
import TaskDetailDrawer from '@/components/TaskDetailDrawer.vue'

const router = useRouter()
const route = useRoute()
const projectStore = useProjectStore()
const memberStore = useMemberStore()
const authStore = useAuthStore()
const taskStore = useTaskStore()
const ganttStore = useGanttStore()
const uiStore = useUIStore()
const { connect: wsConnect, disconnect: wsDisconnect } = useWebSocket()

// Global UI State
const showDrawer = ref(false)
const selectedTaskId = ref<string | null>(null)

const activeTab = computed(() => {
  const name = route.name as string
  return name || 'gantt'
})
const activeFilter = ref('')
const showSmartSchedule = ref(false)
const showCreateTask = ref(false)
const showCreateProject = ref(false)
const editingProject = ref<any>(null)

function onTaskUpdated() {
  if (projectStore.currentProjectId) {
    taskStore.fetchTasks(projectStore.currentProjectId)
  }
}

function handleGlobalEvents(e: any) {
  if (e.type === 'cmd-new-task') {
    showCreateTask.value = true
  } else if (e.type === 'cmd-open-task') {
    selectedTaskId.value = e.detail.taskId
    showDrawer.value = true
  }
}

const tabs = [
  { key: 'gantt', label: '甘特图', icon: '📊' },
  { key: 'kanban', label: '看板', icon: '📋' },
  { key: 'swimlane', label: '泳道', icon: '🌊' },
  { key: 'member', label: '成员', icon: '👥' },
  { key: 'milestone', label: '里程碑', icon: '🏁' },
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
const projectRoleLabel = computed(() => {
  const r = authStore.projectRole
  if (r === 'owner') return '👑 项目负责人'
  if (r === 'member') return '✏️ 成员'
  if (r === 'viewer') return '👀 观察者'
  return ''
})

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

function setFilter(key: string) {
  if (activeFilter.value === key) {
    activeFilter.value = ''
    ganttStore.setTaskFilter('')
  } else {
    activeFilter.value = key
    ganttStore.setTaskFilter(key)
  }
}

async function fetchProjectRole(projectId: string) {
  if (!projectId) {
    authStore.setProjectRole('')
    return
  }
  try {
    const res = await permissionApi.getMyRole(projectId)
    if (res.code === 0) {
      authStore.setProjectRole(res.data || '')
    }
  } catch {
    authStore.setProjectRole('')
  }
}

function selectProject(id: string) {
  projectStore.selectProject(id)
  currentProjectId.value = id
  fetchProjectRole(id)
}

function onProjectChange() {
  projectStore.selectProject(currentProjectId.value)
  fetchProjectRole(currentProjectId.value)
}

function openCreateProject() {
  editingProject.value = null
  projectForm.value = { name: '', description: '' }
  showCreateProject.value = true
}

function openEditProject(p: any) {
  editingProject.value = p
  projectForm.value = { name: p.name || '', description: p.description || '' }
  showCreateProject.value = true
}

function closeProjectModal() {
  showCreateProject.value = false
  editingProject.value = null
  projectForm.value = { name: '', description: '' }
}

async function handleUpdateProject() {
  if (!editingProject.value || !projectForm.value.name) return
  try {
    await projectStore.updateProject(editingProject.value.projectId, {
      name: projectForm.value.name,
      description: projectForm.value.description,
    })
    await projectStore.fetchProjects()
    ElMessage.success('项目已更新')
    closeProjectModal()
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

async function handleDeleteProject() {
  if (!editingProject.value) return
  if (!confirm(`确认删除项目「${editingProject.value.name}」？`)) return
  try {
    await projectStore.deleteProject(editingProject.value.projectId)
    ElMessage.success('已删除')
    closeProjectModal()
  } catch (e) {
    ElMessage.error('删除失败')
  }
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
    if (projectStore.currentProjectId) {
      await taskStore.fetchTasks(projectStore.currentProjectId)
    }
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
  uiStore.applyTheme()
  await authStore.fetchMe()
  if (!authStore.isLoggedIn) return
  await projectStore.fetchProjects()
  await memberStore.fetchMembers()
  await taskStore.fetchTasks(projectStore.currentProjectId || '')
  if (projectStore.currentProjectId) {
    fetchProjectRole(projectStore.currentProjectId)
  }
  wsConnect()

  window.addEventListener('cmd-new-task', handleGlobalEvents)
  window.addEventListener('cmd-open-task', handleGlobalEvents)
})

onBeforeUnmount(() => {
  wsDisconnect()
  window.removeEventListener('cmd-new-task', handleGlobalEvents)
  window.removeEventListener('cmd-open-task', handleGlobalEvents)
})

watch(() => route.name, async (name) => {
  if (!projectStore.currentProjectId) return
  if (name === 'kanban' || name === 'swimlane') {
    await taskStore.fetchTasks(projectStore.currentProjectId)
  }
})
</script>

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
  height: 56px;
  background: var(--surface-1);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 24px;
  flex-shrink: 0;
  z-index: 100;
}

.header-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.logo-icon { font-size: 18px; }
.logo-text { font-size: 15px; font-weight: 700; color: var(--text); letter-spacing: -0.2px; }
.version-badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-faint);
  background: var(--surface-3);
  padding: 1px 6px;
  border-radius: 4px;
}

.header-project-switcher {
  min-width: 180px;
}

.project-select {
  background: var(--surface-2);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  padding: 6px 12px;
  font-size: 13px;
  width: 100%;
  cursor: pointer;
  transition: all 0.15s;
}

.project-select:hover { border-color: var(--border-strong); color: var(--text); }
.project-select:focus { outline: none; border-color: var(--primary); }

.header-tabs {
  display: flex;
  gap: 4px;
  height: 100%;
  align-items: center;
}

.header-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.12s;
  background: transparent;
  border: none;
}

.header-tab:hover { background: var(--surface-2); color: var(--text); }
.header-tab.active { background: var(--surface-3); color: var(--text); }

.tab-icon { font-size: 14px; opacity: 0.8; }

.header-spacer { flex: 1; }

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.user-avatar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.12s;
}

.user-avatar-wrap:hover { background: var(--surface-2); }

.user-avatar {
  background: var(--primary-bg) !important;
  color: var(--primary) !important;
  font-weight: 700;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

/* ── Layout & Sidebar ── */
.layout {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  background: var(--surface-1);
  border-right: 1px solid var(--border);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.sidebar-section {
  margin-bottom: 24px;
}

.sidebar-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.6px;
  color: var(--text-faint);
  text-transform: uppercase;
  padding: 0 10px 10px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.12s;
  margin-bottom: 2px;
}

.sidebar-item:hover { background: var(--surface-2); color: var(--text); }
.sidebar-item.active { background: var(--primary-bg); color: var(--primary); }

.item-icon { font-size: 14px; width: 16px; text-align: center; }
.item-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.sidebar-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.dot-green { background: var(--success); }

.project-item:hover .project-item-actions { opacity: 1; }
.project-item-actions {
  opacity: 0;
  font-size: 11px;
  padding: 2px;
  border-radius: 4px;
  transition: all 0.12s;
}
.project-item-actions:hover { background: var(--surface-4); }

.add-project-btn { color: var(--primary); font-weight: 600; }
.add-project-btn:hover { background: var(--primary-bg); }

.role-info { padding: 4px 10px; }
.role-row { margin-bottom: 8px; }

.role-badge {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
}
.role-admin { background: rgba(167,139,250,0.15); color: #a78bfa; }
.role-member { background: var(--primary-bg); color: var(--primary); }
.role-owner { background: rgba(251,191,36,0.15); color: #fbbf24; }
.role-viewer { background: var(--surface-3); color: var(--text-faint); }

/* ── Main ── */
.main {
  flex: 1;
  overflow: hidden;
  min-width: 0;
  background: var(--bg);
}

/* ── Forms ── */
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 12px; }
.form-row-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; margin-bottom: 12px; }
.modal-field { margin-bottom: 16px; }

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
</style>
