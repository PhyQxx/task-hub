<template>
  <div class="task-comments">
    <div class="comments-header">
      <span class="comments-title">💬 评论 ({{ comments.length }})</span>
    </div>

    <!-- 评论输入 -->
    <div class="comment-input-wrap">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="2"
        placeholder="写下你的评论..."
        maxlength="500"
        show-word-limit
      />
      <el-button type="primary" size="small" :disabled="!newComment.trim()" :loading="sending" @click="submitComment">
        发送
      </el-button>
    </div>

    <!-- 评论列表 -->
    <div v-loading="loading" class="comments-list">
      <el-empty v-if="!comments.length" description="暂无评论" :image-size="60" />
      <div v-for="c in comments" :key="c.commentId || c.id" class="comment-item">
        <div class="comment-header">
          <span class="comment-user">{{ getUserName(c.userId) }}</span>
          <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
          <el-button v-if="c.userId === currentUserId" text size="small" type="danger" @click="deleteComment(c.commentId || c.id)">删除</el-button>
        </div>
        <div class="comment-body">{{ c.content }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
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

const currentUserId = authStore.memberId

function getUserName(userId: string) {
  const m = memberStore.members.find(m => m.memberId === userId)
  return m ? m.nickname : userId
}

function formatTime(t: string) {
  if (!t) return ''
  return dayjs(t).format('MM-DD HH:mm')
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
    ElMessage.error(e.message || '评论失败')
  } finally {
    sending.value = false
  }
}

async function deleteComment(commentId: string) {
  try {
    await commentApi.delete(commentId)
    await fetchComments()
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

watch(() => props.taskId, () => { fetchComments() })
onMounted(() => { fetchComments() })
</script>

<style scoped>
.task-comments {
  margin-top: 12px;
  border-top: 1px solid var(--border-light);
  padding-top: 12px;
}
.comments-header {
  margin-bottom: 8px;
}
.comments-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}
.comment-input-wrap {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-bottom: 12px;
}
.comment-input-wrap .el-input {
  flex: 1;
}
.comments-list {
  max-height: 300px;
  overflow-y: auto;
}
.comment-item {
  padding: 8px 0;
  border-bottom: 1px solid var(--border-light);
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.comment-user {
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
}
.comment-time {
  font-size: 11px;
  color: var(--text-faint);
}
.comment-body {
  font-size: 13px;
  color: var(--text);
  line-height: 1.5;
  white-space: pre-wrap;
}
</style>
