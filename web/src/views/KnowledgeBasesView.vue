<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>知识库列表</h2>
        <p>当前租户：{{ userStore.tenantId }}</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" :loading="knowledgeStore.loading" @click="refresh">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="dialogVisible = true">新建知识库</el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
    />

    <el-table
      v-loading="knowledgeStore.loading"
      :data="knowledgeStore.spaces"
      border
      row-key="id"
      empty-text="暂无知识库"
    >
      <el-table-column prop="name" label="知识库" min-width="220" />
      <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
      <el-table-column prop="visibility" label="可见性" width="120">
        <template #default="{ row }">
          <el-tag effect="plain">{{ row.visibility || 'PRIVATE' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button link @click="selectAndChat(row.id)">问答</el-button>
          <el-button link @click="selectAndUpload(row.id)">上传</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建知识库" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1024" show-word-limit />
        </el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="form.visibility" class="full-width">
            <el-option label="私有" value="PRIVATE" />
            <el-option label="租户内" value="TENANT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createSpace">创建</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Plus, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { apiErrorMessage } from '@/api/http'
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

function openDetail(spaceId: number) {
  knowledgeStore.selectSpace(spaceId)
  router.push(`/knowledge-bases/${spaceId}`)
}

function selectAndChat(spaceId: number) {
  knowledgeStore.selectSpace(spaceId)
  router.push('/chat')
}

function selectAndUpload(spaceId: number) {
  knowledgeStore.selectSpace(spaceId)
  router.push('/documents/upload')
}
</script>
