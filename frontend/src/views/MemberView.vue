<template>
  <div class="member-view">
    <div class="view-header">
      <div class="header-left">
        <h1 class="view-title">团队成员</h1>
        <p class="view-subtitle">实时掌控团队工作负载与任务进展</p>
      </div>
      <div class="header-right">
        <el-input
          v-model="keyword"
          placeholder="搜索成员..."
          clearable
          size="small"
          class="search-input"
        >
          <template #prefix>🔍</template>
        </el-input>
      </div>
    </div>

    <!-- Role Guide (Subtle) -->
    <div v-if="memberStore?.roles?.length" class="role-guide">
      <div v-for="role in (memberStore?.roles || [])" :key="role" class="role-guide-item">
        <span class="role-badge" :class="'role-' + roleKey(role)">{{ roleLabel(role) }}</span>
      </div>
    </div>

    <!-- Main Content -->
    <div v-if="loading" class="loading-wrap">
      <div class="spinner"></div>
    </div>

    <div v-else class="member-grid">
      <div
        v-for="member in filteredMembers"
        :key="member.memberId"
        class="member-card"
      >
        <div class="card-header">
          <el-avatar :size="40" class="member-avatar" :style="{ background: getAvatarColor(member.nickname) }">
            {{ member.nickname?.slice(0, 1) }}
          </el-avatar>
          <div class="member-basic">
            <div class="name-row">
              <span class="member-name">{{ member.nickname }}</span>
              <span class="capacity-tag">{{ member.weeklyCapacity || 40 }}h/周</span>
            </div>
            <div class="role-row">
              <span :class="['role-badge', 'role-' + (member.role || 'member').toLowerCase()]">
                {{ member.role || '成员' }}
              </span>
              <span class="member-phone">{{ member.phone || '无手机号' }}</span>
            </div>
          </div>
        </div>

        <!-- Weekly Load -->
        <div class="workload-panel">
          <div class="panel-header">
            <span class="panel-title">本周负载</span>
            <span :class="['load-status', 'load-' + getWeekLoadLevel(member.loadData)]">
              {{ getLoadLabel(member.loadData) }}
            </span>
          </div>
          <div class="load-bar">
            <div
              class="load-fill"
              :style="{
                width: Math.min(100, getWeekLoad(member.loadData)) + '%',
                background: getLoadColor(member.loadData)
              }"
            ></div>
          </div>
          <div class="load-meta">
            <span>{{ getWeekTasks(member.loadData) }} 个任务</span>
            <span class="percent-val">{{ getWeekLoad(member.loadData) }}%</span>
          </div>
        </div>

        <!-- Trend -->
        <div v-if="member.loadData?.weeks?.length" class="trend-panel">
          <div class="panel-title">近4周趋势</div>
          <div class="trend-chart">
            <div
              v-for="week in member.loadData.weeks.slice(0, 4)"
              :key="week.week"
              class="trend-col"
            >
              <div class="trend-bar-wrap">
                <div
                  class="trend-bar"
                  :style="{
                    height: Math.min(100, week.loadPercent) + '%',
                    background: getLoadColor({ loadPercent: week.loadPercent })
                  }"
                ></div>
              </div>
              <span class="week-label">{{ week.week.slice(-2) }}w</span>
            </div>
          </div>
        </div>

        <!-- Tasks -->
        <div class="task-panel">
          <div class="panel-header">
            <span class="panel-title">任务列表</span>
            <span class="task-count">{{ member.tasks?.length || 0 }}</span>
          </div>
          
          <div v-if="member.tasksLoading" class="panel-msg">加载中...</div>
          <div v-else-if="!member.tasks?.length" class="panel-msg">暂无活跃任务</div>
          <div v-else class="task-mini-list">
            <div v-for="task in member.tasks.slice(0, 5)" :key="task.id" class="task-mini-item">
              <span class="status-dot" :class="'status-' + (task.status || '').toLowerCase()"></span>
              <span class="task-title">{{ task.title }}</span>
              <span :class="['priority-dot', 'p-' + ((task.priority || 'MEDIUM').toLowerCase())]"></span>
            </div>
            <div v-if="member.tasks.length > 5" class="more-tasks">还有 {{ member.tasks.length - 5 }} 个任务...</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && filteredMembers.length === 0" class="empty-state">
      <span class="empty-icon">👥</span>
      <p>{{ keyword ? '未找到匹配成员' : '暂无团队成员数据' }}</p>
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
    idle: 'var(--text-faint)',
    normal: 'var(--success)',
    busy: 'var(--warning)',
    overloaded: 'var(--danger)'
  }
  return map[level] || 'var(--success)'
}

function getAvatarColor(name?: string): string {
  if (!name) return '#64b5f6'
  const colors = ['#5B8DEF', '#32d583', '#f5a623', '#ec5f5f', '#8b5cf6', '#ec4899']
  let hash = 0
  for (const c of name) hash = (hash * 31 + c.charCodeAt(0)) & 0xffffffff
  return colors[Math.abs(hash) % colors.length]
}

