CREATE INDEX idx_kb_audit_tenant_time
    ON kb_audit_log (tenant_id, created_at);

CREATE INDEX idx_kb_audit_action_time
    ON kb_audit_log (tenant_id, action, created_at);

CREATE INDEX idx_kb_audit_status_time
    ON kb_audit_log (tenant_id, result_status, created_at);
