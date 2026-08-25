# CloudOps Manager — Compliance Intelligence & Evidence Correlation Architecture

## 1. Overview & Principles

The Compliance Evidence Correlation subsystem enhances the evaluation framework to correlate multiple normalized infrastructure evidence streams (e.g. EC2 instances, Security Groups, CloudTrail operational events) within explicit account and region boundaries without direct AWS SDK access, database persistence, or subjective risk scoring.

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
|  1. Collects multi-source normalized evidence (EC2, SGs, IAM, S3, RDS, CloudTrail)|
|  2. Builds CorrelatedEvidenceSet enforcing EvidenceScope (Account + Region)      |
|  3. Constructs ComplianceEvaluationContext with explicit EvidenceAvailability     |
|  4. Evaluates single and composite rules from ComplianceRuleRegistry              |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                              ComplianceRuleRegistry                               |
|                                                                                   |
|  Single-Source Rules:                                                             |
|  ├── SEC-IAM-001 (SecIamMfaRule): Verifies IAM MFA                                |
|  ├── SEC-SG-001  (SecSgOpenIngressRule): Flags 0.0.0.0/0 on sensitive ports       |
|  ├── REL-RDS-001 (RelRdsMultiAzRule): Verifies RDS Multi-AZ                       |
|  └── SEC-S3-001  (SecS3PublicAccessBlockRule): Verifies S3 Public Access Block     |
|                                                                                   |
|  Composite Rules (Multi-Source Correlation):                                      |
|  ├── SEC-EC2-001 (SecEc2PublicAdminExposureRule): EC2 (running + public IP)       |
|  │                 + Security Group (0.0.0.0/0 on port 22/3389)                   |
|  └── SEC-EC2-002 (SecEc2AdminActivityCorrelationRule): EC2 (public IP)           |
|                    + CloudTrail (operational management events on that instance)  |
+-----------------------------------------------------------------------------------+
```

---

## 2. Evidence Scope & Account Isolation

- `EvidenceScope`: Strictly binds `accountId` and `region`.
- Cross-account evidence cannot correlate with different account IDs.
- Cross-region evidence cannot correlate unless explicit global service scope (`global`) is declared.

---

## 3. Evidence Completeness & Status Semantics

- `EvidenceAvailability`: `COMPLETE`, `PARTIAL`, `UNAVAILABLE`, `NOT_REQUESTED`.
- Evaluation Statuses:
  - `PASS`: Criteria satisfied across correlated evidence.
  - `FAIL`: Concrete non-compliant multi-source condition observed.
  - `NOT_APPLICABLE`: Target scope has zero applicable resources (e.g. 0 running EC2 instances).
  - `INSUFFICIENT_EVIDENCE`: Required multi-source evidence is missing or unavailable.