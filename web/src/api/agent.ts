import { fetchEventSource } from '@microsoft/fetch-event-source'
import { http, unwrapResponse } from './http'
import type { AgentChatPayload, AgentChatResult, EntityId, AgentSessionDto, AgentMessageDto } from './types'

export async function chatWithAgent(payload: AgentChatPayload): Promise<AgentChatResult> {
  return unwrapResponse(await http.post('/api/v1/agent/chat', payload))
}

export async function chatWithAgentStream(
  payload: AgentChatPayload,
  controller: AbortController,
  onMessage: (chunk: string) => void,
  onDone: (result: AgentChatResult) => void,
  onError: (error: Error) => void
) {
  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
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

    await fetchEventSource('/api/v1/agent/chat/stream', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
      signal: controller.signal,
      async onopen(response) {
        if (!response.ok) {
          throw new Error(`请求失败: ${response.status} ${response.statusText}`)
        }
      },
      onmessage(msg) {
        if (msg.event === 'message') {
          onMessage(msg.data)
        } else if (msg.event === 'done') {
          onDone(JSON.parse(msg.data))
        }
      },
      onerror(err) {
        onError(err instanceof Error ? err : new Error(String(err)))
        throw err // Throw to stop reconnect loop
      }
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      console.log('Stream aborted by user')
    } else {
      onError(error instanceof Error ? error : new Error(String(error)))
    }
  }
}

export async function listSessions(spaceId: EntityId, page = 0, size = 20): Promise<AgentSessionDto[]> {
  const response = await http.get(`/api/v1/agent/kb-spaces/${spaceId}/sessions`, {
    params: { page, size },
  })
  const data = unwrapResponse<any>(response)
  return data.content || []
}

export async function getSessionMessages(spaceId: EntityId, sessionId: EntityId, page = 0, size = 50): Promise<AgentMessageDto[]> {
  const response = await http.get(`/api/v1/agent/kb-spaces/${spaceId}/sessions/${sessionId}/messages`, {
    params: { page, size },
  })
  const data = unwrapResponse<any>(response)
  return data.content || []
}
