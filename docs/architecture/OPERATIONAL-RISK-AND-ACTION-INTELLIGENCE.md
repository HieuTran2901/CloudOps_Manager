# Operational Risk & Action Intelligence Architecture

## 1. Overview
The **Operational Risk & Action Intelligence Layer** (`com.cloudops.manager.operations.risk`) serves as a unified cross-domain aggregation and correlation engine. It ingests observations from Service Quotas, Compliance Rules, Security Exposure analysis, Terraform Drift, and Resilience Incidents/Evidence to produce deterministic, explainable operational risks with actionable resolution checklists.

## 2. Core Principles
- **Read / Analyze / Recommend Only**: Zero automated cloud mutations or autonomous remediation pathways.
- **Deterministic Risk Rules (R001–R012)**:
  - **R001**: EC2 vCPU Quota Critical (`CAPACITY`, `CRITICAL`, `QUOTA`, `REQUIRES_APPROVAL`)
  - **R002**: General Quota Depletion Warning (`CAPACITY`, `HIGH`, `QUOTA`, `REQUIRES_APPROVAL`)
  - **R003**: Open Administrative Ingress (`SECURITY`, `CRITICAL`, `COMPLIANCE`, `HIGH_RISK`)
  - **R004**: Public Compute Node with Elevated Admin (`SECURITY`, `CRITICAL`, `COMPLIANCE`, `REQUIRES_APPROVAL`)
  - **R005**: Single-AZ RDS Production Database (`RELIABILITY`, `HIGH`, `COMPLIANCE`, `REQUIRES_APPROVAL`)
  - **R006**: IAM Users Missing MFA (`SECURITY`, `HIGH`, `COMPLIANCE`, `REQUIRES_APPROVAL`)
  - **R007**: S3 Bucket Public Access Gap (`SECURITY`, `HIGH`, `COMPLIANCE`, `REQUIRES_APPROVAL`)
  - **R008**: Direct Internet Reachability Exposure (`SECURITY`, `HIGH`, `SECURITY`, `REQUIRES_APPROVAL`)
  - **R009**: Out-of-Band Security Group Drift (`SECURITY`, `HIGH`, `DRIFT`, `REQUIRES_APPROVAL`)
  - **R010**: Missing IaC-Managed Infrastructure Resource (`OPERATIONAL`, `CRITICAL`, `DRIFT`, `HIGH_RISK`)
  - **R011**: Active Critical Control-Plane Incident (`OPERATIONAL`, `CRITICAL`, `RESILIENCE`, `READ_ONLY`)
  - **R012**: Expired Analytical Evidence Blindspot (`OPERATIONAL`, `MEDIUM`, `RESILIENCE`, `READ_ONLY`)

## 3. Invariant Guarantee
$$\text{criticalCount} + \text{highCount} + \text{mediumCount} + \text{lowCount} = \text{totalRisksTracked} = \text{risks.size()}$$

## 4. REST API & UI Integration
- **Endpoint**: `GET /api/v1/risks?region={region}&category={category}&severity={severity}`
- **UI**: Interactive `OperationalRiskCenter` on the main Dashboard with severity counters, category filters, expandable instructional checklists, verification badges, and Topology Blast-Radius inspection.
