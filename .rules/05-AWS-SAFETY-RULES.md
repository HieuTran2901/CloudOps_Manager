# AWS Operational Safety Rules

## Production Safety

Production AWS resources must be treated as high-risk.

Dangerous operations require:
- explicit intent;
- authorization;
- confirmation;
- audit logging.

For production deletion, prefer an approval workflow.

## Read-Only First

New AWS modules should normally be implemented in this order:

```text
Discover
  ↓
Read
  ↓
Audit
  ↓
Simulate
  ↓
Mutate
```

Do not begin with destructive operations.

## AWS API Calls

AWS SDK calls must:
- use configured region/account context;
- handle pagination;
- handle throttling where appropriate;
- handle transient failures;
- avoid unbounded retries.

## Retry

Retries must use bounded exponential backoff where appropriate.

Never retry destructive operations blindly.

## Pagination

Any AWS API that supports pagination must be evaluated for pagination handling.

Never assume the first response contains all resources.

## Multi-Region

Do not silently scan only one region when the feature claims to scan an account.

The active region set must be explicit.

## Account Identity

Before performing sensitive operations, verify the AWS identity/account context.

Never assume the configured account is the actual caller identity.

## Dry Run

Where practical, support dry-run or preview behavior before mutation.

Example:

```text
PLAN:
- stop i-123
- modify sg-456
- delete volume vol-789
```

Only execute after confirmation/approval.

## AWS Cost

Never create expensive AWS resources automatically without an explicit user action and clear warning.

Examples:
- NAT Gateway
- large EC2 instances
- large RDS instances
- high-volume logging
- expensive data transfer
