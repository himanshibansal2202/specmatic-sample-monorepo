# Backend REST Java Spring Boot In-Memory

This sample shows a Spring Boot backend using Specmatic native JUnit 5 contract tests. The application implements the Order API from the central contract repository and keeps product and order state in memory so the contract examples are immediately executable.

## Why Specmatic

Specmatic turns the OpenAPI contract into executable tests. The same `specmatic.yaml` points to the central contract repository, configures the system under test, and lets the JUnit test run the contract without a separate hand-written test suite for each endpoint.

## How It Works

- Spring Boot serves the REST API on `http://localhost:8080` by default.
- `ContractTest` implements `io.specmatic.test.SpecmaticContractTest`, so `./mvnw test` runs Specmatic through JUnit 5.
- The checked-in `specmatic.yaml` resolves `io/specmatic/examples/store/openapi/api_order_v5.yaml` from `https://github.com/specmatic/specmatic-order-contracts.git`.
- The in-memory store is seeded with product and order IDs used by the contract examples.

## Run Locally

```bash
./mvnw test
```

Use these overrides when needed:

```bash
SUT_PORT=18080 SUT_BASE_URL=http://localhost:18080 ./mvnw test
```

## Contract Coverage

The sample implements:

- `GET /products/{id}`
- `PATCH /products/{id}`
- `DELETE /products/{id}`
- `PUT /products/{id}/image`
- `GET /products`
- `POST /products`
- `POST /orders`
- `GET /orders`
- `GET /orders/{id}`
- `PATCH /orders/{id}`
- `DELETE /orders/{id}`

Write operations accept the contract's `Authenticate` API-key header. Create operations require `Idempotency-Key`.

## Resiliency Tests

The delivered `specmatic.yaml` uses:

```yaml
specmatic:
  settings:
    test:
      schemaResiliencyTests: none
```

To explore more generated cases, change it to `positiveOnly` or `all`, then run `./mvnw test` again.

## Build Container

```bash
docker build -t backend-rest-java-spring-boot-in-memory .
docker run --rm -p 8080:8080 backend-rest-java-spring-boot-in-memory
```
