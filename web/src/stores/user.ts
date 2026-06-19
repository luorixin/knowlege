import { defineStore } from 'pinia'

const STORAGE_KEY = 'knowledge-user'

export interface MockUser {
  userId: string
  tenantId: string
  username?: string
  displayName: string
  role: string
}

interface UserState {
  currentUser: MockUser | null
  token: string | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    currentUser: null,
    token: window.localStorage.getItem('knowledge-token') || null,
  }),
  getters: {
    isLoggedIn: (state) => state.currentUser !== null,
    isAdmin: (state) => state.currentUser?.role === 'ADMIN' || state.currentUser?.role === 'KNOWLEDGE_ADMIN',
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
    async login(username: string, password: string) {
      const { http, unwrapResponse } = await import('@/api/http')
      const response = await http.post('/api/v1/auth/login', { username, password })
      const data = unwrapResponse<any>(response)
      
      this.token = data.accessToken
      window.localStorage.setItem('knowledge-token', this.token as string)
      
      // Parse JWT
      const base64Url = (this.token as string).split('.')[1]
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
      const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
      }).join(''))
      
      const claims = JSON.parse(jsonPayload)
      
      this.currentUser = {
        userId: claims.userId,
        tenantId: claims.tenantId,
        username: claims.sub,
        displayName: claims.sub,
        role: claims.roles ? claims.roles[0] : 'MVP_USER',
      }
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(this.currentUser))
    },
    logout() {
      this.currentUser = null
      this.token = null
      window.localStorage.removeItem(STORAGE_KEY)
      window.localStorage.removeItem('knowledge-token')
      
      // Redirect to login using window.location to ensure fresh state
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    },
  },
})
