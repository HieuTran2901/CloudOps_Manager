# CloudOps Manager — Security Blast-Radius & Lateral Movement Topology Intelligence Architecture

## 1. Overview & Principles

The Security Blast-Radius and Lateral Movement subsystem provides an evidence-driven, deterministic analysis engine over the infrastructure topology graph. It evaluates resource reachability, blast-radius boundaries, administrative exposure propagation, and security group connections without risk-scoring heuristics, database persistence, or AWS mutations.

```text
+-----------------------------------------------------------------------------------+
|                                 REST API Client                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          | GET /api/v1/aws/security/blast-radius/{nodeId}
                                          | GET /api/v1/aws/security/reachability
                                          | GET /api/v1/aws/security/exposures
                                          | GET /api/v1/aws/security/lateral-movement
                                          | GET /api/v1/aws/security/accounts/{id}/blast-radius/{nodeId}
                                          v
+-----------------------------------------------------------------------------------+
|                           SecurityAnalysisController                              |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                            SecurityAnalysisService                                |
|                                                                                   |
|  1. Acquires TopologyGraph via TopologyQueryService                               |
|  2. Collects normalized EC2 and Security Group detail models                      |
|  3. Coordinates execution of Security Analysis Engines                            |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                         Security Analysis Engines                                 |
|                                                                                   |
|  ├── BlastRadiusAnalysisEngine: Deterministic BFS expansion up to maxDepth        |
|  ├── ReachabilityAnalysisEngine: Shortest-path analysis with tie-breaking         |
|  ├── ExposureAnalysisEngine: Public IP + unrestricted admin ingress (22/3389)     |
|  └── LateralMovementAnalysisEngine: Propagation from exposed nodes to graph targets|
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                            TopologyGraph & Evidence                               |
+-----------------------------------------------------------------------------------+
```

---

## 2. Analysis Semantics

- Blast Radius: Set of all reachable nodes and traversed edges starting from `sourceNodeId` bounded by `maxDepth` ($\ge 0$).
- Reachability: Shortest path between `sourceNodeId` and `targetNodeId` using deterministic BFS traversal.
- Exposure Evaluation (`SECURITY-EXPOSURE-001`): Identifies running EC2 instances with public IPv4 addresses attached to Security Groups allowing `0.0.0.0/0` on administrative ports (22, 3389).
- Lateral Movement Propagation (`SECURITY-LATERAL-002`): Traces concrete topology connections from an exposed source node to other infrastructure resources.

---

## 3. Account and Region Isolation

- Enforces explicit `accountId` and `region` scoping from topology node IDs.
- Cross-account analysis uses Phase 9 STS `AssumeRole` and caller identity verification.