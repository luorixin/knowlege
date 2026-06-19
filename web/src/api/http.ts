import axios, { AxiosError, type AxiosResponse } from 'axios'

import type { ApiEnvelope } from './types'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = window.localStorage.getItem('knowledge-token')
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  // Fallback for X-User-Id / X-Tenant-Id for backward compatibility
  const raw = window.localStorage.getItem('knowledge-user')
  if (raw) {
    try {
      const user = JSON.parse(raw) as { userId?: string; tenantId?: string }
      if (user.userId) config.headers.set('X-User-Id', String(user.userId))
      if (user.tenantId) config.headers.set('X-Tenant-Id', String(user.tenantId))
    } catch {
      window.localStorage.removeItem('knowledge-user')
    }
  }

  return config
})
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.localStorage.removeItem('knowledge-token')
      window.localStorage.removeItem('knowledge-user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)
export function unwrapResponse<T>(response: AxiosResponse<ApiEnvelope<T>>): T {
  const envelope = response.data
  if (envelope.code && envelope.code !== 'OK') {
    throw new Error(envelope.message || '请求失败')
  }
  return envelope.data
}

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiEnvelope<unknown>>
    return axiosError.response?.data?.message || axiosError.message || '网络请求失败'
  }
  if (error instanceof Error) {
    return error.message
  }
  return '未知错误'
}
