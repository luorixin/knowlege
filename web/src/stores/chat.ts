import { defineStore } from 'pinia'

import { chatWithAgent } from '@/api/agent'
import type { AgentCitation, EntityId, SearchFilters } from '@/api/types'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations: AgentCitation[]
  createdAt: string
  error?: boolean
  debugInfo?: any
}

interface ChatState {
  sessionId: EntityId | null
  messages: ChatMessage[]
  activeCitations: AgentCitation[]
  sending: boolean
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    sessionId: null,
    messages: [],
    activeCitations: [],
    sending: false,
  }),
  actions: {
    async send(spaceId: EntityId, query: string, filters: SearchFilters) {
      const question = query.trim()
      if (!question) return

      this.messages.push({
        id: crypto.randomUUID(),
        role: 'user',
        content: question,
        citations: [],
        createdAt: new Date().toISOString(),
      })
      this.sending = true

      try {
        const result = await chatWithAgent({
          space_id: spaceId,
          session_id: this.sessionId,
          query: question,
          filters,
        })
        this.sessionId = result.session_id
        this.activeCitations = result.citations || []
        this.messages.push({
          id: crypto.randomUUID(),
          role: 'assistant',
          content: result.answer,
          citations: result.citations || [],
          createdAt: new Date().toISOString(),
          debugInfo: result.debug_info,
        })
      } catch (error) {
        const message = error instanceof Error ? error.message : '问答请求失败'
        this.messages.push({
          id: crypto.randomUUID(),
          role: 'assistant',
          content: message,
          citations: [],
          createdAt: new Date().toISOString(),
          error: true,
        })
        this.activeCitations = []
      } finally {
        this.sending = false
      }
    },
    focusCitations(citations: AgentCitation[]) {
      this.activeCitations = citations
    },
    clear() {
      this.sessionId = null
      this.messages = []
      this.activeCitations = []
    },
  },
})
