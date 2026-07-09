#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

SUT_PORT="${SUT_PORT:-8080}"
SUT_HOST_FOR_SPECMATIC="${SUT_HOST_FOR_SPECMATIC:-host.docker.internal}"
SPECMATIC_IMAGE="${SPECMATIC_IMAGE:-specmatic/enterprise:1.19.1}"

mkdir -p .specmatic_grpc_working_dir/order_api .specmatic_grpc_working_dir/buf/validate build/specmatic-reports
cp src/main/proto/order_api/product_types.proto .specmatic_grpc_working_dir/order_api/product_types.proto
cp src/main/proto/order_api/order_types.proto .specmatic_grpc_working_dir/order_api/order_types.proto
cp src/main/proto/buf/validate/validate.proto .specmatic_grpc_working_dir/buf/validate/validate.proto

./mvnw -q -DskipTests package

java -jar target/backend-grpc-kotlin-ktor-1.0.0.jar &
APP_PID=$!
cleanup() {
  kill "$APP_PID" >/dev/null 2>&1 || true
  wait "$APP_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

for _ in $(seq 1 60); do
  if nc -z 127.0.0.1 "$SUT_PORT" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! nc -z 127.0.0.1 "$SUT_PORT" >/dev/null 2>&1; then
  echo "gRPC server did not start on port $SUT_PORT" >&2
  exit 1
fi

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -e SPECMATIC_LICENSE_KEY="${SPECMATIC_LICENSE_KEY:-}" \
  -e SUT_HOST_FOR_SPECMATIC="$SUT_HOST_FOR_SPECMATIC" \
  -e SUT_PORT="$SUT_PORT" \
  -v "$ROOT_DIR:/workspace" \
  -w /workspace \
  "$SPECMATIC_IMAGE" run-suite --config specmatic.yaml
