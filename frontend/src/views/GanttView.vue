<template>
  <div class="gantt-view">
    <div class="gantt-toolbar">
      <div class="toolbar-left">
        <span class="project-name">{{ projectStore.currentProject?.name || '全部项目' }}</span>
        <!-- 任务统计 -->
        <div class="gantt-stats" v-if="ganttStore.ganttData.tasks.length">
          <span class="stat-item">
            <span class="stat-label">任务:</span>
            <span class="stat-value">{{ ganttStore.ganttData.tasks.length }}</span>
          </span>
          <span class="stat-item stat-todo">
            <span class="stat-label">待处理:</span>
            <span class="stat-value">{{ taskStats.todo }}</span>
          </span>
          <span class="stat-item stat-progress">
            <span class="stat-label">进行中:</span>
            <span class="stat-value">{{ taskStats.inProgress }}</span>
          </span>
          <span class="stat-item stat-done">
            <span class="stat-label">已完成:</span>
            <span class="stat-value">{{ taskStats.done }}</span>
          </span>
          <span class="stat-item stat-blocked">
            <span class="stat-label">已阻塞:</span>
            <span class="stat-value">{{ taskStats.blocked }}</span>
          </span>
        </div>
      </div>
      <div class="toolbar-right">
        <el-select v-model="timeScale" size="small" style="width:120px;margin-right:8px" @change="initGantt">
          <el-option label="按天" value="day" />
          <el-option label="按周" value="week" />
          <el-option label="按月" value="month" />
        </el-select>
        <div style="width:1px;height:20px;background:var(--border-light);margin:0 4px"></div>
        <el-button size="small" :type="ganttDim === 'task' ? 'primary' : 'default'" @click="ganttDim = 'task'">📋 按任务</el-button>
        <el-button size="small" :type="ganttDim === 'member' ? 'primary' : 'default'" @click="ganttDim = 'member'">👤 按人</el-button>
        <el-button size="small" @click="initGantt">刷新</el-button>
        <el-button type="primary" size="small" @click="showCreateTask = true">+ 新建任务</el-button>
      </div>
    </div>

    <!-- 任务-人维度 -->
    <div class="gantt-dimension" v-if="memberTaskCount.length">
      <div class="dimension-title">👤 任务分布</div>
      <div class="dimension-bars">
        <div v-for="m in memberTaskCount" :key="m.name" class="dimension-bar-item">
          <span class="dimension-name">{{ m.name }}</span>
          <div class="dimension-bar-track">
            <div class="dimension-bar-fill" :style="{ width: m.percent + '%', background: m.color }"></div>
          </div>
          <span class="dimension-count">{{ m.count }}</span>
        </div>
      </div>
    </div>

    <div class="gantt-container" ref="ganttContainer"></div>

    <!-- 创建任务弹窗 -->
    <el-dialog v-model="showCreateTask" title="新建任务" width="520px" :append-to-body="true">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="任务名称">
          <el-input v-model="taskForm.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="taskForm.assigneeId" placeholder="选择负责人" clearable>
            <el-option v-for="m in memberStore.members" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="taskForm.startDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="taskForm.endDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="taskForm.priority">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateTask = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTask">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑任务弹窗 -->
    <el-dialog v-model="showEditTask" title="编辑任务" width="560px" :append-to-body="true" destroy-on-close>
      <el-form v-if="editTaskForm.taskId" :model="editTaskForm" label-width="80px">
        <el-form-item label="任务名称">
          <el-input v-model="editTaskForm.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="editTaskForm.assigneeId" placeholder="选择负责人" clearable>
            <el-option v-for="m in memberStore.members" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editTaskForm.status">
            <el-option label="待处理" value="TODO" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已阻塞" value="BLOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="editTaskForm.priority">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="editTaskForm.startDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="editTaskForm.endDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="进度">
          <el-slider v-model="editTaskForm.progressPercent" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editTaskForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditTask = false">取消</el-button>
        <el-button type="danger" plain @click="handleDeleteTask">删除</el-button>
        <el-button type="primary" @click="handleUpdateTask">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import gantt from 'dhtmlx-gantt'
