<template>
  <div class="kanban-view">
    <!-- Stats row -->
    <div class="stats-row">
      <div class="stat-card"><div class="stat-label">总任务</div><div class="stat-value">{{ taskStore.tasks.length }}</div></div>
      <div class="stat-card"><div class="stat-label">进行中</div><div class="stat-value" style="color:var(--primary)">{{ countByStatus('IN_PROGRESS') }}</div></div>
      <div class="stat-card"><div class="stat-label">已完成</div><div class="stat-value" style="color:var(--success)">{{ countByStatus('DONE') }}</div></div>
      <div class="stat-card"><div class="stat-label">已阻塞</div><div class="stat-value" style="color:var(--danger)">{{ countByStatus('BLOCKED') }}</div></div>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <button class="btn btn-ghost">今天</button>
      <button class="btn btn-ghost">周</button>
      <button class="btn btn-primary" style="font-size:12px">月</button>
      <button class="btn btn-ghost">季</button>
      <span class="toolbar-info">{{ projectStore.currentProject?.name || '所有项目' }} · {{ new Date().getMonth()+1 }}月</span>
    </div>

    <!-- Board -->
    <div class="kanban-board">
      <div v-for="col in columns" :key="col.key" class="kanban-col">
        <div class="kanban-col-header">
          <span class="status-dot" :class="'status-' + col.dotClass"></span>
          <span class="kanban-col-title">{{ col.label }}</span>
          <span class="kanban-col-count">{{ getTasks(col.key).length }}</span>
        </div>
        <div class="kanban-cards">
          <div
            v-for="task in getTasks(col.key)"
            :key="task.id"
            class="kanban-card"
            :class="{ 'blocked-card': col.key === 'BLOCKED', 'selected-card': selectedTasks.has(task.id || task.taskId) }"
            @click="openTask(task)"
            @contextmenu.prevent="showCtxMenu($event, task)"
          >
            <div class="card-select-wrap" @click.stop>
              <input
                type="checkbox"
                class="card-checkbox"
                :checked="selectedTasks.has(task.id || task.taskId)"
                @change="toggleSelect(task)"
              />
            </div>
            <div class="kanban-card-title">{{ task.title }}</div>
            <div class="kanban-card-meta">
              <span class="tag" :class="priorityTag(task.priority)">{{ priorityLabel(task.priority) }}</span>
              <span v-if="task.assigneeName" class="assignee-chip">
                <span class="av-dot" :class="assigneeAvClass(task.assigneeName)"></span>
                {{ task.assigneeName }}
              </span>
              <span v-if="task.endDate" style="margin-left:auto;font-size:10px;color:var(--text-muted)">
                {{ task.endDate }}
              </span>
            </div>
          </div>
          <div v-if="!getTasks(col.key).length" class="kanban-empty">暂无任务</div>
        </div>
      </div>
    </div>

    <!-- Edit Task Modal -->
    <div v-if="showEdit" class="modal-overlay" @click.self="showEdit = false">
      <div class="modal" style="width:520px">
        <div class="modal-title">{{ editingTask ? '编辑任务' : '+ 新建任务' }}</div>
        <div class="modal-field">
          <label class="form-label">任务名称 *</label>
          <input class="form-input" v-model="form.title" placeholder="请输入任务名称" />
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
          <div class="modal-field">
            <label class="form-label">状态</label>
            <select class="form-input" v-model="form.status">
              <option v-for="col in columns" :key="col.key" :value="col.key">{{ col.label }}</option>
            </select>
          </div>
          <div class="modal-field">
            <label class="form-label">负责人</label>
            <select class="form-input" v-model="form.assigneeId">
              <option value="">未分配</option>
              <option v-for="m in memberStore.members" :key="m.id" :value="m.id">{{ m.name }}</option>
            </select>
          </div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
          <div class="modal-field">
            <label class="form-label">优先级</label>
            <select class="form-input" v-model="form.priority">
              <option value="LOW">低</option>
              <option value="MEDIUM">中</option>
              <option value="HIGH">高</option>
              <option value="URGENT">紧急</option>
            </select>
          </div>
          <div class="modal-field">
            <label class="form-label">截止日期</label>
            <input class="form-input" type="date" v-model="form.endDate" />
          </div>
        </div>
        <div class="modal-field">
          <label class="form-label">描述</label>
          <textarea class="form-textarea" v-model="form.description" style="height:60px"></textarea>
        </div>
        <div class="modal-actions">
          <button v-if="editingTask" class="btn btn-ghost" style="color:var(--danger)" @click="handleDelete">删除</button>
          <div style="flex:1"></div>
          <button class="btn btn-ghost" @click="showEdit = false">取消</button>
          <button class="btn btn-primary" @click="handleSave">{{ editingTask ? '保存' : '创建' }}</button>
        </div>
      </div>
    </div>

    <!-- Context Menu -->
    <div
      v-if="ctxVisible"
      class="ctx-menu show"
      :style="{ left: ctxX + 'px', top: ctxY + 'px' }"
      @click.stop
    >
      <div class="ctx-menu-item" @click="ctxChangeStatus('TODO')">○ 待办</div>
      <div class="ctx-menu-item" @click="ctxChangeStatus('IN_PROGRESS')">◐ 进行中</div>
      <div class="ctx-menu-item" @click="ctxChangeStatus('DONE')">✓ 已完成</div>
      <div class="ctx-menu-item" @click="ctxChangeStatus('BLOCKED')">⚠ 已阻塞</div>
      <div class="ctx-menu-sep"></div>
      <div class="ctx-menu-item" @click="ctxEdit">✏️ 编辑</div>
      <div class="ctx-menu-sep"></div>
      <div class="ctx-menu-item danger" @click="ctxDelete">🗑 删除</div>
    </div>

    <!-- Batch Action Toolbar -->
    <div v-if="selectedTasks.size > 0" class="batch-toolbar">
      <span class="batch-info">已选择 {{ selectedTasks.size }} 项</span>
      <button class="btn btn-primary" style="font-size:12px" @click="showBatchStatus = true">批量更新状态</button>
      <button class="btn btn-ghost" style="font-size:12px" @click="showBatchAssign = true">批量指派</button>
      <button class="btn btn-ghost danger" style="font-size:12px" @click="handleBatchDelete">批量删除</button>
      <button class="btn btn-ghost" style="font-size:12px;margin-left:auto" @click="selectedTasks.clear()">取消</button>
    </div>

    <!-- Batch Status Dialog -->
    <el-dialog v-model="showBatchStatus" title="批量更新状态" width="400px" :append-to-body="true">
      <el-form-item label="新状态">
        <el-select v-model="batchStatus" style="width:100%">
          <el-option v-for="col in columns" :key="col.key" :label="col.label" :value="col.key" />
        </el-select>
      </el-form-item>
      <template #footer>
        <el-button @click="showBatchStatus = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="handleBatchStatus">确认更新</el-button>
      </template>
    </el-dialog>

    <!-- Batch Assign Dialog -->
    <el-dialog v-model="showBatchAssign" title="批量指派负责人" width="400px" :append-to-body="true">
      <el-form-item label="负责人">
        <el-select v-model="batchAssigneeId" placeholder="选择负责人（留空则取消指派）" clearable style="width:100%">
          <el-option v-for="m in memberStore.members" :key="m.id" :label="m.name" :value="m.id" />
        </el-select>
      </el-form-item>
      <template #footer>
        <el-button @click="showBatchAssign = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="handleBatchAssign">确认指派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useProjectStore, useTaskStore, useMemberStore } from '@/stores'
