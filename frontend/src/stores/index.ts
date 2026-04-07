import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Project, Task, Member, WorkLog, GanttDataVO } from '@/types'
import { projectApi, taskApi, ganttApi, memberApi, workLogApi } from '@/api'

export const useProjectStore = defineStore('project', () => {
  const projects = ref<Project[]>([])
  const currentProjectId = ref<string>('')
  const currentProject = computed(() => projects.value.find(p => p.id === currentProjectId.value))

  async function fetchProjects() {
    const res = await projectApi.list()
    projects.value = res.data || []
    if (projects.value.length && !currentProjectId.value) {
      currentProjectId.value = projects.value[0].id
    }
  }

  function selectProject(id: string) {
    currentProjectId.value = id
  }

  return { projects, currentProjectId, currentProject, fetchProjects, selectProject }
})

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<Task[]>([])
  const loading = ref(false)

  async function fetchTasks(projectId: string) {
    loading.value = true
    try {
      const res = await taskApi.list(projectId)
      tasks.value = res.data || []
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

  async function fetchGanttData(projectId: string) {
    loading.value = true
    try {
      const res = await ganttApi.getData(projectId)
      ganttData.value = res.data || { tasks: [], links: [], milestones: [] }
    } finally {
      loading.value = false
    }
  }

  return { ganttData, loading, fetchGanttData }
})

export const useMemberStore = defineStore('member', () => {
  const members = ref<Member[]>([])

  async function fetchMembers() {
    const res = await memberApi.list()
    members.value = res.data || []
  }

  return { members, fetchMembers }
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
