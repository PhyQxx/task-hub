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
const tickWidth = 120 // 每天宽度 px（原型为 120px）
const showDetail = ref(false)
const currentTask = ref<Task | null>(null)

const timeTicks = computed(() => {
  const ticks = []
  const start = dayjs().subtract(3, 'day').startOf('day')
  for (let i = 0; i < viewDate.value + 5; i++) {
    const d = start.add(i, 'day')
    const weekDayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    ticks.push({
      date: d.format('YYYY-MM-DD'),
      label: `${d.month() + 1}/${d.date()} ${weekDayNames[d.day()]}`,
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
  await taskStore.fetchTasks(id || '')
  await memberStore.fetchMembers()
}, { immediate: true })
</script>

<style scoped>
.swimlane-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 24px;
  gap: 16px;
  overflow: auto;
}
.swimlane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #222226;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.05);
}
.project-name { font-weight: 590; font-size: 13px; color: var(--text); }
.swimlane-board {
  flex: 1;
  overflow: auto;
  background: #191a1b;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.05);
  max-height: 560px;
}
.time-scale {
  display: flex;
  position: sticky;
  top: 0;
  z-index: 10;
  background: #222226;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  min-width: 1080px;
}
.lane-label-spacer {
  width: 180px;
  min-width: 180px;
  border-right: 1px solid rgba(255,255,255,0.05);
  padding: 8px 12px;
  font-size: 11px;
  font-weight: 590;
  color: #62666d;
  text-transform: uppercase;
  letter-spacing: 0.36px;
}
.time-ticks { display: flex; }
.tick {
  width: 120px;
  flex-shrink: 0;
  text-align: center;
  font-size: 12px;
  font-weight: 590;
  color: #8a8f98;
  padding: 10px 8px;
  border-right: 1px solid rgba(255,255,255,0.04);
  font-variant-numeric: tabular-nums;
}
.tick.today { color: var(--primary); background: rgba(91,90,255,0.08); }
.swimlane-row {
  display: flex;
  min-width: 1080px;
  height: 64px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.lane-label {
  width: 180px;
  min-width: 180px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-right: 1px solid rgba(255,255,255,0.05);
  position: sticky;
  left: 0;
  background: #0f1011;
  z-index: 5;
}
.lane-name { font-size: 13px; font-weight: 590; color: var(--text-secondary); }
.lane-timeline {
  flex: 1;
  position: relative;
  height: 64px;
  display: flex;
}
.timeline-grid {
  display: flex;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}
.grid-cell {
  width: 120px;
  flex-shrink: 0;
  height: 100%;
  border-right: 1px solid rgba(255,255,255,0.04);
}
.grid-cell.today { background: rgba(91,90,255,0.06); }
.grid-cell.weekend { background: rgba(0,0,0,0.15); }
.task-bar {
  position: absolute;
  top: 18px;
  height: 28px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  cursor: pointer;
  font-size: 12px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  box-shadow: 0 1px 3px rgba(0,0,0,0.3);
  transition: filter 0.12s;
  min-width: 20px;
}
.task-bar:hover { filter: brightness(1.15); }
.task-bar.status-todo { background: linear-gradient(90deg,#6366F1,#4F46E5); }
.task-bar.status-in_progress { background: linear-gradient(90deg,#10B981,#059669); }
.task-bar.status-blocked { background: linear-gradient(90deg,#ef4444,#dc2626); }
.task-bar.status-done { background: linear-gradient(90deg,#34D399,#10B981); opacity: 0.7; }
.task-bar-text {
  position: relative;
  z-index: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text-faint);
  font-size: 13px;
}
</style>
