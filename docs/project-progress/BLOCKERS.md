# Blockers

## Active Blockers
*None blocking backend development or unit tests.*

---

## AWS IAM Environment Permissions (Live Integration Note)

- **ID**: BLK-001
- **Date**: 2026-08-23
- **Area**: AWS Live Integration Testing
- **Problem**: The configured AWS IAM caller (`arn:aws:iam::351405419700:user/cloud-agent-antigravity`) is authorized for `sts:GetCallerIdentity` but currently lacks read-only IAM policies for `s3:ListAllMyBuckets`, `ec2:DescribeInstances`, `ec2:DescribeVpcs`, and `rds:DescribeDBInstances`.
- **Evidence**:
  - Live STS call: PASS (`Account: 351405419700`)
  - Live S3 call: `401 Unauthorized` with `AWS_AUTH_FAILED` (`s3:ListAllMyBuckets access denied`)
  - Live EC2 call: `UnauthorizedOperation` (`ec2:DescribeVpcs access denied`)
- **Impact**: Standard development and unit tests are 100% functional and passing using mocked SDK providers. Live end-to-end multi-service discovery on this specific AWS account requires attaching a read-only policy (e.g., `ReadOnlyAccess` or custom read policy) to the IAM user.
- **Next Action**: Attach a read-only IAM policy to `arn:aws:iam::351405419700:user/cloud-agent-antigravity` if live cloud discovery is desired during developer testing.