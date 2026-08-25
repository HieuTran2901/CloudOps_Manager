# CloudOps Manager — Deployment Evidence & Verification Record

## 1. Verified Live AWS Evidence
- **Caller Identity**: `arn:aws:iam::351405419700:user/cloud-agent-antigravity`
- **Account ID**: `351405419700`
- **Region**: `ap-southeast-2`
- **STS GetCallerIdentity**: `PASS`
- **EC2 DescribeInstances**: `PASS`
- **S3 ListBuckets**: `PASS`
- **RDS DescribeDBInstances**: `PASS`
- **IAM ListRoles / ListUsers**: `PASS`
- **CloudWatch GetMetricData**: `PASS`
- **CloudTrail LookupEvents**: `PASS`
- **Cost Explorer GetCostAndUsage**: `PASS`

## 2. Identified IAM Boundary
- **ECR DescribeRepositories**: `ACCESS_DENIED` (Blocked by `BLK-001`)
- **CloudOpsDeployerRole Status**: `IAM_REMEDIATION_PENDING`
- **Sanitized Preflight Status**: `BLOCKED`