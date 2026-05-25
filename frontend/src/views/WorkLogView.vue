<template>
  <div class="worklog-view">
    <div class="view-header">
      <div class="header-left">
        <h1 class="view-title">工作日志</h1>
        <p class="view-subtitle">记录每日点滴，沉淀团队价值</p>
      </div>
      <div class="header-right">
        <button class="btn btn-ghost" @click="exportCsv">📥 导出报告</button>
      </div>
    </div>

    <div class="worklog-content">
      <!-- Left: Form & Stats -->
      <aside class="content-left">
        <div class="card log-form-card">
          <div class="card-title">📝 新建日志</div>
          <el-form label-position="top">
            <div class="form-row">
              <el-form-item label="日期">
                <el-date-picker v-model="logDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
              <el-form-item label="关联任务">
                <el-select v-model="logForm.taskId" placeholder="选择任务 (可选)" clearable filterable style="width:100%">
                  <el-option v-for="task in taskStore.tasks" :key="task.id" :label="task.title" :value="String(task.id)" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="🔵 今日进展" required>
              <el-input v-model="logForm.todayProgress" type="textarea" :rows="3" placeholder="完成了哪些具体工作？" />
            </el-form-item>

            <el-form-item label="🗓 明日计划">
              <el-input v-model="logForm.tomorrowPlan" type="textarea" :rows="2" placeholder="下一步的核心目标..." />
            </el-form-item>

            <el-form-item label="⚠️ 风险与阻碍">
              <el-input v-model="logForm.blockers" type="textarea" :rows="2" placeholder="是否有进度卡点？" />
            </el-form-item>

            <div class="form-actions">
              <button class="btn btn-ghost" @click="handleReset">重置</button>
              <button class="btn btn-primary" :disabled="saving" @click="handleSaveLog">
                {{ saving ? '保存中...' : '提交日志' }}
              </button>
            </div>
          </el-form>
        </div>

        <div class="card stats-card">
          <div class="card-title">📊 汇总统计</div>
          <div class="stats-grid">
            <div class="stat-box">
              <span class="stat-val">{{ logs.length }}</span>
              <span class="stat-lab">总日志数</span>
            </div>
            <div class="stat-box">
              <span class="stat-val text-danger">{{ riskCount }}</span>
              <span class="stat-lab">风险项</span>
            </div>
            <div class="stat-box">
              <span class="stat-val">{{ uniqueTaskCount }}</span>
              <span class="stat-lab">覆盖任务</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- Right: History -->
      <main class="content-right">
        <div class="history-header">
          <h3 class="card-title">📜 历史记录</h3>
          <el-date-picker
            v-model="filterDate"
            type="date"
            placeholder="按日期筛选"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            clearable
            size="small"
            style="width: 150px"
            @change="fetchLogs"
          />
        </div>

        <div v-loading="loading" class="log-list">
          <div v-if="!logs.length" class="empty-state">
            <span class="empty-icon">📝</span>
            <p>{{ filterDate ? '所选日期暂无记录' : '暂无任何日志记录' }}</p>
          </div>

          <div v-for="log in logs" :key="log.id" class="log-item">
            <div class="log-item-header">
              <div class="log-meta">
                <span class="log-date">{{ log.date }}</span>
                <span v-if="log.taskId" class="task-badge">{{ getTaskTitle(log.taskId) }}</span>
              </div>
              <button class="icon-btn" @click="handleEdit(log)">✏️</button>
            </div>

            <div class="log-item-body">
              <div class="log-chunk">
                <span class="chunk-label">PROGRESS</span>
                <p class="chunk-text">{{ log.todayDone || '未记录进展' }}</p>
              </div>
              <div class="log-chunk">
                <span class="chunk-label">PLAN</span>
                <p class="chunk-text">{{ log.tomorrowPlan || '未记录计划' }}</p>
              </div>
              <div v-if="log.blockedReason" class="log-chunk risk">
                <span class="chunk-label">RISK</span>
                <p class="chunk-text">{{ log.blockedReason }}</p>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useProjectStore, useTaskStore, useWorkLogStore } from '@/stores'
import type { WorkLog } from '@/types'
import { workLogApi } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const projectStore = useProjectStore()
const taskStore = useTaskStore()
const workLogStore = useWorkLogStore()

const logDate = ref(dayjs().format('YYYY-MM-DD'))
const filterDate = ref<string | null>(dayjs().format('YYYY-MM-DD'))
const saving = ref(false)
const loading = ref(false)
const logs = ref<WorkLog[]>([])

