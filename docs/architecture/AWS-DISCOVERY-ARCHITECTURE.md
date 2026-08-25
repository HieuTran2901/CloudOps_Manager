# CloudOps Manager — AWS Resource Discovery Architecture

## 1. Overview & Architectural Principles

The AWS Resource Discovery subsystem provides read-only discovery, normalization, and inventory aggregation across core AWS compute, storage, database, and networking services.

### Core Architectural Invariants:
1. **AWS as the Source of Truth**:
   - Live cloud infrastructure state is determined by real-time queries to AWS APIs via the AWS SDK for Java 2.x.
   - MySQL is not used for primary discovery state; persistence is reserved for historical snapshots and audit logs in future phases.
2. **Read-Only Safety**:
   - The discovery layer contains zero mutating operations (no create, delete, update, start, stop, or terminate calls).
3. **Strict Provider Isolation**:
   - REST Controllers and Domain Services never interact directly with AWS SDK client objects (`Ec2Client`, `S3Client`, `RdsClient`, `StsClient`).
   - All AWS communication is strictly encapsulated behind provider interfaces (`Ec2Provider`, `S3Provider`, `RdsProvider`, `VpcProvider`, `SecurityGroupProvider`).
4. **Normalized Domain Models**:
   - Raw AWS SDK response DTOs are mapped into immutable domain models implementing `CloudResource` before traversing architectural boundaries.

---

## 2. Layering & Data Flow

```text
+-------------------------------------------------------+
|                 REST API Clients / UI                 |
+-------------------------------------------------------+
                           |
                           | GET /api/v1/aws/resources/*
                           v
+-------------------------------------------------------+
|            AwsResourceDiscoveryController             |
|            (REST API Endpoint Presentation)           |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|             AwsResourceDiscoveryService               |
|            (Domain Aggregation & Region Flow)         |
+-------------------------------------------------------+
        |                  |                 |
        v                  v                 v
+---------------+  +---------------+  +---------------+
|  Ec2Provider  |  |  S3Provider   |  |  RdsProvider  | ... (Vpc, SG)
+---------------+  +---------------+  +---------------+
        |                  |                 |
        v                  v                 v
+---------------+  +---------------+  +---------------+
| AwsEc2Provider|  | AwsS3Provider |  | AwsRdsProvider| ... (AwsNetwork)
+---------------+  +---------------+  +---------------+
        |                  |                 |
        v                  v                 v
+---------------+  +---------------+  +---------------+
|   Ec2Client   |  |   S3Client    |  |   RdsClient   |
+---------------+  +---------------+  +---------------+
        |                  |                 |
        +------------------+-----------------+
                           |
                           v
                     AWS Cloud APIs
```

---

## 3. Normalized Resource Model

All discovered resources implement the common interface `CloudResource`:

```java
public interface CloudResource {
    String resourceId();
    CloudResourceType resourceType(); // EC2_INSTANCE, S3_BUCKET, RDS_INSTANCE, VPC, SECURITY_GROUP
    String name();
    String region();
    String accountId();
    String status();
    String arn();
    Map<String, String> tags();
    Instant discoveredAt();
}
```

### Specialized Resource DTOs:
- **`Ec2InstanceResource`**: Captures `instanceType`, `privateIp`, `publicIp`, `vpcId`, `subnetId`, `availabilityZone`, `amiId`, `launchTime`.
- **`S3BucketResource`**: Captures `bucketName`, `creationDate`, `arn`.
- **`RdsInstanceResource`**: Captures `engine`, `engineVersion`, `instanceClass`, `endpoint`, `port`, `availabilityZone`, `dbSubnetGroup`, `vpcId`, `publiclyAccessible`, `allocatedStorageGb`.
- **`VpcResource`**: Captures `cidrBlock`, `isDefault`, `dhcpOptionsId`.
- **`SecurityGroupResource`**: Captures `description`, `vpcId`, `List<IpPermissionRule> inboundRules`, `List<IpPermissionRule> outboundRules`.
- **`InventorySummary`**: High-level aggregation DTO containing `totalCount`, `countByType`, and list of `CloudResource` items.

---

## 4. Region Strategy & Account Context

1. **Account Context**:
   - The active caller account is dynamically established via `AwsIdentityService` (`sts:GetCallerIdentity`) before resource queries.
2. **Explicit Region Handling**:
   - Default region is configured via Spring property `cloudops.aws.region` (default: `us-east-1`).
   - Endpoints accept an optional `?region=...` query parameter to target specific AWS regions without silent multi-region scanning.

---

## 5. Pagination & Error Handling

1. **Pagination**:
   - AWS APIs with pagination support (`describeInstancesPaginator`, `describeDBInstancesPaginator`, `describeSecurityGroupsPaginator`) are processed to completion.
2. **Error Translation**:
   - AWS SDK exceptions (`Ec2Exception`, `S3Exception`, `RdsException`, `StsException`) are caught in provider implementations and translated into safe domain exceptions (`AwsAuthenticationException`, `AwsIdentityException`).
   - `GlobalExceptionHandler` ensures structured JSON error responses without raw stack trace or credential leakage.