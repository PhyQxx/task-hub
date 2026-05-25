<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal smart-schedule-modal">
      <div class="modal-header">
        <span class="modal-icon">🧠</span>
        <span class="modal-title">智能排程推荐</span>
        <button class="modal-close" @click="$emit('close')">×</button>
      </div>
      <p class="modal-desc">
        基于成员负载、技能匹配度、上下文压力智能推荐最优执行人
      </p>

      <div v-if="loading" style="text-align:center;padding:24px;color:var(--text-faint)">
        正在分析任务和成员负载...
      </div>

      <div v-else-if="error" style="text-align:center;padding:24px;color:var(--danger)">
        {{ error }}
      </div>

      <div v-else-if="!recommendations.length" style="text-align:center;padding:24px;color:var(--text-faint)">
        当前项目没有待排程的任务（所有任务已分配负责人）
      </div>

      <div v-else class="schedule-list">
        <div v-for="item in recommendations" :key="item.taskId" class="schedule-item">
          <div class="schedule-task">{{ item.taskName }}</div>
          <div class="schedule-recommend">
            推荐执行人：<strong>{{ item.nickname || item.assigneeId }}</strong>
            <span class="reason-badge" :class="reasonBadgeClass(item.reason)">
              {{ item.reason }}
            </span>
          </div>
          <div class="schedule-score">
            匹配度：<strong :class="scoreClass(item.score)">{{ item.score }}%</strong>
            <span v-if="item.score >= 90" class="status-tag best">最优匹配</span>
            <span v-else-if="item.score < 70" class="status-tag risk">存在过载风险</span>
          </div>
        </div>
      </div>

      <div class="modal-actions">
        <button class="btn btn-ghost" @click="$emit('close')">关闭</button>
        <button
          v-if="recommendations.length"
          class="btn btn-primary"
          :disabled="applying"
          @click="applyAll"
        >
          {{ applying ? '应用中...' : '应用推荐' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useProjectStore, useTaskStore } from '@/stores'
import { scheduleApi, taskApi } from '@/api'
import { ElMessage } from 'element-plus'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits(['close', 'applied'])

const projectStore = useProjectStore()
const taskStore = useTaskStore()

const recommendations = ref<any[]>([])
const loading = ref(false)
const applying = ref(false)
const error = ref('')

watch(() => props.visible, async (v) => {
  if (!v) return
  await fetchRecommendations()
})

async function fetchRecommendations() {
  loading.value = true
  error.value = ''
  recommendations.value = []
  try {
    const unscheduled = taskStore.tasks.filter(
      (t: any) => !t.assigneeId || t.status === 'TODO'
    )
    if (!unscheduled.length) return

    const taskIds = unscheduled.map((t: any) => t.taskId || t.id).filter(Boolean)
    if (!taskIds.length) return

    const res = await scheduleApi.batchSchedule(
      projectStore.currentProjectId,
      taskIds,
      'balance_load'
    )
    const results = res.data?.results || []

    recommendations.value = results.map((r: any) => {
      const task = unscheduled.find((t: any) =>
        (t.taskId || t.id) === r.taskId
      )
      return {
        ...r,
        taskName: task?.title || r.taskId,
      }
    })
  } catch (e: any) {
    error.value = e.message || '获取推荐失败'
  } finally {
    loading.value = false
  }
}

async function applyAll() {
  applying.value = true
  try {
    const updates = recommendations.value.map(r =>
      taskApi.update(r.taskId, { assigneeId: r.assigneeId })
    )
    await Promise.all(updates)
    ElMessage.success(`已应用 ${recommendations.value.length} 条排程推荐`)
    emit('applied')
    emit('close')
  } catch (e: any) {
    ElMessage.error(e.message || '应用推荐失败')
  } finally {
    applying.value = false
  }
}

function reasonBadgeClass(reason: string) {
  if (reason.includes('均衡') || reason.includes('负载')) return 'bg-success'
  if (reason.includes('匹配') || reason.includes('技能')) return 'bg-primary'
  if (reason.includes('顺延')) return 'bg-warning'
  return 'bg-secondary'
}

function scoreClass(score: number) {
  if (score >= 90) return 'text-success'
  if (score < 70) return 'text-danger'
  return ''
}
</script>

<style scoped>
.smart-schedule-modal { width: 480px; }
.modal-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.modal-icon { font-size: 18px; }
.modal-title { font-size: 15px; font-weight: 700; color: var(--text); flex: 1; }
.modal-close { background: none; border: none; font-size: 20px; color: var(--text-faint); cursor: pointer; }
.modal-desc { font-size: 12px; color: var(--text-faint); margin-bottom: 16px; }

.schedule-list { display: flex; flex-direction: column; gap: 12px; max-height: 400px; overflow-y: auto; padding-right: 4px; }

.schedule-item {
  background: var(--surface-2);
  border-radius: var(--radius-lg);
  padding: 16px;
  border: 1px solid var(--border-strong);
  transition: all 0.2s;
}

.schedule-item:hover { border-color: var(--primary); transform: translateX(2px); }

.schedule-task { font-size: 14px; font-weight: 700; color: var(--text); margin-bottom: 8px; }
.schedule-recommend { font-size: 12px; color: var(--text-secondary); margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }

.reason-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  color: #fff;
  text-transform: uppercase;
}

.bg-success { background: var(--success); }
.bg-primary { background: var(--primary); }
.bg-warning { background: var(--warning); }
.bg-secondary { background: var(--text-faint); }

.schedule-score { font-size: 11px; color: var(--text-faint); display: flex; align-items: center; gap: 8px; }
.text-success { color: var(--success) !important; }
.text-danger { color: var(--danger) !important; }

.status-tag { font-size: 10px; font-weight: 800; padding: 1px 6px; border-radius: 10px; }
.status-tag.best { background: var(--success-bg); color: var(--success); }
.status-tag.risk { background: var(--danger-bg); color: var(--danger); }

.modal-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--border-subtle); }
</style>
