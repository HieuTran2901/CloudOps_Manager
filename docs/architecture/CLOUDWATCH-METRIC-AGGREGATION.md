# CloudOps Manager — CloudWatch Multi-Resource Metric Aggregation & Advanced Telemetry Architecture

## 1. Overview & Principles

The CloudWatch Telemetry subsystem provides deterministic, batch-optimized, strictly read-only metric querying across EC2, RDS, and Network Interface (ENI) resources. It leverages CloudWatch `GetMetricData` to batch up to 50 queries per API call, handles pagination with `NextToken`, performs deterministic downsampling and statistical rollups, and supports cross-account telemetry via the Phase 9 STS `AssumeRole` architecture.

```text
+-----------------------------------------------------------------------------------+
|                                 REST API Client                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          | GET /api/v1/aws/observability/metrics
                                          |  ?resourceType=EC2&resourceIds=i-1,i-2&...
                                          v
+-----------------------------------------------------------------------------------+
|                            AwsObservabilityController                             |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             AwsObservabilityService                               |
|                                                                                   |
|  1. Validate Query Parameters (Time range, period, statistics, batch limits)      |
|  2. Resolve Target Account & Scoped CloudWatch Client (Local or AssumeRole)       |
|  3. Construct MetricDataQueryRequest list (Namespaces, dimensions)                |
|  4. Delegate to CloudWatchMetricsProvider (AwsCloudWatchMetricsProvider)         |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                          AwsCloudWatchMetricsProvider                             |
|                                                                                   |
|  1. Partition queries into deterministic batches (BATCH_SIZE = 50)                |
|  2. Invoke CloudWatch GetMetricData with NextToken pagination loop                |
|  3. Aggregate results per Query ID                                                |
|  4. Apply TelemetryProcessingUtils (Downsampling, rollups, chronological sorting)  |
|  5. Return normalized MetricSeries list                                           |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             AWS CloudWatch API                                    |
+-----------------------------------------------------------------------------------+
```

---

## 2. Telemetry Processing & Rollup Semantics

- **Batching**: Automatically chunks queries into batches of 50 to prevent oversized requests.
- **Pagination**: Fully paginates through `GetMetricData` responses using `NextToken`.
- **Downsampling**: Groups consecutive datapoint buckets of size `downsampleFactor` and computes rollups (`Average`, `Sum`, `Minimum`, `Maximum`, `SampleCount`).
- **Fidelity Guarantee**: Missing datapoints are preserved as genuine gaps; fake zero-values are never synthesized.
- **Deterministic Ordering**: All returned series and datapoints are chronologically sorted.

---

## 3. Supported Resources & Namespaces

| Resource Type | CloudWatch Namespace | Primary Dimension | Supported Metrics |
|---|---|---|---|
| **EC2** | `AWS/EC2` | `InstanceId` | `CPUUtilization`, `NetworkIn`, `NetworkOut`, `DiskReadOps`, `DiskWriteOps`, `StatusCheckFailed` |
| **RDS** | `AWS/RDS` | `DBInstanceIdentifier` | `CPUUtilization`, `DatabaseConnections`, `FreeStorageSpace`, `FreeableMemory`, `ReadIOPS`, `WriteIOPS`, `ReadThroughput`, `WriteThroughput` |
| **ENI** | `AWS/EC2` | `NetworkInterfaceId` | `NetworkIn`, `NetworkOut` |

---

## 4. Multi-Account & Region Isolation

- Cross-account telemetry executes via `AwsObservabilityService.getCrossAccountMetrics`.
- Temporary STS credentials from `AssumeRole` are scoped to an ephemeral `CloudWatchClient` and released immediately upon query completion.
- Datapoint provenance strictly records `accountId` and `region`.