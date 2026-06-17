export interface ApiEnvelope<T> {
  code: string
  message: string
  data: T
}

export interface KnowledgeSpace {
  id: number
  tenantId: number
  name: string
  description?: string
  ownerUserId?: number
  visibility?: string
  status: string
  createdAt?: string
  updatedAt?: string
}

export interface CreateKnowledgeSpacePayload {
  tenantId: number
  name: string
  description?: string
  ownerUserId?: number
  visibility?: string
}

export interface DocumentListItem {
  id: number
  spaceId: number
  title: string
  docType?: string
  industry?: string
  serviceLine?: string
  confidentialLevel?: string
  fileHash?: string
  status: string
  createdAt?: string
  updatedAt?: string
}

export interface DocumentVersion {
  id: number
  versionNo?: number
  parseStatus?: string
  chunkCount?: number
  totalTokens?: number
  desensitizeStatus?: string
  desensitizedAt?: string
}

export interface DocumentDetail extends DocumentListItem {
  tenantId: number
  sourceUri?: string
  fileSize?: number
  currentVersion?: DocumentVersion
}

export interface DocumentUploadResult {
  documentId: number
  versionId: number
  parseTaskId: number
  title: string
  docType?: string
  industry?: string
  serviceLine?: string
  confidentialLevel?: string
  sourceUri?: string
  fileHash?: string
  status: string
  parseStatus: string
  duplicated: boolean
}

export interface DocumentParseStatus {
  documentId: number
  versionId?: number
  parseTaskId?: number
  taskType?: string
  status?: string
  progressPercent?: number
  parseStatus?: string
  errorCode?: string
  errorMessage?: string
  createdAt?: string
  updatedAt?: string
}

export interface SearchFilters {
  doc_type?: string
  industry?: string
  service_line?: string
  year_from?: number
}

export interface AgentCitation {
  citation_id: number
  doc_id: number
  doc_title: string
  page_no?: number
  section_title?: string
}

export interface AgentChatPayload {
  space_id: number
  session_id?: number | null
  query: string
  filters?: SearchFilters
}

export interface AgentChatResult {
  session_id: number
  answer: string
  citations: AgentCitation[]
}

export interface EvalDataset {
  id: number
  tenant_id: number
  space_id: number
  name: string
  description?: string
  status: string
}

export interface EvalCase {
  id: number
  dataset_id: number
  question: string
  expected_answer?: string
  expected_doc_ids: number[]
  expected_chunk_ids: number[]
  expect_no_answer: boolean
  status: string
}

export interface EvalMetrics {
  recall_at_k: number
  precision_at_k: number
  mrr: number
  citation_accuracy: number
  no_answer_accuracy: number
  permission_violation_count: number
}

export interface EvalCaseReport {
  case_id: number
  question: string
  expect_no_answer: boolean
  actual_answer?: string
  retrieved_chunk_ids: number[]
  retrieved_doc_ids: number[]
  cited_doc_ids: number[]
  recall_hit: boolean
  citation_accuracy: number
  no_answer_correct: boolean
  inaccessible_expected_target_count: number
  unauthorized_citation_count: number
  unauthorized_retrieved_count: number
  permission_violation: boolean
  reciprocal_rank: number
}

export interface EvalRunResult {
  run_id: string
  dataset_id: number
  case_count: number
  metrics: EvalMetrics
  cases: EvalCaseReport[]
}
