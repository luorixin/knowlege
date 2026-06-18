<template>
  <section class="page-section max-w-[1400px] mx-auto py-8 px-6">
    <div class="flex items-center justify-between mb-8">
      <div>
        <h2 class="text-2xl font-bold text-slate-900 tracking-tight m-0">文档库管理</h2>
        <p class="text-slate-500 mt-1.5 font-medium m-0 flex items-center gap-2">
          <span class="material-symbols-outlined text-[16px]">folder_open</span>
          当前空间：{{ selectedSpace?.name || '-' }}
        </p>
      </div>
      <div class="flex items-center gap-3">
        <el-select
          v-model="currentSpaceId"
          class="w-56"
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
        <el-button :icon="Refresh" :loading="loading" @click="loadDocuments" class="!rounded-lg font-medium shadow-sm hover:text-blue-600">刷新</el-button>
        <el-button type="primary" :icon="UploadFilled" @click="router.push('/documents/upload')" class="!rounded-lg font-medium shadow-md shadow-blue-500/20 bg-blue-700 hover:bg-blue-600 border-none">
          上传新文档
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="mb-6 !rounded-xl"
    />

    <div class="stitch-card stitch-card-table bg-white/80 backdrop-blur-sm">
      <el-table
        v-loading="loading"
        :data="documents"
        row-key="id"
        empty-text="当前知识库暂无文档"
        class="w-full"
      >
        <el-table-column prop="title" label="文档名称" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex items-center gap-3 font-medium text-slate-800">
              <span class="material-symbols-outlined text-blue-600/70 text-[20px]" v-if="row.docType === 'pdf'">picture_as_pdf</span>
              <span class="material-symbols-outlined text-blue-600/70 text-[20px]" v-else>description</span>
              {{ row.title }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="docType" label="格式" width="100">
          <template #default="{ row }">
            <span class="px-2 py-1 bg-slate-100 text-slate-600 rounded text-xs font-bold uppercase tracking-wider">{{ row.docType || 'TXT' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="industry" label="行业" width="130" />
        <el-table-column prop="serviceLine" label="服务线" width="150" />
        <el-table-column prop="confidentialLevel" label="密级" width="100">
          <template #default="{ row }">
            <span :class="row.confidentialLevel === 'HIGH' ? 'text-red-600 font-bold' : 'text-slate-600'">{{ row.confidentialLevel || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="解析状态" width="130">
          <template #default="{ row }">
            <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold"
                  :class="row.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-100 text-slate-600 border border-slate-200'">
              <span class="w-1.5 h-1.5 rounded-full" :class="row.status === 'ACTIVE' ? 'bg-emerald-500' : 'bg-slate-400'"></span>
              {{ row.status === 'ACTIVE' ? '可用' : row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="right">
          <template #default="{ row }">
            <el-button link type="primary" class="!font-semibold hover:text-blue-800" @click="openDetail(row.id)">详情</el-button>
            <el-button link type="primary" class="!font-semibold hover:text-blue-800" @click="openParseStatus(row.id)">状态</el-button>
            <el-button link type="danger" class="!font-semibold" @click="removeDocument(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="detailVisible" title="文档详情" size="520px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="detail">
        <el-descriptions :column="1" border class="mb-4">
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.docType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="行业">{{ detail.industry || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务线">{{ detail.serviceLine || '-' }}</el-descriptions-item>
          <el-descriptions-item label="密级">{{ detail.confidentialLevel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="源文件">{{ detail.sourceUri || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Hash">{{ detail.fileHash || '-' }}</el-descriptions-item>
          <el-descriptions-item label="解析状态">
            <el-tag :type="detail.currentVersion?.parseStatus === 'FAILED' ? 'danger' : 'info'">
              {{ detail.currentVersion?.parseStatus || '-' }}
            </el-tag>
            <el-button 
              v-if="detail.currentVersion?.parseStatus === 'FAILED'" 
              link type="primary" 
              class="ml-2"
              @click="retryParse(detail.id)">
              重试解析
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="解析失败原因" v-if="detail.currentVersion?.parseStatus === 'FAILED'">
            <span class="text-danger">{{ parseStatus?.errorMessage || '未知错误' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="脱敏状态">
            {{ detail.currentVersion?.desensitizeStatus || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Chunk 数">
            {{ detail.currentVersion?.chunkCount ?? '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="chunks-preview mt-4">
          <h4>Chunk 预览 <el-tag size="small" type="info">{{ chunks.length }}</el-tag></h4>
          <el-scrollbar max-height="300px">
            <div v-if="chunks.length === 0" class="text-gray-400 text-sm py-4">暂无 Chunk 数据</div>
            <div v-for="(chunk, idx) in chunks" :key="idx" class="chunk-card">
              <div class="chunk-header">
                <span class="chunk-id">#{{ chunk.chunkIndex ?? idx }}</span>
                <span class="chunk-page" v-if="chunk.pageNo">第 {{ chunk.pageNo }} 页</span>
              </div>
              <div class="chunk-content">{{ chunk.content }}</div>
            </div>
          </el-scrollbar>
        </div>
      </template>
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
  rebuildDocumentChunks,
  getDocumentChunks,
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
const chunks = ref<any[]>([])

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
  chunks.value = []
  try {
    const [doc, status, chunkList] = await Promise.all([
      getDocument(documentId),
      getDocumentParseStatus(documentId).catch(() => null),
      getDocumentChunks(documentId).catch(() => []),
    ])
    detail.value = doc
    parseStatus.value = status
    chunks.value = chunkList
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    detailLoading.value = false
  }
}

async function retryParse(documentId: EntityId) {
  try {
    await rebuildDocumentChunks(documentId, { parserProfile: 'default' })
    ElMessage.success('重试指令已发送，请稍后查看')
    await openDetail(documentId)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
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

<style scoped>
.chunks-preview {
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
}
.chunks-preview h4 {
  margin-top: 0;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.chunk-card {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
  background-color: #fafafa;
}
.chunk-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}
.chunk-id {
  font-weight: bold;
  color: #409eff;
}
.chunk-content {
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
}
.text-danger {
  color: #f56c6c;
}
</style>
