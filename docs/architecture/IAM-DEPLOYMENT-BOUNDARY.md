# CloudOps Manager — IAM Deployment & Runtime Boundaries

## 1. Segregation of Duties

| Role | Principal | Scope | Policies |
|---|---|---|---|
| **A. Application Runtime Role** | ECS Task Role | Read-Only AWS Analytics | `ReadOnlyAccess`, `SecurityAudit` |
| **B. Deployment Principal** | CI/CD Deployer | ECR Push & ECS Service Update | `ecr:PutImage`, `ecs:UpdateService` |
| **C. ECS Task Execution Role** | `ecs-tasks.amazonaws.com` | ECR Pull & CloudWatch Logs | `AmazonECSTaskExecutionRolePolicy` |
| **D. Multi-Account AssumeRole** | Target Accounts | Ephemeral STS Assumption | `sts:AssumeRole` with external ID |

---

## 2. Identified Blocker: BLK-001

- **Principal**: `arn:aws:iam::351405419700:user/cloud-agent-antigravity`
- **Blocked Permission**: `ecr:DescribeRepositories`
- **Handling**: Sanitized into `ACCESS_DENIED`, rendering `deploymentReady: false` and `overallStatus: BLOCKED` without failing analytical operations.