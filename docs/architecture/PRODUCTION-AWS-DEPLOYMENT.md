# CloudOps Manager — Production AWS Deployment Architecture

## 1. Overview & Deployment Philosophy
CloudOps Manager is an enterprise-grade, read-only Cloud Operations and Security platform designed for zero-mutation operation and ephemeral in-memory analytics.

This document defines the AWS production deployment target (AWS App Runner / ECS Fargate) without executing automated infrastructure provisioning or mutating AWS resources.

---

## 2. Production Deployment Topology (AWS App Runner / ECS Fargate)

```
+----------------------------------------------------------------------------------------------------+
|                                  AWS PRODUCTION DEPLOYMENT TARGET                                   |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ End User / Web Browser ]                                                                        |
|         │                                                                                          |
|         ▼ HTTPS (Port 443 / TLS 1.3)                                                               |
|  +────────────────────────────────────────────────────────────+                                    |
|  | AWS App Runner / ALB (Ingress Controller)                  |                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|         │                                                                                          |
|         ▼ HTTP (Port 3000)                                                                         |
|  +────────────────────────────────────────────────────────────+                                    |
|  | Frontend Service (Nginx Alpine + React 18 SPA)             |                                    |
|  | - Security Headers: X-Frame-Options, CSP, nosniff          |                                    |
|  | - SPA Route Fallback (/index.html)                         |                                    |
|  | - Health Probe: /healthz                                   |                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|         │                                                                                          |
|         ▼ /api/* (Internal Reverse Proxy Routing to Backend)                                      |
|  +────────────────────────────────────────────────────────────+                                    |
|  | Backend Service (Eclipse Temurin 21 JRE Non-Root)          |                                    |
|  | - Spring Boot REST API (/api/v1/*)                         |                                    |
|  | - Preflight & Capabilities: /api/v1/aws/preflight          |                                    |
|  | - Health Probes: /api/v1/health/live, /ready               |                                    |
|  | - IAM Execution Role (Attached Read-Only Analytical Policy)|                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|         │                                                                                          |
|         ▼ Strictly Read-Only AWS SDK Calls (Default Credential Chain)                              |
|  +────────────────────────────────────────────────────────────+                                    |
|  | Target AWS Account Infrastructure                          |                                    |
|  | (EC2, S3, RDS, IAM, CloudWatch, CloudTrail, Cost Explorer) |                                    |
|  +────────────────────────────────────────────────────────────+                                    |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

---

## 3. Production Service Boundaries & Ports

| Service | Container Image | Port | Health Check | Runtime User |
|---|---|---|---|---|
| **Frontend** | `cloudops-frontend:1.0.0` | `3000` (mapped to `80`) | `GET /healthz` | `nginx` |
| **Backend** | `cloudops-backend:1.0.0` | `8080` | `GET /api/v1/health` | `cloudops:cloudops` (UID 10001) |

---

## 4. IAM Runtime Role Requirements

To operate against target AWS accounts, the backend container requires an attached IAM execution/task role containing only read-only policies:
- `SecurityAudit` (AWS Managed Policy)
- `ViewOnlyAccess` (AWS Managed Policy)
- Custom policy for CloudWatch/Cost Explorer read APIs:
  - `cloudwatch:GetMetricData`, `cloudwatch:ListMetrics`
  - `ce:GetCostAndUsage`
  - `cloudtrail:LookupEvents`
  - `sts:GetCallerIdentity`
  - `sts:AssumeRole` (restricted to explicit cross-account audit role ARNs)

---

## 5. Security & Isolation Guarantees

1. **Zero Client Credentials**: The frontend container never receives AWS credentials or access tokens.
2. **Zero Database**: No persistence layer is provisioned. All graph analytics, blast-radius computations, and forensic aggregations operate ephemerally in memory.
3. **Stateless Rollback**: Rollbacks are executed by updating the container image tag without database schema rollback steps.