import { defineStore } from 'pinia'

const STORAGE_KEY = 'knowledge-user'

export interface MockUser {
  userId: number
  tenantId: number
  username?: string
  displayName: string
  role: string
}

interface UserState {
  currentUser: MockUser | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    currentUser: null,
  }),
  getters: {
    isLoggedIn: (state) => state.currentUser !== null,
    userId: (state) => state.currentUser?.userId,
    tenantId: (state) => state.currentUser?.tenantId,
  },
  actions: {
    loadFromStorage() {
      const raw = window.localStorage.getItem(STORAGE_KEY)
      if (!raw) return
      try {
        this.currentUser = JSON.parse(raw) as MockUser
      } catch {
        window.localStorage.removeItem(STORAGE_KEY)
      }
    },
    login(payload: Pick<MockUser, 'userId' | 'tenantId' | 'displayName'>) {
      this.currentUser = {
        ...payload,
        role: 'MVP_USER',
      }
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(this.currentUser))
    },
    logout() {
      this.currentUser = null
      window.localStorage.removeItem(STORAGE_KEY)
    },
  },
})
