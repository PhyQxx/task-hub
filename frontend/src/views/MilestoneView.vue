<template>
  <div class="milestone-view">
    <div class="view-header">
      <span class="view-title">🎯 里程碑</span>
      <span class="view-subtitle">项目关键节点管理</span>
    </div>

    <div class="toolbar">
      <el-button type="primary" size="small" @click="showCreate = true">+ 新建里程碑</el-button>
    </div>

    <div v-loading="loading" class="milestone-list">
      <div
        v-for="m in milestones"
        :key="m.milestoneId"
        class="milestone-card"
        :style="{ borderLeftColor: m.color || '#3370ff' }"
      >
        <div class="milestone-header">
          <div class="milestone-name">{{ m.name }}</div>
          <div class="milestone-actions">
            <el-button size="small" link @click="openEdit(m)">编辑</el-button>
            <el-button size="small" link type="danger" @click="handleDelete(m)">删除</el-button>
          </div>
        </div>
        <div class="milestone-meta">
          <span class="milestone-date">📅 {{ m.targetDate || '未设置' }}</span>
          <span v-if="m.description" class="milestone-desc">{{ m.description }}</span>
        </div>
        <div v-if="m.taskCount !== undefined" class="milestone-tasks">
          {{ m.taskCount || 0 }} 个任务
        </div>
      </div>

      <div v-if="!loading && milestones.length === 0" class="empty-state">
        <div class="empty-icon">🎯</div>
        <div class="empty-text">暂无里程碑</div>
        <div class="empty-sub">点击上方按钮创建第一个里程碑</div>
      </div>
    </div>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="showCreate" :title="editMilestone ? '编辑里程碑' : '新建里程碑'" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：Beta版本发布" clearable />
        </el-form-item>
        <el-form-item label="目标日期" required>
          <el-date-picker v-model="form.targetDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="颜色">
          <div class="color-picker">
            <div
              v-for="c in colorOptions"
              :key="c"
              class="color-dot"
              :style="{ background: c }"
              :class="{ selected: form.color === c }"
              @click="form.color = c"
            ></div>
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="里程碑说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false; editMilestone = null">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
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
  color: '#3370ff',
  description: '',
})

const colorOptions = [
  '#3370ff', '#00c935', '#ff9914', '#de352b',
  '#8b5cf6', '#ec4899', '#f59e0b', '#06b6d4'
]

async function loadMilestones() {
  const pid = projectStore.currentProjectId
  if (!pid) return
  loading.value = true
  try {
    const res = await milestoneApi.list(pid)
    milestones.value = res.data || []
  } catch (e: any) {
    console.error('加载里程碑失败', e)
  } finally {
    loading.value = false
  }
}

function openEdit(m: any) {
  editMilestone.value = m
  form.value = {
    name: m.name || '',
    targetDate: m.targetDate || '',
    color: m.color || '#3370ff',
    description: m.description || '',
  }
  showCreate.value = true
}

async function handleSave() {
  if (!form.value.name || !form.value.targetDate) {
    ElMessage.warning('名称和目标日期不能为空')
    return
  }
  saving.value = true
  try {
    const pid = projectStore.currentProjectId
    if (!pid) { ElMessage.warning('请先选择项目'); return }

    if (editMilestone.value) {
      // milestone doesn't have update, delete + create
      await milestoneApi.delete(editMilestone.value.milestoneId)
    }
    await milestoneApi.create({
      projectId: pid,
      name: form.value.name.trim(),
      targetDate: form.value.targetDate,
      color: form.value.color,
      description: form.value.description,
    })
    ElMessage.success(editMilestone.value ? '更新成功' : '创建成功')
    showCreate.value = false
    editMilestone.value = null
    form.value = { name: '', targetDate: '', color: '#3370ff', description: '' }
    await loadMilestones()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
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
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadMilestones()
})
</script>

<style scoped>
.milestone-view {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}
.view-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
}
.view-title { font-size: 18px; font-weight: 600; color: var(--text); }
.view-subtitle { font-size: 13px; color: var(--text-secondary); }
.toolbar { margin-bottom: 16px; }
.milestone-list { display: flex; flex-direction: column; gap: 10px; }
.milestone-card {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-left: 4px solid #3370ff;
  border-radius: var(--radius-lg);
  padding: 14px 16px;
}
.milestone-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.milestone-name { font-size: 14px; font-weight: 600; color: var(--text); }
.milestone-actions { display: flex; gap: 4px; }
.milestone-meta { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.milestone-date { font-size: 12px; color: var(--text-secondary); }
.milestone-desc { font-size: 12px; color: var(--text-faint); }
.milestone-tasks { font-size: 11px; color: var(--text-faint); margin-top: 4px; }
.color-picker { display: flex; gap: 8px; flex-wrap: wrap; }
.color-dot {
  width: 22px; height: 22px; border-radius: 50%; cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.15s;
}
.color-dot.selected { border-color: var(--text); }
.empty-state { display: flex; flex-direction: column; align-items: center; padding: 60px; gap: 8px; }
.empty-icon { font-size: 48px; }
.empty-text { font-size: 14px; color: var(--text-secondary); }
.empty-sub { font-size: 12px; color: var(--text-faint); }
</style>
