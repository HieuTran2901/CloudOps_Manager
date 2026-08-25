# CloudOps Manager — Continuous Verification Strategy

## 1. Purpose
Continuous verification guarantees that CloudOps Manager continuously evaluates and enforces read-only safety, account isolation, and deterministic data contracts across local, synthetic, and live AWS environments.

---

## 2. Verification Layers

| Layer | Trigger | Scope | Invariant Checked |
|---|---|---|---|
| **Unit & Mock** | `mvnw test` | Backend providers & services | Logic correctness, boundary handling |
| **API Contract** | `ApiContractVerificationTest` | REST Controllers | Response schemas, zero sensitive keys |
| **Security Invariants** | `SecurityRegressionGateTest` | Static Codebase | 0 SDK in UI, 0 DB, 0 ProcessBuilder |
| **Isolation** | `MultiAccountIsolationRegressionTest` | Graph Engines | Cross-account BFS & exposure partition |
| **Determinism** | `DeterministicRepeatabilityTest` | Forensics & Topology | Bitwise SHA-256 repeatability |
| **Preflight** | `/api/v1/aws/preflight` | Live AWS STS & IAM | Real capability inspection & boundary detection |
| **Release Gate** | `/api/v1/release/gate` | Comprehensive Gate | Overall promotion decision & canonical digest |