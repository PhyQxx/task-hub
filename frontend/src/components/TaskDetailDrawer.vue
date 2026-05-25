<template>
  <el-drawer
    v-model="visible"
    :title="task?.title || '任务详情'"
    size="560px"
    direction="rtl"
    class="task-detail-drawer"
    :destroy-on-close="true"
    @closed="handleClosed"
  >
    <div v-if="loading" class="drawer-loading">
      <div class="spinner"></div>
    </div>
    
    <div v-else-if="task" class="drawer-body">
      <!-- Top Action Bar: Status & Priority -->
      <div class="drawer-section action-header">
        <div class="action-item">
          <span class="action-label">当前状态</span>
          <el-select v-model="form.status" placeholder="选择状态" @change="autoSave" class="adaptive-select">
            <template #prefix><span class="status-dot-mini" :class="'dot-' + form.status.toLowerCase()"></span></template>
            <el-option label="待处理" value="TODO" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已阻塞" value="BLOCKED" />
          </el-select>
        </div>
        <div class="action-item">
          <span class="action-label">优先级</span>
          <el-select v-model="form.priority" placeholder="选择优先级" @change="autoSave" class="adaptive-select">
            <el-option label="P0 紧急" value="URGENT" />
            <el-option label="P1 高" value="HIGH" />
            <el-option label="P2 中" value="MEDIUM" />
            <el-option label="P3 低" value="LOW" />
          </el-select>
        </div>
      </div>

      <!-- Title Section -->
      <div class="drawer-section title-section">
        <el-input
          v-model="form.title"
          placeholder="任务标题"
          class="huge-title-input"
          @blur="autoSave"
        />
      </div>

      <!-- Detail Grid -->
      <div class="drawer-section details-grid">
        <div class="grid-row">
          <div class="grid-label"><span class="icon">👤</span> 负责人</div>
          <div class="grid-content">
            <el-select v-model="form.assigneeId" placeholder="未分配" clearable @change="autoSave" class="full-width-select">
              <el-option v-for="m in memberStore.members" :key="m.memberId" :label="m.nickname" :value="m.memberId" />
            </el-select>
          </div>
        </div>

        <div class="grid-row">
          <div class="grid-label"><span class="icon">📅</span> 开始日期</div>
          <div class="grid-content">
            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="设置开始日期" @change="autoSave" class="full-width-select" />
          </div>
        </div>

        <div class="grid-row">
          <div class="grid-label"><span class="icon">🏁</span> 截止日期</div>
          <div class="grid-content">
            <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="设置截止日期" @change="autoSave" class="full-width-select" />
          </div>
        </div>

        <div class="grid-row">
          <div class="grid-label"><span class="icon">📈</span> 完成进度</div>
          <div class="grid-content slider-content">
            <el-slider v-model="form.progress" :min="0" :max="100" @change="autoSave" />
            <span class="progress-val">{{ form.progress }}%</span>
          </div>
        </div>
      </div>

      <!-- Description Section -->
      <div class="drawer-section desc-section">
        <div class="section-header">
          <span class="icon">📄</span> 任务描述
        </div>
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="6"
          placeholder="添加任务详情、背景或验收标准..."
          class="modern-textarea"
          @blur="autoSave"
        />
      </div>

      <!-- Discussion Section -->
      <div class="drawer-section comment-section">
        <div class="section-header">
          <span class="icon">💬</span> 讨论
        </div>
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
          <span v-else-if="saveError" class="save-status error" key="error">
            <span class="error-icon">⚠️</span> 同步失败 <button class="retry-btn" @click="autoSave">重试</button>
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
const saveError = ref(false)
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
  saveError.value = false
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
  saveError.value = false
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
    saveError.value = true
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
.task-detail-drawer :deep(.el-drawer__body) {
  padding: 0; /* Let body manage padding for better grouping */
}

.drawer-loading { display: flex; justify-content: center; padding: 100px 0; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--surface-3); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.drawer-body { 
  display: flex; 
  flex-direction: column; 
  height: 100%;
  overflow-y: auto;
  padding: 24px 32px;
  gap: 32px;
}

.drawer-section { display: flex; flex-direction: column; }

/* Action Header */
.action-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-subtle);
}

.action-item { display: flex; flex-direction: column; gap: 8px; }
.action-label { font-size: 11px; font-weight: 700; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.5px; }

.adaptive-select { width: 100%; }
.status-dot-mini { width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 4px; }
.dot-todo { background: var(--text-faint); }
.dot-in_progress { background: var(--primary); }
.dot-done { background: var(--success); }
.dot-blocked { background: var(--danger); }

/* Title Section */
.huge-title-input :deep(.el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  padding: 0 !important;
}

.huge-title-input :deep(.el-input__inner) {
  font-size: 26px;
  font-weight: 800;
  color: var(--text);
  height: auto;
  line-height: 1.2;
}

/* Details Grid */
.details-grid {
  background: var(--surface-1);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  padding: 8px 0;
}

.grid-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  align-items: center;
  padding: 10px 20px;
  min-height: 48px;
}

.grid-row:not(:last-child) {
  border-bottom: 1px solid var(--border-subtle);
}

.grid-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.grid-content {
  display: flex;
  align-items: center;
}

.full-width-select { width: 100%; }
.full-width-select :deep(.el-input__wrapper) { background: transparent !important; box-shadow: none !important; }

.slider-content { gap: 16px; }
.slider-content :deep(.el-slider) { flex: 1; }
.progress-val { font-size: 13px; font-weight: 700; color: var(--text-secondary); min-width: 40px; text-align: right; }

/* Description & Comments */
.section-header {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.modern-textarea :deep(.el-textarea__inner) {
  background: var(--surface-1) !important;
  border: 1px solid var(--border) !important;
  color: var(--text-secondary) !important;
  font-size: 14px;
  line-height: 1.6;
  padding: 16px !important;
  border-radius: var(--radius-md) !important;
}

/* Footer */
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
.save-status.error { color: var(--danger); }

.retry-btn {
  background: var(--danger-bg);
  color: var(--danger);
  border: 1px solid var(--danger);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  margin-left: 4px;
}

.dot-pulse {
  width: 6px;
  height: 6px;
  background: var(--primary);
  border-radius: 50%;
  animation: pulse 1.2s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.9); opacity: 0.7; }
  50% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.7; }
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* Overrides */
:deep(.el-drawer) { background: var(--bg) !important; box-shadow: var(--shadow-lg) !important; }
:deep(.el-drawer__header) { margin: 0; padding: 16px 32px; border-bottom: 1px solid var(--border-subtle); }
:deep(.el-drawer__title) { font-size: 13px; font-weight: 700; color: var(--text-faint); text-transform: uppercase; }
:deep(.el-drawer__footer) { border-top: 1px solid var(--border-subtle); padding: 16px 32px; background: var(--surface-1); }
</style>
