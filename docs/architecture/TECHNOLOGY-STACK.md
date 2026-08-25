# CloudOps Manager — Technology Stack Reference

## Backend Architecture
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.x
- **Security & Authorization**: Spring Security (JWT / Role-Based Access Control)
- **Data Persistence**: Spring Data JPA / Hibernate
- **Database Migrations**: Flyway
- **Build & Dependency Management**: Apache Maven
- **AWS Integration**: AWS SDK for Java 2.x

## Database & Caching
- **Primary Database**: MySQL 8.x (Metadata, resource projections, audit records, user accounts)
- **In-Memory Cache & Broker**: Redis (Session management, discovery cache, rate-limiting)

## Frontend Architecture
- **Framework**: React 18+
- **Language**: TypeScript
- **Build Tooling**: Vite
- **CSS Framework**: Tailwind CSS
- **Server State & Caching**: TanStack Query (React Query)
- **Analytics & Telemetry UI**: Recharts
- **Icons & UI Primitives**: Lucide Icons / Headless UI

## AWS Services & Integrations
- **AWS STS**: Secure token service for AssumeRole temporary credential minting
- **AWS IAM**: Permission analysis, role tracking, stale key detection
- **AWS EC2**: Compute instance lifecycle, metadata, and security groups
- **AWS S3**: Storage bucket audit, encryption, public access block verification
- **AWS RDS**: Database instance and cluster inventory & health
- **AWS VPC & Networking**: Virtual Private Clouds, subnets, route tables, security group inspection
- **AWS CloudWatch**: Telemetry, operational metrics, and alarm status aggregation
- **AWS SQS**: Asynchronous operational event dispatching and task queues

## Infrastructure, Tooling & Deployment
- **Infrastructure as Code (IaC)**: Terraform
- **Containerization**: Docker & Docker Compose
- **Continuous Integration / Continuous Deployment (CI/CD)**: GitHub Actions
- **Container Registry**: AWS ECR
- **Container Orchestration**: AWS ECS (Fargate / EC2 launch types)
