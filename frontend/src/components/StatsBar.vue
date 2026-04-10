<template>
  <div class="stats-bar">
    <div class="stat-item">
      <span class="stat-label">总任务</span>
      <span class="stat-value">{{ total }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">本周新增</span>
      <span class="stat-value" style="color:var(--success)">+{{ weeklyNew }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">进行中</span>
      <span class="stat-value" style="color:var(--primary)">{{ inProgress }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">环比</span>
      <span class="stat-value" :style="weekOverWeek >= 0 ? 'color:var(--success)' : 'color:var(--danger)'">
        {{ weekOverWeek >= 0 ? '+' : '' }}{{ weekOverWeek }}
      </span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">已完成</span>
      <span class="stat-value" style="color:var(--success)">{{ done }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">完成率</span>
      <span class="stat-value">{{ completionRate }}%</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">已阻塞</span>
      <span class="stat-value" style="color:var(--danger)">{{ blocked }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">需关注</span>
      <span class="stat-value" style="color:var(--warning)">{{ attention }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">逾期</span>
      <span class="stat-value" style="color:var(--danger)">{{ overdue }}</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-label">本周到期</span>
      <span class="stat-value">{{ weeklyDue }}</span>
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
const weekOverWeek = computed(() => 2) // TODO: real calculation
const attention = computed(() => blocked.value)
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
  display: flex;
  align-items: center;
  gap: 0;
  padding: 10px 20px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border);
  overflow-x: auto;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 0 16px;
  min-width: fit-content;
}
.stat-label { font-size: 11px; color: var(--text-faint); white-space: nowrap; }
.stat-value { font-size: 18px; font-weight: 700; color: var(--text); line-height: 1; }
.stat-divider { width: 1px; height: 32px; background: var(--border); flex-shrink: 0; }
</style>
