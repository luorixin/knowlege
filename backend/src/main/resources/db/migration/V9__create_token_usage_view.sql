CREATE OR REPLACE VIEW v_kb_token_usage AS
SELECT 
    id,
    tenant_id,
    'CHAT' AS usage_type,
    created_at,
    model_provider,
    model_name,
    prompt_tokens,
    completion_tokens,
    latency_ms
FROM kb_query_message
WHERE role = 'ASSISTANT'

UNION ALL

SELECT
    t.id,
    t.tenant_id,
    'EMBEDDING' AS usage_type,
    t.finished_at AS created_at,
    t.model_provider,
    t.model_name,
    COALESCE(c.token_count, 0) AS prompt_tokens,
    0 AS completion_tokens,
    CAST(EXTRACT(EPOCH FROM (t.finished_at - t.started_at)) * 1000 AS BIGINT) AS latency_ms
FROM kb_embedding_index_task t
LEFT JOIN kb_document_chunk c ON t.chunk_id = c.id
WHERE t.status = 'COMPLETED';
