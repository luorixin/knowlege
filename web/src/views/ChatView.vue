<template>
  <section class="chat-workspace">
    <aside class="chat-filter-panel">
      <h2>问答设置</h2>
      <el-form label-position="top">
        <el-form-item label="知识库">
          <el-select
            v-model="spaceId"
            class="full-width"
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
          <el-select v-model="filters.doc_type" class="full-width" clearable>
            <el-option label="Proposal" value="proposal" />
            <el-option label="SOW" value="sow" />
            <el-option label="制度文档" value="policy" />
            <el-option label="行业研究" value="research" />
          </el-select>
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="filters.industry" clearable placeholder="例如：金融" />
        </el-form-item>
        <el-form-item label="服务线">
          <el-input v-model="filters.service_line" clearable placeholder="例如：数据治理" />
        </el-form-item>
        <el-form-item label="起始年份">
          <el-input-number v-model="filters.year_from" class="full-width" :min="2000" :max="2100" />
        </el-form-item>
      </el-form>
      <el-button class="full-width" :icon="Delete" @click="chatStore.clear()">清空会话</el-button>
    </aside>

    <main class="chat-dialog-panel">
      <div class="chat-dialog-header">
        <div>
          <h2>智能问答</h2>
          <p>{{ selectedSpace?.name || '请选择知识库' }}</p>
        </div>
        <el-tag v-if="chatStore.sessionId" effect="plain">Session {{ chatStore.sessionId }}</el-tag>
      </div>

      <div class="message-list">
        <el-empty
          v-if="chatStore.messages.length === 0"
          :image-size="120"
          description="暂无对话"
        />

        <article
          v-for="message in chatStore.messages"
          :key="message.id"
          class="message-item"
          :class="[`message-item--${message.role}`, { 'message-item--error': message.error }]"
        >
          <div class="message-role">{{ message.role === 'user' ? '你' : '知识库智能体' }}</div>
          <div class="message-content">{{ message.content }}</div>
          <div v-if="message.role === 'assistant' && message.content.includes(noEvidenceText)" class="no-evidence">
            未在当前知识库中找到可靠依据
          </div>
          <el-collapse v-if="message.citations.length > 0" class="message-citations">
            <el-collapse-item title="展开引用来源" name="citations">
              <CitationPanel :citations="message.citations" />
            </el-collapse-item>
          </el-collapse>
        </article>

        <article v-if="chatStore.sending" class="message-item message-item--assistant">
          <div class="message-role">知识库智能体</div>
          <el-skeleton :rows="3" animated />
        </article>
      </div>

      <div class="question-box">
        <el-input
          v-model="question"
          type="textarea"
          :rows="4"
          resize="none"
          placeholder="输入问题，例如：请总结金融行业数据治理 proposal 的常见结构"
          @keydown.meta.enter.prevent="send"
          @keydown.ctrl.enter.prevent="send"
        />
        <div class="question-actions">
          <span>Ctrl / Command + Enter 发送</span>
          <el-button
            type="primary"
            :icon="Promotion"
            :loading="chatStore.sending"
            :disabled="!spaceId || !question.trim()"
            @click="send"
          >
            发送
          </el-button>
        </div>
      </div>
    </main>

    <aside class="chat-reference-panel">
      <CitationPanel title="引用来源" :citations="chatStore.activeCitations" />
    </aside>
  </section>
</template>

<script setup lang="ts">
import { Delete, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import CitationPanel from '@/components/CitationPanel.vue'
import type { SearchFilters } from '@/api/types'
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
const spaceId = ref<number | null>(knowledgeStore.selectedSpaceId)
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

function selectSpace(value: number) {
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
