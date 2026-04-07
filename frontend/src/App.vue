<template>
  <div class="app-layout">
    <!-- 顶部导航 -->
    <header class="top-bar">
      <div class="top-bar-left">
        <div class="logo">
          <span class="logo-icon">📋</span>
          <span class="logo-text">任务舱</span>
        </div>
      </div>

      <div class="top-bar-center">
        <el-select
          v-model="currentProjectId"
          placeholder="选择项目"
          size="small"
          class="project-selector"
          @change="onProjectChange"
        >
          <el-option
            v-for="p in projectStore.projects"
            :key="p.id"
            :label="p.name"
            :value="p.id"
          />
        </el-select>
      </div>

      <div class="top-bar-right">
        <el-dropdown trigger="click">
          <div class="user-avatar">
            <el-avatar :style="{ background: 'var(--accent)', fontSize: '12px' }">
              {{ userName.slice(0, 1) }}
            </el-avatar>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>{{ userName }}</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- Tab 导航 -->
    <nav class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </button>
    </nav>

    <!-- 主内容区 -->
    <main class="main-content">
      <GanttView v-show="activeTab === 'gantt'" />
      <KanbanView v-show="activeTab === 'kanban'" />
      <SwimLaneView v-show="activeTab === 'swimlane'" />
      <WorkLogView v-show="activeTab === 'worklog'" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useProjectStore } from '@/stores'
import GanttView from '@/views/GanttView.vue'
import KanbanView from '@/views/KanbanView.vue'
import SwimLaneView from '@/views/SwimLaneView.vue'
import WorkLogView from '@/views/WorkLogView.vue'

const projectStore = useProjectStore()

const activeTab = ref('gantt')

const tabs = [
  { key: 'gantt', label: '甘特图', icon: '📊' },
  { key: 'kanban', label: '看板', icon: '📋' },
  { key: 'swimlane', label: '泳道视图', icon: '🏊' },
  { key: 'worklog', label: '工作日志', icon: '📝' },
]

const currentProjectId = computed({
  get: () => projectStore.currentProjectId,
  set: (v) => projectStore.selectProject(v),
})

const userName = localStorage.getItem('userName') || '用户'

function onProjectChange(id: string) {
  projectStore.selectProject(id)
}

function handleLogout() {
  localStorage.clear()
  location.reload()
}

onMounted(async () => {
  await projectStore.fetchProjects()
})
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: var(--surface-2);
}

/* 顶部栏 */
.top-bar {
  height: 56px;
  background: var(--surface-1);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 16px;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
  z-index: 100;
}
.top-bar-left { flex: 1; }
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}
.logo-icon { font-size: 20px; }
.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--accent);
  letter-spacing: 0.5px;
}
.top-bar-center { flex: 2; display: flex; justify-content: center; }
.top-bar-right { flex: 1; display: flex; justify-content: flex-end; }
.project-selector { width: 200px; }
.user-avatar { cursor: pointer; }

/* Tab 栏 */
.tab-bar {
  display: flex;
  background: var(--surface-1);
  border-bottom: 1px solid var(--border-light);
  padding: 0 20px;
  gap: 4px;
  flex-shrink: 0;
}
.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  font-family: inherit;
}
.tab-btn:hover {
  color: var(--accent);
  background: var(--accent-light);
}
.tab-btn.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
  font-weight: 600;
}
.tab-icon { font-size: 14px; }

/* 主内容 */
.main-content {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}
</style>
