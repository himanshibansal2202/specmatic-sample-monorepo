# Verification: bff-graphql-java-spring-boot (GraphQL + REST)

**Date:** 2026-07-13
**Verified by:** OpenAI Codex
**Result:** PASS

---

## B1. Enterprise Runtime

| Check | Status | Detail |
|---|---|---|
| Enterprise artifact | PASS | `io.specmatic.enterprise:executable-all:1.19.1` |
| CLI mode direct | PASS | `java -jar target/specmatic/specmatic-enterprise.jar run-suite --config specmatic.yaml` |
| Pinned version | PASS | `1.19.1` |
| Java runtime | PASS | Java 17 |

## B2. Config Schema

| Check | Status | Detail |
|---|---|---|
| Single config | PASS | `specmatic.yaml` |
| v3 structure | PASS | `version: 3`, `components`, `$ref` wiring |
| GraphQL SUT | PASS | `graphqlsdl` test run option on `127.0.0.1:8080` |
| REST dependency mock | PASS | `openapi` mock for `api_order_v5.yaml` on `127.0.0.1:8090` |

## B3. Progressive Resiliency

| Level | Tests | Passed | Failed | Increasing? |
|---|---:|---:|---:|---|
| `none` | 3 | 3 | 0 | - |
| `positiveOnly` | 29 | 29 | 0 | PASS |
| `all` | 84 | 84 | 0 | PASS |

Shipped mode: `schemaResiliencyTests: all`.

## B4. Coverage & Reports

| Check | Status | Detail |
|---|---|---|
| GraphQL operations covered | PASS | 3 of 3 |
| API coverage | PASS | 100% in CTRF execution details |
| Report formats | PASS | HTML and CTRF |
| Report directory | PASS | `build/reports/specmatic` |

Note: Specmatic console reports 84 successful tests for `all`; the generated GraphQL CTRF summary records 81 report entries for the same passing run.

## B5. Files, CI, Docker

| Check | Status | Detail |
|---|---|---|
| Maven build | PASS | `./mvnw -q -Dskip.contract.tests=true package` |
| Contract tests | PASS | `./mvnw test` |
| Docker build | PASS | `docker build -t bff-graphql-java-spring-boot .` |
| Sample CI | PASS | Java 17 matrix across Ubuntu, macOS, Windows |
| Root CI | PASS | Added `bff-graphql-java-spring-boot` job |
| Manifest | PASS | Runtime, files, counts, and learnings recorded |

## BFF-specific Boundary Checks

| Check | Status | Detail |
|---|---|---|
| `findAvailableProducts` backend mapping | PASS | GraphQL `type` to REST query, optional `pageSize` to REST header |
| `createProduct` backend mapping | PASS | REST `POST /products` with `Authenticate` and `Idempotency-Key` |
| `findOffersForDate` ownership | PASS | Implemented in BFF because the REST dependency spec has no offers endpoint |
| Date scalar error handling | PASS | Invalid date values return GraphQL errors instead of framework 500 JSON |
