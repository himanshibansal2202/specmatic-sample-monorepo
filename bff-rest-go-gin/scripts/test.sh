#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

SUT_PORT="${SUT_PORT:-8080}"
SUT_BASE_URL="${SUT_BASE_URL:-http://localhost:${SUT_PORT}}"
STUB_BASE_URL="${STUB_BASE_URL:-http://localhost:8090}"
KAFKA_HOST="${KAFKA_HOST:-localhost}"
KAFKA_PORT="${KAFKA_PORT:-9092}"
KAFKA_BROKER_URL="${KAFKA_BROKER_URL:-${KAFKA_HOST}:${KAFKA_PORT}}"
SPECMATIC_VERSION="${SPECMATIC_VERSION:-1.19.1}"
SPECMATIC_JAR="${SPECMATIC_JAR:-$ROOT_DIR/build/specmatic/specmatic-enterprise-${SPECMATIC_VERSION}.jar}"

mkdir -p "$ROOT_DIR/build/specmatic" "$ROOT_DIR/build/reports/specmatic"

if [[ ! -f "$SPECMATIC_JAR" ]]; then
  mvn -q dependency:copy \
    -Dartifact="io.specmatic.enterprise:executable-all:${SPECMATIC_VERSION}" \
    -DoutputDirectory="$ROOT_DIR/build/specmatic" \
    -Dmdep.stripVersion=false
  mv "$ROOT_DIR/build/specmatic/executable-all-${SPECMATIC_VERSION}.jar" "$SPECMATIC_JAR"
fi

go test ./...
go build -o "$ROOT_DIR/build/bff-rest-go-gin" ./cmd/server

"$ROOT_DIR/build/bff-rest-go-gin" &
APP_PID=$!
cleanup() {
  kill "$APP_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

for _ in $(seq 1 60); do
  if curl -fsS "$SUT_BASE_URL/monitor/1" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

export SUT_PORT SUT_BASE_URL STUB_BASE_URL KAFKA_HOST KAFKA_PORT KAFKA_BROKER_URL
java -jar "$SPECMATIC_JAR" run-suite --config specmatic.yaml
