<template>
  <div class="admin-dashboard">
    <div class="view-header">
      <div class="header-left">
        <h1 class="view-title">管理后台概览</h1>
        <p class="view-subtitle">全站资源监控与任务效能分析</p>
      </div>
      <div class="header-right">
        <el-button size="small" @click="fetchAllData" :loading="loading">🔄 刷新全局数据</el-button>
      </div>
    </div>

    <!-- KPI Cards -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <span class="kpi-label">全站项目</span>
        <div class="kpi-value">{{ stats.totalProjects }}</div>
        <div class="kpi-trend success">活跃中</div>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">任务总数</span>
        <div class="kpi-value">{{ stats.totalTasks }}</div>
        <div class="kpi-trend">
          <span class="dot dot-progress"></span> {{ stats.inProgressTasks }} 进行中
        </div>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">团队成员</span>
        <div class="kpi-value">{{ stats.totalMembers }}</div>
        <div class="kpi-trend">平均负载 {{ stats.avgLoad }}%</div>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">总体完成率</span>
        <div class="kpi-value">{{ stats.globalCompletion }}%</div>
        <div class="kpi-progress">
          <div class="kpi-progress-fill" :style="{ width: stats.globalCompletion + '%' }"></div>
        </div>
      </div>
    </div>

    <div class="dashboard-content">
      <!-- Left Column: Project Stats -->
      <div class="content-column">
        <div class="card project-stats-card">
          <div class="card-header">
            <h3 class="card-title">项目进度排行</h3>
            <span class="card-extra">TOP 5</span>
          </div>
          <div class="project-list">
            <div v-for="p in projectStats" :key="p.id" class="project-stat-item">
              <div class="project-info">
                <span class="project-name">{{ p.name }}</span>
                <span class="project-percent">{{ p.completion }}%</span>
              </div>
              <el-progress :percentage="p.completion" :stroke-width="6" :show-text="false" />
              <div class="project-meta">
                <span>{{ p.doneTasks }}/{{ p.totalTasks }} 任务已完成</span>
                <span v-if="p.blockedTasks > 0" class="text-danger">⚠️ {{ p.blockedTasks }} 阻塞</span>
              </div>
            </div>
          </div>
        </div>

        <div class="card status-dist-card">
          <h3 class="card-title">全站任务状态分布</h3>
          <div class="status-chart">
            <div 
              v-for="s in statusDist" 
              :key="s.label" 
              class="status-bar-wrap" 
              :title="s.label + ': ' + s.count"
            >
              <div class="status-bar" :style="{ height: s.percent + '%', background: s.color }"></div>
              <span class="status-label">{{ s.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column: Member Stats -->
      <div class="content-column">
        <div class="card member-load-card">
          <div class="card-header">
            <h3 class="card-title">成员负载监控</h3>
            <span class="card-extra">实时压力值</span>
          </div>
          <div class="member-load-list">
            <div v-for="m in memberLoadRanking" :key="m.id" class="member-load-item">
              <el-avatar :size="24" class="m-av">{{ m.name.slice(0,1) }}</el-avatar>
              <div class="m-info">
                <div class="m-name-row">
                  <span class="m-name">{{ m.name }}</span>
                  <span class="m-count">{{ m.taskCount }} 任务</span>
                </div>
                <div class="m-load-bar-wrap">
                  <div class="m-load-bar" :style="{ width: Math.min(100, m.load) + '%', background: getLoadColor(m.load) }"></div>
                </div>
              </div>
              <span class="m-load-val" :style="{ color: getLoadColor(m.load) }">{{ m.load }}%</span>
            </div>
          </div>
        </div>

        <div class="card recent-logs-card">
          <h3 class="card-title">全站近期风险日志</h3>
          <div class="recent-logs">
            <div v-for="l in recentRiskLogs" :key="l.id" class="log-item">
              <div class="log-meta">
                <span class="log-user">{{ l.userName }}</span>
                <span class="log-date">{{ l.date }}</span>
              </div>
              <p class="log-content text-danger">{{ l.blockedReason }}</p>
              <div class="log-project">项目：{{ l.projectName }}</div>
            </div>
            <div v-if="!recentRiskLogs.length" class="empty-msg">当前全站暂无风险反馈</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { projectApi, taskApi, memberApi, workLogApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const allTasks = ref<any[]>([])
const allProjects = ref<any[]>([])
const allMembers = ref<any[]>([])
const allLogs = ref<any[]>([])

const stats = ref({
  totalProjects: 0,
  totalTasks: 0,
  totalMembers: 0,
  inProgressTasks: 0,
  globalCompletion: 0,
  avgLoad: 0
})

const projectStats = computed(() => {
  return allProjects.value.map(p => {
    const pTasks = allTasks.value.filter(t => (t.projectId || t.project_id) === p.projectId)
    const total = pTasks.length
    const done = pTasks.filter(t => t.status === 'DONE').length
    const blocked = pTasks.filter(t => t.status === 'BLOCKED').length
    return {
      id: p.projectId,
      name: p.name,
      totalTasks: total,
      doneTasks: done,
      blockedTasks: blocked,
      completion: total ? Math.round((done / total) * 100) : 0
    }
  }).sort((a, b) => b.completion - a.completion).slice(0, 5)
})

const statusDist = computed(() => {
  const counts = {
    '待处理': allTasks.value.filter(t => t.status === 'TODO').length,
    '进行中': allTasks.value.filter(t => t.status === 'IN_PROGRESS').length,
    '已完成': allTasks.value.filter(t => t.status === 'DONE').length,
    '已阻塞': allTasks.value.filter(t => t.status === 'BLOCKED').length,
  }
  const total = allTasks.value.length || 1
  const colors = { '待处理': '#9ca3af', '进行中': '#5B8DEF', '已完成': '#10b981', '已阻塞': '#ef4444' }
  return Object.entries(counts).map(([label, count]) => ({
    label,
    count,
    percent: (count / total) * 100,
    color: (colors as any)[label]
  }))
})

const memberLoadRanking = computed(() => {
  return allMembers.value.map(m => {
    const mTasks = allTasks.value.filter(t => (t.assigneeId || t.assignee_id) === m.memberId)
    const activeTasks = mTasks.filter(t => t.status !== 'DONE').length
    // Assuming each active task takes ~10h/week, 40h capacity
    const load = Math.min(120, Math.round((activeTasks * 10 / 40) * 100))
    return {
      id: m.memberId,
      name: m.nickname,
      taskCount: activeTasks,
      load: load
    }
  }).sort((a, b) => b.load - a.load).slice(0, 8)
})

const recentRiskLogs = computed(() => {
  return allLogs.value
    .filter(l => l.blockedReason)
    .map(l => {
      const p = allProjects.value.find(p => p.projectId === (l.projectId || l.project_id))
      return {
        ...l,
        projectName: p ? p.name : '未知项目',
        userName: resolveNickname(l.userId)
      }
    })
    .slice(0, 5)
})

function resolveNickname(userIdRaw: string) {
  if (!userIdRaw) return '未知用户'
  
  // Try parsing raw Java object string
  if (typeof userIdRaw === 'string' && userIdRaw.includes('nickname=')) {
    const match = userIdRaw.match(/nickname=([^,\]\s]+)/)
    if (match) return match[1]
  }

  // Lookup by ID
  let targetId = userIdRaw
  if (typeof userIdRaw === 'string' && userIdRaw.includes('memberId=')) {
    const match = userIdRaw.match(/memberId=([^,\]\s]+)/)
    if (match) targetId = match[1]
  }

  const m = allMembers.value.find(m => m.memberId === targetId)
  return m ? m.nickname : (targetId.length > 20 ? '系统用户' : targetId)
}

function getLoadColor(load: number) {
  if (load > 90) return 'var(--danger)'
  if (load > 60) return 'var(--warning)'
  return 'var(--success)'
}

async function fetchAllData() {
  loading.value = true
  try {
    const [pRes, tRes, mRes, lRes] = await Promise.all([
      projectApi.list(),
      taskApi.listAll(),
      memberApi.list(),
      workLogApi.list()
    ])

    allProjects.value = (pRes.data || []).map((p:any) => ({ ...p, projectId: p.projectId || p.id }))
    allTasks.value = tRes.data || []
    allMembers.value = mRes.data || []
    allLogs.value = (lRes.data as any)?.data || lRes.data || []

    const doneCount = allTasks.value.filter(t => t.status === 'DONE').length
    stats.value = {
      totalProjects: allProjects.value.length,
      totalTasks: allTasks.value.length,
      totalMembers: allMembers.value.length,
      inProgressTasks: allTasks.value.filter(t => t.status === 'IN_PROGRESS').length,
      globalCompletion: allTasks.value.length ? Math.round((doneCount / allTasks.value.length) * 100) : 0,
      avgLoad: memberLoadRanking.value.length 
        ? Math.round(memberLoadRanking.value.reduce((acc, m) => acc + m.load, 0) / memberLoadRanking.value.length)
        : 0
    }
  } catch (e) {
    ElMessage.error('获取全站数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAllData()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 24px 32px;
  height: 100%;
  overflow-y: auto;
  background: var(--bg);
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
}

.view-title { font-size: 24px; font-weight: 800; color: var(--text); letter-spacing: -0.5px; margin-bottom: 4px; }
.view-subtitle { font-size: 14px; color: var(--text-faint); }

/* KPIs */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.kpi-card {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s, border-color 0.2s;
}
.kpi-card:hover { transform: translateY(-2px); border-color: var(--text-faint); }

.kpi-label { font-size: 11px; font-weight: 700; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }
.kpi-value { font-size: 28px; font-weight: 800; color: var(--text); line-height: 1; margin-bottom: 12px; }
.kpi-trend { font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 6px; font-weight: 600; }
.kpi-trend.success { color: var(--success); }

.dot { width: 6px; height: 6px; border-radius: 50%; }
.dot-progress { background: var(--primary); }

.kpi-progress { height: 4px; background: var(--surface-3); border-radius: 2px; overflow: hidden; margin-top: 4px; }
.kpi-progress-fill { height: 100%; background: var(--primary); border-radius: 2px; }

/* Dashboard Content */
.dashboard-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.content-column { display: flex; flex-direction: column; gap: 24px; }

.card {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.card-title { font-size: 14px; font-weight: 700; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }
.card-extra { font-size: 11px; font-weight: 800; background: var(--surface-3); padding: 2px 8px; border-radius: 4px; color: var(--text-faint); }

/* Project List */
.project-list { display: flex; flex-direction: column; gap: 16px; }
.project-stat-item { display: flex; flex-direction: column; gap: 8px; }
.project-info { display: flex; justify-content: space-between; }
.project-name { font-size: 14px; font-weight: 600; color: var(--text); }
.project-percent { font-size: 14px; font-weight: 700; color: var(--primary); }
.project-meta { display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); font-weight: 500; }

/* Status Dist */
.status-chart { display: flex; align-items: flex-end; justify-content: space-around; height: 160px; padding-top: 20px; }
.status-bar-wrap { display: flex; flex-direction: column; align-items: center; gap: 8px; flex: 1; }
.status-bar { width: 32px; border-radius: 4px 4px 0 0; transition: height 0.6s cubic-bezier(0.4, 0, 0.2, 1); }
.status-label { font-size: 11px; color: var(--text-faint); font-weight: 600; }

/* Member Load */
.member-load-list { display: flex; flex-direction: column; gap: 14px; }
.member-load-item { display: flex; align-items: center; gap: 12px; }
.m-av { background: var(--surface-3) !important; color: var(--text-secondary) !important; font-weight: 700; font-size: 11px; }
.m-info { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.m-name-row { display: flex; justify-content: space-between; }
.m-name { font-size: 13px; font-weight: 600; color: var(--text); }
.m-count { font-size: 11px; color: var(--text-faint); }
.m-load-bar-wrap { height: 4px; background: var(--surface-3); border-radius: 2px; overflow: hidden; }
.m-load-bar { height: 100%; transition: width 0.4s; }
.m-load-val { font-size: 12px; font-weight: 700; width: 40px; text-align: right; }

/* Recent Logs */
.recent-logs { display: flex; flex-direction: column; gap: 16px; }
.log-item { padding: 12px; background: var(--surface-2); border-radius: var(--radius-md); border: 1px solid var(--border-subtle); }
.log-meta { display: flex; justify-content: space-between; margin-bottom: 6px; }
.log-user { font-size: 12px; font-weight: 700; color: var(--text); }
.log-date { font-size: 11px; color: var(--text-faint); }
.log-content { font-size: 12px; line-height: 1.5; margin: 0 0 6px; }
.log-project { font-size: 10px; color: var(--text-muted); font-weight: 600; }

.empty-msg { padding: 40px 0; text-align: center; color: var(--text-faint); font-size: 13px; font-style: italic; }

.text-danger { color: var(--danger); }

@media (max-width: 1200px) {
  .dashboard-content { grid-template-columns: 1fr; }
}
</style>
