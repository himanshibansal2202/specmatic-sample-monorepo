# Specmatic Sample: Spring Boot Order API (REST/OpenAPI)

Table of Contents
* [Background](#background)
* [Why Specmatic](#why-specmatic)
* [Tech](#tech)
* [Run Contract Tests](#run-contract-tests)
* [How It Works](#how-it-works)
* [Configuration](#configuration)
* [Project Structure](#project-structure)
* [For More Info](#for-more-info)

## Background

This backend sample uses Specmatic to auto-generate REST contract tests for an in-memory Order API from the [Order API V5 OpenAPI specification](https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/openapi/api_order_v5.yaml). The service owns Product and Order state and uses an in-memory Inventory boundary; it has no external dependency that needs a Specmatic mock.

## Why Specmatic

* **Auto-generated tests from your API spec** - no hand-written contract cases to maintain.
* **Intelligent service virtualisation** - the same contracts can create realistic dependency stubs.
* **Backward compatibility detection** - breaking contract changes are caught before production.
* **Single source of truth** - the existing OpenAPI spec drives tests and documentation without a new DSL.

![Specmatic Order backend architecture](assets/specmatic-order-backend-architecture.gif)

## Tech

1. Spring Boot 4.1.0 service written in Java 17
2. Specmatic Enterprise `io.specmatic.enterprise:executable-all:1.19.1`
3. Maven 3.6.3 or newer via the included wrapper
4. JDK 17

## Run Contract Tests

Prerequisites: JDK 17 and a valid `SPECMATIC_LICENSE_KEY`. Maven downloads the official Enterprise CLI artifact from Maven Central.

For **Unix/macOS** and **Windows PowerShell**:
```shell
./mvnw test
```

For **Windows Command Prompt**:
```shell
mvnw.cmd test
```

The first run may take 1-2 minutes as Specmatic clones the configured contract repository. Subsequent runs are fast because it is cached in `.specmatic/`. [specmatic.yaml](specmatic.yaml) selects the contract, test endpoint, resiliency mode, and reports.

### View Specmatic Test Reports

After tests, open the Specmatic HTML report in [build/reports/specmatic](build/reports/specmatic/). The same directory contains CTRF output for CI tooling.

### Test Modes

This sample ships with `schemaResiliencyTests: all`. Change it under `specmatic.settings.test` in `specmatic.yaml`:

| Mode | What it does |
|------|-------------|
| `none` | Runs named examples only |
| `positiveOnly` | Adds valid input combinations |
| `all` | Adds negative and boundary tests (default) |

## How It Works

`OpenAPI spec -> specmatic.yaml -> generated requests -> Spring Boot service -> contract validation`

The test starts the service, then invokes the Enterprise CLI directly. Specmatic reads `specmatic.yaml`, fetches the contract, generates requests, and validates every response against the OpenAPI schemas and examples.

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `SUT_PORT` | `8080` | Application port |
| `SUT_BASE_URL` | `http://localhost:8080` | Base URL used by Specmatic |
| `SPECMATIC_LICENSE_KEY` | none | Enterprise license key |

## Project Structure

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Contract source and test configuration |
| `src/main/java` | API, in-memory state, and Inventory boundary |
| `src/test/.../ContractTest.java` | Starts the app and invokes the Enterprise CLI |
| `Dockerfile` | Multi-stage production image |
| `.github/workflows/ci.yml` | Cross-platform tests and Docker build |

## For More Info

* [Specmatic Website](https://specmatic.io)
* [Specmatic Documentation](https://docs.specmatic.io)
* [Order API V5 contract](https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/openapi/api_order_v5.yaml)
