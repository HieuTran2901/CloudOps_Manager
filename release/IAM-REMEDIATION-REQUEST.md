# CloudOps Manager — Production IAM Remediation Request

## 1. Context & Identified Boundary
- **AWS Account ID**: `351405419700`
- **Primary Region**: `ap-southeast-2`
- **Current Runtime Principal**: `arn:aws:iam::351405419700:user/cloud-agent-antigravity`
- **Current Denied Action**: `ecr:DescribeRepositories` (`BLK-001`)
- **Impact**: CloudOps Manager read-only analytical operations are 100% functional. However, production container deployment and ECR image publication are blocked.

---

## 2. Segregation of Duties Architecture

| Identity / Role | Scope & Permissions | Separation Principle |
|---|---|---|
| **A. Application Runtime Role** | Read-Only AWS Inspection (`ReadOnlyAccess`, `SecurityAudit`) | Strictly NO ECR push or ECS mutation permissions |
| **B. Deployment Principal** | Dedicated CI/CD identity (GitHub Actions / CodeBuild) | Least-privilege repository-scoped ECR publication only |
| **C. ECS Task Execution Role** | `ecs-tasks.amazonaws.com` | Image pull (`AmazonECSTaskExecutionRolePolicy`) and CloudWatch logging |
| **D. ECS Task Role** | Read-Only AWS Inspection | Least-privilege read-only access for CloudOps backend runtime |
| **E. Multi-Account Federation Role** | Target AWS Accounts | STS AssumeRole with 12-digit account validation |

---

## 3. Dedicated ECR Deployment Principal Policy

Attach the following least-privilege IAM policy to the dedicated **Deployment Principal** (e.g. CI/CD role `CloudOpsDeployerRole`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EcrAuthToken",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    },
    {
      "Sid": "EcrRepositoryScopedPublish",
      "Effect": "Allow",
      "Action": [
        "ecr:DescribeRepositories",
        "ecr:DescribeImages",
        "ecr:BatchCheckLayerAvailability",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:PutImage"
      ],
      "Resource": [
        "arn:aws:ecr:ap-southeast-2:351405419700:repository/cloudops-manager-backend",
        "arn:aws:ecr:ap-southeast-2:351405419700:repository/cloudops-manager-frontend"
      ]
    }
  ]
}
```

### Why Each Permission Is Required:
1. `ecr:GetAuthorizationToken`: Enables Docker CLI authentication against ECR registry endpoint `351405419700.dkr.ecr.ap-southeast-2.amazonaws.com`.
2. `ecr:DescribeRepositories`: Verifies existence and URI of target repositories before upload.
3. `ecr:DescribeImages`: Checks for existing image tags/digests to prevent accidental tag overwrites.
4. `ecr:BatchCheckLayerAvailability`: Identifies which container layers already exist in ECR to optimize upload speed.
5. `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`: Multipart chunked image layer upload capability.
6. `ecr:PutImage`: Finalizes image manifest registration with immutable tags (`1.0.0`, `release-2026.08-p35`).

### Permissions Explicitly NOT Granted:
- `ecr:*` (Wildcard administrative access denied)
- `ecr:DeleteRepository`, `ecr:DeleteRepositoryPolicy` (Destructive actions denied)
- `ecr:BatchDeleteImage` (Image deletion denied)
- `AdministratorAccess` / `PowerUserAccess` (Denied)

---

## 4. Post-Remediation Verification Commands

```bash
# 1. Verify caller identity
aws sts get-caller-identity --region ap-southeast-2

# 2. Verify ECR repository description
aws ecr describe-repositories \
  --repository-names cloudops-manager-backend cloudops-manager-frontend \
  --region ap-southeast-2

# 3. Authenticate Docker client to ECR
aws ecr get-login-password --region ap-southeast-2 | docker login --username AWS --password-stdin 351405419700.dkr.ecr.ap-southeast-2.amazonaws.com
```

---

## 5. Rollback Procedure
If the deployment principal policy needs to be revoked:
```bash
aws iam delete-role-policy --role-name CloudOpsDeployerRole --policy-name CloudOpsEcrPublishPolicy
```