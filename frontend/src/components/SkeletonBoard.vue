<template>
  <div class="skeleton-board" :class="{ 'is-kanban': type === 'kanban' }">
    <div v-for="i in cols" :key="i" class="skeleton-col">
      <div class="skeleton-header">
        <div class="skeleton-dot shine"></div>
        <div class="skeleton-line shine" style="width: 40%"></div>
      </div>
      <div class="skeleton-cards">
        <div v-for="j in 3" :key="j" class="skeleton-card">
          <div class="skeleton-line shine" style="width: 80%; height: 14px; margin-bottom: 12px"></div>
          <div class="skeleton-meta">
            <div class="skeleton-line shine" style="width: 30%"></div>
            <div class="skeleton-dot shine" style="width: 20px; height: 20px"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  type?: 'kanban' | 'list',
  cols?: number
}>()
</script>

<style scoped>
.skeleton-board {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 20px;
  height: 100%;
  overflow: hidden;
}

.skeleton-col {
  display: flex;
  flex-direction: column;
  background: var(--surface-1);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  padding: 12px;
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.skeleton-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--surface-3);
}

.skeleton-line {
  height: 10px;
  background: var(--surface-3);
  border-radius: 4px;
}

.skeleton-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-card {
  background: var(--surface-2);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-strong);
  padding: 12px;
}

.skeleton-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shine {
  background: linear-gradient(
    90deg,
    var(--surface-3) 25%,
    var(--surface-4) 37%,
    var(--surface-3) 63%
  );
  background-size: 400% 100%;
  animation: shine 1.4s ease infinite;
}

@keyframes shine {
  0% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@media (max-width: 900px) {
  .skeleton-board { grid-template-columns: 1fr; }
}
</style>
