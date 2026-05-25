<template>
  <div class="member-manage">
    <div class="view-header">
      <div class="header-left">
        <h1 class="view-title">成员与权限管理</h1>
        <p class="view-subtitle">配置团队成员角色及全站访问权限</p>
      </div>
      <div class="header-right">
        <el-button type="primary" size="small" @click="openAddMember">+ 新增成员</el-button>
      </div>
    </div>

    <div class="card table-card">
      <el-table :data="members" style="width: 100%" v-loading="loading">
        <el-table-column label="成员" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :style="{ background: getAvatarColor(row.nickname) }">
                {{ row.nickname?.slice(0,1) }}
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.nickname }}</div>
                <div class="user-id">ID: {{ row.memberId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column label="全局角色" width="120">
          <template #default="{ row }">
            <span class="role-badge" :class="'role-' + row.role.toLowerCase()">
              {{ row.role === 'admin' ? '👑 管理员' : '👤 成员' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="负载状态" width="180">
          <template #default="{ row }">
            <div class="load-cell">
              <el-progress 
                :percentage="getMemberLoad(row.memberId)" 
                :status="getLoadStatus(getMemberLoad(row.memberId))" 
                :stroke-width="4"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editMember(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)" :disabled="row.memberId === authStore.memberId">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Member Form Dialog -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑成员' : '新增成员'" width="440px">
      <el-form :model="form" label-position="top">
        <el-form-item label="昵称" required>
          <el-input v-model="form.nickname" placeholder="请输入成员昵称" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="form.phone" placeholder="用于登录的手机号" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="设置密码" v-if="!isEdit" required>
          <el-input v-model="form.password" type="password" placeholder="至少 6 位" show-password />
        </el-form-item>
        <el-form-item label="全局权限角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="系统管理员" value="admin" />
            <el-option label="普通成员" value="user" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMember" :loading="saving">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { memberApi, taskApi } from '@/api'
import { useAuthStore } from '@/stores'
import { ElMessage, ElMessageBox } from 'element-plus'

const authStore = useAuthStore()
const members = ref<any[]>([])
const allTasks = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)

const form = reactive({
  memberId: '',
  nickname: '',
  phone: '',
  password: '',
  role: 'user'
})

function getMemberLoad(memberId: string) {
  const activeTasks = allTasks.value.filter(t => (t.assigneeId || t.assignee_id) === memberId && t.status !== 'DONE').length
  return Math.min(100, activeTasks * 25) 
}

function getLoadStatus(load: number) {
  if (load > 80) return 'exception'
  if (load > 50) return 'warning'
  return 'success'
}

function getAvatarColor(name: string) {
  const colors = ['#5B8DEF', '#32d583', '#f5a623', '#ec5f5f', '#8b5cf6', '#ec4899']
  let hash = 0
  for (let i = 0; i < (name?.length || 0); i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

async function fetchAll() {
  loading.value = true
  try {
    const [mRes, tRes] = await Promise.all([
      memberApi.list(),
      taskApi.listAll()
    ])
    members.value = mRes.data || []
    allTasks.value = tRes.data || []
  } catch {
    ElMessage.error('获取成员数据失败')
  } finally {
    loading.value = false
  }
}

function openAddMember() {
  isEdit.value = false
  Object.assign(form, { memberId: '', nickname: '', phone: '', password: '', role: 'user' })
  showDialog.value = true
}

function editMember(m: any) {
  isEdit.value = true
  Object.assign(form, {
    memberId: m.memberId,
    nickname: m.nickname,
    phone: m.phone,
    role: m.role || 'user',
    password: ''
  })
  showDialog.value = true
}

async function saveMember() {
  if (!form.nickname || !form.phone) {
    ElMessage.warning('请填写必填项')
    return
  }
  if (!isEdit.value && !form.password) {
    ElMessage.warning('请设置初始密码')
    return
  }
  
  saving.value = true
  try {
    if (isEdit.value) {
      await memberApi.update(form.memberId, {
        nickname: form.nickname,
        role: form.role
      })
    } else {
      await memberApi.create({
        nickname: form.nickname,
        phone: form.phone,
        password: form.password,
        role: form.role
      })
    }
    ElMessage.success('操作成功')
    showDialog.value = false
    fetchAll()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(m: any) {
  try {
    await ElMessageBox.confirm(`确定要移除成员「${m.nickname}」吗？该操作不可撤销。`, '永久移除成员', { 
      type: 'warning',
      confirmButtonText: '确定移除',
      confirmButtonClass: 'el-button--danger'
    })
    await memberApi.delete(m.memberId)
    ElMessage.success('成员已移除')
    fetchAll()
  } catch {}
}

onMounted(fetchAll)
</script>

<style scoped>
.member-manage { padding: 24px 32px; background: var(--bg); height: 100%; }
.view-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 32px; }
.view-title { font-size: 24px; font-weight: 800; color: var(--text); letter-spacing: -0.5px; margin-bottom: 4px; }
.view-subtitle { font-size: 14px; color: var(--text-faint); }
.card { background: var(--surface-1); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; }
.user-cell { display: flex; align-items: center; gap: 12px; }
.user-name { font-size: 14px; font-weight: 600; color: var(--text); }
.user-id { font-size: 11px; color: var(--text-faint); }
.role-badge { font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 6px; }
.role-admin { background: rgba(167,139,250,0.15); color: #a78bfa; }
.role-user { background: var(--surface-3); color: var(--text-secondary); }
.load-cell { width: 100px; }
:deep(.el-table) { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: var(--surface-2); }
:deep(.el-table__row:hover > td) { background-color: var(--surface-2) !important; }
</style>
