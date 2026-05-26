# Specmatic Sample: ASP.NET Core Backend (gRPC)

This sample demonstrates how Specmatic contract tests an ASP.NET Core gRPC backend in isolation. Specmatic reads the Protobuf service definitions, auto-generates gRPC requests, sends them to the running service, and validates responses against the contract.

## Why Specmatic

- **Auto-generated tests from your API spec** - Specmatic reads your gRPC Protobuf spec and generates test cases automatically.
- **Intelligent service virtualisation** - the same contract can drive realistic mocks for consumers.
- **Backward compatibility detection** - breaking contract changes are caught before they reach production.
- **Single source of truth** - one spec drives tests, stubs, and compatibility checks.
- **gRPC-aware validation** - Protobuf service definitions are tested without spinning up dependent services.

## Tech

1. ASP.NET Core 8.0 gRPC service written in C#
2. Specmatic Enterprise Docker image through .NET Testcontainers
3. .NET SDK 8.0 and Docker

## Run Contract Tests

### Prerequisites

- .NET SDK 8.0
- Docker
- Access to `specmatic/enterprise:latest` for gRPC/Protobuf support

First run takes 1-2 minutes as Specmatic clones the contract repository. Subsequent runs are fast because contracts are cached in `.specmatic/`.

### Using the build tool

```bash
dotnet restore
dotnet build --no-restore
dotnet test --no-build
```

The test adapter starts this gRPC app, exposes the selected host port to Testcontainers, and runs the `specmatic/enterprise:latest` container against [specmatic.yaml](specmatic.yaml).

### Using Docker

```bash
docker build -t backend-grpc-dotnet-aspnet-core-in-memory .
docker run --rm -p 8080:8080 backend-grpc-dotnet-aspnet-core-in-memory
```

### Test Modes

This sample ships with `schemaResiliencyTests: none` for fast, predictable tests.
You can increase test coverage by changing the mode in `specmatic.yaml`:

| Mode | What it does |
|------|-------------|
| `none` | Runs tests from named examples only (default) |
| `positiveOnly` | Adds all valid input combinations |
| `all` | Adds negative/boundary tests |

To enable, update `specmatic.yaml`:

```yaml
specmatic:
  settings:
    test:
      schemaResiliencyTests: all
```

## How It Works

Protobuf contract -> `specmatic.yaml` -> Specmatic generates gRPC calls -> ASP.NET Core service responds -> Specmatic validates responses against the Protobuf spec and examples.

When you run `dotnet test`, the xUnit adapter starts the backend on a free local port, runs the Specmatic Enterprise Docker container, and streams failures back through the test result.

## Services

| Service | Methods |
|---------|---------|
| `com.store.ProductService` | `SearchProducts`, `GetProduct`, `AddProduct`, `UpdateProduct`, `DeleteProduct` |
| `com.store.OrderService` | `SearchOrders`, `GetOrder`, `AddOrder`, `UpdateOrder`, `DeleteOrder`, `EmptyOrder` |

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `SUT_PORT` | `8080` | Application gRPC port |
| `SPECMATIC_SUT_HOST` | `host.docker.internal` | Hostname Specmatic uses to call the app from Docker |
| `PROTOC_VERSION` | `3.23.4` | Protobuf compiler version used by Specmatic |
| `SPECMATIC_REQUEST_TIMEOUT_MS` | `10000` | Specmatic gRPC request timeout |
| `SPECMATIC_IMAGE` | `specmatic/enterprise:latest` | Docker image used by the test adapter |

## Project Structure

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Contract test configuration pointing to the Protobuf contracts |
| `src/StoreGrpcBackend` | ASP.NET Core gRPC application |
| `src/StoreGrpcBackend/Protos` | Protobuf files used by the generated server |
| `tests/StoreGrpcBackend.ContractTests` | xUnit Testcontainers adapter that runs Specmatic |
| `Dockerfile` | Production container image |
| `.github/workflows/ci.yml` | CI pipeline for tests and Docker build |

## For More Info

- [Specmatic Website](https://specmatic.io)
- [Specmatic Documentation](https://docs.specmatic.io)
- [Contract repository](https://github.com/specmatic/specmatic-order-contracts/tree/main/io/specmatic/examples/store/grpc/order_api)
