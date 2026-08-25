# CloudOps Manager — AWS Security & Authentication Model

## 1. Authentication & AssumeRole Foundation

The CloudOps Manager platform operates on a zero-credential-persistence architecture.

### Key Authentication Principles:
1. **No Static Credential Storage**:
   - Long-lived IAM credentials, secrets, or passwords are never stored in databases, configuration files, or logs.
2. **STS Temporary Sessions**:
   - Dynamic cross-account and role assumption operations use AWS Security Token Service (`sts:AssumeRole`), generating short-lived temporary session tokens with automatic TTL expiration.
3. **In-Memory Credential Redaction**:
   - Security session models (`AssumedRoleSession`) override standard representation methods (`toString()`, logging formatters) to redact secret values as `[PROTECTED]`.
4. **Normalized Identity DTO**:
   - The public identity response exposes only high-level identifiers (`accountId`, `arn`, `userId`), preventing credential and secret leakage.

---

## 2. Phase 3 API Security & Error Sanitization

To ensure complete defense-in-depth, raw AWS SDK exceptions are intercepted and transformed at the boundary:

```text
AWS SDK Exception (e.g. 403 AccessDenied with Account & IAM details)
                     │
                     ▼
       AwsErrorTranslator.translate()
                     │
                     ▼
    Domain Exception (AwsAccessDeniedException)
                     │
                     ▼
         GlobalExceptionHandler
                     │
                     ▼
   Sanitized JSON API Error Response
   {
     "success": false,
     "errorCode": "AWS_ACCESS_DENIED",
     "message": "The configured AWS identity does not have permission to perform this operation.",
     "timestamp": "..."
   }
```

### Sanitization Invariants:
- **Zero IAM Leakage**: IAM usernames, policy action names, internal ARNs, and sensitive AWS exception parameters are stripped from client responses.
- **Structured Error Mapping**:
  - `401 Unauthorized`: `AWS_AUTH_FAILED` (credential expiration, invalid tokens)
  - `403 Forbidden`: `AWS_ACCESS_DENIED` (IAM permission denial)
  - `404 Not Found`: `RESOURCE_NOT_FOUND` (invalid instance ID, missing bucket)
  - `429 Too Many Requests`: `AWS_THROTTLED` (AWS API rate limits)
  - `504 Gateway Timeout`: `AWS_TIMEOUT` (network/SDK call timeout)
  - `503 Service Unavailable`: `AWS_SERVICE_UNAVAILABLE` (AWS regional service outage)