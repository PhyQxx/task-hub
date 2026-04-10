<template>
  <div class="member-view">
    <div class="view-header">
      <span class="view-title">👥 成员视图</span>
      <span class="view-subtitle">团队工作负载一览</span>
      <!-- 关键字搜索 -->
      <div class="keyword-search">
        <el-input
          v-model="keyword"
          placeholder="搜索成员姓名 / 手机号"
          clearable
          size="small"
          style="width:220px"
          prefix-icon="Search"
        />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- 角色说明 -->
    <div v-if="memberStore?.roles?.length" class="role-desc-section">
      <div class="role-desc-title">角色说明</div>
      <div class="role-desc-grid">
        <div v-for="role in (memberStore?.roles || [])" :key="role" class="role-desc-item">
          <span class="role-badge" :class="'role-' + roleKey(role)">{{ roleLabel(role) }}</span>
          <span class="role-desc-text">{{ roleDesc(role) }}</span>
        </div>
      </div>
    </div>

    <!-- 成员列表（始终展示，不依赖 roles） -->
    <div class="member-list">
      <div
        v-for="member in filteredMembers"
        :key="member.memberId"
        class="member-card"
      >
        <div class="member-header">
          <div class="member-avatar" :style="{ background: getAvatarColor(member.nickname) }">
            {{ member.nickname?.slice(0, 1) || '?' }}
          </div>
          <div class="member-info">
            <div class="member-name">{{ member.nickname }}</div>
            <div class="member-role">
              <span :class="['role-badge', 'role-' + (member.role || 'member').toLowerCase()]">
                {{ member.role || '成员' }}
              </span>
              <span class="member-phone">{{ member.phone || '—' }}</span>
            </div>
          </div>
          <div class="member-capacity">
            <span class="capacity-label">周容量</span>
            <span class="capacity-value">{{ member.weeklyCapacity || 40 }}h</span>
          </div>
        </div>

        <!-- 工作负载进度 -->
        <div class="workload-section">
          <div class="workload-header">
            <span class="workload-title">本周工作负载</span>
            <span :class="['load-indicator', 'load-' + getWeekLoadLevel(member.loadData)]">
              {{ getLoadLabel(member.loadData) }}
            </span>
          </div>
          <div class="progress-bar">
            <div
              class="progress-bar-fill"
              :style="{
                width: Math.min(100, getWeekLoad(member.loadData)) + '%',
                background: getLoadColor(member.loadData)
              }"
            ></div>
          </div>
          <div class="workload-stats">
            <span>{{ getWeekTasks(member.loadData) }} 个任务</span>
            <span>{{ getWeekLoad(member.loadData) }}%</span>
          </div>
        </div>

        <!-- 多周趋势 -->
        <div v-if="member.loadData?.weeks?.length" class="trend-section">
          <div class="trend-title">负载趋势（近4周）</div>
          <div class="trend-bars">
            <div
              v-for="week in member.loadData.weeks.slice(0, 4)"
              :key="week.week"
              class="trend-bar-item"
            >
              <div class="trend-bar-wrap">
                <div
                  class="trend-bar-fill"
                  :style="{
                    height: Math.min(100, week.loadPercent) + '%',
                    background: getLoadColor({ loadPercent: week.loadPercent })
                  }"
                ></div>
              </div>
              <div class="trend-week-label">{{ week.week }}</div>
            </div>
          </div>
        </div>

        <!-- 任务列表 -->
        <div class="task-list-section">
          <div class="task-list-header">
            <span class="task-list-title">任务列表</span>
            <span class="task-count-badge">{{ member.tasks?.length || 0 }}</span>
          </div>
          <div v-if="member.tasksLoading" class="task-loading">加载中...</div>
          <div v-else-if="!member.tasks?.length" class="task-empty">暂无任务</div>
          <div v-else class="task-items">
            <div v-for="task in member.tasks" :key="task.id" class="task-item">
              <span class="task-status-dot" :class="'status-' + (task.status || '').toLowerCase()"></span>
              <span class="task-item-title">{{ task.title }}</span>
              <span class="task-item-id">{{ task.taskId || '' }}</span>
              <span :class="['task-priority-badge', 'priority-' + ((task.priority || 'P2').toUpperCase())]">
                {{ task.priority || 'P2' }}
              </span>
              <span class="task-progress-text">{{ task.progress || 0 }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredMembers.length === 0" class="empty-state">
        <div class="empty-icon">👥</div>
        <div class="empty-text">{{ keyword ? '未找到匹配的成员' : '暂无成员数据' }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { memberApi } from '@/api'
import { useProjectStore, useMemberStore } from '@/stores'

const projectStore = useProjectStore()
const memberStore = useMemberStore()
const members = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')

// 关键字搜索（姓名或手机号）
const filteredMembers = computed(() => {
  if (!keyword.value.trim()) return members.value
  const kw = keyword.value.trim().toLowerCase()
  return members.value.filter(m =>
    (m.nickname || '').toLowerCase().includes(kw) ||
    (m.phone || '').toLowerCase().includes(kw)
  )
})

interface LoadData {
  weeks?: { week: string; loadPercent: number; taskCount: number; loadLevel: string }[]
  loadPercent?: number
  taskCount?: number
  loadLevel?: string
}

function getWeekLoad(loadData?: LoadData): number {
  if (!loadData) return 0
  if (loadData.weeks?.length) return loadData.weeks[0].loadPercent
  return loadData.loadPercent || 0
}

function getWeekTasks(loadData?: LoadData): number {
  if (!loadData) return 0
  if (loadData.weeks?.length) return loadData.weeks[0].taskCount
  return loadData.taskCount || 0
}

function getWeekLoadLevel(loadData?: LoadData): string {
  if (!loadData) return 'normal'
  if (loadData.weeks?.length) return loadData.weeks[0].loadLevel || 'normal'
  return loadData.loadLevel || 'normal'
}

function getLoadLabel(loadData?: LoadData): string {
  const level = getWeekLoadLevel(loadData)
  const map: Record<string, string> = { idle: '空闲', normal: '正常', busy: '繁忙', overloaded: '过载' }
  return map[level] || '正常'
}

function getLoadColor(loadData?: LoadData): string {
  const level = getWeekLoadLevel(loadData)
  const map: Record<string, string> = {
    idle: 'var(--info, #64b5f6)',
    normal: 'var(--success, #10b981)',
    busy: 'var(--warning, #f59e0b)',
    overloaded: 'var(--danger, #ef4444)'
  }
  return map[level] || 'var(--success)'
}

function getAvatarColor(name?: string): string {
  if (!name) return '#64b5f6'
  const colors = ['#7170ff', '#10b981', '#f59e0b', '#ef4444', '#64b5f6', '#8b5cf6', '#ec4899']
  let hash = 0
  for (const c of name) hash = (hash * 31 + c.charCodeAt(0)) & 0xffffffff
  return colors[Math.abs(hash) % colors.length]
}

async function loadMembers() {
  loading.value = true
  try {
    const res = await memberApi.list()
    members.value = (res.data || []).map((m: any) => ({ ...m }))
    // Load trend and tasks for each member in parallel
    await Promise.all(members.value.map(async (m) => {
      m.tasksLoading = true
      try {
        const [trendRes, tasksRes] = await Promise.all([
          memberApi.loadTrend(m.memberId),
          memberApi.memberTasks(m.memberId),
        ])
        m.loadData = trendRes.data || {}
        m.tasks = tasksRes.data || []
      } catch {
        m.loadData = {}
        m.tasks = []
      } finally {
        m.tasksLoading = false
      }
    }))
  } catch (err) {
    console.error('Failed to load members', err)
  } finally {
    loading.value = false
  }
}

function roleKey(role: string): string {
  const map: Record<string, string> = {
    'ADMIN': 'admin', 'PM': 'pd', 'DEV': 'dev',
    'DES': 'des', 'QA': 'qa', 'OPS': 'ops',
  }
  return map[role] || 'member'
}

function roleLabel(role: string): string {
  const map: Record<string, string> = {
    'ADMIN': '管理员', 'PM': '项目经理', 'DEV': '开发',
    'DES': '设计', 'QA': '测试', 'OPS': '运维',
    'MEMBER': '成员', 'VIEWER': '访客',
  }
  return map[role] || role
}

function roleDesc(role: string): string {
  const map: Record<string, string> = {
    'ADMIN': '系统管理员，拥有全部权限',
    'PM': '项目经理，负责项目规划与进度管理',
    'DEV': '开发工程师，负责功能开发实现',
    'DES': '设计师，负责 UI/UX 设计',
    'QA': '测试工程师，负责质量保障',
    'OPS': '运维工程师，负责部署与运维',
    'MEMBER': '普通成员',
    'VIEWER': '访客，仅可查看',
  }
  return map[role] || ''
}

onMounted(async () => {
  await memberStore.fetchRoles()
  loadMembers()
})
</script>

<style scoped>
.member-view {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.view-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.keyword-search {
  margin-left: auto;
}

.view-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
}

.view-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px;
  color: var(--text-secondary);
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.member-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.member-card {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 20px;
  transition: box-shadow 0.2s;
}

.member-card:hover {
  box-shadow: var(--shadow);
}

.member-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.member-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-role {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-badge {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 500;
}

.role-admin { background: rgba(167,139,250,0.15); color: #7c5af5; }
.role-pd { background: rgba(51,112,255,0.1); color: #3370ff; }
.role-dev { background: rgba(16,185,129,0.1); color: #00832e; }
.role-qa { background: rgba(245,158,11,0.1); color: #c26c00; }
.role-member { background: var(--primary-bg); color: var(--primary); }

.role-viewer { background: var(--surface-4); color: var(--text-muted); }

.member-phone {
  font-size: 12px;
  color: var(--text-faint);
}

.member-capacity {
  text-align: right;
  flex-shrink: 0;
}

.capacity-label {
  display: block;
  font-size: 11px;
  color: var(--text-faint);
}

.capacity-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.workload-section {
  margin-bottom: 16px;
}

.workload-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.workload-title {
  font-size: 12px;
  color: var(--text-secondary);
}

.load-indicator {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 10px;
}

.load-idle { background: rgba(100,181,246,0.15); color: #64b5f6; }
.load-normal { background: rgba(16,185,129,0.12); color: #00832e; }
.load-busy { background: rgba(245,158,11,0.12); color: #c26c00; }
.load-overloaded { background: rgba(239,68,68,0.12); color: #ef4444; }

.progress-bar {
  height: 6px;
  background: var(--border);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}

.progress-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}

.workload-stats {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-faint);
}

.trend-section { border-top: 1px solid var(--border); padding-top: 12px; }

.trend-title {
  font-size: 11px;
  color: var(--text-faint);
  margin-bottom: 8px;
}

.trend-bars {
  display: flex;
  gap: 6px;
  align-items: flex-end;
  height: 48px;
}

.trend-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  height: 100%;
}

.trend-bar-wrap {
  flex: 1;
  width: 100%;
  background: var(--border);
  border-radius: 3px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
  min-height: 4px;
}

.trend-bar-fill {
  width: 100%;
  border-radius: 3px;
  transition: height 0.3s;
  min-height: 4px;
}

.trend-week-label {
  font-size: 9px;
  color: var(--text-faint);
  white-space: nowrap;
}

/* Task list */
.task-list-section {
  border-top: 1px solid var(--border);
  padding-top: 12px;
}
.task-list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.task-list-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
}
.task-count-badge {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
  background: var(--primary-bg);
  color: var(--primary);
  font-weight: 500;
}
.task-loading, .task-empty {
  font-size: 12px;
  color: var(--text-faint);
  text-align: center;
  padding: 8px 0;
}
.task-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 160px;
  overflow-y: auto;
}
.task-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  border-radius: 5px;
  font-size: 12px;
  transition: background 0.12s;
}
.task-item:hover { background: var(--surface-3); }
.task-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-pending, .status-todo { background: #86909c; }
.status-in_progress, .status-progress { background: #3370ff; }
.status-blocked { background: #f53f3f; }
.status-done, .status-completed { background: #00b42a; }
.task-item-title {
  flex: 1;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}
.task-item-id {
  font-size: 10px;
  color: var(--text-faint);
  flex-shrink: 0;
}
.task-priority-badge {
  font-size: 9px;
  padding: 1px 5px;
  border-radius: 3px;
  font-weight: 600;
  flex-shrink: 0;
}
.priority-P0, .priority-URGENT { background: rgba(245,63,63,0.12); color: #f53f3f; }
.priority-P1, .priority-HIGH   { background: rgba(245,158,11,0.12); color: #c26c00; }
.priority-P2, .priority-MEDIUM  { background: rgba(100,181,246,0.12); color: #64b5f6; }
.priority-P3, .priority-LOW     { background: rgba(100,181,246,0.08); color: #86909c; }
.task-progress-text {
  font-size: 10px;
  color: var(--text-faint);
  flex-shrink: 0;
  min-width: 28px;
  text-align: right;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px;
  color: var(--text-secondary);
  gap: 8px;
}

.empty-icon { font-size: 48px; }
.empty-text { font-size: 14px; }

/* Role description */
.role-desc-section {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 16px 20px;
  margin-bottom: 4px;
}
.role-desc-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 12px;
}
.role-desc-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.role-desc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface-3);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 6px 12px;
}
.role-desc-text {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
