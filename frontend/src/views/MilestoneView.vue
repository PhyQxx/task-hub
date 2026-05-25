<template>
  <div class="milestone-view">
    <div class="view-header">
      <div class="header-left">
        <h1 class="view-title">里程碑</h1>
        <p class="view-subtitle">锁定项目核心交付物与关键时间节点</p>
      </div>
      <div class="header-right">
        <el-button type="primary" size="small" @click="showCreate = true">+ 新建里程碑</el-button>
      </div>
    </div>

    <div v-loading="loading" class="milestone-grid">
      <div
        v-for="m in milestones"
        :key="m.milestoneId"
        class="milestone-card"
      >
        <div class="card-indicator" :style="{ background: m.color || 'var(--primary)' }"></div>
        <div class="card-body">
          <div class="card-header">
            <h3 class="milestone-name">{{ m.name }}</h3>
            <div class="milestone-actions">
              <button class="icon-btn" @click="openEdit(m)">✏️</button>
              <button class="icon-btn text-danger" @click="handleDelete(m)">🗑</button>
            </div>
          </div>
          
          <div class="milestone-meta">
            <span class="meta-item">
              <span class="meta-icon">📅</span>
              {{ m.targetDate || '未设置日期' }}
            </span>
            <span v-if="m.taskCount !== undefined" class="meta-item">
              <span class="meta-icon">📋</span>
              {{ m.taskCount || 0 }} 个关联任务
            </span>
          </div>

          <p v-if="m.description" class="milestone-desc">{{ m.description }}</p>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="!loading && milestones.length === 0" class="empty-state">
        <div class="empty-icon">🎯</div>
        <p class="empty-text">暂无里程碑数据</p>
        <el-button type="primary" size="small" @click="showCreate = true" style="margin-top:12px">点击创建</el-button>
      </div>
    </div>

    <!-- Modals -->
    <el-dialog v-model="showCreate" :title="editMilestone ? '编辑里程碑' : '新建里程碑'" width="480px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="例如：Beta 版本发布" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="目标日期" required>
            <el-date-picker v-model="form.targetDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="主题色">
            <div class="color-picker">
              <div
                v-for="c in colorOptions"
                :key="c"
                class="color-dot"
                :style="{ background: c }"
                :class="{ active: form.color === c }"
                @click="form.color = c"
              ></div>
            </div>
          </el-form-item>
        </div>
        <el-form-item label="里程碑说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="详细说明里程碑的具体目标..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存里程碑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { milestoneApi } from '@/api'
import { useProjectStore } from '@/stores'

const projectStore = useProjectStore()
const milestones = ref<any[]>([])
const loading = ref(false)
const showCreate = ref(false)
const editMilestone = ref<any>(null)
const saving = ref(false)

const form = ref({
  name: '',
  targetDate: '',
  color: '#5B8DEF',
  description: '',
})

const colorOptions = [
  '#5B8DEF', '#32d583', '#f5a623', '#ec5f5f',
  '#8b5cf6', '#ec4899', '#32323c', '#06b6d4'
]

async function loadMilestones() {
  const pid = projectStore.currentProjectId
  if (!pid) return
  loading.value = true
  try {
    const res = await milestoneApi.list(pid)
    milestones.value = res.data || []
  } catch (e: any) {
    console.error('Failed to load milestones', e)
  } finally {
    loading.value = false
  }
}

function openEdit(m: any) {
  editMilestone.value = m
  form.value = {
    name: m.name || '',
    targetDate: m.targetDate || '',
    color: m.color || '#5B8DEF',
    description: m.description || '',
  }
  showCreate.value = true
}

async function handleSave() {
  if (!form.value.name || !form.value.targetDate) {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    const pid = projectStore.currentProjectId
    if (!pid) { ElMessage.warning('请先选择项目'); return }

    if (editMilestone.value) {
      await milestoneApi.delete(editMilestone.value.milestoneId)
    }
    await milestoneApi.create({
      projectId: pid,
      name: form.value.name.trim(),
      targetDate: form.value.targetDate,
      color: form.value.color,
      description: form.value.description,
    })
    ElMessage.success('操作成功')
    showCreate.value = false
    editMilestone.value = null
    form.value = { name: '', targetDate: '', color: '#5B8DEF', description: '' }
    await loadMilestones()
  } catch (e: any) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(m: any) {
  if (!confirm(`确认删除里程碑「${m.name}」？`)) return
  try {
    await milestoneApi.delete(m.milestoneId)
    ElMessage.success('已删除')
    await loadMilestones()
  } catch (e: any) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadMilestones()
})
</script>

<style scoped>
.milestone-view {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  background: var(--bg);
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.view-title { font-size: 20px; font-weight: 700; color: var(--text); margin-bottom: 4px; }
.view-subtitle { font-size: 13px; color: var(--text-faint); }

.milestone-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.milestone-card {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  display: flex;
  transition: all 0.2s ease;
}

.milestone-card:hover { border-color: var(--text-faint); transform: translateY(-2px); box-shadow: var(--shadow-md); }

.card-indicator { width: 4px; flex-shrink: 0; }
.card-body { flex: 1; padding: 16px 20px; }

.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.milestone-name { font-size: 15px; font-weight: 600; color: var(--text); }
.milestone-actions { display: flex; gap: 8px; }

.icon-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.12s;
}
.icon-btn:hover { background: var(--surface-3); }

.milestone-meta { display: flex; gap: 16px; margin-bottom: 12px; }
.meta-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-secondary); font-weight: 500; }
.meta-icon { opacity: 0.8; }

.milestone-desc { font-size: 12px; color: var(--text-faint); line-height: 1.5; margin: 0; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.color-picker { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 4px; }
.color-dot {
  width: 20px; height: 20px; border-radius: 50%; cursor: pointer;
  border: 2px solid var(--surface-1);
  box-shadow: 0 0 0 1px var(--border);
  transition: all 0.15s;
}
.color-dot:hover { transform: scale(1.1); }
.color-dot.active { box-shadow: 0 0 0 2px var(--primary); }

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  color: var(--text-faint);
}
.empty-icon { font-size: 48px; margin-bottom: 12px; opacity: 0.5; }
.text-danger { color: var(--danger) !important; }
</style>

