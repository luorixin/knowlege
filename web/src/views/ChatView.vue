<template>
  <section class="stitch-workspace">
    <!-- Left Column: Knowledge Filters -->
    <aside class="stitch-filter-panel glass-panel">
      <div class="panel-header">
        <span class="material-symbols-outlined text-primary">filter_list</span>
        <h2>知识库过滤</h2>
      </div>

      <div class="filter-group">
        <el-form label-position="top">
          <el-form-item label="知识库">
            <el-select
              v-model="spaceId"
              class="stitch-select full-width"
              placeholder="选择知识库"
              @change="selectSpace"
            >
              <el-option
                v-for="space in knowledgeStore.spaces"
                :key="space.id"
                :label="space.name"
                :value="space.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="文档类型">
            <el-select v-model="filters.doc_type" class="stitch-select full-width" clearable>
              <el-option label="Proposal" value="proposal" />
              <el-option label="SOW" value="sow" />
              <el-option label="制度文档" value="policy" />
              <el-option label="行业研究" value="research" />
            </el-select>
          </el-form-item>
          <el-form-item label="行业">
            <el-input v-model="filters.industry" class="stitch-input" clearable placeholder="例如：金融" />
          </el-form-item>
          <el-form-item label="服务线">
            <el-input v-model="filters.service_line" class="stitch-input" clearable placeholder="例如：数据治理" />
          </el-form-item>
          <el-form-item label="起始年份">
            <el-input-number v-model="filters.year_from" class="stitch-input full-width" :min="2000" :max="2100" />
          </el-form-item>
        </el-form>
      </div>

      <div class="panel-footer">
        <button class="clear-btn" @click="chatStore.clear()">
          <span class="material-symbols-outlined">delete_sweep</span>
          清空会话
        </button>
      </div>
    </aside>

    <!-- Middle Column: Chat Interface -->
    <main class="stitch-dialog-panel">
      <div class="chat-feed" id="chat-container">
        <el-empty
          v-if="chatStore.messages.length === 0"
          :image-size="120"
          description="暂无对话，请在下方输入问题"
        />

        <article
          v-for="message in chatStore.messages"
          :key="message.id"
          class="message-wrapper"
          :class="`message-${message.role}`"
        >
          <div class="message-role" v-if="message.role === 'assistant'">
            <div class="icon-bg">
              <span class="material-symbols-outlined">smart_toy</span>
            </div>
            <span>AI Intelligence</span>
          </div>

          <div class="message-bubble" :class="{ 'is-error': message.error }">
            <div class="message-content">{{ message.content }}</div>
            <div v-if="message.role === 'assistant' && message.content.includes(noEvidenceText)" class="no-evidence">
              未在当前知识库中找到可靠依据
            </div>
            <el-collapse v-if="message.citations.length > 0" class="message-citations">
              <el-collapse-item title="展开引用来源" name="citations">
                <CitationPanel :citations="message.citations" />
              </el-collapse-item>
            </el-collapse>
          </div>
        </article>

        <article v-if="chatStore.sending" class="message-wrapper message-assistant">
          <div class="message-role">
            <div class="icon-bg">
              <span class="material-symbols-outlined">smart_toy</span>
            </div>
            <span>AI Intelligence</span>
          </div>
          <div class="message-bubble">
            <el-skeleton :rows="3" animated />
          </div>
        </article>
      </div>

      <!-- Floating Input Area -->
      <div class="floating-input-area">
        <div class="glass-input-panel">
          <button class="icon-btn">
            <span class="material-symbols-outlined">attach_file</span>
          </button>
          <el-input
            v-model="question"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            resize="none"
            placeholder="Type your query..."
            class="stitch-textarea"
            @keydown.meta.enter.prevent="send"
            @keydown.ctrl.enter.prevent="send"
          />
          <button class="send-btn" :disabled="!spaceId || !question.trim()" @click="send">
            <span class="material-symbols-outlined">send</span>
          </button>
        </div>
        <p class="disclaimer">AI can make mistakes. Check key info.</p>
      </div>
    </main>

    <!-- Right Column: Citation References -->
    <aside class="stitch-reference-panel glass-panel">
      <div class="panel-header">
        <div class="flex-row">
          <span class="material-symbols-outlined text-primary">bookmarks</span>
          <h2>引用来源</h2>
        </div>
        <span class="source-count" v-if="chatStore.activeCitations?.length">{{ chatStore.activeCitations.length }} Sources</span>
      </div>

      <div style="margin-top: 24px" v-if="chatStore.activeCitations?.length">
        <CitationPanel :citations="chatStore.activeCitations" />
      </div>
      <div v-else class="empty-citations">
        <span class="material-symbols-outlined text-[60px]">analytics</span>
        <p>暂无引用</p>
      </div>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, nextTick, watch } from 'vue'

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
</script>

