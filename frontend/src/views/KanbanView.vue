<template>
  <div class="kanban-view">
    <StatsBar />

    <!-- Filters & Toolbar -->
    <div class="view-toolbar">
      <div class="toolbar-left">
        <div class="filter-group">
          <el-input v-model="filterKeyword" placeholder="搜索任务..." clearable size="small" class="search-input" />
          <el-select v-model="filterAssignee" placeholder="负责人" clearable size="small" class="filter-select">
            <el-option v-for="m in memberStore.members" :key="m.memberId" :label="m.nickname" :value="m.memberId" />
          </el-select>
          <div class="toolbar-sep"></div>
          <div class="dimension-toggle">
            <button 
              class="toggle-btn" 
              :class="{ active: dimension === 'status' }" 
              @click="dimension = 'status'"
            >
              📊 状态
            </button>
            <button 
              class="toggle-btn" 
              :class="{ active: dimension === 'member' }" 
              @click="dimension = 'member'"
            >
              👥 成员
            </button>
          </div>
        </div>
      </div>
      <div class="toolbar-right">
        <span class="toolbar-info">{{ projectStore.currentProject?.name || '所有项目' }}</span>
        <el-button type="primary" size="small" @click="openTask()">+ 新建任务</el-button>
      </div>
    </div>

    <SkeletonBoard v-if="taskStore.loading" type="kanban" :cols="columns.length" />
    <div v-else class="kanban-board" :style="{ gridTemplateColumns: `repeat(${columns.length}, 1fr)` }">
      <div 
        v-for="col in columns" 
        :key="col.key" 
        class="kanban-col"
        :class="{ 'is-drag-over': dragOverCol === col.key }"
      >
        <div class="kanban-col-header">
          <el-avatar v-if="dimension === 'member' && col.key !== 'unassigned'" :size="20" class="col-av">
            {{ col.label.slice(0,1) }}
          </el-avatar>
          <span v-else-if="dimension === 'status'" class="status-dot" :class="'status-' + col.dotClass"></span>
          <span class="col-title">{{ col.label }}</span>
          <span class="col-count">{{ getTasks(col.key).length }}</span>
        </div>
        
        <div
          class="kanban-cards"
          @dragover.prevent
          @drop="onDrop(col.key)"
        >
          <div
            v-for="task in getTasks(col.key)"
            :key="task.id"
            class="kanban-card"
            :class="{ 
              'is-blocked': task.status === 'BLOCKED', 
              'is-selected': selectedTasks.has(String(task.taskId || task.id)),
              'is-dragging': dragTaskId === String(task.taskId || task.id)
            }"
            draggable="true"
            @click="openTask(task)"
            @contextmenu.prevent="showCtxMenu($event, task)"
            @dragstart="onDragStart(task, col.key)"
            @dragend="onDragEnd"
          >
            <div class="card-selection" @click.stop>
              <input 
                type="checkbox" 
                :checked="selectedTasks.has(String(task.taskId || task.id))" 
                @change="toggleSelect(task)" 
              />
            </div>
            
            <div class="card-priority" :class="'p-' + task.priority.toLowerCase()"></div>
            
            <div class="card-content">
              <div class="card-title">{{ task.title }}</div>
              <div class="card-meta">
                <div class="meta-left">
                  <span class="priority-badge" :class="priorityTag(task.priority)">
                    {{ priorityLabel(task.priority) }}
                  </span>
                </div>
                <div class="meta-right">
                  <span v-if="task.endDate" class="due-date">{{ task.endDate.slice(5) }}</span>
                  <el-avatar v-if="dimension === 'status' && task.assigneeName" :size="18" class="assignee-av">
                    {{ task.assigneeName.slice(0,1) }}
                  </el-avatar>
                  <span v-else-if="dimension === 'member'" class="status-icon" :title="task.status">
                    {{ statusIcon(task.status) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="!getTasks(col.key).length" class="empty-state">拖拽任务至此</div>
        </div>
      </div>
    </div>

    <!-- Batch Toolbar (Floating) -->
    <Transition name="fade-slide">
      <div v-if="selectedTasks.size > 0" class="batch-floating-bar">
        <span class="selection-count">已选择 {{ selectedTasks.size }}</span>
        <div class="bar-sep"></div>
        <button class="btn btn-ghost" @click="showBatchStatus = true">更新状态</button>
        <button class="btn btn-ghost" @click="showBatchAssign = true">指派负责人</button>
        <button class="btn btn-ghost text-danger" @click="handleBatchDelete">删除</button>
        <div class="bar-sep"></div>
        <button class="btn btn-primary btn-sm" @click="selectedTasks.clear()">取消</button>
      </div>
    </Transition>

    <!-- Task Detail Drawer -->
    <TaskDetailDrawer 
      v-model:visible="showDrawer" 
      :task-id="selectedTaskId" 
      @updated="taskStore.fetchTasks(projectStore.currentProjectId)"
      @deleted="taskStore.fetchTasks(projectStore.currentProjectId)"
    />

    <!-- Context Menu -->
    <div v-if="ctxVisible" class="ctx-menu" :style="{ left: ctxX + 'px', top: ctxY + 'px' }" v-click-outside="hideCtx">
      <div class="ctx-item" @click="ctxEdit">✏️ 编辑详情</div>
      <div class="ctx-sep"></div>
      <div class="ctx-group-label">变更状态</div>
      <div v-for="col in columns" :key="col.key" class="ctx-item" @click="ctxChangeStatus(col.key)">
        {{ col.label }}
      </div>
      <div class="ctx-sep"></div>
      <div class="ctx-item text-danger" @click="ctxDelete">🗑 删除任务</div>
    </div>

    <!-- Batch Status Modal -->
    <el-dialog v-model="showBatchStatus" title="批量更新状态" width="320px">
      <el-select v-model="batchStatus" style="width: 100%">
        <el-option label="待处理" value="TODO" />
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已完成" value="DONE" />
        <el-option label="已阻塞" value="BLOCKED" />
      </el-select>
      <template #footer>
        <el-button @click="showBatchStatus = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="handleBatchUpdate({ status: batchStatus })">确认更新</el-button>
      </template>
    </el-dialog>

    <!-- Batch Assign Modal -->
    <el-dialog v-model="showBatchAssign" title="批量指派负责人" width="320px">
      <el-select v-model="batchAssigneeId" placeholder="选择成员" style="width: 100%" clearable>
        <el-option v-for="m in memberStore.members" :key="m.memberId" :label="m.nickname" :value="m.memberId" />
      </el-select>
      <template #footer>
        <el-button @click="showBatchAssign = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="handleBatchUpdate({ assigneeId: batchAssigneeId })">确认指派</el-button>
      </template>
    </el-dialog>

    <!-- Create Modal (Simplified) -->
    <el-dialog v-model="showEdit" title="新建任务" width="480px">
      <el-form label-position="top">
        <el-form-item label="任务名称" required>
          <el-input v-model="form.title" placeholder="任务标题" />
        </el-form-item>
        <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 12px">
          <el-form-item label="优先级">
            <el-select v-model="form.priority">
              <option value="LOW">低</option>
              <option value="MEDIUM">中</option>
              <option value="HIGH">高</option>
              <option value="URGENT">紧急</option>
            </el-select>
          </el-form-item>
          <el-form-item label="截止日期">
            <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="handleSave">立即创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useProjectStore, useTaskStore, useMemberStore, useGanttStore, useAuthStore } from '@/stores'
import { taskApi } from '@/api'
import type { Task } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import StatsBar from '@/components/StatsBar.vue'
import TaskDetailDrawer from '@/components/TaskDetailDrawer.vue'
import SkeletonBoard from '@/components/SkeletonBoard.vue'

const projectStore = useProjectStore()
const taskStore = useTaskStore()
const memberStore = useMemberStore()
const ganttStore = useGanttStore()
const authStore = useAuthStore()

// Dimension state
const dimension = ref<'status' | 'member'>('status')

// Drawer state
const showDrawer = ref(false)
const selectedTaskId = ref<string | null>(null)

// Filters
const filterKeyword = ref('')
const filterAssignee = ref('')

const filteredTasks = computed(() => {
  let tasks = taskStore.tasks
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    tasks = tasks.filter(t => t.title.toLowerCase().includes(kw))
  }
  if (filterAssignee.value) {
    tasks = tasks.filter(t => (t.assigneeId || t.assignee_id) === filterAssignee.value)
  }
  return tasks
})

