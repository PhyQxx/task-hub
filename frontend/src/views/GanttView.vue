<template>
  <div class="gantt-view">
    <div class="gantt-toolbar">
      <div class="toolbar-left">
        <span class="project-name">{{ projectStore.currentProject?.name || '请选择项目' }}</span>
      </div>
      <div class="toolbar-right">
        <el-button size="small" @click="initGantt">刷新</el-button>
        <el-button type="primary" size="small" @click="showCreateTask = true">+ 新建任务</el-button>
      </div>
    </div>

    <div v-loading="ganttStore.loading" class="gantt-container" ref="ganttContainer">
      <div ref="ganttRef" class="gantt-mount"></div>
    </div>

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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import gantt from 'dhtmlx-gantt'
import 'dhtmlx-gantt/codebase/dhtmlxgantt.css'
import { useProjectStore, useGanttStore, useMemberStore, useTaskStore } from '@/stores'
import { taskApi } from '@/api'
import dayjs from 'dayjs'

const projectStore = useProjectStore()
const ganttStore = useGanttStore()
const memberStore = useMemberStore()
const taskStore = useTaskStore()

const ganttRef = ref<HTMLElement>()
const ganttContainer = ref<HTMLElement>()
const showCreateTask = ref(false)

const taskForm = ref({
  title: '',
  assigneeId: '',
  startDate: '',
  endDate: '',
  priority: 'MEDIUM',
  description: '',
})

let ganttInited = false

function initGanttConfig() {
  gantt.config.xml_date = '%Y-%m-%d %H:%i:%s'
  gantt.config.row_height = 40
  gantt.config.bar_height = 28
  gantt.config.date_format = '%Y-%m-%d %H:%i:%s'
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
  if (!ganttRef.value) return

  const projectId = projectStore.currentProjectId; 
  if (!projectId) return

  if (ganttInited) {
    gantt.clearAll()
  }

  initGanttConfig()
  gantt.init(ganttRef.value)

  await ganttStore.fetchGanttData(projectId)
  const { tasks, links } = ganttStore.ganttData

  // dhtmlx-gantt v9 只接受数字类型：0=FS, 1=SS, 2=FF, 3=SF
  const numericLinks = (links || []).map((l: any) => ({
    ...l,
    type: { FS: 0, SS: 1, FF: 2, SF: 3 }[String(l.type)] ?? 0,
  }))

  gantt.parse({ data: tasks || [], links: numericLinks })
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

// 监听项目切换
watch(() => projectStore.currentProjectId, () => {
  if (ganttInited) initGantt()
})

onMounted(async () => {
  await projectStore.fetchProjects()
  await memberStore.fetchMembers()
  initGantt()

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
.gantt-container {
  flex: 1;
  overflow: hidden;
  min-height: 0;
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

