# CloudOps Manager — IAM Deep Inspection & Identity Security Architecture

## 1. Overview & Principles

The IAM Deep Inspection subsystem provides unified, strictly read-only analysis of AWS Identity and Access Management (IAM) entities and policies. IAM is a **global AWS service** (`aws-global`), and caller account identity establishes the authoritative partition boundary.

```text
+-------------------------------------------------------------+
|                     REST API Client                         |
+-------------------------------------------------------------+
              |                                     |
              | GET /resources/iam/users/{name}     | GET /resources/iam/roles/{name}
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
|                         IamProvider                         |
|                      (AwsIamProvider)                       |
+-------------------------------------------------------------+
  |              |              |              |
  | ListUsers    | GetUser/MFA  | ListRoles    | GetRole/Trust
  | ListPolicies | GetAccessKeys| ListProfiles | GetPolicyVersion
  v              v              v              v
+-------------------------------------------------------------+
|                     AWS IAM (Global)                        |
+-------------------------------------------------------------+
```

---

## 2. Normalized IAM Domain Models

Raw AWS SDK objects are parsed and mapped to immutable Java records:

| Domain Model | Responsibilities & Attributes |
|---|---|
| [`IamUserResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamUserResource.java) | Lightweight user summary: username, user ID, ARN, path, create date, account ID. |
| [`IamUserDetailResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamUserDetailResource.java) | Deep user configuration: MFA enabled, MFA device metadata, access key metadata, group memberships, attached managed policies, inline policy names, tags. |
| [`IamMfaDeviceInfo`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamMfaDeviceInfo.java) | Serial number, enable date (zero secret seed exposure). |
| [`IamAccessKeyMetadata`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamAccessKeyMetadata.java) | Access key ID, status (`Active`/`Inactive`), create date, last used date, service name, region. |
| [`IamRoleResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamRoleResource.java) | Lightweight role summary: role name, role ID, ARN, path, create date, account ID. |
| [`IamRoleDetailResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamRoleDetailResource.java) | Deep role configuration: description, max session duration, parsed trust policy statements, attached managed policies, inline policy names, instance profiles, tags. |
| [`IamTrustStatement`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamTrustStatement.java) | Effect, principals (e.g. `Service:ec2.amazonaws.com`), action, condition map. |
| [`IamInstanceProfileInfo`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamInstanceProfileInfo.java) | Instance profile name, ID, ARN, path, create date. |
| [`IamPolicyDetailResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamPolicyDetailResource.java) | Policy ARN, name, ID, path, attachable flag, attachment count, default version ID, create/update dates, policy type (`AWS_MANAGED`/`CUSTOMER_MANAGED`), parsed statements. |
| [`IamPolicyStatement`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamPolicyStatement.java) | Normalized statement: effect, actions, notActions, resources, notResources, condition map. |
| [`IamIdentityTopologyResource`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/model/IamIdentityTopologyResource.java) | Aggregate summary of discovered IAM users and roles for the account. |

---

## 3. Policy Document Normalization

[`IamPolicyDocumentParser`](file:///E:/Github%20project/CloudOps_Manager/backend/src/main/java/com/cloudops/manager/aws/discovery/provider/IamPolicyDocumentParser.java) handles the complexity of IAM policy JSON:
- Automatic URL decoding of URL-encoded policy documents.
- Deterministic polymorphism handling (single statement vs statement array, single string action/resource vs string arrays).
- Structured condition and principal extraction.

---

## 4. Security & Evidence-First Invariants

1. **Strict Read-Only**: Zero mutating operations. No `CreateUser`, `DeleteUser`, `CreateRole`, `DeleteRole`, `AttachUserPolicy`, `PutUserPolicy`, `CreateAccessKey`, `EnableMFADevice`, etc.
2. **Zero Secret Exposure**: Plaintext passwords, secret access keys, and MFA seeds are never queried, logged, or exposed in domain models.
3. **Evidence-First Representation**: Trust relationships, access key metadata, and policy statements are presented factually without speculative risk scoring.
4. **Global Service Handling**: IAM queries are executed against global IAM endpoints without redundant multi-region scans.