# Specmatic Sample: Ktor Backend (gRPC)

This sample shows a Kotlin backend that exposes Product and Order gRPC services and uses Specmatic Enterprise to generate contract tests directly from the Protobuf contracts. The gRPC API is served with grpc-java, while Ktor provides the backend management endpoint.

## Contracts

* [Product gRPC spec](https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/grpc/order_api/product.proto) is used for running contract tests against `ProductService`.
* [Order gRPC spec](https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/grpc/order_api/order.proto) is used for running contract tests against `OrderService`.

## Why Specmatic

Specmatic auto-generates executable tests from the Protobuf service definitions, so the backend is checked against the same API contract that consumers use. It validates the RPC method, request message, response message, and examples without maintaining a parallel hand-written contract test suite.

For gRPC services, Specmatic tests your Protobuf service definitions without spinning up dependent services. This sample also keeps Product and Order state in memory and uses an in-memory Inventory boundary to model backend behavior during tests.

## Prerequisites

* JDK 17
* Maven 3.9+ or the included Maven wrapper
* Specmatic Enterprise license configured with `SPECMATIC_LICENSE_KEY`
* Specmatic Enterprise runtime artifact: `io.specmatic.enterprise:grpc-min:1.18.0`

## Run The Tests

Unix/macOS:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Windows Command Prompt:

```cmd
mvnw.cmd test
```

First run takes 1-2 minutes as Specmatic clones the contract repository. Subsequent runs are fast because contracts are cached in `.specmatic/`.

## Run The Service

Unix/macOS:

```bash
./mvnw -DskipTests package
java -jar target/backend-grpc-kotlin-ktor-1.0.0.jar
```

Windows PowerShell:

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\backend-grpc-kotlin-ktor-1.0.0.jar
```

Windows Command Prompt:

```cmd
mvnw.cmd -DskipTests package
java -jar target\backend-grpc-kotlin-ktor-1.0.0.jar
```

The gRPC server listens on `SUT_PORT` and defaults to `8080`. The Ktor management endpoint listens on `MANAGEMENT_PORT` and defaults to `8081`.

## View Specmatic Test Reports

After the test command runs, Specmatic writes the HTML report here:

[build/reports/specmatic/grpc/test/html/index.html](build/reports/specmatic/grpc/test/html/index.html)

## Test Modes

`specmatic.yaml` controls the test generation mode:

```yaml
specmatic:
  settings:
    test:
      schemaResiliencyTests: all
```

Use `none` for named examples only, `positiveOnly` for valid generated combinations, and `all` for full resiliency tests including invalid and boundary cases.

## How It Works

```text
Protobuf specs -> specmatic.yaml -> Specmatic generates gRPC calls -> Ktor backend process serves grpc-java services -> Specmatic validates responses
```

When `./mvnw test` runs, the JUnit contract test starts the app, the native Specmatic Enterprise gRPC test integration reads `specmatic.yaml`, fetches the contracts from GitHub, generates gRPC test cases, and validates the backend responses against the executable contract.

## Project Structure

| Path | Purpose |
| --- | --- |
| `specmatic.yaml` | Contract test configuration and gRPC runtime options |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper` | Maven wrapper for reproducible local and CI runs |
| `src/main/proto` | Protobuf contracts used to generate server interfaces |
| `src/main/kotlin/io/specmatic/samples/store/grpc` | Product and Order gRPC service implementations |
| `src/main/kotlin/io/specmatic/samples/store/persistence` | In-memory Product and Order store |
| `src/main/kotlin/io/specmatic/samples/store/inventory` | In-memory Inventory dependency boundary |
| `src/test/kotlin/io/specmatic/samples/store/ContractTest.kt` | Native Specmatic Enterprise gRPC contract test adapter |
| `Dockerfile` | Production image built from source |

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `SUT_PORT` | `8080` | gRPC server port used by the app and Specmatic |
| `SPECMATIC_SUT_HOST` | `localhost` | Host Specmatic uses to reach the gRPC server |
| `SPECMATIC_REQUEST_TIMEOUT_MS` | `10000` | gRPC request timeout for Specmatic |
| `MANAGEMENT_PORT` | `8081` | Ktor management endpoint port |

## Links

* [Specmatic Website](https://specmatic.io)
* [Specmatic Documentation](https://docs.specmatic.io)
* [Specmatic Configuration](specmatic.yaml)
