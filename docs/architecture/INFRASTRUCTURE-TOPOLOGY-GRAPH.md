# CloudOps Manager — Infrastructure Topology Graph & Dependency Visualization Architecture

## 1. Overview & Principles

The Infrastructure Topology Graph subsystem constructs an immutable, deterministic, evidence-backed directed graph of normalized AWS resources (EC2, RDS, VPC, Subnets, Security Groups, IAM Roles). It operates without graph databases, persistence, or AWS mutations, providing cycle-safe path traversals, neighbor lookups, and relationship queries.

```text
+-----------------------------------------------------------------------------------+
|                                 REST API Client                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          | GET /api/v1/aws/topology
                                          | GET /api/v1/aws/topology/nodes/{nodeId}
                                          | GET /api/v1/aws/topology/nodes/{nodeId}/neighbors
                                          | GET /api/v1/aws/topology/path?from={a}&to={b}
                                          | GET /api/v1/aws/topology/accounts/{accountId}
                                          v
+-----------------------------------------------------------------------------------+
|                              TopologyController                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             TopologyQueryService                                  |
|                                                                                   |
|  1. Collects live normalized AWS discovery evidence (EC2, SG, VPC, Subnet, RDS)   |
|  2. Invokes TopologyGraphBuilder                                                  |
|  3. Executes deterministic query operations (findNeighbors, findPath)             |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             TopologyGraphBuilder                                  |
|                                                                                   |
|  ├── Node Construction: TopologyNode (EC2, RDS, VPC, SUBNET, SECURITY_GROUP, IAM) |
|  ├── Relationship Extractors:                                                     |
|  │   ├── Ec2TopologyExtractor      (EC2_IN_SUBNET, EC2_ATTACHED_SECURITY_GROUP)    |
|  │   ├── SubnetVpcTopologyExtractor(SUBNET_IN_VPC)                                |
|  │   └── RdsTopologyExtractor      (RDS_IN_VPC, RDS_IN_SUBNET, RDS_ATTACHED_SG)   |
|  └── Deterministic Sort: Sorted by nodeId and edgeId                              |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                         Immutable TopologyGraph Model                             |
|                                                                                   |
|  - Nodes: List<TopologyNode>                                                      |
|  - Edges: List<TopologyEdge>                                                      |
+-----------------------------------------------------------------------------------+
```

---

## 2. Deterministic Node & Edge Identity

- Node ID Format: `${accountId}:${region}:${resourceType}:${resourceId}` (e.g. `123456789012:us-east-1:EC2_INSTANCE:i-1234567890abcdef0`). Global resources use `global` as the region string.
- Edge ID Format: `${relationshipType}:${sourceNodeId}->${targetNodeId}` (e.g. `EC2_IN_SUBNET:123456789012:us-east-1:EC2_INSTANCE:i-123->123456789012:us-east-1:SUBNET:subnet-123`).

---

## 3. Supported Relationships

- `EC2_IN_SUBNET` (EC2 $\rightarrow$ Subnet)
- `SUBNET_IN_VPC` (Subnet $\rightarrow$ VPC)
- `EC2_ATTACHED_SECURITY_GROUP` (EC2 $\rightarrow$ Security Group)
- `RDS_IN_VPC` (RDS $\rightarrow$ VPC)
- `RDS_IN_SUBNET` (RDS $\rightarrow$ Subnet)
- `RDS_ATTACHED_SECURITY_GROUP` (RDS $\rightarrow$ Security Group)
- `EC2_USES_IAM_ROLE` (EC2 $\rightarrow$ IAM Role)

---

## 4. Traversal & Cycle Safety

- Path finding uses deterministic Breadth-First Search (BFS) expanding neighbors sorted by `nodeId`.
- Infinite loop prevention is guaranteed via a `visited` node set.