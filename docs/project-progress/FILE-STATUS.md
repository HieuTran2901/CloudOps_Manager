# File Status

| File/Module | Status | Last Change | Tests | Notes |
|---|---|---|---|---|
| `.rules/` | VERIFIED | 2026-08-24 | N/A | Mandatory engineering governance rules (00 through 10) |
| `AGENTS.md` | VERIFIED | 2026-08-24 | N/A | Bootstrap entry point for AI coding agents |
| `docker-compose.yml` | VERIFIED | 2026-08-24 | PASS | Local multi-container orchestration definition |
| `.env.example` | VERIFIED | 2026-08-24 | N/A | Root environment configuration example |
| `docs/architecture/ARCHITECTURE.md` | VERIFIED | 2026-08-24 | N/A | System architecture & STS layer data flow |
| `docs/architecture/AWS-DISCOVERY-ARCHITECTURE.md` | VERIFIED | 2026-08-24 | N/A | AWS resource discovery architecture & model |
| `docs/architecture/EC2-DEEP-INSPECTION.md` | VERIFIED | 2026-08-24 | N/A | EC2 deep inspection, EBS, ENI architecture |
| `docs/architecture/OBSERVABILITY-ARCHITECTURE.md` | VERIFIED | 2026-08-24 | N/A | CloudWatch metrics query foundation architecture |
| `docs/architecture/CLOUDWATCH-METRIC-AGGREGATION.md` | VERIFIED | 2026-08-24 | N/A | CloudWatch multi-resource telemetry aggregation architecture |
| `docs/architecture/COST-EXPLORER-FINANCIAL-OBSERVABILITY.md` | VERIFIED | 2026-08-24 | N/A | Cost Explorer financial observability architecture |
| `docs/architecture/CLOUDTRAIL-AUDIT-EVENT-HISTORY.md` | VERIFIED | 2026-08-24 | N/A | CloudTrail audit & operational event history architecture |
| `docs/architecture/COMPLIANCE-RULE-EVALUATION-ENGINE.md` | VERIFIED | 2026-08-24 | N/A | Compliance rules evaluation engine architecture |
| `docs/architecture/COMPLIANCE-EVIDENCE-CORRELATION.md` | VERIFIED | 2026-08-24 | N/A | Compliance evidence correlation architecture |
| `docs/architecture/TERRAFORM-DRIFT-DETECTION.md` | VERIFIED | 2026-08-24 | N/A | Terraform IaC drift detection architecture |
| `docs/architecture/INFRASTRUCTURE-TOPOLOGY-GRAPH.md` | VERIFIED | 2026-08-24 | N/A | Infrastructure topology graph architecture |
| `docs/architecture/SECURITY-BLAST-RADIUS-LATERAL-MOVEMENT.md` | VERIFIED | 2026-08-24 | N/A | Security blast radius & lateral movement architecture |
| `docs/architecture/FORENSIC-AUDIT-EVIDENCE-EXPORT.md` | VERIFIED | 2026-08-24 | N/A | Forensic audit & evidence export architecture |
| `docs/architecture/UNIFIED-CLOUDOPS-FRONTEND.md` | VERIFIED | 2026-08-24 | N/A | Unified frontend web dashboard architecture |
| `docs/architecture/PRODUCTION-PACKAGING-AND-CONTAINERIZATION.md` | VERIFIED | 2026-08-24 | N/A | Multi-stage container packaging & Docker Compose architecture |
| `docs/architecture/FRONTEND-CLOUDOPS-INTELLIGENCE-UX.md` | VERIFIED | 2026-08-24 | N/A | Frontend production UX & intelligence visualization architecture |
| `docs/architecture/S3-DEEP-INSPECTION.md` | VERIFIED | 2026-08-24 | N/A | S3 deep inspection, encryption, PAB, CORS, lifecycle |
| `docs/architecture/RDS-DEEP-INSPECTION.md` | VERIFIED | 2026-08-24 | N/A | RDS deep inspection, network, storage, CloudWatch metrics |
| `docs/architecture/VPC-NETWORK-TOPOLOGY-DEEP-INSPECTION.md` | VERIFIED | 2026-08-24 | N/A | VPC network topology, subnets, routes, gateways, NACLs |
| `docs/architecture/SECURITY-GROUP-DEEP-INSPECTION.md` | VERIFIED | 2026-08-24 | N/A | Security Group deep inspection, rules, attachments |
| `docs/architecture/IAM-DEEP-INSPECTION.md` | VERIFIED | 2026-08-24 | N/A | IAM deep inspection, users, roles, policies, MFA, keys |
| `docs/architecture/MULTI-ACCOUNT-AWS-DISCOVERY.md` | VERIFIED | 2026-08-24 | N/A | Multi-account STS AssumeRole discovery architecture |
| `docs/architecture/TECHNOLOGY-STACK.md` | VERIFIED | 2026-08-24 | N/A | Comprehensive technology stack specification |
| `docs/security/AWS-SECURITY-MODEL.md` | VERIFIED | 2026-08-24 | N/A | Security model & error sanitization policy |
| `docs/project-progress/ROADMAP.md` | VERIFIED | 2026-08-24 | N/A | High-level roadmap covering Phases 0 to 21 |
| `docs/project-progress/CURRENT-TASK.md` | VERIFIED | 2026-08-24 | N/A | Active task state and tracking |
| `docs/project-progress/DECISIONS.md` | VERIFIED | 2026-08-24 | N/A | Architecture Decision Records (ADR-001 through ADR-004) |
| `docs/project-progress/BLOCKERS.md` | VERIFIED | 2026-08-24 | N/A | Environment audit & IAM permission notes |
| `docs/project-progress/COMPLETED.md` | VERIFIED | 2026-08-24 | N/A | Chronological log of completed milestones |
| `docs/project-progress/TECH-DEBT.md` | VERIFIED | 2026-08-24 | N/A | Technical debt ledger (TD-001 resolved, TD-002 open) |
| `README.md` | VERIFIED | 2026-08-24 | N/A | Project overview, goals, architecture, and status |
| `.gitignore` | VERIFIED | 2026-08-24 | N/A | Comprehensive ignore rules including secrets protection |
| `backend/pom.xml` | VERIFIED | 2026-08-24 | PASS | Spring Boot 3.3.3 + AWS SDK v2 modules |
| `backend/Dockerfile` | VERIFIED | 2026-08-24 | PASS | Multi-stage Temurin 21 JRE container build |
| `backend/src/main/java/...` | VERIFIED | 2026-08-24 | PASS | 199 Java source files (7,085 LOC) |
| `backend/src/test/java/...` | VERIFIED | 2026-08-24 | PASS | 119 unit tests passing (0 failures, 0 errors) |
| `frontend/Dockerfile` | VERIFIED | 2026-08-24 | PASS | Multi-stage Node 20 -> Nginx alpine container build |
| `frontend/nginx.conf` | VERIFIED | 2026-08-24 | PASS | Nginx SPA fallback + API proxy + caching config |
| `frontend/src/...` | VERIFIED | 2026-08-24 | PASS | React 18 + TS + Tailwind CSS dashboard (~1,650 LOC) |
| `infrastructure/terraform/` | PLANNED | 2026-08-24 | N/A | Terraform IaC modules (Planned Phase 22+) |