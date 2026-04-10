<template>
  <div class="gantt-view">
    <StatsBar />
    <div class="gantt-toolbar">
      <div class="toolbar-left">
        <span class="project-name">{{ projectStore.currentProject?.name || '全部项目' }}</span>
        <div class="gantt-stats" v-if="ganttStore.ganttData.tasks.length">
          <span class="stat-item stat-todo"><span class="stat-label">待处理:</span><span class="stat-value">{{ taskStats.todo }}</span></span>
          <span class="stat-item stat-progress"><span class="stat-label">进行中:</span><span class="stat-value">{{ taskStats.inProgress }}</span></span>
          <span class="stat-item stat-done"><span class="stat-label">已完成:</span><span class="stat-value">{{ taskStats.done }}</span></span>
          <span class="stat-item stat-blocked"><span class="stat-label">已阻塞:</span><span class="stat-value">{{ taskStats.blocked }}</span></span>
        </div>
      </div>
      <div class="toolbar-right">
        <div class="time-scale-group">
          <button v-for="scale in timeScales" :key="scale.key" class="scale-btn" :class="{ active: timeScale === scale.key }" @click="timeScale = scale.key; initGantt()">{{ scale.label }}</button>
        </div>
        <div style="width:1px;height:20px;background:var(--border-light);margin:0 4px"></div>
        <el-button-group>
          <el-button size="small" :type="ganttDim === 'task' ? 'primary' : 'default'" @click="ganttDim = 'task'">📋 按任务</el-button>
          <el-button size="small" :type="ganttDim === 'member' ? 'primary' : 'default'" @click="ganttDim = 'member'">👤 按人</el-button>
        </el-button-group>
        <el-button size="small" @click="initGantt">刷新</el-button>
        <el-button v-if="isAdmin" type="default" size="small" @click="showSmartSchedule = true">🧠 智能排程</el-button>
        <el-button type="primary" size="small" @click="openCreateTask">+ 新建任务</el-button>
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
        <el-form-item label="项目">
          <el-select v-model="taskForm.projectId" placeholder="选择项目" style="width: 100%">
            <el-option v-for="p in projectStore.projects" :key="p.projectId" :label="p.name" :value="p.projectId" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名称">
          <el-input v-model="taskForm.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input :value="authStore.nickname || authStore.memberId" disabled />
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
        <el-form-item label="项目">
          <el-select v-model="editTaskForm.projectId" placeholder="选择项目" style="width: 100%" :disabled="!isAdmin">
            <el-option v-for="p in projectStore.projects" :key="p.projectId" :label="p.name" :value="p.projectId" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="editTaskForm.assigneeId" placeholder="选择负责人" clearable :disabled="!isAdmin">
            <el-option v-for="m in memberStore.members" :key="m.memberId" :label="m.nickname" :value="m.memberId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editTaskForm.status" :disabled="!isAdmin">
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
        <el-button v-if="isAdmin" type="danger" plain @click="handleDeleteTask">删除</el-button>
        <el-button type="primary" @click="handleUpdateTask">保存</el-button>
      </template>
    </el-dialog>

    <!-- 智能排程弹窗 -->
    <SmartScheduleModal :visible="showSmartSchedule" @close="showSmartSchedule = false" />
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import gantt from 'dhtmlx-gantt'
import 'dhtmlx-gantt/codebase/dhtmlxgantt.css'
import {useAuthStore, useGanttStore, useMemberStore, useProjectStore, useTaskStore} from '@/stores'
import {taskApi} from '@/api'
import dayjs from 'dayjs'
import StatsBar from '@/components/StatsBar.vue'
import SmartScheduleModal from '@/components/SmartScheduleModal.vue'

const projectStore = useProjectStore()
const ganttStore = useGanttStore()
const memberStore = useMemberStore()
const authStore = useAuthStore()

// 角色判断
const isAdmin = computed(() => authStore.role === 'admin')

