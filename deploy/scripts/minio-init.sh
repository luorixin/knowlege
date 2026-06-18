#!/usr/bin/env bash
# Create the MinIO bucket used by the backend for document storage.
#
# Idempotent: skips creation when the bucket already exists.
#
# Usage:
#   ./deploy/scripts/minio-init.sh
#
# Reads connection details from deploy/.env (or the environment). Requires
# Docker to be available, because it shells out to the `minio/mc` image.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/deploy/.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a; . "$ENV_FILE"; set +a
fi

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://localhost:9000}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:-knowledge_minio}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-change-me-local-minio}"
MINIO_BUCKET="${MINIO_BUCKET:-knowledge-documents}"
MC_IMAGE="${MINIO_MC_IMAGE:-minio/mc:latest}"

echo "[minio-init] endpoint=$MINIO_ENDPOINT bucket=$MINIO_BUCKET" >&2

# `mc alias set` registers a named connection. `--network host` is only
# convenient on Linux; on macOS pass the host address explicitly via the env.
docker run --rm \
  -e MINIO_ENDPOINT="$MINIO_ENDPOINT" \
  -e MINIO_ROOT_USER="$MINIO_ROOT_USER" \
  -e MINIO_ROOT_PASSWORD="$MINIO_ROOT_PASSWORD" \
  -e MINIO_BUCKET="$MINIO_BUCKET" \
  --network host \
  "$MC_IMAGE" \
  sh -c '
    set -e
    mc alias set local "$MINIO_ENDPOINT" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null
    if mc stat "local/$MINIO_BUCKET" >/dev/null 2>&1; then
      echo "[minio-init] bucket already exists: $MINIO_BUCKET"
    else
      mc mb -p "local/$MINIO_BUCKET"
      echo "[minio-init] created bucket: $MINIO_BUCKET"
    fi
  '

echo "[minio-init] done" >&2
