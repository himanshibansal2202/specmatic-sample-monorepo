# Specmatic Sample: React Frontend (REST/OpenAPI)

This sample demonstrates how Specmatic contract tests a React/TypeScript frontend that consumes a BFF REST API — no running backend, no hand-written mocks, no integration environment needed.

## Why Specmatic

- **Auto-generated stubs from your API spec** — Specmatic reads the OpenAPI spec and generates a realistic mock BFF automatically. No hand-written mocks to maintain.
- **Contract compliance verification** — Your frontend's HTTP requests are validated against the spec, catching integration bugs before deployment.
- **Single source of truth** — one spec drives stubs and contract validation. No drift between frontend expectations and backend reality.
- **Works with your existing OpenAPI spec** — no new DSL to learn.

## Tech

1. React 18 frontend written in TypeScript
2. Specmatic (docker-cli mode) for contract stub and validation
3. Node.js 20+, Docker

## Run Contract Tests

### Prerequisites
- Node.js 20+
- Docker

### Run tests
```bash
npm install
npm test
```

First run takes 1-2 minutes as Specmatic clones the contract repository. Subsequent runs are fast (cached in `.specmatic/`).

The test starts a Specmatic stub of the BFF contract via Docker, then exercises the frontend's API client against it.

### Test Modes

This sample ships with `schemaResiliencyTests: none` for fast, predictable tests. You can increase test coverage by changing the mode in `specmatic.yaml`:

| Mode | What it does |
|------|-------------|
| `none` | Runs tests from named examples only (default) |
| `positiveOnly` | Adds all valid input combinations |
| `all` | Adds negative/boundary tests (expects 400 for invalid inputs) |

To enable, update `specmatic.yaml`:
```yaml
specmatic:
  settings:
    test:
      schemaResiliencyTests: all
```

## How It Works

```
BFF Spec → specmatic.yaml → Specmatic mocks the BFF API (Docker) → Your frontend calls the mock → Contract compliance verified
```

When you run `npm test`, the test adapter starts a Specmatic Docker container that stubs the BFF contract, then the frontend's API client makes HTTP calls to the stub. Specmatic validates that all requests conform to the OpenAPI spec.

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| STUB_PORT | 8090 | Specmatic stub port |
| BFF_BASE_URL | http://localhost:8090 | BFF API base URL for the frontend |
| STUB_BASE_URL | http://localhost:8090 | Specmatic stub base URL in specmatic.yaml |

## Project Structure

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Contract test configuration — points to the BFF API spec |
| `src/api/bffClient.ts` | API client module that calls the BFF |
| `src/components/App.tsx` | React UI component |
| `test/contract.test.ts` | Contract test adapter — starts stub and exercises client |
| `Dockerfile` | Production container image |
| `.github/workflows/ci.yml` | CI pipeline: test + Docker build |

## For More Info

- [Specmatic Website](https://specmatic.io)
- [Specmatic Documentation](https://docs.specmatic.io)
- [BFF Contract](https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/openapi/product_search_bff_v6.yaml)