const columns = computed(() => {
  if (dimension.value === 'status') {
    return [
      { key: 'TODO', label: '待处理', dotClass: 'pending' },
      { key: 'IN_PROGRESS', label: '进行中', dotClass: 'progress' },
      { key: 'DONE', label: '已完成', dotClass: 'done' },
      { key: 'BLOCKED', label: '已阻塞', dotClass: 'blocked' },
    ]
  } else {
    const members = memberStore.members.map(m => ({
      key: m.memberId,
      label: m.nickname,
    }))
    return [...members, { key: 'unassigned', label: '未分配' }]
  }
})

function getTasks(colKey: string) {
  if (dimension.value === 'status') {
    return filteredTasks.value.filter(t => t.status === colKey)
  } else {
    if (colKey === 'unassigned') {
      return filteredTasks.value.filter(t => !t.assigneeId && !t.assignee_id)
    }
    return filteredTasks.value.filter(t => (t.assigneeId || t.assignee_id) === colKey)
  }
}

function statusIcon(status: string) {
  return { TODO: '○', IN_PROGRESS: '◐', DONE: '✓', BLOCKED: '⚠' }[status] || '○'
}

function priorityTag(p: string) {
  return { URGENT: 'tag-p0', HIGH: 'tag-p1', MEDIUM: 'tag-p2', LOW: 'tag-p2' }[p] || 'tag-p2'
}
function priorityLabel(p: string) {
  return { URGENT: 'P0', HIGH: 'P1', MEDIUM: 'P2', LOW: 'P3' }[p] || 'P2'
}

