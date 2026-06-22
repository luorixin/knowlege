<template>
  <div class="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[500px] text-white">
    
    <!-- Outer Banner title -->
    <div class="text-center pb-4 border-b border-white/[0.08] mb-4 shrink-0">
      <h1 class="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase m-0">
        Advanced AI Command Chat
      </h1>
    </div>

    <!-- Primary Layout splits 3 panels: conversations sidebar, active Chat area, Evidence tracing (Image 3) -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-5 flex-1 overflow-hidden relative">
      
      <!-- Left Panel: Conversations List (Cols 3) -->
      <div v-show="showLeftSidebar" class="lg:col-span-3 cyber-panel rounded-2xl p-4 flex flex-col overflow-hidden h-full z-10 transition-all">
        <button
          @click="newChat"
          class="w-full py-2.5 px-4 rounded-xl border border-dashed border-cyan-400/30 hover:border-neon-cyan/80 hover:bg-cyan-950/20 text-neon-cyan hover:text-white text-xs font-mono tracking-wider transition-all flex items-center justify-center gap-2 mb-4 active:scale-95 cursor-pointer"
        >
          <span class="material-symbols-outlined text-[16px]">add</span>
          <span>New Chat</span>
        </button>

        <!-- Chat Search -->
        <div class="relative mb-4">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[16px] text-slate-500">search</span>
          <input
            type="text"
            placeholder="Search conversations..."
            v-model="chatSearch"
            class="w-full text-xs text-white pl-9 pr-3 py-2 rounded-lg border border-white/[0.06] bg-slate-950/80 focus:border-neon-cyan focus:outline-none placeholder:text-slate-500"
          />
        </div>

        <!-- Conversation list with category groups -->
        <div class="flex-1 overflow-y-auto space-y-4 pr-1 scrollbar-thin">
          <div class="space-y-1">
            <span class="block text-[9px] font-mono uppercase tracking-widest text-slate-500 px-2 mb-2">History</span>
            
            <button
              v-for="session in filteredSessions"
              :key="session.id"
              @click="loadHistory(session.id)"
              :class="[
                'w-full text-left p-3 rounded-xl transition-all border flex flex-col justify-between cursor-pointer',
                chatStore.sessionId === session.id
                  ? 'bg-cyan-950/30 border-neon-cyan/40 shadow-[0_0_12px_rgba(0,240,255,0.06)]'
                  : 'bg-transparent border-transparent hover:bg-white/[0.02]'
              ]"
            >
              <div class="flex items-center justify-between mb-1">
                <span :class="['text-xs font-medium truncate tracking-wide', chatStore.sessionId === session.id ? 'text-white font-semibold' : 'text-slate-300']">
                  {{ session.title || 'New Session' }}
                </span>
                <div v-if="chatStore.sessionId === session.id" class="w-1.5 h-1.5 rounded-full bg-neon-cyan shadow-[0_0_6px_#00f0ff]"></div>
              </div>
              <div class="flex items-center justify-between mt-1 text-[10px] font-mono text-slate-500">
                <span>{{ formatDate(session.createdAt) }}</span>
                <span class="text-[9px] opacity-70">
                  {{ selectedSpace?.name || 'Default Space' }}
                </span>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Center Panel: Active Chats & Inputs (Flexible cols based on viewport panel display) -->
      <div :class="[centerColSpanClass, 'flex flex-col h-full overflow-hidden relative min-w-0']">
        
        <!-- Header selectors of chat -->
        <div class="cyber-panel rounded-2xl p-4 mb-3 border border-white/[0.06] flex flex-col shrink-0">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div class="flex items-center space-x-2">
              <span class="text-xs font-mono text-slate-400">Knowledge Base:</span>
              <div class="relative">
                <select
                  v-model="spaceId"
                  @change="selectSpace($event.target.value)"
                  class="bg-slate-950 px-3 py-1.5 rounded-lg border border-white/[0.08] text-xs font-mono text-neon-cyan focus:outline-none pr-8 appearance-none cursor-pointer"
                >
                  <option v-for="space in knowledgeStore.spaces" :key="space.id" :value="space.id">
                    {{ space.name }}
                  </option>
                </select>
                <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-[14px] text-neon-cyan pointer-events-none">expand_more</span>
              </div>
            </div>

            <!-- Dynamic sidebar toggles and filters -->
            <div class="flex items-center space-x-2 shrink-0">
              <!-- Toggle Chat History -->
              <button
                type="button"
                @click="showLeftSidebar = !showLeftSidebar"
                :class="[
                  'py-1.5 px-3 rounded-lg border text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer',
                  showLeftSidebar ? 'border-neon-cyan bg-cyan-950/20 text-neon-cyan font-bold animate-pulse' : 'border-white/[0.08] hover:border-white/20 text-slate-400'
                ]"
                title="Toggle Chat History panel"
              >
                <span class="material-symbols-outlined text-[14px]">chat</span>
                <span class="hidden sm:inline">History</span>
              </button>
              
              <!-- Toggle Evidence Panel -->
              <button
                type="button"
                @click="showRightSidebar = !showRightSidebar"
                :class="[
                  'py-1.5 px-3 rounded-lg border text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer',
                  showRightSidebar ? 'border-neon-cyan bg-cyan-950/20 text-neon-cyan font-bold animate-pulse' : 'border-white/[0.08] hover:border-white/20 text-slate-400'
                ]"
                title="Toggle Evidence tracing panel"
              >
                <span class="material-symbols-outlined text-[14px]">description</span>
                <span class="hidden sm:inline">Evidence</span>
              </button>

              <!-- Filter toggle -->
              <button
                type="button"
                @click="showFilters = !showFilters"
                :class="[
                  'py-1.5 px-3 rounded-lg border text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer',
                  showFilters ? 'border-neon-cyan bg-cyan-950/20 text-neon-cyan' : 'border-white/[0.08] hover:border-white/25 text-slate-300'
                ]"
                title="Toggle advanced document filters"
              >
                <span class="material-symbols-outlined text-[14px]">filter_list</span>
                <span class="hidden sm:inline">Filters</span>
                <span v-if="activeFiltersCount > 0" class="px-1.5 py-0.2 text-[9px] font-mono font-bold bg-neon-cyan text-slate-950 rounded-full">
                  {{ activeFiltersCount }}
                </span>
              </button>
              
              <button @click="chatStore.clear()" class="py-1.5 px-3 rounded-lg border border-red-950/40 hover:border-red-500 hover:bg-red-950/20 text-xs font-mono text-red-500 transition-all flex items-center gap-1.5 cursor-pointer ml-1">
                <span class="material-symbols-outlined text-[14px]">delete_sweep</span>
              </button>
            </div>
          </div>

          <!-- Inline expandable Filters drawer area -->
          <div v-if="showFilters" class="border-t border-white/[0.08] mt-4 pt-4 grid grid-cols-1 sm:grid-cols-3 gap-4 text-left animate-fadeIn">
            
            <!-- Doc Type column -->
            <div class="space-y-2">
              <span class="block text-[9px] font-mono uppercase tracking-wider text-slate-500 font-bold">Doc Type</span>
              <div class="space-y-1.5">
                <select v-model="filters.doc_type" class="w-full text-xs text-white px-3 py-1.5 rounded border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none font-mono">
                  <option value="">All Types</option>
                  <option value="proposal">Proposal</option>
                  <option value="sow">SOW</option>
                  <option value="policy">Policy</option>
                  <option value="research">Research</option>
                </select>
              </div>
            </div>

            <!-- Industry column -->
            <div class="space-y-2">
              <span class="block text-[9px] font-mono uppercase tracking-wider text-slate-500 font-bold">Industry / Service</span>
              <div class="space-y-1.5 flex flex-col gap-2">
                <input v-model="filters.industry" placeholder="Industry (e.g. Finance)" class="w-full text-xs text-white px-3 py-1.5 rounded border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none font-mono" />
                <input v-model="filters.service_line" placeholder="Service Line" class="w-full text-xs text-white px-3 py-1.5 rounded border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none font-mono" />
              </div>
            </div>

            <!-- Year column -->
            <div class="space-y-2">
              <span class="block text-[9px] font-mono uppercase tracking-wider text-slate-500 font-bold">Year From</span>
              <div class="space-y-1.5">
                <input type="number" v-model="filters.year_from" min="2000" max="2100" placeholder="e.g. 2023" class="w-full text-xs text-white px-3 py-1.5 rounded border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none font-mono" />
              </div>
            </div>

          </div>
        </div>

        <!-- Message log content list -->
        <div id="chat-container" class="flex-1 overflow-y-auto space-y-4 pr-1 scrollbar-thin mb-3">
          <div v-if="chatStore.messages.length === 0" class="h-full flex flex-col items-center justify-center text-center p-8">
            <span class="material-symbols-outlined text-[40px] text-slate-600 mb-2 animate-bounce">chat</span>
            <p class="text-slate-400 text-sm">Send a request payload to begin vector parsing.</p>
            <p class="text-[10px] text-slate-500 font-mono mt-1">E.G. "What is the efficiency curve?"</p>
          </div>

          <template v-else>
            <div v-for="message in chatStore.messages" :key="message.id" :class="['flex flex-col relative', message.role === 'user' ? 'items-end' : 'items-start']">
              
              <!-- Standard messages -->
              <div
                :class="[
                  'rounded-xl p-4 text-xs leading-relaxed max-w-[90%] border shadow-md',
                  message.role === 'user'
                    ? 'bg-cyan-950/20 border-cyan-500/20 text-slate-100 font-sans'
                    : 'cyber-panel hover:bg-slate-900/60 transition-all border-white/[0.06]'
                ]"
              >
                <span class="block text-[10px] font-mono tracking-wider uppercase text-slate-500 mb-2">
                  {{ message.role === 'user' ? 'User:' : 'AI:' }}
                </span>

                <div 
                  class="font-sans whitespace-pre-line text-slate-200 chat-markdown"
                  :class="{ 'text-red-400': message.error }"
                  v-html="renderMarkdown(message.content)"
                  @click="handleCitationClick"
                ></div>

                <div v-if="message.content.includes(noEvidenceText)" class="mt-4 inline-flex items-center gap-2 px-3 py-1.5 rounded-md bg-amber-950/40 text-amber-400 text-xs font-mono border border-amber-500/30">
                  <span class="material-symbols-outlined text-[14px]">warning</span>
                  No reliable evidence found in current space
                </div>

                <div class="mt-4 flex items-center gap-3 opacity-0 hover:opacity-100 transition-opacity" v-if="userStore.isAdmin && message.debugInfo">
                  <button @click="openDebug(message.debugInfo)" class="flex items-center gap-1.5 text-[10px] font-mono text-slate-400 hover:text-neon-cyan border border-white/[0.06] hover:border-cyan-500/30 bg-slate-950/50 px-2 py-1 rounded transition-colors cursor-pointer">
                    <span class="material-symbols-outlined text-[12px]">bug_report</span>
                    Debug Trace
                  </button>
                </div>
              </div>
            </div>

            <!-- Loading State -->
            <div v-if="chatStore.sending && (!chatStore.messages.length || chatStore.messages[chatStore.messages.length - 1].content === '')" class="flex flex-col items-start relative">
               <div class="rounded-xl p-4 text-xs leading-relaxed max-w-[90%] border shadow-md cyber-panel border-white/[0.06] w-full max-w-md">
                 <span class="block text-[10px] font-mono tracking-wider uppercase text-slate-500 mb-2">AI:</span>
                 <div class="space-y-2">
                   <div class="h-2 bg-slate-700/50 rounded w-3/4 animate-pulse"></div>
                   <div class="h-2 bg-slate-700/50 rounded w-full animate-pulse"></div>
                   <div class="h-2 bg-slate-700/50 rounded w-5/6 animate-pulse"></div>
                 </div>
               </div>
            </div>
          </template>
        </div>

        <!-- Message input bar with paperclip and send action - matching Image 3 bottom -->
        <div class="cyber-panel rounded-2xl p-2 relative flex items-center gap-2 border border-white/[0.08] shrink-0">
          <button
            type="button"
            class="p-2 border border-white/[0.05] hover:border-neon-cyan/30 bg-slate-950/80 rounded-xl text-slate-400 hover:text-white transition-all cursor-pointer outline-none"
            title="Add context attachment"
          >
            <span class="material-symbols-outlined text-[16px]">attach_file</span>
          </button>
          
          <textarea
            v-model="question"
            rows="3"
            placeholder="Ask anything about enterprise knowledge... (Shift+Enter for new line)"
            class="flex-1 bg-transparent px-3 py-2 text-sm text-white placeholder:text-slate-600 focus:outline-none font-sans resize-y min-h-[60px] max-h-[150px] no-cyber-focus"
            style="background: transparent !important; color: inherit !important; border: none !important; box-shadow: none !important;"
            @keydown.enter.exact.prevent="send"
          ></textarea>

          <button
            v-if="!chatStore.sending"
            @click="send"
            :disabled="!spaceId || !question.trim()" 
            class="p-2 px-3 rounded-xl bg-gradient-to-r from-cyan-500 to-cyan-400 text-slate-950 hover:from-neon-cyan hover:to-cyan-300 transition-all cursor-pointer flex items-center justify-center shrink-0 shadow-[0_0_12px_rgba(0,240,255,0.35)] disabled:opacity-50 disabled:cursor-not-allowed outline-none"
          >
            <span class="material-symbols-outlined text-[16px]">send</span>
          </button>
          <button 
            v-else
            @click="chatStore.stopGeneration()"
            class="p-2 px-3 rounded-xl bg-slate-800 border border-slate-600 text-white hover:bg-slate-700 transition-all cursor-pointer flex items-center justify-center shrink-0 shadow-md outline-none"
            title="Stop generation"
          >
            <span class="material-symbols-outlined text-[16px]">stop_circle</span>
          </button>
        </div>
      </div>

      <!-- Right Panel: Evidence Tracing (Cols 3) - matches design of Image 3 -->
      <div v-show="showRightSidebar" class="lg:col-span-3 cyber-panel rounded-2xl p-4 flex flex-col overflow-hidden h-full z-10 transition-all">
        <div class="flex items-center justify-between pb-3 border-b border-white/[0.08] mb-4 shrink-0">
          <h3 class="text-xs font-mono uppercase tracking-widest text-slate-400 m-0">
            Evidence Tracing
          </h3>
          <span v-if="chatStore.activeCitations?.length" class="text-[9px] font-mono text-neon-cyan uppercase px-1.5 py-0.5 rounded bg-cyan-950/30 border border-cyan-900/50">
            {{ chatStore.activeCitations.length }} Sources
          </span>
          <span v-else class="text-[9px] font-mono text-slate-600 uppercase">Interactive Log</span>
        </div>

        <div class="flex-1 overflow-y-auto relative scrollbar-thin">
          <div v-if="chatStore.activeCitations?.length" class="space-y-4 pr-1">
            <div
              v-for="source in chatStore.activeCitations"
              :key="source.id"
              :id="'citation-card-' + source.id"
              :class="[
                'group p-3.5 rounded-xl border transition-all relative',
                activeCitationId === source.id
                  ? 'border-neon-cyan bg-cyan-950/30 scale-[1.02] shadow-[0_0_20px_rgba(0,240,255,0.25)]'
                  : 'border-white/[0.06] bg-slate-950/40 hover:border-white/15'
              ]"
            >
              <div v-if="activeCitationId === source.id" class="absolute inset-0 bg-cyan-500/[0.02] animate-pulse rounded-xl pointer-events-none"></div>

              <div class="flex items-start justify-between mb-1.5 gap-2">
                <span class="flex items-center gap-1.5">
                  <span class="font-mono font-bold text-[10px] bg-cyan-950 text-neon-cyan border border-neon-cyan/30 px-1.5 py-0.2 rounded shrink-0">
                    [{{ source.id }}]
                  </span>
                  <span class="text-xs font-semibold text-slate-100 truncate tracking-wide" :title="source.doc_title">
                    {{ source.doc_title || 'Unknown Document' }}
                  </span>
                </span>
              </div>

              <p class="text-slate-300 text-[10.5px] leading-relaxed mb-3 italic bg-slate-900/40 p-2 rounded-lg border border-white/[0.03] overflow-hidden line-clamp-4 group-hover:line-clamp-none transition-all duration-300">
                &ldquo;{{ source.chunk_content }}&rdquo;
              </p>

              <button
                @click="handlePreview(source)"
                class="text-[9px] font-mono text-neon-cyan/80 group-hover:text-neon-cyan hover:underline transition-all flex items-center justify-end w-full gap-1 cursor-pointer select-none outline-none border-none bg-transparent"
              >
                <span>View Source</span>
                <span class="material-symbols-outlined text-[12px] group-hover:translate-x-0.5 transition-transform">arrow_forward</span>
              </button>
            </div>
          </div>
          <div v-else class="absolute inset-0 flex flex-col items-center justify-center text-slate-600 opacity-80">
            <span class="material-symbols-outlined text-[40px] mb-4 font-light opacity-50">library_books</span>
            <p class="font-mono text-xs tracking-wide">No active citations</p>
          </div>
        </div>
      </div>

    </div>

    <!-- Debug Drawer -->
    <el-drawer v-model="debugVisible" title="检索链路调试" size="600px" class="cyber-drawer">
      <div v-if="currentDebugInfo">
        <el-collapse v-model="debugActiveNames" class="border-none">
          <el-collapse-item title="最终给到模型的 Context" name="1">
            <pre class="bg-slate-950 p-4 rounded-lg border border-white/[0.1] font-mono text-xs text-slate-300 whitespace-pre-wrap word-break max-h-[400px] overflow-y-auto">{{ currentDebugInfo.final_context || '无' }}</pre>
          </el-collapse-item>
          <el-collapse-item title="向量召回与 Rerank 得分" name="2">
            <pre class="bg-slate-950 p-4 rounded-lg border border-white/[0.1] font-mono text-xs text-slate-300 whitespace-pre-wrap word-break max-h-[400px] overflow-y-auto">{{ JSON.stringify(currentDebugInfo.reranked_chunks, null, 2) }}</pre>
          </el-collapse-item>
          <el-collapse-item title="关键词检索召回 (如果有)" name="3">
            <pre class="bg-slate-950 p-4 rounded-lg border border-white/[0.1] font-mono text-xs text-slate-300 whitespace-pre-wrap word-break max-h-[400px] overflow-y-auto">{{ JSON.stringify(currentDebugInfo.retrieval_results, null, 2) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-drawer>

    <!-- Source Preview Drawer -->
    <el-drawer v-model="previewVisible" size="50%" class="cyber-drawer">
      <template #header>
        <div class="flex justify-between items-center w-full">
          <span class="text-sm font-mono text-neon-cyan font-bold uppercase tracking-wider">{{ previewTitle }}</span>
          <button v-if="previewUrl" @click="downloadPreview" class="px-3 py-1.5 rounded bg-cyan-950/40 text-neon-cyan border border-neon-cyan/30 hover:bg-neon-cyan hover:text-slate-900 transition-colors text-xs font-mono flex items-center gap-1 cursor-pointer">
            <span class="material-symbols-outlined text-[14px]">download</span>
            Download
          </button>
        </div>
      </template>

      <div v-if="previewLoading" class="w-full h-full flex flex-col items-center justify-center text-neon-cyan/50">
        <span class="material-symbols-outlined text-4xl mb-4 animate-spin">refresh</span>
        <p class="font-mono text-xs">Loading Source Vector...</p>
      </div>
      <div v-else-if="previewUrl" class="w-full h-full relative bg-slate-900 rounded-xl overflow-hidden border border-white/[0.05]">
        <vue-office-docx v-if="previewFileType === 'docx'" :src="previewUrl" class="w-full h-full bg-white" />
        <vue-office-excel v-else-if="previewFileType === 'excel'" :src="previewUrl" class="w-full h-full bg-white" />
        <vue-office-pdf v-else-if="previewFileType === 'pdf'" :src="previewUrl" class="w-full h-full" />
        <vue-office-pptx v-else-if="previewFileType === 'pptx'" :src="previewUrl" class="w-full h-full bg-white" />
        <div v-else-if="previewFileType === 'image'" class="w-full h-full flex items-center justify-center bg-slate-950 overflow-auto">
          <img :src="previewUrl" class="max-w-full max-h-full object-contain" />
        </div>
        <iframe v-else :src="previewUrl" class="w-full h-full border-none bg-white"></iframe>
      </div>
      <div v-else class="w-full h-full flex items-center justify-center text-slate-500 font-mono text-xs">
        <p>Preview stream unavailable.</p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, onUnmounted, reactive, ref, nextTick, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

import VueOfficeDocx from '@vue-office/docx'
import '@vue-office/docx/lib/index.css'
import VueOfficeExcel from '@vue-office/excel'
import '@vue-office/excel/lib/index.css'
import VueOfficePdf from '@vue-office/pdf'
import VueOfficePptx from '@vue-office/pptx'

import type { EntityId, SearchFilters } from '@/api/types'
import { useChatStore } from '@/stores/chat'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'ChatView',
})

