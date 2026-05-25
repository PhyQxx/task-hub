<template>
  <div class="login-page">
    <!-- Left: Brand -->
    <div class="login-brand">
      <div class="brand-top">
        <div class="brand-logo">
          <span class="logo-icon">📋</span>
          <span class="logo-text">TaskHub</span>
        </div>
        <p class="brand-slogan">为极客打造的下一代任务协作平台</p>
      </div>

      <div class="brand-features">
        <div class="feature-item">
          <span class="feature-icon">📊</span>
          <div class="feature-text">
            <h3>多维视图</h3>
            <p>甘特图、看板、泳道图，全方位掌控进度</p>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-icon">🧠</span>
          <div class="feature-text">
            <h3>智能排程</h3>
            <p>内置智能算法，自动优化成员任务分配</p>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-icon">⚡</span>
          <div class="feature-text">
            <h3>实时同步</h3>
            <p>毫秒级状态同步，确保团队信息高度一致</p>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        <span class="version-tag">PRO v2.0</span>
        <span class="copyright">© 2024 NewCity Tech.</span>
      </div>
    </div>

    <!-- Right: Form -->
    <div class="login-main">
      <div class="login-container">
        <div class="login-header">
          <h1 class="login-title">欢迎回来</h1>
          <p class="login-subtitle">请登录以管理您的项目与任务</p>
        </div>

        <el-form class="login-form" @submit.prevent="handleLogin" :model="form" :rules="rules" ref="formRef">
          <el-form-item prop="phone">
            <label class="form-label">手机号</label>
            <input
              v-model="form.phone"
              class="form-input"
              placeholder="请输入手机号"
            />
          </el-form-item>

          <el-form-item prop="password">
            <label class="form-label">登录密码</label>
            <input
              v-model="form.password"
              type="password"
              class="form-input"
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <button
            type="submit"
            class="btn btn-primary login-btn"
            :disabled="loading"
            @click.prevent="handleLogin"
          >
            {{ loading ? '正在验证身份...' : '立即登录' }}
          </button>
        </el-form>

        <div class="login-actions">
          <span>还没有账号？</span>
          <button class="btn btn-ghost register-link" @click="showRegister = true">立即注册</button>
        </div>
      </div>
    </div>

    <!-- Register Modal -->
    <el-dialog v-model="showRegister" title="创建新账号" width="400px" destroy-on-close>
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-position="top">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="如何称呼您？" />
        </el-form-item>
        <el-form-item label="设置密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="至少 6 位字符" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="btn btn-ghost" @click="showRegister = false">取消</button>
        <button class="btn btn-primary" :loading="registering" @click="handleRegister">确认注册</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'

const authStore = useAuthStore()
const formRef = ref()
const registerFormRef = ref()
const loading = ref(false)
const registering = ref(false)
const showRegister = ref(false)

const form = reactive({ phone: '', password: '' })
const registerForm = reactive({ phone: '', nickname: '', password: '' })

const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res: any = await client.post('/auth/login', form)
    if (res.code === 0) {
      authStore.setUser(res.data)
      ElMessage.success('欢迎回来')
      window.location.href = '/'
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (err: any) {
    ElMessage.error('服务连接异常')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  registering.value = true
  try {
    const res: any = await client.post('/auth/register', registerForm)
    if (res.code === 0) {
      authStore.setUser(res.data)
      ElMessage.success('注册成功')
      showRegister.value = false
      window.location.href = '/'
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch (err: any) {
    ElMessage.error('注册异常')
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: var(--bg);
  overflow: hidden;
}

/* Brand Section */
.login-brand {
  width: 440px;
  background: var(--surface-1);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 60px 48px;
  position: relative;
}

.brand-logo { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.logo-icon { font-size: 28px; }
.logo-text { font-size: 24px; font-weight: 800; letter-spacing: -1px; color: var(--text); }
.brand-slogan { font-size: 15px; color: var(--text-faint); font-weight: 500; }

.brand-features { display: flex; flex-direction: column; gap: 32px; }
.feature-item { display: flex; gap: 16px; align-items: flex-start; }
.feature-icon { font-size: 20px; padding-top: 2px; }
.feature-text h3 { font-size: 14px; font-weight: 700; color: var(--text); margin-bottom: 4px; }
.feature-text p { font-size: 13px; color: var(--text-faint); line-height: 1.5; margin: 0; }

.brand-footer { display: flex; justify-content: space-between; align-items: center; }
.version-tag { font-size: 10px; font-weight: 800; background: var(--surface-3); padding: 2px 8px; border-radius: 4px; color: var(--text-faint); }
.copyright { font-size: 11px; color: var(--text-muted); }

/* Main Section */
.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-container { width: 360px; }

.login-header { margin-bottom: 40px; }
.login-title { font-size: 28px; font-weight: 700; color: var(--text); margin-bottom: 8px; letter-spacing: -0.5px; }
.login-subtitle { font-size: 14px; color: var(--text-faint); }

.login-form { display: flex; flex-direction: column; gap: 20px; }
.login-btn { width: 100%; height: 42px; font-size: 15px; margin-top: 10px; }

.login-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 32px;
  font-size: 13px;
  color: var(--text-faint);
}
.register-link { color: var(--primary); font-weight: 700; padding: 4px 8px; }

/* Responsive */
@media (max-width: 900px) {
  .login-brand { display: none; }
}
</style>

