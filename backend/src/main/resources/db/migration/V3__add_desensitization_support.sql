ALTER TABLE kb_document_version
    ADD COLUMN desensitize_status VARCHAR(32) NOT NULL DEFAULT 'PENDING';

ALTER TABLE kb_document_version
    ADD COLUMN desensitized_at TIMESTAMP;

CREATE TABLE kb_desensitization_mapping (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    page_no INTEGER,
    section_title VARCHAR(512),
    sensitive_type VARCHAR(64) NOT NULL,
    original_value TEXT NOT NULL,
    masked_value TEXT NOT NULL,
    rule_name VARCHAR(128),
    occurrence_index INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX idx_kb_desens_mapping_doc ON kb_desensitization_mapping (doc_id, version_id, sensitive_type);
CREATE INDEX idx_kb_desens_mapping_tenant ON kb_desensitization_mapping (tenant_id, status, created_at);