const noEvidenceText = '未在当前知识库中找到可靠依据'
const userStore = useUserStore()
const knowledgeStore = useKnowledgeStore()
const chatStore = useChatStore()
const question = ref('')
const spaceId = ref<EntityId | null>(knowledgeStore.selectedSpaceId)
const filters = reactive<SearchFilters>({
  doc_type: '',
  industry: '',
  service_line: '',
  year_from: undefined,
})

const chatSearch = ref('')

const debugVisible = ref(false)
const currentDebugInfo = ref<any>(null)
const debugActiveNames = ref(['1', '2'])
const activeCitationId = ref<string | null>(null)

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewFileType = ref<'docx' | 'excel' | 'pdf' | 'pptx' | 'image' | 'other'>('other')
const previewUrl = ref<string | null>(null)
const previewTitle = ref('文档预览')

const showLeftSidebar = ref(window.innerWidth >= 1280)
const showRightSidebar = ref(window.innerWidth >= 1280)
const showFilters = ref(false)

const activeFiltersCount = computed(() => {
  let count = 0
  if (filters.doc_type) count++
  if (filters.industry) count++
  if (filters.service_line) count++
  if (filters.year_from) count++
  return count
})

const filteredSessions = computed(() => {
  if (!chatSearch.value) return chatStore.sessions
  return chatStore.sessions.filter(s => 
    (s.title || '').toLowerCase().includes(chatSearch.value.toLowerCase())
  )
})

