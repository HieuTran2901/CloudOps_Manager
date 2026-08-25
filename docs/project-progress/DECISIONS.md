# Architecture Decisions

## ADR-001: Selection of MySQL as Primary Relational Database

- **ADR-ID**: ADR-001
- **Date**: 2026-08-23
- **Decision**: Use MySQL 8.x instead of PostgreSQL for the primary relational datastore.
- **Context**: The CloudOps Manager backend requires a relational database to store user accounts, authentication sessions, tenant configurations, resource inventory metadata projections, compliance audit logs, and scheduled operational policies.
- **Alternatives**: PostgreSQL, DynamoDB, SQLite.
- **Reason**: Team experience with Spring Boot + MySQL, avoidance of premature complexity, and focusing on AWS engineering.
- **Trade-offs**: Standard ANSI SQL and JSON querying without custom Postgres types.
- **Consequences**: Spring Boot and Flyway scripts target MySQL 8.

---

## ADR-002: Live AWS Discovery with Deferred Persistence

- **ADR-ID**: ADR-002
- **Date**: 2026-08-23
- **Decision**: Prioritize real-time live discovery via AWS SDK v2 without immediate persistence into MySQL during Phase 2.
- **Context**: Live infrastructure state changes dynamically in AWS. Persisting every discovered item immediately creates cache invalidation and drift overhead before core discovery models are stabilized.
- **Alternatives**: Write-through cache to MySQL on every discovery call; scheduled background polling worker.
- **Reason**: Ensures AWS is strictly maintained as the source of truth, eliminates premature database schemas, and keeps Phase 2 focused on robust read-only API contracts.
- **Trade-offs**: Live queries incur AWS API latency; pagination and rate limits must be strictly handled.
- **Consequences**: Inventory synchronization and historical snapshot persistence will be introduced in subsequent milestones.

---

## ADR-003: Explicit Region Discovery Strategy

- **ADR-ID**: ADR-003
- **Date**: 2026-08-23
- **Decision**: Use configured default region with explicit query parameter override rather than silent all-region scanning.
- **Context**: Scanning all AWS regions automatically increases latency exponentially and can trigger API throttling.
- **Alternatives**: Silent global scan across all 30+ AWS regions on every request.
- **Reason**: Predictable execution time, bounded network overhead, and transparent operational control.
- **Trade-offs**: Clients must request specific regions if querying resources outside their default region.
- **Consequences**: Endpoints accept an optional `?region=` parameter, falling back to `cloudops.aws.region`.

---

## ADR-004: Normalized Observability & Error Sanitization Abstraction

- **ADR-ID**: ADR-004
- **Date**: 2026-08-23
- **Decision**: Encapsulate CloudWatch metric retrieval behind `CloudWatchMetricsProvider` and implement centralized boundary exception translation.
- **Context**: Raw AWS SDK exceptions expose IAM user details and internal account numbers to API clients, and direct CloudWatch DTO exposure leaks AWS-specific data structures.
- **Alternatives**: Catching exceptions inside controllers; returning raw AWS exception strings; passing `GetMetricDataResponse` directly to frontend.
- **Reason**: Strict security posture, clean domain decoupling, and uniform client contracts.
- **Trade-offs**: Requires explicit translation layers between SDK DTOs and domain records.
- **Consequences**: No AWS credentials or internal IAM policy details can leak through client API responses.