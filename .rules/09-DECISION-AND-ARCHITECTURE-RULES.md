# Architecture Decision Rules

## Decision Record

Material architecture decisions must be recorded in:

```text
/docs/project-progress/DECISIONS.md
```

Use:

```text
ADR-ID:
Date:
Decision:
Context:
Alternatives:
Reason:
Trade-offs:
Consequences:
```

## Decisions That Require Documentation

Examples:
- MySQL instead of PostgreSQL.
- Modular monolith instead of microservices.
- AWS SDK instead of AWS CLI.
- STS AssumeRole strategy.
- Redis introduction.
- SQS introduction.
- Terraform strategy.
- ECS vs EC2 deployment.
- Multi-account model.
- Security policy decisions.

## Avoid Architecture by Trend

Do not add:
- Kafka
- Kubernetes
- Elasticsearch
- GraphQL
- microservices
- service mesh

merely because they are common technologies.

A technology must solve a demonstrated project requirement.

## Stability Rule

Once an architectural boundary is implemented and validated, do not change it casually.

Architecture changes require:
- reason;
- impact analysis;
- migration plan;
- validation plan.
