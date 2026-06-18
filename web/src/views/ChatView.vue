<template>
  <div class="flex h-[calc(100vh-74px)] bg-slate-50 -mx-7 -my-6 overflow-hidden">
    <!-- Main Chat Area -->
    <main class="flex-1 flex flex-col relative min-w-0">
      <!-- Top Control Bar (replaces the left sidebar) -->
      <header class="flex items-center gap-4 px-8 py-4 bg-white/80 backdrop-blur-md border-b border-slate-200/60 z-10 shrink-0">
        <div class="flex items-center gap-2 font-semibold text-slate-800 shrink-0">
          <span class="material-symbols-outlined text-blue-800">filter_list</span>
          <span>知识库配置</span>
        </div>
        <el-select v-model="spaceId" class="w-48" placeholder="选择知识库" @change="selectSpace">
          <el-option v-for="space in knowledgeStore.spaces" :key="space.id" :label="space.name" :value="space.id" />
        </el-select>
        
        <el-popover placement="bottom-start" :width="320" trigger="click">
          <template #reference>
            <el-button class="ml-2" plain>
              <span class="material-symbols-outlined mr-1" style="font-size: 18px;">tune</span>
              高级过滤
            </el-button>
          </template>
          <div class="flex flex-col gap-3">
            <h4 class="m-0 text-sm font-semibold text-slate-700">文档属性过滤</h4>
            <el-select v-model="filters.doc_type" size="small" clearable placeholder="文档类型">
              <el-option label="Proposal" value="proposal" />
              <el-option label="SOW" value="sow" />
              <el-option label="制度文档" value="policy" />
              <el-option label="行业研究" value="research" />
            </el-select>
            <el-input v-model="filters.industry" size="small" clearable placeholder="行业 (例如：金融)" />
            <el-input v-model="filters.service_line" size="small" clearable placeholder="服务线 (例如：数据治理)" />
            <el-input-number v-model="filters.year_from" size="small" class="w-full" :min="2000" :max="2100" placeholder="起始年份" />
          </div>
        </el-popover>

        <div class="ml-auto">
          <button @click="chatStore.clear()" class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors">
            <span class="material-symbols-outlined text-[18px]">delete_sweep</span>
            清空对话
          </button>
        </div>
      </header>

      <!-- Chat Feed -->
      <div id="chat-container" class="flex-1 overflow-y-auto px-8 py-12 pb-48">
        <div class="max-w-3xl mx-auto flex flex-col gap-12">
          <el-empty v-if="chatStore.messages.length === 0" :image-size="120" description="暂无对话，请在下方输入问题" />

          <article v-for="message in chatStore.messages" :key="message.id" class="group relative">
            
            <!-- User Query (Marginalia style heading) -->
            <div v-if="message.role === 'user'" class="mb-6">
              <h2 class="text-2xl font-bold text-slate-900 tracking-tight leading-snug m-0">
                {{ message.content }}
              </h2>
            </div>

            <!-- AI Response (Editorial Serif Document style) -->
            <div v-else-if="message.role === 'assistant'" class="flex gap-6">
              <!-- Avatar gutter -->
              <div class="shrink-0 w-8 flex flex-col items-center pt-1">
                <div class="w-8 h-8 rounded bg-blue-900 flex items-center justify-center shadow-sm">
                  <span class="material-symbols-outlined text-white text-[18px]">smart_toy</span>
                </div>
              </div>
              
              <!-- Content -->
              <div class="flex-1 min-w-0">
                <div 
                  class="markdown-body"
                  :class="{ 'text-red-700': message.error }"
                  v-html="renderMarkdown(message.content)"
                  @click="handleCitationClick"
                ></div>

                <div v-if="message.content.includes(noEvidenceText)" class="mt-4 inline-flex items-center gap-2 px-3 py-1.5 rounded-md bg-amber-50 text-amber-700 text-sm font-medium border border-amber-200/60">
                  <span class="material-symbols-outlined text-[16px]">warning</span>
                  未在当前知识库中找到可靠依据
                </div>

                <div class="mt-4 flex items-center gap-3 opacity-0 group-hover:opacity-100 transition-opacity" v-if="userStore.isAdmin && message.debugInfo">
                  <button @click="openDebug(message.debugInfo)" class="flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-blue-700 bg-slate-100 hover:bg-blue-50 px-2.5 py-1 rounded transition-colors border border-slate-200/60">
                    <span class="material-symbols-outlined text-[14px]">bug_report</span>
                    检索调试
                  </button>
                </div>
              </div>
            </div>
          </article>

          <!-- Loading State -->
          <article v-if="chatStore.sending" class="flex gap-6">
            <div class="shrink-0 w-8 flex flex-col items-center pt-1">
              <div class="w-8 h-8 rounded bg-blue-900 flex items-center justify-center shadow-sm opacity-50 animate-pulse">
                <span class="material-symbols-outlined text-white text-[18px]">smart_toy</span>
              </div>
            </div>
            <div class="flex-1 pt-1">
              <div class="h-4 bg-slate-200 rounded w-3/4 animate-pulse mb-4"></div>
              <div class="h-4 bg-slate-200 rounded w-full animate-pulse mb-4"></div>
              <div class="h-4 bg-slate-200 rounded w-5/6 animate-pulse"></div>
            </div>
          </article>
        </div>
      </div>

      <!-- Input Area -->
      <div class="absolute bottom-8 left-1/2 -translate-x-1/2 w-full max-w-3xl px-6">
        <div class="bg-white/90 backdrop-blur-xl border border-slate-200/80 shadow-[0_8px_30px_rgb(0,0,0,0.08)] rounded-2xl p-2 flex items-end gap-2 transition-shadow focus-within:shadow-[0_8px_30px_rgb(0,0,0,0.12)] focus-within:border-blue-300">
          <button class="shrink-0 p-3 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition-colors mb-0.5 outline-none">
            <span class="material-symbols-outlined">attach_file</span>
          </button>
          <el-input
            v-model="question"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            resize="none"
            placeholder="提出问题，AI 将为您检索知识库并进行分析..."
            class="stitch-chat-input flex-1"
            @keydown.meta.enter.prevent="send"
            @keydown.ctrl.enter.prevent="send"
          />
          <button 
            class="shrink-0 w-12 h-12 flex items-center justify-center rounded-xl bg-blue-800 text-white shadow-md transition-all hover:bg-blue-700 hover:shadow-lg active:scale-95 disabled:opacity-50 disabled:pointer-events-none mb-0.5 outline-none"
            :disabled="!spaceId || !question.trim()" 
            @click="send"
          >
            <span class="material-symbols-outlined">send</span>
          </button>
        </div>
        <p class="text-center text-[10px] font-semibold text-slate-400 uppercase tracking-widest mt-4">
          AI generated content may be inaccurate
        </p>
      </div>
    </main>

    <!-- Right Column: Citations Sidebar -->
    <aside class="w-[420px] shrink-0 border-l border-slate-200/60 bg-slate-50 flex flex-col shadow-[-4px_0_15px_rgba(0,0,0,0.02)] z-10">
      <div class="px-6 py-5 border-b border-slate-200/60 flex items-center justify-between bg-white/50 backdrop-blur-sm shrink-0">
        <div class="flex items-center gap-2 text-slate-800 font-semibold">
          <span class="material-symbols-outlined text-amber-600">book_4</span>
          <span>证据溯源</span>
        </div>
        <span v-if="chatStore.activeCitations?.length" class="px-2.5 py-1 text-xs font-bold text-slate-600 bg-white border border-slate-200 rounded-md shadow-sm">
          {{ chatStore.activeCitations.length }} Sources
        </span>
      </div>
      
      <div class="flex-1 overflow-y-auto relative">
        <CitationPanel 
          v-if="chatStore.activeCitations?.length"
          :citations="chatStore.activeCitations" 
          :active-id="activeCitationId"
        />
        <div v-else class="absolute inset-0 flex flex-col items-center justify-center text-slate-400 opacity-60">
          <span class="material-symbols-outlined text-[64px] mb-4 font-light">library_books</span>
          <p class="font-medium tracking-wide">暂无引用数据</p>
        </div>
      </div>
    </aside>

    <!-- Debug Drawer -->
    <el-drawer v-model="debugVisible" title="检索链路调试" size="600px">
      <div v-if="currentDebugInfo">
        <el-collapse v-model="debugActiveNames" class="border-none">
          <el-collapse-item title="最终给到模型的 Context" name="1">
            <pre class="bg-slate-50 p-4 rounded-lg border border-slate-200 font-mono text-xs text-slate-700 whitespace-pre-wrap word-break max-h-[400px] overflow-y-auto">{{ currentDebugInfo.final_context || '无' }}</pre>
          </el-collapse-item>
          <el-collapse-item title="向量召回与 Rerank 得分" name="2">
            <pre class="bg-slate-50 p-4 rounded-lg border border-slate-200 font-mono text-xs text-slate-700 whitespace-pre-wrap word-break max-h-[400px] overflow-y-auto">{{ JSON.stringify(currentDebugInfo.reranked_chunks, null, 2) }}</pre>
          </el-collapse-item>
          <el-collapse-item title="关键词检索召回 (如果有)" name="3">
            <pre class="bg-slate-50 p-4 rounded-lg border border-slate-200 font-mono text-xs text-slate-700 whitespace-pre-wrap word-break max-h-[400px] overflow-y-auto">{{ JSON.stringify(currentDebugInfo.retrieval_results, null, 2) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, nextTick, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

import CitationPanel from '@/components/CitationPanel.vue'
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

const debugVisible = ref(false)
const currentDebugInfo = ref<any>(null)
const debugActiveNames = ref(['1', '2'])
const activeCitationId = ref<string | null>(null)

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
})

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
    return `<sup class="citation-link inline-flex items-center justify-center min-w-[1.2em] h-[1.2em] px-0.5 rounded text-[0.75em] font-sans font-bold text-blue-700 bg-blue-50 border border-blue-200/60 cursor-pointer hover:bg-blue-600 hover:text-white transition-colors mx-0.5 align-super select-none" data-id="${id}">${id}</sup>`
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
        activeCitationId.value = id
        // Scroll the citation panel to the active citation
        const citationEl = document.getElementById(`citation-card-${id}`)
        if (citationEl) {
          citationEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }
      }
      return
    }
    target = target.parentNode as HTMLElement
  }
}
</script>

