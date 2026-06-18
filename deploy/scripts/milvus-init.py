#!/usr/bin/env python3
"""Create the Milvus collection used for vector chunk search.

Idempotent: re-runs are safe. The collection is only created when missing;
existing collections are left untouched (drop+recreate would lose data).

Usage:
    python3 deploy/scripts/milvus-init.py

Connection details are read from deploy/.env (or the process environment).
Requires the optional ``pymilvus`` dependency::

    cd ai-service
    .venv/bin/python -m pip install '.[deploy]'
"""

from __future__ import annotations

import os
import sys
from pathlib import Path


def _load_env_file(path: Path) -> None:
    """Load simple KEY=VALUE lines from a .env file into os.environ (without override)."""
    if not path.is_file():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


def main() -> int:
    repo_root = Path(__file__).resolve().parents[2]
    env_file = Path(os.environ.get("ENV_FILE", repo_root / "deploy" / ".env"))
    _load_env_file(env_file)

    try:
        from pymilvus import (
            Collection,
            CollectionSchema,
            DataType,
            FieldSchema,
            connections,
            utility,
        )
    except ImportError:
        print(
            "[milvus-init] pymilvus is not installed.\n"
            "  Install it with: cd ai-service && .venv/bin/python -m pip install '.[deploy]'",
            file=sys.stderr,
        )
        return 2

    endpoint = os.environ.get("MILVUS_ENDPOINT", "http://localhost:19530")
    # pymilvus expects host and port separately.
    if "://" in endpoint:
        _, _, rest = endpoint.partition("://")
    else:
        rest = endpoint
    host, _, port = rest.partition(":")
    port = port or "19530"
    collection_prefix = os.environ.get("VECTOR_COLLECTION_PREFIX", "knowledge")
    dimension = int(os.environ.get("EMBEDDING_DIMENSION", "3"))
    collection_name = f"{collection_prefix}_chunk_vector"

    print(f"[milvus-init] endpoint={host}:{port} collection={collection_name} dim={dimension}", file=sys.stderr)

    connections.connect(alias="default", host=host, port=port)

    if utility.has_collection(collection_name):
        print(f"[milvus-init] collection already exists: {collection_name}", file=sys.stderr)
        return 0

    fields = [
        FieldSchema(name="chunk_id", dtype=DataType.INT64, is_primary=True, auto_id=False),
        FieldSchema(name="source_doc_id", dtype=DataType.INT64),
        FieldSchema(name="version_id", dtype=DataType.INT64),
        FieldSchema(name="page_no", dtype=DataType.INT32),
        FieldSchema(name="section_title", dtype=DataType.VARCHAR, max_length=512),
        FieldSchema(name="space_id", dtype=DataType.INT64),
        FieldSchema(name="tenant_id", dtype=DataType.INT64),
        FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=dimension),
    ]
    schema = CollectionSchema(fields=fields, description="Knowledge chunk vector collection (MVP)")
    Collection(name=collection_name, schema=schema)

    # Create a default IVF_FLAT index so the collection is searchable once vectors are written.
    collection = Collection(collection_name)
    collection.create_index(
        field_name="embedding",
        index_params={
            "index_type": "IVF_FLAT",
            "metric_type": "COSINE",
            "params": {"nlist": 128},
        },
    )

    print(f"[milvus-init] created collection: {collection_name}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
