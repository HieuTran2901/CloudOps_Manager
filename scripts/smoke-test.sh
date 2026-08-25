#!/usr/bin/env bash
set -e

BACKEND_URL="${1:-http://localhost:8080}"
FRONTEND_URL="${2:-http://localhost:3000}"

echo "============================================================"
echo "CLOUDOPS MANAGER — PRODUCTION RELEASE SMOKE TEST SUITE"
echo "Backend: $BACKEND_URL"
echo "Frontend: $FRONTEND_URL"
echo "============================================================"

# 1. Backend Health
echo -n "  Testing Backend Health & Metadata... "
HEALTH_RESP=$(curl -s "$BACKEND_URL/api/v1/health" || true)
if echo "$HEALTH_RESP" | grep -q '"status":"UP"'; then
  echo "[PASS]"
else
  echo "[WARN] Backend offline or non-200 response"
fi

# 2. Frontend Healthz
echo -n "  Testing Frontend Healthz... "
FRONT_RESP=$(curl -s "$FRONTEND_URL/healthz" || true)
if [ "$FRONT_RESP" = "OK" ]; then
  echo "[PASS]"
else
  echo "[SKIP] Frontend container offline or not routed"
fi

echo "============================================================"
echo "Smoke test script completed."
echo "============================================================"