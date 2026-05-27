# Specmatic Sample: Express Backend (REST/OpenAPI)

## What This Is

This sample demonstrates how Specmatic contract tests an Express backend REST API in isolation. Specmatic reads the OpenAPI contract, generates provider-side requests automatically, sends them to this service, and validates every response against the executable API spec.

## Why Specmatic

- **Auto-generated tests from your API spec** - Specmatic reads OpenAPI and generates test cases automatically. No hand-written contract test cases to maintain.
- **Intelligent service virtualisation** - the same contract can generate realistic stubs so consumers and providers can develop independently.
- **Backward compatibility detection** - Specmatic can compare spec versions and flag breaking API changes before they reach production.
- **Works with your existing OpenAPI spec** - no new DSL is required.

## Tech

1. JavaScript backend service using Express 5.2.1
2. Specmatic CLI through the `specmatic` npm package
3. Node.js 20+ and JRE 17+

## Run Contract Tests

### Prerequisites

- Node.js 20+
- JRE 17+

### Using npm

```bash
npm install
npm test
```

First run takes 1-2 minutes as Specmatic clones the contract repository. Subsequent runs are fast because contracts are cached in `.specmatic/`.

The [specmatic.yaml](specmatic.yaml) file points Specmatic at `io/specmatic/examples/store/openapi/api_order_v5.yaml` in the central order contracts repository and configures the test endpoint.

### Test Modes

This sample ships with `schemaResiliencyTests: none` for fast, predictable tests.
You can increase test coverage by changing the mode in `specmatic.yaml`:

| Mode | What it does |
|------|-------------|
| `none` | Runs tests from named examples only (default) |
| `positiveOnly` | Adds all valid input combinations |
| `all` | Adds negative/boundary tests and expects 400 responses for invalid inputs |

To enable:

```yaml
specmatic:
  settings:
    test:
      schemaResiliencyTests: all
```

## How It Works

```text
OpenAPI spec -> specmatic.yaml -> Specmatic generates requests -> Express service responds -> Specmatic validates responses against the spec
```

When `npm test` runs, the test adapter starts the Express app on `SUT_PORT`, invokes `npx specmatic test`, and fails if Specmatic reports any contract mismatch.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/products` | List products, optionally filtered by type/date |
| POST | `/products` | Create a product |
| GET | `/products/{id}` | Fetch product details |
| PATCH | `/products/{id}` | Update product details |
| DELETE | `/products/{id}` | Delete a product |
| PUT | `/products/{id}/image` | Upload or update a product image |
| GET | `/orders` | List orders |
| POST | `/orders` | Create an order |
| GET | `/orders/{id}` | Fetch order details |
| PATCH | `/orders/{id}` | Update order details |
| DELETE | `/orders/{id}` | Cancel an order |

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `SUT_HOST` | `127.0.0.1` | Host used by the Express app |
| `SUT_PORT` | `8080` | Port used by the Express app |
| `SUT_BASE_URL` | `http://localhost:8080` | Base URL used by Specmatic tests |

## Project Structure

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Contract test configuration pointing to the OpenAPI spec |
| `src/app.js` | Express routes that implement the contract |
| `src/store.js` | In-memory products and orders store seeded from contract examples |
| `test/contract.test.js` | Test adapter that starts the app and runs Specmatic CLI |
| `Dockerfile` | Production container image |
| `.github/workflows/ci.yml` | CI pipeline for contract tests and Docker build |

## For More Info

- [Specmatic Website](https://specmatic.io)
- [Specmatic Documentation](https://docs.specmatic.io)
- [Contract used by this sample](https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/openapi/api_order_v5.yaml)
