import { http, unwrapResponse } from './http'
import type { AuditLogItem, PageResult } from './types'

export interface AuditLogQueryParams {
  actor_user_id?: string
  action?: string
  resource_type?: string
  resource_id?: string
  result_status?: string
  trace_id?: string
  created_from?: string
  created_to?: string
  page?: number
  size?: number
}

export async function listAuditLogs(params: AuditLogQueryParams): Promise<PageResult<AuditLogItem>> {
  return unwrapResponse(await http.get('/api/v1/admin/audit-logs', { params }))
}
