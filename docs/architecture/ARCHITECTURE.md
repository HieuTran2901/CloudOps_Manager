# CloudOps Manager — System Architecture

## 1. System Overview

CloudOps Manager is an enterprise cloud operations and security platform designed to discover, audit, monitor, and manage AWS infrastructure across multi-account environments.

### Architectural Style: Modular Monolith
- **Pattern**: Modular Monolith.
- **Rationale**: Keeps domain logic unified, simplifies transaction boundaries, eliminates distributed network latency/complexity, and lowers operational overhead.
- **Rule**: Do not introduce microservices. Maintain modular boundaries within the single backend deployment unit.

---

## 2. Component Stack

### Frontend (Planned)
- **Framework**: React
- **Language**: TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **Server State / Data Fetching**: TanStack Query
- **Visualization / Charts**: Recharts

### Backend (Implemented - Phase 1)
- **Platform**: Java 21 LTS
- **Framework**: Spring Boot 3.3.x
- **Build Tool**: Maven with Maven Wrapper (`mvnw`, `mvnw.cmd`)
- **Cloud Integration**: AWS SDK for Java 2.x (`software.amazon.awssdk:sts`, `auth`, `regions`)
- **Web Layer**: Spring MVC (`@RestControllerAdvice` global exception handling)
- **Security & Persistence** (Planned Phase 2+): Spring Security, Spring Data JPA, Flyway

### Data Layer (Planned Phase 2+)
- **Relational Database**: MySQL 8.x (Metadata, audit logs, resource projections)
- **Cache & Ephemeral Store**: Redis (Session storage, cache)

### Infrastructure & Operations
- **Infrastructure as Code**: Terraform
- **Containerization**: Docker
- **CI/CD**: GitHub Actions

---

## 3. Data Flow & Layering (Implemented Architecture)

```text
+-------------------------------------------------------+
|                 REST API Clients / UI                 |
+-------------------------------------------------------+
                           |
                           | GET /api/v1/aws/identity
                           v
+-------------------------------------------------------+
|                 AwsIdentityController                 |
|             (Spring Web REST Controller)              |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|                  AwsIdentityService                   |
|             (Application Domain Service)              |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|                 StsIdentityProvider                   |
|            (AWS Adapter Abstraction Layer)            |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|               AwsStsIdentityProvider                  |
|         (AWS SDK v2 Implementation & Error Map)       |
+-------------------------------------------------------+
                           |
                           | StsClient (DefaultCredentialsProvider)
                           v
+-------------------------------------------------------+
|                    AWS STS Service                    |
|             (Live Cloud Identity Provider)            |
+-------------------------------------------------------+
```

---

## 4. State & Source of Truth Principles

1. **AWS Live State is Source of Truth**:
   - AWS APIs remain the source of truth for all live cloud infrastructure state.
2. **Isolation of AWS SDK Code**:
   - Web controllers and business services never directly instantiate `StsClient`. All SDK interaction is strictly encapsulated within `com.cloudops.manager.aws.sts.provider`.
3. **Safe DTO Models**:
   - `CallerIdentity` (Account ID, ARN, User ID) is safely serialized without credential leakage.
   - `AssumedRoleSession` maintains sensitive temporary credentials in-memory only and masks them in string representations.
4. **Resilience & Safe Error Mapping**:
   - AWS SDK exceptions (`StsException`, `SdkClientException`) are mapped to clean domain exceptions (`AwsAuthenticationException`, `AwsIdentityException`) handled by `GlobalExceptionHandler`.
