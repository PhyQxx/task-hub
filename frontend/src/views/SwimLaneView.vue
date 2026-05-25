<template>
  <div class="swimlane-view">
    <div class="view-header">
      <div class="header-left">
        <h1 class="view-title">泳道图</h1>
        <p class="view-subtitle">按成员视角平铺的任务时间轴</p>
      </div>
      <div class="header-right">
        <span class="proj-label">{{ projectStore.currentProject?.name || '所有项目' }}</span>
        <el-select v-model="viewDate" size="small" class="date-select">
          <el-option label="近两周" :value="14" />
          <el-option label="近一个月" :value="30" />
          <el-option label="近三个月" :value="90" />
        </el-select>
      </div>
    </div>

    <div v-loading="taskStore.loading" class="swimlane-board" ref="boardRef">
      <!-- Time Scale -->
      <div class="time-scale">
        <div class="lane-label-spacer">TEAM MEMBERS</div>
        <div class="time-ticks" ref="ticksRef">
          <span
            v-for="tick in timeTicks"
            :key="tick.date"
            class="tick"
            :class="{ isToday: tick.isToday }"
            :style="{ width: `${tickWidth}px` }"
          >
            {{ tick.label }}
          </span>
        </div>
      </div>

      <!-- Rows -->
      <div v-for="lane in swimLanes" :key="lane.memberId" class="swimlane-row">
        <div class="lane-label">
          <el-avatar :size="24" class="member-av">
            {{ lane.memberName?.slice(0, 1) }}
          </el-avatar>
          <span class="lane-name">{{ lane.memberName || '未分配' }}</span>
        </div>

        <div class="lane-timeline">
          <!-- Grid BG -->
          <div class="timeline-grid" :style="{ width: `${totalTicksWidth}px` }">
            <span
              v-for="tick in timeTicks"
              :key="tick.date"
              class="grid-cell"
              :class="{ isToday: tick.isToday, isWeekend: tick.isWeekend }"
              :style="{ width: `${tickWidth}px` }"
            ></span>
          </div>

          <!-- Task Bars -->
          <div
            v-for="task in lane.tasks"
            :key="task.id"
            class="task-bar"
            :class="[`status-${task.status.toLowerCase()}`]"
            :style="getTaskBarStyle(task)"
            @click="openTask(task)"
          >
            <div class="task-bar-content">
              <span class="task-bar-title">{{ task.title }}</span>
              <span class="task-bar-progress" :style="{ width: `${task.progress}%` }"></span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!swimLanes.length" class="empty-state">
        <span class="empty-icon">🌊</span>
        <p>当前项目下暂无任务数据</p>
      </div>
    </div>

    <!-- Task Detail Drawer -->
    <TaskDetailDrawer 
      v-model:visible="showDrawer" 
      :task-id="selectedTaskId" 
      @updated="taskStore.fetchTasks(projectStore.currentProjectId)"
      @deleted="taskStore.fetchTasks(projectStore.currentProjectId)"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useProjectStore, useTaskStore, useMemberStore } from '@/stores'
import type { Task } from '@/types'
import dayjs from 'dayjs'
import TaskDetailDrawer from '@/components/TaskDetailDrawer.vue'

const projectStore = useProjectStore()
const taskStore = useTaskStore()
const memberStore = useMemberStore()

const viewDate = ref(30)
const tickWidth = 140
const showDrawer = ref(false)
const selectedTaskId = ref<string | null>(null)

const timeTicks = computed(() => {
  const ticks = []
  const start = dayjs().subtract(2, 'day').startOf('day')
  for (let i = 0; i < viewDate.value + 5; i++) {
    const d = start.add(i, 'day')
    const weekMap = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
    ticks.push({
      date: d.format('YYYY-MM-DD'),
      label: `${d.month() + 1}/${d.date()} ${weekMap[d.day()]}`,
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
    memberId: m.memberId,
    memberName: m.nickname,
    tasks: tasks.filter(t => t.assigneeId === m.memberId),
  }))
  const unassigned = tasks.filter(t => !t.assigneeId)
  if (unassigned.length) {
    lanes.push({ memberId: 'unassigned', memberName: '未分配', tasks: unassigned })
  }
  return lanes.filter(l => l.tasks.length > 0)
})

function getTaskBarStyle(task: Task) {
  if (!task.startDate || !task.endDate) return { display: 'none' }
  const start = dayjs().subtract(2, 'day').startOf('day')
  const taskStart = dayjs(task.startDate).startOf('day')
  const taskEnd = dayjs(task.endDate).startOf('day')
  const offset = taskStart.diff(start, 'day')
  const duration = taskEnd.diff(taskStart, 'day') + 1
  return {
    left: `${offset * tickWidth + 8}px`,
    width: `${duration * tickWidth - 16}px`,
  }
}

function openTask(task: Task) {
  selectedTaskId.value = String(task.taskId || task.id)
  showDrawer.value = true
}

