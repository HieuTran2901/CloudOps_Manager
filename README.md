# CloudOps Manager

CloudOps Manager is an enterprise cloud operations, inventory, and security posture management platform designed for multi-account AWS environments.

## Project Goals

- **Comprehensive Inventory Discovery**: Multi-region, multi-account automated resource discovery for EC2, S3, RDS, IAM, VPC, and networking assets.
- **Security & Compliance Auditing**: Proactive detection of overly permissive security groups, stale credentials, public buckets, and unencrypted data.
- **Observability & Health Telemetry**: Integrated CloudWatch metrics, alarms aggregation, and health scoring.
- **Safe Cloud Operations**: Governed operational workflows leveraging AWS STS temporary role assumption, dry-run simulations, and maker-checker approval pipelines.
- **Cost & Drift Management**: Identification of idle/orphaned resources and Terraform drift detection.

## Technology Stack

- **Frontend**: React, TypeScript, Vite, Tailwind CSS, TanStack Query, Recharts
- **Backend**: Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Flyway, AWS SDK for Java 2.x
- **Data & Caching**: MySQL 8.x, Redis
- **Infrastructure & Deployment**: Terraform, Docker, GitHub Actions, AWS ECR, AWS ECS

For complete technical specifications, see [`docs/architecture/TECHNOLOGY-STACK.md`](docs/architecture/TECHNOLOGY-STACK.md).

## Architecture Overview

CloudOps Manager is implemented as a **modular monolith** to maintain high cohesion and transactional simplicity while avoiding unnecessary distributed microservices complexity.

```text
React (Vite UI)
       │
       ▼ (REST API)
Spring Boot 3 Modular Monolith
       │
       ├─► Persistence Layer ──► MySQL (Projections & Metadata)
       │
       └─► AWS Provider Layer ──► AWS STS / SDK ──► Target AWS Accounts
```

For detailed architectural diagrams and data flow, see [`docs/architecture/ARCHITECTURE.md`](docs/architecture/ARCHITECTURE.md).

## Current Project Status

- **Phase**: **Phase 0 — Repository & Architecture Foundation** (Complete)
- **Status**: Foundation, engineering rules, progress tracking, architecture models, and repository structure established.
- **Source of Truth for Progress**: See [`docs/project-progress/ROADMAP.md`](docs/project-progress/ROADMAP.md) and [`docs/project-progress/CURRENT-TASK.md`](docs/project-progress/CURRENT-TASK.md).

## Development Setup

> **Note**: Application features and build modules will be scaffolded in subsequent phases. Commands below reflect verified prerequisites and planned development workflows.

### Prerequisites Verified
- **Java**: Java 21 LTS installed.
- **Node.js / npm**: Node v25.x / npm v11.x installed.
- **Git**: Git 2.46+ initialized.
- **AWS CLI**: AWS CLI v2 installed.

### Planned Build Commands (Phases 1+)
- **Backend Build**: `./mvnw clean compile` (planned once backend module is scaffolded)
- **Frontend Dev Server**: `npm run dev` in `frontend/` (planned once frontend module is scaffolded)
- **Database Service**: MySQL 8.0 instance required for persistence
