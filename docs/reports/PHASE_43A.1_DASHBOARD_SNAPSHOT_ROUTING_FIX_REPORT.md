# PHASE 43A.1 — DASHBOARD SNAPSHOT API ROUTING CORRECTION & REGRESSION REPORT

- **Phase Identifier**: `PHASE_43A_1`
- **Execution Date**: `2026-08-27`
- **AWS Target Account**: `351405419700`
- **Primary Verified AWS Region**: `ap-southeast-2`
- **IAM Principal**: `arn:aws:iam::351405419700:user/cloud-agent-antigravity`
- **Phase Status**: **`PHASE_43A_1_STATUS = PASS`**

---

### 1. Executive Summary & Root Cause Analysis

Phase 43A.1 diagnosed and corrected a routing path composition defect in `frontend/src/api/index.ts`.

#### Root Cause Analysis
- **`APP_CONFIG.apiBaseUrl`**: Evaluates to `/api/v1/aws` (from `frontend/src/config/env.ts`).
- **ApiClient Path Composition**: `apiFetch(endpoint)` computes `${APP_CONFIG.apiBaseUrl}${endpoint}`.
- **Defective Path Call**: `getDashboardSnapshot` in `frontend/src/api/index.ts` passed `/aws/dashboard/snapshot`.
- **Composition Result**: `${'/api/v1/aws'}${'/aws/dashboard/snapshot'}` = **`/api/v1/aws/aws/dashboard/snapshot`** (duplicated `/aws/aws/` prefix).
- **Backend Response**: Spring threw `NoResourceFoundException: No static resource api/v1/aws/aws/dashboard/snapshot` because the endpoint was unmapped.

```
+-----------------------------------------------------------------------------------+
|              PHASE 43A.1 ROUTING PATH COMPOSITION CORRECTION                      |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  BEFORE (Duplicated /aws Prefix):                                                 |
|  apiBaseUrl (/api/v1/aws) + endpoint (/aws/dashboard/snapshot)                    |
|  ==> /api/v1/aws/aws/dashboard/snapshot (HTTP 500 / NoResourceFoundException)    |
|                                                                                   |
|  AFTER (Canonical Path):                                                          |
|  apiBaseUrl (/api/v1/aws) + endpoint (/dashboard/snapshot)                        |
|  ==> /api/v1/aws/dashboard/snapshot (HTTP 200 OK / Snapshot Received)             |
+-----------------------------------------------------------------------------------+
```

---

### 2. Mandatory Certification Matrix

```
PHASE_43A_1_STATUS = PASS

ROUTING_BUG = ELIMINATED
ROOT_CAUSE = IDENTIFIED (apiBaseUrl /api/v1/aws + endpoint /aws/dashboard/snapshot duplication)
CANONICAL_ENDPOINT = /api/v1/aws/dashboard/snapshot
DUPLICATED_ENDPOINT_ELIMINATED = VERIFIED (/api/v1/aws/aws/dashboard/snapshot removed)

BACKEND_CONTROLLER_MAPPING = VERIFIED (@RequestMapping("/api/v1/aws/dashboard"))
FRONTEND_API_COMPOSITION = VERIFIED (apiFetch("/dashboard/snapshot"))
DASHBOARD_SNAPSHOT_API = VERIFIED (200 OK)

REGRESSION_TEST = VERIFIED (testDashboardSnapshotRouteContract in ApiContractVerificationTest)
BACKEND_TESTS = PASS (186/186 PASS)
FRONTEND_BUILD = PASS (0 errors)

MOCK_DATA = 0
STATIC_BUSINESS_DATA = 0
DANGEROUS_FALLBACKS = 0

AWS_CREDENTIALS_IN_FRONTEND = 0
AWS_SDK_IN_FRONTEND = 0
AWS_MUTATIONS = 0
IAM_MUTATIONS = 0
DEPLOYMENT_EXECUTED = 0
GIT_PUSH_EXECUTED = 0

SOURCE_CODE_CHANGES = 2 (frontend/src/api/index.ts, ApiContractVerificationTest.java)
DEFECTS = 0
BLOCKERS = 0
NEXT_RECOMMENDED_PHASE = PHASE_43B
```

---

### 3. Applied Source Changes

#### 1. `frontend/src/api/index.ts`
```typescript
  // Operations & Health
  getDashboardSnapshot: (region?: string) =>
    apiFetch<DashboardSnapshot>(`/dashboard/snapshot${region ? `?region=${region}` : ''}`),
  refreshDashboardSnapshot: (region?: string) =>
    apiFetch<DashboardSnapshot>(`/dashboard/snapshot/refresh${region ? `?region=${region}` : ''}`, {
      method: 'POST',
    }),
```

#### 2. `ApiContractVerificationTest.java`
Added `testDashboardSnapshotRouteContract()` asserting:
- `GET /api/v1/aws/dashboard/snapshot` returns `200 OK`.
- `GET /api/v1/aws/aws/dashboard/snapshot` returns `500` error (unmapped static resource handled by `GlobalExceptionHandler`).

---

### 4. Forensic & Security Audit Results

- **Static Business Data Scan**: `STATIC_BUSINESS_DATA = 0`, `MOCK_DATA = 0`.
- **Frontend Credential Boundary**: `0` `@aws-sdk` imports in frontend, `0` AWS access keys in browser.
- **AWS Infrastructure Mutations**: `AWS_MUTATIONS = 0`, `IAM_MUTATIONS = 0`.
- **Deployment & Git Safety**: `DEPLOYMENT_EXECUTED = 0`, `GIT_PUSH_EXECUTED = 0`.

---

### 5. Regression & Build Verification Results

- **Backend Unit Test Suite**: **`186 / 186 PASS`** (`mvnw clean test`, 2m 25s).
- **Frontend Production Build**: **`PASS`** (`npm run build`, built in 13.65s, 0 errors).
