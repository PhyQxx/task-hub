import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'

const baseURL = (import.meta as any).env?.VITE_API_BASE_URL || '/api'

const rawClient: AxiosInstance = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器：附加 JWT token 和用户ID
rawClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  const memberId = localStorage.getItem('memberId')
  if (memberId) {
    config.headers.set('X-User-Id', memberId)
  }
  return config
})

// 响应拦截器：解包 ApiResponse
rawClient.interceptors.response.use(
  (res: AxiosResponse) => res.data as any,
  (err: any) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      window.location.href = '/login'
    }
    const msg = err.response?.data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  },
)

// 包装后的客户端，返回类型为 ApiResponse<T>（拦截器已解包）
const client = {
  get: <T>(url: string, config?: AxiosRequestConfig) => rawClient.get(url, config).then((res) => res as unknown as T),
  post: <T>(url: string, data?: any, config?: AxiosRequestConfig) =>
    rawClient.post(url, data, config).then((res) => res as unknown as T),
  put: <T>(url: string, data?: any, config?: AxiosRequestConfig) =>
    rawClient.put(url, data, config).then((res) => res as unknown as T),
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    rawClient.delete(url, config).then((res) => res as unknown as T),
}

export default client
