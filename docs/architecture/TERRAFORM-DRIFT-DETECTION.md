# CloudOps Manager — Terraform Read-Only IaC Drift Detection Architecture

## 1. Overview & Principles

The Terraform Drift Detection subsystem performs strictly read-only, deterministic comparisons between Terraform desired state (provided as standard `.tfstate` JSON format v4) and live normalized AWS observed evidence from discovery services. It operates without Terraform CLI execution, local/remote state modifications, state locking, database persistence, or AWS mutations.

```text
+-----------------------------------------------------------------------------------+
|                                 REST API Client                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          | GET  /api/v1/aws/drift/supported-resources
                                          | POST /api/v1/aws/drift/evaluate
                                          | POST /api/v1/aws/drift/accounts/{id}/evaluate
                                          v
+-----------------------------------------------------------------------------------+
|                                DriftController                                    |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             DriftComparisonService                                |
|                                                                                   |
|  1. Parses desired state using TerraformStateParser (Pure JSON parsing)           |
|  2. Fetches live normalized AWS inventory via AwsResourceDiscoveryService         |
|  3. Normalizes & compares attributes using TerraformResourceNormalizer            |
|  4. Assembles deterministic DriftReport                                           |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                        Supported Terraform Resource Types                         |
|                                                                                   |
|  ├── aws_instance         (EC2: instance_type, subnet_id)                         |
|  ├── aws_security_group   (VPC/SG: vpc_id, name)                                  |
|  ├── aws_db_instance      (RDS: instance_class, multi_az)                         |
|  ├── aws_s3_bucket        (S3: bucket)                                            |
|  └── aws_vpc              (VPC: cidr_block)                                       |
+-----------------------------------------------------------------------------------+
```

---

## 2. Drift Status Semantics

- `IN_SYNC`: All normalized desired attributes match live AWS observed facts.
- `DRIFTED`: One or more normalized attributes differ from live AWS observed facts (includes explicit list of `DriftAttributeDifference`).
- `NOT_FOUND`: Resource address declared in Terraform state was not found in live AWS inventory.
- `UNSUPPORTED`: Resource type declared in Terraform state is not in the supported resource set.
- `INSUFFICIENT_EVIDENCE`: Required AWS discovery evidence could not be collected or resolved.

---

## 3. Account and Region Isolation

- Enforces `desired account == observed account` and `desired region == observed region`.
- Cross-account drift evaluation uses Phase 9 STS `AssumeRole` and `discoverAccount(target)`.