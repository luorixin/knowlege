<template>
  <div class="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[560px] text-white">
    <div class="text-center pb-4 border-b border-white/[0.08] mb-4 shrink-0">
      <h1 class="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase m-0">
        Audit Log Console
      </h1>
    </div>

    <div class="cyber-panel rounded-2xl p-4 mb-5 border border-white/[0.06] shrink-0">
      <div class="flex flex-col gap-4">
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div class="flex flex-col gap-1">
            <h2 class="text-lg font-bold text-white m-0 tracking-tight">Audit Events</h2>
            <p class="text-[10px] font-mono text-slate-500 uppercase tracking-widest m-0">Tenant scoped operation trail</p>
          </div>
          <button
            @click="loadLogs"
            :disabled="loading"
            class="py-2 px-3 rounded-lg border border-white/[0.08] hover:border-neon-cyan hover:text-neon-cyan text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            <span class="material-symbols-outlined text-[14px]" :class="{ 'animate-spin': loading }">refresh</span>
            <span>Refresh</span>
          </button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-4 xl:grid-cols-8 gap-3">
          <input v-model="filters.actor_user_id" class="filter-input" placeholder="Actor ID" />
          <input v-model="filters.action" class="filter-input" placeholder="Action" />
          <input v-model="filters.resource_type" class="filter-input" placeholder="Resource Type" />
          <input v-model="filters.resource_id" class="filter-input" placeholder="Resource ID" />
          <select v-model="filters.result_status" class="filter-input cursor-pointer">
            <option value="">All Results</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="DENIED">DENIED</option>
            <option value="FAILED">FAILED</option>
          </select>
          <input v-model="filters.trace_id" class="filter-input" placeholder="Trace ID" />
          <input v-model="filters.created_from" type="datetime-local" class="filter-input" />
          <input v-model="filters.created_to" type="datetime-local" class="filter-input" />
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
        empty-text="No audit events"
        class="flex-1 w-full"
        height="100%"
        @row-click="openDetail"
      >
        <el-table-column label="Time" width="170">
          <template #default="{ row }">
            <span class="text-[10px] font-mono text-slate-400">{{ formatTime(row.created_at) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Actor" width="120">
          <template #default="{ row }">
            <span class="text-xs font-mono text-neon-cyan">{{ row.actor_user_id || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Action" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-xs font-mono text-slate-200">{{ row.action }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Result" width="120">
          <template #default="{ row }">
            <span class="text-[10px] font-mono font-bold px-2 py-1 rounded border uppercase" :class="resultClass(row.result_status)">
              {{ row.result_status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Resource" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex flex-col gap-0.5">
              <span class="text-xs text-slate-300">{{ row.resource_type || '-' }}</span>
              <span class="text-[10px] font-mono text-slate-500">{{ row.resource_id || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Request" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex flex-col gap-0.5">
              <span class="text-xs text-slate-300">{{ row.request_method || '-' }}</span>
              <span class="text-[10px] font-mono text-slate-500">{{ row.request_uri || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Trace" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-[10px] font-mono text-slate-500">{{ row.trace_id || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pt-4 flex justify-end shrink-0">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :page-size="pageSize"
          :current-page="page + 1"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="Audit Event Detail" size="640px" class="cyber-drawer">
      <div v-if="selectedLog" class="flex flex-col gap-4">
        <div class="cyber-panel rounded-xl p-4 border border-white/[0.06] grid grid-cols-2 gap-3">
          <div v-for="item in detailItems" :key="item.label" class="flex flex-col gap-1 min-w-0">
            <span class="text-[10px] font-mono text-slate-500 uppercase tracking-wider">{{ item.label }}</span>
            <span class="text-xs text-slate-200 font-mono break-all">{{ item.value || '-' }}</span>
          </div>
        </div>

        <div class="cyber-panel rounded-xl p-4 border border-white/[0.06]">
          <span class="text-[10px] font-mono text-slate-500 uppercase tracking-wider font-bold mb-2 block">Detail JSON</span>
          <pre class="m-0 bg-slate-950 rounded-lg p-3 border border-white/[0.05] overflow-auto max-h-[360px] text-[11px] leading-relaxed text-slate-300 font-mono whitespace-pre-wrap break-words">{{ formattedDetailJson }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { listAuditLogs, type AuditLogQueryParams } from '@/api/audit'
import type { AuditLogItem } from '@/api/types'

defineOptions({
  name: 'AuditLogsView',
})

const loading = ref(false)
const error = ref('')
const rows = ref<AuditLogItem[]>([])
const total = ref(0)
const page = ref(0)
const pageSize = ref(20)
const selectedLog = ref<AuditLogItem | null>(null)
const detailVisible = ref(false)

const filters = reactive<AuditLogQueryParams>({
  actor_user_id: '',
  action: '',
  resource_type: '',
  resource_id: '',
  result_status: '',
  trace_id: '',
  created_from: '',
  created_to: '',
})

const detailItems = computed(() => {
  const log = selectedLog.value
  if (!log) return []
  return [
    { label: 'ID', value: log.id },
    { label: 'Tenant', value: log.tenant_id },
    { label: 'Actor', value: log.actor_user_id },
    { label: 'Action', value: log.action },
    { label: 'Result', value: log.result_status },
    { label: 'Trace', value: log.trace_id },
    { label: 'Resource Type', value: log.resource_type },
    { label: 'Resource ID', value: log.resource_id },
    { label: 'Method', value: log.request_method },
    { label: 'URI', value: log.request_uri },
    { label: 'IP', value: log.ip_address },
    { label: 'User Agent', value: log.user_agent },
  ]
})

const formattedDetailJson = computed(() => {
  const raw = selectedLog.value?.detail_json
  if (!raw) return '{}'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
})

onMounted(() => {
  loadLogs()
})

async function loadLogs() {
  loading.value = true
  error.value = ''
  try {
    const result = await listAuditLogs(cleanParams({
      ...filters,
      page: page.value,
      size: pageSize.value,
    }))
    rows.value = result.content || []
    total.value = result.totalElements || 0
  } catch (err) {
    error.value = apiErrorMessage(err)
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 0
  loadLogs()
}

function resetFilters() {
  filters.actor_user_id = ''
  filters.action = ''
  filters.resource_type = ''
  filters.resource_id = ''
  filters.result_status = ''
  filters.trace_id = ''
  filters.created_from = ''
  filters.created_to = ''
  applyFilters()
}

function handlePageChange(nextPage: number) {
  page.value = Math.max(nextPage - 1, 0)
  loadLogs()
}

function handleSizeChange(nextSize: number) {
  pageSize.value = nextSize
  page.value = 0
  loadLogs()
}

function openDetail(row: AuditLogItem) {
  selectedLog.value = row
  detailVisible.value = true
}

function cleanParams(params: AuditLogQueryParams): AuditLogQueryParams {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== undefined && value !== null)
  ) as AuditLogQueryParams
}

function resultClass(status?: string) {
  const normalized = (status || '').toUpperCase()
  if (normalized === 'SUCCESS') return 'bg-emerald-950/30 border-emerald-500/40 text-emerald-400'
  if (normalized === 'DENIED') return 'bg-amber-950/30 border-amber-500/40 text-amber-400'
  return 'bg-red-950/30 border-red-500/40 text-red-400'
}

function formatTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}
</script>

<style scoped>
.filter-input {
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid rgb(255 255 255 / 0.08);
  background: rgb(2 6 23 / 0.9);
  padding: 0.625rem 0.75rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 0.75rem;
  color: rgb(226 232 240);
  outline: none;
}

.filter-input:focus {
  border-color: rgb(34 211 238 / 0.8);
}

.filter-input::placeholder {
  color: rgb(100 116 139);
}
</style>
