import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import client from '@/api/client'
import type { ApiResponse } from '@/types'

interface LoginData {
  token: string
  memberId: string
  nickname: string
  role: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const memberId = ref<string>(localStorage.getItem('memberId') || '')
  const nickname = ref<string>(localStorage.getItem('nickname') || '')
  const role = ref<string>(localStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setUser(data: LoginData) {
    token.value = data.token
    memberId.value = data.memberId
    nickname.value = data.nickname
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('memberId', data.memberId)
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('role', data.role)
  }

  function logout() {
    token.value = ''
    memberId.value = ''
    nickname.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('memberId')
    localStorage.removeItem('nickname')
    localStorage.removeItem('role')
    client.post('/auth/logout').catch(() => {})
    window.location.href = '/login'
  }

  async function fetchMe() {
    if (!token.value) return
    try {
      const res = await client.get<ApiResponse<any>>('/auth/me')
      if (res.code === 0 && res.data) {
        nickname.value = res.data.nickname || nickname.value
        role.value = res.data.role || role.value
        localStorage.setItem('nickname', nickname.value)
        localStorage.setItem('role', role.value)
      }
    } catch {
      // token invalid
    }
  }

  return { token, memberId, nickname, role, isLoggedIn, setUser, logout, fetchMe }
})
