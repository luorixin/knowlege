import { http, unwrapResponse } from './http'
import type { AgentChatPayload, AgentChatResult, EntityId, AgentSessionDto, AgentMessageDto } from './types'

export async function chatWithAgent(payload: AgentChatPayload): Promise<AgentChatResult> {
  return unwrapResponse(await http.post('/api/agent/chat', payload))
}

export async function chatWithAgentStream(
  payload: AgentChatPayload,
  onMessage: (chunk: string) => void,
  onDone: (result: AgentChatResult) => void,
  onError: (error: Error) => void
) {
  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    }
    
    const rawUser = window.localStorage.getItem('knowledge-user')
    if (rawUser) {
      try {
        const user = JSON.parse(rawUser) as { userId?: string; tenantId?: string }
        if (user.userId) headers['X-User-Id'] = String(user.userId)
        if (user.tenantId) headers['X-Tenant-Id'] = String(user.tenantId)
      } catch (e) {
        // ignore JSON parse error
      }
    }

    const response = await fetch('/api/agent/chat/stream', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload)
    })
    
    if (!response.ok) {
      throw new Error(`请求失败: ${response.status}`)
    }
    
    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应流')
    
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let currentEvent = 'message'
    
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      buffer += decoder.decode(value, { stream: true })
      
      let eolIndex
      while ((eolIndex = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, eolIndex).trim()
        buffer = buffer.slice(eolIndex + 1)
        
        if (line.startsWith('event:')) {
          currentEvent = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
          const dataStr = line.substring(5)
          if (currentEvent === 'message') {
            onMessage(dataStr)
          } else if (currentEvent === 'done') {
            onDone(JSON.parse(dataStr))
          }
        }
      }
    }
  } catch (error) {
    onError(error instanceof Error ? error : new Error(String(error)))
  }
}

export async function listSessions(spaceId: EntityId): Promise<AgentSessionDto[]> {
  const response = await http.get(`/api/agent/kb-spaces/${spaceId}/sessions`)
  return unwrapResponse<AgentSessionDto[]>(response)
}

export async function getSessionMessages(spaceId: EntityId, sessionId: EntityId): Promise<AgentMessageDto[]> {
  const response = await http.get(`/api/agent/kb-spaces/${spaceId}/sessions/${sessionId}/messages`)
  return unwrapResponse<AgentMessageDto[]>(response)
}
