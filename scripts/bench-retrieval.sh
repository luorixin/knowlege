#!/usr/bin/env bash
# Run a quick latency benchmark against the hybrid retrieval API.
#
# Sends N queries and prints per-query latency plus a p50/p95 summary.
# Pure shell — no extra dependencies beyond curl.
#
# Usage:
#   ./scripts/bench-retrieval.sh
#   ROUNDS=20 TOP_K=10 ./scripts/bench-retrieval.sh
set -euo pipefail

# --- Configurable variables (override via environment) -----------------------
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
USER_ID="${X_USER_ID:-42}"
TENANT_ID="${X_TENANT_ID:-1001}"
SPACE_ID="${SPACE_ID:-1}"
TOP_K="${TOP_K:-20}"
ROUNDS="${ROUNDS:-10}"

# Predefined queries (mix of keyword-heavy and semantic-style queries).
QUERIES=(
  "金融行业数据治理 proposal 有哪些类似案例"
  "风控系统的实时决策延迟指标是多少"
  "客户数据脱敏规则有哪些"
  "企业知识库建设规范中权限管理的要求"
  "数据治理项目的交付计划和里程碑"
)

echo "=== Retrieval benchmark ===" >&2
echo "backend=$BACKEND_URL space=$SPACE_ID top_k=$TOP_K rounds=$ROUNDS" >&2
echo "" >&2

TIMES=()
TOTAL=0

for ((i = 1; i <= ROUNDS; i++)); do
  Q="${QUERIES[$(( (i - 1) % ${#QUERIES[@]} ))]}"
  ELAPSED=$(curl -sf -o /dev/null -w '%{time_total}' \
    -X POST "$BACKEND_URL/api/retrieval/search" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: $USER_ID" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -d "{
      \"query\": \"$Q\",
      \"space_id\": $SPACE_ID,
      \"filters\": {
        \"industry\": \"金融\",
        \"service_line\": \"数据治理\"
      },
      \"top_k\": $TOP_K
    }" 2>&1) || true

  MS=$(echo "$ELAPSED" | awk '{printf "%.0f", $1 * 1000}')
  TIMES+=("$MS")
  TOTAL=$((TOTAL + 1))
  printf "  round %2d/%d  %6d ms  query: %.40s\n" "$i" "$ROUNDS" "$MS" "$Q" >&2
done

# Compute p50 and p95 using awk.
echo "" >&2
echo "--- Summary ---" >&2
printf "  total queries: %d\n" "$TOTAL" >&2
printf "  top_k:          %d\n" "$TOP_K" >&2
echo "${TIMES[*]}" | tr ' ' '\n' | awk '
  { vals[NR] = $1; sum += $1 }
  END {
    n = NR
    avg = sum / n
    # Sort for percentiles
    for (i = 1; i <= n; i++) for (j = i + 1; j <= n; j++) {
      if (vals[i] > vals[j]) { tmp = vals[i]; vals[i] = vals[j]; vals[j] = tmp }
    }
    p50_idx = int(n * 0.50 + 0.5); if (p50_idx < 1) p50_idx = 1
    p95_idx = int(n * 0.95 + 0.5); if (p95_idx < 1) p95_idx = 1; if (p95_idx > n) p95_idx = n
    printf "  avg latency:   %d ms\n", avg
    printf "  p50 latency:   %d ms\n", vals[p50_idx]
    printf "  p95 latency:   %d ms\n", vals[p95_idx]
    printf "  min latency:   %d ms\n", vals[1]
    printf "  max latency:   %d ms\n", vals[n]
  }
' >&2
echo "" >&2