import type { Task } from '@/types'
import { ElMessage } from 'element-plus'
import { taskApi } from '@/api'

const projectStore = useProjectStore()
const taskStore = useTaskStore()
const memberStore = useMemberStore()

const columns = [
  { key: 'TODO', label: '待处理', dotClass: 'pending' },
  { key: 'IN_PROGRESS', label: '进行中', dotClass: 'progress' },
  { key: 'DONE', label: '已完成', dotClass: 'done' },
  { key: 'BLOCKED', label: '已阻塞', dotClass: 'blocked' },
]

function getTasks(status: string) {
  return taskStore.tasks.filter(t => t.status === status)
}

function countByStatus(status: string) {
  return taskStore.tasks.filter(t => t.status === status).length
}

function priorityTag(p: string) {
  return { URGENT: 'tag-p0', HIGH: 'tag-p1', MEDIUM: 'tag-p2', LOW: 'tag-p2' }[p] || 'tag-p2'
}
function priorityLabel(p: string) {
  return { URGENT: 'P0', HIGH: 'P1', MEDIUM: 'P2', LOW: 'P2' }[p] || 'P2'
}
function assigneeAvClass(name: string) {
  const map: Record<string, string> = { 'Dev': 'av-dev', 'Des': 'av-des', 'QA': 'av-qa', 'Ops': 'av-ops', 'PD': 'av-pm' }
  return map[name] || 'av-dev'
}

