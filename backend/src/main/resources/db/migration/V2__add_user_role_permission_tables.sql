CREATE TABLE kb_user (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(128) NOT NULL,
    display_name VARCHAR(256),
    email VARCHAR(256),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_kb_user_tenant_username UNIQUE (tenant_id, username)
);

CREATE TABLE kb_role (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_kb_role_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE kb_user_role (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_kb_user_role_user_role UNIQUE (tenant_id, user_id, role_id)
);

CREATE INDEX idx_kb_user_tenant_status ON kb_user (tenant_id, status);
CREATE INDEX idx_kb_role_tenant_status ON kb_role (tenant_id, status);
CREATE INDEX idx_kb_user_role_user ON kb_user_role (tenant_id, user_id, status);
CREATE INDEX idx_kb_user_role_role ON kb_user_role (tenant_id, role_id, status);
