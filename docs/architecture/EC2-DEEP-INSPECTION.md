# CloudOps Manager — EC2 Deep Inspection Architecture

## 1. Overview

The EC2 Deep Inspection subsystem provides in-depth, read-only architectural visibility into compute instances, attached EBS storage volumes, network interfaces (ENIs), private/public IP mappings, and associated security group references.

```text
+-------------------------------------------------------------+
|                     REST API Client                         |
+-------------------------------------------------------------+
                               |
                               | GET /api/v1/aws/resources/ec2/{instanceId}
                               v
+-------------------------------------------------------------+
|               AwsResourceDiscoveryController                |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                AwsResourceDiscoveryService                  |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                         Ec2Provider                         |
|                      (AwsEc2Provider)                       |
+-------------------------------------------------------------+
                 |                           |
                 | DescribeInstances         | DescribeVolumes
                 v                           v
+-------------------------------------------------------------+
|                          AWS EC2                            |
+-------------------------------------------------------------+
```

---

## 2. Deep Inspection Invariants

1. **Strictly Read-Only**:
   - Zero mutating operations exist. The provider exclusively issues `DescribeInstances` and `DescribeVolumes` queries.
2. **Normalized Domain Model**:
   - The returned model `Ec2DetailResource` decouples upstream callers from AWS SDK v2 data structures.
3. **Evidence-First Representation**:
   - Storage encryption status (`encrypted: true/false`), volume sizes, and interface statuses are recorded purely as verifiable factual attributes without speculative risk ratings or recommendations.
4. **AWS as Live Source of Truth**:
   - State is resolved dynamically via AWS SDK; no relational cache tables or synthetic database entities are maintained in Phase 3.

---

## 3. Data Model Hierarchy

```text
Ec2DetailResource
 ├── Identity: instanceId, arn, accountId, region, name
 ├── Compute: instanceType, architecture, platform, platformDetails, amiId, kernelId, launchTime
 ├── Runtime: instanceState, stateReason, instanceLifecycle, monitoringState
 ├── Placement: availabilityZone, placementGroup, tenancy
 ├── Network Summary: vpcId, subnetId, privateIp, publicIp, privateDnsName, publicDnsName
 ├── Storage Attachments (Ec2EbsAttachment):
 │    ├── volumeId, deviceName, sizeGiB, volumeType, iops, throughput
 │    └── encrypted, state, availabilityZone, attachTime, deleteOnTermination
 ├── Network Interfaces (Ec2NetworkInterfaceDetail):
 │    ├── networkInterfaceId, subnetId, vpcId, primaryPrivateIp, privateIpAddresses
 │    ├── publicIp, macAddress, securityGroupIds, securityGroupNames
 │    └── status, description, interfaceType
 └── Tags: Map<String, String>
```

---

## 4. Error Sanitization & Not-Found Handling

- When an invalid or nonexistent instance ID is supplied, AWS SDK returns `InvalidInstanceID.NotFound` or `InvalidInstanceID.Malformed`.
- The provider maps this to `Optional.empty()`, prompting the domain service to raise `ResourceNotFoundException`.
- `GlobalExceptionHandler` converts this to a clean HTTP 404 response with `RESOURCE_NOT_FOUND`.