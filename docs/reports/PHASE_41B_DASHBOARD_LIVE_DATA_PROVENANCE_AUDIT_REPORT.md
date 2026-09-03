# PHASE 41B — DASHBOARD LIVE DATA PROVENANCE & MOCK DATA FORENSIC AUDIT REPORT

- **Phase Identifier**: `PHASE_41B`
- **Execution Date**: `2026-08-26`
- **AWS Target Account**: `351405419700`
- **Primary AWS Region**: `ap-southeast-2`
- **IAM Principal**: `arn:aws:iam::351405419700:user/cloud-agent-antigravity`
- **Phase Audit Status**: **`PHASE_41B_STATUS = STATIC_DATA_DETECTED`**

---

### 1. Executive Summary

Phase 41B performed a strict read-only forensic audit to establish the exact data provenance of every value rendered on the CloudOps Manager `DashboardPage` and its child components.

#### Critical Finding
While backend REST APIs (`/api/v1/aws/resources`, `/api/v1/aws/topology`, `/api/v1/aws/compliance/evaluate`, `/api/v1/aws/costs`, `/api/v1/aws/audit/events`) and specialized domain sub-pages (`ResourcesPage`, `TopologyPage`, `CompliancePage`, `CostsPage`, `CloudTrailPage`) are **100% live-data-backed**, the root `DashboardPage.tsx` component is currently composed of presentational card components (`TopMetricCards`, `ComplianceOverviewCard`, `ResourceDistributionCard`, `RecentAlertsCard`, `BottomTrendCards`) that **receive no API props from `DashboardPage.tsx`**, causing them to render static default fallback values (`8,742`, `156`, `92%`, `$12,845`, `"Test Account"`, `"us-east-1"`).

```
+-----------------------------------------------------------------------------------+
|                        DASHBOARD DATA PROVENANCE FORENSIC AUDIT                   |
+-----------------------------------------------------------------------------------+
|                                                                                   |
| [ AWS Live Environment ] (Account 351405419700 / ap-southeast-2)                  |
|        │                                                                          |
|        ▼ (1 EC2, 1 VPC, 9 Security Groups, 14 Topology Nodes)                    |
| [ Spring Boot Backend REST APIs ] (/api/v1/aws/*)                                 |
|        │                                                                          |
|        ├───────────────► Specialized Domain Pages (100% LIVE_API)                  |
|        │                 (ResourcesPage, TopologyPage, CompliancePage, CostsPage)   |
|        │                                                                          |
|        X (NOT CONNECTED IN DASHBOARDPAGE.TSX)                                     |
|        │                                                                          |
|        ▼                                                                          |
| [ DashboardPage.tsx ] ──> Unconnected Presentational Cards                        |
|                           ├─ TopMetricCards.tsx (8,742, 156, 92%, $12,845)        |
|                           ├─ ComplianceOverviewCard.tsx (144/8/4/0)              |
|                           ├─ ResourceDistributionCard.tsx (9,740 resources)      |
|                           ├─ RecentAlertsCard.tsx (4 static alerts)              |
|                           ├─ BottomTrendCards.tsx ($12,845 / 23 findings)        |
|                           └─ Header.tsx ("Test Account" / "us-east-1")            |
+-----------------------------------------------------------------------------------+
```

---

### 2. Dashboard Value Reconciliation Table

