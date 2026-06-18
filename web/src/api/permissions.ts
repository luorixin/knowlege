import { http, unwrapResponse } from './http'
import type { ApiEnvelope } from './types'

export interface Role {
  id: string
  name: string
  description: string
  memberCount: number
  policyCount: number
  isSystem: boolean
}

export interface Member {
  id: string
  username: string
  displayName: string
  role: string
  status: string
}

export interface Policy {
  id: string
  name: string
  resource: string
  actions: string[]
  isSystem: boolean
}

export const getRoles = () => {
  return http.get<ApiEnvelope<Role[]>>('/api/v1/permissions/roles').then(unwrapResponse)
}

export const createRole = (data: { name: string; description: string }) => {
  return http.post<ApiEnvelope<Role>>('/api/v1/permissions/roles', data).then(unwrapResponse)
}

export const deleteRole = (roleId: string) => {
  return http.delete<ApiEnvelope<void>>(`/api/v1/permissions/roles/${roleId}`).then(unwrapResponse)
}

export const getMembers = (spaceId?: string) => {
  return http.get<ApiEnvelope<Member[]>>('/api/v1/permissions/members', { params: { spaceId } }).then(unwrapResponse)
}

export const addMember = (data: { username: string; roleId: string }) => {
  return http.post<ApiEnvelope<void>>('/api/v1/permissions/members', data).then(unwrapResponse)
}

export const getPolicies = (spaceId?: string) => {
  return http.get<ApiEnvelope<Policy[]>>('/api/v1/permissions/policies', { params: { spaceId } }).then(unwrapResponse)
}
