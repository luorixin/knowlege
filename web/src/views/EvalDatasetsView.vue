<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>评估数据集</h2>
        <p>规则评估 Recall、引用准确率、拒答和权限安全</p>
      </div>
      <el-button :icon="Refresh" :loading="loadingSpaces" @click="loadSpaces">刷新知识库</el-button>
    </div>

    <div class="eval-grid">
      <section class="tool-panel">
        <h3>创建数据集</h3>
        <el-form ref="datasetFormRef" :model="datasetForm" :rules="datasetRules" label-position="top">
          <el-form-item label="知识库" prop="spaceId">
            <el-select v-model="datasetForm.spaceId" class="full-width">
              <el-option
                v-for="space in knowledgeStore.spaces"
                :key="space.id"
                :label="space.name"
                :value="space.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="名称" prop="name">
            <el-input v-model="datasetForm.name" />
          </el-form-item>
          <el-form-item label="说明">
            <el-input v-model="datasetForm.description" type="textarea" :rows="2" />
          </el-form-item>
          <el-button type="primary" :loading="creatingDataset" @click="createDataset">创建</el-button>
        </el-form>
      </section>

      <section class="tool-panel">
        <h3>新增评估问题</h3>
        <el-form ref="caseFormRef" :model="caseForm" :rules="caseRules" label-position="top">
          <el-form-item label="数据集" prop="datasetId">
            <el-select v-model="caseForm.datasetId" class="full-width" placeholder="选择刚创建的数据集">
              <el-option
                v-for="dataset in datasets"
                :key="dataset.id"
                :label="dataset.name"
                :value="dataset.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="问题" prop="question">
            <el-input v-model="caseForm.question" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="标准答案">
            <el-input v-model="caseForm.expectedAnswer" type="textarea" :rows="2" />
          </el-form-item>
          <div class="form-row">
            <el-form-item label="期望文档 ID">
              <el-input v-model="caseForm.expectedDocIds" placeholder="逗号分隔" />
            </el-form-item>
            <el-form-item label="期望 Chunk ID">
              <el-input v-model="caseForm.expectedChunkIds" placeholder="逗号分隔" />
            </el-form-item>
          </div>
          <el-form-item>
            <el-checkbox v-model="caseForm.expectNoAnswer">期望拒答</el-checkbox>
          </el-form-item>
          <el-button :loading="creatingCase" @click="createCase">添加问题</el-button>
        </el-form>
      </section>
    </div>

    <section class="tool-panel">
      <div class="section-header compact">
        <div>
          <h3>运行评估</h3>
          <p>当前页面仅展示本次会话创建的数据集</p>
        </div>
      </div>
      <el-table :data="datasets" border empty-text="暂无数据集">
        <el-table-column prop="name" label="数据集" min-width="220" />
        <el-table-column prop="space_id" label="Space ID" width="120" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" :loading="runningDatasetId === row.id" @click="run(row.id)">
              运行评估
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="latestResult" class="tool-panel">
      <div class="section-header compact">
        <div>
          <h3>评估报告</h3>
          <p>Run ID：{{ latestResult.run_id }}</p>
        </div>
      </div>
      <div class="metric-strip">
        <div class="metric-item">
          <span>Recall@K</span>
          <strong>{{ percent(latestResult.metrics.recall_at_k) }}</strong>
        </div>
        <div class="metric-item">
          <span>Precision@K</span>
          <strong>{{ percent(latestResult.metrics.precision_at_k) }}</strong>
        </div>
        <div class="metric-item">
          <span>MRR</span>
          <strong>{{ latestResult.metrics.mrr.toFixed(2) }}</strong>
        </div>
        <div class="metric-item">
          <span>引用准确率</span>
          <strong>{{ percent(latestResult.metrics.citation_accuracy) }}</strong>
        </div>
        <div class="metric-item">
          <span>权限违规</span>
          <strong>{{ latestResult.metrics.permission_violation_count }}</strong>
        </div>
      </div>
      <el-table :data="latestResult.cases" border>
        <el-table-column prop="question" label="问题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="recall_hit" label="召回命中" width="110">
          <template #default="{ row }">
            <el-tag :type="row.recall_hit ? 'success' : 'danger'" effect="plain">
              {{ row.recall_hit ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="citation_accuracy" label="引用准确率" width="130">
          <template #default="{ row }">{{ percent(row.citation_accuracy) }}</template>
        </el-table-column>
        <el-table-column prop="permission_violation" label="权限违规" width="120">
          <template #default="{ row }">
            <el-tag :type="row.permission_violation ? 'danger' : 'success'" effect="plain">
              {{ row.permission_violation ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  createEvalCase,
  createEvalDataset,
  runEval,
} from '@/api/eval'
import { apiErrorMessage } from '@/api/http'
import type { EntityId, EvalDataset, EvalRunResult } from '@/api/types'
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
  spaceId: [{ required: true, message: '请选择知识库', trigger: 'change' }],
  name: [{ required: true, message: '请输入数据集名称', trigger: 'blur' }],
}

const caseRules: FormRules = {
  datasetId: [{ required: true, message: '请选择数据集', trigger: 'change' }],
  question: [{ required: true, message: '请输入评估问题', trigger: 'blur' }],
}

onMounted(loadSpaces)

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
    ElMessage.success('评估数据集已创建')
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
    ElMessage.success('评估问题已添加')
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
    ElMessage.success('评估完成')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    runningDatasetId.value = null
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
