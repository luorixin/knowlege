import { http, unwrapResponse } from './http'
import type { CreateKnowledgeSpacePayload, KnowledgeSpace } from './types'

export async function listKnowledgeSpaces(tenantId: number): Promise<KnowledgeSpace[]> {
  return unwrapResponse(await http.get('/api/v1/kb-spaces', { params: { tenantId } }))
}

export async function createKnowledgeSpace(payload: CreateKnowledgeSpacePayload): Promise<KnowledgeSpace> {
  return unwrapResponse(await http.post('/api/v1/kb-spaces', payload))
}
