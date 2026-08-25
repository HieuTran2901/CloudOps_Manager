# CloudOps Manager — Live Runtime Acceptance Framework

## 1. Acceptance Checklist

1. [x] Health Probes (`/api/v1/health`, `/healthz`)
2. [x] STS Identity Verification (`/api/v1/sts/caller-identity`)
3. [x] Read-Only Resource Discovery (`/api/v1/aws/resources`)
4. [x] Topology Graph Generation (`/api/v1/aws/topology`)
5. [x] Blast Radius & Reachability (`/api/v1/aws/security/...`)
6. [x] Compliance & Forensics (`/api/v1/aws/compliance`, `/forensics`)
7. [x] Deterministic SHA-256 Digest Verification
8. [ ] ECR Repository Publishing (BLOCKED by `BLK-001`)
9. [ ] ECS Fargate Live Task Execution (BLOCKED by `BLK-001`)