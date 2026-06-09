# BFF REST JavaScript Express In Memory

This sample demonstrates a JavaScript Express BFF verified from executable Specmatic contracts. The BFF exposes the Store product and order API to clients, delegates product/order work to a backend mock generated from the provider contract, and keeps dependency endpoints configurable for local and CI runs.

## Stack

- Application type: BFF
- Protocol: REST/OpenAPI
- Language runtime: Node.js 24 LTS
- Framework: Express 5.1
- Data layer: none; BFF state is delegated to dependency contracts
- Specmatic integration mode: native JavaScript package

## Contracts

The checked-in `specmatic.yaml` is the only Specmatic configuration source.

- System under test: `io/specmatic/examples/store/openapi/product_search_bff_v6.yaml`
- REST dependency mock: `io/specmatic/examples/store/openapi/api_order_v5.yaml`
- Kafka dependency mock: `io/specmatic/examples/store/asyncapi/kafka.yaml`
- Contract repository: `https://github.com/specmatic/specmatic-order-contracts.git`

## Operation Mapping

| BFF operation | Dependency behavior |
| --- | --- |
| `POST /products` | Calls backend `POST /products` with the same JSON body, plus dependency-only `Authenticate` and `Idempotency-Key` headers. |
| `GET /findAvailableProducts` | Calls backend `GET /products`, forwarding `type`, `from-date`, `to-date`, and `pageSize` where present. |
| `POST /orders` | Calls backend `POST /orders` with the same JSON body, plus dependency-only `Authenticate` and `Idempotency-Key` headers. |
| `GET /orders` | Calls backend `GET /orders`; when `orderId` is present, calls `GET /orders/{id}`, returns the object inside the BFF array response, and maps backend status `fulfilled` to BFF status `completed`. |
| `GET /monitor/{id}` | Returns the BFF monitor response directly because no REST backend operation owns this endpoint. |

For `POST /products` and `POST /orders`, the BFF returns the asynchronous `202` response when Specmatic sends `Specmatic-Response-Code: 202`; the normal example flow delegates to the backend and returns `201`.

## Run

```bash
npm install
npm test
```

Start the BFF manually:

```bash
npm start
```

## Configuration

| Variable | Default |
| --- | --- |
| `SUT_PORT` | `8080` |
| `SUT_BASE_URL` | `http://localhost:8080` |
| `STUB_BASE_URL` | `http://localhost:8090` |
| `STUB_PORT` | `8090` |
| `BROKER_HOST` | `localhost` |
| `BROKER_PORT` | `9092` |
| `BROKER_URL` | `localhost:9092` |
| `BACKEND_AUTHENTICATE` | `sample-api-key` |
| `IDEMPOTENCY_KEY` | `00000000-0000-4000-8000-000000000001` |

The checked-in resiliency level is `all`, so `npm test` runs the named examples, positive generated cases, and negative generated cases. The verified full run covers 228 scenarios and reports 92% API coverage.

The Kafka dependency is declared in `specmatic.yaml` because the BFF contract family includes it. The current JavaScript native package verifies the REST BFF through the OpenAPI test and HTTP stub APIs; if your environment requires executable AsyncAPI/Kafka mock verification, use a Specmatic Enterprise runtime mode for that dependency.

At full resiliency, Specmatic generates two WIP-tagged negative cases for `GET /orders` even though that operation declares only a `200` response. The adapter preserves those WIP results in the reports and fails the build only on non-WIP contract failures, matching Specmatic's WIP semantics.
