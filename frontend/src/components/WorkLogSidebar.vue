<template>
  <aside class="worklog-sidebar" :class="{ 'is-collapsed': !visible }">
    <div class="sidebar-header">
      <div class="header-content">
        <span class="header-icon">📝</span>
        <span class="header-title">任务日志</span>
      </div>
      <button class="icon-btn" @click="$emit('close')">✕</button>
    </div>

    <div v-if="!taskId" class="empty-ctx">
      <div class="empty-icon">🖱️</div>
      <p>在左侧点击任务<br/>开始记录日志</p>
    </div>

    <div v-else class="sidebar-body">
      <div class="ctx-task-info">
        <label>正在记录</label>
        <h3>{{ taskTitle }}</h3>
      </div>

      <el-form label-position="top" class="log-form">
        <el-form-item label="🔵 今日进展">
          <el-input
            v-model="form.todayDone"
            type="textarea"
            :rows="4"
            placeholder="完成了哪些工作？"
            @blur="autoSave"
          />
        </el-form-item>

        <el-form-item label="🗓 明日计划">
          <el-input
            v-model="form.tomorrowPlan"
            type="textarea"
            :rows="3"
            placeholder="接下来的安排..."
            @blur="autoSave"
          />
        </el-form-item>

        <el-form-item label="⚠️ 风险与阻碍">
          <el-input
            v-model="form.blockedReason"
            type="textarea"
            :rows="2"
            placeholder="有无进度卡点？"
            @blur="autoSave"
          />
        </el-form-item>
      </el-form>

      <div class="log-history-mini">
        <div class="history-title">近期记录</div>
        <div v-if="loading" class="mini-loading">加载中...</div>
        <div v-else-if="!history.length" class="mini-empty">暂无历史记录</div>
        <div v-else class="history-items">
          <div v-for="item in history.slice(0, 3)" :key="item.id" class="history-item">
            <div class="item-meta">
              <span class="item-date">{{ item.date }}</span>
            </div>
            <p class="item-text">{{ item.todayDone }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="sidebar-footer" v-if="taskId">
      <Transition name="fade">
        <span v-if="saving" class="status-msg"><span class="dot-pulse"></span> 正在保存...</span>
        <span v-else class="status-msg success">✓ 已自动同步</span>
      </Transition>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, watch, reactive, computed } from 'vue'
import { useTaskStore } from '@/stores'
import { workLogApi } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const props = defineProps<{
  taskId: string | null
  visible: boolean
}>()

const emit = defineEmits(['close'])

const taskStore = useTaskStore()
const loading = ref(false)
const saving = ref(false)
const history = ref<any[]>([])

const taskTitle = computed(() => {
  if (!props.taskId) return ''
  const t = taskStore.tasks.find(x => String(x.taskId || x.id) === String(props.taskId))
  return t?.title || '未知任务'
})

const form = reactive({
  todayDone: '',
  tomorrowPlan: '',
  blockedReason: '',
})

watch(() => props.taskId, (newId) => {
  if (newId) {
    loadTodayLog()
    loadHistory()
  } else {
    resetForm()
  }
})

async function loadTodayLog() {
  if (!props.taskId) return
  try {
    const today = dayjs().format('YYYY-MM-DD')
    const res = await workLogApi.list({ taskId: props.taskId, date: today })
    const data = res.data as any
    const logs = data?.data || data || []
    if (logs.length) {
      const log = logs[0]
      form.todayDone = log.todayDone || ''
      form.tomorrowPlan = log.tomorrowPlan || ''
      form.blockedReason = log.blockedReason || ''
    } else {
      resetForm()
    }
  } catch (e) {
    console.error('Failed to load today log', e)
  }
}

async function loadHistory() {
  if (!props.taskId) return
  loading.value = true
  try {
    const res = await workLogApi.list({ taskId: props.taskId })
    const data = res.data as any
    history.value = data?.data || data || []
  } catch {
    history.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.todayDone = ''
  form.tomorrowPlan = ''
  form.blockedReason = ''
}

async function autoSave() {
  if (!props.taskId || !form.todayDone.trim()) return
  saving.value = true
  try {
    await workLogApi.create({
      taskId: props.taskId,
      logDate: dayjs().format('YYYY-MM-DD'),
      todayDone: form.todayDone,
      tomorrowPlan: form.tomorrowPlan,
      blockedReason: form.blockedReason,
    })
    loadHistory()
  } catch (e) {
    console.error('WorkLog auto-save failed', e)
  } finally {
    setTimeout(() => { saving.value = false }, 800)
  }
}
</script>

<style scoped>
.worklog-sidebar {
  width: 320px;
  background: var(--surface-1);
  border-left: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 100;
  flex-shrink: 0;
}

.worklog-sidebar.is-collapsed {
  width: 0;
  border-left: none;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content { display: flex; align-items: center; gap: 8px; }
.header-icon { font-size: 16px; }
.header-title { font-size: 13px; font-weight: 700; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }

.icon-btn { background: transparent; border: none; cursor: pointer; color: var(--text-faint); font-size: 14px; padding: 4px; border-radius: 4px; }
.icon-btn:hover { background: var(--surface-3); color: var(--text); }

.sidebar-body { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 24px; }

.ctx-task-info label { font-size: 10px; font-weight: 800; color: var(--primary); text-transform: uppercase; }
.ctx-task-info h3 { font-size: 16px; font-weight: 700; color: var(--text); margin: 4px 0 0; line-height: 1.4; }

.log-form :deep(.el-form-item__label) { font-size: 11px; font-weight: 700; color: var(--text-faint); margin-bottom: 4px; text-transform: uppercase; }
.log-form :deep(.el-textarea__inner) { background: var(--surface-2) !important; border: 1px solid var(--border) !important; color: var(--text) !important; font-size: 13px; }

.log-history-mini { border-top: 1px solid var(--border-subtle); padding-top: 20px; }
.history-title { font-size: 11px; font-weight: 700; color: var(--text-faint); text-transform: uppercase; margin-bottom: 12px; }

.history-items { display: flex; flex-direction: column; gap: 12px; }
.history-item { background: var(--surface-2); padding: 10px; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); }
.item-date { font-size: 10px; font-weight: 700; color: var(--text-muted); }
.item-text { font-size: 12px; color: var(--text-secondary); margin: 4px 0 0; line-height: 1.5; white-space: pre-wrap; }

.mini-loading, .mini-empty, .empty-ctx { padding: 40px 0; text-align: center; color: var(--text-faint); font-size: 12px; }
.empty-icon { font-size: 32px; margin-bottom: 12px; opacity: 0.3; }

.sidebar-footer { padding: 12px 20px; border-top: 1px solid var(--border-subtle); background: var(--surface-2); }
.status-msg { font-size: 11px; font-weight: 600; color: var(--text-faint); display: flex; align-items: center; gap: 6px; }
.status-msg.success { color: var(--success); }

.dot-pulse { width: 6px; height: 6px; background: var(--primary); border-radius: 50%; animation: pulse 1.2s infinite; }
@keyframes pulse { 0% { transform: scale(0.9); opacity: 0.7; } 50% { transform: scale(1.2); opacity: 1; } 100% { transform: scale(0.9); opacity: 0.7; } }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
