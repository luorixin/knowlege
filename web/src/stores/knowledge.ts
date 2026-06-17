import { defineStore } from 'pinia'

import { createKnowledgeSpace, listKnowledgeSpaces } from '@/api/knowledge'
import type { CreateKnowledgeSpacePayload, KnowledgeSpace } from '@/api/types'

interface KnowledgeState {
  spaces: KnowledgeSpace[]
  selectedSpaceId: number | null
  loading: boolean
}

export const useKnowledgeStore = defineStore('knowledge', {
  state: (): KnowledgeState => ({
    spaces: [],
    selectedSpaceId: Number(window.localStorage.getItem('knowledge-selected-space')) || null,
    loading: false,
  }),
  getters: {
    selectedSpace: (state) => state.spaces.find((item) => item.id === state.selectedSpaceId) || null,
  },
  actions: {
    async fetchSpaces(tenantId: number) {
      this.loading = true
      try {
        this.spaces = await listKnowledgeSpaces(tenantId)
        if (!this.selectedSpaceId && this.spaces.length > 0) {
          this.selectSpace(this.spaces[0].id)
        }
      } finally {
        this.loading = false
      }
    },
    async ensureSpaces(tenantId: number) {
      if (this.spaces.length === 0) {
        await this.fetchSpaces(tenantId)
      }
    },
    async createSpace(payload: CreateKnowledgeSpacePayload) {
      const space = await createKnowledgeSpace(payload)
      this.spaces = [space, ...this.spaces.filter((item) => item.id !== space.id)]
      this.selectSpace(space.id)
      return space
    },
    selectSpace(spaceId: number | null) {
      this.selectedSpaceId = spaceId
      if (spaceId) {
        window.localStorage.setItem('knowledge-selected-space', String(spaceId))
      } else {
        window.localStorage.removeItem('knowledge-selected-space')
      }
    },
  },
})
