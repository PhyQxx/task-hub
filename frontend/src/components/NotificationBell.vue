<template>
  <div class="notification-bell" @click="togglePanel">
    <el-badge :value="notificationStore.unreadCount" :hidden="!notificationStore.unreadCount" :max="99">
      <span class="bell-icon">🔔</span>
    </el-badge>

    <!-- 通知面板 -->
    <div v-if="showPanel" class="notification-panel" @click.stop>
      <div class="panel-header">
        <span class="panel-title">通知</span>
        <el-button v-if="notificationStore.unreadCount" text size="small" @click="notificationStore.markAllAsRead()">全部已读</el-button>
      </div>

      <div class="panel-body">
        <div v-if="!notificationStore.notifications.length" class="empty-notice">暂无通知</div>
        <div
          v-for="n in notificationStore.notifications"
          :key="n.id"
          class="notice-item"
          :class="{ unread: !n.read }"
          @click="notificationStore.markAsRead(n.id)"
        >
          <div class="notice-icon">{{ typeIcon(n.type) }}</div>
          <div class="notice-content">
            <div class="notice-title">{{ n.title }}</div>
            <div class="notice-message">{{ n.message }}</div>
            <div class="notice-time">{{ n.time }}</div>
          </div>
        </div>
      </div>

      <div v-if="notificationStore.notifications.length" class="panel-footer">
        <el-button text size="small" @click="notificationStore.clearAll()">清除全部</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useNotificationStore } from '@/stores'

const notificationStore = useNotificationStore()
const showPanel = ref(false)

function togglePanel() {
  showPanel.value = !showPanel.value
}

function typeIcon(type: string) {
  const map: Record<string, string> = {
    task_created: '📋',
    task_updated: '✏️',
    task_deleted: '🗑️',
    status_change: '🔄',
    blocked: '⚠️',
    milestone: '🎯',
    worklog: '📝',
  }
  return map[type] || '🔔'
}

// 点击外部关闭面板
document.addEventListener('click', () => { showPanel.value = false })
</script>

<style scoped>
.notification-bell {
  position: relative;
  cursor: pointer;
  padding: 4px;
}
.bell-icon {
  font-size: 18px;
}
.notification-panel {
  position: absolute;
  top: 100%;
  right: 0;
  width: 360px;
  max-height: 480px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f1f5;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  max-height: 380px;
}
.empty-notice {
  text-align: center;
  padding: 32px 0;
  color: #86909c;
  font-size: 13px;
}
.notice-item {
  display: flex;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.notice-item:hover {
  background: #f7f8fc;
}
.notice-item.unread {
  background: rgba(51,112,255,0.04);
}
.notice-icon {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 2px;
}
.notice-content {
  flex: 1;
  min-width: 0;
}
.notice-title {
  font-size: 13px;
  font-weight: 500;
  color: #1f2329;
}
.notice-message {
  font-size: 12px;
  color: #646a73;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-time {
  font-size: 11px;
  color: #86909c;
  margin-top: 4px;
}
.panel-footer {
  padding: 8px 16px;
  border-top: 1px solid #f0f1f5;
  text-align: center;
}
</style>
