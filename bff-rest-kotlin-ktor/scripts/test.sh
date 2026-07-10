#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

SPECMATIC_IMAGE="${SPECMATIC_IMAGE:-specmatic/enterprise:1.19.1}"
APP_IMAGE="${APP_IMAGE:-bff-rest-kotlin-ktor:local}"
NETWORK_NAME="${NETWORK_NAME:-bff-rest-kotlin-ktor-net}"
APP_CONTAINER="${APP_CONTAINER:-bff-rest-kotlin-ktor-app}"
SPECMATIC_CONTAINER="${SPECMATIC_CONTAINER:-bff-rest-kotlin-ktor-specmatic}"

mkdir -p build/reports/specmatic build/specmatic-kafka-logs
docker build -t "$APP_IMAGE" .

docker rm -f "$APP_CONTAINER" "$SPECMATIC_CONTAINER" >/dev/null 2>&1 || true
docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true
docker network create "$NETWORK_NAME" >/dev/null

cleanup() {
  docker rm -f "$APP_CONTAINER" "$SPECMATIC_CONTAINER" >/dev/null 2>&1 || true
  docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run -d \
  --name "$APP_CONTAINER" \
  --network "$NETWORK_NAME" \
  --network-alias app \
  -e SUT_PORT=8080 \
  -e STUB_BASE_URL=http://specmatic:8090 \
  -e KAFKA_BROKER=specmatic:9092 \
  "$APP_IMAGE" >/dev/null

for _ in $(seq 1 60); do
  if docker run --rm --network "$NETWORK_NAME" curlimages/curl:8.8.0 -fsS http://app:8080/health >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! docker run --rm --network "$NETWORK_NAME" curlimages/curl:8.8.0 -fsS http://app:8080/health >/dev/null 2>&1; then
  docker logs "$APP_CONTAINER" || true
  echo "Ktor BFF did not start on http://app:8080" >&2
  exit 1
fi

docker run --rm \
  --name "$SPECMATIC_CONTAINER" \
  --network "$NETWORK_NAME" \
  --network-alias specmatic \
  -e SPECMATIC_LICENSE_KEY="${SPECMATIC_LICENSE_KEY:-}" \
  -e SUT_BASE_URL=http://app:8080 \
  -e STUB_HOST=0.0.0.0 \
  -e STUB_PORT=8090 \
  -e KAFKA_HOST=specmatic \
  -e KAFKA_PORT=9092 \
  -e KAFKA_BROKER=specmatic:9092 \
  -v "$ROOT_DIR:/workspace" \
  -w /workspace \
  "$SPECMATIC_IMAGE" run-suite --config specmatic.yaml
