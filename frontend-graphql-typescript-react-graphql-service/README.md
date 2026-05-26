# Specmatic Sample: React Frontend (GraphQL)

This sample demonstrates a React frontend that consumes a GraphQL BFF contract. Specmatic starts a GraphQL mock from the SDL and examples, and the frontend client tests verify that the UI-facing calls stay aligned with the executable contract.

## Why Specmatic

- **Auto-generated mocks from your API spec** - Specmatic reads the GraphQL SDL and examples to create a realistic BFF mock.
- **Intelligent service virtualisation** - the frontend can be developed and tested without a running BFF.
- **Backward compatibility detection** - the same contract can be used to catch breaking API changes before rollout.
- **GraphQL schema compliance** - queries, mutations, scalar values, enums, headers, and response shapes are validated from the SDL.

## Tech

1. React 19.2.6 frontend written in TypeScript 6.0.3
2. Vite 8.0.14 for development and production build
3. Specmatic Enterprise Docker image running a GraphQL mock from `specmatic.yaml`
4. Node.js 20.19.0+ and Docker

## Run Contract Tests

### Prerequisites

- Node.js 20.19.0 or newer
- Docker
- Specmatic Enterprise access/license for GraphQL support

### Using the build tool

```bash
npm install
npm test
```

First run takes 1-2 minutes as Specmatic clones the contract repository. Subsequent runs are fast because the contracts are cached in `.specmatic/`.

[specmatic.yaml](specmatic.yaml) points to the central contract repository and configures the GraphQL BFF mock used by the tests.

### Test Modes

This frontend uses Specmatic in mock mode, so `schemaResiliencyTests` does not change the mock request count. The sample still ships with the standard setting:

| Mode | What it does |
|------|-------------|
| `none` | Keeps generated samples fast and predictable |
| `positiveOnly` | Used for provider-side generated tests |
| `all` | Used for provider-side negative and boundary tests |

To change it, update `specmatic.yaml`:

```yaml
specmatic:
  settings:
    test:
      schemaResiliencyTests: all
```

## How It Works

```text
BFF GraphQL SDL -> specmatic.yaml -> Specmatic mocks the BFF API -> React client calls the mock -> Contract compliance verified
```

When `npm test` runs, Vitest starts the Specmatic Enterprise Docker image in mock mode, then calls the same `ProductsBffClient` used by the React app. The client exercises `findAvailableProducts`, `createProduct`, and `findOffersForDate` with the request shapes and headers declared by the GraphQL contract examples.

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `VITE_BFF_BASE_URL` | `http://localhost:9000` | BFF mock/service base URL for the app |
| `VITE_BFF_GRAPHQL_URL` | `http://localhost:9000/graphql` | Full GraphQL endpoint for the app |
| `VITE_REGION` | `north-west` | Region sent as `X-region` for product search |
| `STUB_HOST` | `localhost` | Host used by Specmatic mock |
| `STUB_PORT` | `9000` | Host port mapped to the Specmatic GraphQL mock |
| `SPECMATIC_IMAGE` | `specmatic/enterprise:latest` | Docker image used for GraphQL service virtualization |
| `SPECMATIC_LICENSE_PATH` | unset | Optional Enterprise license path passed into the container |
| `FRONTEND_PORT` | `3000` | Vite dev or preview server port |

## Project Structure

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Contract mock configuration pointing to the GraphQL BFF SDL |
| `src/productsBffClient.ts` | Contract-derived GraphQL client used by tests and UI |
| `src/App.tsx` | React workflows for searching products, creating products, and finding offers |
| `test/productsBffClient.contract.test.ts` | Contract consumption tests that call the Specmatic mock |
| `test/specmaticMock.ts` | Docker CLI adapter that starts and stops the Specmatic mock |
| `Dockerfile` | Production container image for the built frontend |
| `.github/workflows/ci.yml` | CI pipeline for install, tests, build, and Docker image build |

## For More Info

- [Specmatic Website](https://specmatic.io)
- [Specmatic Documentation](https://docs.specmatic.io)
- Contract: <https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/graphql/products_bff.graphqls>
