<template>
  <div class="kanban-view">
    <div class="kanban-header">
      <span class="project-name">{{ projectStore.currentProject?.name || '请选择项目' }}</span>
      <el-button type="primary" size="small" @click="showCreate = true">+ 新建任务</el-button>
    </div>

    <div v-loading="taskStore.loading" class="kanban-board">
      <div
        v-for="col in columns"
        :key="col.key"
        class="kanban-column"
      >
        <div class="column-header" :style="{ borderTopColor: col.color }">
          <span class="column-title">{{ col.label }}</span>
          <span class="column-count">{{ getTasksByStatus(col.key).length }}</span>
        </div>

        <div class="column-body">
          <div
            v-for="task in getTasksByStatus(col.key)"
            :key="task.id"
            class="kanban-card"
            @click="openTask(task)"
          >
            <div class="card-title">{{ task.title }}</div>
            <div class="card-meta">
              <el-tag v-if="task.priority === 'URGENT'" type="danger" size="small" effect="dark">紧急</el-tag>
              <el-tag v-else-if="task.priority === 'HIGH'" type="warning" size="small">高</el-tag>
              <el-tag v-else-if="task.priority === 'MEDIUM'" size="small">中</el-tag>
              <el-tag v-else size="small" effect="plain">低</el-tag>
              <span v-if="task.assigneeName" class="assignee">
                <el-avatar size="small" :style="{ background: 'var(--accent)', fontSize: '10px' }">
                  {{ task.assigneeName.slice(0, 1) }}
                </el-avatar>
                {{ task.assigneeName }}
              </span>
            </div>
            <div v-if="task.endDate" class="card-date">
              <span :class="{ overdue: isOverdue(task.endDate) }">
                {{ task.endDate }}
              </span>
            </div>
          </div>

          <div v-if="!getTasksByStatus(col.key).length" class="column-empty">
            暂无任务
          </div>
        </div>
      </div>
    </div>

    <!-- 新建/编辑任务弹窗 -->
    <el-dialog v-model="showCreate" :title="editingTask ? '编辑任务' : '新建任务'" width="520px" :append-to-body="true">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="任务名称">
          <el-input v-model="taskForm.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="taskForm.status">
            <el-option v-for="col in columns" :key="col.key" :label="col.label" :value="col.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="taskForm.assigneeId" placeholder="选择负责人" clearable>
            <el-option v-for="m in memberStore.members" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="taskForm.priority">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="taskForm.endDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button v-if="editingTask" type="danger" @click="handleDelete">删除</el-button>
        <el-button type="primary" @click="handleSave">{{ editingTask ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useProjectStore, useTaskStore, useMemberStore } from '@/stores'
import type { Task } from '@/types'
import dayjs from 'dayjs'

const projectStore = useProjectStore()
const taskStore = useTaskStore()
const memberStore = useMemberStore()

const columns = [
  { key: 'TODO' as const, label: '待办', color: '#bfbfbf' },
  { key: 'IN_PROGRESS' as const, label: '进行中', color: '#3370ff' },
  { key: 'BLOCKED' as const, label: '已阻塞', color: '#ff4d4f' },
  { key: 'DONE' as const, label: '已完成', color: '#52c41a' },
]

function getTasksByStatus(status: Task['status']) {
  return taskStore.tasks.filter(t => t.status === status)
}

function isOverdue(date: string) {
  return dayjs(date).isBefore(dayjs(), 'day')
}

const showCreate = ref(false)
const editingTask = ref<Task | null>(null)

const taskForm = ref({
  title: '',
  status: 'TODO' as Task['status'],
  assigneeId: '',
  priority: 'MEDIUM' as Task['priority'],
  endDate: '',
  description: '',
})

function openTask(task: Task) {
  editingTask.value = task
  taskForm.value = {
    title: task.title,
    status: task.status,
    assigneeId: task.assigneeId || '',
    priority: task.priority,
    endDate: task.endDate || '',
    description: task.description || '',
  }
  showCreate.value = true
}

async function handleSave() {
  const projectId = projectStore.currentProjectId
  if (!projectId || !taskForm.value.title) return

  if (editingTask.value) {
    await taskStore.updateTask(editingTask.value.id, taskForm.value)
  } else {
    await taskStore.createTask({ projectId, ...taskForm.value })
  }
  showCreate.value = false
  editingTask.value = null
  await taskStore.fetchTasks(projectId)
}

async function handleDelete() {
  if (!editingTask.value) return
  await taskStore.deleteTask(editingTask.value.id)
  showCreate.value = false
  editingTask.value = null
  await taskStore.fetchTasks(projectStore.currentProjectId)
}

watch(() => projectStore.currentProjectId, async (id) => {
  if (id) await taskStore.fetchTasks(id)
}, { immediate: true })
</script>

<style scoped>
.kanban-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--surface-2);
  padding: 16px;
  gap: 12px;
}
.kanban-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--surface-1);
  padding: 12px 16px;
  border-radius: var(--radius-md);
}
.project-name {
  font-weight: 600;
  font-size: 15px;
}
.kanban-board {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.kanban-column {
  background: var(--surface-1);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.column-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-top: 3px solid;
  background: var(--surface-1);
}
.column-title {
  font-weight: 600;
  font-size: 13px;
  color: var(--text);
}
.column-count {
  background: var(--surface-2);
  color: var(--accent);
  border-radius: 10px;
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 600;
}
.column-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.kanban-card {
  background: var(--surface-3);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.kanban-card:hover {
  border-color: var(--accent);
  box-shadow: var(--shadow-sm);
}
.card-title {
  font-size: 13px;
  color: var(--text);
  margin-bottom: 8px;
  line-height: 1.4;
}
.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.assignee {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-secondary);
}
.card-date {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-secondary);
}
.overdue {
  color: #ff4d4f;
  font-weight: 600;
}
.column-empty {
  text-align: center;
  padding: 24px 0;
  color: var(--text-placeholder);
  font-size: 13px;
}
</style>
