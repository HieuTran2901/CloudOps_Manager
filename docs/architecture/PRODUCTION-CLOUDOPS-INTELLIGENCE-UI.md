# CloudOps Manager — Phase 21 Architecture Document
## Production CloudOps Intelligence UI & Interactive Visualization

### 1. Executive Overview
Phase 21 establishes the production intelligence and interactive visualization layer for CloudOps Manager.
The UI layer remains strictly read-only, non-mutating, zero-persistence, and isolated from browser-side AWS SDK imports.

### 2. Architecture & Design Principles
- **Read-Only Intelligence**: Pure consumption of normalized evidence exposed by backend endpoints (Phases 1-20).
- **Centralized Typed API Client**: All HTTP requests flow strictly through `src/api/client/apiClient.ts` and `src/api/index.ts`. No raw `fetch()` calls in components.
- **Zero AWS SDK in Frontend**: Eliminates credential exposure and ensures enterprise air-gapped security.
- **Evidence-First Representation**: Displays `INSUFFICIENT_EVIDENCE`, `AWS_ACCESS_DENIED`, or explicit loading/empty states rather than fabricating metrics, security scores, or topology edges.
- **Modular Component Governance**: Strict adherence to file size budgets ($\le 120-150$ LOC where practical) and separation of 3D projection data from presentation widgets.

### 3. Key Milestone Capabilities
- **Milestone 21A (Interactive Topology Explorer)**:
  - Directed graph rendering using deterministic layout and layered coordinates.
  - Interactive 3D Perspective Projection with orbit drag rotation, depth sorting, and zoom controls.
  - Node inspection drawer with neighbor relationship list, edge direction, and live attribute display.
- **Milestone 21B (Resource Inspection Experience)**:
  - Multi-attribute search, type filtering, region isolation, status badge classification, and detailed resource drawer.
- **Milestone 21C (Security & Blast Radius Experience)**:
  - Observed public exposure detection (Administrative Port Ingress, Public IPs).
  - Configurable depth Blast Radius analysis with reachable node traversal.
  - Shortest-path reachability engine between source and target nodes.
  - Verified Lateral Movement propagation path visualization.
- **Milestone 21D (Telemetry / Compliance / Cost Visualization)**:
  - Multi-resource CloudWatch metric aggregation charts.
  - Compliance donut breakdown by pillar (Security, Reliability, Cost, Performance).
  - Financial trends and CloudTrail operational event audit log inspection.

### 4. Verification & Governance Summary
- **Frontend Build**: `npm run build` PASS (0 errors).
- **Backend Tests**: `mvnw clean test` PASS (119/119 tests passing, 0 failures).
- **Secrets Audit**: 0 credentials or private keys detected.
- **AWS SDK Boundary**: 0 `@aws-sdk` imports in frontend.
- **Mutations**: 0 mutating HTTP methods in frontend UI.