const leftSpan = computed(() => showLeftSidebar.value ? 3 : 0)
const rightSpan = computed(() => showRightSidebar.value ? 3 : 0)
const centerSpan = computed(() => 12 - leftSpan.value - rightSpan.value)

const centerColSpanClass = computed(() => {
  if (centerSpan.value === 12) return 'lg:col-span-12'
  if (centerSpan.value === 9) return 'lg:col-span-9'
  return 'lg:col-span-6'
})

function handleResize() {
  if (window.innerWidth < 1280) {
    showLeftSidebar.value = false
    showRightSidebar.value = false
  } else {
    showLeftSidebar.value = true
    showRightSidebar.value = true
  }
}

function openDebug(info: any) {
  currentDebugInfo.value = info
  debugVisible.value = true
}

const selectedSpace = computed(() => knowledgeStore.spaces.find((item) => item.id === spaceId.value) || null)

onMounted(async () => {
  if (userStore.tenantId) {
    await knowledgeStore.ensureSpaces(userStore.tenantId)
  }
  spaceId.value = knowledgeStore.selectedSpaceId || knowledgeStore.spaces[0]?.id || null
  if (spaceId.value) {
    chatStore.loadSessions(spaceId.value)
  }
  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    window.URL.revokeObjectURL(previewUrl.value)
  }
})

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function newChat() {
  chatStore.clear()
}

