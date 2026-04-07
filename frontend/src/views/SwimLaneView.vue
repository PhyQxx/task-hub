<template>
  <div class="swimlane-view">
    <div class="swimlane-header">
      <span class="project-name">{{ projectStore.currentProject?.name || '请选择项目' }}</span>
      <el-select v-model="viewDate" size="small" style="width: 200px">
        <el-option label="近两周" :value="14" />
        <el-option label="近一个月" :value="30" />
        <el-option label="近三个月" :value="90" />
      </el-select>
    </div>

    <div v-loading="taskStore.loading" class="swimlane-board" ref="boardRef">
      <!-- 时间刻度 -->
      <div class="time-scale">
        <div class="lane-label-spacer"></div>
        <div class="time-ticks" ref="ticksRef">
          <span
            v-for="tick in timeTicks"
            :key="tick.date"
            class="tick"
            :class="{ today: tick.isToday }"
            :style="{ width: `${tickWidth}px` }"
          >
            {{ tick.label }}
          </span>
        </div>
      </div>

      <!-- 泳道行 -->
      <div v-for="lane in swimLanes" :key="lane.memberId" class="swimlane-row">
        <div class="lane-label">
          <el-avatar size="small" :style="{ background: 'var(--accent)', fontSize: '10px', color: '#fff' }">
            {{ lane.memberName?.slice(0, 1) }}
          </el-avatar>
          <span class="lane-name">{{ lane.memberName || '未分配' }}</span>
        </div>

        <div class="lane-timeline" ref="timelineRef">
          <!-- 时间格子背景 -->
          <div class="timeline-grid" :style="{ width: `${totalTicksWidth}px` }">
            <span
              v-for="tick in timeTicks"
              :key="tick.date"
              class="grid-cell"
              :class="{ today: tick.isToday, weekend: tick.isWeekend }"
              :style="{ width: `${tickWidth}px` }"
            ></span>
          </div>

          <!-- 任务条 -->
          <div
            v-for="task in lane.tasks"
            :key="task.id"
            class="task-bar"
            :class="[`status-${task.status.toLowerCase()}`]"
            :style="getTaskBarStyle(task)"
            @click="openTask(task)"
            :title="task.title"
          >
            <span class="task-bar-text">{{ task.title }}</span>
            <span class="task-progress-bar" :style="{ width: `${task.progress}%` }"></span>
          </div>
        </div>
      </div>

      <div v-if="!swimLanes.length" class="empty-state">
        暂无泳道数据
      </div>
    </div>

    <!-- 任务详情弹窗 -->
    <el-dialog v-model="showDetail" title="任务详情" width="480px" :append-to-body="true">
      <el-descriptions :column="1" border v-if="currentTask">
        <el-descriptions-item label="任务名称">{{ currentTask.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(currentTask.status) }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ currentTask.priority }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ currentTask.assigneeName || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ currentTask.startDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="截止日期">{{ currentTask.endDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ currentTask.progress }}%</el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentTask.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useProjectStore, useTaskStore, useMemberStore } from '@/stores'
import type { Task } from '@/types'
import dayjs from 'dayjs'

const projectStore = useProjectStore()
const taskStore = useTaskStore()
const memberStore = useMemberStore()

const viewDate = ref(30)
const tickWidth = 40 // 每天宽度 px
const showDetail = ref(false)
const currentTask = ref<Task | null>(null)

const timeTicks = computed(() => {
  const ticks = []
  const start = dayjs().subtract(3, 'day').startOf('day')
  for (let i = 0; i < viewDate.value + 5; i++) {
    const d = start.add(i, 'day')
    ticks.push({
      date: d.format('YYYY-MM-DD'),
      label: d.format('MM/DD'),
      isToday: d.isSame(dayjs(), 'day'),
      isWeekend: d.day() === 0 || d.day() === 6,
    })
  }
  return ticks
})

const totalTicksWidth = computed(() => timeTicks.value.length * tickWidth)

