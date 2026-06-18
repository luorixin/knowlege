import { http, unwrapResponse } from './http'
import type { EntityId, EvalCase, EvalDataset, EvalRunResult, SearchFilters } from './types'

export interface CreateEvalDatasetPayload {
  tenant_id: number
  space_id: EntityId
  name: string
  description?: string
}

export interface CreateEvalCasePayload {
  dataset_id: EntityId
  question: string
  expected_answer?: string
  expected_doc_ids?: EntityId[]
  expected_chunk_ids?: EntityId[]
  expect_no_answer?: boolean
  filters?: SearchFilters
  tags?: string[]
}

export async function createEvalDataset(payload: CreateEvalDatasetPayload): Promise<EvalDataset> {
  return unwrapResponse(await http.post('/api/eval/dataset', payload))
}

export async function createEvalCase(payload: CreateEvalCasePayload): Promise<EvalCase> {
  return unwrapResponse(await http.post('/api/eval/case', payload))
}

export async function runEval(datasetId: EntityId, topK = 20): Promise<EvalRunResult> {
  return unwrapResponse(await http.post('/api/eval/run', {
    dataset_id: datasetId,
    top_k: topK,
  }))
}

export async function getEvalResult(runId: string): Promise<EvalRunResult> {
  return unwrapResponse(await http.get(`/api/eval/result/${runId}`))
}
