#!/usr/bin/env bash
# Upload seed fixture documents to a knowledge space via the backend API.
#
# Requires the backend and a target knowledge space to exist. The space is
# created automatically if it does not already exist.
#
# Usage:
#   ./scripts/seed-documents.sh
#   SPACE_ID=42 ./scripts/seed-documents.sh
#
# The script uses curl and exits non-zero on any upload failure.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FIXTURES_DIR="$SCRIPT_DIR/fixtures"

# --- Configurable variables (override via environment) -----------------------
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
USER_ID="${X_USER_ID:-42}"
TENANT_ID="${X_TENANT_ID:-1001}"
SPACE_ID="${SPACE_ID:-}"
SPACE_NAME="${SPACE_NAME:-Seed Fixtures}"

# ---------------------------------------------------------------------------
if [[ ! -d "$FIXTURES_DIR" ]]; then
  echo "[seed] ERROR: fixtures directory not found: $FIXTURES_DIR" >&2
  exit 1
fi

# Auto-create the knowledge space if no SPACE_ID is given.
if [[ -z "$SPACE_ID" ]]; then
  echo "[seed] Creating knowledge space (tenant=$TENANT_ID name=$SPACE_NAME)..." >&2
  RESP=$(curl -sf -X POST "$BACKEND_URL/api/v1/kb-spaces" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: $USER_ID" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -d "{\"tenantId\":$TENANT_ID,\"name\":\"$SPACE_NAME\"}" 2>&1) || true

  SPACE_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null || true)
  if [[ -z "$SPACE_ID" ]]; then
    echo "[seed] Could not create knowledge space. Response:" >&2
    echo "$RESP" >&2
    echo "[seed] You can set SPACE_ID manually: SPACE_ID=<id> $0" >&2
    exit 1
  fi
  echo "[seed] Created space: id=$SPACE_ID" >&2
fi

# Upload each fixture file.
FILES=("$FIXTURES_DIR"/*.md "$FIXTURES_DIR"/*.txt)
COUNT=0
FAIL=0

for FILE in "${FILES[@]}"; do
  [[ -f "$FILE" ]] || continue
  TITLE="$(basename "$FILE")"
  echo -n "[seed] uploading $TITLE ... " >&2
  HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "$BACKEND_URL/api/v1/kb-spaces/$SPACE_ID/documents" \
    -H "X-User-Id: $USER_ID" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -F "file=@$FILE" \
    -F "title=$TITLE" \
    -F "industry=金融" \
    -F "serviceLine=数据治理" \
    -F "confidentialLevel=INTERNAL") || true

  if [[ "$HTTP_CODE" =~ ^2 ]]; then
    echo "ok (http $HTTP_CODE)" >&2
    COUNT=$((COUNT + 1))
  else
    echo "FAILED (http $HTTP_CODE)" >&2
    FAIL=$((FAIL + 1))
  fi
done

echo "[seed] done: $COUNT uploaded, $FAIL failed, space_id=$SPACE_ID" >&2
[[ "$FAIL" -eq 0 ]]
