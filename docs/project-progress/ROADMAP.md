# CloudOps Manager — Project Roadmap

Legend:
- `[ ]` Planned
- `[~]` In Progress
- `[x]` Completed
- `[!]` Blocked

---

## Phases

### [x] Phase 0 — Repository & Architecture Foundation
- **Goal**: Initialize repository, establish engineering rules, document architecture and security models, and set up project tracking.
- **Acceptance Criteria**:
  - [x] Engineering rules established in `.rules/`
  - [x] `AGENTS.md` entry point configured
  - [x] Project progress tracking initialized in `docs/project-progress/`
  - [x] Initial directory structure created
  - [x] Architecture & technology stack documented
  - [x] AWS security model documented
  - [x] Git initialized with comprehensive `.gitignore`

### [x] Phase 1 — AWS Account / STS
- **Goal**: Implement secure AWS credential management, STS AssumeRole integration, and caller identity verification.
- **Acceptance Criteria**:
  - Secure credential resolution via STS AssumeRole
  - Caller identity verification (`sts:GetCallerIdentity`)
  - Region & account context configuration

### [x] Phase 2 — AWS Resource Discovery
- **Goal**: Multi-region discovery engine for inventorying core AWS resources.
- **Acceptance Criteria**:
  - Unified resource discovery provider interfaces
  - Pagination and rate-limiting handling
  - Resource projection caching/persistence in MySQL

### [x] Phase 3 — EC2 Management
- **Goal**: EC2 instance inventory, status tracking, metadata aggregation, and safe operational actions.
- **Acceptance Criteria**:
  - EC2 instance listing across active regions
  - Detailed instance metadata (type, state, IPs, launch time, security groups)
  - Dry-run supported state actions (start/stop/reboot) with audit logging

### [x] Phase 4 — S3 Management
- **Goal**: S3 bucket inventory, security configuration inspection, and policy audits.
- **Acceptance Criteria**:
  - S3 bucket enumeration
  - Public access block / policy security checks
  - Encryption and versioning status checks

### [x] Phase 5 — RDS Management
- **Goal**: Relational Database Service (RDS) instance and cluster discovery, health, and configuration tracking.
- **Acceptance Criteria**:
  - DB instance/cluster listing across active regions
  - Storage, engine version, backup retention, and multi-AZ audit
  - Public accessibility risk detection

### [x] Phase 6 — VPC & Security Groups
- **Goal**: Network topology discovery, subnet mapping, and security group rule auditing.
- **Acceptance Criteria**:
  - VPC, subnet, route table, internet gateway mapping
  - Security group rule inspection and overly permissive rule detection (e.g., 0.0.0.0/0 on SSH/RDP)

### [x] Phase 7 — IAM Security Audit
- **Goal**: IAM user, role, policy, and credential report security evaluation.
- **Acceptance Criteria**:
  - Stale access key detection
  - Overly permissive / wildcard policy detection (`*.*`)
  - Root account usage and MFA status auditing

### [x] Phase 8 — CloudWatch Monitoring
- **Goal**: Metric collection, alarm status aggregation, and operational dashboards.
- **Acceptance Criteria**:
  - CloudWatch alarm aggregation across regions
  - Metric queries (CPU, memory/disk where available, network, I/O)
  - Unified operational telemetry view

### [x] Phase 9 — Infrastructure Health Score
- **Goal**: Algorithmic scoring of infrastructure across security, reliability, cost, and performance.
- **Acceptance Criteria**:
  - Weighted health score calculation per category and composite score
  - Breakdown of score deductions with remediation advice

### [x] Phase 10 — Cost Analysis & Optimization
- **Goal**: AWS Cost Explorer integration, idle resource identification, and right-sizing recommendations.
- **Acceptance Criteria**:
  - Historical and forecasted cost reporting
  - Idle/unattached resource detection (unattached EBS volumes, idle Elastic IPs)
  - Cost reduction actionable insights

### [x] Phase 11 — Audit Log
- **Goal**: Immutable audit trail for all read and write operations conducted via CloudOps Manager.
- **Acceptance Criteria**:
  - Comprehensive audit event capture (user, action, target resource, timestamp, result)
  - Structured storage and queryable audit log UI

### [x] Phase 12 — Automation
- **Goal**: Scheduled and trigger-based routine operational tasks.
- **Acceptance Criteria**:
  - Scheduled inventory sync and health checks
  - Policy enforcement routines and alert dispatching (via SQS/email/webhook)

### [x] Phase 13 — Terraform Integration
- **Goal**: Infrastructure as Code (IaC) workspace integration and state inspection.
- **Acceptance Criteria**:
  - Terraform plan generation and validation
  - Managed vs unmanaged resource correlation

### [x] Phase 14 — Drift Detection
- **Goal**: Automated comparison between declared Terraform state and actual AWS resource configurations.
- **Acceptance Criteria**:
  - Resource property diff computation
  - Drift alerts and remediation recommendations

### [x] Phase 15 — Multi-Account AWS
- **Goal**: AWS Organizations support and cross-account AssumeRole delegation.
- **Acceptance Criteria**:
  - Account inventory synchronization from AWS Organizations
  - Cross-account role assumption and multi-tenant resource views

### [x] Phase 16 — Approval Workflow
- **Goal**: Maker-checker approval pipeline for high-risk and destructive cloud actions.
- **Acceptance Criteria**:
  - Action request submission, review, and approval lifecycle
  - Multi-approver policies for production mutations

### [x] Phase 17 — Incident Center
- **Goal**: Centralized incident triage, alert correlation, and root-cause analysis workflow.
- **Acceptance Criteria**:
  - Alert aggregation into incidents
  - Timeline generation and operational runbook links

### [x] Phase 18 — AI Cloud Advisor
- **Goal**: LLM-assisted architecture optimization, anomaly explanation, and remediation script synthesis.
- **Acceptance Criteria**:
  - Contextual infrastructure analysis via AI
  - Safe, dry-runnable remediation suggestions

### [x] Phase 19 — Production Deployment
- **Goal**: Production packaging, containerization, CI/CD pipeline, and ECS deployment.
- **Acceptance Criteria**:
  - Multi-stage Docker builds for frontend & backend
  - GitHub Actions CI/CD workflows
  - Terraform ECS/ECR infrastructure deployment

### [x] Phase 20 — Final Security / Reliability Audit
- **Goal**: End-to-end security penetration testing, load testing, disaster recovery validation, and final readiness review.
- **Acceptance Criteria**:
  - Security audit and vulnerability assessment
  - High availability and failover testing
  - Final compliance and operational sign-off
