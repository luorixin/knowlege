#!/usr/bin/env bash
# Initialize the optional middleware used by the knowledge platform.
#
# By default only the always-on middleware (MinIO) is initialized. Pass flags
# to also initialize search / vector middleware that you started via the
# matching docker compose profile.
#
# Usage:
#   ./deploy/scripts/init-all.sh              # MinIO only (always-on)
#   ./deploy/scripts/init-all.sh --search     # + OpenSearch index template
#   ./deploy/scripts/init-all.sh --vector     # + Milvus collection
#   ./deploy/scripts/init-all.sh --search --vector
#
# Run AFTER `docker compose ... up -d` has started the relevant profile.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DO_SEARCH=0
DO_VECTOR=0
for arg in "$@"; do
  case "$arg" in
    --search) DO_SEARCH=1 ;;
    --vector) DO_VECTOR=1 ;;
    -h|--help)
      sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "[init-all] unknown argument: $arg" >&2
      exit 2
      ;;
  esac
done

echo "[init-all] MinIO bucket (always-on)"
bash "$SCRIPT_DIR/minio-init.sh"

if [[ "$DO_SEARCH" -eq 1 ]]; then
  echo "[init-all] OpenSearch index template"
  bash "$SCRIPT_DIR/opensearch-init.sh"
fi

if [[ "$DO_VECTOR" -eq 1 ]]; then
  echo "[init-all] Milvus collection"
  REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
  AI_VENV="$REPO_ROOT/ai-service/.venv"
  if [[ -x "$AI_VENV/bin/python" ]]; then
    "$AI_VENV/bin/python" "$SCRIPT_DIR/milvus-init.py"
  else
    python3 "$SCRIPT_DIR/milvus-init.py"
  fi
fi

echo "[init-all] done"