// Drag & Drop
const dragTaskId = ref<string | null>(null)
const dragFromCol = ref<string | null>(null)
const dragOverCol = ref<string | null>(null)

function onDragStart(task: any, fromCol: string) {
  dragTaskId.value = String(task.taskId || task.id)
  dragFromCol.value = fromCol
}
function onDragEnd() {
  dragTaskId.value = null
  dragFromCol.value = null
  dragOverCol.value = null
}
function onDragOver(colKey: string) {
  dragOverCol.value = colKey
}
async function onDrop(toCol: string) {
  dragOverCol.value = null
  if (!dragTaskId.value || !dragFromCol.value) return
  if (dragFromCol.value === toCol) return
  
  const updateData: any = {}
  if (dimension.value === 'status') {
    updateData.status = toCol
  } else {
    updateData.assigneeId = toCol === 'unassigned' ? null : toCol
  }

  try {
    await taskApi.update(dragTaskId.value, updateData)
    await taskStore.fetchTasks(projectStore.currentProjectId)
  } catch (e) {
    ElMessage.error('更新失败')
  }
  dragTaskId.value = null
  dragFromCol.value = null
}

// Single task edit
const showEdit = ref(false)
const form = ref({ title:'', status:'TODO', assigneeId:'', priority:'MEDIUM', endDate:'', description:'' })

function openTask(task?: Task) {
  if (task) {
    selectedTaskId.value = String(task.taskId || task.id)
    showDrawer.value = true
  } else {
    form.value = { title:'', status:'TODO', assigneeId:'', priority:'MEDIUM', endDate:'', description:'' }
    showEdit.value = true
  }
}

