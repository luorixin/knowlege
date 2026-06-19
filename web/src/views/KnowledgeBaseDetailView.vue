<template>
  <section class="page-section">
    <div class="section-header !mb-4">
      <div class="header-title flex items-center gap-3">
        <h2 class="text-2xl font-bold text-slate-900 m-0">{{ space?.name || '知识库详情' }}</h2>
        <el-tag type="info" effect="plain" class="font-mono bg-slate-100 border-slate-200">ID: {{ spaceId }}</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-button plain :icon="Back" @click="router.push('/knowledge-bases')" class="stitch-btn">返回</el-button>
        <el-button plain :icon="ChatDotRound" @click="goChat" class="stitch-btn">进入问答</el-button>
        <el-button type="primary" :icon="UploadFilled" @click="goUpload" class="stitch-btn shadow-sm">上传文档</el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="mb-2"
    />

    <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
      <div class="xl:col-span-2 stitch-card flex flex-col justify-center">
        <el-descriptions class="info-panel !border-none !p-0 !shadow-none bg-transparent" :column="2">
          <el-descriptions-item label="描述说明" :span="2">
            <span class="text-slate-600 leading-relaxed">{{ space?.description || '暂无说明' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="可见性">
            <el-tag size="small" type="info" effect="plain" class="bg-slate-50">{{ space?.visibility === 'PRIVATE' ? '私有' : '租户可见' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="space?.status === 'ACTIVE' ? 'success' : 'info'" effect="light" class="border-none font-medium" :class="{'bg-green-50 text-green-600': space?.status === 'ACTIVE'}">{{ space?.status || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="空间负责人">
            <span class="text-slate-700 font-medium">{{ space?.ownerUserId || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-2 gap-4">
        <div class="stitch-card flex flex-col items-center justify-center p-4 bg-gradient-to-br from-blue-50 to-slate-50 border-blue-100/50">
          <span class="text-sm text-slate-500 font-medium mb-1">总文档数</span>
          <strong class="text-3xl font-bold text-blue-900">{{ documents.length }}</strong>
        </div>
        <div class="stitch-card flex flex-col items-center justify-center p-4 bg-gradient-to-br from-amber-50 to-slate-50 border-amber-100/50">
          <span class="text-sm text-slate-500 font-medium mb-1">解析处理中</span>
          <strong class="text-3xl font-bold text-amber-700">{{ parsingCount }}</strong>
        </div>
        <div class="stitch-card flex flex-col items-center justify-center p-4 bg-gradient-to-br from-green-50 to-slate-50 border-green-100/50 sm:col-span-1 xl:col-span-2">
          <span class="text-sm text-slate-500 font-medium mb-1">已就绪 (ACTIVE)</span>
          <strong class="text-3xl font-bold text-green-700">{{ activeCount }}</strong>
        </div>
      </div>
    </div>

    <div class="stitch-card stitch-card-table mt-2">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-bold text-slate-800 m-0">空间文档库</h3>
      </div>
      <el-table
        v-loading="loading"
        :data="documents"
        row-key="id"
        empty-text="当前知识库暂无文档"
        class="stitch-table"
      >
        <el-table-column prop="title" label="文档名称" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium text-slate-700">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="docType" label="类型" width="120" />
        <el-table-column prop="industry" label="行业" width="140" />
        <el-table-column prop="serviceLine" label="服务线" width="160" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="light" round class="stitch-tag border-none font-medium" :class="{'bg-green-50 text-green-600': row.status === 'ACTIVE', 'bg-slate-100 text-slate-600': row.status !== 'ACTIVE'}">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" class="font-medium" @click="router.push(`/documents?docId=${row.id}`)">查阅详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Back, ChatDotRound, UploadFilled } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { listDocuments } from '@/api/documents'
import { apiErrorMessage } from '@/api/http'
import type { DocumentListItem, EntityId } from '@/api/types'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'KnowledgeBaseDetailView',
})

const props = defineProps<{
  id: string
}>()

const router = useRouter()
const userStore = useUserStore()
const knowledgeStore = useKnowledgeStore()
const documents = ref<DocumentListItem[]>([])
const loading = ref(false)
const error = ref('')

const spaceId = computed<EntityId>(() => props.id)
const space = computed(() => knowledgeStore.spaces.find((item) => item.id === spaceId.value) || null)
const parsingCount = computed(() => documents.value.filter((item) => item.status !== 'ACTIVE').length)
const activeCount = computed(() => documents.value.filter((item) => item.status === 'ACTIVE').length)

onMounted(async () => {
  if (userStore.tenantId) {
    await knowledgeStore.ensureSpaces(userStore.tenantId)
  }
  knowledgeStore.selectSpace(spaceId.value)
  await loadDocuments()
})

async function loadDocuments() {
  loading.value = true
  error.value = ''
  try {
    documents.value = await listDocuments(spaceId.value)
  } catch (err) {
    error.value = apiErrorMessage(err)
  } finally {
    loading.value = false
  }
}

function goUpload() {
  knowledgeStore.selectSpace(spaceId.value)
  router.push('/documents/upload')
}

function goChat() {
  knowledgeStore.selectSpace(spaceId.value)
  router.push('/chat')
}
</script>

<style scoped>
.page-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 8px;
}

.header-title h2 {
  font-size: 28px;
  font-weight: 700;
  color: #191b24;
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.header-title p {
  font-size: 14px;
  color: #424656;
  margin: 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
}

/* Override Element Plus Descriptions to fit Stitch UI */
:deep(.info-panel.el-descriptions) {
  --el-descriptions-table-border: 1px solid rgba(194, 198, 216, 0.3);
  --el-descriptions-item-bordered-label-background: #f7f9fb;
}

:deep(.info-panel .el-descriptions__label) {
  font-weight: 600;
  color: #424656;
}
</style>
