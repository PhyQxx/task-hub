import { ref, watch } from 'vue'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { useProjectStore, useTaskStore, useGanttStore, useNotificationStore } from '@/stores'

const connected = ref(false)
let stompClient: Client | null = null

export function useWebSocket() {
  const projectStore = useProjectStore()
  const taskStore = useTaskStore()
  const ganttStore = useGanttStore()
  const notificationStore = useNotificationStore()

  function connect() {
    if (stompClient?.active) return

    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        const pid = projectStore.currentProjectId
        if (pid) subscribeProject(pid)
      },
      onDisconnect: () => {
        connected.value = false
      },
    })

    stompClient.activate()
  }

  function disconnect() {
    if (stompClient?.active) {
      stompClient.deactivate()
    }
    stompClient = null
    connected.value = false
  }

  function subscribeProject(projectId: string) {
    if (!stompClient?.active) return

    stompClient.subscribe(`/topic/project/${projectId}`, (message: IMessage) => {
      const msg = JSON.parse(message.body)
      handleMessage(msg)
    })
  }

  function handleMessage(msg: { type: string; payload: any }) {
    switch (msg.type) {
      case 'TASK_CREATED':
        if (msg.payload?.task) {
          taskStore.tasks.push(msg.payload.task)
          ganttStore.fetchGanttData(projectStore.currentProjectId || '')
          notificationStore.addNotification({
            type: 'task_created',
            title: '新任务创建',
            message: `${msg.payload.actor || '某人'} 创建了任务「${msg.payload.task.title || ''}」`,
            projectId: msg.payload.task.projectId,
          })
        }
        break
      case 'TASK_UPDATED':
        if (msg.payload?.taskId) {
          const idx = taskStore.tasks.findIndex((t: any) => (t.taskId || t.id) === msg.payload.taskId)
          if (idx !== -1 && msg.payload.fields) {
            taskStore.tasks[idx] = { ...taskStore.tasks[idx], ...msg.payload.fields }
          }
          if (msg.payload.fields?.workLogUpdated || msg.payload.fields?.milestoneCreated) {
            ganttStore.fetchGanttData(projectStore.currentProjectId || '')
          }
          notificationStore.addNotification({
            type: msg.payload.fields?.status ? 'status_change' : 'task_updated',
            title: '任务更新',
            message: `${msg.payload.actor || '某人'} 更新了任务 ${msg.payload.taskId}`,
            projectId: projectStore.currentProjectId,
          })
        }
        break
      case 'TASK_DELETED':
        if (msg.payload?.taskId) {
          taskStore.tasks = taskStore.tasks.filter((t: any) => (t.taskId || t.id) !== msg.payload.taskId)
          ganttStore.fetchGanttData(projectStore.currentProjectId || '')
          notificationStore.addNotification({
            type: 'task_deleted',
            title: '任务删除',
            message: `${msg.payload.actor || '某人'} 删除了任务 ${msg.payload.taskId}`,
            projectId: projectStore.currentProjectId,
          })
        }
        break
    }
  }

  watch(
    () => projectStore.currentProjectId,
    (newPid) => {
      if (newPid && stompClient?.active) {
        subscribeProject(newPid)
      }
    },
  )

  return { connected, connect, disconnect }
}
