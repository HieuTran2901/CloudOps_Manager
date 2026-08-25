# CloudOps Manager — Production Deployment Execution

## 1. Deployment Topology

```
+----------------------------------------------------------------------------------------------------+
|                               AWS ECS FARGATE PRODUCTION DEPLOYMENT                                 |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  [ Ingress Layer ]                                                                                 |
|  AWS ALB / CloudFront (HTTPS TLS 1.3 Termination)                                                   |
|         |                                                                                          |
|         v                                                                                          |
|  [ Frontend Container Service ]                                                                    |
|  cloudops-frontend:1.0.0 (Nginx Alpine SPA Reverse Proxy, Port 80)                                 |
|         |                                                                                          |
|         v (Internal VPC Network Bridge)                                                            |
|  [ Backend API Container Service ]                                                                 |
|  cloudops-backend:1.0.0 (Eclipse Temurin JRE 21, Non-root UID 10001, Port 8080)                     |
|         |                                                                                          |
|         v (Read-Only AWS IAM Task Role)                                                            |
|  [ AWS Read-Only Intelligence Layer ]                                                              |
|  STS | EC2 | S3 | RDS | IAM | VPC | CloudWatch | CloudTrail | Cost Explorer                         |
|                                                                                                    |
+----------------------------------------------------------------------------------------------------+
```

---

## 2. Invariant Enforcement

1. **Zero Database Persistence**: All analytical graph structures and incident correlation buffers are ephemeral in-memory state.
2. **Zero AWS Mutation**: No mutation APIs exist in CloudOps Manager controllers.
3. **Zero Secrets in Images**: Images contain 0 hardcoded credentials or environment secrets.