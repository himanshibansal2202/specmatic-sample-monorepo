# Verification: backend-rest-java-spring-boot (cli mode)

**Date:** 2026-07-10
**Verified by:** Kiro CLI (automated checklist)
**Result:** ✅ PASS

---

## B1. Enterprise Runtime ✅

| Check | Status | Detail |
|---|---|---|
| Enterprise artifact | ✅ | `io.specmatic.enterprise:executable-all:1.19.1` |
| No forbidden refs | ✅ | |
| CLI mode direct | ✅ | `java -jar` via Maven lifecycle |
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
| `none` | 31 | 31 | — |
| `positiveOnly` | 74 | 74 | ✅ |
| `all` | 293 | 293 | ✅ |

Shipped: `all` ✅

## B4. Coverage & Discovery ✅

| Check | Status | Detail |
|---|---|---|
| `actuatorUrl` | ✅ | `{SUT_BASE_URL}/actuator/mappings` |
| Filter | ✅ | `PATH!='/actuator/.*'` |
| Coverage | ✅ | 68% measured and enforced |

## B5. Reports & Governance ✅

| Check | Status | Detail |
|---|---|---|
| `html` + `ctrf` | ✅ | |
| `enforce: true` | ✅ | |
| Thresholds | ✅ | `minCoveragePercentage: 68`, `maxMissedOperationsInSpec: 10` |

## B6. Files, Manifest, CI ✅

| Check | Status | Detail |
|---|---|---|
| Required files | ✅ | All present including mvnw, .mvn/wrapper |
| Manifest | ✅ | Complete |
| Dockerfile from source | ✅ | Multi-stage `maven:3.9.12-eclipse-temurin-17` |
| CI matrix | ✅ | `ubuntu-latest, macos-latest, windows-latest` |
| Architecture GIF | ✅ | Present |
| Docker build verified | ✅ | |

## Backend-specific ✅

| Check | Status | Detail |
|---|---|---|
| `SpecmaticResponseCodeFilter` | ✅ | Present |
| Seed data (InMemoryStore) | ✅ | Present |
| `Authenticate` header | ✅ | Implied by 293 passing |
| Error handler override | ✅ | `ApiExceptionHandler.java` |
| Inventory boundary | ✅ | `InventoryService.java` |

## Skipped

None — all checks passed including Docker.
