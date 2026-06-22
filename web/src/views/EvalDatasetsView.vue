<template>
  <div class="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[500px] text-white">
    <!-- Outer Banner title -->
    <div class="text-center pb-4 border-b border-white/[0.08] mb-6 shrink-0">
      <h1 class="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase m-0">
        System Evaluation Protocol
      </h1>
    </div>

    <!-- Page Header -->
    <div class="cyber-panel rounded-2xl p-4 mb-6 border border-white/[0.06] flex flex-col md:flex-row md:items-center justify-between gap-4 shrink-0">
      <div class="flex flex-col gap-1">
        <h2 class="text-lg font-bold text-white m-0 tracking-tight">AI Evaluation Datasets</h2>
        <p class="text-[10px] font-mono text-slate-500 uppercase tracking-widest m-0 flex items-center gap-1">
          <span class="material-symbols-outlined text-[12px]">analytics</span>
          METRICS: RECALL, CITATION ACCURACY, REFUSAL, SECURITY
        </p>
      </div>
      <button @click="loadSpaces" :disabled="loadingSpaces" class="py-2 px-4 rounded-lg bg-slate-900 border border-slate-700 hover:border-neon-cyan hover:text-neon-cyan text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50 outline-none uppercase tracking-wider">
        <span class="material-symbols-outlined text-[14px]" :class="{'animate-spin': loadingSpaces}">sync</span>
        Resync Nexuses
      </button>
    </div>

    <div class="flex-1 min-h-0 overflow-y-auto scrollbar-thin pr-2 flex flex-col gap-6">
      
      <!-- Top Forms Grid -->
      <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
        
        <!-- Create Dataset -->
        <div class="cyber-panel rounded-2xl p-5 border border-white/[0.06] relative overflow-hidden">
          <div class="absolute right-[-20px] top-[-20px] text-blue-900/10 text-[100px] font-black material-symbols-outlined pointer-events-none">dataset</div>
          <h3 class="text-xs font-mono font-bold uppercase tracking-widest text-white m-0 mb-4 pb-3 border-b border-white/[0.05] flex items-center gap-2 relative z-10">
            <span class="material-symbols-outlined text-blue-400 text-[16px]">add_box</span>
            Initialize Dataset
          </h3>
          <el-form ref="datasetFormRef" :model="datasetForm" :rules="datasetRules" label-position="top" class="cyber-form relative z-10">
            <el-form-item label="Target Nexus" prop="spaceId" class="cyber-form-item">
              <div class="relative w-full">
                 <select
                   v-model="datasetForm.spaceId"
                   class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-blue-400 focus:border-blue-500 focus:outline-none pr-8 appearance-none cursor-pointer uppercase tracking-wider"
                 >
                   <option v-for="space in knowledgeStore.spaces" :key="space.id" :value="space.id">
                     {{ space.name }}
                   </option>
                 </select>
                 <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-[16px] text-blue-400 pointer-events-none">expand_more</span>
              </div>
            </el-form-item>
            <el-form-item label="Dataset Alias" prop="name" class="cyber-form-item">
              <input v-model="datasetForm.name" type="text" class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-white focus:border-blue-500 focus:outline-none transition-colors placeholder-slate-600" placeholder="E.g. Fin-QA-Test" />
            </el-form-item>
            <el-form-item label="Parameters / Description" class="cyber-form-item">
              <textarea v-model="datasetForm.description" rows="2" class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-white focus:border-blue-500 focus:outline-none transition-colors placeholder-slate-600 resize-none" placeholder="Optional context..."></textarea>
            </el-form-item>
            <div class="flex justify-end mt-4">
              <button class="px-5 py-2 bg-blue-950/30 text-blue-400 border border-blue-500/50 hover:bg-blue-500 hover:text-slate-900 rounded text-[10px] font-mono uppercase font-bold transition-colors flex items-center gap-1.5 cursor-pointer outline-none disabled:opacity-50" :disabled="creatingDataset" @click="createDataset" type="button">
                <span v-if="creatingDataset" class="material-symbols-outlined text-[14px] animate-spin">sync</span>
                <span v-else class="material-symbols-outlined text-[14px]">save</span>
                Commit Dataset
              </button>
            </div>
          </el-form>
        </div>

        <!-- Add Eval Case -->
        <div class="cyber-panel rounded-2xl p-5 border border-white/[0.06] relative overflow-hidden">
          <div class="absolute right-[-20px] top-[-20px] text-amber-900/10 text-[100px] font-black material-symbols-outlined pointer-events-none">quiz</div>
          <h3 class="text-xs font-mono font-bold uppercase tracking-widest text-white m-0 mb-4 pb-3 border-b border-white/[0.05] flex items-center gap-2 relative z-10">
            <span class="material-symbols-outlined text-amber-400 text-[16px]">add_task</span>
            Inject Test Case
          </h3>
          <el-form ref="caseFormRef" :model="caseForm" :rules="caseRules" label-position="top" class="cyber-form relative z-10">
            <el-form-item label="Target Dataset" prop="datasetId" class="cyber-form-item">
              <div class="relative w-full">
                 <select
                   v-model="caseForm.datasetId"
                   class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-amber-400 focus:border-amber-500 focus:outline-none pr-8 appearance-none cursor-pointer uppercase tracking-wider"
                 >
                   <option v-for="dataset in datasets" :key="dataset.id" :value="dataset.id">
                     {{ dataset.name }}
                   </option>
                 </select>
                 <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-[16px] text-amber-400 pointer-events-none">expand_more</span>
              </div>
            </el-form-item>
            <el-form-item label="Query Vector" prop="question" class="cyber-form-item">
              <textarea v-model="caseForm.question" rows="2" class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-white focus:border-amber-500 focus:outline-none transition-colors placeholder-slate-600 resize-none" placeholder="Input the question..."></textarea>
            </el-form-item>
            <el-form-item label="Expected Output" class="cyber-form-item">
              <textarea v-model="caseForm.expectedAnswer" rows="2" class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-white focus:border-amber-500 focus:outline-none transition-colors placeholder-slate-600 resize-none" placeholder="Standard answer..."></textarea>
            </el-form-item>
            
            <div class="grid grid-cols-2 gap-4">
              <el-form-item label="Target Doc IDs" class="cyber-form-item mb-0">
                <input v-model="caseForm.expectedDocIds" type="text" class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-white focus:border-amber-500 focus:outline-none transition-colors placeholder-slate-600" placeholder="Comma separated" />
              </el-form-item>
              <el-form-item label="Target Chunk IDs" class="cyber-form-item mb-0">
                <input v-model="caseForm.expectedChunkIds" type="text" class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-white focus:border-amber-500 focus:outline-none transition-colors placeholder-slate-600" placeholder="Comma separated" />
              </el-form-item>
            </div>
            
            <div class="flex items-center justify-between mt-4">
              <label class="flex items-center gap-2 cursor-pointer group">
                <input type="checkbox" v-model="caseForm.expectNoAnswer" class="hidden" />
                <div class="w-4 h-4 rounded border flex items-center justify-center transition-colors" :class="caseForm.expectNoAnswer ? 'bg-amber-500 border-amber-500' : 'bg-slate-900 border-slate-600 group-hover:border-amber-500'">
                  <span v-if="caseForm.expectNoAnswer" class="material-symbols-outlined text-[12px] text-slate-900 font-bold">check</span>
                </div>
                <span class="text-[10px] font-mono text-slate-400 uppercase tracking-widest group-hover:text-amber-400 transition-colors">Expect Refusal</span>
              </label>
              <button class="px-5 py-2 bg-amber-950/30 text-amber-500 border border-amber-500/50 hover:bg-amber-500 hover:text-slate-900 rounded text-[10px] font-mono uppercase font-bold transition-colors flex items-center gap-1.5 cursor-pointer outline-none disabled:opacity-50" :disabled="creatingCase" @click="createCase" type="button">
                <span v-if="creatingCase" class="material-symbols-outlined text-[14px] animate-spin">sync</span>
                <span v-else class="material-symbols-outlined text-[14px]">add</span>
                Inject Case
              </button>
            </div>
          </el-form>
        </div>
      </div>

      <!-- Datasets Table -->
      <div class="cyber-panel rounded-2xl p-5 border border-white/[0.06]">
        <h3 class="text-xs font-mono font-bold uppercase tracking-widest text-white m-0 mb-4 pb-3 border-b border-white/[0.05] flex items-center gap-2">
          <span class="material-symbols-outlined text-neon-cyan text-[16px]">list_alt</span>
          Registry of Eval Datasets
        </h3>
        
        <el-table
          v-loading="loadingSpaces"
          :data="datasets"
          row-key="id"
          empty-text="No datasets allocated."
          class="w-full"
        >
          <el-table-column prop="name" label="Dataset Alias" min-width="220">
            <template #default="{ row }">
              <span class="font-medium text-slate-200">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="space_id" label="Nexus ID" width="120">
            <template #default="{ row }">
              <span class="text-[10px] font-mono text-slate-400 uppercase">{{ row.space_id }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="Integrity" width="120">
            <template #default="{ row }">
              <span class="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-600 text-[9px] font-mono uppercase font-bold tracking-wider">
                {{ row.status || 'READY' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="Protocols" width="220" align="right">
            <template #default="{ row }">
              <div class="flex items-center justify-end gap-2">
                <button class="px-2 py-1 bg-slate-800 border border-slate-600 text-slate-300 hover:bg-slate-700 hover:text-white rounded text-[10px] font-mono uppercase transition-colors outline-none cursor-pointer" 
                        @click="viewCases(row)">
                  Inspect
                </button>
                <button class="px-2 py-1 bg-green-950/30 border border-green-500/50 text-green-400 hover:bg-green-500 hover:text-slate-900 rounded text-[10px] font-mono font-bold uppercase transition-colors outline-none flex items-center gap-1 cursor-pointer disabled:opacity-50" 
                        :disabled="runningDatasetId === row.id"
                        @click="run(row.id)">
                  <span v-if="runningDatasetId === row.id" class="material-symbols-outlined text-[12px] animate-spin">sync</span>
                  <span v-else class="material-symbols-outlined text-[12px]">play_arrow</span>
                  Execute
                </button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Report Section -->
      <transition name="fade-slide">
        <div v-if="latestResult" class="cyber-panel rounded-2xl p-5 border border-purple-500/30 relative overflow-hidden">
          <div class="absolute inset-0 bg-gradient-to-br from-purple-900/10 to-transparent pointer-events-none"></div>
          
          <div class="flex items-end justify-between border-b border-purple-500/20 pb-3 mb-4 relative z-10">
            <div>
              <h3 class="text-xs font-mono font-bold uppercase tracking-widest text-purple-400 m-0 mb-1 flex items-center gap-2">
                <span class="material-symbols-outlined text-[16px]">insert_chart</span>
                Evaluation Telemetry
              </h3>
              <p class="text-[9px] font-mono text-slate-500 uppercase tracking-widest m-0">Run ID: {{ latestResult.run_id }}</p>
            </div>
            <button @click="latestResult = null" class="text-slate-500 hover:text-white transition-colors cursor-pointer outline-none">
              <span class="material-symbols-outlined text-[18px]">close</span>
            </button>
          </div>

          <!-- Metrics Strip -->
          <div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6 relative z-10">
            <div class="bg-slate-950/50 border border-slate-800 rounded-lg p-3 flex flex-col items-center justify-center gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest text-center">Recall@K</span>
              <strong class="text-2xl font-bold text-neon-cyan drop-shadow-[0_0_8px_rgba(0,240,255,0.4)]">{{ percent(latestResult.metrics.recall_at_k) }}</strong>
            </div>
            <div class="bg-slate-950/50 border border-slate-800 rounded-lg p-3 flex flex-col items-center justify-center gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest text-center">Precision@K</span>
              <strong class="text-2xl font-bold text-blue-400 drop-shadow-[0_0_8px_rgba(96,165,250,0.4)]">{{ percent(latestResult.metrics.precision_at_k) }}</strong>
            </div>
            <div class="bg-slate-950/50 border border-slate-800 rounded-lg p-3 flex flex-col items-center justify-center gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest text-center">MRR</span>
              <strong class="text-2xl font-bold text-amber-400 drop-shadow-[0_0_8px_rgba(251,191,36,0.4)]">{{ latestResult.metrics.mrr.toFixed(2) }}</strong>
            </div>
            <div class="bg-slate-950/50 border border-slate-800 rounded-lg p-3 flex flex-col items-center justify-center gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest text-center">Citation Acc</span>
              <strong class="text-2xl font-bold text-green-400 drop-shadow-[0_0_8px_rgba(74,222,128,0.4)]">{{ percent(latestResult.metrics.citation_accuracy) }}</strong>
            </div>
            <div class="bg-slate-950/50 border border-slate-800 rounded-lg p-3 flex flex-col items-center justify-center gap-1">
              <span class="text-[9px] font-mono text-slate-500 uppercase tracking-widest text-center">Security Violations</span>
              <strong class="text-2xl font-bold text-red-500 drop-shadow-[0_0_8px_rgba(239,68,68,0.4)]">{{ latestResult.metrics.permission_violation_count }}</strong>
            </div>
          </div>

          <!-- Case Details Table -->
          <div class="border border-white/[0.05] rounded-xl overflow-hidden relative z-10">
            <el-table :data="latestResult.cases" row-key="caseId" class="w-full">
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div class="p-4 bg-slate-900/50 border-t border-white/[0.05]">
                    <div class="grid grid-cols-1 gap-3 text-xs font-mono text-slate-300">
                      <div class="flex flex-col gap-1">
                        <strong class="text-neon-cyan uppercase tracking-wider text-[10px]">Actual Output:</strong>
                        <span class="bg-slate-950 p-2 rounded border border-white/[0.05]">{{ row.actual_answer || 'N/A' }}</span>
                      </div>
                      <div class="flex items-center gap-6">
                        <p class="m-0"><strong class="text-slate-500 uppercase tracking-wider text-[10px] mr-2">Expected Refusal:</strong> <span :class="row.expect_no_answer ? 'text-amber-400' : 'text-slate-400'">{{ row.expect_no_answer ? 'YES' : 'NO' }}</span></p>
                        <p class="m-0"><strong class="text-slate-500 uppercase tracking-wider text-[10px] mr-2">Refusal Accurate:</strong> <span :class="row.no_answer_correct ? 'text-green-400' : 'text-red-400'">{{ row.no_answer_correct ? 'YES' : 'NO' }}</span></p>
                      </div>
                      <div class="flex flex-col gap-1">
                        <strong class="text-slate-500 uppercase tracking-wider text-[10px]">Recalled Chunks:</strong>
                        <span class="bg-slate-950 p-2 rounded border border-white/[0.05] break-all">{{ row.retrieved_chunk_ids?.join(', ') || 'None' }}</span>
                      </div>
                      <div class="flex flex-col gap-1">
                        <strong class="text-slate-500 uppercase tracking-wider text-[10px]">Cited Docs:</strong>
                        <span class="bg-slate-950 p-2 rounded border border-white/[0.05] break-all">{{ row.cited_doc_ids?.join(', ') || 'None' }}</span>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="question" label="Query Vector" min-width="260" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="text-slate-200">{{ row.question }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="recall_hit" label="Recall Hit" width="110">
                <template #default="{ row }">
                  <span class="px-2 py-0.5 rounded text-[9px] font-mono uppercase font-bold tracking-wider"
                        :class="row.recall_hit ? 'bg-green-950/50 text-green-400 border border-green-500/50' : 'bg-red-950/50 text-red-400 border border-red-500/50'">
                    {{ row.recall_hit ? 'YES' : 'NO' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="citation_accuracy" label="Citation Acc" width="120">
                <template #default="{ row }">
                  <span class="px-2 py-0.5 rounded text-[9px] font-mono uppercase font-bold tracking-wider"
                        :class="row.citation_accuracy === 1 ? 'bg-green-950/50 text-green-400 border border-green-500/50' : 'bg-orange-950/50 text-orange-400 border border-orange-500/50'">
                    {{ percent(row.citation_accuracy) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="permission_violation" label="Sec Violate" width="110">
                <template #default="{ row }">
                  <span class="px-2 py-0.5 rounded text-[9px] font-mono uppercase font-bold tracking-wider"
                        :class="row.permission_violation ? 'bg-red-950/50 text-red-400 border border-red-500/50' : 'bg-green-950/50 text-green-400 border border-green-500/50'">
                    {{ row.permission_violation ? 'YES' : 'NO' }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </transition>
    </div>

    <!-- Cases Dialog -->
    <el-dialog v-model="casesDialogVisible" title="Cases List" width="800px" class="cyber-dialog" :show-close="false">
      <template #header="{ titleId, titleClass }">
         <div class="flex items-center justify-between border-b border-white/[0.08] pb-3 mb-2">
            <h4 :id="titleId" :class="titleClass" class="!m-0 !text-sm font-mono uppercase tracking-widest text-white flex items-center gap-2">
              <span class="material-symbols-outlined text-neon-cyan text-[18px]">list</span>
              {{ currentDataset?.name }} - Case Vectors
            </h4>
            <button @click="casesDialogVisible = false" class="text-slate-500 hover:text-white cursor-pointer outline-none">
              <span class="material-symbols-outlined text-[18px]">close</span>
            </button>
         </div>
      </template>

      <div class="border border-white/[0.05] rounded-xl overflow-hidden">
        <el-table :data="currentCases" v-loading="loadingCases" empty-text="No vectors injected." class="w-full">
          <el-table-column prop="question" label="Query Vector" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="text-slate-200">{{ row.question }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="expected_answer" label="Expected Output" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="text-[11px] font-mono text-slate-400">{{ row.expected_answer || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Expect Refusal" width="120" align="center">
            <template #default="{ row }">
              <span class="px-2 py-0.5 rounded text-[9px] font-mono uppercase font-bold tracking-wider"
                    :class="row.expect_no_answer ? 'bg-amber-950/50 text-amber-400 border border-amber-500/50' : 'bg-slate-800 text-slate-400 border border-slate-600'">
                {{ row.expect_no_answer ? 'YES' : 'NO' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <div class="flex justify-end pt-3 border-t border-white/[0.08] mt-4">
          <button @click="casesDialogVisible = false" class="px-4 py-2 rounded bg-slate-800 text-white text-xs font-mono border border-slate-700 hover:bg-slate-700 transition-colors cursor-pointer outline-none uppercase tracking-wider">
            Close
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  createEvalCase,
  createEvalDataset,
  listEvalDatasets,
  listEvalCases,
  runEval,
} from '@/api/eval'
import { apiErrorMessage } from '@/api/http'
import type { EntityId, EvalDataset, EvalCase, EvalRunResult } from '@/api/types'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'EvalDatasetsView',
})

const userStore = useUserStore()
const knowledgeStore = useKnowledgeStore()
const datasetFormRef = ref<FormInstance>()
const caseFormRef = ref<FormInstance>()
const datasets = ref<EvalDataset[]>([])
const latestResult = ref<EvalRunResult | null>(null)
const loadingSpaces = ref(false)
const creatingDataset = ref(false)
const creatingCase = ref(false)
const runningDatasetId = ref<EntityId | null>(null)

const casesDialogVisible = ref(false)
const currentDataset = ref<EvalDataset | null>(null)
const currentCases = ref<EvalCase[]>([])
const loadingCases = ref(false)

const datasetForm = reactive({
  spaceId: knowledgeStore.selectedSpaceId,
  name: '',
  description: '',
})

const caseForm = reactive({
  datasetId: null as EntityId | null,
  question: '',
  expectedAnswer: '',
  expectedDocIds: '',
  expectedChunkIds: '',
  expectNoAnswer: false,
})

const datasetRules: FormRules = {
  spaceId: [{ required: true, message: 'Select a Nexus', trigger: 'change' }],
  name: [{ required: true, message: 'Alias required', trigger: 'blur' }],
}

const caseRules: FormRules = {
  datasetId: [{ required: true, message: 'Select Dataset', trigger: 'change' }],
  question: [{ required: true, message: 'Vector required', trigger: 'blur' }],
}

onMounted(async () => {
  await loadSpaces()
  await fetchDatasets()
})

async function fetchDatasets() {
  try {
    datasets.value = await listEvalDatasets()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  }
}

async function loadSpaces() {
  if (!userStore.tenantId) return
  loadingSpaces.value = true
  try {
    await knowledgeStore.ensureSpaces(userStore.tenantId)
    datasetForm.spaceId = knowledgeStore.selectedSpaceId || knowledgeStore.spaces[0]?.id || null
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    loadingSpaces.value = false
  }
}

async function createDataset() {
  if (!datasetFormRef.value || !userStore.tenantId || !datasetForm.spaceId) return
  await datasetFormRef.value.validate()
  creatingDataset.value = true
  try {
    const dataset = await createEvalDataset({
      tenant_id: userStore.tenantId,
      space_id: datasetForm.spaceId,
      name: datasetForm.name,
      description: datasetForm.description,
    })
    datasets.value = [dataset, ...datasets.value]
    caseForm.datasetId = dataset.id
    datasetForm.name = ''
    datasetForm.description = ''
    ElMessage.success('Eval Dataset initialized.')
    await fetchDatasets()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    creatingDataset.value = false
  }
}

async function createCase() {
  if (!caseFormRef.value || !caseForm.datasetId) return
  await caseFormRef.value.validate()
  creatingCase.value = true
  try {
    await createEvalCase({
      dataset_id: caseForm.datasetId,
      question: caseForm.question,
      expected_answer: caseForm.expectedAnswer,
      expected_doc_ids: parseIds(caseForm.expectedDocIds),
      expected_chunk_ids: parseIds(caseForm.expectedChunkIds),
      expect_no_answer: caseForm.expectNoAnswer,
    })
    caseForm.question = ''
    caseForm.expectedAnswer = ''
    caseForm.expectedDocIds = ''
    caseForm.expectedChunkIds = ''
    caseForm.expectNoAnswer = false
    ElMessage.success('Vector injected.')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    creatingCase.value = false
  }
}

async function run(datasetId: EntityId) {
  runningDatasetId.value = datasetId
  try {
    latestResult.value = await runEval(datasetId, 20)
    ElMessage.success('Evaluation sequence completed.')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    runningDatasetId.value = null
  }
}

async function viewCases(row: EvalDataset) {
  currentDataset.value = row
  casesDialogVisible.value = true
  loadingCases.value = true
  try {
    currentCases.value = await listEvalCases(row.id)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    loadingCases.value = false
  }
}

function parseIds(value: string): EntityId[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}

function percent(value: number): string {
  return `${Math.round(value * 100)}%`
}
</script>

<style>
/* Transitions */
.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.5s ease;
}
.fade-slide-enter-from, .fade-slide-leave-to {
  opacity: 0;
  transform: translateY(16px);
}

/* Scrollbar */
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

/* Cyber Form Element Plus Overrides */
.cyber-form-item .el-form-item__label {
  font-size: 10px !important;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace !important;
  color: #94a3b8 !important;
  text-transform: uppercase !important;
  letter-spacing: 0.05em !important;
  padding-bottom: 4px !important;
  line-height: 1 !important;
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
</style>