const ganttContainer = ref<HTMLElement>()
const ganttInstance = ref<any>(null)
const showCreateTask = ref(false)
const showEditTask = ref(false)
const showSmartSchedule = ref(false)
const timeScale = ref<'day' | 'week' | 'month' | 'quarter'>('week')
const ganttDim = ref<'task' | 'member'>('task')
const timeScales = [
  { key: 'day', label: '今天' },
  { key: 'week', label: '周' },
  { key: 'month', label: '月' },
  { key: 'quarter', label: '季' },
]

// ganttDim 切换后重渲染
watch(ganttDim, async () => {
  if (!ganttInited) return
  gantt.clearAll()
  if (ganttContainer.value) ganttContainer.value.innerHTML = ''
  ganttInited = false
  await nextTick()
  initGanttConfig()
  await nextTick()
  gantt.init(ganttContainer.value!)
  ganttInited = true
  await loadGanttData()
})

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
  projectId: '',
  title: '',
  assigneeId: '',
  startDate: '',
  endDate: '',
  priority: 'MEDIUM',
  description: '',
})

// 新建任务时自动设置负责人为当前用户
function openCreateTask() {
  taskForm.value = {
    projectId: projectStore.currentProjectId || '',
    title: '',
    assigneeId: authStore.memberId || '',
    startDate: '',
    endDate: '',
    priority: 'MEDIUM',
    description: '',
  }
  showCreateTask.value = true
}

