# CloudOps Manager — Incident Detection & Evidence-Driven Recovery

## 1. Purpose
Provides deterministic in-memory correlation of operational failures (such as `AWS_ACCESS_DENIED`, `AWS_TIMEOUT`, `AWS_THROTTLED`) without creating unbounded duplicate records or introducing database persistence.

---

## 2. Correlation Key & Lifecycle

Incidents correlate based on:
$$\text{Correlation Key} = \text{type} : \text{accountId} : \text{region} : \text{source}$$

Lifecycle Transitions:
$$\text{HEALTHY} \longrightarrow \text{OPEN} \longrightarrow \text{RECOVERING} \longrightarrow \text{RESOLVED}$$

Recovery is strictly evidence-driven: an incident transitions to `RESOLVED` only after a subsequent operational check successfully executes.