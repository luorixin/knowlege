<template>
  <div class="flex flex-col gap-4 p-4">
    <div
      v-for="citation in citations"
      :key="citation.citation_id"
      :id="`citation-card-${citation.citation_id}`"
      class="group relative bg-white rounded-xl border p-5 transition-all duration-300"
      :class="[
        activeId === String(citation.citation_id) 
          ? 'border-amber-400 shadow-[0_0_20px_rgba(245,158,11,0.15)] ring-1 ring-amber-400' 
          : 'border-slate-200/80 shadow-sm hover:border-blue-300 hover:shadow-md'
      ]"
    >
      <!-- Glowing indicator line when active -->
      <div 
        class="absolute left-0 top-0 bottom-0 w-1 rounded-l-xl transition-colors duration-300"
        :class="activeId === String(citation.citation_id) ? 'bg-amber-400' : 'bg-transparent group-hover:bg-blue-300'"
      ></div>

      <!-- Header: Source info -->
      <div class="flex items-start gap-3 mb-4">
        <div 
          class="shrink-0 flex items-center justify-center w-6 h-6 rounded-md text-xs font-bold font-sans"
          :class="activeId === String(citation.citation_id) ? 'bg-amber-100 text-amber-700' : 'bg-slate-100 text-slate-500'"
        >
          {{ citation.citation_id }}
        </div>
        <div class="flex-1 min-w-0">
          <h4 class="m-0 text-sm font-semibold text-slate-800 leading-snug truncate" :title="citation.doc_title">
            {{ citation.doc_title }}
          </h4>
          <div class="flex items-center gap-2 mt-1 text-xs text-slate-500 font-medium">
            <span v-if="citation.page_no" class="flex items-center gap-1">
              <span class="material-symbols-outlined text-[14px]">find_in_page</span>
              第 {{ citation.page_no }} 页
            </span>
            <span v-if="citation.section_title" class="flex items-center gap-1 max-w-[120px] truncate" :title="citation.section_title">
              <span class="material-symbols-outlined text-[14px]">segment</span>
              {{ citation.section_title }}
            </span>
          </div>
        </div>
      </div>

      <!-- Content snippet (The Evidence) -->
      <div v-if="citation.chunk_content" class="relative">
        <div class="absolute -left-2 top-0 bottom-0 w-[3px] bg-slate-200/60 rounded"></div>
        <p class="m-0 pl-3 text-[13px] leading-relaxed font-serif text-slate-700 break-words line-clamp-[8] group-hover:line-clamp-none transition-all">
          {{ citation.chunk_content }}
        </p>
      </div>

      <!-- Footer/Metadata -->
      <div class="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400 font-mono">
        <span>ID: {{ String(citation.doc_id).substring(0,8) }}...</span>
        <button 
          class="hover:text-blue-600 transition-colors flex items-center gap-1 opacity-0 group-hover:opacity-100"
          @click="openSource(citation.source_uri, citation.doc_id)"
          v-if="citation.source_uri"
        >
          <span class="material-symbols-outlined text-[14px]">open_in_new</span>
          查看原文
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AgentCitation } from '@/api/types'
import { http } from '@/api/http'

defineOptions({
  name: 'CitationPanel',
})

withDefaults(defineProps<{
  citations: AgentCitation[]
  activeId?: string | null
}>(), {
  activeId: null
})

async function openSource(uri?: string, docId?: string | number) {
  if (!uri) return
  if (uri.startsWith('local://') && docId) {
    try {
      const response = await http.get(`/api/v1/documents/${docId}/download`, { responseType: 'blob' })
      const contentType = response.headers['content-type']
      const blobUrl = window.URL.createObjectURL(new Blob([response.data], { type: contentType }))
      
      // Try to extract filename from Content-Disposition
      let filename = 'document'
      const disposition = response.headers['content-disposition']
      if (disposition) {
        const matchesUtf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
        const matches = /filename="([^"]*)"/i.exec(disposition)
        if (matchesUtf8 && matchesUtf8[1]) {
          filename = decodeURIComponent(matchesUtf8[1])
        } else if (matches && matches[1]) {
          filename = matches[1]
        }
      }

      const link = document.createElement('a')
      link.href = blobUrl
      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      link.remove()
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 1000)
    } catch (e) {
      console.error('Failed to download document', e)
    }
  } else {
    window.open(uri, '_blank')
  }
}
</script>
