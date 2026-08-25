# CloudOps Manager — Cloud Architecture Forensic Audit & Evidence Export Architecture

## 1. Overview & Principles

The Forensic Audit & Evidence Export subsystem aggregates normalized evidence across Discovery, Compliance, Topology, and Security Reachability into deterministic, structured, cryptographically verifiable forensic evidence bundles. Supported export formats include canonical JSON and CSV, signed with SHA-256 digests.

```text
+-----------------------------------------------------------------------------------+
|                                 REST API Client                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          | GET /api/v1/aws/forensics/export?format=json
                                          | GET /api/v1/aws/forensics/export?format=csv
                                          | GET /api/v1/aws/forensics/accounts/{id}/export
                                          v
+-----------------------------------------------------------------------------------+
|                            ForensicAuditController                                |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             ForensicAuditService                                  |
|                                                                                   |
|  1. Invokes ForensicEvidenceAggregator                                            |
|  2. Coordinates JSON or CSV Exporter with SHA-256 Signer                          |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                          ForensicEvidenceAggregator                               |
|                                                                                   |
|  ├── Discovery Subsystem (EC2, S3, RDS, VPC, SG, IAM)                             |
|  ├── Compliance Evaluation Subsystem (Rule Results & Findings)                    |
|  ├── Infrastructure Topology Subsystem (Nodes & Edges)                            |
|  └── Security Analysis Subsystem (Exposures & Lateral Movement)                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                       Forensic Exporters & Integrity                              |
|                                                                                   |
|  ├── ForensicJsonExporter: Formatted canonical JSON bundle                        |
|  ├── ForensicCsvExporter: Escaped tabular CSV format                              |
|  └── ForensicIntegritySigner: SHA-256 Cryptographic Digest                       |
+-----------------------------------------------------------------------------------+
```

---

## 2. Forensic Metadata & Tamper Detection

- Every exported bundle includes:
  - `bundleId`
  - `accountId`
  - `region`
  - `generatedAt`
  - `format` (`JSON` / `CSV`)
  - `sha256Digest`
  - `sectionCounts`
- The `X-Forensic-SHA256-Digest` HTTP header provides tamper verification for offline validation.