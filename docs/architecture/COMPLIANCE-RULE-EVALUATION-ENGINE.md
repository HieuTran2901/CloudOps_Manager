# CloudOps Manager — AWS Well-Architected & Compliance Rules Evaluation Engine Architecture

## 1. Overview & Principles

The Compliance Evaluation Engine provides a deterministic, evidence-first framework that evaluates normalized AWS infrastructure facts against well-defined rules across Security, Reliability, Cost, and Performance pillars. It operates strictly on normalized domain models produced by existing discovery and observability providers without database persistence or subjective risk scoring.

```text
+-----------------------------------------------------------------------------------+
|                                 REST API Client                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          | GET /api/v1/aws/compliance/rules
                                          | GET /api/v1/aws/compliance/evaluate
                                          | GET /api/v1/aws/compliance/accounts/{id}/evaluate
                                          v
+-----------------------------------------------------------------------------------+
|                              ComplianceController                                 |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                           ComplianceEvaluationService                             |
|                                                                                   |
|  1. Collects normalized evidence via existing discovery services                  |
|  2. Builds ComplianceEvaluationContext (Snapshots of IAM, SGs, S3, RDS, etc.)    |
|  3. Executes registered rules in ComplianceRuleRegistry                           |
|  4. Aggregates results into ComplianceEvaluationReport                            |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                              ComplianceRuleRegistry                               |
|                                                                                   |
|  ├── SEC-IAM-001 (SecIamMfaRule): Verifies MFA enabled on all IAM users           |
|  ├── SEC-SG-001  (SecSgOpenIngressRule): Flags 0.0.0.0/0 on ports 22 & 3389      |
|  ├── REL-RDS-001 (RelRdsMultiAzRule): Verifies Multi-AZ enabled on RDS instances  |
|  └── SEC-S3-001  (SecS3PublicAccessBlockRule): Verifies S3 Public Access Block     |
+-----------------------------------------------------------------------------------+
```

---

## 2. Evaluation Status Semantics

- `PASS`: Objective criteria fully satisfied.
- `FAIL`: Concrete non-compliant evidence observed.
- `NOT_APPLICABLE`: Target resource category does not exist in the queried scope.
- `INSUFFICIENT_EVIDENCE`: Required factual evidence is unavailable or inaccessible (e.g. IAM permission missing).

---

## 3. Evidence-First Rule Contract

- Pure evaluation logic with zero side-effects and zero direct AWS SDK calls.
- Concrete evidence items (`ComplianceEvidence`) contain observed resource IDs, types, and fact maps without speculative or subjective risk scores.