async function loadMembers() {
  loading.value = true
  try {
    const res = await memberApi.list()
    members.value = (res.data || []).map((m: any) => ({ ...m }))
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
    'ADMIN': '管理员', 'PM': 'PM', 'DEV': '开发',
    'DES': '设计', 'QA': '测试', 'OPS': '运维',
    'MEMBER': '成员', 'VIEWER': '访客',
  }
  return map[role] || role
}

onMounted(async () => {
  await memberStore.fetchRoles()
  loadMembers()
})
</script>

<style scoped>
.member-view {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  background: var(--bg);
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.view-title { font-size: 20px; font-weight: 700; color: var(--text); margin-bottom: 4px; }
.view-subtitle { font-size: 13px; color: var(--text-faint); }
.search-input { width: 240px; }

.role-guide {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.role-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  text-transform: uppercase;
  letter-spacing: 0.2px;
}

.role-admin { background: rgba(167,139,250,0.15); color: #a78bfa; }
.role-pd { background: rgba(91,141,239,0.15); color: var(--primary); }
.role-dev { background: rgba(50,213,131,0.15); color: var(--success); }
.role-qa { background: rgba(245,166,35,0.15); color: var(--warning); }
.role-member { background: var(--surface-3); color: var(--text-secondary); }

.member-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.member-card {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  transition: all 0.2s ease;
}

.member-card:hover { border-color: var(--text-faint); transform: translateY(-2px); box-shadow: var(--shadow-md); }

.card-header {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.member-avatar {
  border: 2px solid var(--bg);
  box-shadow: var(--shadow-sm);
}

.member-basic { flex: 1; }
.name-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.member-name { font-size: 16px; font-weight: 600; color: var(--text); }
.capacity-tag { font-size: 11px; color: var(--text-faint); font-weight: 500; }
.role-row { display: flex; align-items: center; gap: 8px; }
.member-phone { font-size: 11px; color: var(--text-muted); }

.workload-panel, .trend-panel, .task-panel {
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
  margin-top: 16px;
}

.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.panel-title { font-size: 11px; font-weight: 700; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.4px; }

.load-status { font-size: 10px; font-weight: 700; padding: 1px 6px; border-radius: 4px; }
.load-idle { background: var(--surface-3); color: var(--text-faint); }
.load-normal { background: var(--success-bg); color: var(--success); }
.load-busy { background: var(--warning-bg); color: var(--warning); }
.load-overloaded { background: var(--danger-bg); color: var(--danger); }

.load-bar { height: 6px; background: var(--surface-3); border-radius: 3px; overflow: hidden; margin-bottom: 6px; }
.load-fill { height: 100%; border-radius: 3px; transition: width 0.4s ease; }
.load-meta { display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); font-weight: 500; }
.percent-val { font-weight: 700; color: var(--text-secondary); }

.trend-chart { display: flex; gap: 8px; align-items: flex-end; height: 40px; margin-top: 12px; }
.trend-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.trend-bar-wrap { width: 100%; height: 32px; background: var(--surface-3); border-radius: 2px; display: flex; align-items: flex-end; overflow: hidden; }
.trend-bar { width: 100%; transition: height 0.3s; }
.week-label { font-size: 9px; color: var(--text-faint); font-weight: 500; }

.task-count { font-size: 11px; font-weight: 700; color: var(--primary); background: var(--primary-bg); padding: 1px 6px; border-radius: 10px; }
.panel-msg { font-size: 11px; color: var(--text-faint); padding: 8px 0; text-align: center; }

.task-mini-list { display: flex; flex-direction: column; gap: 6px; margin-top: 8px; }
.task-mini-item { display: flex; align-items: center; gap: 8px; padding: 6px; border-radius: 4px; transition: background 0.12s; }
.task-mini-item:hover { background: var(--surface-2); }
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.status-todo { background: var(--text-faint); }
.status-in_progress { background: var(--primary); }
.status-done { background: var(--success); }
.status-blocked { background: var(--danger); }
.task-title { flex: 1; font-size: 12px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.priority-dot { width: 4px; height: 4px; border-radius: 50%; }
.p-urgent { background: var(--danger); }
.p-high { background: var(--warning); }
.p-medium { background: var(--primary); }
.p-low { background: var(--text-faint); }
.more-tasks { font-size: 10px; color: var(--text-faint); text-align: center; margin-top: 4px; font-style: italic; }

.loading-wrap { display: flex; justify-content: center; padding: 100px; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--surface-3); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty-state { display: flex; flex-direction: column; align-items: center; padding: 80px 0; color: var(--text-faint); }
.empty-icon { font-size: 48px; margin-bottom: 12px; opacity: 0.5; }
</style>
