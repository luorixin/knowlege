<template>
  <section class="page-section">
    <div class="section-header">
      <div class="header-title">
        <h2>任务中心</h2>
        <p>统一查看解析、切片、Embedding 和检索索引任务</p>
      </div>
      <div class="toolbar-actions">
        <el-select
          v-model="spaceId"
          class="space-select"
          placeholder="选择知识库"
          @change="loadTasks"
        >
          <el-option
            v-for="space in knowledgeStore.spaces"
            :key="space.id"
            :label="space.name"
            :value="space.id"
          />
        </el-select>
        <el-select v-model="taskCategory" class="filter-select" placeholder="任务类型" @change="loadTasks">
          <el-option label="全部阶段" value="" />
          <el-option label="解析 / 切片" value="PARSE_CHUNK" />
          <el-option label="Embedding / 索引" value="EMBEDDING_INDEX" />
        </el-select>
        <el-select v-model="status" class="filter-select" placeholder="任务状态" @change="loadTasks">
          <el-option label="全部状态" value="" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="运行中" value="RUNNING" />
          <el-option label="部分成功" value="PARTIAL_SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-button plain :icon="Refresh" :loading="loading" @click="loadTasks">刷新</el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
    />

    <div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-2">
      <div class="stitch-card flex flex-col justify-center p-4 bg-white border-l-4 border-l-slate-400">
        <span class="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">总任务数</span>
        <strong class="text-3xl font-bold text-slate-800">{{ rows.length }}</strong>
      </div>
      <div class="stitch-card flex flex-col justify-center p-4 bg-white border-l-4 border-l-amber-400">
        <span class="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">待处理 (PENDING)</span>
        <strong class="text-3xl font-bold text-amber-600">{{ statusCount.PENDING }}</strong>
      </div>
      <div class="stitch-card flex flex-col justify-center p-4 bg-white border-l-4 border-l-blue-400">
        <span class="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">运行中 (RUNNING)</span>
        <strong class="text-3xl font-bold text-blue-600">{{ statusCount.RUNNING }}</strong>
      </div>
      <div class="stitch-card flex flex-col justify-center p-4 bg-white border-l-4 border-l-orange-400" :class="{'opacity-75 grayscale': statusCount.PARTIAL_SUCCESS === 0}">
        <span class="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">部分成功 (PARTIAL)</span>
        <strong class="text-3xl font-bold text-orange-500">{{ statusCount.PARTIAL_SUCCESS }}</strong>
      </div>
      <div class="stitch-card flex flex-col justify-center p-4 bg-white border-l-4 border-l-red-500" :class="{'opacity-75 grayscale': statusCount.FAILED === 0}">
        <span class="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">失败 (FAILED)</span>
        <strong class="text-3xl font-bold" :class="statusCount.FAILED > 0 ? 'text-red-600' : 'text-slate-400'">{{ statusCount.FAILED }}</strong>
      </div>
    </div>

    <div class="stitch-card-table">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="task_key"
        empty-text="暂无任务"
        class="stitch-table"
      >
        <el-table-column label="阶段" width="170">
          <template #default="{ row }">
            <div class="stage-cell">
              <el-tag :type="categoryTagType(row.task_category)" effect="plain" class="stitch-tag">
                {{ row.stage_label || categoryLabel(row.task_category) }}
              </el-tag>
              <span class="task-key">{{ row.task_key }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文档" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="doc-cell">
              <span class="doc-title">{{ row.document_title || '-' }}</span>
              <span class="doc-meta">doc {{ row.doc_id || '-' }} / version {{ row.version_id || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain" class="stitch-tag">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }">
            <el-progress 
              :percentage="progress(row)" 
              :status="progressStatus(row.status)" 
              :striped="row.status === 'RUNNING'" 
              :striped-flow="row.status === 'RUNNING'" 
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column label="模型 / 解析详情" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="target-cell">
              <span>{{ modelLabel(row) }}</span>
              <small>{{ indexLabel(row) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="重试" width="90">
          <template #default="{ row }">
            {{ row.retry_count || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.updated_at || row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button
                v-if="row.runnable"
                link
                type="primary"
                :icon="VideoPlay"
                :loading="operatingKey === row.task_key"
                @click="handleRun(row)"
              >
                执行
              </el-button>
              <el-button
                v-if="row.retryable"
                link
                type="warning"
                :icon="RefreshRight"
                :loading="operatingKey === row.task_key"
                @click="handleRetry(row)"
              >
                重试
              </el-button>
              <el-button
                v-if="row.error_message || (row.metadata && row.metadata.errors && row.metadata.errors.length > 0)"
                link
                type="danger"
                :icon="WarningFilled"
                @click="viewError(row)"
              >
                错误
              </el-button>
              <span v-if="!row.runnable && !row.retryable && !row.error_message && (!row.metadata || !row.metadata.errors || row.metadata.errors.length === 0)" class="muted-action">-</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="errorDialogVisible" title="任务执行错误" width="640px" class="stitch-dialog">
      <div v-if="currentErrorTask" class="flex flex-col gap-4">
        <el-alert
          type="error"
          show-icon
          :closable="false"
          class="border border-red-200"
        >
          <template #title>
            <span class="font-bold">阶段：{{ currentErrorTask.stage_label }}</span>
          </template>
          <p class="mt-1 m-0 text-sm opacity-90">任务 ID: {{ currentErrorTask.task_key }}</p>
        </el-alert>
        
        <div class="stitch-card !p-4 bg-slate-50 border-slate-200">
          <div class="flex items-center gap-2 mb-2">
            <span class="text-xs font-bold text-slate-500 uppercase tracking-wider">错误代码</span>
            <el-tag size="small" type="danger" effect="plain" class="font-mono bg-white">{{ currentErrorTask.error_code || 'PARTIAL_ERROR' }}</el-tag>
          </div>
          <div class="mt-4" v-if="currentErrorTask.error_message">
            <span class="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2 block">详细堆栈与日志</span>
            <div class="bg-slate-900 rounded-lg p-4 overflow-auto max-h-[200px]">
              <code class="text-red-400 font-mono text-sm leading-relaxed whitespace-pre-wrap break-all">{{ currentErrorTask.error_message }}</code>
            </div>
          </div>
          
          <div class="mt-4" v-if="currentErrorTask.metadata?.errors?.length">
            <span class="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2 block">页面解析错误 ({{ currentErrorTask.metadata.errors.length }})</span>
            <div class="bg-red-50 border border-red-100 rounded-lg max-h-[300px] overflow-auto">
              <div v-for="(err, idx) in currentErrorTask.metadata.errors" :key="idx" class="p-3 border-b border-red-100 last:border-0">
                <div class="flex gap-2 items-start mb-1">
                  <el-tag size="small" type="danger" effect="dark">页 {{ err.page_no ?? '未知' }}</el-tag>
                  <span class="text-sm font-semibold text-red-800">{{ err.error_type || 'Error' }}</span>
                </div>
                <div class="text-xs text-red-600 font-mono pl-1">{{ err.error_message || JSON.stringify(err) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="errorDialogVisible = false" plain class="stitch-btn">关闭</el-button>
        <el-button v-if="currentErrorTask?.retryable" type="warning" :icon="RefreshRight" @click="handleRetry(currentErrorTask); errorDialogVisible = false" class="stitch-btn">重新尝试</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Refresh, RefreshRight, VideoPlay, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { listTaskCenter, retryTask, runTask } from '@/api/tasks'
import type { EntityId, TaskCenterItem } from '@/api/types'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'TasksView',
})

const userStore = useUserStore()
const knowledgeStore = useKnowledgeStore()
const spaceId = ref<EntityId | null>(knowledgeStore.selectedSpaceId)
const taskCategory = ref('')
const status = ref('')
const rows = ref<TaskCenterItem[]>([])
const loading = ref(false)
const error = ref('')
const operatingKey = ref('')
const errorDialogVisible = ref(false)
const currentErrorTask = ref<TaskCenterItem | null>(null)
let pollingInterval: number | null = null

const statusCount = computed(() => ({
  PENDING: rows.value.filter((item) => item.status === 'PENDING').length,
  RUNNING: rows.value.filter((item) => item.status === 'RUNNING').length,
  PARTIAL_SUCCESS: rows.value.filter((item) => item.status === 'PARTIAL_SUCCESS').length,
  COMPLETED: rows.value.filter((item) => item.status === 'COMPLETED').length,
  FAILED: rows.value.filter((item) => item.status === 'FAILED').length,
}))

onMounted(async () => {
  if (userStore.tenantId) {
    await knowledgeStore.ensureSpaces(userStore.tenantId)
  }
  spaceId.value = knowledgeStore.selectedSpaceId || knowledgeStore.spaces[0]?.id || null
  if (spaceId.value) {
    await loadTasks()
  }
  
  pollingInterval = window.setInterval(async () => {
    if (statusCount.value.PENDING > 0 || statusCount.value.RUNNING > 0) {
      await loadTasks(true)
    }
  }, 3000)
})

onUnmounted(() => {
  if (pollingInterval) {
    window.clearInterval(pollingInterval)
  }
})

watch(spaceId, (value) => {
  knowledgeStore.selectSpace(value)
})

async function loadTasks(silent?: boolean | Event) {
  const isSilent = silent === true
  if (!spaceId.value) return
  if (!isSilent) loading.value = true
  error.value = ''
  try {
    rows.value = await listTaskCenter({
      spaceId: spaceId.value,
      status: status.value || undefined,
      taskCategory: taskCategory.value || undefined,
      limit: 100,
    })
  } catch (err) {
    if (!isSilent) error.value = apiErrorMessage(err)
  } finally {
    if (!isSilent) loading.value = false
  }
}

async function handleRun(row: TaskCenterItem) {
  await operate(row, async () => {
    await runTask(row.task_key)
    ElMessage.success('任务已执行')
  })
}

async function handleRetry(row: TaskCenterItem) {
  await operate(row, async () => {
    await retryTask(row.task_key)
    ElMessage.success('任务已重置为待处理')
  })
}

async function operate(row: TaskCenterItem, action: () => Promise<void>) {
  operatingKey.value = row.task_key
  error.value = ''
  try {
    await action()
    await loadTasks()
  } catch (err) {
    error.value = apiErrorMessage(err)
  } finally {
    operatingKey.value = ''
  }
}

function viewError(row: TaskCenterItem) {
  currentErrorTask.value = row
  errorDialogVisible.value = true
}

function progress(row: TaskCenterItem): number {
  return Math.min(Math.max(row.progress_percent ?? 0, 0), 100)
}

function statusTagType(value: string) {
  if (value === 'COMPLETED') return 'success'
  if (value === 'PARTIAL_SUCCESS') return 'warning'
  if (value === 'FAILED') return 'danger'
  if (value === 'RUNNING') return 'primary'
  if (value === 'PENDING') return 'warning'
  return 'info'
}

function progressStatus(value: string) {
  if (value === 'COMPLETED') return 'success'
  if (value === 'PARTIAL_SUCCESS') return 'warning'
  if (value === 'FAILED') return 'exception'
  return undefined
}

function categoryTagType(value: string) {
  return value === 'EMBEDDING_INDEX' ? 'primary' : 'info'
}

function categoryLabel(value: string) {
  if (value === 'PARSE_CHUNK') return '解析 / 切片'
  if (value === 'EMBEDDING_INDEX') return 'Embedding / 索引'
  return value || '-'
}

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    PENDING: '待处理',
    RUNNING: '运行中',
    PARTIAL_SUCCESS: '部分成功',
    COMPLETED: '已完成',
    FAILED: '失败',
  }
  return labels[value] || value || '-'
}

function modelLabel(row: TaskCenterItem) {
  if (row.task_category === 'PARSE_CHUNK') {
    if (row.metadata?.parser) {
      return `解析引擎: ${row.metadata.parser}`
    }
    return row.worker_id ? `Worker: ${row.worker_id}` : '解析与切片流水线'
  }
  const model = [row.model_provider, row.model_name].filter(Boolean).join(' / ')
  return model || '等待模型执行'
}

function indexLabel(row: TaskCenterItem) {
  if (row.task_category === 'PARSE_CHUNK') {
    if (row.metadata) {
      const parts = []
      if (row.metadata.page_count != null) parts.push(`共 ${row.metadata.page_count} 页`)
      if (row.metadata.block_count != null) parts.push(`生成 ${row.metadata.block_count} 个Block`)
      if (row.metadata.error_count != null && row.metadata.error_count > 0) parts.push(`${row.metadata.error_count}页失败`)
      return parts.join(' | ') || (row.finished_at ? `完成于 ${formatTime(row.finished_at)}` : '-')
    }
    return row.finished_at ? `完成于 ${formatTime(row.finished_at)}` : '生成 chunk 后创建索引任务'
  }
  const parts = []
  if (row.embedding_dimension) parts.push(`${row.embedding_dimension} 维`)
  if (row.index_name) parts.push(row.index_name)
  if (row.vector_collection) parts.push(row.vector_collection)
  if (row.chunk_id) parts.push(`chunk ${row.chunk_id}`)
  return parts.join(' / ') || '关键词索引 + 向量索引'
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}
</script>

<style scoped>
.page-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
}

.header-title h2 {
  font-size: 26px;
  font-weight: 700;
  color: #191b24;
  margin: 0 0 8px 0;
  letter-spacing: 0;
}

.header-title p {
  font-size: 14px;
  color: #596070;
  margin: 0;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.space-select {
  width: 220px;
}

.filter-select {
  width: 150px;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #e3e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 12px;
  color: #6b7280;
}

.summary-item strong {
  font-size: 22px;
  line-height: 1;
  color: #1f2937;
}

.danger-number {
  color: #c2410c !important;
}

.stage-cell,
.doc-cell,
.target-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.task-key,
.doc-meta,
.target-cell small {
  font-size: 12px;
  color: #7b8190;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-title,
.target-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 28px;
}

.muted-action {
  color: #9ca3af;
}

.text-danger {
  color: #c2410c;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 960px) {
  .section-header {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .space-select,
  .filter-select {
    width: 100%;
  }

  .summary-strip {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
