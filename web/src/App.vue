<template>
  <router-view v-if="isBlankLayout" />

  <div v-else class="flex min-h-screen font-sans bg-slate-50 text-slate-900">
    <!-- Sidebar -->
    <aside class="fixed left-0 top-0 h-full w-[260px] flex flex-col p-4 gap-2 border-r border-slate-200 z-50 bg-white/70 backdrop-blur-xl">
      <!-- Logo Section -->
      <div class="flex items-center gap-4 p-2 mb-4">
        <div class="w-10 h-10 bg-blue-700 text-white flex items-center justify-center rounded-lg font-black text-2xl shrink-0">K</div>
        <div class="flex flex-col">
          <span class="text-lg font-bold text-blue-700 leading-tight">Enterprise KB</span>
          <span class="text-[11px] text-slate-500 tracking-wider mt-0.5">Management Console</span>
        </div>
      </div>

      <!-- Navigation Menu -->
      <nav class="flex flex-col gap-1 grow">
        <router-link
          to="/knowledge-bases"
          class="flex items-center gap-4 px-4 py-3 rounded-xl text-slate-700 text-[13px] font-medium transition-all duration-200 hover:bg-slate-200/50 hover:translate-x-1"
          active-class="bg-blue-100 !text-slate-900 font-bold border-l-4 border-blue-700 !translate-x-0"
          :class="{ 'bg-blue-100 !text-slate-900 font-bold border-l-4 border-blue-700 !translate-x-0': $route.path.startsWith('/knowledge-bases') }"
        >
          <span class="material-symbols-outlined text-xl transition-transform duration-200 hover:scale-110">database</span>
          <span>知识库管理</span>
        </router-link>
        
        <router-link
          to="/documents/upload"
          class="flex items-center gap-4 px-4 py-3 rounded-xl text-slate-700 text-[13px] font-medium transition-all duration-200 hover:bg-slate-200/50 hover:translate-x-1"
          :class="{ 'bg-blue-100 !text-slate-900 font-bold border-l-4 border-blue-700 !translate-x-0': $route.path.startsWith('/documents/upload') }"
        >
          <span class="material-symbols-outlined text-xl transition-transform duration-200 hover:scale-110">upload_file</span>
          <span>文档上传</span>
        </router-link>

        <router-link
          to="/documents"
          class="flex items-center gap-4 px-4 py-3 rounded-xl text-slate-700 text-[13px] font-medium transition-all duration-200 hover:bg-slate-200/50 hover:translate-x-1"
          :class="{ 'bg-blue-100 !text-slate-900 font-bold border-l-4 border-blue-700 !translate-x-0': $route.path === '/documents' }"
        >
          <span class="material-symbols-outlined text-xl transition-transform duration-200 hover:scale-110">folder_managed</span>
          <span>文档管理</span>
        </router-link>

        <router-link
          to="/chat"
          class="flex items-center gap-4 px-4 py-3 rounded-xl text-slate-700 text-[13px] font-medium transition-all duration-200 hover:bg-slate-200/50 hover:translate-x-1"
          :class="{ 'bg-blue-100 !text-slate-900 font-bold border-l-4 border-blue-700 !translate-x-0': $route.path.startsWith('/chat') }"
        >
          <span class="material-symbols-outlined text-xl transition-transform duration-200 hover:scale-110">chat_bubble</span>
          <span>智能问答</span>
        </router-link>

        <router-link
          to="/tasks"
          class="flex items-center gap-4 px-4 py-3 rounded-xl text-slate-700 text-[13px] font-medium transition-all duration-200 hover:bg-slate-200/50 hover:translate-x-1"
          :class="{ 'bg-blue-100 !text-slate-900 font-bold border-l-4 border-blue-700 !translate-x-0': $route.path.startsWith('/tasks') }"
        >
          <span class="material-symbols-outlined text-xl transition-transform duration-200 hover:scale-110">analytics</span>
          <span>任务监控</span>
        </router-link>

        <router-link
          to="/eval"
          class="flex items-center gap-4 px-4 py-3 rounded-xl text-slate-700 text-[13px] font-medium transition-all duration-200 hover:bg-slate-200/50 hover:translate-x-1"
          :class="{ 'bg-blue-100 !text-slate-900 font-bold border-l-4 border-blue-700 !translate-x-0': $route.path.startsWith('/eval') }"
        >
          <span class="material-symbols-outlined text-xl transition-transform duration-200 hover:scale-110">dataset</span>
          <span>评估数据集</span>
        </router-link>

        <router-link
          to="/permissions"
          class="flex items-center gap-4 px-4 py-3 rounded-xl text-slate-700 text-[13px] font-medium transition-all duration-200 hover:bg-slate-200/50 hover:translate-x-1"
          :class="{ 'bg-blue-100 !text-slate-900 font-bold border-l-4 border-blue-700 !translate-x-0': $route.path.startsWith('/permissions') }"
        >
          <span class="material-symbols-outlined text-xl transition-transform duration-200 hover:scale-110">shield_person</span>
          <span>权限管理</span>
        </router-link>
      </nav>

      <!-- Sidebar Footer -->
      <div class="mt-auto border-t border-slate-200 pt-4 pl-2">
        <div class="flex items-center gap-2 text-[11px] font-semibold text-slate-500">
          <div class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
          <span>System Online</span>
        </div>
      </div>
    </aside>

    <!-- Main Content Area -->
    <div class="ml-[260px] flex flex-col min-h-screen w-[calc(100%-260px)]">
      <!-- Header -->
      <header class="sticky top-0 z-40 flex justify-between items-center h-16 px-10 border-b border-slate-200 bg-white/70 backdrop-blur-xl">
        <div class="flex flex-col">
          <h1 class="text-xl font-bold text-slate-900 mb-0.5 tracking-tight">Knowledge Agent Console</h1>
          <nav class="flex items-center gap-2 text-[11px] font-semibold">
            <span class="text-blue-700 border-b-2 border-blue-700 pb-0.5">MVP Workspace</span>
            <span class="text-slate-400">/</span>
            <span class="text-slate-400">{{ currentRouteName }}</span>
          </nav>
        </div>

        <div class="flex items-center gap-6">
          <!-- MVP Tag -->
          <div class="flex items-center gap-1.5 px-4 py-1 bg-emerald-50 text-emerald-700 border border-emerald-100 rounded-full text-[11px] font-bold tracking-widest">
            <div class="w-1.5 h-1.5 bg-emerald-500 rounded-full"></div>
            <span>MVP</span>
          </div>

          <!-- User Profile -->
          <div class="flex items-center gap-4 pl-6 border-l border-slate-200">
            <div class="flex flex-col text-right" v-if="userStore.currentUser">
              <span class="text-[13px] font-semibold text-slate-900">Tenant-{{ userStore.currentUser.tenantId }}</span>
              <span class="text-[11px] text-slate-600">{{ userStore.currentUser.displayName || userStore.currentUser.username }}</span>
            </div>
            <div class="w-9 h-9 rounded-full bg-slate-200 border border-slate-300 flex items-center justify-center text-slate-500">
              <span class="material-symbols-outlined">person</span>
            </div>
          </div>

          <!-- Logout -->
          <button class="flex items-center gap-1 text-slate-600 bg-transparent border-none text-[13px] font-medium cursor-pointer transition-colors duration-200 p-0 hover:text-red-600 group" @click="logout">
            <span class="material-symbols-outlined text-xl transition-transform duration-200 group-hover:translate-x-1">logout</span>
            <span>Logout</span>
          </button>
        </div>
      </header>

      <!-- Main Workspace Canvas -->
      <main class="grow p-6 md:p-8 bg-transparent">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'App',
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isBlankLayout = computed(() => route.meta.layout === 'blank')

const currentRouteName = computed(() => {
  const path = route.path
  if (path.includes('knowledge-bases')) return 'Knowledge Bases'
  if (path.includes('upload')) return 'Document Upload'
  if (path.includes('documents')) return 'Document Management'
  if (path.includes('chat')) return 'Q&A Chat'
  if (path.includes('tasks')) return 'Task Monitor'
  if (path.includes('eval')) return 'Eval Datasets'
  if (path.includes('permissions')) return 'Permissions'
  return 'Overview'
})

onMounted(() => {
  userStore.loadFromStorage()
})

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>
