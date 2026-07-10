# Verification: backend-rest-typescript-express (test-container mode)

**Date:** 2026-07-09
**Verified by:** Kiro CLI (automated checklist)
**Result:** ✅ PASS

---

## B1. Enterprise Runtime ✅

| Check | Status | Detail |
|---|---|---|
| Enterprise artifact | ✅ | `specmatic/enterprise:1.19.1` Docker image |
| No forbidden refs | ✅ | Clean |
| test-container mode | ✅ | `testcontainers` npm package manages Docker lifecycle |
| Pinned version | ✅ | `1.19.1` |
| No local Java | ✅ | Runs inside container |

## B2. Config Schema (v3) ✅

| Check | Status | Detail |
|---|---|---|
| Single specmatic.yaml | ✅ | |
| v3 structure | ✅ | `version: 3`, `$ref` wiring |
| Settings under `specmatic:` | ✅ | Template values for env-override |

## B3. Progressive Resiliency ✅

| Level | Tests | Passed | Increasing? |
|---|---|---|---|
| `none` | 31 | 31 | — |
| `positiveOnly` | 74 | 74 | ✅ |
| `all` | 293 | 293 | ✅ |

Shipped: `all` ✅
Test runner validates progressive increases as built-in assertion.

## B4. Coverage & Discovery ✅

| Check | Status | Detail |
|---|---|---|
| Discovery | N/A | Express — no native discovery (correct) |
| Filter | N/A | No infra endpoints exposed |
| Coverage | ✅ | 68% measured and enforced |

## B5. Reports & Governance ✅

| Check | Status | Detail |
|---|---|---|
| `html` + `ctrf` | ✅ | |
| `enforce: true` | ✅ | |
| Thresholds | ✅ | `minCoveragePercentage: 68`, `maxMissedOperationsInSpec: 0` |

## B6. Files, Manifest, CI ✅

| Check | Status | Detail |
|---|---|---|
| Required files | ✅ | All present including lockfile |
| Manifest | ✅ | Runtime (docker-hub, testcontainers), coverage, learnings |
| Dockerfile from source | ✅ | Multi-stage build |
| CI ubuntu-only | ✅ | Correct for test-container mode |
| Architecture GIF | ✅ | Present |
| Pinned deps | ✅ | All exact versions |
| Docker build verified | ✅ | Docker was available |

## Backend-specific ✅

| Check | Status | Detail |
|---|---|---|
| All 11 operations | ✅ | 293 tests passing at `all` |
| Seed data | ✅ | store.ts present |
| `Specmatic-Response-Code` | ✅ | Implied by 293 passing tests |

## Skipped

None — all checks passed including Docker.
