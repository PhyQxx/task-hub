<template>
  <div class="task-manage">
    <div class="view-header">
      <div class="header-left">
        <h1 class="view-title">全站任务监控</h1>
        <p class="view-subtitle">跨项目任务调度、状态追踪与效能合规审计</p>
      </div>
      <div class="header-right">
        <div class="filter-group">
          <el-input v-model="search" placeholder="搜索任务标题..." size="small" class="search-input" clearable />
          <el-select v-model="filterStatus" placeholder="所有状态" size="small" clearable class="filter-select">
            <el-option label="待处理" value="TODO" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已阻塞" value="BLOCKED" />
          </el-select>
          <el-button type="primary" size="small" @click="openCreate">+ 新增任务</el-button>
        </div>
      </div>
    </div>

    <div class="card table-card">
      <el-table :data="filteredTasks" style="width: 100%" v-loading="loading" height="calc(100vh - 240px)">
        <el-table-column label="任务 ID" prop="taskId" width="100" />
        <el-table-column label="任务名称" min-width="240">
          <template #default="{ row }">
            <div class="task-cell" @click="openDrawer(row)">
              <span class="task-title">{{ row.title }}</span>
              <div class="task-project">项目：{{ getProjectName(row.projectId || row.project_id) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="160">
          <template #default="{ row }">
            <div class="assignee-cell">
              <el-avatar :size="24" class="m-av">{{ row.mappedAssignee?.slice(0,1) || '?' }}</el-avatar>
              <span class="m-nickname">{{ row.mappedAssignee }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <span class="p-badge" :class="'p-' + row.priority?.toLowerCase()">{{ row.priority }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span class="status-tag" :class="row.status?.toLowerCase()">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间周期" width="200">
          <template #default="{ row }">
            <div class="time-cell">
              <span class="time-val">{{ row.startDate || '-' }}</span>
              <span class="time-sep">→</span>
              <span class="time-val">{{ row.endDate || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Task Detail Drawer -->
    <TaskDetailDrawer 
      v-model:visible="showDrawer" 
      :task-id="selectedTaskId" 
      @updated="fetchTasks"
      @deleted="fetchTasks"
    />

    <!-- Global Create Task Dialog -->
    <el-dialog v-model="showCreate" title="全站新增任务" width="500px">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="所属项目" required>
          <el-select v-model="createForm.projectId" placeholder="选择目标项目" style="width: 100%" filterable>
            <el-option v-for="p in projects" :key="p.projectId" :label="p.name" :value="p.projectId" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务标题" required>
          <el-input v-model="createForm.title" placeholder="请输入任务标题" />
        </el-form-item>
        <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 16px">
          <el-form-item label="负责人">
            <el-select v-model="createForm.assigneeId" placeholder="留空以待智排" style="width: 100%" clearable filterable>
              <el-option v-for="m in members" :key="m.memberId" :label="m.nickname" :value="m.memberId" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级">
            <el-select v-model="createForm.priority" style="width: 100%">
              <option value="LOW">🔵 低</option>
              <option value="MEDIUM">🟡 中</option>
              <option value="HIGH">🟠 高</option>
              <option value="URGENT">🔴 紧急</option>
            </el-select>
          </el-form-item>
        </div>
        <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 16px">
          <el-form-item label="开始日期">
            <el-date-picker v-model="createForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="截止日期">
            <el-date-picker v-model="createForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="任务详细要求..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">立即发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { taskApi, projectApi, memberApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import TaskDetailDrawer from '@/components/TaskDetailDrawer.vue'
import dayjs from 'dayjs'

const tasks = ref<any[]>([])
const projects = ref<any[]>([])
const members = ref<any[]>([])
const loading = ref(false)
const search = ref('')
const filterStatus = ref('')

const showDrawer = ref(false)
const selectedTaskId = ref<string | null>(null)

const showCreate = ref(false)
const creating = ref(false)
const createForm = reactive({
  projectId: '',
  title: '',
  assigneeId: '',
  priority: 'MEDIUM',
  startDate: dayjs().format('YYYY-MM-DD'),
  endDate: dayjs().add(3, 'day').format('YYYY-MM-DD'),
  description: ''
})

const filteredTasks = computed(() => {
  return tasks.value.filter(t => {
    const matchSearch = !search.value || t.title.toLowerCase().includes(search.value.toLowerCase())
    const matchStatus = !filterStatus.value || t.status === filterStatus.value
    return matchSearch && matchStatus
  })
})

function getProjectName(pid: string) {
  const p = projects.value.find(p => p.projectId === pid)
  return p ? p.name : pid
}

function resolveNickname(userIdRaw: any) {
  if (!userIdRaw) return '未分配'
  const rawStr = String(userIdRaw)
  if (rawStr.includes('nickname=')) {
    const match = rawStr.match(/nickname=([^,\]\s]+)/)
    if (match) return match[1]
  }
  let targetId = rawStr
  if (rawStr.includes('memberId=')) {
    const match = rawStr.match(/memberId=([^,\]\s]+)/)
    if (match) targetId = match[1]
  }
  const m = members.value.find(m => String(m.memberId) === String(targetId))
  if (m) return m.nickname
  return targetId.length > 15 ? '系统用户' : targetId
}

function statusLabel(s: string) {
  const map: any = { 'TODO': '待处理', 'IN_PROGRESS': '进行中', 'DONE': '已完成', 'BLOCKED': '已阻塞' }
  return map[s] || s
}

async function fetchTasks() {
  loading.value = true
  try {
    const [tRes, pRes, mRes] = await Promise.all([
      taskApi.listAll(),
      projectApi.list(),
      memberApi.list()
    ])
    projects.value = (pRes.data || []).map((p:any) => ({ ...p, projectId: p.projectId || p.id }))
    members.value = mRes.data || []
    tasks.value = (tRes.data || []).map((t: any) => ({
      ...t,
      mappedAssignee: resolveNickname(t.assigneeId || t.assignee_id)
    }))
  } catch (e) {
    ElMessage.error('获取全站任务数据失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, {
    projectId: '',
    title: '',
    assigneeId: '',
    priority: 'MEDIUM',
    startDate: dayjs().format('YYYY-MM-DD'),
    endDate: dayjs().add(3, 'day').format('YYYY-MM-DD'),
    description: ''
  })
  showCreate.value = true
}

async function handleCreate() {
  if (!createForm.projectId || !createForm.title) {
    ElMessage.warning('请填写必填项')
    return
  }
  creating.value = true
  try {
    await taskApi.create({
      ...createForm,
      assigneeId: createForm.assigneeId || undefined
    })
    ElMessage.success('任务发布成功')
    showCreate.value = false
    fetchTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    creating.value = false
  }
}

function openDrawer(row: any) {
  selectedTaskId.value = String(row.taskId || row.id)
  showDrawer.value = true
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要永久删除任务「${row.title}」吗？`, '删除任务', { type: 'warning' })
    await taskApi.delete(String(row.taskId || row.id))
    ElMessage.success('已删除')
    fetchTasks()
  } catch {}
}

onMounted(fetchTasks)
</script>

<style scoped>
.task-manage { padding: 24px 32px; background: var(--bg); height: 100%; }
.view-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }
.view-title { font-size: 24px; font-weight: 800; color: var(--text); letter-spacing: -0.5px; margin-bottom: 4px; }
.view-subtitle { font-size: 14px; color: var(--text-faint); }
.filter-group { display: flex; gap: 12px; }
.search-input { width: 220px; }
.filter-select { width: 140px; }
.card { background: var(--surface-1); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; }
.task-cell { cursor: pointer; display: flex; flex-direction: column; gap: 4px; }
.task-cell:hover .task-title { color: var(--primary); }
.task-title { font-size: 14px; font-weight: 600; color: var(--text); transition: color 0.2s; }
.task-project { font-size: 11px; color: var(--text-faint); }
.assignee-cell { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--text-secondary); }
.m-nickname { font-weight: 500; }
.p-badge { font-size: 10px; font-weight: 800; padding: 1px 6px; border-radius: 4px; text-transform: uppercase; }
.p-urgent { background: var(--danger-bg); color: var(--danger); }
.p-high { background: var(--warning-bg); color: var(--warning); }
.p-medium { background: var(--primary-bg); color: var(--primary); }
.p-low { background: var(--surface-3); color: var(--text-faint); }
.status-tag { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 6px; }
.status-tag.todo { background: var(--surface-3); color: var(--text-faint); }
.status-tag.in_progress { background: var(--primary-bg); color: var(--primary); }
.status-tag.done { background: var(--success-bg); color: var(--success); }
.status-tag.blocked { background: var(--danger-bg); color: var(--danger); }
.time-cell { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-muted); }
.time-sep { opacity: 0.5; }
:deep(.el-table) { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: var(--surface-2); }
:deep(.el-table__row:hover > td) { background-color: var(--surface-2) !important; }
</style>