| Dashboard Metric | Displayed Value | Expected Live Source | Actual Rendered Source | Live AWS API Evidence | Classification |
|---|---|---|---|---|---|
| **Discovered Resources** | `8,742` | `GET /api/v1/aws/resources` (`data.Count`) | Default param in `TopMetricCards.tsx:12` | Live AWS has 11 resources (1 EC2, 1 VPC, 9 SGs) | **`STATIC_FALLBACK`** |
| **Compliance Rules** | `156` | `GET /api/v1/aws/compliance/evaluate` | Default param in `TopMetricCards.tsx:13` | Live API evaluates 6 rules | **`STATIC_FALLBACK`** |
| **Compliance Passing %** | `92%` | `GET /api/v1/aws/compliance/evaluate` | Default param in `TopMetricCards.tsx:14` | Live API evaluates 0% pass rate (2 FAIL, 2 N/A, 2 Insufficient) | **`STATIC_FALLBACK`** |
| **Security Score** | `85 / 100` | `GET /api/v1/aws/security/blast-radius` | Hardcoded in JSX (`TopMetricCards.tsx:90`) | N/A | **`HARDCODED`** |
| **Est. Monthly Cost** | `$12,845` | `GET /api/v1/aws/costs` | Default param in `TopMetricCards.tsx:15` | Live Cost Explorer API returns `-0.0000000384 USD` | **`STATIC_FALLBACK`** |
| **Compliance Breakdown** | `144 Passed (92%)` `8 Warning (5%)` `4 Failed (3%)` | `GET /api/v1/aws/compliance/evaluate` | Hardcoded SVG + text in `ComplianceOverviewCard.tsx:58-94` | Live API returns 0 Pass, 2 Fail, 2 N/A, 2 Insufficient | **`HARDCODED`** |
| **Resource Distribution** | `9,740 resources` (EC2: 780, S3: 1240, RDS: 1850, Lambda: 1420, VPC: 960, IAM: 1100, DynamoDB: 890, Others: 1500) | `GET /api/v1/aws/resources` (Grouped) | Hardcoded `services` array in `ResourceDistributionCard.tsx:7-16` | Live AWS has 1 EC2, 1 VPC, 9 SGs, 0 S3, 0 RDS | **`HARDCODED`** |
| **Recent Alerts** | `4 Alerts` (S3 Public, IAM Password, EC2 i-0a1b2c3, SG modified) | `GET /api/v1/aws/audit/events` / Compliance | Hardcoded `alerts` array in `RecentAlertsCard.tsx:9-46` | Real alerts: 5 Security Groups open to 0.0.0.0/0 | **`HARDCODED`** |
| **Topology Status** | `Operational` | `GET /api/v1/aws/topology` | Hardcoded text in `TopMetricCards.tsx:71` | Live topology has 14 nodes, 5 edges | **`HARDCODED`** |
| **Bottom Cost Trend** | `$12,845 (8.2%)` | `GET /api/v1/aws/costs` | Hardcoded text in `BottomTrendCards.tsx:31` | Live Cost Explorer API returns `-0.0000000384 USD` | **`HARDCODED`** |
| **Bottom Security Findings**| `23 (15%)` | `GET /api/v1/aws/security/blast-radius` | Hardcoded text in `BottomTrendCards.tsx:70` | Live API returns 5 open SG findings | **`HARDCODED`** |
| **Bottom Top Risks** | `S3 Bucket Public Read (High) - 3 Resources` | `GET /api/v1/aws/compliance/evaluate` | Hardcoded text in `BottomTrendCards.tsx:112-118` | Live AWS has 0 S3 buckets | **`HARDCODED`** |
| **Account Identity** | `"Test Account"` | `GET /api/v1/health` / STS | Default prop `accountId = 'Test Account'` in `Header.tsx:14` | Live STS Account ID is `351405419700` | **`HARDCODED`** |
| **Region Selector** | `"us-east-1"` | `AWS_REGION` | `APP_CONFIG.defaultRegion` in `env.ts:3` | Live AWS Region is `ap-southeast-2` (Missing from dropdown) | **`HARDCODED`** |
| **Last Sync** | `"34s ago"` | API Timestamp | Hardcoded text in `LiveConnectionBanner.tsx:54` | Live API timestamp | **`HARDCODED`** |
| **Data Source** | `"AWS Evidence Service"` | Backend Service metadata | Hardcoded text in `LiveConnectionBanner.tsx:46` | Backend REST API | **`HARDCODED`** |

---

### 3. Account and Region Context Discrepancy Audit

