# Specmatic Sample: Express BFF (REST/OpenAPI)

This sample demonstrates how Specmatic contract tests a TypeScript Express BFF over REST/OpenAPI. Specmatic reads the OpenAPI contracts, starts a backend dependency mock from the provider contract, auto-generates requests to the BFF, and validates the responses against the BFF contract.

## Why Specmatic

- **Auto-generated tests from your API spec** - Specmatic reads OpenAPI and generates contract tests automatically.
- **Intelligent service virtualisation** - the backend REST API is mocked from the same contract the backend provider owns.
- **Backward compatibility detection** - contract changes can be checked before they break consumers.
- **Works with existing OpenAPI specs** - no new DSL is needed for this REST sample.

## Tech

1. TypeScript BFF written with Express 5.2.1
2. Specmatic Docker CLI using `specmatic/specmatic:latest`
3. Node.js 24+ and Docker

## Run Contract Tests

### Prerequisites

- Node.js 24+
- Docker with the daemon running

### Using npm

```bash
npm install
npm test
```

First run takes 1-2 minutes as Specmatic clones the contract repository. Subsequent runs are fast because the contracts are cached in `.specmatic/`.

The [specmatic.yaml](specmatic.yaml) file points to the BFF system-under-test contract and the backend dependency mock contract.

### Using Docker

```bash
docker build -t bff-rest-typescript-express-rest-api .
docker run --rm -p 8080:8080 -e STUB_BASE_URL=http://host.docker.internal:8090 bff-rest-typescript-express-rest-api
```

### Test Modes

This sample ships with `schemaResiliencyTests: none` for fast, predictable tests. You can increase test coverage by changing the mode in `specmatic.yaml`:

| Mode | What it does |
|------|-------------|
| `none` | Runs tests from named examples only |
| `positiveOnly` | Adds all valid input combinations |
| `all` | Adds negative and boundary tests |

```yaml
specmatic:
  settings:
    test:
      schemaResiliencyTests: all
```

## How It Works

BFF Spec -> `specmatic.yaml` -> Specmatic generates requests to the BFF -> the BFF calls the Specmatic backend mock -> Specmatic validates both sides.

When `npm test` runs, the test adapter starts the Express app on `SUT_PORT`, invokes `docker run specmatic/specmatic:latest test --config specmatic.yaml`, and lets Specmatic start the backend dependency mock declared under `dependencies.services`.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/products` | Create a product through the backend dependency |
| GET | `/findAvailableProducts` | Search available products |
| POST | `/orders` | Create an order through the backend dependency |
| GET | `/orders` | Retrieve orders |
| GET | `/monitor/{id}` | Retrieve monitor status |

## BFF to Backend Mapping

| BFF operation | Backend dependency operation | Adapter behavior |
|---------------|------------------------------|------------------|
| `POST /products` | `POST /products` | Forwards JSON body and adds backend-only `Authenticate` and `Idempotency-Key` headers |
| `GET /findAvailableProducts` | `GET /products` | Translates path, forwards `type`, `from-date`, `to-date`, and `pageSize` |
| `POST /orders` | `POST /orders` | Forwards JSON body and adds backend-only `Authenticate` and `Idempotency-Key` headers |
| `GET /orders` | `GET /orders` | Translates backend status `fulfilled` to BFF status `completed` |
| `GET /monitor/{id}` | Local BFF response | Returns monitor response shaped by the BFF contract |

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `SUT_HOST` | `0.0.0.0` | Express bind host |
| `SUT_PORT` | `8080` | Express bind port |
| `SUT_BASE_URL` | `http://host.docker.internal:8080` | URL Specmatic Docker uses to reach the BFF |
| `STUB_BASE_URL` | `http://localhost:8090` | Backend mock URL used by the BFF and Specmatic |
| `BACKEND_API_KEY` | `sample-api-key` | Sample value for backend-only `Authenticate` header |
| `SPECMATIC_DOCKER_IMAGE` | `specmatic/specmatic:latest` | Specmatic Docker image |

## Project Structure

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Contract test configuration |
| `src/app.ts` | Express routes for the BFF contract |
| `src/backendClient.ts` | Adapter from BFF operations to backend dependency operations |
| `test/contract.test.ts` | Docker CLI contract test adapter |
| `Dockerfile` | Production container image |
| `.github/workflows/ci.yml` | CI pipeline for test and Docker build |

## For More Info

- [Specmatic Website](https://specmatic.io)
- [Specmatic Documentation](https://docs.specmatic.io)
- [BFF contract](https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/openapi/product_search_bff_v6.yaml)
- [Backend dependency contract](https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/openapi/api_order_v5.yaml)
