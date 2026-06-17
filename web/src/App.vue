<template>
  <router-view v-if="isBlankLayout" />

  <el-container v-else class="app-shell">
    <el-aside class="app-sidebar" width="232px">
      <div class="brand">
        <span class="brand-mark">K</span>
        <span class="brand-text">企业知识库</span>
      </div>

      <el-menu
        router
        :default-active="$route.path"
        class="nav-menu"
      >
        <el-menu-item index="/knowledge-bases">
          <el-icon><Collection /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
        <el-menu-item index="/documents/upload">
          <el-icon><UploadFilled /></el-icon>
          <span>文档上传</span>
        </el-menu-item>
        <el-menu-item index="/documents">
          <el-icon><Files /></el-icon>
          <span>文档管理</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>问答</span>
        </el-menu-item>
        <el-menu-item index="/tasks">
          <el-icon><Monitor /></el-icon>
          <span>任务监控</span>
        </el-menu-item>
        <el-menu-item index="/eval">
          <el-icon><DataAnalysis /></el-icon>
          <span>评估数据集</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div>
          <h1>知识库智能体控制台</h1>
          <p>文档上传、检索、问答和任务状态的 MVP 工作台</p>
        </div>
        <div class="user-area">
          <el-tag type="success" effect="plain">MVP</el-tag>
          <span v-if="userStore.currentUser" class="user-name">
            {{ userStore.currentUser.displayName }} / 租户 {{ userStore.currentUser.tenantId }}
          </span>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import {
  ChatDotRound,
  Collection,
  DataAnalysis,
  Files,
  Monitor,
  UploadFilled,
} from '@element-plus/icons-vue'
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isBlankLayout = computed(() => route.meta.layout === 'blank')

onMounted(() => {
  userStore.loadFromStorage()
})

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>