async function handleSave() {
  if (!form.value.title) { ElMessage.warning('请输入任务名称'); return }
  try {
    const data = {
      projectId: projectStore.currentProjectId,
      ...form.value,
      assigneeId: form.value.assigneeId || undefined
    }
    await taskApi.create(data)
    showEdit.value = false
    await taskStore.fetchTasks(projectStore.currentProjectId)
  } catch(e) { ElMessage.error('保存失败') }
}

// Context Menu
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
function ctxEdit() { if (ctxTask.value) openTask(ctxTask.value); hideCtx() }
async function ctxChangeStatus(status: string) {
  if (!ctxTask.value) return
  await taskApi.update(String(ctxTask.value.taskId || ctxTask.value.id), { status })
  await taskStore.fetchTasks(projectStore.currentProjectId)
  hideCtx()
}
async function ctxDelete() {
  if (!ctxTask.value) return
  try {
    await ElMessageBox.confirm('确认删除？')
    await taskApi.delete(String(ctxTask.value.taskId || ctxTask.value.id))
    await taskStore.fetchTasks(projectStore.currentProjectId)
  } catch {}
  hideCtx()
}

// Batch operations
const selectedTasks = ref(new Set<string>())
const showBatchStatus = ref(false)
const showBatchAssign = ref(false)
const batchStatus = ref('TODO')
const batchAssigneeId = ref('')
const batchLoading = ref(false)

function toggleSelect(task: any) {
  const id = String(task.taskId || task.id)
  if (selectedTasks.value.has(id)) selectedTasks.value.delete(id)
  else selectedTasks.value.add(id)
}

