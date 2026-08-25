# CloudOps Manager — Enterprise Multi-Account Federation & Security Hardening Architecture

## 1. Executive Summary

Phase 25 delivers the production-grade multi-account federation, cross-account role assumption, account/region security isolation, API hardening, and final release sign-off foundation for CloudOps Manager (v1.0.0, release `release-2026.08-p25`). The platform maintains strict read-only operation, zero database persistence, zero credential exposure, and absolute cross-account data isolation.

---

## 2. Multi-Account Role Federation Architecture

```
+----------------------------------------------------------------------------------------------------+
|                               MULTI-ACCOUNT FEDERATION PIPELINE                                     |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ Browser Client / UI ]                                                                           |
|         │                                                                                          |
|         ▼ POST /api/v1/aws/federation/assume-role (Target Account ID, Role ARN, Region)            |
|  +────────────────────────────────────────────────────────────+                                    |
|  | Federation Controller & Validation Layer                   |                                    |
|  | - Strict 12-Digit Account ID Regex Validation              |                                    |
|  | - Strict IAM Role ARN Regex Validation                      |                                    |
|  | - Target Account vs Role ARN Account Matching Check        |                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|         │                                                                                          |
|         ▼ Validated AssumeRoleRequest                                                              |
|  +────────────────────────────────────────────────────────────+                                    |
|  | STS Identity Provider & AssumeRole Engine                  |                                    |
|  | - Ephemeral STS Session Generation                         |                                    |
|  | - Caller Identity Post-Assumption Account Verification    |                                    |
|  | - Temporary Credentials Retained Strictly on Backend       |                                    |
|  | - Sanitized FederationResult (Zero Credentials Leaked)     |                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|         │                                                                                          |
|         ▼ Scoped Account Context (In-Memory, Thread-Safe)                                          |
|  +────────────────────────────────────────────────────────────+                                    |
|  | Read-Only Analytical Subsystems (Discovery, Topology, etc) |                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

---

## 3. Account & Region Security Isolation Invariants

1. **Explicit Scoping**:
   - Every request, service execution, graph node, security finding, and forensic export bundle carries explicit `accountId` and `region`.
2. **Cross-Account Collision Prevention**:
   - Resources with identical IDs across distinct accounts (e.g. `i-123456` in Account A and `i-123456` in Account B) are strictly partitioned.
3. **Graph & Reachability Boundary**:
   - Topology graphs and BFS blast-radius reachability algorithms never cross account or region boundaries.
4. **Forensic Integrity**:
   - Export bundles contain account-specific JSON/CSV records sealed with a deterministic SHA-256 digest.

---

## 4. API & Frontend Security Hardening

- **Zero Credential Exposure**:
  - Temporary AWS session tokens and access keys never leave the backend JVM memory.
  - REST endpoints only return sanitized metadata (`targetAccountId`, `assumedRoleArn`, `status`, `federatedAt`).
- **Sanitized Error Boundaries**:
  - Infrastructure exceptions (`AwsAccessDeniedException`, `AwsThrottlingException`, `AwsTimeoutException`) are mapped to typed statuses (`ACCESS_DENIED`, `AWS_THROTTLED`, `AWS_TIMEOUT`, `INVALID_ROLE`, `ACCOUNT_MISMATCH`).
  - No stack traces or raw SDK messages are exposed to clients.
- **Frontend Isolation**:
  - 0 AWS SDK imports.
  - 0 raw `fetch()` invocations outside centralized typed `apiClient.ts`.
  - Account and region contexts are explicitly displayed in the UI header and operations dashboard.

---

## 5. Release Artifacts & Checksums

The `release/` directory contains complete, reproducible release assets:
- `release/VERSION`: `1.0.0`
- `release/RELEASE-NOTES.md`: Comprehensive capability matrix and verification audit.
- `release/DEPLOYMENT.md`: Hardened non-root deployment guide and rollback procedure.
- `release/SMOKE-TEST.md`: Non-destructive read-only deployment validation steps.
- `release/CHECKSUMS.md`: SHA-256 reference digests.

---

## 6. Known Environment Limitations & Technical Debt

- **Docker Daemon Runtime**: In the current local environment, the Docker Desktop Linux Engine daemon is offline (`open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`). Static compose configuration (`docker compose config`) was fully validated. Recorded as `DOCKER_RUNTIME_UNAVAILABLE`.
- **Known IAM Blocker (BLK-001)**: IAM User `cloud-agent-antigravity` (Account `351405419700`, Region `ap-southeast-2`) has denied permission `ecr:DescribeRepositories`. The application correctly detects and sanitizes this boundary via `AWS_ACCESS_DENIED` without exposing credentials or stack traces.
- **Technical Debt (TD-002)**: Frontend test suite remains tracked for dedicated Cypress/Playwright integration in subsequent release operations.