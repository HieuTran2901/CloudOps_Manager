# CloudOps Manager — CloudTrail Audit & Operational Event History Architecture

## 1. Overview & Principles

The CloudTrail Audit subsystem provides strictly read-only operational event lookup and governance inspection. It interfaces with `cloudtrail:LookupEvents` to retrieve management and data event history across accounts and regions, applies deterministic filtering, supports NextToken pagination, sorts events chronologically, and integrates with Phase 9 STS `AssumeRole` for cross-account operational audits without database persistence.

```text
+-----------------------------------------------------------------------------------+
|                                 REST API Client                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          | GET /api/v1/aws/audit/cloudtrail/events...
                                          | GET /api/v1/aws/audit/accounts/{id}/cloudtrail/events...
                                          v
+-----------------------------------------------------------------------------------+
|                            CloudTrailAuditController                              |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             CloudTrailAuditService                                |
|                                                                                   |
|  1. Validate Query Parameters via CloudTrailValidationUtils (Time window, limits) |
|  2. Resolve Target Account & Scoped CloudTrailClient (Local or AssumeRole)        |
|  3. Delegate to CloudTrailProvider (AwsCloudTrailProvider)                        |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             AwsCloudTrailProvider                                 |
|                                                                                   |
|  1. Construct LookupEventsRequest with LookupAttributes (EventName/User/Resource) |
|  2. Execute query with NextToken pagination loop                                  |
|  3. Parse raw events into normalized CloudTrailEventResource and References        |
|  4. Sort chronologically (Descending / newest first)                              |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                              AWS CloudTrail API                                   |
+-----------------------------------------------------------------------------------+
```

---

## 2. API Scope & CloudTrail Constraints

- **LookupEvents Semantics**: AWS `LookupEvents` searches the past 90 days of management events and supported data events.
- **Lookup Attribute Constraints**: AWS `LookupEvents` API supports at most one primary lookup attribute key (`EventName`, `Username`, `ResourceName`, `ResourceType`). `CloudTrailValidationUtils` strictly validates mutual exclusivity before dispatching AWS network calls.
- **No Configuration Mutation**: No `CreateTrail`, `UpdateTrail`, `DeleteTrail`, `StartLogging`, or `StopLogging` calls exist.
- **Cross-Account Auditing**: Leverages ephemeral `CloudTrailClient` instances scoped via Phase 9 STS `AssumeRole` with caller identity verification.