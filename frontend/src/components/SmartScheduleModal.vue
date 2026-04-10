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
      <div class="schedule-list">
        <div v-for="item in recommendations" :key="item.taskId" class="schedule-item">
          <div class="schedule-task">{{ item.taskName }}</div>
          <div class="schedule-recommend">
            推荐执行人：<strong>{{ item.assignee }}</strong>
            <span class="schedule-load">({{ item.loadDesc }})</span>
          </div>
          <div class="schedule-score">
            得分：<strong>{{ item.score }}</strong>
            <span v-if="item.flag" class="schedule-flag">{{ item.flag }}</span>
          </div>
        </div>
      </div>
      <div class="modal-actions">
        <button class="btn btn-ghost" @click="$emit('close')">关闭</button>
        <button class="btn btn-primary" @click="applyAll">应用推荐</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ visible: boolean }>()
defineEmits(['close'])

const recommendations = [
  { taskId: '1', taskName: '甘特图前端组件开发', assignee: 'Dev', loadDesc: '负载 70%，技能匹配 95%', score: '87.5 / 100', flag: '' },
  { taskId: '2', taskName: '智能排程推荐算法', assignee: 'Dev', loadDesc: '当前负载过高，建议拆分任务', score: '62.0 / 100', flag: '⚠ 风险提示' },
  { taskId: '3', taskName: '数据库建表 SQL', assignee: 'Ops', loadDesc: '负载 28%，技能完全匹配', score: '94.0 / 100', flag: '✅ 最优' },
]

function applyAll() {
  // TODO: call API to apply recommendations
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