const editTaskForm = ref({
  taskId: '',
  projectId: '',
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

async function loadGanttData() {
  const projectId = projectStore.currentProjectId
  await ganttStore.fetchGanttData(projectId || '')
  let { tasks, links } = ganttStore.ganttData

  // 快捷筛选
  const f = ganttStore.taskFilter
  if (f) {
    const todayStr = dayjs().format('YYYY-MM-DD')
    if (f === 'mine') tasks = tasks.filter((t: any) => t.assigneeId === authStore.memberId || t.assignee_id === authStore.memberId)
    else if (f === 'today') tasks = tasks.filter((t: any) => t.end_date === todayStr || t.endDate === todayStr)
    else if (f === 'blocked') tasks = tasks.filter((t: any) => t.status === 'BLOCKED')
    else if (f === 'owner') tasks = tasks.filter((t: any) => t.assigneeId === authStore.memberId || t.assignee_id === authStore.memberId)
  }

  const today = new Date()
  let normalizedTasks = (tasks || [])
    .filter((t: any) => t.text && t.text.trim() && t.start_date)
    .map((t: any) => {
      let startDate: Date
      try {
        startDate = new Date(t.start_date + 'T00:00:00')
        if (isNaN(startDate.getTime())) startDate = today
      } catch {
        startDate = today
      }
      // 保留所有任务字段（description、priority、status 等）传给 gantt，
      // 以便 lightbox 能正确回显，后续保存也能取到所有字段
      const { end_date, project_id, ...rest } = t as any
      let endDate: Date | undefined
      if (end_date) {
        try {
          endDate = new Date(end_date + 'T00:00:00')
          if (isNaN(endDate.getTime())) endDate = undefined
        } catch { endDate = undefined }
      }
      return {
        ...rest,
        projectId: project_id || rest.projectId,
        id: t.id || String(Math.random()),
        text: t.text || '(无标题)',
        start_date: startDate,
        end_date: endDate,  // 保留用于表格列显示
        duration: Number(t.duration) || 1,
        progress: t.progress ?? 0,
        parent: t.parent ?? 0,
        open: true,
        assigneeName: t.assignee_name || t.assigneeName || '',  // 后端返回 snake_case，转换为 camelCase 供 gantt 列使用
      }
    })

  // 按人维度：注入虚拟父任务（同 dhtmlx-gantt 免费版 tree 分组效果）
  if (ganttDim.value === 'member') {
    const memberGroups: Record<string, any[]> = {}
    normalizedTasks.forEach((t: any) => {
      const key = t.assigneeName || '未分配'
      if (!memberGroups[key]) memberGroups[key] = []
      memberGroups[key].push(t)
    })
    const virtualParents: any[] = []
    const memberPrefix = '__member_'
    Object.entries(memberGroups).forEach(([name, groupTasks]) => {
      const pid = memberPrefix + name
      // 虚拟父任务的日期取该组最早开始和最晚结束
      const starts = groupTasks.map((t: any) => t.start_date as Date).filter(Boolean).sort((a, b) => a.getTime() - b.getTime())
      const ends = groupTasks.map((t: any) => t.end_date as Date).filter(Boolean).sort((a, b) => b.getTime() - a.getTime())
      virtualParents.push({
        id: pid,
        text: name,
        start_date: starts[0] || today,
        end_date: ends[0] || today,
        duration: 1,
        progress: 0,
        parent: 0,
        open: true,
        isVirtual: true,
        isGroup: true,
      })
      groupTasks.forEach((t: any) => { t.parent = pid })
    })
    normalizedTasks = [...virtualParents, ...normalizedTasks]
  }

  const numericLinks = (links || []).map((l: any) => ({
    ...l,
    type: { FS: 0, SS: 1, FF: 2, SF: 3 }[String(l.type)] ?? 0,
  }))

  try {
    applyTimeScale()
    gantt.clearAll()
    gantt.parse({ data: normalizedTasks, links: numericLinks || [] })
    gantt.render()
  } catch(e) {
    console.error('[Gantt] ERROR:', e)
  }
}

function applyTimeScale() {
  // dhtmlx-gantt v9 使用 config.scales 数组替代旧的 scale_unit + subscales
  switch (timeScale.value) {
    case 'day':
      gantt.config.scales = [
        { unit: 'day', step: 1, date: '%m-%d' },
      ]
      break
    case 'week':
      gantt.config.scales = [
        { unit: 'week', step: 1, date: '第%W周' },
        { unit: 'day', step: 1, date: '%m-%d' },
      ]
      break
    case 'month':
      gantt.config.scales = [
        { unit: 'month', step: 1, date: '%F %Y' },
        { unit: 'day', step: 1, date: '%m-%d' },
      ]
      break
    case 'quarter':
      gantt.config.scales = [
        { unit: 'month', step: 1, template: (date: Date) => `Q${Math.floor(date.getMonth() / 3) + 1} ${date.getFullYear()}` },
        { unit: 'day', step: 1, date: '%m-%d' },
      ]
      break
    default:
      gantt.config.scales = [
        { unit: 'week', step: 1, date: '第%W周' },
        { unit: 'day', step: 1, date: '%m-%d' },
      ]
  }
}

function initGanttConfig() {
  // 中文 locale 配置（lightbox 按钮和月份名称）
  gantt.locale.labels = {
    ...gantt.locale.labels,
    button_save: '保存',
    button_cancel: '取消',
    button_delete: '删除',
    button_clear: '清除',
    button_yes: '是',
    button_no: '否',
    button_open: '打开',
    button_close: '关闭',
    button_apply: '应用',
    section_plan_date: '计划时间',
    section_text: '任务名称',
    section_description: '描述',
    section_priority: '优先级',
    section_duration: '工期',
    section_type: '类型',
    section_time: '时间',
    section_details: '详情',
    confirm_closing: '确认关闭？',
    confirm_deleting: '确认删除任务？',
    message_delete_task: '任务将被删除',
    message_undo: '操作已撤销',
    message_confirm_deleting: '此操作不可撤销，确认删除？',
    message_on_milestone: '里程碑',
    text_tab: '常规',
    text_filter: '筛选',
    text_undo: '撤销',
    text_redo: '重做',
    link_start: '开始',
    link_finish: '完成',
    hours: '小时',
    days: '天',
    weeks: '周',
    months: '月',
    quarter: '季度',
    hours_short: '时',
    days_short: '天',
    weeks_short: '周',
    months_short: '月',
  }

  // 月份中文名称（date picker 用）
  gantt.locale.date = {
    ...gantt.locale.date,
    month_full: ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'],
    month_short: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
    week_full: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'],
    week_short: ['日', '一', '二', '三', '四', '五', '六'],
  }

  gantt.config.xml_date = '%Y-%m-%d'
  gantt.config.row_height = 40
  gantt.config.bar_height = 28
  gantt.config.date_format = '%Y-%m-%d'
  applyTimeScale()
  gantt.config.show_progress = true
  gantt.config.auto_types = true
  gantt.config.duration_unit = 'day'  // 工期单位为天
  gantt.config.duration_date = '%Y-%m-%d'  // 日期格式
  gantt.config.drag_progress = true
  gantt.config.drag_resize = true
  gantt.config.drag_move = true
  gantt.config.drag_links = true
  gantt.config.open_tree_initially = true
  gantt.config.fit_tasks = true
  gantt.config.initial_scroll = true
  gantt.config.dblclick_create = false  // 禁用双击创建（用 lightbox 编辑）
  gantt.config.details_on_dblclick = true  // 双击打开 lightbox 编辑

  // 自定义 lightbox sections（完全控制标签文字）
  gantt.config.lightbox = {
    sections: [
      {
        name: 'text',
        map_to: 'text',
        type: 'textarea',
        label: '任务名称',
        focus: true,
      },
      {
        name: 'time',
        map_to: 'auto',
        type: 'duration',
        label: '时间范围',
        single_date: false,
      },
      {
        name: 'description',
        map_to: 'description',
        type: 'textarea',
        label: '描述',
      },
      {
        name: 'priority',
        map_to: 'priority',
        type: 'select',
        label: '优先级',
        options: [
          { key: 'LOW', label: '低' },
          { key: 'MEDIUM', label: '中' },
          { key: 'HIGH', label: '高' },
          { key: 'URGENT', label: '紧急' },
        ],
      },
    ],
  }

  // lightbox 打开前：用 API 数据填充 task 对象（description/priority 等）
  gantt.attachEvent('onBeforeLightbox', (id: string) => {
    const apiTask = ganttStore.ganttData.tasks.find((t: any) => String(t.id) === String(id) || String(t.taskId) === String(id))
    console.log('[onBeforeLightbox] id:', id, 'apiTask:', apiTask ? JSON.stringify(apiTask) : 'NOT FOUND')
    if (apiTask) {
      const task = gantt.getTask(id) as any
      task.description = apiTask.description ?? ''
      task.priority = apiTask.priority ?? 'MEDIUM'
    }
    return true
  })

  // lightbox 弹出后：按钮替换中文 + 手动填充 description textarea
  gantt.attachEvent('onLightbox', (id: string) => {
    const container = gantt.getLightbox()
    if (!container) return
    // 按钮文字中文
    const btnMap: Record<string, string> = {
      'Save': '保存',
      'Cancel': '取消',
      'Delete': '删除',
    }
    container.querySelectorAll('.gantt_btn_set > div:last-child').forEach((el) => {
      const text = el.textContent?.trim()
      if (text && btnMap[text]) {
        el.textContent = btnMap[text]
      }
    })
    // 手动填充 description textarea（gantt lightbox 的 textarea 不会自动从 task 对象读取自定义字段）
    const task = gantt.getTask(id) as any
    if (task) {
      const descTextarea = container.querySelector('.gantt_section_description textarea') as HTMLTextAreaElement
      if (descTextarea && task.description) {
        descTextarea.value = task.description
      }
    }
  })

  // lightbox 保存：手动调 API 持久化
  gantt.attachEvent('onLightboxSave', async (id: string, task: any) => {
    // 从 DOM 读取 description（gantt 可能没有同步到 task 对象）
    const container = gantt.getLightbox()
    const descTextarea = container?.querySelector('.gantt_section_description textarea') as HTMLTextAreaElement
    const descriptionVal = descTextarea?.value ?? task.description
    try {
      await taskApi.update(id, {
        title: task.text || task.title,
        startDate: task.start_date ? dayjs(task.start_date).format('YYYY-MM-DD') : undefined,
        endDate: task.end_date ? dayjs(task.end_date).format('YYYY-MM-DD') : undefined,
        priority: task.priority,
        description: descriptionVal,
        progress: task.progress != null ? Math.round(task.progress * 100) : undefined,
      })
      await loadGanttData()
    } catch (err) {
      console.error('[Gantt] 保存失败', err)
    }
    return true  // 关闭 lightbox
  })

  // lightbox 删除
  gantt.attachEvent('onLightboxDelete', async (id: string) => {
    try {
      await taskApi.delete(id)
      await loadGanttData()
    } catch (err) {
      console.error('[Gantt] 删除失败', err)
    }
    return true  // 关闭 lightbox
  })

  // 列配置
  const baseColumns = [
    { name: 'text', label: '任务名称', tree: true, width: 200, resize: true },
    { name: 'projectName', label: '项目', align: 'center', width: 120, template: (task: any) => {
      if (!task.projectId) return '—'
      const p = projectStore.projects.find((x: any) => x.projectId === task.projectId)
      return p ? p.name : (task.projectId || '—')
    }},
    { name: 'assigneeName', label: '负责人', align: 'center', width: 80, template: (task: any) => task.assigneeName || '未分配' },
    { name: 'start_date', label: '开始', align: 'center', width: 90 },
    { name: 'end_date', label: '结束', align: 'center', width: 90, template: (task: any) => {
      if (task.end_date) return dayjs(task.end_date).format('YYYY-MM-DD')
      return ''
    }},
    { name: 'duration', label: '天数', align: 'center', width: 50 },
    { name: 'progress', label: '进度', align: 'center', width: 70, template: (task: any) => {
      const p = task.progress ?? 0
      return Math.round(p * 100) + '%'
    }},
  ]
  gantt.config.columns = baseColumns

  // 根据任务状态显示不同颜色的任务条
  gantt.templates.task_class = (start: any, end: any, task: any) => {
    const status = task.status || 'TODO'
    const statusMap: Record<string, string> = {
      'TODO': 'gantt-status-todo',
      'IN_PROGRESS': 'gantt-status-progress',
      'DONE': 'gantt-status-done',
      'BLOCKED': 'gantt-status-blocked',
    }
    return statusMap[status] || 'gantt-status-todo'
  }
  gantt.templates.grid_row_class = () => ''
  gantt.templates.bar_class = () => ''
  // 按人分组：虚拟父行高亮
  if (ganttDim.value === 'member') {
    gantt.templates.grid_row_class = (start: any, end: any, task: any) => {
      return task.isVirtual ? 'gantt-virtual-parent' : ''
    }
    gantt.templates.task_class = (start: any, end: any, task: any) => {
      if (task.isVirtual) return 'gantt-virtual-parent'
      const status = task.status || 'TODO'
      const statusMap: Record<string, string> = {
        'TODO': 'gantt-status-todo',
        'IN_PROGRESS': 'gantt-status-progress',
        'DONE': 'gantt-status-done',
        'BLOCKED': 'gantt-status-blocked',
      }
      return statusMap[status] || 'gantt-status-todo'
    }
  }
}

async function initGantt() {
  if (!ganttContainer.value) return

  if (!ganttInited) {
    initGanttConfig()
    await nextTick()
    gantt.init(ganttContainer.value)
    ganttInstance.value = gantt
    ganttInited = true
  } else {
    // 刻度切换时：重置 gantt 实例，清除 DOM，重新初始化
    gantt.clearAll()
    if (ganttContainer.value) {
      ganttContainer.value.innerHTML = ''
    }
    ganttInited = false
    await nextTick()
    initGanttConfig()
    await nextTick()
    gantt.init(ganttContainer.value)
    ganttInited = true
    await loadGanttData()
    return
  }
  await loadGanttData()
}

async function handleCreateTask() {
  if (!taskForm.value.title) return
  if (!taskForm.value.projectId) return

  try {
    await taskApi.create({
      projectId: taskForm.value.projectId,
      title: taskForm.value.title,
      assigneeId: authStore.memberId || undefined,
      startDate: taskForm.value.startDate || dayjs().format('YYYY-MM-DD'),
      endDate: taskForm.value.endDate || dayjs().add(1, 'day').format('YYYY-MM-DD'),
      priority: taskForm.value.priority,
      description: taskForm.value.description,
      status: 'TODO',
      progress: 0,
    })
    showCreateTask.value = false
    taskForm.value = { projectId: projectStore.currentProjectId || '', title: '', assigneeId: authStore.memberId || '', startDate: '', endDate: '', priority: 'MEDIUM', description: '' }
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
    projectId: task.projectId || '',
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
      projectId: editTaskForm.value.projectId,
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
    await loadGanttData()
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
    await loadGanttData()
  } catch (e: any) {
    console.error('删除任务失败', e)
  }
}

// 监听项目切换
watch(() => projectStore.currentProjectId, () => {
  if (ganttInited) loadGanttData()
})

// 监听快捷筛选变化
watch(() => ganttStore.taskFilter, () => {
  if (ganttInited) loadGanttData()
})

onMounted(async () => {
  await projectStore.fetchProjects()
  await memberStore.fetchMembers()
  await initGantt()

  // 点击任务打开编辑弹窗
  gantt.attachEvent('onClickTask', (taskId: string) => {
    console.log('[onClickTask] taskId:', taskId)
    openEditTask(taskId)
    return true
  })

  // 记录拖拽前的任务数据，用于判断是否为真实拖拽
  let dragOriginalTask: any = null

  // 拖拽开始前记录原始数据
  gantt.attachEvent('onBeforeTaskDrag', (_id: string, mode: string, e: any) => {
    const id = gantt.getState().task_id
    if (id) {
      const task = gantt.getTask(id)
      dragOriginalTask = task ? { start_date: task.start_date, end_date: task.end_date, progress: task.progress } : null
    }
    return true
  })

  // 双击任务打开编辑弹窗（gantt 的 onDblClick）
  gantt.attachEvent('onDblClick', (taskId: string) => {
    console.log('[onDblClick] taskId:', taskId)
    if (taskId) {
      openEditTask(taskId)
    }
    return true
  })

  // 拖拽保存（区分真实拖拽和双击误触发）
  gantt.attachEvent('onAfterTaskDrag', async (id: string, mode: string, e: any) => {
    const task = gantt.getTask(id) as any
    console.log('[Drag] id:', id, 'mode:', mode, 'start:', task.start_date, 'end:', task.end_date, 'progress:', task.progress)
    // 判断是否为真实拖拽：无原始数据 或 位置无变化
    const isRealDrag = dragOriginalTask && (
      task.start_date?.getTime() !== dragOriginalTask.start_date?.getTime() ||
      task.end_date?.getTime() !== dragOriginalTask.end_date?.getTime() ||
      task.progress !== dragOriginalTask.progress
    )
    dragOriginalTask = null
    if (!isRealDrag) {
      // 无真实位移，双击打开编辑弹窗
      console.log('[Drag] no movement, opening edit dialog')
      openEditTask(id)
      return
    }
    try {
      await taskApi.update(id, {
        startDate: task.start_date ? dayjs(task.start_date).format('YYYY-MM-DD') : undefined,
        endDate: task.end_date ? dayjs(task.end_date).format('YYYY-MM-DD') : undefined,
        progress: task.progress != null ? Math.round(task.progress * 100) : undefined,
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
:deep(.gantt-status-todo .gantt_task_line) {
  background: #60a5fa !important;
  border-color: #3b82f6 !important;
}
:deep(.gantt-status-progress .gantt_task_line) {
  background: #f59e0b !important;
  border-color: #d97706 !important;
}
:deep(.gantt-status-done .gantt_task_line) {
  background: #10b981 !important;
  border-color: #059669 !important;
}
:deep(.gantt-status-blocked .gantt_task_line) {
  background: #ef4444 !important;
  border-color: #dc2626 !important;
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
/* 虚拟父任务行（按人分组） */
:deep(.gantt-virtual-parent) {
  background: var(--surface-2) !important;
  font-weight: 700;
  color: var(--text) !important;
}
:deep(.gantt-virtual-parent .gantt_cell) {
  background: var(--surface-2) !important;
  font-weight: 700;
}

/* 时间刻度按钮 */
.time-scale-group {
  display: flex;
  gap: 2px;
  background: var(--surface-3);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 3px;
}
.scale-btn {
  padding: 4px 10px;
  font-size: 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.15s ease;
  font-weight: 500;
}
.scale-btn:hover {
  background: var(--surface-2);
  color: var(--text);
}
.scale-btn.active {
  background: var(--primary-color, #3370ff);
  color: #fff;
  box-shadow: 0 1px 3px rgba(51, 112, 255, 0.3);
}
</style>

