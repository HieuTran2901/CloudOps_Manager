# CloudOps Manager — Production Packaging, Containerization & Local Orchestration

## 1. Overview & Principles

Phase 20 establishes reproducible, isolated multi-stage container packaging for both the backend (Spring Boot) and frontend (React/Vite/Nginx) with local multi-container orchestration via `docker-compose.yml`.

```text
+-----------------------------------------------------------------------------------+
|                              Host Environment (Browser)                           |
+-----------------------------------------------------------------------------------+
                                          |
                                          | HTTP :3000
                                          v
+-----------------------------------------------------------------------------------+
|                        cloudops-frontend (Nginx Static SPA)                       |
|                                                                                   |
|  - Serves compiled React 18 / Vite static assets from /usr/share/nginx/html       |
|  - Fallback routing for SPA URLs (overview, compliance, topology, forensics)      |
|  - Reverse proxies /api/ -> http://backend:8080/api/                              |
|  - Healthcheck on /healthz                                                        |
+-----------------------------------------------------------------------------------+
                                          |
                                          | Internal Bridge Network (cloudops-net)
                                          | HTTP :8080
                                          v
+-----------------------------------------------------------------------------------+
|                        cloudops-backend (Spring Boot JRE 21)                      |
|                                                                                   |
|  - Non-root user execution (`cloudops`)                                           |
|  - Multi-stage build (Temurin 21 JDK builder -> Temurin 21 JRE runtime)           |
|  - Healthcheck on /api/v1/health                                                  |
|  - Environment variable injection (AWS_REGION, AWS_ROLE_ARN)                      |
+-----------------------------------------------------------------------------------+
                                          |
                                          | Read-Only AWS APIs
                                          v
+-----------------------------------------------------------------------------------+
|                               AWS Cloud Services                                  |
+-----------------------------------------------------------------------------------+
```

---

## 2. Multi-Stage Container Architecture

### Backend (`backend/Dockerfile`)
- **Stage 1 (Builder)**: `eclipse-temurin:21-jdk-alpine` — Downloads dependencies offline, compiles Java 21 classes, and packages the production fat JAR.
- **Stage 2 (Runtime)**: `eclipse-temurin:21-jre-alpine` — Minimal footprint JRE runtime without Maven or source files. Runs as unprivileged system user `cloudops` with container-level health checking on `/api/v1/health`.

### Frontend (`frontend/Dockerfile`)
- **Stage 1 (Builder)**: `node:20-alpine` — Installs npm dependencies and compiles static bundle into `dist/`.
- **Stage 2 (Runtime)**: `nginx:alpine` — Minimal Nginx web server serving compiled assets, handling SPA route fallback (`try_files $uri $uri/ /index.html`), reverse proxying `/api/` calls to the backend, and offering lightweight health checks on `/healthz`.

---

## 3. Local Orchestration (`docker-compose.yml`)

- **Services**:
  - `backend`: Exposes `8080:8080`, configures `AWS_REGION` and `AWS_ROLE_ARN`, runs healthchecks.
  - `frontend`: Exposes `3000:80`, depends on `backend` healthy condition.
- **Networks**: Single isolated bridge network `cloudops-net`.

---

## 4. Startup & Shutdown Commands

```bash
# Start local orchestration
docker compose up -d

# Check service health
docker compose ps

# View backend logs
docker compose logs -f backend

# Stop orchestration
docker compose down
```

---

## 5. Docker Runtime Status on Build Environment

- **Docker CLI Version**: 29.7.2
- **Docker Compose Version**: v5.4.0
- **Docker Desktop Engine**: `DOCKER_RUNTIME_UNAVAILABLE` (Daemon not started on Windows host).
- **Validation**: Full static Dockerfile/Compose validation + local Maven clean test + Vite production build.