import 'dhtmlx-gantt/codebase/dhtmlxgantt.css'
import { useProjectStore, useGanttStore, useMemberStore, useTaskStore } from '@/stores'
import { taskApi } from '@/api'
import dayjs from 'dayjs'
import { computed } from 'vue'

const projectStore = useProjectStore()
const ganttStore = useGanttStore()
const memberStore = useMemberStore()
const taskStore = useTaskStore()

const ganttContainer = ref<HTMLElement>()
const ganttInstance = ref<any>(null)
const showCreateTask = ref(false)
const showEditTask = ref(false)
const timeScale = ref<'day' | 'week' | 'month'>('week')
const ganttDim = ref<'task' | 'member'>('task')

// 任务统计
const taskStats = computed(() => {
  const tasks = ganttStore.ganttData.tasks || []
  return {
    total: tasks.length,
    todo: tasks.filter(t => t.status === 'TODO').length,
    inProgress: tasks.filter(t => t.status === 'IN_PROGRESS').length,
    done: tasks.filter(t => t.status === 'DONE').length,
    blocked: tasks.filter(t => t.status === 'BLOCKED').length,
  }
})

// 任务-人维度统计
const memberTaskCount = computed(() => {
  const tasks = ganttStore.ganttData.tasks || []
  const memberMap = new Map<string, number>()
  const memberNames = new Map<string, string>()

  tasks.forEach(t => {
    const name = t.assigneeName || '未分配'
    const count = memberMap.get(name) || 0
    memberMap.set(name, count + 1)
    if (t.assigneeName) memberNames.set(name, t.assigneeName)
  })

  const maxCount = Math.max(...Array.from(memberMap.values()), 1)
  const colors = ['#3370ff', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899']

  return Array.from(memberMap.entries())
    .map(([name, count], i) => ({
      name,
      count,
      percent: (count / maxCount) * 100,
      color: name === '未分配' ? '#6b7280' : colors[i % colors.length],
    }))
    .sort((a, b) => b.count - a.count)
})

const taskForm = ref({
  title: '',
  assigneeId: '',
  startDate: '',
  endDate: '',
  priority: 'MEDIUM',
  description: '',
})

const editTaskForm = ref({
  taskId: '',
  title: '',
  assigneeId: '',
  startDate: '',
  endDate: '',
  priority: 'MEDIUM',
  status: 'TODO',
  description: '',
  progressPercent: 0,
})

let ganttInited = false

function initGanttConfig() {
  gantt.config.xml_date = '%Y-%m-%d'
  gantt.config.row_height = 40
  gantt.config.bar_height = 28
  gantt.config.date_format = '%Y-%m-%d'
  gantt.config.scale_unit = 'day'
  gantt.config.date_scale = '%d %M'
  gantt.config.subscales = [{ unit: 'week', step: 1, date: '第%W周' }]
  gantt.config.show_progress = true
  gantt.config.auto_types = true
  gantt.config.drag_progress = true
  gantt.config.drag_resize = true
  gantt.config.drag_move = true
  gantt.config.drag_links = true
  gantt.config.open_tree_initially = true
  gantt.config.fit_tasks = true
  gantt.config.initial_scroll = true

  // 列配置
  gantt.config.columns = [
    { name: 'text', label: '任务名称', tree: true, width: 220, resize: true },
    { name: 'start_date', label: '开始', align: 'center', width: 90 },
    { name: 'duration', label: '天数', align: 'center', width: 50 },
    { name: 'progress', label: '进度', align: 'center', width: 60, template: (task: any) => `${Math.round(task.progress * 100)}%` },
  ]

  // 飞书风格颜色
  gantt.templates.task_class = () => 'feishu-task'
  gantt.templates.grid_row_class = () => ''
  gantt.templates.bar_class = () => 'feishu-bar'
}

async function initGantt() {
  if (!ganttContainer.value) return

  const projectId = projectStore.currentProjectId;

  if (!ganttInited) {
    // 首次：配置 + 创建实例
    initGanttConfig()
    await nextTick()
    gantt.init(ganttContainer.value)
    ganttInstance.value = gantt
  } else {
    gantt.clearAll()
  }

  await nextTick()

  await ganttStore.fetchGanttData(projectId || '')
  const { tasks, links } = ganttStore.ganttData

  // 调试日志
  // 调试日志
  console.log('[Gantt] initGantt START, container:', ganttContainer.value)
  const today = new Date()
  // 过滤无效任务（无有意义文本或无日期），并转换为 Date 对象
  const normalizedTasks = (tasks || [])
    .filter((t: any) => t.text && t.text.trim() && t.start_date)
    .map((t: any) => {
      let startDate: Date
      try {
        startDate = new Date(t.start_date + 'T00:00:00')
        if (isNaN(startDate.getTime())) startDate = today
      } catch {
        startDate = today
      }
      return {
        ...t,
        id: t.id || String(Math.random()),
        text: t.text || '(无标题)',
        start_date: startDate,
        duration: Number(t.duration) || 1,
        parent: 0,
        open: true,
      }
    })

  console.log('[Gantt] normalized tasks:', normalizedTasks.length)
  if (normalizedTasks.length === 0) {
    console.warn('[Gantt] WARNING: all tasks filtered out! Raw tasks:', tasks)
  }
  normalizedTasks.slice(0, 3).forEach((t: any) => {
    console.log('[Gantt] task sample:', t.id, t.text, t.start_date, t.duration)
  })

  // dhtmlx-gantt v9 只接受数字类型：0=FS, 1=SS, 2=FF, 3=SF
  const numericLinks = (links || []).map((l: any) => ({
    ...l,
    type: { FS: 0, SS: 1, FF: 2, SF: 3 }[String(l.type)] ?? 0,
  }))

  try {
    gantt.clearAll()
    gantt.parse({ data: normalizedTasks, links: numericLinks || [] })
    gantt.render()
  } catch(e) {
    console.error('[Gantt] ERROR:', e)
  }
  ganttInited = true
}

async function handleCreateTask() {
  if (!taskForm.value.title) return
  const projectId = projectStore.currentProjectId;
  if (!projectId) return

  try {
    await taskApi.create({
      projectId,
      title: taskForm.value.title,
      assigneeId: taskForm.value.assigneeId || undefined,
      startDate: taskForm.value.startDate || dayjs().format('YYYY-MM-DD'),
      endDate: taskForm.value.endDate || dayjs().add(1, 'day').format('YYYY-MM-DD'),
      priority: taskForm.value.priority,
      description: taskForm.value.description,
      status: 'TODO',
      progress: 0,
    })
    showCreateTask.value = false
    taskForm.value = { title: '', assigneeId: '', startDate: '', endDate: '', priority: 'MEDIUM', description: '' }
    await initGantt()
  } catch (e: any) {
    console.error('创建任务失败', e)
  }
}

function openEditTask(taskId: string) {
  const task = gantt.getTask(taskId) as any
  if (!task) return
  editTaskForm.value = {
    taskId,
    title: task.text || task.title || '',
    assigneeId: task.assignee_id || '',
    startDate: task.start_date ? dayjs(task.start_date).format('YYYY-MM-DD') : '',
    endDate: task.end_date ? dayjs(task.end_date).format('YYYY-MM-DD') : '',
    priority: task.priority || 'MEDIUM',
    status: task.status || 'TODO',
    description: task.description || '',
    progressPercent: task.progress ? Math.round(task.progress * 100) : 0,
  }
  showEditTask.value = true
}

async function handleUpdateTask() {
  if (!editTaskForm.value.taskId) return
  try {
    await taskApi.update(editTaskForm.value.taskId, {
      title: editTaskForm.value.title,
      assigneeId: editTaskForm.value.assigneeId || undefined,
      startDate: editTaskForm.value.startDate,
      endDate: editTaskForm.value.endDate,
      priority: editTaskForm.value.priority,
      status: editTaskForm.value.status,
      description: editTaskForm.value.description,
      progress: editTaskForm.value.progressPercent,
    })
    showEditTask.value = false
    await initGantt()
  } catch (e: any) {
    console.error('更新任务失败', e)
  }
}

async function handleDeleteTask() {
  if (!editTaskForm.value.taskId) return
  if (!confirm('确认删除该任务？')) return
  try {
    await taskApi.delete(editTaskForm.value.taskId)
    showEditTask.value = false
    await initGantt()
  } catch (e: any) {
    console.error('删除任务失败', e)
  }
}

// 监听项目切换
watch(() => projectStore.currentProjectId, () => {
  if (ganttInited) initGantt()
})

onMounted(async () => {
  await projectStore.fetchProjects()
  await memberStore.fetchMembers()
  initGantt()

  // 点击任务打开编辑弹窗
  gantt.attachEvent('onClickTask', (taskId: string) => {
    openEditTask(taskId)
    return true
  })

  // 拖拽保存
  gantt.attachEvent('onAfterTaskDrag', async (id: string, mode: string, e: any) => {
    const task = gantt.getTask(id) as any
    try {
      await taskApi.update(id, {
        startDate: dayjs(task.start_date).format('YYYY-MM-DD'),
        endDate: dayjs(task.end_date).format('YYYY-MM-DD'),
        progress: task.progress,
      })
    } catch (err) {
      console.error('更新失败', err)
    }
  })

  // 链接创建
  gantt.attachEvent('onAfterLinkAdd', async (id: string, link: any) => {
    try {
      await taskApi.dependencies.add(link.target, link.source, {0:'FS',1:'SS',2:'FF',3:'SF'}[String(link.type)] ?? 'FS')
    } catch (err) {
      console.error('添加依赖失败', err)
    }
  })

  // 任务进度拖拽
  gantt.attachEvent('onAfterTaskUpdate', async (id: string, task: any) => {
    try {
      await taskApi.update(id, { progress: task.progress })
    } catch (err) {
      console.error('更新进度失败', err)
    }
  })
})

onBeforeUnmount(() => {
  if (ganttInited) {
    gantt.clearAll()
    ganttInited = false
  }
})
</script>

<style scoped>
.gantt-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--surface-1);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.gantt-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border-light);
  background: var(--surface-1);
}
.project-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

