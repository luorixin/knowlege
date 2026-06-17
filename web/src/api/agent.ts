import { http, unwrapResponse } from './http'
import type { AgentChatPayload, AgentChatResult } from './types'

export async function chatWithAgent(payload: AgentChatPayload): Promise<AgentChatResult> {
  return unwrapResponse(await http.post('/api/agent/chat', payload))
}