async function loadHistory(id: EntityId) {
  if (!spaceId.value) return
  await chatStore.loadHistory(spaceId.value, id)
}

// Auto-scroll chat to bottom
watch(() => chatStore.messages.length, () => {
  nextTick(() => {
    const container = document.getElementById('chat-container')
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}, { deep: true })

function selectSpace(value: EntityId) {
  knowledgeStore.selectSpace(value)
  chatStore.clear()
  chatStore.loadSessions(value)
}

async function send() {
  if (!spaceId.value) {
    ElMessage.warning('请先选择知识库')
    return
  }
  const cleanedFilters: SearchFilters = {
    doc_type: filters.doc_type || undefined,
    industry: filters.industry || undefined,
    service_line: filters.service_line || undefined,
    year_from: filters.year_from || undefined,
  }
  const currentQuestion = question.value
  question.value = ''
  await chatStore.send(spaceId.value, currentQuestion, cleanedFilters)
}

function renderMarkdown(text: string) {
  if (!text) return ''
  // Format citations from [1] or 【1】 to styled HTML spans with data-id
  const processedText = text.replace(/\[(\d+)\]|【(\d+)】/g, (match, p1, p2) => {
    const id = p1 || p2
    return `<sup class="citation-link inline-block mx-1 font-mono font-bold text-[10px] bg-cyan-950 text-neon-cyan border border-neon-cyan/40 px-1 py-0 rounded hover:bg-neon-cyan hover:text-slate-950 transition-colors cursor-pointer select-none shadow-[0_0_6px_rgba(0,240,255,0.2)]" data-id="${id}">[${id}]</sup>`
  })
  const rawHtml = marked.parse(processedText, { async: false }) as string
  return DOMPurify.sanitize(rawHtml)
}

function handleCitationClick(e: MouseEvent) {
  let target = e.target as HTMLElement
  
  // Walk up just in case
  while (target && target !== e.currentTarget) {
    if (target.classList && target.classList.contains('citation-link')) {
      e.preventDefault()
      const id = target.getAttribute('data-id')
      if (id) {
        showRightSidebar.value = true // Ensure sidebar is open
        activeCitationId.value = id
        // Scroll the citation panel to the active citation
        setTimeout(() => {
          const citationEl = document.getElementById(`citation-card-${id}`)
          if (citationEl) {
            citationEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
          }
          // Remove highlight after a delay
          setTimeout(() => {
            if (activeCitationId.value === id) activeCitationId.value = null
          }, 2500)
        }, 100)
      }
      return
    }
    target = target.parentNode as HTMLElement
  }
}

async function handlePreview(citation: import('@/api/types').AgentCitation) {
  if (!citation.source_uri || !citation.doc_id) return
  
  previewTitle.value = citation.doc_title || 'Document Preview'
  previewVisible.value = true
  
  // Determine file type from extension
  const filename = citation.source_uri.split('/').pop() || ''
  const ext = filename.split('.').pop()?.toLowerCase() || ''
  
  if (['doc', 'docx'].includes(ext)) previewFileType.value = 'docx'
  else if (['xls', 'xlsx'].includes(ext)) previewFileType.value = 'excel'
  else if (['ppt', 'pptx'].includes(ext)) previewFileType.value = 'pptx'
  else if (ext === 'pdf') previewFileType.value = 'pdf'
  else if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext)) previewFileType.value = 'image'
  else previewFileType.value = 'other'

  if (citation.source_uri.startsWith('local://')) {
    previewLoading.value = true
    if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
      window.URL.revokeObjectURL(previewUrl.value)
    }
    previewUrl.value = null
    
    try {
      const { http } = await import('@/api/http')
      const response = await http.get(`/api/v1/documents/${citation.doc_id}/download`, { responseType: 'blob' })
      let contentType = response.headers['content-type'] as string | undefined
      if (contentType && contentType.startsWith('text/') && !contentType.includes('charset')) {
        contentType += ';charset=utf-8'
      }
      
      const blob = new Blob([response.data as BlobPart], { type: contentType })
      let url = window.URL.createObjectURL(blob)
      
      if (previewFileType.value === 'other') {
         if (contentType?.includes('application/pdf')) previewFileType.value = 'pdf'
         else if (contentType?.startsWith('image/')) previewFileType.value = 'image'
      }

      // If it's a PDF, we can append #search= to trigger browser's native highlight
      if ((contentType === 'application/pdf' || previewFileType.value === 'pdf') && citation.chunk_content) {
        // Find a representative sentence to highlight (to avoid too long search string)
        const sentences = citation.chunk_content.split(/[。！？.!?\n]/).filter(s => s.trim().length > 5)
        const searchKeyword = sentences.length > 0 ? sentences[0].trim() : citation.chunk_content.substring(0, 50)
        url += `#search="${encodeURIComponent(searchKeyword)}"`
      }
      
      previewUrl.value = url
    } catch (e) {
      console.error('Failed to load document preview', e)
      ElMessage.error('Failed to load document stream.')
      previewVisible.value = false
    } finally {
      previewLoading.value = false
    }
  } else {
    // For external URLs, we can just use them
    previewUrl.value = citation.source_uri
  }
}