/* 任务统计 */
.gantt-stats {
  display: flex;
  gap: 12px;
  margin-left: 16px;
  font-size: 12px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  background: var(--surface-3);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 6px 12px;
  min-width: 60px;
}
.stat-label {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  line-height: 1;
}
.stat-todo .stat-value { color: #60a5fa; }
.stat-progress .stat-value { color: #f59e0b; }
.stat-done .stat-value { color: #10b981; }
.stat-blocked .stat-value { color: #ef4444; }

/* 任务-人维度 */
.gantt-dimension {
  padding: 12px 20px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border-light);
}
.dimension-title {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.dimension-bars {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.dimension-bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 120px;
}
.dimension-name {
  font-size: 11px;
  color: var(--text);
  width: 50px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dimension-bar-track {
  flex: 1;
  height: 6px;
  background: var(--border-light);
  border-radius: 3px;
  overflow: hidden;
}
.dimension-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}
.dimension-count {
  font-size: 11px;
  color: var(--text-secondary);
  min-width: 20px;
  text-align: right;
}

.gantt-container {
  flex: 1;
  overflow: hidden;
  min-height: 500px;
  height: 100%;
}
.gantt-mount {
  width: 100%;
  height: 100%;
}
:deep(.feishu-task .gantt_task_line) {
  background: var(--accent) !important;
  border-color: var(--accent-hover) !important;
}
:deep(.gantt_task_progress) {
  background: rgba(0,0,0,0.2) !important;
}
:deep(.gantt_grid_head_cell) {
  background: var(--surface-2) !important;
  color: var(--text) !important;
  font-weight: 600;
}
:deep(.gantt_row, .gantt_cell) {
  border-color: var(--border-light) !important;
}
</style>

