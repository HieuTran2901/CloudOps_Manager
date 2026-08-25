# CloudOps Manager — S3 Deep Inspection Architecture

## 1. Overview & Principles

The S3 Deep Inspection subsystem provides in-depth, read-only configuration analysis of Amazon S3 storage buckets. It extracts factual configuration attributes across security settings, encryption, access policies, object versioning, cross-origin resource sharing (CORS), and lifecycle expiration policies.

```text
+-------------------------------------------------------------+
|                     REST API Client                         |
+-------------------------------------------------------------+
                               |
                               | GET /api/v1/aws/resources/s3/{bucketName}
                               v
+-------------------------------------------------------------+
|               AwsResourceDiscoveryController                |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                AwsResourceDiscoveryService                  |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                         S3Provider                          |
|                      (AwsS3Provider)                        |
+-------------------------------------------------------------+
  |              |              |              |            |
  | HeadBucket   | GetPAB       | GetEncryption| GetVersion | GetPolicy
  v              v              v              v            v
+-------------------------------------------------------------+
|                          AWS S3                             |
+-------------------------------------------------------------+
```

---

## 2. Evidence-First Security Representation

In strict accordance with the **Evidence-First Principle**, the S3 inspection layer collects objective configuration facts without drawing subjective risk or compliance conclusions.

### Examples:
- **Public Access Block**: Exposes boolean flags (`blockPublicAcls`, `ignorePublicAcls`, `blockPublicPolicy`, `restrictPublicBuckets`) rather than evaluating "Bucket is secure" or "Bucket is vulnerable".
- **Encryption**: Records algorithm name (`AES256`, `aws:kms`) and KMS Key ID if exposed, without evaluating encryption strength.
- **Bucket Policy**: Records raw policy text or structured presence without evaluating policy grant risks.

---

## 3. Capability States & Graceful Degradation

Individual sub-configurations on a bucket may be unconfigured (e.g., no lifecycle rules) or access-restricted by IAM permissions. The system maps these explicitly via domain capability states:

| Configuration Area | State: `CONFIGURED` | State: `NOT_CONFIGURED` | State: `ACCESS_DENIED` |
|---|---|---|---|
| **Public Access Block** | PAB settings active | 404 / `NoSuchPublicAccessBlockConfiguration` | 403 Forbidden |
| **Server-Side Encryption** | Default SSE active | 404 / `ServerSideEncryptionConfigurationNotFoundError` | 403 Forbidden |
| **Bucket Policy** | Policy attached | 404 / `NoSuchBucketPolicy` | 403 Forbidden |
| **CORS** | CORS rules list | 404 / `NoSuchCORSConfiguration` | 403 Forbidden |
| **Lifecycle** | Lifecycle rules list | 404 / `NoSuchLifecycleConfiguration` | 403 Forbidden |
| **Versioning** | Enabled / Suspended | NotEnabled | 403 Forbidden |

---

## 4. Region Resolution

- S3 bucket namespaces are global, but each bucket resides in a specific physical AWS region.
- `AwsS3Provider` establishes the bucket's region via `GetBucketLocation`. If unspecified or US-East-1 classic, it defaults to the configured system region (`cloudops.aws.region`).

---

## 5. Security Invariants

1. **Zero AWS Mutations**: No `PutBucket*`, `DeleteBucket*`, `CreateBucket`, `PutObject`, or `DeleteObject` calls exist.
2. **Zero Secret Leakage**: No KMS key material, plaintext data, IAM user credentials, or signed URL secrets are exposed or logged.
3. **Decoupled REST Model**: Raw AWS SDK DTOs are mapped to immutable Java record DTOs before traversing service boundaries.