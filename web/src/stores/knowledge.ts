import { defineStore } from 'pinia'

import { createKnowledgeSpace, listKnowledgeSpaces } from '@/api/knowledge'
import type { CreateKnowledgeSpacePayload, EntityId, KnowledgeSpace } from '@/api/types'

interface KnowledgeState {
  spaces: KnowledgeSpace[]
  selectedSpaceId: EntityId | null
  loading: boolean
}

export const useKnowledgeStore = defineStore('knowledge', {
  state: (): KnowledgeState => ({
    spaces: [],
    selectedSpaceId: window.localStorage.getItem('knowledge-selected-space') || null,
    loading: false,
  }),
  getters: {
    selectedSpace: (state) => state.spaces.find((item) => item.id === state.selectedSpaceId) || null,
  },
  actions: {
    async fetchSpaces(tenantId: EntityId) {
      this.loading = true
      try {
        this.spaces = await listKnowledgeSpaces(tenantId)
        const selectedExists = this.spaces.some((item) => item.id === this.selectedSpaceId)
        if (!selectedExists) {
          this.selectSpace(this.spaces[0]?.id || null)
        }
      } finally {
        this.loading = false
      }
    },
    async ensureSpaces(tenantId: EntityId) {
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
    selectSpace(spaceId: EntityId | null) {
      this.selectedSpaceId = spaceId
      if (spaceId) {
        window.localStorage.setItem('knowledge-selected-space', spaceId)
      } else {
        window.localStorage.removeItem('knowledge-selected-space')
      }
    },
  },
})