function downloadPreview() {
  if (!previewUrl.value) return
  const a = document.createElement('a')
  a.href = previewUrl.value
  a.download = previewTitle.value
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}
</script>

<style>
/* Unscoped global adjustments for this view's dynamic components if needed */
.chat-markdown {
  color: theme('colors.slate.200');
  font-family: theme('fontFamily.sans');
  line-height: 1.6;
}
.chat-markdown p {
  margin-bottom: 0.75rem;
}
.chat-markdown p:last-child {
  margin-bottom: 0;
}
.chat-markdown h1,
.chat-markdown h2,
.chat-markdown h3,
.chat-markdown h4 {
  font-family: theme('fontFamily.display');
  font-weight: 600;
  color: theme('colors.white');
  margin-top: 1.25rem;
  margin-bottom: 0.5rem;
}
.chat-markdown h1 { font-size: 1.25rem; }
.chat-markdown h2 { font-size: 1.125rem; }
.chat-markdown h3 { font-size: 1rem; }
.chat-markdown ul, .chat-markdown ol {
  margin-bottom: 0.75rem;
  padding-left: 1.25rem;
}
.chat-markdown ul { list-style-type: disc; }
.chat-markdown ol { list-style-type: decimal; }
.chat-markdown li { margin-bottom: 0.25rem; }
.chat-markdown a:not(.citation-link) {
  color: theme('colors.neon-cyan');
  text-decoration: none;
}
.chat-markdown a:not(.citation-link):hover {
  text-decoration: underline;
}
.chat-markdown strong {
  font-weight: 700;
  color: theme('colors.cyan.300');
}
.chat-markdown blockquote {
  border-left: 2px solid theme('colors.cyan.500');
  padding-left: 0.75rem;
  color: theme('colors.slate.400');
  font-style: italic;
  margin-bottom: 0.75rem;
  background: rgba(0,0,0,0.2);
  border-radius: 0 4px 4px 0;
}
.chat-markdown code {
  font-family: theme('fontFamily.mono');
  font-size: 0.85em;
  background-color: rgba(0, 0, 0, 0.3);
  padding: 0.1em 0.3em;
  border-radius: 0.25rem;
  color: theme('colors.neon-pink');
}
.chat-markdown pre {
  background-color: rgba(0, 0, 0, 0.4);
  padding: 0.75rem;
  border-radius: 0.5rem;
  border: 1px solid theme('colors.white/10');
  overflow-x: auto;
  margin-bottom: 0.75rem;
}
.chat-markdown pre code {
  background-color: transparent;
  padding: 0;
  color: theme('colors.slate.300');
}

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
}
.cyber-drawer .el-drawer__body {
  padding: 1.5rem;
}
.cyber-drawer .el-collapse-item__header {
  background-color: transparent !important;
  color: #94a3b8 !important;
  border-bottom-color: rgba(255,255,255,0.05) !important;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 0.75rem;
  text-transform: uppercase;
}
.cyber-drawer .el-collapse-item__wrap {
  background-color: transparent !important;
  border-bottom-color: rgba(255,255,255,0.05) !important;
}
</style>
