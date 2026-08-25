# CloudOps Manager — VPC & Network Topology Deep Inspection Architecture

## 1. Overview & Principles

The VPC & Network Topology Deep Inspection subsystem provides unified, read-only topology mapping of Amazon Virtual Private Clouds (VPCs). It extracts factual configuration attributes across VPC networking, subnets, route tables, internet gateways, NAT gateways, network access control lists (NACLs), and VPC peering connections.

```text
+-------------------------------------------------------------+
|                     REST API Client                         |
+-------------------------------------------------------------+
              |                                     |
              | GET /resources/vpcs/{vpcId}         | GET /resources/vpcs/{vpcId}/topology
              v                                     v
+------------------------------------+   +------------------------------------+
|   AwsResourceDiscoveryController   |   |   AwsResourceDiscoveryController   |
+------------------------------------+   +------------------------------------+
              |                                     |
              v                                     v
+-------------------------------------------------------------+
|                AwsResourceDiscoveryService                  |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                         VpcProvider                         |
|                      (AwsVpcProvider)                       |
+-------------------------------------------------------------+
  |              |              |              |            |
  | DescribeVpcs | DescSubnets  | DescRoutes   | DescIGW    | DescNAT/NACL/Peering
  v              v              v              v            v
+-------------------------------------------------------------+
|                       AWS EC2 / VPC                         |
+-------------------------------------------------------------+
```

---

## 2. Normalized Network Topology Models

Raw AWS SDK objects are mapped to cohesive, immutable Java records:

| Domain Model | Responsibilities & Attributes |
|---|---|
| [`VpcDetailResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/VpcDetailResource.java) | VPC identity, state, primary CIDR, secondary CIDRs, IPv6 CIDRs, DHCP options ID, tenancy, default VPC flag, DNS support, DNS hostnames, tags. |
| [`SubnetDetailResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/SubnetDetailResource.java) | Subnet ID, ARN, VPC ID, CIDR block, IPv6 CIDR, availability zone, AZ ID, state, map public IP on launch, assign IPv6 on creation, available IP count, default for AZ, tags. |
| [`RouteTableDetailResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/RouteTableDetailResource.java) | Route table ID, VPC ID, main association status, associated subnet IDs, routes (destinations, targets, target types, states), tags. |
| [`InternetGatewayResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/InternetGatewayResource.java) | Internet gateway ID, attached VPC ID, attachment state, tags. |
| [`NatGatewayResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/NatGatewayResource.java) | NAT gateway ID, VPC ID, subnet ID, state, connectivity type, public IP, private IP, network interface ID, tags. |
| [`NetworkAclResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/NetworkAclResource.java) | NACL ID, VPC ID, default status, associated subnet IDs, inbound/outbound rules (rule number, protocol, action, CIDR, port ranges, ICMP), tags. |
| [`VpcPeeringResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/VpcPeeringResource.java) | Peering connection ID, requester VPC, accepter VPC, requester CIDR, accepter CIDR, status code, status message, tags. |
| [`VpcTopologyResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/VpcTopologyResource.java) | Composite topology root aggregating the VPC and all its associated subnets, route tables, internet gateways, NAT gateways, NACLs, and peering connections. |

---

## 3. Pagination Verification

All multi-resource network queries utilize AWS SDK v2 paginators to ensure complete dataset retrieval:
- `ec2Client.describeSubnetsPaginator(...)`
- `ec2Client.describeRouteTablesPaginator(...)`
- `ec2Client.describeNatGatewaysPaginator(...)`
- `ec2Client.describeNetworkAclsPaginator(...)`

---

## 4. Security & Evidence-First Invariants

1. **Strict Read-Only**: Zero mutating network operations. No `CreateVpc`, `DeleteVpc`, `CreateSubnet`, `DeleteSubnet`, `CreateRoute`, `DeleteRoute`, `AttachInternetGateway`, or `CreateNatGateway`.
2. **Zero Secret Leakage**: No credentials, private keys, or raw stack traces exposed.
3. **Evidence-First Representation**: Public IP auto-assignment, default route destinations (`0.0.0.0/0`), and NACL rules are reported as factual metadata without speculative risk classifications.
4. **Decoupled Architecture**: Controllers never import AWS SDK classes; all errors are mapped to sanitized domain exceptions.