import { http, unwrapResponse } from './http'
import type { EntityId, CreateKnowledgeSpacePayload, KnowledgeSpace } from './types'

export async function listKnowledgeSpaces(tenantId: EntityId): Promise<KnowledgeSpace[]> {
  const data = unwrapResponse(await http.get('/api/v1/kb-spaces', { params: { tenantId } })) as any
  return data.content || []
}

export async function createKnowledgeSpace(payload: CreateKnowledgeSpacePayload): Promise<KnowledgeSpace> {
  return unwrapResponse(await http.post('/api/v1/kb-spaces', payload))
}
