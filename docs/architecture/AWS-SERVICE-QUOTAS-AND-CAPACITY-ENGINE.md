# AWS Service Quotas & Capacity Exhaustion Engine Architecture

## 1. Overview
The **AWS Service Quotas & Capacity Exhaustion Engine** (`com.cloudops.manager.aws.quota`) provides deterministic visibility into AWS service quotas and correlates applied limits with live discovered resource usage to prevent silent deployment or auto-scaling capacity exhaustion failures.

## 2. Key Capabilities
- **Multi-Region Support**: Dynamic client resolution and regional caching via `AwsClientFactory.getServiceQuotasClient(region)`.
- **vCPU Dimension Alignment**: Real-time aggregation of EC2 vCPU consumption (`L-1216C47A`) based on instance core topologies and state.
- **Threshold-Driven Classification**:
  - `NORMAL`: Utilization $< 80\%$
  - `WARNING`: Utilization $\ge 80\%$ and $< 90\%$
  - `CRITICAL`: Utilization $\ge 90\%$
  - `UNKNOWN`: Live usage or applied limit unavailable (prevents false-positive healthy states)
- **REST Endpoints**:
  - `GET /api/v1/quotas?region={region}`
  - `GET /api/v1/quotas/{serviceCode}?region={region}`

## 3. Core Models
- `ServiceQuotaItem`: Immutable record capturing `serviceCode`, `quotaCode`, `quotaName`, `appliedLimit`, `currentUsage`, `utilizationPercentage`, `status`, `region`, `usageSource`, and `unit`.
- `QuotaUtilizationReport`: Aggregated report capturing `totalQuotasTracked`, `criticalCount`, `warningCount`, `normalCount`, `unknownCount`, `highestUtilization`, `quotas`, and `statusSummary`.

## 4. Invariant Contract
$$\text{criticalCount} + \text{warningCount} + \text{normalCount} + \text{unknownCount} = \text{totalQuotasTracked} = \text{quotas.size()}$$