1. **Account Context**:
   - `Header.tsx:14` hardcodes default prop `accountId = 'Test Account'`.
   - `AppLayout.tsx` does not fetch account identity from `sts:GetCallerIdentity` or pass the live account ID `351405419700` to `Header.tsx`.
2. **Region Context**:
   - `env.ts:3` specifies `defaultRegion: 'us-east-1'`.
   - `Header.tsx:16` hardcodes the region dropdown options as `['us-east-1', 'us-west-2', 'eu-west-1', 'ap-southeast-1']`.
   - **`ap-southeast-2` (the active live AWS region for account `351405419700`) is absent from the dropdown list**.
   - Changing the region selector in `Header.tsx` does **not** trigger a new backend API fetch because `DashboardPage.tsx` does not consume `currentRegion` state.

---

### 4. Health Endpoint Audit (`GET /api/v1/aws/health`)

- **Payload Response**:
  ```json
  {
    "success": true,
    "data": {
      "status": "UP",
      "components": {
        "aws": "UP", "application": "UP", "topology": "UP",
        "discovery": "UP", "observability": "UP", "security": "UP", "forensics": "UP"
      }
    }
  }
  ```
- **Audit Conclusion**: `GET /api/v1/aws/health` verifies that the Spring Boot backend process is running and components are initialized. **It DOES NOT prove that metrics rendered on `DashboardPage.tsx` are live data**.

---

### 5. Root Cause & Architectural Discrepancy

- **Why did this happen?**
  - During initial UI development, presentational card components (`TopMetricCards`, `ComplianceOverviewCard`, `ResourceDistributionCard`, `RecentAlertsCard`, `BottomTrendCards`, `Header`) were built with static mockup values as visual placeholders.
  - In Phase 40B & 40C, backend API endpoints and domain pages (`ResourcesPage`, `TopologyPage`, `CompliancePage`, `CostsPage`) were wired to live AWS APIs.
  - However, `DashboardPage.tsx` was never updated to fetch API data via `apiFetch` and pass props down to `TopMetricCards`, `ComplianceOverviewCard`, `ResourceDistributionCard`, etc.
- **Current State**:
  - `DashboardPage.tsx`: **`0% LIVE DATA`** (Renders 100% static/fallback values).
  - Domain Sub-Pages (`ResourcesPage`, `TopologyPage`, `CompliancePage`, `CostsPage`, `CloudTrailPage`): **`100% LIVE DATA`**.

---

### 6. Mandatory Status Classification

```
PHASE_41B_STATUS = STATIC_DATA_DETECTED

HEALTH_ENDPOINT = VERIFIED
RESOURCE_API = VERIFIED
TOPOLOGY_API = VERIFIED
COMPLIANCE_API = VERIFIED
COST_API = VERIFIED
OBSERVABILITY_API = VERIFIED

DASHBOARD_LIVE_DATA_PERCENTAGE = 0%

MOCK_DATA = 0
STATIC_DATA = 12
FALLBACK_DATA = 5
UNKNOWN_DATA = 0

ACCOUNT_CONTEXT = HARDCODED ("Test Account")
REGION_CONTEXT = HARDCODED ("us-east-1", ap-southeast-2 missing from UI dropdown)

SOURCE_CODE_MODIFIED = 0
AWS_MUTATIONS = 0
IAM_MUTATIONS = 0
DEPLOYMENT_EXECUTED = 0
GIT_PUSH_EXECUTED = 0
```

---

### 7. Recommended Next Phase (Phase 41C)

- **Phase 41C Goal**: Wire `DashboardPage.tsx` to live backend APIs (`/api/v1/aws/resources`, `/api/v1/aws/compliance/evaluate`, `/api/v1/aws/costs`, `/api/v1/aws/topology`, `/api/v1/health`) so that the Dashboard renders real AWS metrics from account `351405419700` (`ap-southeast-2`).
- **Header Fix**: Update `Header.tsx` to include `ap-southeast-2` in region dropdown and display live STS account ID `351405419700`.
