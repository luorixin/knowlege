<template>
  <section class="page-section">
    <div class="section-header">
      <div class="header-title">
        <h2>{{ space?.name || '知识库详情' }}</h2>
        <p>Space ID：{{ spaceId }}</p>
      </div>
      <div class="toolbar-actions">
        <el-button plain :icon="Back" @click="router.push('/knowledge-bases')">返回</el-button>
        <el-button type="primary" :icon="UploadFilled" @click="goUpload" class="stitch-btn">上传文档</el-button>
        <el-button plain :icon="ChatDotRound" @click="goChat">进入问答</el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
    />

    <div class="detail-grid">
      <div class="stitch-card">
        <el-descriptions class="info-panel" :column="2" border>
          <el-descriptions-item label="名称">{{ space?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ space?.status || '-' }}</el-descriptions-item>
          <el-descriptions-item label="可见性">{{ space?.visibility || '-' }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ space?.ownerUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="说明" :span="2">{{ space?.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="metric-strip">
        <div class="metric-item stitch-metric">
          <span>文档数</span>
          <strong>{{ documents.length }}</strong>
        </div>
        <div class="metric-item stitch-metric">
          <span>解析中</span>
          <strong>{{ parsingCount }}</strong>
        </div>
        <div class="metric-item stitch-metric">
          <span>已启用</span>
          <strong>{{ activeCount }}</strong>
        </div>
      </div>
    </div>

    <div class="stitch-card-table">
      <div class="card-header">
        <h3>文档列表</h3>
      </div>
      <el-table
        v-loading="loading"
        :data="documents"
        row-key="id"
        empty-text="暂无文档"
        class="stitch-table"
      >
        <el-table-column prop="title" label="文档" min-width="240" />
        <el-table-column prop="docType" label="类型" width="120" />
        <el-table-column prop="industry" label="行业" width="140" />
        <el-table-column prop="serviceLine" label="服务线" width="160" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain" class="stitch-tag">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/documents?docId=${row.id}`)">查看</el-button>
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
