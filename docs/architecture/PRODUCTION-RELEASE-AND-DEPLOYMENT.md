# CloudOps Manager — Production Release & Deployment Architecture

## 1. Executive Summary

Phase 23 establishes the production release and deployment foundation for CloudOps Manager (v1.0.0, release `release-2026.08-p23`). The system is packaged as an enterprise-grade, read-only cloud intelligence platform designed for zero-mutation operation, zero database persistence, and hardened container deployment.

---

## 2. Release & Deployment Architecture

```
+----------------------------------------------------------------------------------------------------+
|                                    CLOUDOPS MANAGER ARCHITECTURE                                   |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ Browser Client ]                                                                                |
|         │                                                                                          |
|         ▼ (Port 3000)                                                                              |
|  +────────────────────────────────────────────────────────────+                                    |
|  | Frontend Container (Nginx Alpine + React 18 SPA)           |                                    |
|  | - Security Headers: X-Frame-Options, CSP, nosniff          |                                    |
|  | - Static Asset Caching (Immutable, gzip enabled)          |                                    |
|  | - Health Endpoint: /healthz                                |                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|         │                                                                                          |
|         ▼ /api/* (Reverse Proxy over cloudops-net internal bridge)                                 |
|  +────────────────────────────────────────────────────────────+                                    |
|  | Backend Container (Eclipse Temurin 21 JRE Non-Root)        |                                    |
|  | - Spring Boot 3 REST API (/api/v1/*)                       |                                    |
|  | - Health & Release Metadata: /api/v1/health                |                                    |
|  | - Ephemeral Memory Processing (Zero Database)              |                                    |
|  | - Provider Boundary Isolation (AWS SDK hidden)             |                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|         │                                                                                          |
|         ▼ Read-Only IAM Session (STS AssumeRole)                                                   |
|  +────────────────────────────────────────────────────────────+                                    |
|  | Target AWS Account(s)                                      |                                    |
|  | - EC2 / S3 / RDS / VPC / Security Groups                   |                                    |
|  | - CloudWatch / Cost Explorer / CloudTrail                  |                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

---

## 3. Configuration & Secrets Hierarchy

CloudOps Manager enforces strict configuration governance:

1. **Hierarchy**:
   - `application.yml` (Base defaults & version metadata)
   - `application-dev.yml` (Local development defaults)
   - `application-test.yml` (Isolated deterministic test environment)
   - `application-prod.yml` (Production-hardened logging and security policies)
2. **Environment Variable Bindings**:
   - `SERVER_PORT`: Backend HTTP port (default `8080`).
   - `AWS_REGION`: Target default AWS region (e.g. `us-east-1`).
   - `AWS_ROLE_ARN`: Target IAM role for cross-account operations.
   - `SPRING_PROFILES_ACTIVE`: Selected Spring profile (`prod`, `dev`, `test`).
   - `LOG_LEVEL`: Logging verbosity.
3. **Secret Security Model**:
   - Zero hardcoded credentials anywhere in the repository.
   - Standard AWS SDK default credential provider chain used at runtime.
   - All sample configuration files use explicit placeholders (`AWS_REGION=us-east-1`, `AWS_ROLE_ARN=`).

---

## 4. Release Versioning & Health Metadata

The `/api/v1/health` endpoint exposes structured, non-sensitive release metadata:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "cloudops-manager",
    "version": "1.0.0",
    "release": "release-2026.08-p23"
  },
  "message": "Service is healthy."
}
```

---

## 5. Deployment Smoke-Test & Rollback Strategy

1. **Smoke-Test Foundation**:
   - Implemented in [`scripts/smoke-test.ps1`](file:///E:/Github%20project/CloudOps_Manager/scripts/smoke-test.ps1) and [`scripts/smoke-test.sh`](file:///E:/Github%20project/CloudOps_Manager/scripts/smoke-test.sh).
   - Verifies frontend healthz, backend health, STS authentication, discovery contracts, topology graph generation, and error sanitization.
   - Handles expected AWS `AccessDenied` without application crashes.
2. **Rollback Strategy**:
   - Fully containerized stateless architecture allows zero-downtime rollback:
     ```bash
     docker compose down
     git checkout <PREVIOUS_RELEASE_TAG>
     docker compose up -d --build
     ```

---

## 6. Known Environment Limitations & Technical Debt

- **Docker Runtime Limitation**: Docker Desktop Linux engine daemon is currently offline in the host environment. Configuration and Dockerfiles are statically validated via `docker compose config`. Status: `DOCKER_RUNTIME_UNAVAILABLE`.
- **Known IAM Blocker (BLK-001)**: IAM User `cloud-agent-antigravity` (Account `351405419700`, Region `ap-southeast-2`) lacks `ecr:DescribeRepositories`. The application correctly detects and sanitizes this permission boundary.
- **Technical Debt (TD-002)**: Frontend test suite remains tracked for dedicated Cypress/Playwright integration in subsequent release operations.