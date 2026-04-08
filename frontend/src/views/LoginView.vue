<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">📋 任务舱</div>
        <div class="brand-slogan">让团队协作更高效</div>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">📊</span>
            <span>甘特图 · 看板 · 泳道</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">👥</span>
            <span>成员管理 · 角色权限</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">⚡</span>
            <span>智能排程 · 负载均衡</span>
          </div>
        </div>
      </div>
      <div class="brand-version">v2.0 · 飞书风格</div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-main">
      <div class="login-card">
        <div class="login-header">
          <h1 class="login-title">登录任务舱</h1>
          <p class="login-subtitle">使用你的账号登录</p>
        </div>

        <el-form class="login-form" @submit.prevent="handleLogin" :model="form" :rules="rules" ref="formRef">
          <el-form-item prop="phone">
            <label class="field-label">手机号</label>
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <label class="field-label">密码</label>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <span>还没有账号？</span>
          <a href="#" @click.prevent="showRegister = true">立即注册 →</a>
        </div>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog v-model="showRegister" title="注册账号" width="420px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="70px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="手机号" clearable />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="你的昵称" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="handleRegister">注册</el-button>
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
      ElMessage.success('登录成功')
      window.location.href = '/'
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (err: any) {
    ElMessage.error(err.message || '登录失败')
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
    ElMessage.error(err.message || '注册失败')
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
  overflow: hidden;
}

/* 左侧品牌区 */
.login-brand {
  width: 420px;
  flex-shrink: 0;
  background: var(--primary);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px;
  color: #fff;
}

.brand-content { margin-top: 40px; }

.brand-logo {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 8px;
}

.brand-slogan {
  font-size: 16px;
  color: rgba(255,255,255,0.75);
  margin-bottom: 48px;
}

.brand-features { display: flex; flex-direction: column; gap: 16px; }

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: rgba(255,255,255,0.9);
}

.feature-icon { font-size: 18px; }

.brand-version {
  font-size: 12px;
  color: rgba(255,255,255,0.5);
}

.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 40px;
}

.login-card {
  width: 400px;
  max-width: 100%;
}

.login-header { margin-bottom: 32px; }

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
  letter-spacing: -0.3px;
}

.login-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

.login-form { margin-top: 8px; }

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.login-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 500;
  background: var(--primary) !important;
  border-color: var(--primary) !important;
  letter-spacing: 0.3px;
}

.login-btn:hover {
  background: var(--primary-hover) !important;
  border-color: var(--primary-hover) !important;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--text-secondary);
}

.login-footer a {
  color: var(--primary);
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.login-footer a:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 768px) {
  .login-brand { display: none; }
  .login-main { padding: 20px; }
}
</style>
