# CloudOps Manager — Rollback & Disaster Recovery

## 1. Stateless Rollback Principle
Because CloudOps Manager has zero persistent database storage and zero AWS infrastructure mutations, rollbacks are 100% instantaneous, deterministic, and non-destructive.

## 2. Rollback Command
```bash
aws ecs update-service \
  --cluster cloudops-production \
  --service cloudops-backend-service \
  --task-definition cloudops-backend:previous \
  --region ap-southeast-2
```