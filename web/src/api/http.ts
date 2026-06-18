import axios, { AxiosError, type AxiosResponse } from 'axios'

import type { ApiEnvelope } from './types'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const raw = window.localStorage.getItem('knowledge-user')
  if (!raw) {
    return config
  }

  try {
    const user = JSON.parse(raw) as { userId?: string; tenantId?: string }
    if (user.userId) {
      config.headers.set('X-User-Id', String(user.userId))
    }
    if (user.tenantId) {
      config.headers.set('X-Tenant-Id', String(user.tenantId))
    }
  } catch {
    window.localStorage.removeItem('knowledge-user')
  }

  return config
})

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