// Edit modal
const showEdit = ref(false)
const editingTask = ref<Task | null>(null)
const form = ref({ title:'', status:'TODO', assigneeId:'', priority:'MEDIUM', endDate:'', description:'' })

// Batch operations
const selectedTasks = ref(new Set<string>())
const showBatchStatus = ref(false)
const showBatchAssign = ref(false)
const batchStatus = ref('TODO')
const batchAssigneeId = ref('')
const batchLoading = ref(false)

function toggleSelect(task: any) {
  const id = task.id || task.taskId
  if (selectedTasks.value.has(id)) {
    selectedTasks.value.delete(id)
  } else {
    selectedTasks.value.add(id)
  }
}

async function handleBatchStatus() {
  if (selectedTasks.value.size === 0) return
  batchLoading.value = true
  try {
    await taskApi.batchUpdate({
      taskIds: Array.from(selectedTasks.value),
      status: batchStatus.value,
    })
    ElMessage.success(`已更新 ${selectedTasks.value.size} 个任务的状态`)
    selectedTasks.value.clear()
    showBatchStatus.value = false
    await taskStore.fetchTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '批量更新失败')
  } finally {
    batchLoading.value = false
  }
}

async function handleBatchDelete() {
  if (selectedTasks.value.size === 0) return
  if (!confirm(`确认删除选中的 ${selectedTasks.value.size} 个任务？`)) return
  try {
    for (const id of selectedTasks.value) {
      await taskApi.delete(id)
    }
    ElMessage.success(`已删除 ${selectedTasks.value.size} 个任务`)
    selectedTasks.value.clear()
    await taskStore.fetchTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '批量删除失败')
  }
}

async function handleBatchAssign() {
  if (selectedTasks.value.size === 0) return
  batchLoading.value = true
  try {
    await taskApi.batchUpdate({
      taskIds: Array.from(selectedTasks.value),
      assigneeId: batchAssigneeId.value || undefined,
    })
    ElMessage.success(`已指派 ${selectedTasks.value.size} 个任务`)
    selectedTasks.value.clear()
    showBatchAssign.value = false
    batchAssigneeId.value = ''
    await taskStore.fetchTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '批量指派失败')
  } finally {
    batchLoading.value = false
  }
}


function openTask(task?: Task) {
  editingTask.value = task || null
  form.value = task ? {
    title: task.title,
    status: task.status,
    assigneeId: task.assigneeId || '',
    priority: task.priority,
    endDate: task.endDate || '',
    description: task.description || '',
  } : { title:'', status:'TODO', assigneeId:'', priority:'MEDIUM', endDate:'', description:'' }
  showEdit.value = true
}

async function handleSave() {
  if (!form.value.title) { ElMessage.warning('请输入任务名称'); return }
  const projectId = projectStore.currentProjectId
  if (!projectId) { ElMessage.warning('请先选择项目'); return }
  try {
    if (editingTask.value) {
      await taskStore.updateTask(editingTask.value.id, { projectId, ...form.value })
    } else {
      await taskStore.createTask({ projectId, ...form.value })
    }
    showEdit.value = false
    await taskStore.fetchTasks(projectId)
  } catch(e) {
    ElMessage.error('保存失败')
  }
}

async function handleDelete() {
  if (!editingTask.value) return
  try {
    await taskStore.deleteTask(editingTask.value.id)
    showEdit.value = false
    await taskStore.fetchTasks(projectStore.currentProjectId)
  } catch(e) {
    ElMessage.error('删除失败')
  }
}

