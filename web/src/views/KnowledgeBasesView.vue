<template>
  <section class="page-section">
    <div class="section-header">
      <div class="flex flex-col">
        <h2 class="text-2xl font-bold text-slate-900 m-0 mb-1 tracking-tight">知识库列表</h2>
        <p class="text-sm text-slate-600 m-0">当前租户：{{ userStore.tenantId }}</p>
      </div>
      <div class="toolbar-actions">
        <el-button plain :icon="Refresh" :loading="knowledgeStore.loading" @click="refresh">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="dialogVisible = true" class="stitch-btn">新建知识库</el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
    />

    <div v-loading="knowledgeStore.loading" class="min-h-[200px]">
      <div v-if="knowledgeStore.spaces.length === 0 && !knowledgeStore.loading" class="flex flex-col items-center justify-center p-12 bg-white rounded-2xl border border-slate-200/60 shadow-sm border-dashed">
        <el-icon class="text-4xl text-slate-300 mb-4"><FolderOpened /></el-icon>
        <p class="text-slate-500 m-0 mb-4">暂无知识库</p>
        <el-button type="primary" plain @click="dialogVisible = true">立即创建</el-button>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <div 
          v-for="space in knowledgeStore.spaces" 
          :key="space.id" 
          class="stitch-card flex flex-col cursor-pointer transition-all hover:-translate-y-1 hover:shadow-lg hover:border-blue-200 group"
          @click="openDetail(space.id)"
        >
          <div class="flex justify-between items-start mb-3">
            <h3 class="text-lg font-bold text-slate-900 m-0 line-clamp-1 group-hover:text-blue-600 transition-colors" :title="space.name">{{ space.name }}</h3>
            <el-tag :type="space.status === 'ACTIVE' ? 'success' : 'info'" effect="light" round size="small" class="font-medium shrink-0 ml-2 border-none bg-slate-100" :class="{ '!bg-green-50 !text-green-600': space.status === 'ACTIVE' }">
              {{ space.status }}
            </el-tag>
          </div>
          
          <p class="text-slate-500 text-sm flex-1 mb-5 line-clamp-2 leading-relaxed" :title="space.description">
            {{ space.description || '暂无说明' }}
          </p>
          
          <div class="flex items-center justify-between border-t border-slate-100 pt-4 mt-auto">
            <div class="flex gap-2">
              <el-tag size="small" effect="plain" type="info" class="border-slate-200 text-slate-500 bg-transparent">
                {{ space.visibility === 'PRIVATE' ? '私有' : space.visibility }}
              </el-tag>
            </div>
            <div class="flex gap-2" @click.stop>
              <el-button link type="primary" class="!px-1" @click="selectAndUpload(space.id)">上传</el-button>
              <el-button link type="primary" class="!px-1" @click="selectAndChat(space.id)">问答</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新建知识库" width="520px" class="stitch-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="128" show-word-limit placeholder="给您的知识库起个响亮的名字" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1024" show-word-limit placeholder="简单描述一下这个知识库的用途..." />
        </el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="form.visibility" class="full-width">
            <el-option label="私有" value="PRIVATE" />
            <el-option label="租户内公开" value="TENANT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" plain class="stitch-btn">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createSpace" class="stitch-btn">创建知识库</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Plus, Refresh, FolderOpened } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { apiErrorMessage } from '@/api/http'
import type { EntityId } from '@/api/types'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'KnowledgeBasesView',
})

const router = useRouter()
const userStore = useUserStore()
const knowledgeStore = useKnowledgeStore()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const creating = ref(false)
const error = ref('')

const form = reactive({
  name: '',
  description: '',
  visibility: 'PRIVATE',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
}

onMounted(refresh)

async function refresh() {
  if (!userStore.tenantId) return
  error.value = ''
  try {
    await knowledgeStore.fetchSpaces(userStore.tenantId)
  } catch (err) {
    error.value = apiErrorMessage(err)
  }
}

async function createSpace() {
  if (!formRef.value || !userStore.tenantId || !userStore.userId) return
  await formRef.value.validate()
  creating.value = true
  try {
    const space = await knowledgeStore.createSpace({
      tenantId: userStore.tenantId,
      ownerUserId: userStore.userId,
      name: form.name,
      description: form.description,
      visibility: form.visibility,
    })
    dialogVisible.value = false
    form.name = ''
    form.description = ''
    ElMessage.success('知识库已创建')
    router.push(`/knowledge-bases/${space.id}`)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    creating.value = false
  }
}

function openDetail(spaceId: EntityId) {
  knowledgeStore.selectSpace(spaceId)
  router.push(`/knowledge-bases/${spaceId}`)
}

function selectAndChat(spaceId: EntityId) {
  knowledgeStore.selectSpace(spaceId)
  router.push('/chat')
}

function selectAndUpload(spaceId: EntityId) {
  knowledgeStore.selectSpace(spaceId)
  router.push('/documents/upload')
}
</script>
