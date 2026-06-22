<template>
  <div class="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[500px] text-white">
    <!-- Outer Banner title -->
    <div class="text-center pb-4 border-b border-white/[0.08] mb-4 shrink-0">
      <h1 class="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase m-0">
        Task Monitor Console
      </h1>
    </div>

    <!-- Toolbar -->
    <div class="cyber-panel rounded-2xl p-4 mb-5 border border-white/[0.06] flex flex-col md:flex-row md:items-center justify-between gap-4 shrink-0">
      <div class="flex flex-col gap-1">
        <h2 class="text-lg font-bold text-white m-0 tracking-tight">System Tasks</h2>
        <p class="text-[10px] font-mono text-slate-500 uppercase tracking-widest m-0">Pipeline execution tracing</p>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <!-- space select -->
        <div class="relative w-full md:w-48">
           <select
             v-model="spaceId"
             @change="loadTasks"
             class="w-full bg-slate-950 px-3 py-2 rounded-lg border border-white/[0.08] text-xs font-mono text-neon-cyan focus:outline-none pr-8 appearance-none cursor-pointer"
           >
             <option v-for="space in knowledgeStore.spaces" :key="space.id" :value="space.id">
               {{ space.name }}
             </option>
           </select>
           <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-[14px] text-neon-cyan pointer-events-none">expand_more</span>
        </div>

        <div class="relative w-full md:w-36">
           <select
             v-model="taskCategory"
             @change="loadTasks"
             class="w-full bg-slate-950 px-3 py-2 rounded-lg border border-white/[0.08] text-xs font-mono text-slate-300 focus:outline-none pr-8 appearance-none cursor-pointer"
           >
             <option value="">All Categories</option>
             <option value="PARSE_CHUNK">Parse / Chunk</option>
             <option value="EMBEDDING_INDEX">Embedding / Index</option>
           </select>
           <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-[14px] text-slate-500 pointer-events-none">expand_more</span>
        </div>

        <div class="relative w-full md:w-36">
           <select
             v-model="status"
             @change="loadTasks"
             class="w-full bg-slate-950 px-3 py-2 rounded-lg border border-white/[0.08] text-xs font-mono text-slate-300 focus:outline-none pr-8 appearance-none cursor-pointer"
           >
             <option value="">All Statuses</option>
             <option value="PENDING">Pending</option>
             <option value="RUNNING">Running</option>
             <option value="PARTIAL_SUCCESS">Partial Success</option>
             <option value="FAILED">Failed</option>
           </select>
           <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-[14px] text-slate-500 pointer-events-none">expand_more</span>
        </div>

        <button @click="loadTasks" :disabled="loading" class="py-2 px-3 rounded-lg border border-white/[0.08] hover:border-neon-cyan hover:text-neon-cyan text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50">
          <span class="material-symbols-outlined text-[14px]" :class="{ 'animate-spin': loading }">refresh</span>
          <span>Refresh</span>
        </button>
      </div>
    </div>

    <!-- Alert -->
    <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-950/40 border border-red-500/30 text-red-400 text-xs font-mono flex items-center gap-2">
      <span class="material-symbols-outlined text-[16px]">error</span>
      {{ error }}
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-5 shrink-0">
      <div class="cyber-panel rounded-xl p-4 border border-white/[0.05] border-l-2 border-l-slate-500 flex flex-col justify-center relative overflow-hidden">
        <div class="absolute right-[-10px] top-[-10px] text-slate-800 opacity-50 text-[64px] font-black material-symbols-outlined">data_usage</div>
        <span class="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest mb-1 relative z-10">Total Tasks</span>
        <strong class="text-3xl font-bold text-white relative z-10">{{ rows.length }}</strong>
      </div>
      <div class="cyber-panel rounded-xl p-4 border border-white/[0.05] border-l-2 border-l-amber-500 flex flex-col justify-center relative overflow-hidden">
        <div class="absolute right-[-10px] top-[-10px] text-amber-900/20 text-[64px] font-black material-symbols-outlined">pending_actions</div>
        <span class="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest mb-1 relative z-10">Pending</span>
        <strong class="text-3xl font-bold text-amber-400 relative z-10 shadow-[0_0_10px_rgba(251,191,36,0.3)]">{{ statusCount.PENDING }}</strong>
      </div>
      <div class="cyber-panel rounded-xl p-4 border border-white/[0.05] border-l-2 border-l-blue-500 flex flex-col justify-center relative overflow-hidden">
        <div class="absolute right-[-10px] top-[-10px] text-blue-900/20 text-[64px] font-black material-symbols-outlined">model_training</div>
        <span class="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest mb-1 relative z-10">Running</span>
        <strong class="text-3xl font-bold text-blue-400 relative z-10 shadow-[0_0_10px_rgba(96,165,250,0.3)]">{{ statusCount.RUNNING }}</strong>
      </div>
      <div class="cyber-panel rounded-xl p-4 border border-white/[0.05] border-l-2 border-l-orange-500 flex flex-col justify-center relative overflow-hidden" :class="{'opacity-60 grayscale': statusCount.PARTIAL_SUCCESS === 0}">
        <div class="absolute right-[-10px] top-[-10px] text-orange-900/20 text-[64px] font-black material-symbols-outlined">warning</div>
        <span class="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest mb-1 relative z-10">Partial Success</span>
        <strong class="text-3xl font-bold text-orange-400 relative z-10">{{ statusCount.PARTIAL_SUCCESS }}</strong>
      </div>
      <div class="cyber-panel rounded-xl p-4 border border-white/[0.05] border-l-2 border-l-red-500 flex flex-col justify-center relative overflow-hidden" :class="{'opacity-60 grayscale': statusCount.FAILED === 0}">
        <div class="absolute right-[-10px] top-[-10px] text-red-900/20 text-[64px] font-black material-symbols-outlined">error</div>
        <span class="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest mb-1 relative z-10">Failed</span>
        <strong class="text-3xl font-bold relative z-10" :class="statusCount.FAILED > 0 ? 'text-red-500 shadow-[0_0_10px_rgba(239,68,68,0.5)]' : 'text-slate-500'">{{ statusCount.FAILED }}</strong>
      </div>
    </div>

    <!-- Table -->
    <div class="cyber-panel rounded-2xl p-4 border border-white/[0.06] flex-1 overflow-hidden flex flex-col min-h-0">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="task_key"
        empty-text="No active tasks"
        class="flex-1 w-full"
        height="100%"
      >
        <el-table-column label="Stage" width="180">
          <template #default="{ row }">
            <div class="flex flex-col gap-1">
              <span class="text-[10px] font-mono uppercase px-1.5 py-0.5 rounded border inline-block w-fit" 
                    :class="row.task_category === 'EMBEDDING_INDEX' ? 'bg-cyan-950/30 border-neon-cyan/50 text-neon-cyan' : 'bg-slate-800 border-slate-600 text-slate-300'">
                {{ row.stage_label || categoryLabel(row.task_category) }}
              </span>
              <span class="text-[9px] font-mono text-slate-500 truncate" :title="row.task_key">{{ row.task_key }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Document" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex flex-col gap-1">
              <span class="text-xs font-semibold text-slate-200 truncate">{{ row.document_title || '-' }}</span>
              <span class="text-[10px] font-mono text-slate-500">ID: {{ row.doc_id || '-' }} | V: {{ row.version_id || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Status" width="130">
          <template #default="{ row }">
            <span class="text-[10px] font-mono font-bold px-2 py-1 rounded border uppercase" :class="statusClass(row.status)">
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Progress" width="160">
          <template #default="{ row }">
            <el-progress 
              :percentage="progress(row)" 
              :status="progressStatus(row.status)" 
              :striped="row.status === 'RUNNING'" 
              :striped-flow="row.status === 'RUNNING'" 
              :stroke-width="6"
              :color="progressColor(row.status)"
            />
          </template>
        </el-table-column>
        <el-table-column label="Compute Target" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex flex-col gap-0.5">
              <span class="text-xs text-slate-300">{{ modelLabel(row) }}</span>
              <span class="text-[10px] font-mono text-slate-500">{{ indexLabel(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Retries" width="80" align="center">
          <template #default="{ row }">
            <span class="text-xs font-mono text-slate-400">{{ row.retry_count || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Updated At" width="160">
          <template #default="{ row }">
             <span class="text-[10px] font-mono text-slate-400">{{ formatTime(row.updated_at || row.created_at) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="160" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <button
                v-if="row.runnable"
                class="px-2 py-1 bg-cyan-950/30 border border-neon-cyan/50 text-neon-cyan hover:bg-neon-cyan hover:text-slate-900 rounded text-[10px] font-mono uppercase transition-colors disabled:opacity-50 flex items-center gap-1 cursor-pointer outline-none"
                :disabled="operatingKey === row.task_key"
                @click="handleRun(row)"
              >
                <span class="material-symbols-outlined text-[12px]" :class="{ 'animate-spin': operatingKey === row.task_key }">{{ operatingKey === row.task_key ? 'refresh' : 'play_arrow' }}</span>
                Exec
              </button>
              <button
                v-if="row.retryable"
                class="px-2 py-1 bg-amber-950/30 border border-amber-500/50 text-amber-500 hover:bg-amber-500 hover:text-slate-900 rounded text-[10px] font-mono uppercase transition-colors disabled:opacity-50 flex items-center gap-1 cursor-pointer outline-none"
                :disabled="operatingKey === row.task_key"
                @click="handleRetry(row)"
              >
                <span class="material-symbols-outlined text-[12px]" :class="{ 'animate-spin': operatingKey === row.task_key }">refresh</span>
                Retry
              </button>
              <button
                v-if="row.error_message || (row.metadata && row.metadata.errors && row.metadata.errors.length > 0)"
                class="px-2 py-1 bg-red-950/30 border border-red-500/50 text-red-500 hover:bg-red-500 hover:text-white rounded text-[10px] font-mono uppercase transition-colors flex items-center gap-1 cursor-pointer outline-none"
                @click="viewError(row)"
              >
                <span class="material-symbols-outlined text-[12px]">warning</span>
                Err
              </button>
              <span v-if="!row.runnable && !row.retryable && !row.error_message && (!row.metadata || !row.metadata.errors || row.metadata.errors.length === 0)" class="text-slate-600 text-xs">-</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Error Drawer (replaces dialog for cyber feel) -->
    <el-drawer v-model="errorDialogVisible" title="Task Fault Trace" size="640px" class="cyber-drawer">
      <div v-if="currentErrorTask" class="flex flex-col gap-4">
        
        <div class="p-3 bg-red-950/20 border border-red-500/30 rounded-lg flex flex-col gap-1">
          <div class="flex items-center gap-2 text-red-400">
            <span class="material-symbols-outlined text-lg">dangerous</span>
            <span class="font-bold font-mono text-sm uppercase">Stage: {{ currentErrorTask.stage_label || categoryLabel(currentErrorTask.task_category) }}</span>
          </div>
          <p class="m-0 text-xs font-mono text-slate-400 ml-7">ID: {{ currentErrorTask.task_key }}</p>
        </div>
        
        <div class="cyber-panel rounded-xl p-4 border border-white/[0.06]">
          <div class="flex items-center gap-2 mb-4">
            <span class="text-[10px] font-mono text-slate-500 uppercase tracking-wider font-bold">Error Code</span>
            <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-red-950/40 border border-red-500 text-red-400 shadow-[0_0_8px_rgba(239,68,68,0.2)]">
              {{ currentErrorTask.error_code || 'PARTIAL_ERROR' }}
            </span>
          </div>
          
          <div v-if="currentErrorTask.error_message" class="mb-4">
            <span class="text-[10px] font-mono text-slate-500 uppercase tracking-wider font-bold mb-2 block">Stack Trace / Log</span>
            <div class="bg-slate-950 rounded-lg p-3 border border-white/[0.05] overflow-auto max-h-[300px] scrollbar-thin">
              <code class="text-red-400 font-mono text-[11px] leading-relaxed whitespace-pre-wrap break-all">{{ currentErrorTask.error_message }}</code>
            </div>
          </div>
          
          <div v-if="currentErrorTask.metadata?.errors?.length">
            <span class="text-[10px] font-mono text-slate-500 uppercase tracking-wider font-bold mb-2 block">Parse Errors ({{ currentErrorTask.metadata.errors.length }})</span>
            <div class="bg-slate-950 rounded-lg border border-white/[0.05] max-h-[300px] overflow-auto scrollbar-thin flex flex-col">
              <div v-for="(err, idx) in currentErrorTask.metadata.errors" :key="idx" class="p-3 border-b border-white/[0.05] last:border-0 flex flex-col gap-1">
                <div class="flex items-center gap-2">
                  <span class="text-[10px] font-mono bg-red-950/50 text-red-400 px-1 py-0.5 rounded border border-red-900">Page {{ err.page_no ?? '?' }}</span>
                  <span class="text-xs font-mono text-slate-300 font-bold">{{ err.error_type || 'Unknown Fault' }}</span>
                </div>
                <div class="text-[10px] text-red-400/80 font-mono mt-1">{{ err.error_message || JSON.stringify(err) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <button @click="errorDialogVisible = false" class="px-4 py-2 rounded bg-slate-800 text-white text-xs font-mono border border-slate-700 hover:bg-slate-700 transition-colors cursor-pointer">
            Close
          </button>
          <button v-if="currentErrorTask?.retryable" @click="handleRetry(currentErrorTask); errorDialogVisible = false" class="px-4 py-2 rounded bg-amber-950/30 text-amber-500 border border-amber-500/50 hover:bg-amber-500 hover:text-slate-900 transition-colors text-xs font-mono flex items-center gap-1 cursor-pointer">
            <span class="material-symbols-outlined text-[14px]">refresh</span>
            Force Retry
          </button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
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
    ElMessage.success('Task execution requested')
  })
}

async function handleRetry(row: TaskCenterItem) {
  await operate(row, async () => {
    await retryTask(row.task_key)
    ElMessage.success('Task queued for retry')
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

function statusClass(value: string) {
  if (value === 'COMPLETED') return 'bg-green-950/40 text-green-400 border-green-500/40'
  if (value === 'PARTIAL_SUCCESS') return 'bg-orange-950/40 text-orange-400 border-orange-500/40'
  if (value === 'FAILED') return 'bg-red-950/40 text-red-400 border-red-500/40'
  if (value === 'RUNNING') return 'bg-blue-950/40 text-blue-400 border-blue-500/40 animate-pulse'
  if (value === 'PENDING') return 'bg-amber-950/40 text-amber-400 border-amber-500/40'
  return 'bg-slate-800 text-slate-300 border-slate-600'
}

function progressStatus(value: string) {
  if (value === 'COMPLETED') return 'success'
  if (value === 'PARTIAL_SUCCESS') return 'warning'
  if (value === 'FAILED') return 'exception'
  return undefined
}

function progressColor(value: string) {
  if (value === 'COMPLETED') return '#22c55e'
  if (value === 'PARTIAL_SUCCESS') return '#f97316'
  if (value === 'FAILED') return '#ef4444'
  if (value === 'RUNNING') return '#3b82f6'
  if (value === 'PENDING') return '#f59e0b'
  return '#94a3b8'
}

function categoryLabel(value: string) {
  if (value === 'PARSE_CHUNK') return 'Parse / Chunk'
  if (value === 'EMBEDDING_INDEX') return 'Embed / Index'
  return value || '-'
}

function modelLabel(row: TaskCenterItem) {
  if (row.task_category === 'PARSE_CHUNK') {
    if (row.metadata?.parser) {
      return `Engine: ${row.metadata.parser}`
    }
    return row.worker_id ? `Worker: ${row.worker_id}` : 'Parse & Chunk Pipeline'
  }
  const model = [row.model_provider, row.model_name].filter(Boolean).join(' / ')
  return model || 'Awaiting model selection'
}

function indexLabel(row: TaskCenterItem) {
  if (row.task_category === 'PARSE_CHUNK') {
    if (row.metadata) {
      const parts = []
      if (row.metadata.page_count != null) parts.push(`${row.metadata.page_count} pages`)
      if (row.metadata.block_count != null) parts.push(`${row.metadata.block_count} blocks`)
      if (row.metadata.error_count != null && row.metadata.error_count > 0) parts.push(`${row.metadata.error_count} fails`)
      return parts.join(' | ') || (row.finished_at ? `Finished: ${formatTime(row.finished_at)}` : '-')
    }
    return row.finished_at ? `Finished: ${formatTime(row.finished_at)}` : 'Generating chunks...'
  }
  const parts = []
  if (row.embedding_dimension) parts.push(`${row.embedding_dimension}d`)
  if (row.index_name) parts.push(row.index_name)
  if (row.vector_collection) parts.push(row.vector_collection)
  if (row.chunk_id) parts.push(`chunk ${row.chunk_id}`)
  return parts.join(' / ') || 'Vector + Keyword Indexing'
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}
</script>

<style>
/* Custom scrollbar for cyber components */
.scrollbar-thin::-webkit-scrollbar {
  width: 4px;
}
.scrollbar-thin::-webkit-scrollbar-track {
  background: transparent;
}
.scrollbar-thin::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}
.scrollbar-thin:hover::-webkit-scrollbar-thumb {
  background-color: rgba(0, 240, 255, 0.3);
}

/* Drawer override */
.cyber-drawer {
  background: #020617 !important;
  border-left: 1px solid rgba(255,255,255,0.1) !important;
}
.cyber-drawer .el-drawer__header {
  margin-bottom: 0;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  color: #fff;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 700;
}
.cyber-drawer .el-drawer__body {
  padding: 1.5rem;
}

/* El Progress overrides for dark theme */
.el-progress__text {
  color: #fff !important;
  font-family: ui-monospace, SFMono-Regular, monospace;
  font-size: 10px !important;
}
.el-progress-bar__outer {
  background-color: rgba(255,255,255,0.1) !important;
}
</style>
