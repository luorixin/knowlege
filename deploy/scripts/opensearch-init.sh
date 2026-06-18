#!/usr/bin/env bash
# Create the OpenSearch index template used for keyword + metadata chunk search.
#
# Idempotent: PUT on _index_template always overwrites the latest version, so
# re-running after a template change simply updates it.
#
# Usage:
#   ./deploy/scripts/opensearch-init.sh
#
# Reads connection details from deploy/.env (or the environment). Requires curl.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/deploy/.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a; . "$ENV_FILE"; set +a
fi

OPENSEARCH_ENDPOINT="${OPENSEARCH_ENDPOINT:-http://localhost:9200}"
SEARCH_INDEX_PREFIX="${SEARCH_INDEX_PREFIX:-knowledge}"

TEMPLATE_NAME="${SEARCH_INDEX_PREFIX}_chunk_template"
INDEX_PATTERN="${SEARCH_INDEX_PREFIX}_chunk_keyword*"

echo "[opensearch-init] endpoint=$OPENSEARCH_ENDPOINT template=$TEMPLATE_NAME" >&2

PAYLOAD="$(cat <<JSON
{
  "index_patterns": ["$INDEX_PATTERN"],
  "template": {
    "settings": {
      "index": {
        "number_of_shards": 1,
        "number_of_replicas": 0
      }
    },
    "mappings": {
      "properties": {
        "chunk_id": { "type": "long" },
        "source_doc_id": { "type": "long" },
        "version_id": { "type": "long" },
        "page_no": { "type": "integer" },
        "section_title": { "type": "text", "analyzer": "standard" },
        "content": { "type": "text", "analyzer": "standard" },
        "content_type": { "type": "keyword" },
        "space_id": { "type": "long" },
        "tenant_id": { "type": "long" },
        "doc_type": { "type": "keyword" },
        "industry": { "type": "keyword" },
        "service_line": { "type": "keyword" },
        "confidential_level": { "type": "keyword" },
        "created_at": { "type": "date" },
        "content_hash": { "type": "keyword" }
      }
    }
  }
}
JSON
)"

HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' \
  -X PUT "$OPENSEARCH_ENDPOINT/_index_template/$TEMPLATE_NAME" \
  -H 'Content-Type: application/json' \
  --data "$PAYLOAD")

# 200 = updated, 201 = created; both are success.
if [[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "201" ]]; then
  echo "[opensearch-init] index template ready: $TEMPLATE_NAME (http $HTTP_CODE)" >&2
else
  echo "[opensearch-init] FAILED to create index template, http=$HTTP_CODE" >&2
  echo "[opensearch-init] endpoint was: $OPENSEARCH_ENDPOINT" >&2
  exit 1
fi

echo "[opensearch-init] done" >&2
