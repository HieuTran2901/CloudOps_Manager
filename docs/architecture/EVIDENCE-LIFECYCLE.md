# CloudOps Manager — Evidence Lifecycle & Freshness Framework

## 1. Freshness Classification Policy

| State | Age Threshold | Description |
|---|---|---|
| **`FRESH`** | $\le 300\text{ seconds}$ | Recent, highly accurate evidence bundle |
| **`AGING`** | $301 - 900\text{ seconds}$ | Valid evidence requiring upcoming background refresh |
| **`STALE`** | $901 - 3600\text{ seconds}$ | Retained evidence; warning indicator active |
| **`EXPIRED`** | $> 3600\text{ seconds}$ | Expired evidence; marked for refresh |

Evidence hashes are computed using deterministic SHA-256 digests over canonical resource IDs.