<style scoped>
/* Stitch Design System Colors & Variables */
* {
  box-sizing: border-box;
}

.text-primary { color: #0058be; }
.full-width { width: 100%; }

.stitch-workspace {
  display: flex;
  height: calc(100vh - 74px); /* assuming app header is ~74px */
  background: #f7f9fb;
  color: #191c1e;
  font-family: 'Inter', sans-serif;
  overflow: hidden;
  margin: -24px -28px; /* Override app-main padding from main.css */
}

/* Scrollbars */
.stitch-workspace ::-webkit-scrollbar {
  width: 6px;
}
.stitch-workspace ::-webkit-scrollbar-track {
  background: transparent;
}
.stitch-workspace ::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}
.stitch-workspace ::-webkit-scrollbar-thumb:hover {
  background: #cbd5e1;
}

.glass-panel {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* Left Panel */
.stitch-filter-panel {
  width: 320px;
  border-right: 1px solid rgba(194, 198, 214, 0.3);
  display: flex;
  flex-direction: column;
  padding: 32px 24px;
  flex-shrink: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 32px;
}
.panel-header h2 {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
}

.flex-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group {
  flex-grow: 1;
  overflow-y: auto;
  padding-right: 8px;
}

:deep(.el-form-item__label) {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  color: #424754;
  padding-bottom: 4px;
}

:deep(.stitch-select .el-select__wrapper),
:deep(.stitch-input .el-input__wrapper) {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: none !important;
  border: 1px solid #c2c6d6;
  padding: 8px 12px;
  min-height: 44px;
}
:deep(.stitch-select .el-select__wrapper.is-focused),
:deep(.stitch-input .el-input__wrapper.is-focus) {
  border-color: #0058be;
  box-shadow: 0 0 0 1px #0058be inset !important;
}

.panel-footer {
  margin-top: auto;
  padding-top: 32px;
  border-top: 1px solid rgba(194, 198, 214, 0.3);
}

.clear-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  border-radius: 16px;
  background-color: #e6e8ea;
  color: #191c1e;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}
.clear-btn:hover {
  background-color: #ffdad6;
  color: #ba1a1a;
  transform: scale(0.98);
}

/* Middle Chat Panel */
.stitch-dialog-panel {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background-color: #f7f9fb;
}

.chat-feed {
  flex-grow: 1;
  overflow-y: auto;
  padding: 40px;
  padding-bottom: 160px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 85%;
}

.message-assistant {
  align-self: flex-start;
}

.message-user {
  align-self: flex-end;
  align-items: flex-end;
}

.message-role {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.icon-bg {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background-color: rgba(0, 88, 190, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}
.icon-bg span {
  font-size: 16px;
  color: #0058be;
}
.message-role > span {
  font-size: 12px;
  font-weight: 600;
  color: #424754;
}

.message-bubble {
  padding: 16px;
  border-radius: 16px;
  font-size: 16px;
  line-height: 1.6;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.message-assistant .message-bubble {
  background-color: #ffffff;
  border: 1px solid rgba(194, 198, 214, 0.3);
  border-top-left-radius: 0;
  color: #191c1e;
}

.message-user .message-bubble {
  background-color: #0058be;
  color: #ffffff;
  border-top-right-radius: 0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.message-bubble.is-error {
  background-color: #ffdad6;
  border-color: #ba1a1a;
  color: #93000a;
}

/* Input Area */
.floating-input-area {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 768px;
  padding: 0 24px;
}

.glass-input-panel {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  padding: 12px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.icon-btn {
  padding: 12px;
  border-radius: 16px;
  background: transparent;
  border: none;
  color: #424754;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2px;
}
.icon-btn:hover {
  background-color: #e6e8ea;
}

:deep(.stitch-textarea .el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none !important;
  font-size: 16px;
  padding: 12px 0;
  color: #191c1e;
  font-family: 'Inter', sans-serif;
}

.send-btn {
  background-color: #0058be;
  color: #ffffff;
  padding: 14px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 10px 15px -3px rgba(0, 88, 190, 0.3);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-bottom: 2px;
}
.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
}
.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}
.send-btn:disabled {
  background-color: #adc6ff;
  box-shadow: none;
  cursor: not-allowed;
}

.disclaimer {
  text-align: center;
  font-size: 11px;
  color: rgba(66, 71, 84, 0.6);
  margin-top: 12px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

/* Right Panel */
.stitch-reference-panel {
  width: 380px;
  border-left: 1px solid rgba(194, 198, 214, 0.3);
  display: flex;
  flex-direction: column;
  padding: 32px 24px;
  flex-shrink: 0;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.source-count {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  background-color: #e6e8ea;
  border-radius: 4px;
  color: #424754;
}

.empty-citations {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #b7c8e1;
  opacity: 0.5;
}
.empty-citations span {
  font-size: 64px;
  margin-bottom: 16px;
}
</style>
