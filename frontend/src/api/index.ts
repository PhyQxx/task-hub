import client from './client'
import type { Project, Member, Task } from '@/types'
import type { ApiResponse } from '@/types'

export const projectApi = {
  list: () => client.get<ApiResponse<Project[]>>('/projects'),
  get: (id: string) => client.get<ApiResponse<Project>>(`/projects/${id}`),
  create: (data: Partial<Project>) => client.post<ApiResponse<Project>>('/projects', data),
  update: (id: string, data: Partial<Project>) => client.put<ApiResponse<Project>>(`/projects/${id}`, data),
  delete: (id: string) => client.delete<ApiResponse<void>>(`/projects/${id}`),
  members: (id: string) => client.get<ApiResponse<Member[]>>(`/projects/${id}/members`),
}

export const taskApi = {
  list: (projectId: string) => client.get<ApiResponse<any[]>>(`/tasks/project/${projectId}`),
  listAll: () => client.get<ApiResponse<any[]>>('/tasks/all'),
  get: (taskId: string) => client.get<ApiResponse<any>>(`/tasks/${taskId}`),
  create: (data: any) => client.post<ApiResponse<any>>('/tasks', data),
  update: (taskId: string, data: any) => client.put<ApiResponse<any>>(`/tasks/${taskId}`, data),
  delete: (taskId: string) => client.delete<ApiResponse<void>>(`/tasks/${taskId}`),
  history: (taskId: string) => client.get<ApiResponse<any>>(`/tasks/${taskId}/history`),
  batchUpdate: async (data: { taskIds: string[]; status?: string; assigneeId?: string; priority?: string }) => {
    const results = await Promise.all(
      data.taskIds.map(taskId =>
        client.put<ApiResponse<any>>(`/tasks/${taskId}`, {
          status: data.status,
          assigneeId: data.assigneeId,
          priority: data.priority,
        })
      )
    )
    return { code: 0, data: results.length }
  },
  dependencies: {
    add: (taskId: string, dependsOn: string, dependencyType: string) =>
      client.post<ApiResponse<void>>('/tasks/dependencies', { taskId, dependsOn, dependencyType }),
    remove: (taskId: string, dependsOn: string) =>
      client.delete<ApiResponse<void>>('/tasks/dependencies', { taskId, dependsOn }),
  },
}

export const ganttApi = {
  getData: (projectId: string) => {
    if (!projectId) {
      return client.get<ApiResponse<any>>('/gantt/all')
    }
    return client.get<ApiResponse<any>>(`/gantt/project/${projectId}`)
  },
}

export const memberApi = {
  list: () => client.get<ApiResponse<Member[]>>('/members'),
  byRole: (role: string) => client.get<ApiResponse<Member[]>>(`/members/role/${role}`),
  loadTrend: (memberId: string) => client.get<ApiResponse<any[]>>(`/members/${memberId}/load-trend`),
  roles: () => client.get<ApiResponse<string[]>>('/members/roles'),
  memberTasks: (memberId: string) => client.get<ApiResponse<Task[]>>(`/members/${memberId}/tasks`),
}

export const workLogApi = {
  list: (params?: { date?: string; userId?: string }) => {
    const query = params ? '?' + new URLSearchParams(params as any).toString() : ''
    return client.get<ApiResponse<any[]>>(`/work-logs${query}`)
  },
  listByTask: (taskId: string) => client.get<ApiResponse<any[]>>(`/work-logs/task/${taskId}`),
  create: (data: any) => client.post<ApiResponse<any>>('/work-logs', data),
  update: (id: string, data: any) => client.put<ApiResponse<any>>(`/work-logs/${id}`, data),
  delete: (id: string) => client.delete<ApiResponse<void>>(`/work-logs/${id}`),
}

export const milestoneApi = {
  list: (projectId: string) => client.get<ApiResponse<any[]>>(`/milestones/project/${projectId}`),
  create: (data: any) => client.post<ApiResponse<any>>('/milestones', data),
  delete: (id: string) => client.delete<ApiResponse<void>>(`/milestones/${id}`),
}

export const scheduleApi = {
  batchSchedule: (projectId: string, taskIds: string[], mode: string) =>
    client.post<ApiResponse<any>>('/tasks/batch-schedule', { projectId, taskIds, mode }),
  reorder: (taskId: string, startDate: string, endDate: string, autoReschedule: boolean) =>
    client.post<ApiResponse<any>>('/tasks/reorder', { taskId, startDate, endDate, autoReschedule }),
}
