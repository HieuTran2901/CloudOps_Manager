# CloudOps Manager — Security Group & Network Access Deep Inspection Architecture

## 1. Overview & Principles

The Security Group & Network Access Deep Inspection subsystem provides unified, read-only analysis of AWS Virtual Private Cloud Security Groups. It normalizes complex firewall rule structures (IPv4/IPv6 CIDRs, prefix lists, referenced security groups, and port ranges) and discovers reverse attachments to elastic network interfaces (ENIs), EC2 compute instances, and RDS database instances.

```text
+-------------------------------------------------------------+
|                     REST API Client                         |
+-------------------------------------------------------------+
              |                                     |
              | GET /resources/security-groups/{id} | GET /resources/security-groups/{id}/topology
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
|                    SecurityGroupProvider                    |
|                 (AwsSecurityGroupProvider)                  |
+-------------------------------------------------------------+
  |              |              |              |
  | DescSecGroup | DescNetInt   | DescCompute  | DescRDS
  v              v              v              v
+-------------------------------------------------------------+
|                     AWS Cloud Services                      |
|                  (EC2 / VPC / ENI / RDS)                    |
+-------------------------------------------------------------+
```

---

## 2. Normalized Security Group Domain Models

Raw AWS SDK objects are mapped to cohesive, immutable Java records:

| Domain Model | Responsibilities & Attributes |
|---|---|
| [`SecurityGroupDetailResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/SecurityGroupDetailResource.java) | Identity, name, description, VPC ID, owner ID, account ID, region, normalized inbound rules, normalized outbound rules, tags, discovery timestamp. |
| [`SecurityGroupRuleDetail`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/SecurityGroupRuleDetail.java) | Protocol (`tcp`, `udp`, `icmp`, `-1`), port range (`fromPort`, `toPort`), IPv4 CIDRs, IPv6 CIDRs, prefix list IDs, referenced security groups, description. |
| [`SecurityGroupReference`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/SecurityGroupReference.java) | Referenced security group ID, owner user ID, VPC ID, VPC peering connection ID, rule description. |
| [`NetworkInterfaceAttachment`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/NetworkInterfaceAttachment.java) | ENI ID, subnet ID, VPC ID, private IP address, interface type (`interface`, `lambda`, `nat_gateway`), attachment status (`attached`, `attaching`). |
| [`ComputeResourceAttachment`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/ComputeResourceAttachment.java) | Attached EC2 compute instance ID, instance name tag, instance type, runtime state. |
| [`DatabaseResourceAttachment`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/DatabaseResourceAttachment.java) | Attached RDS database instance identifier, engine, runtime status. |
| [`SecurityGroupAttachment`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/SecurityGroupAttachment.java) | Aggregates all attached ENIs, compute instances, and database instances. |
| [`SecurityGroupTopologyResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/SecurityGroupTopologyResource.java) | Composite root aggregating the Security Group detail and its complete attachment topology. |

---

## 3. Pagination Verification

All multi-resource relationship queries utilize AWS SDK v2 paginators:
- `ec2Client.describeSecurityGroupsPaginator(...)`
- `ec2Client.describeNetworkInterfacesPaginator(...)`
- `ec2Client.describeInstancesPaginator(...)`
- `rdsClient.describeDBInstancesPaginator(...)`

---

## 4. Security & Evidence-First Invariants

1. **Strict Read-Only**: Zero mutating operations. No `AuthorizeSecurityGroupIngress`, `AuthorizeSecurityGroupEgress`, `RevokeSecurityGroupIngress`, `RevokeSecurityGroupEgress`, `CreateSecurityGroup`, `DeleteSecurityGroup`, or `ModifySecurityGroupRules`.
2. **Zero Secret Leakage**: Passwords, private keys, and raw stack traces are never exposed.
3. **Evidence-First Representation**: Port ranges (e.g. `22`), CIDR blocks (e.g. `0.0.0.0/0`), and cross-security group references are reported as objective factual metadata without speculative risk labels.
4. **Decoupled Architecture**: Controllers never import AWS SDK classes; all errors are mapped to sanitized domain exceptions.