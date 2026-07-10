# Verification: bff-rest-java-spring-boot (REST + Kafka)

**Date:** 2026-07-09
**Verified by:** Kiro CLI (automated checklist)
**Result:** ✅ PASS
**PR #53 validated:** Async dependencies wired correctly when user provides them.

---

## B1. Enterprise Runtime ✅

| Check | Status | Detail |
|---|---|---|
| Enterprise artifact | ✅ | `io.specmatic.enterprise:executable-all:1.19.1` |
| No forbidden refs | ✅ | Clean |
| CLI mode direct | ✅ | `java -jar target/specmatic/specmatic-enterprise.jar run-suite` |
| Pinned version | ✅ | `1.19.1` |

## B2. Config Schema (v3) ✅

| Check | Status | Detail |
|---|---|---|
| Single specmatic.yaml | ✅ | |
| v3 structure | ✅ | `version: 3`, `$ref` wiring |
| Settings under `specmatic:` | ✅ | |

## B3. Progressive Resiliency ✅

| Level | Tests | Passed | Increasing? |
|---|---|---|---|
| `none` | 18 | 17 | — |
| `positiveOnly` | 34 | 32 | ✅ |
| `all` | 228 | 224 | ✅ |

Shipped: `all`, 4 WIP (GET /orders tagged WIP in contract) ✅

## B4. Coverage & Discovery ✅

| Check | Status | Detail |
|---|---|---|
| `actuatorUrl` | ✅ | `{SUT_BASE_URL}/actuator/mappings` |
| Filter | ✅ | `PATH!='/actuator/health,/actuator/mappings'` |
| Coverage | ✅ | 92% enforced |

## B5. Reports & Governance ✅

| Check | Status | Detail |
|---|---|---|
| `html` + `ctrf` | ✅ | |
| `enforce: true` | ✅ | |
| Thresholds | ✅ | `minCoveragePercentage: 92`, `maxMissedOperationsInSpec: 1` |

## B6. Files, Manifest, CI ✅

| Check | Status | Detail |
|---|---|---|
| Required files | ✅ | All present |
| Manifest | ✅ | Complete with runtime, coverage, learnings |
| Dockerfile from source | ✅ | Multi-stage `maven:3.9.9-eclipse-temurin-17` |
| CI matrix | ✅ | `ubuntu-latest, macos-latest, windows-latest` |
| Architecture GIF | ✅ | Present |
| No orphaned files | ✅ | Single package namespace |

## B7. BFF Dependency Boundary Integrity ✅

| Check | Status | Detail |
|---|---|---|
| No blanket catch/fabrication | ✅ | `restTemplate` responses relayed directly |
| Mock responses relayed | ✅ | `dependencyResponse.getBody()` used |
| `Specmatic-Response-Code` | ✅ | Branches for 202 and 429 |

## BFF-specific: Dependencies Match User Input ✅

| User provided | Wired? | Protocol | Implementation |
|---|---|---|---|
| `api_order_v5.yaml` | ✅ | `openapi` mock | `RestTemplate` calls to REST mock |
| `kafka.yaml` | ✅ | `asyncapi` mock | `ProductAuditPublisher` via `KafkaTemplate` |

## BFF-specific: Kafka Wiring ✅

| Check | Status | Detail |
|---|---|---|
| Kafka in specmatic.yaml | ✅ | `productAuditsKafka` with in-memory broker |
| Publisher implemented | ✅ | Sends to `product-queries` topic |
| Message schema matches | ✅ | `{name, inventory, id, categories}` |
| Triggered on product creation | ✅ | `publishCreatedProduct()` called after REST dep response |

## Minor Issues (non-blocking)

| Issue | Severity | Detail |
|---|---|---|
| `mvnw` is 32 bytes | 🟡 Low | Placeholder — CI relies on Maven being available |
| Test adapter is `main()` | 🟡 Low | Not JUnit `@Test` — functionally correct for cli mode |

## Skipped

| Check | Reason |
|---|---|
| Docker build | Docker daemon unavailable locally |