<style scoped>
/* Markdown Content Styles */
.markdown-body {
  color: theme('colors.slate.800');
  font-family: theme('fontFamily.serif');
  line-height: 1.8;
  font-size: 1.125rem; /* text-lg */
}
.markdown-body :deep(p) {
  margin-bottom: 1.25rem;
}
.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  font-family: theme('fontFamily.sans');
  font-weight: 600;
  color: theme('colors.slate.900');
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
  line-height: 1.4;
}
.markdown-body :deep(h1) { font-size: 1.5rem; }
.markdown-body :deep(h2) { font-size: 1.25rem; }
.markdown-body :deep(h3) { font-size: 1.125rem; }
.markdown-body :deep(ul), .markdown-body :deep(ol) {
  margin-bottom: 1.25rem;
  padding-left: 1.5rem;
}
.markdown-body :deep(ul) { list-style-type: disc; }
.markdown-body :deep(ol) { list-style-type: decimal; }
.markdown-body :deep(li) { margin-bottom: 0.5rem; }
.markdown-body :deep(a:not(.citation-link)) {
  color: theme('colors.blue.700');
  text-decoration: none;
}
.markdown-body :deep(a:not(.citation-link):hover) {
  text-decoration: underline;
}
.markdown-body :deep(strong) {
  font-weight: 700;
  color: theme('colors.slate.900');
}
.markdown-body :deep(blockquote) {
  border-left: 4px solid theme('colors.slate.200');
  padding-left: 1rem;
  color: theme('colors.slate.600');
  font-style: italic;
  margin-bottom: 1.25rem;
}
.markdown-body :deep(code) {
  font-family: theme('fontFamily.mono');
  font-size: 0.875em;
  background-color: theme('colors.slate.100');
  padding: 0.2em 0.4em;
  border-radius: 0.25rem;
  color: theme('colors.pink.600');
}
.markdown-body :deep(pre) {
  background-color: theme('colors.slate.50');
  padding: 1rem;
  border-radius: 0.5rem;
  border: 1px solid theme('colors.slate.200');
  overflow-x: auto;
  margin-bottom: 1.25rem;
}
.markdown-body :deep(pre code) {
  background-color: transparent;
  padding: 0;
  color: theme('colors.slate.800');
}

/* Chat Input Reset */
:deep(.stitch-chat-input .el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none !important;
  font-size: 16px;
  padding: 8px 12px;
  color: theme('colors.slate.800');
  font-family: theme('fontFamily.sans');
  line-height: 1.5;
}

:deep(.stitch-chat-input .el-textarea__inner::placeholder) {
  color: theme('colors.slate.400');
}
</style>
