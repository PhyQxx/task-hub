<template>
  <div class="stats-bar">
    <div class="stats-scroll">
      <div class="stat-group">
        <div class="stat-item">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">总任务</span>
        </div>
        <div class="stat-item">
          <span class="stat-value text-success">+{{ weeklyNew }}</span>
          <span class="stat-label">本周新增</span>
        </div>
      </div>
      
      <div class="stat-sep"></div>

      <div class="stat-group">
        <div class="stat-item">
          <span class="stat-value text-primary">{{ inProgress }}</span>
          <span class="stat-label">进行中</span>
        </div>
        <div class="stat-item">
          <span class="stat-value text-success">{{ done }}</span>
          <span class="stat-label">已完成</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ completionRate }}%</span>
          <span class="stat-label">完成率</span>
        </div>
      </div>

      <div class="stat-sep"></div>

      <div class="stat-group">
        <div class="stat-item">
          <span class="stat-value text-danger">{{ blocked }}</span>
          <span class="stat-label">已阻塞</span>
        </div>
        <div class="stat-item">
          <span class="stat-value text-warning">{{ overdue }}</span>
          <span class="stat-label">逾期</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ weeklyDue }}</span>
          <span class="stat-label">本周到期</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTaskStore } from '@/stores'

const taskStore = useTaskStore()

const tasks = computed(() => taskStore.tasks || [])
const total = computed(() => tasks.value.length)
const weeklyNew = computed(() => {
  const weekAgo = new Date()
  weekAgo.setDate(weekAgo.getDate() - 7)
  return tasks.value.filter((t: any) => t.createdAt && new Date(t.createdAt) >= weekAgo).length
})
const inProgress = computed(() => tasks.value.filter((t: any) => t.status === 'IN_PROGRESS').length)
const done = computed(() => tasks.value.filter((t: any) => t.status === 'DONE').length)
const blocked = computed(() => tasks.value.filter((t: any) => t.status === 'BLOCKED').length)
const completionRate = computed(() => total.value ? Math.round((done.value / total.value) * 100) : 0)

const overdue = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  return tasks.value.filter((t: any) => t.endDate && t.endDate < today && t.status !== 'DONE').length
})
const weeklyDue = computed(() => {
  const now = new Date()
  const endOfWeek = new Date(now)
  endOfWeek.setDate(now.getDate() + (7 - now.getDay()))
  return tasks.value.filter((t: any) => {
    if (!t.endDate) return false
    return t.endDate >= now.toISOString().split('T')[0] && t.endDate <= endOfWeek.toISOString().split('T')[0]
  }).length
})
</script>

<style scoped>
.stats-bar {
  background: var(--surface-1);
  border-bottom: 1px solid var(--border);
  padding: 8px 16px;
  overflow: hidden;
}

.stats-scroll {
  display: flex;
  align-items: center;
  gap: 24px;
  overflow-x: auto;
}

.stat-group {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: fit-content;
}

.stat-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
  line-height: 1.2;
}

.stat-label {
  font-size: 10px;
  font-weight: 500;
  color: var(--text-faint);
  text-transform: uppercase;
  letter-spacing: 0.2px;
}

.stat-sep {
  width: 1px;
  height: 20px;
  background: var(--border);
  flex-shrink: 0;
}

.text-success { color: var(--success); }
.text-primary { color: var(--primary); }
.text-danger { color: var(--danger); }
.text-warning { color: var(--warning); }

/* Hide scrollbar */
.stats-scroll::-webkit-scrollbar { display: none; }
.stats-scroll { -ms-overflow-style: none; scrollbar-width: none; }
</style>