onMounted(async () => {
  if (projectStore.currentProjectId) {
    await taskStore.fetchTasks(projectStore.currentProjectId)
  }
})
</script>

<style scoped>
.swimlane-view {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.view-title { font-size: 20px; font-weight: 700; color: var(--text); margin-bottom: 4px; }
.view-subtitle { font-size: 13px; color: var(--text-faint); }

.header-right { display: flex; align-items: center; gap: 16px; }
.proj-label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.date-select { width: 140px; }

.swimlane-board {
  flex: 1;
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: auto;
  position: relative;
}

.time-scale {
  display: flex;
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border);
}

.lane-label-spacer {
  width: 200px;
  min-width: 200px;
  padding: 12px 20px;
  font-size: 10px;
  font-weight: 800;
  color: var(--text-faint);
  letter-spacing: 1px;
  border-right: 1px solid var(--border);
  background: var(--surface-2);
  position: sticky;
  left: 0;
  z-index: 21;
}

.time-ticks { display: flex; }
.tick {
  flex-shrink: 0;
  padding: 12px 0;
  text-align: center;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-faint);
  border-right: 1px solid var(--border-subtle);
  font-variant-numeric: tabular-nums;
}
.tick.isToday { color: var(--primary); background: var(--primary-bg); }

.swimlane-row {
  display: flex;
  border-bottom: 1px solid var(--border-subtle);
  height: 72px;
}

.lane-label {
  width: 200px;
  min-width: 200px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-right: 1px solid var(--border);
  background: var(--surface-1);
  position: sticky;
  left: 0;
  z-index: 10;
}

.member-av { background: var(--surface-3) !important; color: var(--text-secondary) !important; font-weight: 700; font-size: 11px; border: 1px solid var(--border); }
.lane-name { font-size: 13px; font-weight: 600; color: var(--text); }

.lane-timeline { flex: 1; position: relative; }
.timeline-grid { display: flex; height: 100%; position: absolute; top: 0; left: 0; }
.grid-cell { flex-shrink: 0; border-right: 1px solid var(--border-subtle); }
.grid-cell.isToday { background: rgba(91, 141, 239, 0.03); }
.grid-cell.isWeekend { background: rgba(0, 0, 0, 0.1); }

.task-bar {
  position: absolute;
  top: 18px;
  height: 36px;
  border-radius: 8px;
  background: var(--surface-elevated);
  border: 1px solid var(--border-strong);
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  z-index: 5;
}

.task-bar:hover { transform: scaleY(1.05); border-color: var(--text-muted); box-shadow: var(--shadow-md); z-index: 6; }

.task-bar-content { width: 100%; height: 100%; display: flex; align-items: center; padding: 0 12px; position: relative; }
.task-bar-title { font-size: 12px; font-weight: 600; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; position: relative; z-index: 2; }
.task-bar-progress { position: absolute; left: 0; top: 0; height: 100%; opacity: 0.2; z-index: 1; transition: width 0.3s; }

.status-todo .task-bar-progress { background: var(--text-faint); }
.status-in_progress .task-bar-progress { background: var(--primary); }
.status-done .task-bar-progress { background: var(--success); }
.status-blocked .task-bar-progress { background: var(--danger); }

.detail-wrap { display: flex; flex-direction: column; gap: 20px; }
.detail-header { border-bottom: 1px solid var(--border); padding-bottom: 12px; }
.detail-status { font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 4px; text-transform: uppercase; margin-bottom: 8px; display: inline-block; }
.detail-title { font-size: 18px; font-weight: 700; color: var(--text); margin: 0; }

.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-item label { font-size: 11px; font-weight: 700; color: var(--text-faint); text-transform: uppercase; }
.detail-item span { font-size: 13px; font-weight: 500; color: var(--text-secondary); }

.p-badge { padding: 1px 6px; border-radius: 4px; font-size: 11px; font-weight: 700; }
.p-urgent { background: var(--danger-bg); color: var(--danger); }
.p-high { background: var(--warning-bg); color: var(--warning); }
.p-medium { background: var(--primary-bg); color: var(--primary); }
.p-low { background: var(--surface-3); color: var(--text-faint); }

.detail-desc { border-top: 1px solid var(--border); padding-top: 16px; }
.detail-desc label { font-size: 11px; font-weight: 700; color: var(--text-faint); text-transform: uppercase; margin-bottom: 8px; display: block; }
.detail-desc p { font-size: 13px; line-height: 1.6; color: var(--text-secondary); margin: 0; }

.empty-state { display: flex; flex-direction: column; align-items: center; padding: 100px 0; color: var(--text-faint); }
.empty-icon { font-size: 40px; margin-bottom: 16px; opacity: 0.4; }

.status-todo { color: var(--text-faint); border-color: var(--text-faint); }
.status-in_progress { color: var(--primary); border-color: var(--primary); }
.status-done { color: var(--success); border-color: var(--success); }
.status-blocked { color: var(--danger); border-color: var(--danger); }
</style>
