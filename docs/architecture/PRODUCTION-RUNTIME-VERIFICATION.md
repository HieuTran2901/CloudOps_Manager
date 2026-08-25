# CloudOps Manager — Production Runtime Verification Architecture

## 1. Overview
This document details the live verification methodology distinguishing real AWS observations from synthetic test fixtures across all CloudOps analytical engines.

---

## 2. Live vs Synthetic Evidence Flow

```
+----------------------------------------------------------------------------------------------------+
|                                    EVIDENCE PIPELINE ISOLATION                                     |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ Production Live Mode ]                                                                          |
|  AWS Account ──► AWS SDK Provider Chain ──► Discovery Engine ──► Topology/Security ──► Dashboard   |
|                                                                                                    |
|  [ Test Fixture Mode (Tests Only) ]                                                                |
|  SyntheticEvidenceFixtures ──► Mock Provider ──► Verification Test Suite                           |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

---

## 3. Subsystem Runtime Verification Matrix

| Subsystem | Live Mode Endpoint | Verification Guarantee |
|---|---|---|
| **STS Identity** | `GET /api/v1/aws/sts/caller-identity` | Validates active account ID, user ARN, and credentials |
| **Preflight** | `GET /api/v1/aws/preflight` | Audits IAM capabilities and reports BLK-001 boundary |
| **Discovery** | `GET /api/v1/aws/resources` | Discovers live EC2, S3, RDS, IAM, VPC, Subnets |
| **Topology** | `GET /api/v1/aws/topology` | Builds deterministic 2D/3D graph from observed resources |
| **Security** | `GET /api/v1/aws/security/exposures` | Computes public subnet and security group exposures |
| **Blast Radius** | `GET /api/v1/aws/security/blast-radius/{id}` | BFS traversal bounded to active account context |
| **Compliance** | `GET /api/v1/aws/compliance` | Evaluates AWS Well-Architected Framework rules |
| **Observability**| `GET /api/v1/aws/observability/metrics` | Queries live CloudWatch time series telemetry |
| **Audit** | `GET /api/v1/aws/audit/cloudtrail/events`| Inspects live CloudTrail management events |
| **Forensics** | `GET /api/v1/aws/forensics/export?format=json` | Generates immutable bundle with SHA-256 digest |

---

## 4. Error Sanitization & Resilience
When live AWS calls encounter permission limits, rate limits, or timeouts:
- Exceptions are caught and translated into structured `ApiResponse` with `AWS_ACCESS_DENIED`, `AWS_THROTTLED`, or `AWS_TIMEOUT`.
- Zero credentials, tokens, or JVM stack traces are returned to clients.