# CloudOps Manager — Release Smoke Test Guide

## Purpose
The smoke test suite performs non-destructive, strictly read-only validation of a running CloudOps Manager deployment.

## Execution
Run the smoke test script:

### Windows (PowerShell)
```powershell
.\scripts\smoke-test.ps1 -BackendUrl "http://localhost:8080" -FrontendUrl "http://localhost:3000"
```

### Linux / macOS (Bash)
```bash
./scripts/smoke-test.sh http://localhost:8080 http://localhost:3000
```

## Validation Criteria
1. **Backend Health**: `GET /api/v1/health` returns HTTP 200 with `status: "UP"`, `version: "1.0.0"`, and `release: "release-2026.08-p23"`.
2. **Frontend Routing**: `GET /healthz` returns HTTP 200 `OK`.
3. **Identity Verification**: `GET /api/v1/aws/sts/caller-identity` returns active identity or sanitized 403.
4. **Discovery Contract**: `GET /api/v1/aws/resources` returns valid inventory structure.
5. **Topology Engine**: `GET /api/v1/aws/topology` returns valid node and edge lists.
6. **Security Module**: `GET /api/v1/aws/security/exposures` returns exposure evaluations.