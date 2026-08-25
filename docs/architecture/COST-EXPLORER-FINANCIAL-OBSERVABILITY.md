# CloudOps Manager — AWS Cost Explorer & Financial Observability Architecture

## 1. Overview & Principles

The Financial Observability subsystem integrates with AWS Cost Explorer (`ce:GetCostAndUsage`) to provide strictly read-only financial analysis across standalone and member AWS accounts. It adheres strictly to exact decimal arithmetic using `BigDecimal`, deterministic grouping and dimensions, explicit billing scope identification, and zero persistence of financial snapshots.

```text
+-----------------------------------------------------------------------------------+
|                                 REST API Client                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          | GET /api/v1/aws/costs?metric=UnblendedCost...
                                          | GET /api/v1/aws/costs/accounts/{accountId}...
                                          v
+-----------------------------------------------------------------------------------+
|                            CostObservabilityController                            |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             CostObservabilityService                              |
|                                                                                   |
|  1. Validate Query Parameters via CostValidationUtils (Metric, granularity, dates)|
|  2. Resolve Target Account & Scoped CostExplorerClient (Local or AssumeRole)      |
|  3. Determine Explicit Billing Scope (STANDALONE, MEMBER, CROSS_ACCOUNT_ASSUMED)  |
|  4. Delegate to CostExplorerProvider (AwsCostExplorerProvider)                    |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                            AwsCostExplorerProvider                                |
|                                                                                   |
|  1. Construct GetCostAndUsageRequest with DateInterval, Granularity, GroupBy      |
|  2. Execute query with NextPageToken pagination loop                              |
|  3. Parse results into exact BigDecimal monetary amounts (No float/double loss)   |
|  4. Map groups and total amounts into CostAggregationResult                       |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             AWS Cost Explorer API                                 |
+-----------------------------------------------------------------------------------+
```

---

## 2. Billing Scope & Multi-Account Semantics

- **Account Authorization**: Cost Explorer visibility is bounded by the target account's billing role.
  - Member accounts only have visibility into their own member spend.
  - Consolidated organization-level spend is accessible only if querying the Management/Payer account.
- **Cross-Account Telemetry**: Cross-account cost queries execute through Phase 9 STS `AssumeRole` and `AwsClientFactory.createCostExplorerClient(session)` with caller identity verification.
- **Explicit Metadata**: The returned `CostAggregationResult` explicitly identifies `billingScope` (`STANDALONE_ACCOUNT`, `MEMBER_ACCOUNT`, `CROSS_ACCOUNT_ASSUMED`).

---

## 3. Supported Metrics, Granularities & Dimensions

- **Supported Metrics**: `UnblendedCost`, `AmortizedCost`, `NetUnblendedCost`, `NetAmortizedCost`, `UsageQuantity`.
- **Supported Granularities**: `DAILY`, `MONTHLY`.
- **Supported GroupBy Dimensions**: `SERVICE`, `LINKED_ACCOUNT`, `USAGE_TYPE` (up to 2 concurrent dimensions per AWS limit).
- **Monetary Precision**: All monetary values are preserved as exact `BigDecimal` instances.

---

## 4. Cost-Awareness of Cost Explorer API Calls

AWS Cost Explorer API calls incur charges per request ($0.01 per paginated API request). CloudOps Manager minimizes calls through strict upfront validation (`CostValidationUtils`) before making AWS network requests and prevents uncontrolled polling loops.