<template>
  <div class="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[500px] text-white">
    <!-- Outer Banner title -->
    <div class="text-center pb-4 border-b border-white/[0.08] mb-4 shrink-0">
      <h1 class="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase m-0">
        Data Matrix Interface
      </h1>
    </div>

    <!-- Toolbar -->
    <div class="cyber-panel rounded-2xl p-4 mb-5 border border-white/[0.06] flex flex-col md:flex-row md:items-center justify-between gap-4 shrink-0">
      <div class="flex flex-col gap-1">
        <h2 class="text-lg font-bold text-white m-0 tracking-tight">Enclave Databanks</h2>
        <p class="text-[10px] font-mono text-slate-500 uppercase tracking-widest m-0 flex items-center gap-1">
          <span class="material-symbols-outlined text-[12px]">folder_open</span>
          NEXUS: {{ selectedSpace?.name || '-' }}
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <div class="relative w-full md:w-64">
           <select
             v-model="currentSpaceId"
             class="w-full bg-slate-950 px-3 py-2 rounded-lg border border-white/[0.08] text-xs font-mono text-neon-cyan focus:outline-none pr-8 appearance-none cursor-pointer"
           >
             <option v-for="space in knowledgeStore.spaces" :key="space.id" :value="space.id">
               {{ space.name }}
             </option>
           </select>
           <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-[14px] text-neon-cyan pointer-events-none">expand_more</span>
        </div>
        <button @click="loadDocuments()" :disabled="loading" class="py-2 px-3 rounded-lg border border-white/[0.08] hover:border-neon-cyan hover:text-neon-cyan text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50 outline-none">
          <span class="material-symbols-outlined text-[14px]" :class="{ 'animate-spin': loading }">refresh</span>
          <span>Sync</span>
        </button>
        <button @click="router.push('/documents/upload')" class="py-2 px-4 rounded-lg bg-neon-cyan text-slate-900 border border-neon-cyan hover:bg-cyan-400 hover:shadow-[0_0_15px_rgba(0,240,255,0.4)] text-xs font-mono font-bold transition-all flex items-center gap-1.5 cursor-pointer uppercase tracking-wider outline-none">
          <span class="material-symbols-outlined text-[14px]">upload</span>
          Inject Data
        </button>
      </div>
    </div>

    <!-- Alert -->
    <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-950/40 border border-red-500/30 text-red-400 text-xs font-mono flex items-center gap-2 shrink-0">
      <span class="material-symbols-outlined text-[16px]">error</span>
      {{ error }}
    </div>

    <!-- Table -->
    <div class="cyber-panel rounded-2xl p-4 border border-white/[0.06] flex-1 overflow-hidden flex flex-col min-h-0">
      <el-table
        v-loading="loading"
        :data="documents"
        row-key="id"
        empty-text="No data packets found in this nexus."
        class="flex-1 w-full"
        height="100%"
      >
        <el-table-column prop="title" label="Document Alias" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-[20px]" :class="row.docType === 'pdf' ? 'text-red-400' : 'text-neon-cyan'">
                {{ row.docType === 'pdf' ? 'picture_as_pdf' : 'description' }}
              </span>
              <span class="font-medium text-slate-200">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="docType" label="Format" width="100">
          <template #default="{ row }">
            <span class="px-2 py-0.5 bg-slate-900 border border-white/[0.1] text-slate-400 rounded text-[9px] font-mono uppercase tracking-wider">{{ row.docType || 'TXT' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="industry" label="Sector" width="130">
           <template #default="{ row }">
             <span class="text-[10px] font-mono text-slate-400">{{ row.industry || '-' }}</span>
           </template>
        </el-table-column>
        <el-table-column prop="serviceLine" label="Vector" width="150">
           <template #default="{ row }">
             <span class="text-[10px] font-mono text-slate-400">{{ row.serviceLine || '-' }}</span>
           </template>
        </el-table-column>
        <el-table-column prop="confidentialLevel" label="Security" width="100">
          <template #default="{ row }">
             <span class="text-[10px] font-mono uppercase tracking-wider font-bold" :class="row.confidentialLevel === 'HIGH' ? 'text-red-400' : 'text-slate-500'">{{ row.confidentialLevel || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Integrity" width="130">
          <template #default="{ row }">
            <span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded border text-[9px] font-mono uppercase font-bold tracking-wider"
                  :class="row.status === 'ACTIVE' ? 'bg-green-950/30 text-green-400 border-green-500/50' : 'bg-slate-800 text-slate-400 border-slate-600'">
              <span class="w-1.5 h-1.5 rounded-full" :class="row.status === 'ACTIVE' ? 'bg-green-400 shadow-[0_0_5px_rgba(74,222,128,0.8)]' : 'bg-slate-500'"></span>
              {{ row.status === 'ACTIVE' ? 'ONLINE' : row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="220" fixed="right" align="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-2">
              <button class="px-2 py-1 bg-slate-800 border border-slate-600 text-slate-300 hover:bg-slate-700 hover:text-white rounded text-[10px] font-mono uppercase transition-colors outline-none cursor-pointer" 
                      @click="openDetail(row.id)">
                INSPECT
              </button>
              <button class="px-2 py-1 bg-cyan-950/30 border border-neon-cyan/50 text-neon-cyan hover:bg-neon-cyan hover:text-slate-900 rounded text-[10px] font-mono uppercase transition-colors outline-none cursor-pointer" 
                      @click="openParseStatus(row.id)">
                TRACE
              </button>
              <button class="px-2 py-1 bg-red-950/30 border border-red-500/50 text-red-500 hover:bg-red-500 hover:text-white rounded text-[10px] font-mono uppercase transition-colors outline-none cursor-pointer" 
                      @click="removeDocument(row.id)">
                PURGE
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Details Drawer -->
    <el-drawer v-model="detailVisible" title="Packet Inspection" size="560px" class="cyber-drawer">
      <div v-if="detailLoading" class="p-4">
        <div class="animate-pulse flex flex-col gap-4">
          <div class="h-8 bg-slate-800 rounded w-full"></div>
          <div class="h-8 bg-slate-800 rounded w-full"></div>
          <div class="h-8 bg-slate-800 rounded w-full"></div>
        </div>
      </div>
      <template v-else-if="detail">
        <div class="cyber-panel rounded-xl p-4 border border-white/[0.06] mb-4">
          <div class="grid grid-cols-2 gap-4">
            <div class="flex flex-col gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Alias</span>
              <span class="text-xs font-mono text-slate-200 break-all">{{ detail.title }}</span>
            </div>
            <div class="flex flex-col gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Format</span>
              <span class="text-xs font-mono text-slate-400 uppercase">{{ detail.docType || '-' }}</span>
            </div>
            <div class="flex flex-col gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Sector</span>
              <span class="text-xs font-mono text-slate-400">{{ detail.industry || '-' }}</span>
            </div>
            <div class="flex flex-col gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Vector</span>
              <span class="text-xs font-mono text-slate-400">{{ detail.serviceLine || '-' }}</span>
            </div>
            <div class="flex flex-col gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Security Clearance</span>
              <span class="text-xs font-mono font-bold" :class="detail.confidentialLevel === 'HIGH' ? 'text-red-400' : 'text-slate-400'">{{ detail.confidentialLevel || '-' }}</span>
            </div>
            <div class="flex flex-col gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Source URI</span>
              <span class="text-[10px] font-mono text-neon-cyan truncate" :title="detail.sourceUri">{{ detail.sourceUri || '-' }}</span>
            </div>
            <div class="flex flex-col gap-1 col-span-2">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Hash Signature</span>
              <span class="text-[10px] font-mono text-slate-500 break-all bg-slate-900 p-1 rounded">{{ detail.fileHash || '-' }}</span>
            </div>
          </div>
          
          <div class="mt-4 pt-4 border-t border-white/[0.05] grid grid-cols-2 gap-4">
             <div class="flex flex-col gap-1">
                <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Parse Integrity</span>
                <div class="flex items-center gap-2">
                  <span class="px-2 py-0.5 rounded text-[9px] font-mono uppercase font-bold border" 
                        :class="detail.currentVersion?.parseStatus === 'FAILED' ? 'bg-red-950/30 text-red-400 border-red-500/50' : (detail.currentVersion?.parseStatus === 'PARTIAL_SUCCESS' ? 'bg-orange-950/30 text-orange-400 border-orange-500/50' : 'bg-slate-800 text-slate-400 border-slate-600')">
                    {{ detail.currentVersion?.parseStatus || '-' }}
                  </span>
                  <button v-if="detail.currentVersion?.parseStatus === 'FAILED' || detail.currentVersion?.parseStatus === 'PARTIAL_SUCCESS'" 
                          @click="retryParse(detail.id)"
                          class="px-2 py-0.5 rounded bg-amber-950/30 text-amber-500 border border-amber-500/50 hover:bg-amber-500 hover:text-slate-900 transition-colors text-[9px] font-mono flex items-center gap-1 cursor-pointer outline-none uppercase">
                    <span class="material-symbols-outlined text-[10px]">refresh</span> Retry
                  </button>
                </div>
             </div>
             <div class="flex flex-col gap-1" v-if="detail.currentVersion?.parseStatus === 'FAILED' || detail.currentVersion?.parseStatus === 'PARTIAL_SUCCESS'">
               <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Fault Reason</span>
               <span class="text-[10px] font-mono text-red-400" v-if="parseStatus?.errorMessage">{{ parseStatus.errorMessage }}</span>
               <span class="text-[10px] font-mono text-orange-400" v-else-if="parseStatus?.metadata?.errors?.length">Partial page failure ({{ parseStatus.metadata.error_count || parseStatus.metadata.errors.length }} pgs)</span>
               <span class="text-[10px] font-mono text-slate-500" v-else>Unknown anomalies</span>
             </div>
             <div class="flex flex-col gap-1">
               <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Desensitize</span>
               <span class="text-xs font-mono text-slate-400">{{ detail.currentVersion?.desensitizeStatus || '-' }}</span>
             </div>
             <div class="flex flex-col gap-1">
               <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Data Chunks</span>
               <span class="text-xs font-mono text-neon-cyan font-bold">{{ detail.currentVersion?.chunkCount ?? '-' }}</span>
             </div>
          </div>
        </div>

        <div class="mt-4">
          <div class="flex items-center gap-2 mb-3 border-b border-white/[0.05] pb-2">
             <span class="material-symbols-outlined text-neon-cyan text-[16px]">data_array</span>
             <h4 class="m-0 text-xs font-mono uppercase tracking-widest text-slate-300 font-bold">Fragment Preview</h4>
             <span class="px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 text-[9px] font-mono ml-2 border border-slate-700">{{ chunks.length }} items</span>
          </div>
          
          <div class="overflow-auto max-h-[300px] scrollbar-thin pr-2 flex flex-col gap-3">
            <div v-if="chunks.length === 0" class="text-slate-500 text-xs font-mono py-4 text-center border border-dashed border-white/[0.1] rounded-lg bg-slate-900/30">
              No fragments allocated.
            </div>
            <div v-for="(chunk, idx) in chunks" :key="idx" class="bg-slate-900 rounded-lg p-3 border border-white/[0.05] hover:border-white/[0.1] transition-colors relative group">
              <div class="flex justify-between items-center mb-2 pb-2 border-b border-white/[0.05]">
                <span class="text-[10px] font-mono text-neon-cyan font-bold bg-cyan-950/30 px-1.5 py-0.5 rounded border border-neon-cyan/20">SEQ: {{ chunk.chunkIndex ?? idx }}</span>
                <span class="text-[9px] font-mono text-slate-500" v-if="chunk.pageNo">PG: {{ chunk.pageNo }}</span>
              </div>
              <div class="text-[11px] font-mono text-slate-300 leading-relaxed whitespace-pre-wrap break-all">{{ chunk.content }}</div>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="flex flex-col items-center justify-center h-40 text-slate-500 border border-dashed border-white/[0.1] rounded-xl bg-slate-950/30">
        <span class="material-symbols-outlined text-4xl mb-2 opacity-50">search_off</span>
        <span class="font-mono text-xs uppercase tracking-wider">No packet selected</span>
      </div>
    </el-drawer>

    <!-- Status Dialog -->
    <el-dialog v-model="statusVisible" title="Telemetry Trace" width="520px" class="cyber-dialog" :show-close="false">
      <template #header="{ titleId, titleClass }">
         <div class="flex items-center justify-between border-b border-white/[0.08] pb-3 mb-2">
            <h4 :id="titleId" :class="titleClass" class="!m-0 !text-sm font-mono uppercase tracking-widest text-white flex items-center gap-2">
              <span class="material-symbols-outlined text-neon-cyan text-[18px]">query_stats</span>
              Telemetry Trace
            </h4>
            <button @click="statusVisible = false" class="text-slate-500 hover:text-white cursor-pointer outline-none">
              <span class="material-symbols-outlined text-[18px]">close</span>
            </button>
         </div>
      </template>
      
      <div v-if="statusLoading" class="p-4 animate-pulse flex flex-col gap-3">
        <div class="h-6 bg-slate-800 rounded w-full"></div>
        <div class="h-6 bg-slate-800 rounded w-3/4"></div>
      </div>
      <div v-else-if="parseStatus" class="flex flex-col gap-4">
        
        <div class="cyber-panel rounded-xl p-4 border border-white/[0.06] grid grid-cols-2 gap-4">
           <div class="flex flex-col gap-1 col-span-2">
             <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Task Identifier</span>
             <span class="text-xs font-mono text-slate-200">{{ parseStatus.parseTaskId || '-' }}</span>
           </div>
           <div class="flex flex-col gap-1">
             <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Phase</span>
             <span class="text-xs font-mono text-neon-cyan uppercase font-bold">{{ parseStatus.taskType || '-' }}</span>
           </div>
           <div class="flex flex-col gap-1">
             <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Core Status</span>
             <span class="px-2 py-0.5 rounded text-[9px] font-mono uppercase font-bold border w-fit" 
                   :class="parseStatus.status === 'FAILED' ? 'bg-red-950/30 text-red-400 border-red-500/50' : (parseStatus.status === 'PARTIAL_SUCCESS' ? 'bg-orange-950/30 text-orange-400 border-orange-500/50' : 'bg-slate-800 text-slate-400 border-slate-600')">
               {{ parseStatus.status || '-' }}
             </span>
           </div>
           <div class="flex flex-col gap-1">
             <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest font-bold">Parse Engine Status</span>
             <span class="px-2 py-0.5 rounded text-[9px] font-mono uppercase font-bold border w-fit" 
                   :class="parseStatus.parseStatus === 'FAILED' ? 'bg-red-950/30 text-red-400 border-red-500/50' : (parseStatus.parseStatus === 'PARTIAL_SUCCESS' ? 'bg-orange-950/30 text-orange-400 border-orange-500/50' : 'bg-slate-800 text-slate-400 border-slate-600')">
               {{ parseStatus.parseStatus || '-' }}
             </span>
           </div>
        </div>

        <div class="cyber-panel rounded-xl p-4 border border-white/[0.06]" v-if="parseStatus.metadata">
          <span class="text-[10px] font-mono text-slate-500 uppercase tracking-widest font-bold mb-3 block border-b border-white/[0.05] pb-2">Parse Telemetry</span>
          <div class="grid grid-cols-2 gap-3 text-xs font-mono text-slate-300">
            <div class="flex items-center justify-between bg-slate-900/50 px-2 py-1 rounded" v-if="parseStatus.metadata.parser">
               <span class="text-slate-500">Engine</span><span>{{ parseStatus.metadata.parser }}</span>
            </div>
            <div class="flex items-center justify-between bg-slate-900/50 px-2 py-1 rounded" v-if="parseStatus.metadata.page_count != null">
               <span class="text-slate-500">Pages</span><span>{{ parseStatus.metadata.page_count }}</span>
            </div>
            <div class="flex items-center justify-between bg-slate-900/50 px-2 py-1 rounded" v-if="parseStatus.metadata.block_count != null">
               <span class="text-slate-500">Blocks</span><span class="text-neon-cyan">{{ parseStatus.metadata.block_count }}</span>
            </div>
            <div class="flex items-center justify-between bg-red-950/30 border border-red-500/20 px-2 py-1 rounded" v-if="parseStatus.metadata.error_count != null && parseStatus.metadata.error_count > 0">
               <span class="text-red-500/80">Fails</span><span class="text-red-400 font-bold">{{ parseStatus.metadata.error_count }}</span>
            </div>
          </div>
        </div>

        <div class="cyber-panel rounded-xl p-4 border border-white/[0.06]">
          <span class="text-[10px] font-mono text-slate-500 uppercase tracking-widest font-bold mb-3 block">Progress / Completion</span>
          <el-progress 
            :percentage="parseStatus.progressPercent || 0" 
            :status="parseStatus.status === 'FAILED' ? 'exception' : (parseStatus.status === 'PARTIAL_SUCCESS' ? 'warning' : 'success')" 
            :stroke-width="8"
            :color="parseStatus.status === 'FAILED' ? '#ef4444' : (parseStatus.status === 'PARTIAL_SUCCESS' ? '#f97316' : '#00f0ff')"
          />
        </div>

        <div class="cyber-panel rounded-xl p-4 border border-red-500/30 bg-red-950/10" v-if="parseStatus.errorMessage || parseStatus.metadata?.errors?.length">
          <span class="text-[10px] font-mono text-red-500 uppercase tracking-widest font-bold mb-3 block flex items-center gap-1">
             <span class="material-symbols-outlined text-[14px]">warning</span> Anomaly Logs
          </span>
          <div v-if="parseStatus.errorMessage" class="text-red-400 text-[11px] font-mono mb-3 bg-red-950/40 p-2 rounded border border-red-900/50 whitespace-pre-wrap">{{ parseStatus.errorMessage }}</div>
          <div v-if="parseStatus.metadata?.errors?.length" class="flex flex-col gap-2 max-h-40 overflow-auto scrollbar-thin">
            <div v-for="(err, idx) in parseStatus.metadata.errors" :key="idx" class="bg-red-950/40 p-2 rounded border border-red-900/50 flex flex-col gap-1">
              <span class="text-[10px] font-bold text-red-300 font-mono">PG: {{ err.page_no || '?' }}</span>
              <span class="text-[10px] text-red-400/80 font-mono">{{ err.error_message || err.error_type || 'Unknown Fault' }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="flex flex-col items-center justify-center h-40 text-slate-500 border border-dashed border-white/[0.1] rounded-xl bg-slate-950/30">
        <span class="font-mono text-xs uppercase tracking-wider">No telemetry available</span>
      </div>
      
      <template #footer>
        <div class="flex justify-end pt-3 border-t border-white/[0.08]">
          <button @click="statusVisible = false" class="px-4 py-2 rounded bg-slate-800 text-white text-xs font-mono border border-slate-700 hover:bg-slate-700 transition-colors cursor-pointer outline-none uppercase tracking-wider">
            Close
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
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
let pollingInterval: number | null = null

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
  
  pollingInterval = window.setInterval(async () => {
    const hasProcessing = documents.value.some(d => d.status === 'PROCESSING' || d.status === 'PENDING')
    if (hasProcessing) {
      await loadDocuments(true)
    }
  }, 3000)
})

onUnmounted(() => {
  if (pollingInterval) {
    window.clearInterval(pollingInterval)
  }
})

watch(currentSpaceId, (spaceId) => {
  knowledgeStore.selectSpace(spaceId)
})

async function loadDocuments(silent = false) {
  if (!currentSpaceId.value) return
  if (!silent) loading.value = true
  error.value = ''
  try {
    documents.value = await listDocuments(currentSpaceId.value)
  } catch (err) {
    if (!silent) error.value = apiErrorMessage(err)
  } finally {
    if (!silent) loading.value = false
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
    ElMessage.success('Retry sequence initiated.')
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
  await ElMessageBox.confirm('Data deletion is irreversible. This data will be purged from the vector index.', 'Purge Authorization', {
    type: 'warning',
    confirmButtonText: 'Authorize Purge',
    cancelButtonText: 'Abort',
    customClass: 'cyber-confirm-box'
  })
  try {
    await deleteDocument(documentId)
    ElMessage.success('Packet purged successfully.')
    await loadDocuments()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  }
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

/* ElDialog Cyber overrides */
.cyber-dialog {
  background: #020617 !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
  border-radius: 1rem !important;
  box-shadow: 0 0 40px rgba(0,0,0,0.8), 0 0 0 1px rgba(0,240,255,0.1) !important;
}
.cyber-dialog .el-dialog__header {
  padding: 1.5rem 1.5rem 0.5rem !important;
  margin-right: 0 !important;
}
.cyber-dialog .el-dialog__body {
  padding: 1rem 1.5rem !important;
}
.cyber-dialog .el-dialog__footer {
  padding: 0 1.5rem 1.5rem !important;
}

/* Message box */
.cyber-confirm-box {
  background: #020617 !important;
  border: 1px solid rgba(239,68,68,0.3) !important;
}
.cyber-confirm-box .el-message-box__title {
  color: #fff !important;
}
.cyber-confirm-box .el-message-box__content {
  color: #94a3b8 !important;
}
</style>
