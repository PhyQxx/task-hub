<template>
  <div class="worklog-view">
    <!-- 顶部标题栏 -->
    <div class="worklog-header">
      <div class="header-left">
        <span class="header-icon">📋</span>
        <div>
          <div class="header-title">工作日志</div>
          <div class="header-subtitle">记录每日进展，追踪团队进度</div>
        </div>
      </div>
    </div>

    <!-- 填写区域 -->
    <div class="log-form-card">
      <div class="card-title">📝 填写日志</div>
      <el-form class="log-form" label-position="top" size="large">
        <!-- 日期选择 -->
        <el-form-item label="日期">
          <el-date-picker
            v-model="logDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 200px"
          />
        </el-form-item>

        <!-- 任务标签 -->
        <el-form-item label="关联任务">
          <el-select
            v-model="logForm.taskId"
            placeholder="选择关联任务（可选）"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="task in taskStore.tasks"
              :key="task.id"
              :label="`${task.taskId || ''} ${task.title}`"
              :value="String(task.id)"
            />
          </el-select>
        </el-form-item>

        <!-- 今日进展 -->
        <el-form-item label="🔵 今日进展" required>
          <el-input
            v-model="logForm.todayProgress"
            type="textarea"
            :rows="3"
            placeholder="今天完成了哪些工作？"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 明日计划 -->
        <el-form-item label="🗓 明日计划">
          <el-input
            v-model="logForm.tomorrowPlan"
            type="textarea"
            :rows="2"
            placeholder="明天计划做什么？"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>

        <!-- 阻碍风险 -->
        <el-form-item label="⚠️ 阻碍风险">
          <el-input
            v-model="logForm.blockers"
            type="textarea"
            :rows="2"
            placeholder="遇到什么阻碍或风险？"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <div class="form-actions">
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" :loading="saving" @click="handleSaveLog">
              保存日志
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 历史日志列表 -->
    <div class="log-history">
      <div class="history-header">
        <div class="history-title">📜 历史日志</div>
        <div class="history-filter">
          <span class="filter-label">筛选：</span>
          <el-date-picker
            v-model="filterDate"
            type="date"
            placeholder="选择日期查看"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 180px"
            @change="fetchLogs"
          />
        </div>
      </div>

      <div v-loading="loading" class="log-list">
        <el-empty v-if="!logs.length" :description="filterDate ? '该日期暂无日志' : '暂无历史日志'" />

        <div v-for="log in logs" :key="log.id" class="log-item">
          <div class="log-item-header">
            <div class="log-meta">
              <span class="log-date-badge">{{ log.date }}</span>
              <span v-if="log.taskId" class="log-task-tag">
                {{ getTaskTitle(log.taskId) }}
              </span>
            </div>
            <el-button size="small" text type="primary" @click="handleEdit(log)">编辑</el-button>
          </div>

          <div class="log-body">
            <div class="log-section today-progress">
              <div class="section-icon">🔵</div>
              <div class="section-content">
                <div class="section-label">今日进展</div>
                <div class="section-text">{{ log.todayDone || '—' }}</div>
              </div>
            </div>

            <div class="log-section tomorrow-plan">
              <div class="section-icon">🗓</div>
              <div class="section-content">
                <div class="section-label">明日计划</div>
                <div class="section-text">{{ log.tomorrowPlan || '—' }}</div>
              </div>
            </div>

            <div v-if="log.blockedReason" class="log-section risk-section">
              <div class="section-icon">⚠️</div>
              <div class="section-content">
                <div class="section-label">阻碍风险</div>
                <div class="section-text risk-text">{{ log.blockedReason }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
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
    const userName = localStorage.getItem('userName') || '匿名用户'
    await workLogApi.create({
      taskId: logForm.value.taskId || null,
      userId: userName,
      logDate: logDate.value,
      todayDone: logForm.value.todayProgress,
      tomorrowPlan: logForm.value.tomorrowPlan,
      blockedReason: logForm.value.blockers || null,
    })
    ElMessage.success('日志保存成功')
    handleReset()
    await fetchLogs()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
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
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 24px;
  gap: 20px;
  overflow-y: auto;
  background: #f7f8fc;
}

/* 顶部标题栏 */
.worklog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 16px 20px;
  border-radius: 10px;
  border: 1px solid #e5e6eb;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-icon { font-size: 24px; }
.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}
.header-subtitle {
  font-size: 12px;
  color: #86909c;
  margin-top: 2px;
}

/* 填写卡片 */
.log-form-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  border: 1px solid #e5e6eb;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 16px;
}
.log-form {
  max-width: 720px;
}
.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

/* 历史日志区 */
.log-history {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  border: 1px solid #e5e6eb;
  flex: 1;
}
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}
.history-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.history-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-label {
  font-size: 12px;
  color: #86909c;
}
.log-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 日志条目 */
.log-item {
  background: #fafafa;
  border: 1px solid #f1f2f5;
  border-radius: 8px;
  padding: 14px 16px;
  transition: box-shadow 0.15s, border-color 0.15s;
}
.log-item:hover {
  box-shadow: 0 2px 8px rgba(31,35,41,0.06);
  border-color: #e4e7ec;
}
.log-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.log-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.log-date-badge {
  font-size: 12px;
  font-weight: 600;
  color: #3370ff;
  background: rgba(51,112,255,0.08);
  padding: 2px 8px;
  border-radius: 4px;
}
.log-task-tag {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(100,181,246,0.12);
  color: #3370ff;
  border-radius: 4px;
}

/* 日志内容体 */
.log-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.log-section {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid transparent;
}
.log-section.today-progress {
  border-color: rgba(51,112,255,0.12);
}
.log-section.tomorrow-plan {
  border-color: rgba(0,185,76,0.12);
}
.log-section.risk-section {
  border-color: rgba(245,63,63,0.12);
  background: rgba(245,63,63,0.03);
}
.section-icon {
  font-size: 14px;
  flex-shrink: 0;
  margin-top: 1px;
}
.section-content {
  flex: 1;
  min-width: 0;
}
.section-label {
  font-size: 11px;
  font-weight: 600;
  color: #86909c;
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.section-text {
  font-size: 13px;
  color: #1f2329;
  line-height: 1.5;
  word-break: break-word;
}
.risk-text { color: #c0261c; }
</style>
