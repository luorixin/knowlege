<template>
  <div class="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[500px] text-white">
    <!-- Outer Banner title -->
    <div class="text-center pb-4 border-b border-white/[0.08] mb-4 shrink-0">
      <h1 class="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase m-0">
        Nexus Detailed Telemetry
      </h1>
    </div>

    <!-- Toolbar -->
    <div class="cyber-panel rounded-2xl p-4 mb-5 border border-white/[0.06] flex flex-col md:flex-row md:items-center justify-between gap-4 shrink-0">
      <div class="flex flex-col gap-1">
        <div class="flex items-center gap-3">
           <h2 class="text-lg font-bold text-white m-0 tracking-tight">{{ space?.name || 'Loading Nexus...' }}</h2>
           <span class="text-[10px] font-mono px-1.5 py-0.5 rounded border border-white/[0.1] text-slate-500 uppercase">ID: {{ spaceId }}</span>
        </div>
        <p class="text-[10px] font-mono text-slate-500 uppercase tracking-widest m-0">Owner: {{ space?.ownerUserId || '-' }} | Security: {{ space?.visibility === 'PRIVATE' ? 'PRIVATE' : 'TENANT WIDE' }}</p>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <button @click="router.push('/knowledge-bases')" class="py-2 px-3 rounded-lg border border-white/[0.08] hover:border-neon-cyan hover:text-neon-cyan text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer outline-none">
          <span class="material-symbols-outlined text-[14px]">arrow_back</span>
          <span>Back</span>
        </button>
        <button @click="goChat" class="py-2 px-3 rounded-lg bg-purple-950/30 border border-purple-500/50 hover:bg-purple-500 hover:text-white text-purple-400 text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer outline-none">
          <span class="material-symbols-outlined text-[14px]">chat</span>
          <span>Query</span>
        </button>
        <button @click="goUpload" class="py-2 px-4 rounded-lg bg-neon-cyan text-slate-900 border border-neon-cyan hover:bg-cyan-400 hover:shadow-[0_0_15px_rgba(0,240,255,0.4)] text-xs font-mono font-bold transition-all flex items-center gap-1.5 cursor-pointer uppercase tracking-wider outline-none">
          <span class="material-symbols-outlined text-[14px]">upload</span>
          Upload Data
        </button>
      </div>
    </div>

    <!-- Alert -->
    <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-950/40 border border-red-500/30 text-red-400 text-xs font-mono flex items-center gap-2 shrink-0">
      <span class="material-symbols-outlined text-[16px]">error</span>
      {{ error }}
    </div>

    <!-- Overview Stats -->
    <div class="grid grid-cols-1 xl:grid-cols-3 gap-6 mb-5 shrink-0">
      <div class="xl:col-span-2 cyber-panel rounded-xl p-5 border border-white/[0.05] flex flex-col relative overflow-hidden">
        <div class="absolute right-[-20px] top-[-20px] text-slate-800 opacity-30 text-[100px] font-black material-symbols-outlined pointer-events-none">info</div>
        <span class="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest mb-3 relative z-10">Nexus Parameters</span>
        <div class="flex-1 relative z-10">
          <p class="text-sm text-slate-300 leading-relaxed m-0">{{ space?.description || 'No operational parameters defined for this nexus.' }}</p>
        </div>
        <div class="mt-4 pt-4 border-t border-white/[0.05] flex items-center gap-4 relative z-10">
           <div class="flex items-center gap-2">
             <span class="text-[10px] font-mono text-slate-500 uppercase tracking-wider">Status:</span>
             <span class="text-[9px] font-mono font-bold px-2 py-0.5 rounded border uppercase" :class="space?.status === 'ACTIVE' ? 'bg-green-950/30 text-green-400 border-green-500/50' : 'bg-slate-800 border-slate-600 text-slate-300'">
               {{ space?.status || '-' }}
             </span>
           </div>
        </div>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-2 gap-4">
        <div class="cyber-panel rounded-xl p-4 border border-white/[0.05] border-l-2 border-l-blue-500 flex flex-col justify-center relative overflow-hidden">
          <div class="absolute right-[-10px] top-[-10px] text-blue-900/20 text-[64px] font-black material-symbols-outlined pointer-events-none">description</div>
          <span class="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest mb-1 relative z-10">Total Documents</span>
          <strong class="text-3xl font-bold text-blue-400 relative z-10 shadow-[0_0_10px_rgba(96,165,250,0.3)]">{{ documents.length }}</strong>
        </div>
        <div class="cyber-panel rounded-xl p-4 border border-white/[0.05] border-l-2 border-l-amber-500 flex flex-col justify-center relative overflow-hidden">
          <div class="absolute right-[-10px] top-[-10px] text-amber-900/20 text-[64px] font-black material-symbols-outlined pointer-events-none">memory</div>
          <span class="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest mb-1 relative z-10">Parsing / Processing</span>
          <strong class="text-3xl font-bold text-amber-400 relative z-10 shadow-[0_0_10px_rgba(251,191,36,0.3)]">{{ parsingCount }}</strong>
        </div>
        <div class="cyber-panel rounded-xl p-4 border border-white/[0.05] border-l-2 border-l-green-500 flex flex-col justify-center relative overflow-hidden sm:col-span-1 xl:col-span-2">
          <div class="absolute right-[-10px] top-[-10px] text-green-900/20 text-[64px] font-black material-symbols-outlined pointer-events-none">check_circle</div>
          <span class="text-[10px] font-mono font-bold text-slate-400 uppercase tracking-widest mb-1 relative z-10">Ready (ACTIVE)</span>
          <strong class="text-3xl font-bold text-green-400 relative z-10 shadow-[0_0_10px_rgba(34,197,94,0.3)]">{{ activeCount }}</strong>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="cyber-panel rounded-2xl p-4 border border-white/[0.06] flex-1 overflow-hidden flex flex-col min-h-0">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-sm font-bold text-white m-0 uppercase tracking-widest font-mono">Enclave Databanks</h3>
      </div>
      <el-table
        v-loading="loading"
        :data="documents"
        row-key="id"
        empty-text="No data packets uploaded to this nexus."
        class="flex-1 w-full"
        height="100%"
      >
        <el-table-column prop="title" label="Document Alias" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium text-slate-200">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="docType" label="Type" width="120">
           <template #default="{ row }">
             <span class="text-[10px] font-mono text-slate-400">{{ row.docType || '-' }}</span>
           </template>
        </el-table-column>
        <el-table-column prop="industry" label="Sector" width="140">
           <template #default="{ row }">
             <span class="text-[10px] font-mono text-slate-400">{{ row.industry || '-' }}</span>
           </template>
        </el-table-column>
        <el-table-column prop="serviceLine" label="Vector" width="160">
           <template #default="{ row }">
             <span class="text-[10px] font-mono text-slate-400">{{ row.serviceLine || '-' }}</span>
           </template>
        </el-table-column>
        <el-table-column prop="status" label="Integrity" width="120">
          <template #default="{ row }">
            <span class="text-[9px] font-mono font-bold px-2 py-0.5 rounded border uppercase" 
                  :class="row.status === 'ACTIVE' ? 'bg-green-950/30 text-green-400 border-green-500/50' : 'bg-slate-800 border-slate-600 text-slate-300'">
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="120" fixed="right">
          <template #default="{ row }">
            <button class="px-2 py-1 bg-cyan-950/30 border border-neon-cyan/50 text-neon-cyan hover:bg-neon-cyan hover:text-slate-900 rounded text-[10px] font-mono uppercase transition-colors outline-none cursor-pointer" 
                    @click="router.push(`/documents?docId=${row.id}`)">
              INSPECT
            </button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
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

<style>
/* ElTable overrides are handled globally in main.scss, so we only need custom scrollbar here if any */
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
</style>
