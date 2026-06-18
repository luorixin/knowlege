<template>
  <router-view v-if="isBlankLayout" />

  <div v-else class="stitch-app-layout">
    <!-- Sidebar -->
    <aside class="stitch-sidebar sidebar-glass">
      <!-- Logo Section -->
      <div class="sidebar-brand">
        <div class="brand-logo">K</div>
        <div class="brand-text-col">
          <span class="brand-title">Enterprise KB</span>
          <span class="brand-subtitle">Management Console</span>
        </div>
      </div>

      <!-- Navigation Menu -->
      <nav class="sidebar-nav">
        <router-link
          to="/knowledge-bases"
          class="nav-item"
          :class="{ 'nav-item-active': $route.path.startsWith('/knowledge-bases') }"
        >
          <span class="material-symbols-outlined">database</span>
          <span>知识库管理</span>
        </router-link>
        
        <router-link
          to="/documents/upload"
          class="nav-item"
          :class="{ 'nav-item-active': $route.path.startsWith('/documents/upload') }"
        >
          <span class="material-symbols-outlined">upload_file</span>
          <span>文档上传</span>
        </router-link>

        <router-link
          to="/documents"
          class="nav-item"
          :class="{ 'nav-item-active': $route.path === '/documents' }"
        >
          <span class="material-symbols-outlined">folder_managed</span>
          <span>文档管理</span>
        </router-link>

        <router-link
          to="/chat"
          class="nav-item"
          :class="{ 'nav-item-active': $route.path.startsWith('/chat') }"
        >
          <span class="material-symbols-outlined">chat_bubble</span>
          <span>智能问答</span>
        </router-link>

        <router-link
          to="/tasks"
          class="nav-item"
          :class="{ 'nav-item-active': $route.path.startsWith('/tasks') }"
        >
          <span class="material-symbols-outlined">analytics</span>
          <span>任务监控</span>
        </router-link>

        <router-link
          to="/eval"
          class="nav-item"
          :class="{ 'nav-item-active': $route.path.startsWith('/eval') }"
        >
          <span class="material-symbols-outlined">dataset</span>
          <span>评估数据集</span>
        </router-link>
      </nav>

      <!-- Sidebar Footer -->
      <div class="sidebar-footer">
        <div class="status-indicator">
          <div class="status-dot"></div>
          <span>System Online</span>
        </div>
      </div>
    </aside>

    <!-- Main Content Area -->
    <div class="stitch-main-wrapper">
      <!-- Header -->
      <header class="stitch-header header-glass">
        <div class="header-left">
          <h1>Knowledge Agent Console</h1>
          <nav class="header-breadcrumbs">
            <span class="breadcrumb-active">MVP Workspace</span>
            <span class="breadcrumb-separator">/</span>
            <span class="breadcrumb-current">{{ currentRouteName }}</span>
          </nav>
        </div>

        <div class="header-right">
          <!-- MVP Tag -->
          <div class="mvp-tag">
            <div class="mvp-dot"></div>
            <span>MVP</span>
          </div>

          <!-- User Profile -->
          <div class="user-profile">
            <div class="user-info" v-if="userStore.currentUser">
              <span class="user-tenant">Tenant-{{ userStore.currentUser.tenantId }}</span>
              <span class="user-name">{{ userStore.currentUser.displayName || userStore.currentUser.username }}</span>
            </div>
            <div class="user-avatar">
              <span class="material-symbols-outlined">person</span>
            </div>
          </div>

          <!-- Logout -->
          <button class="logout-btn group" @click="logout">
            <span class="material-symbols-outlined icon-transition">logout</span>
            <span>Logout</span>
          </button>
        </div>
      </header>

      <!-- Main Workspace Canvas -->
      <main class="stitch-main-content">
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

<style scoped>
/* Stitch App Layout Variables */
* {
  box-sizing: border-box;
}

.stitch-app-layout {
  display: flex;
  min-height: 100vh;
  font-family: 'Inter', sans-serif;
  background-color: #f7f9fb;
  color: #191b24;
}

/* Sidebar */
.stitch-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  height: 100%;
  width: 260px;
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 8px;
  border-right: 1px solid rgba(194, 198, 216, 0.3);
  z-index: 50;
}

.sidebar-glass {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px;
  margin-bottom: 16px;
}

.brand-logo {
  width: 40px;
  height: 40px;
  background-color: #0050cb;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-weight: 900;
  font-size: 24px;
  flex-shrink: 0;
}

.brand-text-col {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
  color: #0050cb;
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 11px;
  color: rgba(66, 70, 86, 0.7);
  letter-spacing: 0.05em;
  margin-top: 2px;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-grow: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 12px;
  color: #424656;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.nav-item:hover {
  background-color: rgba(225, 226, 238, 0.5);
  transform: translateX(4px);
}

.nav-item .material-symbols-outlined {
  transition: transform 0.2s;
  font-size: 20px;
}

.nav-item:hover .material-symbols-outlined {
  transform: scale(1.1);
}

.nav-item-active {
  background-color: #d0e1fb !important;
  color: #0b1c30 !important;
  font-weight: 700;
  border-left: 4px solid #0050cb;
  transform: none !important;
}

.nav-item-active .material-symbols-outlined {
  font-variation-settings: 'FILL' 1;
}

.sidebar-footer {
  margin-top: auto;
  border-top: 1px solid rgba(194, 198, 216, 0.3);
  padding-top: 16px;
  padding-left: 8px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  font-weight: 600;
  color: rgba(66, 70, 86, 0.6);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #10b981;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

/* Main Content Wrapper */
.stitch-main-wrapper {
  margin-left: 260px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: calc(100% - 260px);
}

/* Header */
.stitch-header {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  padding: 0 40px;
  border-bottom: 1px solid rgba(194, 198, 216, 0.3);
}

.header-glass {
  background: rgba(250, 248, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.header-left {
  display: flex;
  flex-direction: column;
}

.header-left h1 {
  font-size: 20px;
  font-weight: 700;
  color: #191b24;
  margin: 0 0 2px 0;
  letter-spacing: -0.01em;
}

.header-breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  font-weight: 600;
}

.breadcrumb-active {
  color: #0050cb;
  border-bottom: 2px solid #0050cb;
  padding-bottom: 2px;
}

.breadcrumb-separator, .breadcrumb-current {
  color: rgba(66, 70, 86, 0.5);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.mvp-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 16px;
  background-color: #ecfdf5;
  color: #047857;
  border: 1px solid #d1fae5;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.mvp-dot {
  width: 6px;
  height: 6px;
  background-color: #10b981;
  border-radius: 50%;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-left: 24px;
  border-left: 1px solid rgba(194, 198, 216, 0.3);
}

.user-info {
  display: flex;
  flex-direction: column;
  text-align: right;
}

.user-tenant {
  font-size: 13px;
  font-weight: 600;
  color: #191b24;
}

.user-name {
  font-size: 11px;
  color: #424656;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #e1e2ee;
  border: 1px solid rgba(194, 198, 216, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #505f76;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #424656;
  background: transparent;
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
  padding: 0;
}

.logout-btn:hover {
  color: #ba1a1a;
}

.icon-transition {
  transition: transform 0.2s;
  font-size: 20px;
}

.logout-btn:hover .icon-transition {
  transform: translateX(4px);
}

/* Main Content */
.stitch-main-content {
  flex-grow: 1;
  padding: 24px 28px;
  background-color: transparent;
}
</style>
