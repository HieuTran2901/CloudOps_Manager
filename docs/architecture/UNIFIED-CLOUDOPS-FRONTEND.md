# CloudOps Manager — Unified Frontend Web Dashboard Architecture

## 1. Overview & Principles

The CloudOps Manager frontend web dashboard is a modern, responsive single-page application built on React 18, TypeScript, Vite, and Tailwind CSS. It connects strictly to the read-only CloudOps Manager REST API backend to present live cloud evidence across discovery, telemetry, costs, audit, compliance, drift, topology, security blast radius, and forensic exports.

```text
+-----------------------------------------------------------------------------------+
|                           Web Browser (React + TypeScript)                        |
|                                                                                   |
|  AppLayout                                                                        |
|  ├── Header (Account, Region Selector, Status)                                    |
|  ├── Sidebar (Overview, Discovery, Telemetry, Costs, Audit, Compliance, Drift,   |
|  │            Topology, Blast Radius, Forensics)                                  |
|  └── Active Page Container (ErrorBoundary, LoadingSpinner, ErrorBanner, Cards)    |
+-----------------------------------------------------------------------------------+
                                          |
                                          | Typed API Client (apiFetch)
                                          v
+-----------------------------------------------------------------------------------+
|                        CloudOps Manager Backend REST APIs                         |
|                                                                                   |
|  - /api/v1/aws/sts/caller-identity                                                |
|  - /api/v1/aws/resources                                                          |
|  - /api/v1/aws/observability/metrics                                              |
|  - /api/v1/aws/costs                                                              |
|  - /api/v1/aws/audit/cloudtrail/events                                            |
|  - /api/v1/aws/compliance                                                         |
|  - /api/v1/aws/drift                                                              |
|  - /api/v1/aws/topology                                                           |
|  - /api/v1/aws/security/blast-radius                                              |
|  - /api/v1/aws/forensics/export                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 2. Component Hierarchy & Organization

- `src/app`: Application shell (`App.tsx`) and root setup (`main.tsx`).
- `src/config`: Environment configuration (`env.ts`).
- `src/types`: Strongly typed backend DTO and API response definitions (`api.ts`).
- `src/api`: Centralized API client (`apiClient.ts`) and domain endpoint mappings (`index.ts`).
- `src/components`:
  - `layout`: `Header`, `Sidebar`, `AppLayout`
  - `cards`: `StatCard`
  - `ui`: `StatusBadge`
  - `feedback`: `LoadingSpinner`, `ErrorBanner`, `EmptyState`, `ErrorBoundary`
- `src/pages`: `DashboardPage`, `ResourcesPage`, `ObservabilityPage`, `CostsPage`, `CloudTrailPage`, `CompliancePage`, `DriftPage`, `TopologyPage`, `SecurityPage`, `ForensicsPage`.

---

## 3. Security & Read-Only Guarantees

- Zero AWS SDK imports in browser code.
- Zero credential storage (no local storage, session storage, or committed secrets).
- Centralized error normalization displaying friendly error banners without exposing raw stack traces.