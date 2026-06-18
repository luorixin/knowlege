CREATE INDEX idx_kb_document_scope_metadata
    ON kb_document (tenant_id, space_id, status, doc_type, industry, service_line, created_at);

CREATE INDEX idx_kb_chunk_scope_doc_updated
    ON kb_document_chunk (tenant_id, space_id, status, doc_id, updated_at);

CREATE INDEX idx_kb_permission_policy_lookup
    ON kb_permission_policy (tenant_id, status, effect, subject_type, subject_id, resource_type, resource_id);
