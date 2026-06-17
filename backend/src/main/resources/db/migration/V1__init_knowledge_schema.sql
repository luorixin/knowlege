CREATE TABLE kb_space (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1024),
    owner_user_id BIGINT,
    visibility VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_document (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    title VARCHAR(512) NOT NULL,
    doc_type VARCHAR(64) NOT NULL,
    industry VARCHAR(128),
    service_line VARCHAR(128),
    confidential_level VARCHAR(64) NOT NULL DEFAULT 'INTERNAL',
    source_uri VARCHAR(1024),
    storage_uri VARCHAR(1024),
    file_hash VARCHAR(128) NOT NULL,
    file_size BIGINT,
    current_version_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_document_version (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    version_no INTEGER NOT NULL,
    source_uri VARCHAR(1024),
    storage_uri VARCHAR(1024),
    file_hash VARCHAR(128) NOT NULL,
    file_size BIGINT,
    parser_profile VARCHAR(128),
    parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    chunk_count INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_kb_document_version_doc_version UNIQUE (doc_id, version_no)
);

CREATE TABLE kb_document_chunk (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    page_no INTEGER,
    section_title VARCHAR(512),
    content TEXT NOT NULL,
    token_count INTEGER NOT NULL DEFAULT 0,
    content_hash VARCHAR(128),
    metadata_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_kb_document_chunk_version_index UNIQUE (version_id, chunk_index)
);

CREATE TABLE kb_document_parse_task (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    task_type VARCHAR(64) NOT NULL DEFAULT 'PARSE_DOCUMENT',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    priority INTEGER NOT NULL DEFAULT 0,
    retry_count INTEGER NOT NULL DEFAULT 0,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    worker_id VARCHAR(128),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error_code VARCHAR(128),
    error_message TEXT,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_embedding_index_task (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    doc_id BIGINT,
    version_id BIGINT,
    chunk_id BIGINT,
    model_provider VARCHAR(128),
    model_name VARCHAR(256) NOT NULL,
    embedding_dimension INTEGER,
    index_name VARCHAR(256),
    vector_collection VARCHAR(256),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    priority INTEGER NOT NULL DEFAULT 0,
    retry_count INTEGER NOT NULL DEFAULT 0,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error_code VARCHAR(128),
    error_message TEXT,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_permission_policy (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    space_id BIGINT,
    subject_type VARCHAR(64) NOT NULL,
    subject_id VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id BIGINT,
    effect VARCHAR(16) NOT NULL DEFAULT 'ALLOW',
    actions VARCHAR(512) NOT NULL,
    condition_json TEXT,
    priority INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_query_session (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    space_id BIGINT,
    user_id BIGINT NOT NULL,
    title VARCHAR(512),
    channel VARCHAR(64) NOT NULL DEFAULT 'WEB',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_query_message (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    parent_message_id BIGINT,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    model_provider VARCHAR(128),
    model_name VARCHAR(256),
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    latency_ms BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_answer_citation (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    chunk_id BIGINT NOT NULL,
    page_no INTEGER,
    section_title VARCHAR(512),
    quote_text TEXT,
    score DECIMAL(10, 6),
    rank_no INTEGER,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_audit_log (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    actor_user_id BIGINT,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64),
    resource_id VARCHAR(128),
    trace_id VARCHAR(128),
    ip_address VARCHAR(64),
    user_agent VARCHAR(1024),
    request_method VARCHAR(16),
    request_uri VARCHAR(1024),
    result_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    detail_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_eval_dataset (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    space_id BIGINT,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_eval_case (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    dataset_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    expected_answer TEXT,
    expected_doc_ids VARCHAR(1024),
    tags VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE kb_eval_result (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    dataset_id BIGINT NOT NULL,
    case_id BIGINT NOT NULL,
    run_id VARCHAR(128) NOT NULL,
    query_session_id BIGINT,
    answer_message_id BIGINT,
    actual_answer TEXT,
    score DECIMAL(10, 6),
    hit_count INTEGER NOT NULL DEFAULT 0,
    citation_hit_count INTEGER NOT NULL DEFAULT 0,
    evaluator_type VARCHAR(64),
    evaluator_model VARCHAR(256),
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    detail_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX idx_kb_space_tenant_status ON kb_space (tenant_id, status);
CREATE INDEX idx_kb_space_owner ON kb_space (owner_user_id);

CREATE INDEX idx_kb_document_space_status ON kb_document (space_id, status);
CREATE INDEX idx_kb_document_hash ON kb_document (tenant_id, file_hash);
CREATE INDEX idx_kb_document_classification ON kb_document (tenant_id, industry, service_line, confidential_level);

CREATE INDEX idx_kb_document_version_doc ON kb_document_version (doc_id, status);
CREATE INDEX idx_kb_document_version_parse ON kb_document_version (tenant_id, parse_status);

CREATE INDEX idx_kb_chunk_doc ON kb_document_chunk (doc_id, version_id);
CREATE INDEX idx_kb_chunk_space_status ON kb_document_chunk (space_id, status);
CREATE INDEX idx_kb_chunk_content_hash ON kb_document_chunk (tenant_id, content_hash);

CREATE INDEX idx_kb_parse_task_status ON kb_document_parse_task (tenant_id, status, priority, created_at);
CREATE INDEX idx_kb_parse_task_doc ON kb_document_parse_task (doc_id, version_id);

CREATE INDEX idx_kb_embedding_task_status ON kb_embedding_index_task (tenant_id, status, priority, created_at);
CREATE INDEX idx_kb_embedding_task_chunk ON kb_embedding_index_task (chunk_id);
CREATE INDEX idx_kb_embedding_task_model ON kb_embedding_index_task (tenant_id, model_name, index_name);

CREATE INDEX idx_kb_permission_subject ON kb_permission_policy (tenant_id, subject_type, subject_id, status);
CREATE INDEX idx_kb_permission_resource ON kb_permission_policy (tenant_id, resource_type, resource_id, effect);
CREATE INDEX idx_kb_permission_space ON kb_permission_policy (space_id, status);

CREATE INDEX idx_kb_query_session_user ON kb_query_session (tenant_id, user_id, created_at);
CREATE INDEX idx_kb_query_session_space ON kb_query_session (space_id, status);

CREATE INDEX idx_kb_query_message_session ON kb_query_message (session_id, created_at);
CREATE INDEX idx_kb_query_message_role ON kb_query_message (tenant_id, role, created_at);

CREATE INDEX idx_kb_citation_message ON kb_answer_citation (message_id, rank_no);
CREATE INDEX idx_kb_citation_chunk ON kb_answer_citation (chunk_id);
CREATE INDEX idx_kb_citation_doc ON kb_answer_citation (doc_id, page_no);

CREATE INDEX idx_kb_audit_actor_time ON kb_audit_log (tenant_id, actor_user_id, created_at);
CREATE INDEX idx_kb_audit_resource ON kb_audit_log (tenant_id, resource_type, resource_id);
CREATE INDEX idx_kb_audit_trace ON kb_audit_log (trace_id);

CREATE INDEX idx_kb_eval_dataset_space ON kb_eval_dataset (tenant_id, space_id, status);
CREATE INDEX idx_kb_eval_case_dataset ON kb_eval_case (dataset_id, status);
CREATE INDEX idx_kb_eval_result_run ON kb_eval_result (tenant_id, run_id);
CREATE INDEX idx_kb_eval_result_case ON kb_eval_result (case_id, created_at);
