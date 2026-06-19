import { defineStore } from 'pinia'

import { chatWithAgent, listSessions, getSessionMessages } from '@/api/agent'
import type { AgentCitation, EntityId, SearchFilters, AgentSessionDto } from '@/api/types'

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
  sessions: AgentSessionDto[]
  messages: ChatMessage[]
  activeCitations: AgentCitation[]
  sending: boolean
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    sessionId: null,
    sessions: [],
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

      // Create a placeholder assistant message
      const assistantMessageId = crypto.randomUUID()
      this.messages.push({
        id: assistantMessageId,
        role: 'assistant',
        content: '', // Will be streamed in
        citations: [],
        createdAt: new Date().toISOString(),
      })

      const payload = {
        space_id: spaceId,
        session_id: this.sessionId,
        query: question,
        filters,
      }

      const { chatWithAgentStream } = await import('@/api/agent')
      
      return new Promise<void>((resolve) => {
        chatWithAgentStream(
          payload,
          (chunk) => {
            const msg = this.messages.find(m => m.id === assistantMessageId)
            if (msg) msg.content += chunk
          },
          (result) => {
            this.sessionId = result.session_id
            this.activeCitations = result.citations || []
            const msg = this.messages.find(m => m.id === assistantMessageId)
            if (msg) {
              msg.citations = result.citations || []
              msg.debugInfo = result.debug_info
            }
            this.sending = false
            // Refresh sessions list to show new chat
            this.loadSessions(spaceId)
            resolve()
          },
          (error) => {
            const msg = this.messages.find(m => m.id === assistantMessageId)
            if (msg) {
              msg.error = true
              msg.content += `\n\n**请求失败**: ${error.message}`
            }
            this.activeCitations = []
            this.sending = false
            resolve()
          }
        )
      })
    },
    focusCitations(citations: AgentCitation[]) {
      this.activeCitations = citations
    },
    clear() {
      this.sessionId = null
      this.messages = []
      this.activeCitations = []
    },
    async loadSessions(spaceId: EntityId) {
      try {
        const sessions = await listSessions(spaceId)
        this.sessions = sessions
      } catch (err) {
        console.error('Failed to load sessions', err)
      }
    },
    async loadHistory(spaceId: EntityId, sessionId: EntityId) {
      this.sessionId = sessionId
      this.messages = []
      this.activeCitations = []
      try {
        const history = await getSessionMessages(spaceId, sessionId)
        this.messages = history.map(m => ({
          id: m.id,
          role: m.role.toLowerCase() as 'user' | 'assistant',
          content: m.content,
          createdAt: m.createdAt,
          citations: m.citations,
          error: m.error
        }))
        // Load citations from the last assistant message
        const lastMsg = this.messages.slice().reverse().find(m => m.role === 'assistant')
        if (lastMsg && lastMsg.citations?.length) {
          this.activeCitations = lastMsg.citations
        }
      } catch (err) {
        console.error('Failed to load history', err)
      }
    }
  },
})
