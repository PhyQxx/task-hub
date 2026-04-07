import axios, { AxiosInstance } from 'axios'
import type { ApiResponse } from '@/types'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const client: AxiosInstance = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器
client.interceptors.request.use(config => {
  // 可在此附加用户 token
  const userId = localStorage.getItem('userId') || 'system'
  config.headers.set('X-User-Id', userId)
  return config
})

// 响应拦截器
client.interceptors.response.use(
  res => res.data as ApiResponse<unknown>,
  err => {
    const msg = err.response?.data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default client
