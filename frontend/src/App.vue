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
        <button class="btn btn-ghost" @click="showSmartSchedule = true">🧠 智能排程</button>
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
          <div class="sidebar-item" style="color: var(--primary)" @click="showCreateProject = true">
            + 新建项目
          </div>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-title">🎯 里程碑</div>
          <div class="sidebar-item">🎯 v1.0 发布 <span class="role-badge" style="margin-left:4px;font-size:10px;color:var(--success)">4/30</span></div>
          <div class="sidebar-item">🎯 Beta 验收 <span class="role-badge" style="margin-left:4px;font-size:10px;color:var(--text-faint)">5/15</span></div>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-title">👤 角色权限</div>
          <div style="padding:8px 10px;font-size:11px;line-height:1.6;color:var(--text-muted)">
            <div style="margin-bottom:6px"><span class="role-badge role-admin" style="font-size:9px">👑 管理员</span> 创建项目/任务</div>
            <div style="margin-bottom:6px"><span class="role-badge role-member" style="font-size:9px">👤 成员</span> 查看/更新任务</div>
            <div><span class="role-badge role-viewer" style="font-size:9px">👁️ 访客</span> 仅查看</div>
          </div>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="main">
        <GanttView  v-show="activeTab === 'gantt'" />
        <KanbanView v-show="activeTab === 'kanban'" />
        <SwimLaneView v-show="activeTab === 'swimlane'" />
        <WorkLogView v-show="activeTab === 'worklog'" />
      </main>
    </div>

    <!-- Smart Schedule Modal -->
    <div v-if="showSmartSchedule" class="modal-overlay" @click.self="showSmartSchedule = false">
      <div class="modal">
        <div class="modal-title">🧠 智能排程推荐</div>
        <p style="font-size:12px;color:var(--text-faint);margin-bottom:12px">
          基于成员负载、技能匹配度、上下文压力智能推荐最优执行人
        </p>
        <div class="log-entry" style="border-left-color:var(--primary)">
          <div style="font-size:12px;color:var(--text-faint)">甘特图前端组件开发</div>
          <div style="font-size:12px;margin-top:4px">
            推荐：<strong style="color:var(--success)">Dev</strong>
            <span style="color:var(--text-muted)">（负载 70%，技能匹配 95%）</span>
          </div>
          <div style="font-size:11px;color:var(--text-faint);margin-top:4px">得分：87.5 / 100</div>
        </div>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="showSmartSchedule = false">关闭</button>
          <button class="btn btn-primary" @click="showSmartSchedule = false">应用推荐</button>
        </div>
      </div>
    </div>

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
import { ref, computed, onMounted } from 'vue'
import { useProjectStore, useMemberStore, useAuthStore } from '@/stores'
import { projectApi } from '@/api'
import { ElMessage } from 'element-plus'
import GanttView from '@/views/GanttView.vue'
import KanbanView from '@/views/KanbanView.vue'
import SwimLaneView from '@/views/SwimLaneView.vue'
import WorkLogView from '@/views/WorkLogView.vue'
import LoginView from '@/views/LoginView.vue'

const projectStore = useProjectStore()
const memberStore = useMemberStore()
const authStore = useAuthStore()

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
  height: 48px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 8px;
  flex-shrink: 0;
  z-index: 100;
}
.header-logo {
  font-size: 14px;
  font-weight: 590;
  color: var(--text-secondary);
  white-space: nowrap;
  letter-spacing: -0.24px;
}
.version-badge {
  font-size: 10px;
  font-weight: 400;
  color: var(--text-faint);
  margin-left: 4px;
}
.header-tabs {
  display: flex;
  gap: 2px;
  margin-left: 16px;
  overflow-x: auto;
}
.header-tab {
  padding: 5px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 510;
  color: var(--text-faint);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.12s;
  border: 1px solid transparent;
  letter-spacing: -0.13px;
  background: transparent;
  font-family: inherit;
}
.header-tab:hover { color: var(--text-secondary); background: var(--border-subtle); }
.header-tab.active {
  color: var(--text-secondary);
  background: var(--border-subtle);
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
  width: 200px;
  background: var(--surface-2);
  border-right: 1px solid var(--border);
  padding: 8px;
  flex-shrink: 0;
  overflow-y: auto;
}
.sidebar-section { margin-bottom: 8px; }
.sidebar-title {
  font-size: 11px;
  font-weight: 590;
  letter-spacing: 0.36px;
  color: var(--text-faint);
  text-transform: uppercase;
  padding: 8px 8px 4px;
}
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 400;
  color: var(--text-faint);
  cursor: pointer;
  transition: all 0.12s;
}
.sidebar-item:hover { background: var(--border-subtle); color: var(--text-secondary); }
.sidebar-item.active { background: var(--primary-bg); color: var(--primary); }
.sidebar-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.dot-green { background: var(--success); }
.dot-yellow { background: var(--warning); }
.dot-red { background: var(--danger); }
.dot-gray { background: var(--text-faint); }

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
.role-admin { background: rgba(167,139,250,0.2); color: #A78BFA; }
.role-member { background: var(--primary-bg); color: var(--primary); }
.role-viewer { background: rgba(148,163,184,0.15); color: var(--text-faint); }

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
