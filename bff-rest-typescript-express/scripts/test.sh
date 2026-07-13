#!/usr/bin/env bash
set -euo pipefail

SUT_PORT="${SUT_PORT:-8080}"
SUT_BASE_URL="${SUT_BASE_URL:-http://localhost:${SUT_PORT}}"
APP_STUB_BASE_URL="${APP_STUB_BASE_URL:-http://localhost:8090}"
STUB_BASE_URL="${STUB_BASE_URL:-http://0.0.0.0:8090}"
APP_KAFKA_BROKER_URL="${APP_KAFKA_BROKER_URL:-localhost:9092}"
KAFKA_BROKER_URL="${KAFKA_BROKER_URL:-localhost:9092}"
KAFKA_CONTAINER_NAME="${KAFKA_CONTAINER_NAME:-specmatic-sample-redpanda}"
APP_CONTAINER_NAME="${APP_CONTAINER_NAME:-bff-rest-typescript-express-test}"
SPECMATIC_REPORT_DIR="${SPECMATIC_REPORT_DIR:-build/reports/specmatic}"

mkdir -p "${SPECMATIC_REPORT_DIR}"

if ! docker ps --format '{{.Names}}' | grep -qx "${KAFKA_CONTAINER_NAME}"; then
  docker run -d --rm \
    --name "${KAFKA_CONTAINER_NAME}" \
    -p "${SUT_PORT}:${SUT_PORT}" \
    -p 8090:8090 \
    -p 9092:9092 \
    -p 9999:9999 \
    docker.redpanda.com/redpandadata/redpanda:v24.3.5 \
    redpanda start \
    --overprovisioned \
    --smp 1 \
    --memory 256M \
    --reserve-memory 0M \
    --node-id 0 \
    --check=false \
    --kafka-addr PLAINTEXT://0.0.0.0:9092 \
    --advertise-kafka-addr PLAINTEXT://${KAFKA_BROKER_URL}
fi

for _ in $(seq 1 45); do
  if docker logs "${KAFKA_CONTAINER_NAME}" 2>&1 | grep -q "Started Kafka API server"; then
    break
  fi
  sleep 1
done

docker build -t bff-rest-typescript-express:test .
docker rm -f "${APP_CONTAINER_NAME}" >/dev/null 2>&1 || true
docker run -d --rm \
  --name "${APP_CONTAINER_NAME}" \
  --network "container:${KAFKA_CONTAINER_NAME}" \
  -e SUT_PORT="${SUT_PORT}" \
  -e STUB_BASE_URL="${APP_STUB_BASE_URL}" \
  -e KAFKA_BROKER_URL="${APP_KAFKA_BROKER_URL}" \
  bff-rest-typescript-express:test

cleanup() {
  docker rm -f "${APP_CONTAINER_NAME}" >/dev/null 2>&1 || true
  if [ "${KEEP_KAFKA:-false}" != "true" ]; then
    docker rm -f "${KAFKA_CONTAINER_NAME}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

for _ in $(seq 1 30); do
  if curl -fsS "http://localhost:${SUT_PORT}/monitor/1" >/dev/null 2>&1; then
    break
  fi
  docker logs "${APP_CONTAINER_NAME}" 2>&1 | tail -20
  sleep 1
done

docker run --rm \
  --network "container:${KAFKA_CONTAINER_NAME}" \
  -e SPECMATIC_LICENSE_KEY="${SPECMATIC_LICENSE_KEY:-}" \
  -e SUT_BASE_URL="${SUT_BASE_URL}" \
  -e STUB_BASE_URL="${STUB_BASE_URL}" \
  -e KAFKA_BROKER_URL="${KAFKA_BROKER_URL}" \
  -e SPECMATIC_REPORT_DIR="${SPECMATIC_REPORT_DIR}" \
  -v "${PWD}:/workspace" \
  -w /workspace \
  specmatic/enterprise:1.19.1 run-suite --config specmatic.yaml
