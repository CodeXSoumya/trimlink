# TrimLink

Distributed URL shortener built with Spring Boot, ZooKeeper, Redis, and Postgres shards.

This README is intentionally aligned with the current implementation in this repository.

## Current State At A Glance

1. Single deployable Spring Boot application runs in two profiles:
1. gateway profile: API ingress, rate limiting, and proxying to app nodes.
1. default profile: app node behavior, node discovery registration, KGS-backed ID leasing.
1. Cluster topology from docker-compose:
1. 1 gateway
1. 5 app nodes
1. 1 Redis
1. 3 Postgres shards
1. 1 ZooKeeper

Detailed architecture is documented in docs/current-state-architecture.md.

## Tech Stack

1. Java 17
1. Spring Boot 3.2.5
1. React 18 + Vite + TypeScript
1. Spring Data JPA
1. Spring Data Redis
1. Apache Curator (ZooKeeper client)
1. PostgreSQL 15
1. Redis 7
1. Docker and Docker Compose

## Public API

Gateway listens on port 8080.

1. Create short code

```bash
curl -X POST "http://localhost:8080/api/v1/shorten?longUrl=https://example.com" -H "X-Client-ID: developer_1"
```

Expected: 200 OK with short code body.

1. Resolve short code through gateway proxy path

```bash
curl -i "http://localhost:8080/api/v1/resolve/<shortCode>"
```

Expected: 302 Found with Location header.

1. Alternate direct redirect path available in current code

```bash
curl -i "http://localhost:8080/api/v1/<shortCode>"
```

Expected: 302 Found when mapping exists, 404 otherwise.

## Run Locally With Docker

```bash
docker compose up --build
```

Frontend URL:

```text
http://localhost:3000
```

Backend gateway URL:

```text
http://localhost:8080
```

Stop:

```bash
docker compose down
```

## Frontend Module

Frontend source lives in frontend and is production-ready with:

1. React + TypeScript + Vite build pipeline
1. Route handling and 404 fallback
1. Input validation (react-hook-form + zod)
1. API client with stable X-Client-ID identity header
1. Dockerized Nginx static serving and gateway API proxy

Local frontend-only run (if Node is available):

```bash
cd frontend
npm install
npm run dev
```

Production frontend build:

```bash
cd frontend
npm install
npm run build
```

## Rate Limiter Behavior

Gateway profile wires RedisSlidingWindowRateLimiter with current limits:

1. Window: 60 seconds
1. Max requests: 100 per client
1. Client identity: X-Client-ID header (falls back to remote IP)

Quick burst test:

```powershell
for ($i = 1; $i -le 110; $i++) {
     curl.exe -s -o NUL -w "%{http_code}`n" -X POST "http://localhost:8080/api/v1/shorten?longUrl=https://loadtest$i.com" -H "X-Client-ID: attacker_user"
}
```

## Development On Company Laptop Without Java Or Docker

You can still develop productively by using remote/CI validation.

1. Keep coding locally (editor only).
1. Push frequently to a branch.
1. Use GitHub Actions for build, test, and integration checks in cloud runners.
1. Review logs and artifacts from CI for feedback.
1. Optionally use a free cloud dev environment (GitHub Codespaces monthly free quota for personal account tiers that include it, or Gitpod alternatives) when full runtime testing is needed.

Recommended testing split:

1. Unit tests for pure logic classes (Base62, hashing, rate limiter logic).
1. Integration tests for persistence and Redis behavior using Testcontainers in CI.
1. End-to-end smoke test using docker compose in CI.

## Production Readiness Plan (Next Milestones)

1. Documentation parity: keep README and architecture docs synchronized with code behavior.
1. Security hardening:
1. move secrets to environment variables and secret manager
1. remove hardcoded credentials
1. add auth for internal endpoints
1. Reliability:
1. add health checks for gateway and node services
1. add retry/backoff and circuit-breaker around inter-node calls
1. remove recursive retry in cache-miss path and replace with bounded loop
1. Observability:
1. add structured logging, trace IDs, and metrics (Micrometer + Prometheus)
1. Testing:
1. meaningful unit and integration coverage
1. smoke tests on each PR
1. Deployment:
1. CI/CD pipeline to container registry and target platform

## Free Web Deployment Options

For true multi-service distributed deployment, fully free options are limited. Most providers now have usage caps or sleep behavior. Practical options:

1. Best practical low-cost/free-start path:
1. Deploy app services on Render free web services or Fly.io trial credits
1. Use Neon free Postgres for initial stage
1. Use Upstash free Redis
1. Replace ZooKeeper in internet-facing free tier by moving KGS to DB-sequence strategy (recommended for first public release)
1. Alternative:
1. Keep distributed architecture for local and CI validation
1. Deploy a simplified single-node production profile publicly for free tier hosting

If you want, next step can be adding CI workflows and a production profile so you can ship from your company laptop with zero local Java/Docker.
