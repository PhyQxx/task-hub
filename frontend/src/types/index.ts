// ===== API 类型定义 =====

// 通用响应
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 项目
export interface Project {
  id: string
  name: string
  description?: string
  status: 'ACTIVE' | 'ARCHIVED'
  createdAt: string
  updatedAt: string
}

// 成员
export interface Member {
  id: string
  name: string
  avatar?: string
  role: 'PM' | 'DEV' | 'DESIGN' | 'TEST' | 'OPS'
  email?: string
  createdAt: string
}

// 任务
export interface Task {
  id: string
  projectId: string
  title: string
  description?: string
  status: 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'DONE'
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  assigneeId?: string
  assigneeName?: string
  milestoneId?: string
  startDate?: string
  endDate?: string
  progress: number // 0-100
  parentId?: string
  type?: 'TASK' | 'MILESTONE'
  createdAt: string
  updatedAt: string
}

// 任务依赖
export interface TaskDependency {
  id: string
  taskId: string
  dependsOn: string
  dependencyType: 'FS' | 'SS' | 'FF' | 'SF' // finish-start, start-start, finish-finish, start-finish
}

// 里程碑
export interface Milestone {
  id: string
  projectId: string
  name: string
  date: string
  completed: boolean
  createdAt: string
}

// 工作日志
export interface WorkLog {
  id: string
  taskId?: string
  userId: string
  userName: string
  date: string // yyyy-MM-dd
  todayProgress: string
  tomorrowPlan: string
  blockers?: string
  createdAt: string
  updatedAt: string
}

// ===== dhtmlxGantt 数据格式 =====
export interface GanttTask {
  id: string | number
  text: string
  start_date: Date
  end_date: Date
  duration: number // 天数
  progress: number // 0-1
  parent?: string | number
  type?: string
  status?: string
  assigneeName?: string
  open?: boolean
}

export interface GanttLink {
  id: string | number
  source: string | number
  target: string | number
  type: string // '0'=finish-start
}

export interface GanttDataVO {
  tasks: GanttTask[]
  links: GanttLink[]
  milestones: Milestone[]
}

// ===== 看板 =====
export interface KanbanColumn {
  key: Task['status']
  label: string
  color: string
  tasks: Task[]
}

// ===== 泳道视图 =====
export interface SwimLane {
  memberId: string
  memberName: string
  memberAvatar?: string
  tasks: Task[]
}
