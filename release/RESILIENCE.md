# CloudOps Manager — Operational Resilience Specification

- **Release Tag**: `release-2026.08-p30`
- **Application Version**: `1.0.0`
- **Operational Score**: `RESILIENT_WITH_DEPLOYMENT_BOUNDARY`
- **Active Incidents**: `0` Unresolved Critical Failures
- **Evidence Lifecycle Policy**:
  - `FRESH`: Age $\le 300\text{s}$
  - `AGING`: Age $\le 900\text{s}$
  - `STALE`: Age $\le 3600\text{s}$
  - `EXPIRED`: Age $> 3600\text{s}$
- **Deployment Boundary**: `BLK-001` (`ecr:DescribeRepositories` denied for `cloud-agent-antigravity`)
- **Docker Daemon Runtime**: `DOCKER_RUNTIME_UNAVAILABLE` (Host daemon offline)