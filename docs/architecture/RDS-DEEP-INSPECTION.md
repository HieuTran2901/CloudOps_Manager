# CloudOps Manager — RDS Deep Inspection & Observability Architecture

## 1. Overview & Principles

The RDS Deep Inspection subsystem provides unified, read-only configuration and telemetry analysis of Amazon Relational Database Service (RDS) instances. It extracts factual configuration attributes across database engines, high-availability placement, storage encryption, automated backups, network topology, enhanced monitoring, and CloudWatch performance metrics.

```text
+-------------------------------------------------------------+
|                     REST API Client                         |
+-------------------------------------------------------------+
              |                                     |
              | GET /resources/rds/{id}             | GET /resources/rds/{id}/metrics
              v                                     v
+------------------------------------+   +------------------------------------+
|   AwsResourceDiscoveryController   |   |   AwsResourceDiscoveryController   |
+------------------------------------+   +------------------------------------+
              |                                     |
              v                                     v
+------------------------------------+   +------------------------------------+
|    AwsResourceDiscoveryService     |   |      AwsObservabilityService       |
+------------------------------------+   +------------------------------------+
              |                                     |
              v                                     v
+------------------------------------+   +------------------------------------+
|            RdsProvider             |   |     CloudWatchMetricsProvider      |
|          (AwsRdsProvider)          |   |  (AwsCloudWatchMetricsProvider)    |
+------------------------------------+   +------------------------------------+
              |                                     |
              | DescribeDBInstances                 | GetMetricStatistics (AWS/RDS)
              v                                     v
+-------------------------------------------------------------+
|                      AWS Cloud APIs                         |
+-------------------------------------------------------------+
```

---

## 2. Normalized RDS Domain Models

Raw AWS SDK `DBInstance` objects are mapped to cohesive, immutable Java records:

| Domain Model | Responsibilities & Attributes |
|---|---|
| [`RdsDetailResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/RdsDetailResource.java) | Composite root: identifier, ARN, engine, version, instance class, status, AZ, multi-AZ, secondary AZ, promotion tier, deletion protection, IAM DB auth, CA certificate, tags. |
| [`RdsStorageConfiguration`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/RdsStorageConfiguration.java) | Allocated storage (GB), max allocated storage, storage type (`gp2`, `gp3`, `io1`), IOPS, throughput, encryption flag, KMS Key ID. |
| [`RdsBackupConfiguration`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/RdsBackupConfiguration.java) | Retention period (days), preferred backup window, latest restorable time, copy tags to snapshot flag. |
| [`RdsNetworkConfiguration`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/RdsNetworkConfiguration.java) | VPC ID, DB subnet group, subnet IDs, subnet AZs, security group IDs, security group statuses, public accessibility, endpoint address, endpoint port. |
| [`RdsMaintenanceConfiguration`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/RdsMaintenanceConfiguration.java) | Maintenance window, auto minor version upgrade, pending modified values flag. |
| [`RdsMonitoringConfiguration`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/RdsMonitoringConfiguration.java) | Enhanced monitoring status, monitoring interval (seconds), monitoring IAM role ARN. |
| [`RdsParameterGroupInfo`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/RdsParameterGroupInfo.java) | Parameter group name, apply status. |
| [`RdsOptionGroupInfo`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/RdsOptionGroupInfo.java) | Option group name, membership status. |

---

## 3. Database Observability (CloudWatch)

RDS metrics are integrated through the shared observability pipeline:
- **Namespace**: `AWS/RDS`
- **Dimension**: `DBInstanceIdentifier`
- **Supported Metrics**: `CPUUtilization`, `DatabaseConnections`, `FreeStorageSpace`, `FreeableMemory`, `ReadIOPS`, `WriteIOPS`, `ReadThroughput`, `WriteThroughput`, `DiskQueueDepth`, `BurstBalance`.
- **Query Validation**: Strict query window ($\le 30$ days), positive multiple of 60s period ($60 \le period \le 86400$), allowed statistics (`Average`, `Sum`, `SampleCount`, `Maximum`, `Minimum`).
- **Telemetry Series**: Datapoints are sorted chronologically and normalized into [`MetricSeries`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/observability/model/MetricSeries.java).

---

## 4. Security & Evidence-First Invariants

1. **Strict Read-Only**: Zero mutating operations. No `startDBInstance`, `stopDBInstance`, `modifyDBInstance`, `deleteDBInstance`, or snapshot creation.
2. **Zero Credential Exposure**: Passwords, connection authentication tokens, master user credentials, and KMS plaintext keys are never extracted, logged, or serialized.
3. **Evidence-First Representation**: Public accessibility, encryption status, and multi-AZ configurations are reported as objective factual boolean attributes without speculative risk ratings.
4. **Decoupled Architecture**: Controllers never import AWS SDK classes; all errors are mapped to sanitized domain exceptions.