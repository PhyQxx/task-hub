<template>
  <div class="task-comments">
    <!-- Header with count -->
    <div class="comments-header">
      <span class="count-badge">{{ comments.length }}</span>
      <span class="header-text">条评论</span>
    </div>

    <!-- Input Area -->
    <div class="comment-input-area">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="3"
        placeholder="写下你的评论..."
        maxlength="500"
        class="modern-textarea"
      />
      <div class="input-actions">
        <span class="char-count">{{ newComment.length }} / 500</span>
        <el-button 
          type="primary" 
          size="small" 
          :disabled="!newComment.trim()" 
          :loading="sending" 
          @click="submitComment"
          class="send-btn"
        >
          发送评论
        </el-button>
      </div>
    </div>

    <!-- List Area -->
    <div v-loading="loading" class="comments-container">
      <div v-if="!comments.length" class="empty-placeholder">
        <span class="icon">💬</span>
        <p>暂无讨论，发表第一条见解吧</p>
      </div>
      
      <div v-for="c in comments" :key="c.commentId || c.id" class="comment-card">
        <div class="comment-sidebar">
          <div class="avatar-mini" :style="{ background: getAvatarColor(parseNickname(c.userId)) }">
            {{ parseNickname(c.userId).slice(0, 1) }}
          </div>
        </div>
        <div class="comment-main">
          <div class="comment-top">
            <span class="user-name">{{ parseNickname(c.userId) }}</span>
            <span class="dot">·</span>
            <span class="time-stamp">{{ formatTime(c.createdAt) }}</span>
            <div class="spacer"></div>
            <el-button 
              v-if="isMyComment(c.userId)" 
              link 
              size="small" 
              class="delete-btn" 
              @click="deleteComment(c.commentId || c.id)"
            >
              删除
            </el-button>
          </div>
          <div class="comment-text">{{ c.content }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { commentApi } from '@/api'
import { useMemberStore, useAuthStore } from '@/stores'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const props = defineProps<{ taskId: string }>()

const memberStore = useMemberStore()
const authStore = useAuthStore()

const comments = ref<any[]>([])
const newComment = ref('')
const loading = ref(false)
const sending = ref(false)

const currentUserId = computed(() => authStore.memberId)

/**
 * Parsing logic to handle raw object strings from backend 
 * (e.g. "LoginUser[memberId=..., nickname=裴浩宇, ...]")
 */
function parseNickname(userIdRaw: string) {
  if (!userIdRaw) return '未知用户'
  
  // 1. Try string matching for raw Java object string
  if (typeof userIdRaw === 'string' && userIdRaw.includes('nickname=')) {
    const match = userIdRaw.match(/nickname=([^,\]]+)/)
    if (match && match[1]) return match[1].trim()
  }
  
  // 2. Lookup in member store
  const m = memberStore.members.find(m => m.memberId === userIdRaw)
  if (m) return m.nickname
  
  return userIdRaw.length > 20 ? '系统用户' : userIdRaw
}

function isMyComment(userIdRaw: string) {
  const myId = currentUserId.value
  if (!myId || !userIdRaw) return false
  return String(userIdRaw).includes(String(myId))
}

function formatTime(t: string) {
  if (!t) return ''
  return dayjs(t).format('YYYY-MM-DD HH:mm')
}

function getAvatarColor(name: string) {
  if (!name) return '#5B8DEF'
  const colors = ['#5B8DEF', '#32d583', '#f5a623', '#ec5f5f', '#8b5cf6', '#ec4899']
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

async function fetchComments() {
  if (!props.taskId) return
  loading.value = true
  try {
    const res = await commentApi.listByTask(props.taskId)
    comments.value = (res.data as any)?.data ?? (Array.isArray(res.data) ? res.data : [])
  } catch {
    comments.value = []
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!newComment.value.trim()) return
  sending.value = true
  try {
    await commentApi.create({ taskId: props.taskId, content: newComment.value.trim() })
    newComment.value = ''
    await fetchComments()
  } catch (e: any) {
    ElMessage.error('发表评论失败')
  } finally {
    sending.value = false
  }
}

async function deleteComment(commentId: string) {
  try {
    await commentApi.delete(commentId)
    await fetchComments()
  } catch (e: any) {
    ElMessage.error('删除失败')
  }
}

watch(() => props.taskId, () => { fetchComments() })
onMounted(() => { fetchComments() })
</script>

<style scoped>
.task-comments {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comments-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.count-badge {
  background: var(--surface-3);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}

.header-text {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.comment-input-area {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px;
  transition: border-color 0.2s;
}

.comment-input-area:focus-within {
  border-color: var(--primary);
}

.modern-textarea :deep(.el-textarea__inner) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0 !important;
  font-size: 14px;
  color: var(--text-secondary);
  resize: none;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-subtle);
}

.char-count {
  font-size: 11px;
  color: var(--text-faint);
}

.comments-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--surface-1);
  border: 1px solid transparent;
  transition: all 0.2s;
}

.comment-card:hover {
  background: var(--surface-2);
  border-color: var(--border-subtle);
}

.avatar-mini {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.dot { color: var(--text-faint); }

.time-stamp {
  font-size: 11px;
  color: var(--text-faint);
}

.spacer { flex: 1; }

.delete-btn {
  padding: 0;
  height: auto;
  color: var(--text-faint) !important;
}

.delete-btn:hover {
  color: var(--danger) !important;
}

.comment-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty-placeholder {
  padding: 40px 0;
  text-align: center;
  color: var(--text-faint);
}

.empty-placeholder .icon { font-size: 24px; opacity: 0.3; margin-bottom: 8px; display: block; }
.empty-placeholder p { font-size: 12px; margin: 0; }
</style>
