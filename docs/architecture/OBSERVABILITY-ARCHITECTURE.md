# CloudOps Manager — Observability Architecture

## 1. Overview & Principles

The Observability subsystem provides a decoupled, read-only foundation for querying cloud runtime telemetry from AWS CloudWatch without creating persistent alarms, dashboards, or mutating configurations.

```text
+-------------------------------------------------------------+
|                     REST API Client                         |
+-------------------------------------------------------------+
                               |
                               | GET /api/v1/aws/resources/ec2/{instanceId}/metrics
                               v
+-------------------------------------------------------------+
|               AwsResourceDiscoveryController                |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                  AwsObservabilityService                    |
|      (Validation: Time Window <= 30d, Period % 60 == 0)     |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                 CloudWatchMetricsProvider                   |
|              (AwsCloudWatchMetricsProvider)                 |
+-------------------------------------------------------------+
                               |
                               | GetMetricStatistics
                               v
+-------------------------------------------------------------+
|                      AWS CloudWatch                         |
+-------------------------------------------------------------+
```

---

## 2. Telemetry Scope & Metrics

The system queries standard EC2 metric namespaces (`AWS/EC2`):
- `CPUUtilization` (Percent)
- `NetworkIn` / `NetworkOut` (Bytes)
- `DiskReadOps` / `DiskWriteOps` (Count)
- `DiskReadBytes` / `DiskWriteBytes` (Bytes)
- `StatusCheckFailed` / `StatusCheckFailed_Instance` / `StatusCheckFailed_System` (Count)

---

## 3. Query Model & Validation

The normalized domain model `MetricSeries` encapsulates metric responses:

```java
public record MetricSeries(
    String metricName,
    String instanceId,
    String region,
    String accountId,
    String unit,
    String statistic,
    int periodSeconds,
    Instant startTime,
    Instant endTime,
    List<MetricDataPoint> dataPoints
) {}
```

### Validation Constraints:
1. **Window Duration**: `startTime` must precede `endTime`; duration cannot exceed 30 days.
2. **Period Duration**: Must be a positive multiple of 60 seconds (min 60s, max 86400s).
3. **Statistics**: Restricted to `Average`, `Sum`, `SampleCount`, `Maximum`, `Minimum`.
4. **Data Sorting**: Discovered datapoints are sorted chronologically by timestamp.
5. **No Data Behavior**: An empty dataset returns a valid `MetricSeries` containing an empty `dataPoints` list without failing the query.