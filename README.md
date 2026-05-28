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

## Pull Request CI Pipeline

GitHub Actions workflow is available at .github/workflows/pr-ci.yml and runs on every pull request to main/master.

Stages:

1. Frontend job
1. npm ci
1. npm run lint
1. npm run build

1. Backend job
1. Maven test
1. Maven package

1. Compose smoke job
1. docker compose up --build
1. frontend availability check on port 3000
1. backend shorten and resolve smoke flow verification
1. logs on failure and teardown

PR feedback:

1. Workflow automatically posts (and updates) a single sticky PR comment with stage status:
1. frontend
1. backend
1. smoke
1. Direct link to the workflow run is included for quick triage.

This ensures that merged code is always in a deployable state.
