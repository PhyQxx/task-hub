<template>
  <div class="member-card">
    <div class="card-header">
      <div class="member-avatar" :style="{ background: member.color }">
        {{ member.name.slice(0, 1) }}
      </div>
      <div class="member-info">
        <div class="member-name">{{ member.name }}</div>
        <div class="member-role">{{ member.role }}</div>
      </div>
      <div class="member-status" :class="'status-' + member.status">
        {{ statusLabel(member.status) }}
      </div>
    </div>
    <div class="card-stats">
      <div class="card-stat">
        <span class="card-stat-value">{{ member.inProgressCount }}</span>
        <span class="card-stat-label">进行中</span>
      </div>
      <div class="card-stat">
        <span class="card-stat-value">{{ member.weeklyDoneCount }}</span>
        <span class="card-stat-label">本周完成</span>
      </div>
      <div class="card-stat">
        <span class="card-stat-value" :class="loadClass(member.load)">{{ member.load }}%</span>
        <span class="card-stat-label">工作负载</span>
      </div>
    </div>
    <div class="load-bar">
      <div class="load-bar-fill" :class="loadClass(member.load)" :style="{ width: member.load + '%' }"></div>
    </div>
    <div v-if="member.load >= 80" class="load-warning">
      ⚠ 超载预警
    </div>
  </div>
</template>

<script setup lang="ts">
interface Member {
  id: string
  name: string
  role: string
  status: 'idle' | 'working' | 'overloaded'
  inProgressCount: number
  weeklyDoneCount: number
  load: number
  color: string
}

const props = defineProps<{ member: Member }>()

function statusLabel(status: string) {
  return { idle: '空闲', working: '工作中', overloaded: '超载' }[status] || '未知'
}
function loadClass(load: number) {
  if (load >= 90) return 'load-critical'
  if (load >= 80) return 'load-warning'
  return 'load-normal'
}
</script>

<style scoped>
.member-card {
  background: var(--surface-3);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-lg);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card-header { display: flex; align-items: center; gap: 12px; }
.member-avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; color: #fff;
  flex-shrink: 0;
}
.member-info { flex: 1; }
.member-name { font-size: 14px; font-weight: 600; color: var(--text); }
.member-role { font-size: 12px; color: var(--text-faint); margin-top: 2px; }
.member-status {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.status-idle { background: var(--surface-4); color: var(--text-faint); }
.status-working { background: var(--success-bg); color: var(--success); }
.status-overloaded { background: var(--danger-bg); color: var(--danger); }
.card-stats { display: flex; gap: 16px; }
.card-stat { display: flex; flex-direction: column; gap: 2px; }
.card-stat-value { font-size: 18px; font-weight: 700; color: var(--text); }
.card-stat-label { font-size: 11px; color: var(--text-faint); }
.load-bar { height: 4px; background: var(--surface-5); border-radius: 2px; overflow: hidden; }
.load-bar-fill { height: 100%; border-radius: 2px; transition: width 0.3s; }
.load-normal { background: var(--success); }
.load-warning { background: var(--warning); }
.load-critical { background: var(--danger); }
.load-warning-text { color: var(--warning); font-size: 11px; }
</style>
