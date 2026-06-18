<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>文档列表</h2>
        <p>当前知识库：{{ selectedSpace?.name || '-' }}</p>
      </div>
      <div class="toolbar-actions">
        <el-select
          v-model="currentSpaceId"
          class="space-select"
          placeholder="选择知识库"
          @change="loadDocuments"
        >
          <el-option
            v-for="space in knowledgeStore.spaces"
            :key="space.id"
            :label="space.name"
            :value="space.id"
          />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadDocuments">刷新</el-button>
        <el-button type="primary" :icon="UploadFilled" @click="router.push('/documents/upload')">
          上传文档
        </el-button>
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
      v-loading="loading"
      :data="documents"
      border
      row-key="id"
      empty-text="暂无文档"
    >
      <el-table-column prop="title" label="文档" min-width="240" show-overflow-tooltip />
      <el-table-column prop="docType" label="类型" width="110" />
      <el-table-column prop="industry" label="行业" width="130" />
      <el-table-column prop="serviceLine" label="服务线" width="150" />
      <el-table-column prop="confidentialLevel" label="密级" width="120" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button link @click="openParseStatus(row.id)">解析状态</el-button>
          <el-button link type="danger" @click="removeDocument(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailVisible" title="文档详情" size="520px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <el-descriptions v-else-if="detail" :column="1" border>
        <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.docType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="行业">{{ detail.industry || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务线">{{ detail.serviceLine || '-' }}</el-descriptions-item>
        <el-descriptions-item label="密级">{{ detail.confidentialLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="源文件">{{ detail.sourceUri || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Hash">{{ detail.fileHash || '-' }}</el-descriptions-item>
        <el-descriptions-item label="解析状态">
          {{ detail.currentVersion?.parseStatus || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="Chunk 数">
          {{ detail.currentVersion?.chunkCount ?? '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="未选择文档" />
    </el-drawer>

    <el-dialog v-model="statusVisible" title="解析任务状态" width="520px">
      <el-skeleton v-if="statusLoading" :rows="5" animated />
      <el-descriptions v-else-if="parseStatus" :column="1" border>
        <el-descriptions-item label="任务 ID">{{ parseStatus.parseTaskId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="阶段">{{ parseStatus.taskType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">{{ parseStatus.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="解析状态">{{ parseStatus.parseStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="进度">
          <el-progress :percentage="parseStatus.progressPercent || 0" />
        </el-descriptions-item>
        <el-descriptions-item label="错误">
          {{ parseStatus.errorMessage || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无状态" />
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Refresh, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  deleteDocument,
  getDocument,
  getDocumentParseStatus,
  listDocuments,
} from '@/api/documents'
import { apiErrorMessage } from '@/api/http'
import type { DocumentDetail, DocumentListItem, DocumentParseStatus, EntityId } from '@/api/types'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'DocumentsView',
})

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const knowledgeStore = useKnowledgeStore()

const currentSpaceId = ref<EntityId | null>(knowledgeStore.selectedSpaceId)
const documents = ref<DocumentListItem[]>([])
const loading = ref(false)
const error = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<DocumentDetail | null>(null)
const statusVisible = ref(false)
const statusLoading = ref(false)
const parseStatus = ref<DocumentParseStatus | null>(null)

const selectedSpace = computed(() => knowledgeStore.spaces.find((item) => item.id === currentSpaceId.value) || null)

onMounted(async () => {
  if (userStore.tenantId) {
    await knowledgeStore.ensureSpaces(userStore.tenantId)
  }
  currentSpaceId.value = knowledgeStore.selectedSpaceId || knowledgeStore.spaces[0]?.id || null
  if (currentSpaceId.value) {
    await loadDocuments()
  }
  const docId = typeof route.query.docId === 'string' ? route.query.docId : null
  if (docId) {
    await openDetail(docId)
  }
})

watch(currentSpaceId, (spaceId) => {
  knowledgeStore.selectSpace(spaceId)
})

async function loadDocuments() {
  if (!currentSpaceId.value) return
  loading.value = true
  error.value = ''
  try {
    documents.value = await listDocuments(currentSpaceId.value)
  } catch (err) {
    error.value = apiErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function openDetail(documentId: EntityId) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getDocument(documentId)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    detailLoading.value = false
  }
}

async function openParseStatus(documentId: EntityId) {
  statusVisible.value = true
  statusLoading.value = true
  parseStatus.value = null
  try {
    parseStatus.value = await getDocumentParseStatus(documentId)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    statusLoading.value = false
  }
}

async function removeDocument(documentId: EntityId) {
  await ElMessageBox.confirm('删除后文档状态将不可用于检索。', '删除文档', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  try {
    await deleteDocument(documentId)
    ElMessage.success('文档已删除')
    await loadDocuments()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  }
}
</script>
