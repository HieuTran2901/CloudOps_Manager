# CloudOps Manager — Deployment Rollback Procedure

## 1. Overview
CloudOps Manager is a stateless, ephemeral, read-only application. Because there is zero database persistence and zero AWS infrastructure mutation, rollbacks can be executed instantaneously without data migration or state recovery procedures.

---

## 2. Standard Container Rollback Procedure

To roll back from version `v1.0.0` (`release-2026.08-p29`) to a previous release tag:

```bash
# 1. Gracefully stop current running containers
docker compose down

# 2. Check out target release tag
git checkout release-2026.08-p28

# 3. Rebuild and launch the previous container version
docker compose up -d --build

# 4. Verify deployment health
wget --spider http://localhost:8080/api/v1/health
wget --spider http://localhost:3000/healthz

# 5. Run operational smoke test
powershell -ExecutionPolicy Bypass -File scripts/production-smoke-test.ps1
```

---

## 3. Unhealthy Container Remediation

1. **Backend Health Check Failure**:
   - Verify `AWS_REGION` and `AWS_ROLE_ARN` in `.env`.
   - Inspect container logs: `docker logs cloudops-backend`.
2. **Frontend Reverse Proxy Failure**:
   - Ensure backend container is in `healthy` status before frontend starts (`depends_on.backend.condition: service_healthy`).
   - Check Nginx error log: `docker logs cloudops-frontend`.