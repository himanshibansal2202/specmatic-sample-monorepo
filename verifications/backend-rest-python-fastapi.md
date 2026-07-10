# Verification: backend-rest-python-fastapi

**Date:** 2026-07-09
**Verified by:** Kiro CLI (automated checklist)
**Result:** ✅ PASS

---

## B1. Enterprise Runtime ✅

| Check | Status | Detail |
|---|---|---|
| Enterprise artifact | ✅ | `io.specmatic.enterprise:executable-all:1.19.1` |
| No forbidden refs | ✅ | No `npx specmatic`, `npm exec`, etc. |
| Artifact confirmed | ✅ | JAR path: `tools/specmatic-enterprise-executable-all-1.19.1.jar` |
| CLI mode direct | ✅ | `java -jar <jar> run-suite --config specmatic.yaml` |
| Pinned version | ✅ | `1.19.1` |

## B2. Config Schema (v3) ✅

| Check | Status | Detail |
|---|---|---|
| Single specmatic.yaml at root | ✅ | |
| v3 structure | ✅ | `version: 3`, `systemUnderTest`, `components`, `$ref` wiring |
| Settings under `specmatic:` | ✅ | `specmatic.settings.test.schemaResiliencyTests` |

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
| Discovery | N/A | No `swaggerUrl` — FastAPI has no native Specmatic discovery. Matches Specmatic's own Python sample behavior. |
| Filter | ✅ | `PATH!='/health,/openapi.json,/docs,/redoc'` — correct for FastAPI infra endpoints |
| Coverage measured | ✅ | 68% from report |

## B5. Reports & Governance ✅

| Check | Status | Detail |
|---|---|---|
| `html` + `ctrf` | ✅ | |
| Report path | ✅ | `build/reports/specmatic` |
| `enforce: true` | ✅ | |
| Thresholds from measured | ✅ | `minCoveragePercentage: 68`, `maxMissedOperationsInSpec: 10` |
| No custom renderer | ✅ | |

## B6. Files, Manifest, CI ✅

| Check | Status | Detail |
|---|---|---|
| Required files | ✅ | specmatic.yaml, requirements.txt, src/, test/, Dockerfile, CI, README, manifest, .gitignore, .dockerignore |
| Manifest populated | ✅ | Runtime, testCoverage, learnings all present |
| Dockerfile from source | ✅ | `COPY requirements.txt` → `pip install` → `COPY src` |
| CI matrix | ✅ | `ubuntu-latest, macos-latest, windows-latest` (cli mode) |
| Architecture GIF | ✅ | `assets/specmatic-order-backend-architecture.gif` |
| No orphaned files | ✅ | |
| Pinned versions | ✅ | All deps in requirements.txt have exact versions |

## Backend-specific ✅

| Check | Status | Detail |
|---|---|---|
| Security header `Authenticate` | ✅ | Accepted via `Header(alias="Authenticate")` |
| PATCH/DELETE return `text/plain` | ✅ | `PlainTextResponse("success")` |
| PUT image multipart | ✅ | `UploadFile = File(...)` |
| Seed data products {10, 20} | ✅ | |
| Seed data orders {10, 20} | ✅ | |
| POST IDs start ≥1000 | ✅ | `next_product_id = 1000` |
| `Specmatic-Response-Code` handled | ✅ | `requested_response_code()` branches on it |
| Error body contract-only fields | ✅ | `{timestamp, status, error, message}` only |

## Skipped

| Check | Reason |
|---|---|
| Docker build | Docker daemon unavailable locally |
