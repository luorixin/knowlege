<template>
  <router-view v-if="isBlankLayout" />

  <div v-else class="relative min-h-screen bg-[#02050f] text-slate-100 flex flex-col md:flex-row overflow-hidden font-sans select-none cyber-grid-overlay">
    
    <!-- Mobile Sticky Header Bar -->
    <div class="md:hidden flex items-center justify-between p-4 bg-[#050b18] border-b border-[#00f0ff]/10 w-full shrink-0 z-40 relative">
      <div class="flex items-center space-x-3 select-none">
        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center border border-neon-cyan/20">
          <span class="material-symbols-outlined w-4 h-4 text-neon-cyan animate-pulse text-[16px] leading-none">memory</span>
        </div>
        <div>
          <span class="block font-display font-black text-xs tracking-wider text-white uppercase">Enterprise KB</span>
        </div>
      </div>
      <button
        @click="isSidebarOpen = !isSidebarOpen"
        class="p-1.5 border border-[#00f0ff]/20 bg-slate-950/80 rounded-lg text-neon-cyan hover:text-white hover:border-neon-cyan/50 transition-all cursor-pointer flex items-center justify-center select-none"
        title="Toggle Navigation Menu"
      >
        <span class="material-symbols-outlined text-[18px]">{{ isSidebarOpen ? 'close' : 'menu' }}</span>
      </button>
    </div>

    <!-- Sidebar -->
    <aside :class="[
      'w-full md:w-64 bg-[#050b18] border-r border-[#00f0ff]/10 flex-col shrink-0 z-30 transition-all duration-200',
      isSidebarOpen ? 'flex' : 'hidden md:flex'
    ]">
      
      <!-- Branding header block -->
      <div class="p-6 pb-2 border-b border-white/[0.05] flex items-center space-x-3 select-none">
        <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center border border-neon-cyan/20">
          <span class="material-symbols-outlined text-neon-cyan animate-pulse text-[20px] leading-none">memory</span>
        </div>
        <div>
          <span class="block font-display font-black text-sm tracking-widest text-white uppercase">Enterprise KB</span>
          <span class="block text-[9px] font-mono text-neon-cyan/85 tracking-widest uppercase mt-0.5">Console Unit</span>
        </div>
      </div>

      <!-- Sidebar nav menu links -->
      <nav class="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
        <router-link
          to="/knowledge-bases"
          @click="isSidebarOpen = false"
          class="w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer border border-transparent text-slate-400 hover:text-white hover:bg-white/[0.015]"
          active-class="bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border-neon-cyan/30 text-white font-bold"
        >
          <span class="material-symbols-outlined text-[18px]" :class="$route.path.startsWith('/knowledge-bases') ? 'text-neon-cyan' : 'text-slate-500'">database</span>
          <span>Knowledge Base</span>
        </router-link>

        <router-link
          to="/documents/upload"
          @click="isSidebarOpen = false"
          class="w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer border border-transparent text-slate-400 hover:text-white hover:bg-white/[0.015]"
          active-class="bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border-neon-cyan/30 text-white font-bold"
        >
          <span class="material-symbols-outlined text-[18px]" :class="$route.path.startsWith('/documents/upload') ? 'text-neon-cyan' : 'text-slate-500'">upload_file</span>
          <span>Document Upload</span>
        </router-link>

        <router-link
          to="/documents"
          @click="isSidebarOpen = false"
          class="w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer border border-transparent text-slate-400 hover:text-white hover:bg-white/[0.015]"
          active-class="bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border-neon-cyan/30 text-white font-bold"
        >
          <span class="material-symbols-outlined text-[18px]" :class="$route.path === '/documents' ? 'text-neon-cyan' : 'text-slate-500'">folder_managed</span>
          <span>Document Mgmt</span>
        </router-link>

        <router-link
          to="/chat"
          @click="isSidebarOpen = false"
          class="w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer border border-transparent text-slate-400 hover:text-white hover:bg-white/[0.015]"
          active-class="bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border-neon-cyan/30 text-white font-bold"
        >
          <span class="material-symbols-outlined text-[18px]" :class="$route.path.startsWith('/chat') ? 'text-neon-cyan' : 'text-slate-500'">chat_bubble</span>
          <span>RAG Q&A</span>
        </router-link>

        <router-link
          to="/tasks"
          @click="isSidebarOpen = false"
          class="w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer border border-transparent text-slate-400 hover:text-white hover:bg-white/[0.015]"
          active-class="bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border-neon-cyan/30 text-white font-bold"
        >
          <span class="material-symbols-outlined text-[18px]" :class="$route.path.startsWith('/tasks') ? 'text-neon-cyan' : 'text-slate-500'">analytics</span>
          <span>Task Telemetry</span>
        </router-link>

        <router-link
          v-if="userStore.isAdmin"
          to="/eval"
          @click="isSidebarOpen = false"
          class="w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer border border-transparent text-slate-400 hover:text-white hover:bg-white/[0.015]"
          active-class="bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border-neon-cyan/30 text-white font-bold"
        >
          <span class="material-symbols-outlined text-[18px]" :class="$route.path.startsWith('/eval') ? 'text-neon-cyan' : 'text-slate-500'">dataset</span>
          <span>Eval Datasets</span>
        </router-link>

        <router-link
          v-if="userStore.isAdmin"
          to="/permissions"
          @click="isSidebarOpen = false"
          class="w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer border border-transparent text-slate-400 hover:text-white hover:bg-white/[0.015]"
          active-class="bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border-neon-cyan/30 text-white font-bold"
        >
          <span class="material-symbols-outlined text-[18px]" :class="$route.path.startsWith('/permissions') ? 'text-neon-cyan' : 'text-slate-500'">shield</span>
          <span>Permissions</span>
        </router-link>
      </nav>

      <!-- Sidebar Footer User detail drawer panel -->
      <div class="p-4.5 border-t border-white/[0.05] bg-[#030612]/60 mt-auto text-xs font-mono text-slate-400">
        <div class="flex items-center justify-between mb-3">
          <span class="block text-[9px] uppercase tracking-wider text-slate-500">System Mode</span>
          <button @click="toggleTheme" class="flex items-center gap-1 text-slate-400 hover:text-white transition-colors cursor-pointer outline-none">
            <span class="material-symbols-outlined text-[14px]">{{ isLightMode ? 'dark_mode' : 'light_mode' }}</span>
            <span class="text-[9px] uppercase tracking-wider">{{ isLightMode ? 'Dark' : 'Light' }}</span>
          </button>
        </div>
        <div class="flex items-center justify-between mb-3">
          <span class="block text-[9px] uppercase tracking-wider text-slate-500">Security Sector</span>
          <span class="flex h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-500" />
        </div>
        <div class="flex items-center gap-3" v-if="userStore.currentUser">
          <div class="w-8 h-8 rounded-full bg-gradient-to-br from-cyan-500 to-purple-600 border border-white/[0.05] flex items-center justify-center font-bold text-white uppercase text-xs">
            {{ userStore.currentUser.username?.slice(0, 2) || '??' }}
          </div>
          <div class="flex-1 truncate">
            <span class="block text-white font-sans font-medium truncate">{{ userStore.currentUser.displayName || userStore.currentUser.username || 'User' }}</span>
            <span class="block text-[9px] truncate text-slate-500 font-mono">Tenant-{{ userStore.currentUser.tenantId }}</span>
          </div>
        </div>
        <button
          @click="logout"
          class="w-full mt-4 flex items-center justify-center space-x-2 py-2 border border-white/[0.06] hover:border-red-400 rounded-xl hover:text-red-400 transition-all cursor-pointer active:scale-95 text-[11px] bg-slate-950/40 font-semibold"
        >
          <span class="material-symbols-outlined text-[14px]">logout</span>
          <span>Log Out</span>
        </button>
      </div>
    </aside>

    <!-- Main Viewing Area panel -->
    <main class="flex-1 p-6 md:p-8 overflow-y-auto max-h-screen relative z-10">
      <div class="w-full max-w-7xl mx-auto h-full">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'App',
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isBlankLayout = computed(() => route.meta.layout === 'blank')
const isSidebarOpen = ref(false)
const isLightMode = ref(false)

onMounted(() => {
  userStore.loadFromStorage()
  
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'light') {
    isLightMode.value = true
    document.documentElement.classList.add('light')
  }
})

function toggleTheme() {
  isLightMode.value = !isLightMode.value
  if (isLightMode.value) {
    document.documentElement.classList.add('light')
    localStorage.setItem('theme', 'light')
  } else {
    document.documentElement.classList.remove('light')
    localStorage.setItem('theme', 'dark')
  }
}

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>
