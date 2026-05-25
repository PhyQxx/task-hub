<template>
  <div class="project-manage">
    <div class="view-header">
      <div class="header-left">
        <h1 class="view-title">项目资源管理</h1>
        <p class="view-subtitle">全站项目生命周期监控与核心指标分析</p>
      </div>
      <div class="header-right">
        <el-button type="primary" size="small" @click="openCreate">+ 新建全站项目</el-button>
      </div>
    </div>

    <div class="card table-card">
      <el-table :data="projects" style="width: 100%" v-loading="loading">
        <el-table-column label="项目名称" min-width="200">
          <template #default="{ row }">
            <div class="project-cell">
              <span class="p-icon">📁</span>
              <div class="p-info">
                <div class="p-name">{{ row.name }}</div>
                <div class="p-desc">{{ row.description || '暂无描述' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="180">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress :percentage="row.stats?.completion || 0" :stroke-width="6" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="任务分布" width="220">
          <template #default="{ row }">
            <div class="task-stats">
              <span class="stat-tag todo" title="待处理">{{ row.stats?.todo || 0 }}</span>
              <span class="stat-tag doing" title="进行中">{{ row.stats?.doing || 0 }}</span>
              <span class="stat-tag done" title="已完成">{{ row.stats?.done || 0 }}</span>
              <span class="stat-tag blocked" v-if="row.stats?.blocked" title="已阻塞">{{ row.stats?.blocked }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="120">
          <template #default="{ row }">
            <span class="owner-name">{{ row.ownerNickname || row.ownerId || '系统' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editProject(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Project Dialog (Create/Edit) -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑项目' : '新建项目'" width="480px">
      <el-form :model="form" label-position="top">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="简述项目目标与背景..." />
        </el-form-item>
        <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 16px">
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <option value="planning">筹备中</option>
              <option value="active">进行中</option>
              <option value="completed">已结项</option>
              <option value="archived">已归档</option>
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProject" :loading="saving">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { projectApi, taskApi, memberApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const projects = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: '',
  name: '',
  description: '',
  status: 'active'
})

async function fetchProjects() {
  loading.value = true
  try {
    const [pRes, tRes, mRes] = await Promise.all([
      projectApi.list(),
      taskApi.listAll(),
      memberApi.list()
    ])
    
    const allTasks = tRes.data || []
    const allMembers = mRes.data || []

    projects.value = (pRes.data || []).map((p: any) => {
      const pid = p.projectId || p.id
      const pTasks = allTasks.filter((t: any) => (t.projectId || t.project_id) === pid)
      const total = pTasks.length
      const done = pTasks.filter((t: any) => t.status === 'DONE').length
      const owner = allMembers.find(m => m.memberId === p.ownerId)
      
      return {
        ...p,
        projectId: pid,
        ownerNickname: owner ? owner.nickname : p.ownerId,
        stats: {
          total,
          done,
          todo: pTasks.filter((t: any) => t.status === 'TODO').length,
          doing: pTasks.filter((t: any) => t.status === 'IN_PROGRESS').length,
          blocked: pTasks.filter((t: any) => t.status === 'BLOCKED').length,
          completion: total ? Math.round((done / total) * 100) : 0
        }
      }
    })
  } catch {
    ElMessage.error('获取项目数据失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { id: '', name: '', description: '', status: 'active' })
  showDialog.value = true
}

function editProject(row: any) {
  isEdit.value = true
  Object.assign(form, {
    id: row.projectId,
    name: row.name,
    description: row.description || '',
    status: row.status || 'active'
  })
  showDialog.value = true
}

async function saveProject() {
  if (!form.name.trim()) {
    ElMessage.warning('项目名称不能为空')
    return
  }
  
  saving.value = true
  try {
    if (isEdit.value) {
      await projectApi.update(form.id, {
        name: form.name,
        description: form.description,
        status: form.status as any
      })
    } else {
      await projectApi.create({
        name: form.name,
        description: form.description
      })
    }
    ElMessage.success('保存成功')
    showDialog.value = false
    fetchProjects()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(p: any) {
  try {
    await ElMessageBox.confirm(`确定要删除项目「${p.name}」及其所有关联任务吗？此操作不可逆。`, '风险警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      confirmButtonClass: 'el-button--danger'
    })
    await projectApi.delete(p.projectId)
    ElMessage.success('项目已删除')
    fetchProjects()
  } catch {}
}

onMounted(fetchProjects)
</script>

<style scoped>
.project-manage { padding: 24px 32px; background: var(--bg); height: 100%; }
.view-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 32px; }
.view-title { font-size: 24px; font-weight: 800; color: var(--text); letter-spacing: -0.5px; margin-bottom: 4px; }
.view-subtitle { font-size: 14px; color: var(--text-faint); }
.card { background: var(--surface-1); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; }
.project-cell { display: flex; align-items: flex-start; gap: 12px; }
.p-icon { font-size: 18px; margin-top: 2px; }
.p-name { font-size: 14px; font-weight: 700; color: var(--text); }
.p-desc { font-size: 11px; color: var(--text-faint); margin-top: 2px; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.task-stats { display: flex; gap: 4px; }
.stat-tag { font-size: 10px; font-weight: 800; padding: 1px 6px; border-radius: 4px; color: #fff; min-width: 24px; text-align: center; cursor: help; }
.stat-tag.todo { background: #9ca3af; }
.stat-tag.doing { background: var(--primary); }
.stat-tag.done { background: var(--success); }
.stat-tag.blocked { background: var(--danger); }
.owner-name { font-size: 12px; color: var(--text-secondary); font-weight: 600; }
:deep(.el-table) { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: var(--surface-2); }
:deep(.el-table__row:hover > td) { background-color: var(--surface-2) !important; }
</style>
