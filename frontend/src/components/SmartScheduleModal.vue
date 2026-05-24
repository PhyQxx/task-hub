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
            <span class="schedule-load">({{ item.reason }})</span>
          </div>
          <div class="schedule-score">
            得分：<strong>{{ item.score }} / 100</strong>
            <span v-if="item.score >= 90" class="schedule-flag">✅ 最优</span>
            <span v-else-if="item.score < 70" class="schedule-flag">⚠ 风险提示</span>
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
</script>

<style scoped>
.smart-schedule-modal { width: 480px; }
.modal-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.modal-icon { font-size: 18px; }
.modal-title { font-size: 15px; font-weight: 700; color: var(--text); flex: 1; }
.modal-close { background: none; border: none; font-size: 20px; color: var(--text-faint); cursor: pointer; }
.modal-desc { font-size: 12px; color: var(--text-faint); margin-bottom: 16px; }
.schedule-list { display: flex; flex-direction: column; gap: 10px; }
.schedule-item {
  background: var(--surface-4);
  border-radius: var(--radius-md);
  padding: 12px;
  border-left: 3px solid var(--primary);
}
.schedule-task { font-size: 13px; font-weight: 500; color: var(--text-secondary); margin-bottom: 4px; }
.schedule-recommend { font-size: 12px; color: var(--text-faint); margin-bottom: 2px; }
.schedule-score { font-size: 12px; color: var(--text-faint); }
.schedule-load { margin-left: 4px; }
.schedule-flag { margin-left: 8px; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
</style>
