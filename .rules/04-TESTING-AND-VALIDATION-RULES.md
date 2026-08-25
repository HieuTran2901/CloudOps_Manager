# Testing and Validation Rules

## Test Pyramid

Prefer:

```text
Unit Tests
    ↓
Integration Tests
    ↓
AWS Integration Tests
    ↓
End-to-End Tests
```

Do not make every test an AWS integration test.

## Required Tests

For every meaningful backend feature, consider:

- happy path;
- validation failure;
- authorization failure;
- AWS failure;
- timeout/retry behavior;
- idempotency;
- persistence behavior.

## AWS Testing

AWS APIs should be isolated behind provider interfaces.

Unit tests should not require a real AWS account.

Real AWS integration tests may run separately.

## No Fake Success

Never replace a failing AWS integration with a fake response merely to make tests pass.

If an external environment is unavailable:

```text
Status: BLOCKED
Reason: AWS integration environment unavailable
```

## Validation Commands

The project should maintain documented commands for:

```text
compile
unit tests
integration tests
lint/static analysis
frontend build
frontend tests
```

## Regression Rule

Before declaring a task complete:
- relevant tests must pass;
- existing affected tests must pass;
- build must succeed when applicable.

## Failure Reporting

When validation fails, report:

```text
Command:
Expected:
Actual:
Likely cause:
Is it caused by this change? YES/NO/UNKNOWN
```

Never hide failures.
