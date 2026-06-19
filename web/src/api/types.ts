export type EntityId = string

export interface ApiEnvelope<T> {
  code: string
  message: string
  data: T
}

export interface KnowledgeSpace {
  id: EntityId
  tenantId: EntityId
  name: string
  description?: string
  ownerUserId?: EntityId
  visibility?: string
  status: string
  createdAt?: string
  updatedAt?: string
}

export interface CreateKnowledgeSpacePayload {
  tenantId: EntityId
  name: string
  description?: string
  ownerUserId?: EntityId
  visibility?: string
}

export interface DocumentListItem {
  id: EntityId
  spaceId: EntityId
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
  id: EntityId
  versionNo?: number
  parseStatus?: string
  chunkCount?: number
  totalTokens?: number
  desensitizeStatus?: string
  desensitizedAt?: string
}

export interface DocumentDetail extends DocumentListItem {
  tenantId: EntityId
  sourceUri?: string
  fileSize?: number
  currentVersion?: DocumentVersion
}

export interface DocumentUploadResult {
  documentId: EntityId
  versionId: EntityId
  parseTaskId: EntityId
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

export interface ParseMetadata {
  parser?: string
  page_count?: number
  block_count?: number
  error_count?: number
  page_modes?: string[]
  errors?: any[]
}

export interface DocumentParseStatus {
  documentId: EntityId
  versionId?: EntityId
  parseTaskId?: EntityId
  taskType?: string
  status?: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'PARTIAL_SUCCESS' | 'FAILED' | string
  progressPercent?: number
  parseStatus?: string
  errorCode?: string
  errorMessage?: string
  metadata?: ParseMetadata
  createdAt?: string
  updatedAt?: string
}

export interface TaskCenterItem {
  task_key: string
  task_id: EntityId
  task_category: 'PARSE_CHUNK' | 'EMBEDDING_INDEX' | string
  task_type: string
  stage_label: string
  tenant_id: EntityId
  space_id: EntityId
  doc_id?: EntityId
  document_title?: string
  version_id?: EntityId
  chunk_id?: EntityId
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'PARTIAL_SUCCESS' | 'FAILED' | string
  retry_count?: number
  progress_percent?: number
  model_provider?: string
  model_name?: string
  embedding_dimension?: number
  index_name?: string
  vector_collection?: string
  worker_id?: string
  started_at?: string
  finished_at?: string
  error_code?: string
  error_message?: string
  metadata?: ParseMetadata
  created_at?: string
  updated_at?: string
  retryable: boolean
  runnable: boolean
}

export interface SearchFilters {
  doc_type?: string
  industry?: string
  service_line?: string
  year_from?: number
}

export interface AgentCitation {
  citation_id: EntityId
  doc_id: EntityId
  doc_title: string
  page_no?: number
  section_title?: string
  chunk_content?: string
  source_uri?: string
}

export interface AgentChatPayload {
  space_id: EntityId
  session_id?: EntityId | null
  query: string
  filters?: SearchFilters
}

export interface AgentChatResult {
  session_id: EntityId
  answer: string
  citations: AgentCitation[]
  debug_info?: any
}

export interface AgentSessionDto {
  id: EntityId
  spaceId: EntityId
  title: string
  status: string
  createdAt: string
}

export interface AgentMessageDto {
  id: EntityId
  role: 'user' | 'assistant'
  content: string
  createdAt: string
  citations: AgentCitation[]
  error: boolean
}

export interface EvalDataset {
  id: EntityId
  tenant_id: EntityId
  space_id: EntityId
  name: string
  description?: string
  status: string
}

export interface EvalCase {
  id: EntityId
  dataset_id: EntityId
  question: string
  expected_answer?: string
  expected_doc_ids: EntityId[]
  expected_chunk_ids: EntityId[]
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
  case_id: EntityId
  question: string
  expect_no_answer: boolean
  actual_answer?: string
  retrieved_chunk_ids: EntityId[]
  retrieved_doc_ids: EntityId[]
  cited_doc_ids: EntityId[]
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
  dataset_id: EntityId
  case_count: number
  metrics: EvalMetrics
  cases: EvalCaseReport[]
}
