# CloudOps Manager — ECS Fargate Specification

## 1. Task Definition Specification

### Backend Container:
- **Image**: `351405419700.dkr.ecr.ap-southeast-2.amazonaws.com/cloudops-manager-backend:1.0.0`
- **CPU**: 1024 (1 vCPU)
- **Memory**: 2048 MB
- **User**: `10001:10001` (Non-root `cloudops`)
- **Health Check**: `CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health`

### Frontend Container:
- **Image**: `351405419700.dkr.ecr.ap-southeast-2.amazonaws.com/cloudops-manager-frontend:1.0.0`
- **CPU**: 256 (0.25 vCPU)
- **Memory**: 512 MB
- **Health Check**: `CMD wget --no-verbose --tries=1 --spider http://localhost:80/healthz`