// Context menu
const ctxVisible = ref(false)
const ctxX = ref(0)
const ctxY = ref(0)
const ctxTask = ref<Task | null>(null)

function showCtxMenu(e: MouseEvent, task: Task) {
  ctxTask.value = task
  ctxX.value = Math.min(e.clientX, window.innerWidth - 200)
  ctxY.value = Math.min(e.clientY, window.innerHeight - 300)
  ctxVisible.value = true
}

function hideCtx() { ctxVisible.value = false }

async function ctxChangeStatus(status: string) {
  if (!ctxTask.value) return
  await taskStore.updateTask(ctxTask.value.id, { status } as any)
  hideCtx()
  await taskStore.fetchTasks(projectStore.currentProjectId)
}

function ctxEdit() {
  if (!ctxTask.value) return
  openTask(ctxTask.value)
  hideCtx()
}

async function ctxDelete() {
  if (!ctxTask.value) return
  await taskStore.deleteTask(ctxTask.value.id)
  hideCtx()
  await taskStore.fetchTasks(projectStore.currentProjectId)
}

document.addEventListener('click', hideCtx)

watch(() => projectStore.currentProjectId, async (id) => {
  await taskStore.fetchTasks(id || '')
}, { immediate: true })
</script>

<style scoped>
.kanban-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 24px;
  gap: 16px;
  overflow: auto;
}

/* Stats */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 160px);
  gap: 8px;
}

/* Toolbar */
.toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.toolbar-info {
  font-size: 13px;
  font-weight: 400;
  color: var(--text-faint);
  margin-left: auto;
}

/* Board */
.kanban-board {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  flex: 1;
  min-height: 0;
  align-items: start;
}
.kanban-col {
  background: var(--surface-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-strong);
  min-height: 400px;
  display: flex;
  flex-direction: column;
}
.kanban-col-header {
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 8px;
}
.kanban-col-title { font-size: 13px; font-weight: 590; }
.kanban-col-count {
  font-size: 11px;
  background: rgba(0,0,0,0.06);
  padding: 2px 7px;
  border-radius: 6px;
  color: var(--text-faint);
}
.kanban-cards {
  flex: 1;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  max-height: calc(100vh - 340px);
}
.kanban-card {
  background: var(--surface-4);
  border-radius: var(--radius-md);
  padding: 10px;
  border: 1px solid var(--border-strong);
  cursor: pointer;
  transition: all 0.2s;
}
.kanban-card:hover { background: var(--surface-5); }
.blocked-card { border-left: 3px solid var(--danger); }
.kanban-card-title {
  font-size: 13px;
  font-weight: 400;
  margin-bottom: 6px;
  line-height: 1.4;
  color: var(--text-secondary);
}
.kanban-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  flex-wrap: wrap;
}
.kanban-empty {
  text-align: center;
  padding: 24px 0;
  color: var(--text-faint);
  font-size: 13px;
}

/* Assignee avatar dot */
.av-dot {
  width: 14px; height: 14px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 8px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}
.av-dev { background: #34D399; }
.av-des { background: #F472B6; }
.av-qa { background: #FBBF24; }
.av-ops { background: #60A5FA; }
.av-pm { background: #818CF8; }

/* Modal */
.modal-field { margin-bottom: 10px; }
.modal-actions { display: flex; gap: 8px; align-items: center; margin-top: 14px; }

/* Batch toolbar */
.batch-toolbar {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--surface-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.3);
  z-index: 100;
  backdrop-filter: blur(10px);
}
.batch-info {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}
.btn.danger { color: var(--danger) !important; }

/* Card checkbox */
.card-select-wrap {
  position: absolute;
  top: 8px;
  left: 8px;
  opacity: 0;
  transition: opacity 0.15s;
}
.kanban-card:hover .card-select-wrap,
.kanban-card.selected-card .card-select-wrap {
  opacity: 1;
}
.card-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--primary);
}
.kanban-card {
  position: relative;
}
.kanban-card.selected-card {
  border-color: var(--primary) !important;
  background: color-mix(in srgb, var(--primary) 8%, var(--surface-1));
}
</style>
