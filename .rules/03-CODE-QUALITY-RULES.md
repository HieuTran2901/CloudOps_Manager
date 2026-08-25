# Code Quality Rules

## General

Code must be:
- readable;
- explicit;
- testable;
- cohesive;
- minimally coupled;
- production-oriented.

## Naming

Prefer domain-specific names.

Bad:

```java
AwsService
Manager
Helper
Utils
Data
```

Good:

```java
Ec2InventoryService
AwsStsRoleAssumer
SecurityFindingRepository
ResourceSynchronizationService
```

## Methods

Prefer small cohesive methods.

Target:
- normal method: <= 40 lines
- complex method: <= 80 lines only with justification

Avoid deeply nested conditionals.

## Exceptions

Do not swallow exceptions.

Bad:

```java
try {
    ...
} catch (Exception e) {
}
```

Every caught exception must be:
- handled;
- translated;
- logged safely;
- or rethrown.

## Null Handling

Do not use null as an undocumented state.

Use:
- validation;
- Optional where appropriate;
- explicit result/error types where useful.

## Logging

Logs must contain enough context to debug failures.

Never log secrets.

Prefer structured context such as:
- request ID;
- account ID;
- resource ID;
- operation;
- result.

## Configuration

No environment-specific secrets in source code.

Use configuration/environment variables.

Production configuration must fail clearly when required configuration is missing.

## Comments

Do not comment obvious code.

Comments should explain:
- why;
- security constraints;
- AWS-specific behavior;
- non-obvious trade-offs.

## TODO

Every TODO must have enough context to become an actionable task.

Bad:

```text
TODO: fix this
```

Good:

```text
TODO(CLOUD-042): replace polling with SQS event processing after MVP.
```
