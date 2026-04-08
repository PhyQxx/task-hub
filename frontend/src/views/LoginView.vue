<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">📋 任务舱</div>
      <div class="login-subtitle">Team Task Tracker</div>

      <el-form class="login-form" @submit.prevent="handleLogin" :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="手机号"
            size="large"
            prefix-icon="📱"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="🔒"
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
        <a href="#" @click.prevent="showRegister = true">立即注册</a>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog v-model="showRegister" title="注册账号" width="420px" :close-on-click-modal="false">
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="80px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="手机号" clearable />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="你的昵称" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码" show-password />
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
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
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
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0d0d10;
}

.login-card {
  background: var(--surface-1, #16161a);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 16px;
  padding: 48px 40px;
  width: 400px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.5);
}

.login-logo {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  color: #fff;
  margin-bottom: 4px;
}

.login-subtitle {
  text-align: center;
  color: var(--text-secondary, #62666d);
  font-size: 13px;
  margin-bottom: 36px;
}

.login-form {
  margin-top: 8px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  background: var(--primary, #7170ff) !important;
  border-color: var(--primary, #7170ff) !important;
}

.login-btn:hover {
  opacity: 0.9;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--text-secondary, #62666d);
}

.login-footer a {
  color: var(--primary, #7170ff);
  text-decoration: none;
  margin-left: 4px;
}

.login-footer a:hover {
  text-decoration: underline;
}
</style>