const logForm = ref({
  taskId: '',
  todayProgress: '',
  tomorrowPlan: '',
  blockers: '',
})

function getTaskTitle(taskId: string) {
  return taskStore.tasks.find(t => String(t.id) === String(taskId))?.title || ''
}

async function fetchLogs() {
  loading.value = true
  try {
    const dateParam = filterDate.value || undefined
    const res = await workLogApi.list({ date: dateParam })
    const raw = res.data
    logs.value = (raw as any)?.data ?? (Array.isArray(raw) ? raw : [])
  } catch {
    logs.value = []
  } finally {
    loading.value = false
  }
}

async function handleSaveLog() {
  if (!logForm.value.todayProgress.trim()) {
    ElMessage.warning('请填写今日进展')
    return
  }
  saving.value = true
  try {
    const userId = localStorage.getItem('memberId') || '匿名用户'
    await workLogApi.create({
      taskId: logForm.value.taskId || null,
      userId: userId,
      logDate: logDate.value,
      todayDone: logForm.value.todayProgress,
      tomorrowPlan: logForm.value.tomorrowPlan,
      blockedReason: logForm.value.blockers || null,
    })
    ElMessage.success('日志已保存')
    handleReset()
    await fetchLogs()
  } catch (e: any) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handleReset() {
  logForm.value = { taskId: '', todayProgress: '', tomorrowPlan: '', blockers: '' }
  logDate.value = dayjs().format('YYYY-MM-DD')
}

function handleEdit(log: WorkLog) {
  logForm.value = {
    taskId: String(log.taskId || ''),
    todayProgress: log.todayDone || '',
    tomorrowPlan: log.tomorrowPlan || '',
    blockers: log.blockedReason || '',
  }
  logDate.value = log.date
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const riskCount = computed(() => logs.value.filter(l => l.currentStatus === '有风险' || l.currentStatus === '已阻塞' || l.blockedReason).length)
const uniqueTaskCount = computed(() => new Set(logs.value.filter(l => l.taskId).map(l => l.taskId)).size)

function exportCsv() {
  if (!logs.value.length) { ElMessage.warning('暂无日志可导出'); return }
  const header = ['日期', '关联任务', '今日进展', '明日计划', '阻碍风险']
  const rows = logs.value.map(l => [
    l.date,
    getTaskTitle(String(l.taskId || '')),
    (l.todayDone || '').replace(/\n/g, ' '),
    (l.tomorrowPlan || '').replace(/\n/g, ' '),
    (l.blockedReason || '').replace(/\n/g, ' '),
  ])
  const csv = [header, ...rows].map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `工作日志_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

onMounted(async () => {
  const pid = projectStore.currentProjectId
  if (pid) {
    await taskStore.fetchTasks(pid)
    await fetchLogs()
  }
})

watch(() => projectStore.currentProjectId, async (pid) => {
  if (pid) {
    await taskStore.fetchTasks(pid)
    await fetchLogs()
  }
})
</script>

<style scoped>
.worklog-view {
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

.worklog-content {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 24px;
  align-items: start;
}

.card {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 24px;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-faint);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
}

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-box {
  background: var(--surface-2);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  padding: 12px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-val { font-size: 18px; font-weight: 700; color: var(--text); }
.stat-lab { font-size: 10px; color: var(--text-faint); font-weight: 600; text-transform: uppercase; }

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.log-list { display: flex; flex-direction: column; gap: 16px; }

.log-item {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  transition: all 0.2s ease;
}

.log-item:hover { border-color: var(--text-faint); }

.log-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.log-meta { display: flex; align-items: center; gap: 12px; }
.log-date { font-size: 13px; font-weight: 700; color: var(--primary); }
.task-badge {
  font-size: 11px;
  background: var(--surface-3);
  color: var(--text-secondary);
  padding: 2px 8px;
  border-radius: 4px;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.icon-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.15s;
}
.icon-btn:hover { opacity: 1; }

.log-item-body { display: flex; flex-direction: column; gap: 12px; }
.log-chunk { padding-left: 12px; border-left: 2px solid var(--border-strong); }
.log-chunk.risk { border-left-color: var(--danger); }

.chunk-label {
  display: block;
  font-size: 9px;
  font-weight: 800;
  color: var(--text-faint);
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.chunk-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

.risk .chunk-text { color: var(--danger); }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: var(--text-faint);
}
.empty-icon { font-size: 32px; margin-bottom: 12px; opacity: 0.5; }

.text-danger { color: var(--danger) !important; }

@media (max-width: 900px) {
  .worklog-content { grid-template-columns: 1fr; }
}
</style>
