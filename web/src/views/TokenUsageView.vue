<template>
  <div class="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[560px] text-white">
    <div class="text-center pb-4 border-b border-white/[0.08] mb-4 shrink-0">
      <h1 class="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase m-0">
        LLM Token Usage Console
      </h1>
    </div>

    <div class="cyber-panel rounded-2xl p-4 mb-5 border border-white/[0.06] shrink-0">
      <div class="flex flex-col gap-4">
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div class="flex flex-col gap-1">
            <h2 class="text-lg font-bold text-white m-0 tracking-tight">Token Consumption</h2>
            <p class="text-[10px] font-mono text-slate-500 uppercase tracking-widest m-0">Monitor external model API usage</p>
          </div>
          <button
            @click="loadData"
            :disabled="loading"
            class="py-2 px-3 rounded-lg border border-white/[0.08] hover:border-neon-cyan hover:text-neon-cyan text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            <span class="material-symbols-outlined text-[14px]" :class="{ 'animate-spin': loading }">refresh</span>
            <span>Refresh</span>
          </button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-4 xl:grid-cols-8 gap-3">
          <input v-model="filters.model_provider" class="w-full bg-slate-900/50 border border-white/[0.08] rounded-lg px-3 py-1.5 text-xs text-white placeholder:text-slate-600 focus:border-neon-cyan focus:outline-none transition-colors" placeholder="Provider (e.g. OPENAI)" />
          <input v-model="filters.model_name" class="w-full bg-slate-900/50 border border-white/[0.08] rounded-lg px-3 py-1.5 text-xs text-white placeholder:text-slate-600 focus:border-neon-cyan focus:outline-none transition-colors" placeholder="Model Name" />
          <input v-model="filters.created_from" type="datetime-local" class="w-full bg-slate-900/50 border border-white/[0.08] rounded-lg px-3 py-1.5 text-xs text-white placeholder:text-slate-600 focus:border-neon-cyan focus:outline-none transition-colors" />
          <input v-model="filters.created_to" type="datetime-local" class="w-full bg-slate-900/50 border border-white/[0.08] rounded-lg px-3 py-1.5 text-xs text-white placeholder:text-slate-600 focus:border-neon-cyan focus:outline-none transition-colors" />
        </div>

        <div class="flex items-center justify-end gap-2">
          <button
            @click="resetFilters"
            class="px-3 py-2 rounded-lg border border-white/[0.08] text-slate-400 hover:text-white hover:border-white/[0.2] text-xs font-mono transition-all cursor-pointer"
          >
            Reset
          </button>
          <button
            @click="applyFilters"
            class="px-3 py-2 rounded-lg bg-cyan-400 text-slate-950 hover:bg-neon-cyan text-xs font-mono font-bold transition-all cursor-pointer"
          >
            Search
          </button>
        </div>
      </div>
    </div>

    <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-950/40 border border-red-500/30 text-red-400 text-xs font-mono flex items-center gap-2">
      <span class="material-symbols-outlined text-[16px]">error</span>
      {{ error }}
    </div>

    <div class="cyber-panel rounded-2xl p-4 border border-white/[0.06] flex-1 overflow-hidden flex flex-col min-h-0">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        empty-text="No token usage data"
        class="flex-1 w-full"
        height="100%"
      >
        <el-table-column label="Time" width="170">
          <template #default="{ row }">
            <span class="text-[10px] font-mono text-slate-400">{{ formatTime(row.created_at) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Provider" width="120">
          <template #default="{ row }">
            <span class="text-xs font-mono font-bold text-slate-200">{{ row.model_provider || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Type" width="100">
          <template #default="{ row }">
            <span class="text-[10px] font-mono uppercase px-1.5 py-0.5 rounded border inline-block w-fit" 
                  :class="row.usage_type === 'EMBEDDING' ? 'bg-cyan-950/30 border-neon-cyan/50 text-neon-cyan' : 'bg-purple-950/30 border-purple-500/50 text-purple-400'">
              {{ row.usage_type || 'CHAT' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Model" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-xs font-mono text-neon-cyan">{{ row.model_name || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Prompt Tokens" width="120" align="right">
          <template #default="{ row }">
            <span class="text-[11px] font-mono text-slate-400">{{ row.prompt_tokens || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Completion Tokens" width="140" align="right">
          <template #default="{ row }">
            <span class="text-[11px] font-mono text-slate-400">{{ row.completion_tokens || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Total Tokens" width="120" align="right">
          <template #default="{ row }">
            <span class="text-[11px] font-mono font-bold text-purple-400">{{ row.total_tokens || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Latency (ms)" width="120" align="right">
          <template #default="{ row }">
            <span class="text-[11px] font-mono" :class="row.latency_ms > 5000 ? 'text-amber-400' : 'text-slate-400'">
              {{ row.latency_ms || 0 }}ms
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pt-4 flex justify-end shrink-0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[20, 50, 100]"
          :total="totalElements"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          class="cyber-pagination"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listTokenUsage } from '@/api/token-usage'
import type { TokenUsageItem, TokenUsageFilters } from '@/api/token-usage'

const loading = ref(false)
const error = ref('')

const rows = ref<TokenUsageItem[]>([])
const page = ref(1)
const size = ref(20)
const totalElements = ref(0)

const filters = ref<TokenUsageFilters>({
  model_provider: '',
  model_name: '',
  created_from: '',
  created_to: ''
})

const formatTime = (ts: string | null) => {
  if (!ts) return '-'
  return new Date(ts).toLocaleString()
}

const loadData = async () => {
  loading.value = true
  error.value = ''
  try {
    const data = await listTokenUsage(page.value - 1, size.value, {
      ...filters.value
    })
    rows.value = data.content
    totalElements.value = data.totalElements
  } catch (e: any) {
    error.value = e.message || 'Failed to load token usage'
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  page.value = 1
  loadData()
}

const resetFilters = () => {
  filters.value = {
    model_provider: '',
    model_name: '',
    created_from: '',
    created_to: ''
  }
  applyFilters()
}

const handleSizeChange = () => {
  page.value = 1
  loadData()
}

const handlePageChange = () => {
  loadData()
}

onMounted(() => {
  loadData()
})
</script>
