<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>解析任务状态</h2>
        <p>当前知识库：{{ selectedSpace?.name || '-' }}</p>
      </div>
      <div class="toolbar-actions">
        <el-select
          v-model="spaceId"
          class="space-select"
          placeholder="选择知识库"
          @change="loadStatuses"
        >
          <el-option
            v-for="space in knowledgeStore.spaces"
            :key="space.id"
            :label="space.name"
            :value="space.id"
          />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadStatuses">刷新</el-button>
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
      :data="rows"
      border
      row-key="documentId"
      empty-text="暂无解析任务"
    >
      <el-table-column prop="documentTitle" label="文档" min-width="240" show-overflow-tooltip />
      <el-table-column prop="parseTaskId" label="任务 ID" width="140" />
      <el-table-column prop="taskType" label="阶段" width="130" />
      <el-table-column prop="status" label="任务状态" width="140">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)" effect="plain">{{ row.status || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="parseStatus" label="解析状态" width="140" />
      <el-table-column label="进度" width="180">
        <template #default="{ row }">
          <el-progress :percentage="row.progressPercent || 0" />
        </template>
      </el-table-column>
      <el-table-column prop="errorMessage" label="错误信息" min-width="220" show-overflow-tooltip />
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'

import { getDocumentParseStatus, listDocuments } from '@/api/documents'
import { apiErrorMessage } from '@/api/http'
import type { DocumentParseStatus, EntityId } from '@/api/types'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'

interface TaskRow extends DocumentParseStatus {
  documentTitle: string
}

defineOptions({
  name: 'TasksView',
})

const userStore = useUserStore()
const knowledgeStore = useKnowledgeStore()
const spaceId = ref<EntityId | null>(knowledgeStore.selectedSpaceId)
const rows = ref<TaskRow[]>([])
const loading = ref(false)
const error = ref('')

const selectedSpace = computed(() => knowledgeStore.spaces.find((item) => item.id === spaceId.value) || null)

onMounted(async () => {
  if (userStore.tenantId) {
    await knowledgeStore.ensureSpaces(userStore.tenantId)
  }
  spaceId.value = knowledgeStore.selectedSpaceId || knowledgeStore.spaces[0]?.id || null
  if (spaceId.value) {
    await loadStatuses()
  }
})

watch(spaceId, (value) => {
  knowledgeStore.selectSpace(value)
})

async function loadStatuses() {
  if (!spaceId.value) return
  loading.value = true
  error.value = ''
  try {
    const documents = await listDocuments(spaceId.value)
    const statuses = await Promise.all(
      documents.map(async (document) => {
        const status = await getDocumentParseStatus(document.id)
        return {
          ...status,
          documentTitle: document.title,
        }
      }),
    )
    rows.value = statuses
  } catch (err) {
    error.value = apiErrorMessage(err)
  } finally {
    loading.value = false
  }
}

function tagType(status?: string) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}
</script>
