# CloudOps Manager — Deployment Guide

## Prerequisites
- **Runtime**: Java 21 JRE, Node.js 20+ (for local builds), or Docker / Docker Compose.
- **AWS Credentials**: Standard AWS credentials configured via environment variables, `~/.aws/credentials`, or instance IAM roles.
- **Required IAM Permissions**: Read-only AWS actions (`ec2:Describe*`, `s3:List*`, `s3:GetBucket*`, `rds:Describe*`, `iam:List*`, `iam:Get*`, `cloudwatch:GetMetricData`, `ce:GetCostAndUsage`, `cloudtrail:LookupEvents`, `sts:GetCallerIdentity`, `sts:AssumeRole`).

## Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

Key environment variables:
- `AWS_REGION`: Target default AWS region (e.g. `us-east-1`).
- `AWS_ROLE_ARN`: Optional cross-account IAM role ARN to assume.
- `SERVER_PORT`: Backend HTTP port (default `8080`).
- `FRONTEND_PORT`: Frontend HTTP port (default `3000`).

## Deployment Options

### Option 1: Docker Compose (Recommended)
```bash
docker compose up -d --build
```
Access the application:
- Frontend Dashboard: `http://localhost:3000`
- Backend API & Health: `http://localhost:8080/api/v1/health`

### Option 2: Standalone Local Binary Execution
1. **Backend**:
   ```powershell
   cd backend
   $env:JAVA_HOME = "E:\java"
   .\mvnw.cmd clean package -DskipTests
   java -jar target/cloudops-manager-1.0.0.jar
   ```
2. **Frontend**:
   ```bash
   cd frontend
   npm install
   npm run build
   npm run preview -- --port 3000
   ```

## Rollback Procedure
To rollback to a previous release tag:
1. Stop running containers: `docker compose down`
2. Checkout target tag/commit: `git checkout <PREVIOUS_TAG>`
3. Restart containers: `docker compose up -d --build`