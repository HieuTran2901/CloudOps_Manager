# Change Impact & Blast-Radius Intelligence Architecture

## 1. Overview
The **Change Impact & Blast-Radius Intelligence Engine** (`com.cloudops.manager.operations.impact`) provides graph-theoretic analysis of upstream dependencies and downstream dependents across the AWS infrastructure topology.

## 2. Design Invariants
- **Composition over Duplication**: Reuses existing `TopologyGraph`, `TopologyNode`, `TopologyEdge`, and `TopologyQueryService` without creating duplicate graph structures.
- **Directional Semantics**:
  - Edge $A \rightarrow B$ means: *"A depends on / is attached to / is contained by B"*.
  - **Upstream Dependencies**: Follow forward outgoing edges ($A \rightarrow B$).
  - **Downstream Dependents (Blast Radius)**: Follow reverse incoming edges ($B \leftarrow A$).
- **Cycle Safety & Bounded BFS**:
  - Uses `visitedNodeIds` Set to guarantee safe termination on cyclic dependencies (e.g., mutual SG references).
  - Depth clamped between $1 \le \text{maxDepth} \le 10$ (default: 3) and hard-bounded at 1,000 steps.
- **Direct vs Indirect Classification**:
  - $\text{Direct}$: Discovered at minimum traversal depth $= 1$.
  - $\text{Indirect}$: Discovered at minimum traversal depth $> 1$.
  - Target node itself is excluded from affected counts.
- **Determinism**: Lexicographic ordering across nodes, edges, adjacency lists, and results.

## 3. Canonical Identity Mapping
- Formatted as:
  $$\text{nodeId} = \text{accountId} : \text{region} : \text{TopologyNodeType} : \text{resourceId}$$
- Non-graph resource types (e.g., `ALB`, `TARGET_GROUP`, `REDIS`, `S3`, `CLOUDWATCH`) are categorized as `AVAILABLE_BUT_NOT_GRAPH_NODE` and return `UNSUPPORTED_RESOURCE_TYPE`.

## 4. REST Endpoint
- **Endpoint**: `GET /api/v1/impact/blast-radius?resourceType={type}&resourceId={id}&region={region}&accountId={accountId}&maxDepth={depth}`
- Returns `ApiResponse<ImpactAnalysisResult>` containing `targetResource`, `totalAffectedResources`, `directAffectedCount`, `indirectAffectedCount`, `affectedTypeSummary`, `upstreamDependencies`, `downstreamDependents`, and `impactPaths`.
