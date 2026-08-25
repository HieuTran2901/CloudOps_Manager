# CloudOps Manager — Enterprise Operations & Live Observability Architecture

## 1. Executive Summary

Phase 24 establishes the production operations, live health observability, and runtime diagnostic framework for CloudOps Manager. The platform provides real-time component health checks, structured liveness/readiness probes, typed AWS connectivity state modeling, evidence freshness metrics, ephemeral in-memory operational telemetry, and dedicated monitoring dashboards.

---

## 2. Health & Diagnostic Architecture

```
+----------------------------------------------------------------------------------------------------+
|                                  ENTERPRISE OPERATIONS SUBSYSTEM                                    |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ Health & Probes ]                 [ AWS Operational State ]       [ In-Memory Event Telemetry ]  |
|  - GET /api/v1/health                - CONNECTED                     - Ephemeral Ring Buffer (50)   |
|  - GET /api/v1/health/live           - AWS_ACCESS_DENIED             - Subsystem event streaming    |
|  - GET /api/v1/health/ready          - AWS_THROTTLED                 - Sanitized error logs         |
|  - Structured component matrix       - AWS_TIMEOUT                   - Zero credentials leaked      |
|  (App, AWS, Discovery, Topology,     - AWS_UNAVAILABLE                                              |
|   Security, Forensics, Metrics)      - Evidence Freshness Tracking                                  |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

---

## 3. Subsystem Health & Readiness Probes

### 1. Component Health (`GET /api/v1/health`)
Returns the structured status of all internal subsystems:
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "cloudops-manager",
    "version": "1.0.0",
    "release": "release-2026.08-p24",
    "components": {
      "application": "UP",
      "aws": "UP",
      "discovery": "UP",
      "topology": "UP",
      "security": "UP",
      "forensics": "UP",
      "observability": "UP"
    },
    "timestamp": "2026-08-24T15:30:00Z"
  },
  "message": "Service is healthy."
}
```

### 2. Liveness Probe (`GET /api/v1/health/live`)
Returns a lightweight `{"status": "UP"}` response indicating the JVM and web server are operational.

### 3. Readiness Probe (`GET /api/v1/health/ready`)
Returns readiness status indicating whether the application is prepared to handle ingress operations.

---

## 4. AWS Operational State & Evidence Freshness

The operational status engine (`GET /api/v1/operations/status`) categorizes connectivity without leaking credentials or stack traces:

| Status Code | Semantics | Degradation Handling |
|---|---|---|
| `CONNECTED` | STS caller identity verified; permissions active | Normal operations |
| `AWS_ACCESS_DENIED` | AWS credentials present but lack required IAM actions | Graceful partial degradation; sanitized banner |
| `AWS_THROTTLED` | AWS API rate limits exceeded | Backoff and retry with cached/partial evidence |
| `AWS_TIMEOUT` | Regional AWS API endpoint unreachable / high latency | Gateway timeout notification; no crash |
| `AWS_UNAVAILABLE` | Network or AWS service outage | Degraded indicator; safe fallback |
| `PARTIAL_EVIDENCE` | Some resource types unavailable | Partial metrics rendering; explicit badges |

### Evidence Freshness:
- `lastSuccessfulSync`: Exact UTC timestamp of latest confirmed observation (null if never observed).
- `lastAttemptedSync`: Timestamp of most recent connectivity attempt.
- `evidenceAgeSeconds`: Age of evidence in seconds.

---

## 5. Ephemeral Operational Event Stream

The system maintains an in-memory, thread-safe ring buffer ([`OperationalEventBuffer.java`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/operations/service/OperationalEventBuffer.java)) capped at 50 events.
- **Zero Database Persistence**: All events reside in memory during container lifecycle.
- **Sanitized Logging**: All event messages and details are scrubbed of Authorization headers, tokens, and access keys.

---

## 6. Production Monitoring Experience (Frontend)

The frontend features a dedicated **Operations & Health** view ([`OperationsPage.tsx`](file:///E:/Github%20project/CloudOps_Manager/frontend/src/pages/OperationsPage.tsx)) and an interactive System Health widget in the sidebar:
- Real-time Core Health and AWS Connectivity cards.
- Live Evidence Freshness timer.
- Subsystem matrix showing status of each analytical engine.
- Real-time operational event stream table with severity badges.
- Degradation alerts when operating with partial evidence.

---

## 7. Known Environment Limitations & Technical Debt

- **Docker Daemon Runtime**: In the current local environment, the Docker Desktop Linux Engine daemon is offline (`open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`). Static compose configuration (`docker compose config`) was fully validated. Recorded as `DOCKER_RUNTIME_UNAVAILABLE`.
- **Known IAM Blocker (BLK-001)**: IAM User `cloud-agent-antigravity` (Account `351405419700`, Region `ap-southeast-2`) has denied permission `ecr:DescribeRepositories`. The application correctly detects and handles this boundary via sanitized error handling without exposing credentials or stack traces.
- **Technical Debt (TD-002)**: Frontend test suite remains tracked for dedicated Cypress/Playwright integration in subsequent release operations.