const swimLanes = computed(() => {
  const members = memberStore.members
  const tasks = taskStore.tasks
  const lanes = members.map(m => ({
    memberId: m.id,
    memberName: m.name,
    memberAvatar: m.avatar,
    tasks: tasks.filter(t => t.assigneeId === m.id),
  }))
  // 加上未分配泳道
  const unassigned = tasks.filter(t => !t.assigneeId)
  if (unassigned.length) {
    lanes.push({ memberId: 'unassigned', memberName: '未分配', tasks: unassigned })
  }
  return lanes
})

function getTaskBarStyle(task: Task) {
  if (!task.startDate || !task.endDate) return { display: 'none' }
  const start = dayjs().subtract(3, 'day').startOf('day')
  const taskStart = dayjs(task.startDate).startOf('day')
  const taskEnd = dayjs(task.endDate).startOf('day')
  const offset = Math.max(0, taskStart.diff(start, 'day'))
  const duration = Math.max(1, taskEnd.diff(taskStart, 'day') + 1)
  return {
    left: `${offset * tickWidth}px`,
    width: `${duration * tickWidth - 4}px`,
  }
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    TODO: '待办', IN_PROGRESS: '进行中', BLOCKED: '已阻塞', DONE: '已完成'
  }
  return map[status] || status
}

function openTask(task: Task) {
  currentTask.value = task
  showDetail.value = true
}

watch(() => projectStore.currentProjectId, async (id) => {
  if (id) {
    await taskStore.fetchTasks(id)
    await memberStore.fetchMembers()
  }
}, { immediate: true })
</script>

<style scoped>
.swimlane-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--surface-2);
  padding: 16px;
  gap: 12px;
  overflow: hidden;
}
.swimlane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--surface-1);
  padding: 12px 16px;
  border-radius: var(--radius-md);
}
.project-name { font-weight: 600; font-size: 15px; }
.swimlane-board {
  flex: 1;
  overflow: auto;
  background: var(--surface-1);
  border-radius: var(--radius-md);
}
.time-scale {
  display: flex;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--surface-1);
  border-bottom: 1px solid var(--border-light);
}
.lane-label-spacer {
  width: 120px;
  min-width: 120px;
  border-right: 1px solid var(--border-light);
}
.time-ticks {
  display: flex;
}
.tick {
  text-align: center;
  font-size: 11px;
  color: var(--text-secondary);
  padding: 6px 0;
  border-right: 1px solid var(--border-light);
}
.tick.today {
  color: var(--accent);
  font-weight: 700;
  background: var(--accent-light);
}
.swimlane-row {
  display: flex;
  border-bottom: 1px solid var(--border-light);
  min-height: 60px;
}
.lane-label {
  width: 120px;
  min-width: 120px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-right: 1px solid var(--border-light);
  position: sticky;
  left: 0;
  background: var(--surface-1);
  z-index: 5;
}
.lane-name {
  font-size: 12px;
  color: var(--text);
  font-weight: 500;
}
.lane-timeline {
  flex: 1;
  position: relative;
  min-height: 52px;
  overflow: hidden;
}
.timeline-grid {
  display: flex;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}
.grid-cell {
  height: 100%;
  border-right: 1px solid var(--border-light);
}
.grid-cell.today { background: rgba(51, 112, 255, 0.05); }
.grid-cell.weekend { background: rgba(0,0,0,0.02); }
.task-bar {
  position: absolute;
  top: 10px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  cursor: pointer;
  overflow: hidden;
  font-size: 11px;
  color: #fff;
  font-weight: 500;
  min-width: 20px;
  transition: opacity 0.2s;
}
.task-bar:hover { opacity: 0.85; }
.task-bar.status-todo { background: #bfbfbf; }
.task-bar.status-in_progress { background: var(--accent); }
.task-bar.status-blocked { background: #ff4d4f; }
.task-bar.status-done { background: #52c41a; }
.task-bar-text {
  position: relative;
  z-index: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.task-progress-bar {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: rgba(0,0,0,0.2);
  transition: width 0.3s;
}
.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text-placeholder);
}
</style>
