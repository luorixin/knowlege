import { createRouter, createWebHistory } from 'vue-router'

import ChatView from '@/views/ChatView.vue'
import DocumentUploadView from '@/views/DocumentUploadView.vue'
import DocumentsView from '@/views/DocumentsView.vue'
import EvalDatasetsView from '@/views/EvalDatasetsView.vue'
import KnowledgeBaseDetailView from '@/views/KnowledgeBaseDetailView.vue'
import KnowledgeBasesView from '@/views/KnowledgeBasesView.vue'
import LoginView from '@/views/LoginView.vue'
import PermissionsView from '@/views/PermissionsView.vue'
import TasksView from '@/views/TasksView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/knowledge-bases',
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true, layout: 'blank' },
    },
    {
      path: '/knowledge-bases',
      name: 'knowledge-bases',
      component: KnowledgeBasesView,
    },
    {
      path: '/knowledge-bases/:id',
      name: 'knowledge-base-detail',
      component: KnowledgeBaseDetailView,
      props: true,
    },
    {
      path: '/documents',
      name: 'documents',
      component: DocumentsView,
    },
    {
      path: '/documents/upload',
      name: 'document-upload',
      component: DocumentUploadView,
    },
    {
      path: '/chat',
      name: 'chat',
      component: ChatView,
    },
    {
      path: '/eval',
      name: 'eval',
      component: EvalDatasetsView,
    },
    {
      path: '/tasks',
      name: 'tasks',
      component: TasksView,
    },
    {
      path: '/permissions',
      name: 'permissions',
      component: PermissionsView,
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  const hasUser = Boolean(window.localStorage.getItem('knowledge-user'))
  if (!hasUser) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }
  return true
})

export default router
