<template>
  <el-drawer
    v-model="visible"
    :title="task?.title || '任务详情'"
    size="520px"
    direction="rtl"
    class="task-detail-drawer"
    :destroy-on-close="true"
    @closed="handleClosed"
  >
    <div v-if="loading" class="drawer-loading">
      <div class="spinner"></div>
    </div>
    
    <div v-else-if="task" class="drawer-body">
      <!-- Status & Priority Header -->
      <div class="drawer-section header-actions">
        <div class="action-group">
          <label>状态</label>
          <el-select v-model="form.status" size="small" class="status-select" @change="autoSave">
            <el-option label="待处理" value="TODO" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已阻塞" value="BLOCKED" />
          </el-select>
        </div>
        <div class="action-group">
          <label>优先级</label>
          <el-select v-model="form.priority" size="small" class="priority-select" @change="autoSave">
            <el-option label="P0 紧急" value="URGENT" />
            <el-option label="P1 高" value="HIGH" />
            <el-option label="P2 中" value="MEDIUM" />
            <el-option label="P3 低" value="LOW" />
          </el-select>
        </div>
      </div>

      <!-- Title Edit -->
      <div class="drawer-section">
        <input 
          v-model="form.title" 
          class="title-input" 
          placeholder="任务名称" 
          @blur="autoSave"
        />
      </div>

      <!-- Main Fields Grid -->
      <div class="drawer-section fields-grid">
        <div class="field-item">
          <label><span class="icon">👤</span> 负责人</label>
          <el-select v-model="form.assigneeId" placeholder="未分配" clearable size="small" @change="autoSave">
            <el-option v-for="m in memberStore.members" :key="m.memberId" :label="m.nickname" :value="m.memberId" />
          </el-select>
        </div>
        <div class="field-item">
          <label><span class="icon">📅</span> 开始日期</label>
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" size="small" @change="autoSave" />
        </div>
        <div class="field-item">
          <label><span class="icon">🏁</span> 截止日期</label>
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" size="small" @change="autoSave" />
        </div>
        <div class="field-item">
          <label><span class="icon">📈</span> 进度 ({{ form.progress }}%)</label>
          <el-slider v-model="form.progress" :min="0" :max="100" @change="autoSave" />
        </div>
      </div>

      <!-- Description -->
      <div class="drawer-section">
        <label class="section-label">描述</label>
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="添加更详细的描述..."
          class="desc-textarea"
          @blur="autoSave"
        />
      </div>

      <!-- Comments -->
      <div class="drawer-section comments-section">
        <label class="section-label">讨论</label>
        <TaskComments :task-id="String(task.taskId || task.id)" />
      </div>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <button class="btn btn-ghost text-danger" @click="handleDelete">🗑 删除任务</button>
        <div class="spacer"></div>
        <Transition name="fade" mode="out-in">
          <span v-if="saving" class="save-status" key="saving">
            <span class="dot-pulse"></span> 正在保存...
          </span>
          <span v-else class="save-status success" key="saved">
            <span class="check-icon">✓</span> 已同步
          </span>
        </Transition>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import { useTaskStore, useMemberStore } from '@/stores'
import { taskApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import TaskComments from './TaskComments.vue'

const props = defineProps<{
  taskId: string | null
  visible: boolean
}>()

const emit = defineEmits(['update:visible', 'updated', 'deleted'])

const taskStore = useTaskStore()
const memberStore = useMemberStore()

const visible = ref(props.visible)
const loading = ref(false)
const saving = ref(false)
const task = ref<any>(null)

const form = reactive({
  title: '',
  status: 'TODO',
  priority: 'MEDIUM',
  assigneeId: '',
  startDate: '',
  endDate: '',
  description: '',
  progress: 0,
})

watch(() => props.visible, (v) => {
  visible.value = v
  if (v && props.taskId) {
    loadTask()
  }
})

watch(visible, (v) => {
  emit('update:visible', v)
})

async function loadTask() {
  if (!props.taskId) return
  loading.value = true
  try {
    const t = taskStore.tasks.find(x => String(x.taskId || x.id) === String(props.taskId))
    if (t) {
      task.value = t
      Object.assign(form, {
        title: t.title,
        status: t.status,
        priority: t.priority,
        assigneeId: t.assigneeId || '',
        startDate: t.startDate || '',
        endDate: t.endDate || '',
        description: t.description || '',
        progress: t.progress || 0,
      })
    }
  } catch (e) {
    ElMessage.error('加载任务失败')
  } finally {
    loading.value = false
  }
}

async function autoSave() {
  if (!task.value || !props.taskId) return
  saving.value = true
  try {
    await taskApi.update(props.taskId, {
      title: form.title,
      status: form.status,
      priority: form.priority,
      assigneeId: form.assigneeId || undefined,
      startDate: form.startDate || undefined,
      endDate: form.endDate || undefined,
      description: form.description,
      progress: form.progress,
    })
    emit('updated')
  } catch (e) {
    console.error('Auto save failed', e)
  } finally {
    setTimeout(() => { saving.value = false }, 800)
  }
}

async function handleDelete() {
  if (!props.taskId) return
  try {
    await ElMessageBox.confirm('确定要删除这个任务吗？此操作不可撤销。', '删除任务', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
    await taskApi.delete(props.taskId)
    ElMessage.success('任务已删除')
    visible.value = false
    emit('deleted')
  } catch {
    // cancelled
  }
}

function handleClosed() {
  task.value = null
}
</script>

<style scoped>
.drawer-loading { display: flex; justify-content: center; padding: 100px 0; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--surface-3); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.drawer-body { display: flex; flex-direction: column; gap: 24px; padding: 10px 4px; }

.drawer-section { display: flex; flex-direction: column; gap: 8px; }

.header-actions { flex-direction: row; gap: 24px; border-bottom: 1px solid var(--border-subtle); padding-bottom: 20px; }
.action-group { display: flex; flex-direction: column; gap: 6px; }
.action-group label { font-size: 11px; font-weight: 700; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.5px; }

.title-input {
  background: transparent;
  border: none;
  border-radius: 4px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
  padding: 8px 0;
  width: 100%;
  outline: none;
  transition: all 0.2s;
}
.title-input:focus { background: var(--surface-2); padding-left: 8px; margin-left: -8px; }

.fields-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  background: var(--surface-1);
  padding: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
}

.field-item { display: flex; flex-direction: column; gap: 8px; }
.field-item label { font-size: 12px; font-weight: 600; color: var(--text-secondary); display: flex; align-items: center; gap: 6px; }
.icon { font-size: 14px; opacity: 0.7; }

.section-label { font-size: 13px; font-weight: 700; color: var(--text-secondary); margin-bottom: 4px; }

.drawer-footer { display: flex; align-items: center; width: 100%; }
.spacer { flex: 1; }

.save-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-faint);
}

.save-status.success { color: var(--success); }

.dot-pulse {
  width: 6px;
  height: 6px;
  background: var(--primary);
  border-radius: 50%;
  animation: pulse 1.2s infinite;
}

.check-icon { font-size: 14px; }

@keyframes pulse {
  0% { transform: scale(0.9); opacity: 0.7; }
  50% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.7; }
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* Overrides for Element Plus in dark mode */
:deep(.el-drawer) { background: var(--surface-2) !important; color: var(--text) !important; border-left: 1px solid var(--border-strong); }
:deep(.el-drawer__header) { margin-bottom: 0; padding: 20px 24px; border-bottom: 1px solid var(--border-subtle); }
:deep(.el-drawer__title) { font-size: 14px; font-weight: 700; color: var(--text-faint); text-transform: uppercase; }
:deep(.el-drawer__body) { padding: 24px; }
:deep(.el-drawer__footer) { border-top: 1px solid var(--border-subtle); padding: 16px 24px; }

:deep(.el-textarea__inner) { background: var(--surface-1) !important; border: 1px solid var(--border) !important; color: var(--text) !important; box-shadow: none !important; }
:deep(.el-textarea__inner:focus) { border-color: var(--primary) !important; }

:deep(.el-select .el-input__wrapper) { background: var(--surface-1) !important; border: 1px solid var(--border) !important; box-shadow: none !important; }
:deep(.el-date-editor.el-input__wrapper) { background: var(--surface-1) !important; border: 1px solid var(--border) !important; box-shadow: none !important; }
</style>
