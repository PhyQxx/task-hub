export { useAuthStore } from './auth'
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Project, Task, Member, WorkLog, GanttDataVO } from '@/types'
import { projectApi, taskApi, ganttApi, memberApi, workLogApi } from '@/api'

export const useProjectStore = defineStore('project', () => {
  const projects = ref<Project[]>([])
  const currentProjectId = ref<string>('')
  const currentProject = computed(() => projects.value.find(p => p.projectId === currentProjectId.value))

  async function fetchProjects() {
    const res = await projectApi.list()
    projects.value = (res.data || []).map((p: any) => ({
      ...p,
      projectId: p.project_id || p.projectId || p.id,
    }))
    if (projects.value.length && !currentProjectId.value) {
      currentProjectId.value = projects.value[0].projectId
    }
  }

  async function selectProject(id: string) {
    currentProjectId.value = id
  }

  async function deleteProject(id: string) {
    await projectApi.delete(id)
    projects.value = projects.value.filter(p => p.projectId !== id)
    if (currentProjectId.value === id) {
      currentProjectId.value = projects.value[0]?.projectId || ''
    }
  }

  async function updateProject(id: string, data: Partial<Project>) {
    const res = await projectApi.update(id, data)
    const updated = res.data
    if (updated) {
      // 保证新对象有 projectId 字段
      const normalized: Project = {
        ...updated,
        projectId: updated.project_id || updated.projectId || updated.id,
      } as Project
      const idx = projects.value.findIndex(p => p.projectId === id)
      if (idx !== -1) projects.value[idx] = normalized
    }
  }

  return { projects, currentProjectId, currentProject, fetchProjects, selectProject, deleteProject, updateProject }
})

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<Task[]>([])
  const loading = ref(false)

  async function fetchTasks(projectId: string) {
    loading.value = true
    try {
      // 如果 projectId 为空，获取所有任务
      if (!projectId) {
        const res = await taskApi.listAll()
        tasks.value = res.data || []
      } else {
        const res = await taskApi.list(projectId)
        tasks.value = res.data || []
      }
    } finally {
      loading.value = false
    }
  }

  async function createTask(data: Partial<Task>) {
    const res = await taskApi.create(data)
    tasks.value.push(res.data)
    return res.data
  }

  async function updateTask(taskId: string, data: Partial<Task>) {
    const res = await taskApi.update(taskId, data)
    const idx = tasks.value.findIndex(t => t.id === taskId)
    if (idx !== -1) tasks.value[idx] = res.data
    return res.data
  }

  async function deleteTask(taskId: string) {
    await taskApi.delete(taskId)
    tasks.value = tasks.value.filter(t => t.id !== taskId)
  }

  const todoTasks = computed(() => tasks.value.filter(t => t.status === 'TODO'))
  const inProgressTasks = computed(() => tasks.value.filter(t => t.status === 'IN_PROGRESS'))
  const blockedTasks = computed(() => tasks.value.filter(t => t.status === 'BLOCKED'))
  const doneTasks = computed(() => tasks.value.filter(t => t.status === 'DONE'))

  return {
    tasks, loading, todoTasks, inProgressTasks, blockedTasks, doneTasks,
    fetchTasks, createTask, updateTask, deleteTask
  }
})

export const useGanttStore = defineStore('gantt', () => {
  const ganttData = ref<GanttDataVO>({ tasks: [], links: [], milestones: [] })
  const loading = ref(false)
  // 快捷筛选：mine | today | blocked | owner | ''（全部）
  const taskFilter = ref<string>('')

  async function fetchGanttData(projectId: string) {
    loading.value = true
    try {
      const res = await ganttApi.getData(projectId)
      // 兼容两种响应结构，后端返回 snake_case，转为驼峰以匹配前端类型
      const d = res.data ?? {}
      const rawTasks = d.data || d.tasks || []
      const tasks = rawTasks.map((t: any) => ({
        ...t,
        assigneeName: t.assignee_name || t.assigneeName || '',
      }))
      ganttData.value = {
        tasks,
        links: d.links || [],
        milestones: d.milestones || [],
      }
    } finally {
      loading.value = false
    }
  }

  function setTaskFilter(f: string) { taskFilter.value = f }

  return { ganttData, loading, taskFilter, fetchGanttData, setTaskFilter }
})

export const useMemberStore = defineStore('member', () => {
  const members = ref<Member[]>([])
  const roles = ref<string[]>([])

  async function fetchMembers() {
    const res = await memberApi.list()
    members.value = res.data || []
  }

  async function fetchRoles() {
    const res = await memberApi.roles()
    roles.value = res.data || []
  }

  return { members, roles, fetchMembers, fetchRoles }
})

export const useWorkLogStore = defineStore('worklog', () => {
  const logs = ref<WorkLog[]>([])
  const loading = ref(false)

  async function fetchLogs(taskId: string) {
    loading.value = true
    try {
      const res = await workLogApi.list(taskId)
      logs.value = res.data || []
    } finally {
      loading.value = false
    }
  }

  async function createLog(data: Partial<WorkLog>) {
    const res = await workLogApi.create(data)
    logs.value.unshift(res.data)
    return res.data
  }

  async function updateLog(id: string, data: Partial<WorkLog>) {
    const res = await workLogApi.update(id, data)
    const idx = logs.value.findIndex(l => l.id === id)
    if (idx !== -1) logs.value[idx] = res.data
    return res.data
  }

  return { logs, loading, fetchLogs, createLog, updateLog }
})
