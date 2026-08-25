# CloudOps Manager — Multi-Account AWS Discovery & STS AssumeRole Architecture

## 1. Overview & Principles

The Multi-Account AWS Discovery subsystem orchestrates cross-account resource discovery using AWS Security Token Service (STS) `AssumeRole`. It allows CloudOps Manager to discover resources across member accounts without database credential storage, without permanent credentials, and without creating duplicate provider hierarchies.

```text
+---------------------------------------------------------------------------------+
|                                 REST API Client                                 |
+---------------------------------------------------------------------------------+
                                         |
                                         | GET /api/v1/aws/resources/accounts/{accountId}
                                         |  ?roleArn=arn:aws:iam::{accountId}:role/...
                                         v
+---------------------------------------------------------------------------------+
|                         AwsResourceDiscoveryController                          |
+---------------------------------------------------------------------------------+
                                         |
                                         v
+---------------------------------------------------------------------------------+
|                          AwsResourceDiscoveryService                            |
|                                                                                 |
|  1. Validate AwsAccountTarget (12-digit ID, ARN-to-Account binding)             |
|  2. AssumeRole via AwsIdentityService / StsIdentityProvider                     |
|  3. Verify caller identity via Scoped STS Client (Account Mismatch Protection)  |
|  4. Instantiate ephemeral account-scoped AWS SDK clients via AwsClientFactory   |
|  5. Reuse existing discovery providers (AwsEc2Provider, AwsS3Provider, etc.)    |
|  6. Aggregate normalized InventorySummary and close clients                     |
+---------------------------------------------------------------------------------+
           |                                                      |
           v                                                      v
+-----------------------+                              +-----------------------+
|   AwsClientFactory    |                              |   AwsIdentityService  |
| (Static Credentials)  |                              |    (AssumeRole API)   |
+-----------------------+                              +-----------------------+
```

---

## 2. Security Invariants & Isolation

1. **Zero Credential Persistence**: Assumed temporary session credentials exist exclusively in memory within ephemeral AWS SDK clients during discovery execution.
2. **Deterministic Account Binding**: The target `roleArn` is validated to contain the target `accountId`. Additionally, after assuming the role, `sts:GetCallerIdentity` is executed against the scoped client to ensure the verified AWS account strictly matches the requested account ID.
3. **Transient Resource Isolation**: Account A clients and sessions cannot cross-contaminate Account B discovery. Ephemeral clients are closed immediately after discovery aggregation.
4. **Zero AWS Mutation**: No roles, policies, or infrastructure are created or modified during cross-account discovery.

---

## 3. Supported Multi-Account Discovery Resources

Cross-account discovery reuses existing core providers:
- **EC2 Instances** (`AwsEc2Provider`)
- **S3 Buckets** (`AwsS3Provider`)
- **RDS Databases** (`AwsRdsProvider`)
- **VPC Networks** (`AwsVpcProvider`)
- **Security Groups** (`AwsSecurityGroupProvider`)