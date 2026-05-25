<template>
  <Transition name="fade">
    <div v-if="visible" class="cmd-overlay" @click.self="close">
      <div class="cmd-dialog">
        <div class="cmd-search-wrap">
          <span class="cmd-icon">🔍</span>
          <input
            ref="inputRef"
            v-model="query"
            class="cmd-input"
            placeholder="搜索任务、导航或执行操作..."
            @keydown.down.prevent="moveDown"
            @keydown.up.prevent="moveUp"
            @keydown.enter="executeAction"
          />
          <span class="cmd-esc">ESC</span>
        </div>

        <div class="cmd-results" ref="resultsRef">
          <div v-for="(group, gIdx) in groups" :key="group.title" class="cmd-group">
            <div v-if="group.items.length" class="cmd-group-title">{{ group.title }}</div>
            <div
              v-for="(item, iIdx) in group.items"
              :key="item.id"
              class="cmd-item"
              :class="{ active: isSelected(gIdx, iIdx) }"
              @mouseenter="selectedIndex = absoluteIndex(gIdx, iIdx)"
              @click="executeAction"
            >
              <span class="item-icon">{{ item.icon }}</span>
              <span class="item-label">{{ item.label }}</span>
              <span v-if="item.shortcut" class="item-shortcut">{{ item.shortcut }}</span>
            </div>
          </div>
          
          <div v-if="!totalItems" class="cmd-no-results">
            未找到相关结果
          </div>
        </div>
        
        <div class="cmd-footer">
          <div class="footer-tip"><span>↑↓</span> 选择</div>
          <div class="footer-tip"><span>ENTER</span> 执行</div>
          <div class="footer-tip"><span>ESC</span> 关闭</div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useTaskStore, useProjectStore, useUIStore } from '@/stores'

const router = useRouter()
const taskStore = useTaskStore()
const projectStore = useProjectStore()
const uiStore = useUIStore()

const visible = ref(false)
const query = ref('')
const selectedIndex = ref(0)
const inputRef = ref<HTMLInputElement>()
const resultsRef = ref<HTMLElement>()

const navigationItems = [
  { id: 'nav-home', label: '回到 首页概览', icon: '🏠', action: () => router.push('/') },
  { id: 'nav-gantt', label: '跳转到 甘特图', icon: '📊', action: () => router.push('/gantt') },
  { id: 'nav-kanban', label: '跳转到 看板', icon: '📋', action: () => router.push('/kanban') },
  { id: 'nav-member', label: '跳转到 成员负载', icon: '👥', action: () => router.push('/member') },
  { id: 'nav-milestone', label: '跳转到 项目里程碑', icon: '🏁', action: () => router.push('/milestone') },
  { id: 'nav-worklog', label: '跳转到 工作日志', icon: '📝', action: () => router.push('/worklog') },
]

const actionItems = computed(() => [
  { id: 'act-new-task', label: '新建任务', icon: '➕', shortcut: 'N', action: () => { window.dispatchEvent(new CustomEvent('cmd-new-task')) } },
  { id: 'act-theme', label: `切换到 ${uiStore.theme === 'dark' ? '明亮' : '深色'} 模式`, icon: '🌓', shortcut: 'T', action: () => { uiStore.toggleTheme() } },
  { id: 'act-refresh', label: '刷新当前数据', icon: '🔄', shortcut: 'R', action: () => { window.location.reload() } },
])

const taskItems = computed(() => {
  if (!query.value) return []
  return taskStore.tasks
    .filter(t => t.title.toLowerCase().includes(query.value.toLowerCase()))
    .slice(0, 8)
    .map(t => ({
      id: `task-${t.id}`,
      label: t.title,
      icon: '📝',
      action: () => { window.dispatchEvent(new CustomEvent('cmd-open-task', { detail: { taskId: String(t.taskId || t.id) } })) }
    }))
})

const groups = computed(() => [
  { title: '常用操作', items: actionItems.value.filter(i => i.label.includes(query.value)) },
  { title: '视图导航', items: navigationItems.filter(i => i.label.includes(query.value)) },
  { title: '匹配任务', items: taskItems.value },
].filter(g => g.items.length > 0))

const totalItems = computed(() => groups.value.reduce((acc, g) => acc + g.items.length, 0))

function absoluteIndex(gIdx: number, iIdx: number) {
  let count = 0
  for (let i = 0; i < gIdx; i++) count += groups.value[i].items.length
  return count + iIdx
}

function isSelected(gIdx: number, iIdx: number) {
  return selectedIndex.value === absoluteIndex(gIdx, iIdx)
}

function moveDown() {
  selectedIndex.value = (selectedIndex.value + 1) % totalItems.value
}

function moveUp() {
  selectedIndex.value = (selectedIndex.value - 1 + totalItems.value) % totalItems.value
}

function executeAction() {
  let count = 0
  for (const group of groups.value) {
    for (const item of group.items) {
      if (count === selectedIndex.value) {
        item.action()
        close()
        return
      }
      count++
    }
  }
}

function close() {
  visible.value = false
  query.value = ''
  selectedIndex.value = 0
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    visible.value = !visible.value
  }
  if (e.key === 'Escape') close()
}

watch(visible, (v) => {
  if (v) {
    nextTick(() => inputRef.value?.focus())
  }
})

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.cmd-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  justify-content: center;
  padding-top: 15vh;
}

.cmd-dialog {
  width: 640px;
  max-width: 90vw;
  background: var(--surface-1);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: fit-content;
  max-height: 60vh;
}

.cmd-search-wrap {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  gap: 12px;
}

.cmd-icon { font-size: 18px; opacity: 0.5; }

.cmd-input {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--text);
  font-size: 16px;
  outline: none;
}

.cmd-esc {
  font-size: 10px;
  font-weight: 800;
  color: var(--text-faint);
  background: var(--surface-3);
  padding: 2px 6px;
  border-radius: 4px;
}

.cmd-results {
  overflow-y: auto;
  padding: 8px;
}

.cmd-group-title {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-faint);
  padding: 12px 12px 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.cmd-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.1s;
}

.cmd-item.active {
  background: var(--primary-bg);
  color: var(--primary);
}

.item-icon { font-size: 16px; }
.item-label { flex: 1; font-size: 14px; font-weight: 500; }
.item-shortcut { font-size: 11px; opacity: 0.5; }

.cmd-no-results {
  padding: 40px;
  text-align: center;
  color: var(--text-faint);
  font-size: 14px;
}

.cmd-footer {
  background: var(--surface-2);
  padding: 10px 20px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  gap: 16px;
}

.footer-tip {
  font-size: 11px;
  color: var(--text-faint);
  display: flex;
  align-items: center;
  gap: 4px;
}

.footer-tip span {
  background: var(--surface-4);
  padding: 1px 4px;
  border-radius: 3px;
  color: var(--text-secondary);
  font-weight: 700;
}

/* Transitions */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: scale(0.98); }
</style>
