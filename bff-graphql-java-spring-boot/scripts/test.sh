#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

export SUT_PORT="${SUT_PORT:-8080}"
export SUT_HOST="${SUT_HOST:-127.0.0.1}"
export SUT_BASE_URL="${SUT_BASE_URL:-http://127.0.0.1:${SUT_PORT}}"
export STUB_BASE_URL="${STUB_BASE_URL:-http://127.0.0.1:8090}"
export BACKEND_AUTHENTICATE="${BACKEND_AUTHENTICATE:-sample-api-key}"
export BACKEND_IDEMPOTENCY_KEY="${BACKEND_IDEMPOTENCY_KEY:-00000000-0000-0000-0000-000000000001}"

APP_CLASSPATH="target/classes:$(cat target/runtime-classpath.txt)"
java -cp "$APP_CLASSPATH" io.specmatic.examples.bffgraphql.BffGraphqlApplication &
APP_PID=$!

cleanup() {
  kill "$APP_PID" >/dev/null 2>&1 || true
  wait "$APP_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

for attempt in {1..60}; do
  if curl -fsS "$SUT_BASE_URL/graphql" \
      -H 'Content-Type: application/json' \
      --data '{"query":"{ __typename }"}' >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "$APP_PID" >/dev/null 2>&1; then
    echo "Application process exited before it became ready." >&2
    wait "$APP_PID"
  fi
  sleep 1
done

if ! curl -fsS "$SUT_BASE_URL/graphql" \
    -H 'Content-Type: application/json' \
    --data '{"query":"{ __typename }"}' >/dev/null 2>&1; then
  echo "Application did not become ready at $SUT_BASE_URL/graphql" >&2
  exit 1
fi

java -jar target/specmatic/specmatic-enterprise.jar run-suite --config specmatic.yaml
