import { http, unwrapResponse } from './http'
import type { PageResult } from './types'

export interface TokenUsageItem {
  id: number
  session_id?: number
  usage_type: string
  model_provider: string
  model_name: string
  prompt_tokens: number
  completion_tokens: number
  total_tokens: number
  latency_ms: number
  created_at: string
}

export interface TokenUsageFilters {
  model_provider?: string
  model_name?: string
  created_from?: string
  created_to?: string
}

export async function listTokenUsage(page: number, size: number, filters: TokenUsageFilters = {}): Promise<PageResult<TokenUsageItem>> {
  const params: Record<string, any> = { page, size, ...filters }
  // remove empty strings
  Object.keys(params).forEach(key => {
    if (params[key] === '') {
      delete params[key]
    }
  })
  if (params.created_from && !params.created_from.includes('T')) {
    params.created_from += 'T00:00:00'
  }
  if (params.created_to && !params.created_to.includes('T')) {
    params.created_to += 'T23:59:59'
  }
  
  const response = await http.get('/api/v1/admin/token-usage', { params })
  return unwrapResponse(response)
}
