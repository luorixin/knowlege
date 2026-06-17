<template>
  <div class="citation-panel">
    <div v-if="title" class="citation-panel__header">
      <span>{{ title }}</span>
      <el-tag v-if="citations.length" size="small" effect="plain">{{ citations.length }}</el-tag>
    </div>

    <el-empty
      v-if="citations.length === 0"
      :image-size="96"
      description="暂无引用来源"
    />

    <el-collapse v-else class="citation-panel__list">
      <el-collapse-item
        v-for="citation in citations"
        :key="citation.citation_id"
        :name="citation.citation_id"
      >
        <template #title>
          <div class="citation-title">
            <el-tag size="small" type="info" effect="plain">引用{{ citation.citation_id }}</el-tag>
            <span>{{ citation.doc_title }}</span>
          </div>
        </template>
        <dl class="citation-meta">
          <div>
            <dt>文档 ID</dt>
            <dd>{{ citation.doc_id }}</dd>
          </div>
          <div>
            <dt>页码</dt>
            <dd>{{ citation.page_no ?? '-' }}</dd>
          </div>
          <div>
            <dt>章节</dt>
            <dd>{{ citation.section_title || '-' }}</dd>
          </div>
        </dl>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup lang="ts">
import type { AgentCitation } from '@/api/types'

defineOptions({
  name: 'CitationPanel',
})

withDefaults(defineProps<{
  title?: string
  citations: AgentCitation[]
}>(), {
  title: '',
})
</script>