async function handleBatchUpdate(data: any) {
  batchLoading.value = true
  try {
    const ids = Array.from(selectedTasks.value)
    await Promise.all(ids.map(id => taskApi.update(id, data)))
    ElMessage.success('批量更新成功')
    selectedTasks.value.clear()
    showBatchStatus.value = false
    showBatchAssign = false
    await taskStore.fetchTasks(projectStore.currentProjectId)
  } catch { ElMessage.error('部分任务更新失败') }
  finally { batchLoading.value = false }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedTasks.value.size} 个任务？`)
    batchLoading.value = true
    const ids = Array.from(selectedTasks.value)
    await Promise.all(ids.map(id => taskApi.delete(id)))
    ElMessage.success('批量删除成功')
    selectedTasks.value.clear()
    await taskStore.fetchTasks(projectStore.currentProjectId)
  } catch {}
  finally { batchLoading.value = false }
}

onMounted(() => {
  if (projectStore.currentProjectId) taskStore.fetchTasks(projectStore.currentProjectId)
})

watch(() => projectStore.currentProjectId, (pid) => {
  if (pid) taskStore.fetchTasks(pid)
})
</script>

<style scoped>
.kanban-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg);
  overflow: hidden;
}

.view-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: var(--surface-1);
  border-bottom: 1px solid var(--border);
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input { width: 180px; }
.filter-select { width: 120px; }

.toolbar-info { font-size: 13px; color: var(--text-faint); font-weight: 500; }

.dimension-toggle { display: flex; background: var(--surface-3); padding: 3px; border-radius: var(--radius-sm); gap: 2px; }
.toggle-btn { padding: 4px 12px; font-size: 12px; font-weight: 600; border: none; background: transparent; color: var(--text-faint); cursor: pointer; border-radius: 4px; transition: all 0.12s; }
.toggle-btn:hover { color: var(--text-secondary); }
.toggle-btn.active { background: var(--surface-5); color: var(--text); box-shadow: var(--shadow-sm); }

.toolbar-sep { width: 1px; height: 16px; background: var(--border-strong); }

.kanban-board {
  flex: 1;
  display: grid;
  gap: 16px;
  padding: 16px;
  overflow-x: auto;
  align-items: start;
}

.kanban-col {
  display: flex;
  flex-direction: column;
  background: var(--surface-1);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  min-width: 280px;
  max-height: 100%;
  transition: background 0.2s, transform 0.2s;
}

.kanban-col.is-drag-over {
  background: var(--surface-3);
  border-color: var(--primary);
  transform: scale(1.01);
}

.kanban-col-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-pending { background: var(--text-faint); }
.status-progress { background: var(--primary); }
.status-done { background: var(--success); }
.status-blocked { background: var(--danger); }

.col-title { font-size: 13px; font-weight: 600; color: var(--text); flex: 1; }
.col-count { font-size: 11px; font-weight: 600; color: var(--text-faint); background: var(--surface-3); padding: 2px 6px; border-radius: 10px; }

.kanban-cards { flex: 1; padding: 12px; display: flex; flex-direction: column; gap: 10px; overflow-y: auto; }

.kanban-card {
  position: relative;
  background: var(--surface-2);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-strong);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: grab;
  user-select: none;
}

.kanban-card:hover { border-color: var(--text-muted); transform: translateY(-2px); box-shadow: var(--shadow-md); }
.kanban-card.is-selected { border-color: var(--primary); background: var(--primary-bg); }
.kanban-card.is-dragging { opacity: 0.5; cursor: grabbing; }

.card-selection { position: absolute; top: 8px; left: 8px; opacity: 0; transition: opacity 0.15s; z-index: 10; }
.kanban-card:hover .card-selection, .kanban-card.is-selected .card-selection { opacity: 1; }
.card-selection input { width: 14px; height: 14px; cursor: pointer; accent-color: var(--primary); }

.card-priority { height: 2px; width: 100%; border-radius: 2px 2px 0 0; }
.p-urgent { background: var(--danger); }
.p-high { background: var(--warning); }
.p-medium { background: var(--primary); }
.p-low { background: var(--text-faint); }

.card-content { padding: 12px; }
.card-title { font-size: 13px; font-weight: 500; color: var(--text); line-height: 1.4; margin-bottom: 12px; }
.card-meta { display: flex; justify-content: space-between; align-items: center; }

.priority-badge { font-size: 10px; font-weight: 700; padding: 1px 4px; border-radius: 4px; }
.due-date { font-size: 10px; color: var(--text-faint); font-weight: 500; }
.assignee-av { background: var(--primary-bg) !important; color: var(--primary) !important; font-weight: 700; font-size: 10px !important; border: 1px solid var(--border); }
.status-icon { font-size: 12px; color: var(--text-faint); font-weight: 700; }

.empty-state { text-align: center; padding: 32px 0; font-size: 12px; color: var(--text-faint); border: 1px dashed var(--border-strong); border-radius: var(--radius-md); }

.batch-floating-bar { position: fixed; bottom: 32px; left: 50%; transform: translateX(-50%); background: var(--surface-elevated); border: 1px solid var(--border-strong); border-radius: 12px; padding: 8px 16px; display: flex; align-items: center; gap: 12px; box-shadow: var(--shadow-lg); z-index: 1000; backdrop-filter: blur(8px); }
.selection-count { font-size: 13px; font-weight: 600; color: var(--text); }
.bar-sep { width: 1px; height: 16px; background: var(--border-strong); }

.ctx-menu { position: fixed; background: var(--surface-elevated); border: 1px solid var(--border-strong); border-radius: var(--radius-md); padding: 4px; box-shadow: var(--shadow-lg); z-index: 2000; min-width: 160px; }
.ctx-item { padding: 8px 12px; font-size: 13px; color: var(--text-secondary); cursor: pointer; border-radius: 4px; }
.ctx-item:hover { background: var(--surface-3); color: var(--text); }
.ctx-sep { height: 1px; background: var(--border-subtle); margin: 4px 0; }
.ctx-group-label { font-size: 10px; font-weight: 800; color: var(--text-faint); padding: 4px 12px; text-transform: uppercase; }

.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s ease; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translate(-50%, 20px); }
.text-danger { color: var(--danger) !important; }
.btn-sm { padding: 4px 10px; font-size: 12px; }
</style>
