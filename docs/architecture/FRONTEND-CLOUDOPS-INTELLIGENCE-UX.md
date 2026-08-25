# CloudOps Manager — Frontend Production UX & CloudOps Intelligence Visualization Architecture

## 1. Overview & Principles

Phase 21 elevates the CloudOps Manager frontend into an interactive, deterministic, responsive visual control plane for all 18 completed backend capabilities.

```text
+-----------------------------------------------------------------------------------+
|                        Web Browser (React 18 + TypeScript)                        |
|                                                                                   |
|  [AppLayout]                                                                      |
|  ├── Header (Region / Account Context, Live System Status)                        |
|  ├── Sidebar (Unified Navigation: Overview, Discovery, Telemetry, Costs, Audit,   |
|  │            Compliance, Drift, Topology, Security, Forensics)                   |
|  └── Page View Container                                                          |
|      ├── [TopologyPage]: Interactive Directed SVG Graph, Zoom/Pan, Node Details  |
|      ├── [SecurityPage]: Blast Radius Calculator, Reachability Path Finder        |
|      ├── [ResourcesPage]: Filterable Inventory Table + ResourceDetailDrawer       |
|      ├── [ObservabilityPage]: Responsive SVG Telemetry Line Charts                |
|      ├── [CostsPage]: Exact BigDecimal Financial Breakdown & Progress Bars        |
|      ├── [CompliancePage]: Pillar Filter Tabs & Detailed Rule Explanations        |
|      ├── [DriftPage]: Terraform JSON Parser & Attribute Difference Explorer       |
|      ├── [CloudTrailPage]: Live Operational Audit Event Search                    |
|      └── [ForensicsPage]: Cryptographic SHA-256 Bundle & CSV Exports              |
+-----------------------------------------------------------------------------------+
                                          |
                                          | Typed Client (`cloudOpsApi`)
                                          v
+-----------------------------------------------------------------------------------+
|                        CloudOps Manager Backend REST APIs                         |
+-----------------------------------------------------------------------------------+
```

---

## 2. Interactive Visualization Components

### A. Topology Graph (`TopologyGraphView.tsx`)
- **Technology**: Lightweight deterministic SVG rendering with zoom, pan, and fit controls.
- **Layout**: Layered 2D placement based on AWS hierarchy (`VPC` top $\rightarrow$ `Subnets` $\rightarrow$ `EC2`/`RDS` $\rightarrow$ `Security Groups` $\rightarrow$ `IAM Roles`).
- **Features**: Color-coded node circles, directed relationship arrows, node selection drawer, and path highlighting.

### B. Resource Detail Drawer (`ResourceDetailDrawer.tsx`)
- Slide-over panel presenting normalized facts: Resource ID, Type, Account, Region, Status badge, Tags, and ARNs without raw unformatted dumps.

### C. Telemetry Chart (`TelemetryChart.tsx`)
- Clean SVG-based responsive line chart rendering CloudWatch metrics (`CPUUtilization`, `NetworkIn`, `NetworkOut`, etc.) with grid lines, maximum markers, and units.

### D. Security Intelligence (`BlastRadiusPanel.tsx` & `ReachabilityPanel.tsx`)
- **Blast Radius**: Configurable depth (1–5 hops) computing traversed nodes and edges.
- **Reachability**: Shortest pathfinder highlighting the exact traversal path.

---

## 3. Governance & Invariants

- **Zero AWS SDK in Frontend**: Pure decoupled web architecture communicating solely via typed backend REST endpoints.
- **Zero Credentials**: No AWS access keys or secrets in source code, localStorage, or environment files.
- **Strict Evidence Integrity**: All numbers and metrics are sourced directly from backend API responses without subjective risk scoring.