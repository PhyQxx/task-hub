<template>
  <div class="worklog-view">
    <div class="worklog-header">
      <span class="project-name">{{ projectStore.currentProject?.name || '请选择项目' }}</span>
      <el-date-picker
        v-model="logDate"
        type="date"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        size="small"
        style="width: 160px"
      />
    </div>

    <!-- 日志填写区 -->
    <div class="log-form-card">
      <div class="card-title">📝 {{ logDate }} 工作日志</div>

      <el-form :model="logForm" label-width="100px" class="log-form">
        <el-form-item label="关联任务">
          <el-select v-model="logForm.taskId" placeholder="选择关联任务（可选）" clearable filterable>
            <el-option v-for="t in taskStore.tasks" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="🔵 今日进展">
          <el-input
            v-model="logForm.todayProgress"
            type="textarea"
            :rows="4"
            placeholder="今天完成了哪些工作？"
          />
        </el-form-item>

        <el-form-item label="🗓 明日计划">
          <el-input
            v-model="logForm.tomorrowPlan"
            type="textarea"
            :rows="4"
            placeholder="明天计划做什么？"
          />
        </el-form-item>

        <el-form-item label="⚠️ 阻碍/风险">
          <el-input
            v-model="logForm.blockers"
            type="textarea"
            :rows="2"
            placeholder="有遇到什么阻碍吗？（可选）"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSaveLog" :loading="saving">
            保存日志
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 历史日志列表 -->
    <div class="log-history">
      <div class="history-title">历史日志</div>

      <div v-loading="loading" class="log-list">
        <el-empty v-if="!logs.length" description="暂无历史日志" />

        <div v-for="log in logs" :key="log.id" class="log-item">
          <div class="log-item-header">
            <div class="log-meta">
              <span class="log-date">{{ log.date }}</span>
              <span v-if="log.taskId" class="log-task-tag">
                {{ getTaskTitle(log.taskId) }}
              </span>
            </div>
            <el-button size="small" text type="primary" @click="handleEdit(log)">编辑</el-button>
          </div>

          <div class="log-section">
            <span class="section-label">🔵 今日进展：</span>
            <span class="section-text">{{ log.todayProgress }}</span>
          </div>
          <div class="log-section">
            <span class="section-label">🗓 明日计划：</span>
            <span class="section-text">{{ log.tomorrowPlan }}</span>
          </div>
          <div v-if="log.blockers" class="log-section blockers">
            <span class="section-label">⚠️ 阻碍：</span>
            <span class="section-text">{{ log.blockers }}</span>
          </div>
        </div>
      </div>
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
  return taskStore.tasks.find(t => t.id === taskId)?.title || ''
}

async function fetchLogs() {
  const pid = projectStore.currentProjectId
  if (!pid) return
  loading.value = true
  try {
    const res = await workLogApi.list({ date: logDate.value })
    logs.value = res.data || []
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
      blockers: logForm.value.blockers,
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
}

function handleEdit(log: WorkLog) {
  logForm.value = {
    taskId: log.taskId || '',
    todayProgress: log.todayProgress,
    tomorrowPlan: log.tomorrowPlan,
    blockers: log.blockers || '',
  }
  logDate.value = log.date
}

watch([() => projectStore.currentProjectId, logDate], async ([pid]) => {
  if (pid) {
    await taskStore.fetchTasks(pid)
    await fetchLogs()
  }
}, { immediate: true })
</script>

<style scoped>
.worklog-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--surface-2);
  padding: 16px;
  gap: 16px;
  overflow-y: auto;
}
.worklog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--surface-1);
  padding: 12px 16px;
  border-radius: var(--radius-md);
}
.project-name { font-weight: 600; font-size: 15px; }
.log-form-card {
  background: var(--surface-1);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 16px;
}
.log-form {
  max-width: 680px;
}
.log-history { flex: 1; }
.history-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 12px;
}
.log-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.log-item {
  background: var(--surface-1);
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-sm);
}
.log-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.log-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.log-date {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
}
.log-task-tag {
  background: var(--accent-light);
  color: var(--accent);
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 11px;
}
.log-section {
  margin-bottom: 6px;
  font-size: 13px;
  line-height: 1.6;
}
.section-label {
  font-weight: 600;
  color: var(--text);
}
.section-text {
  color: var(--text-secondary);
}
.log-section.blockers .section-label { color: #ff4d4f; }
</style>
