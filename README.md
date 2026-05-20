# TrimLink: High-Concurrency Distributed URL Shortener

TrimLink is a production-grade, highly available, fault-tolerant distributed URL shortener designed to handle massive concurrent write and read traffic. Built using a multi-node Spring Boot cluster orchestrated inside Docker, the system implements core distributed systems concepts—including Consistent Hashing, Sliding Window Rate Limiting, a Centralized Distributed Key Generation Service (KGS), and multi-tier Cache-Aside persistence.

## 🚀 System Architecture Overview

[ Client Request ]
                            │
                            ▼
               ┌─────────────────────────┐
               │   API Gateway Layer     │ (Port 8080)
               │  └─ Sliding Window RL   │
               │  └─ Murmur3 Hash Ring   │
               └────────────┬────────────┘
                            │
           ┌────────────────┴────────────────┐
           ▼ (Routed via Consistent Hashing) ▼
 ┌───────────────────┐             ┌───────────────────┐
 │  App Node 1...5   │             │  App Node 6...10  │ (Ports 8081-8090)
 └─────────┬─────────┘             └─────────┬─────────┘
           │                                 │
           ├─────────────────────────────────┤
           ▼                                 ▼
┌───────────────────────┐         ┌───────────────────────┐
│ Apache ZooKeeper KGS  │         │  Redis Cache Cluster  │
│  (Atomic ID Leases)   │         │    (L1 Cache-Aside)   │
└───────────────────────┘         └───────────────────────┘
│
▼
┌───────────────────────┐
│ PostgreSQL Datastore  │
│  (L2 Persistent Row)  │
└───────────────────────┘


### Key Architectural Patterns Implemented:
* **API Gateway & Layer-7 Routing:** Acts as the cluster single-entry point. Protects backend infrastructure via a custom custom **Sliding Window Rate Limiter** and proxies requests dynamically.
* **Consistent Hashing (Murmur3):** Eliminates hot-spotting. The gateway maps destination long URLs across a virtual token ring containing 10 separate application node containers, ensuring static data partitioning.
* **Centralized Key Generation Service (KGS):** Solves the multi-node sequence collision problem. Nodes communicate with an **Apache ZooKeeper Quorum Cluster** via Apache Curator to atomically lease block ranges (e.g., blocks of 1,000 IDs) using distributed Compare-And-Swap (CAS) transactions. 
* **High Performance Read Path (Cache-Aside):** Shortened token expansion achieves sub-millisecond response latency by leveraging an automated L1 **Redis Cache Lookup Layer** before falling back to the persistent L2 database row.
* **Base62 Numeric Bijective Encoding:** Database-generated integer sequence identifiers are compressed mathematically into compact, short alphanumeric strings `[a-zA-Z0-9]`.

---

## 🛠️ Tech Stack & Infrastructure Components
* **Core Framework:** Spring Boot 3.x (Java 17)
* **Distributed Coordinator:** Apache ZooKeeper 3.9 (via Apache Curator)
* **High-Speed Cache Engine:** Redis 7 (Alpine Distribution)
* **Persistent Datastore:** PostgreSQL 15 Relational Engine
* **Containerization Engine:** Docker & Docker Compose

---

## 🏃‍♂️ Local Verification & Stress Testing Workbook

Follow these steps to build, run, and stress-test the entire distributed architecture infrastructure locally on your machine.

### 1. Build and Boot the Cluster
From your project root directory containing the `docker-compose.yml` file, run:

```
docker-compose up --build
```

Watch the logs to see PostgreSQL initialize, ZooKeeper open its coordination socket, and all 10 application nodes boot up simultaneously.

### 2. Verify the Write Path (URL Shortening)
Execute an HTTP POST request passing a client ID tracking header to target the gateway:

```
curl.exe -X POST "http://localhost:8080/api/v1/shorten?longUrl=https://github.com/CodeXSoumya/" -H "X-Client-ID: developer_1"
```

Expected Response: A Base62 shortcode token like b or c.
Check your main terminal window to witness the gateway hash the string and route it to a specific node, which seamlessly leases an ID block from ZooKeeper.

### 3. Verify the Read Path (302 Redirection)
Query the gateway resolve endpoint to expand the short token:
```
curl.exe -v "http://localhost:8080/internal/resolve/b"
```
Expected Response: An HTTP 200 Found with header pointing dynamically back to Google Maps.

### 4. Verify Rate Limiter Layer Under Attack
Simulate a rapid denial-of-service burst of 12 requests in under 10 seconds:

```
for ($i = 1; $i -le 12; $i++) {
     curl.exe -s -w "`nResponse Code: %{http_code}`n" `
          -X POST "http://localhost:8080/api/v1/shorten?longUrl=https://test$i.com" `
          -H "X-Client-ID: attacker_user"
}
```
Expected Response: The first 5 requests process successfully, and then the gateway immediately severs the connection returning Too Many Requests: Try again later (Response Code: 429), protecting the internal compute cluster from resource starvation.

### 5. Tear Down Cluster Memory
```
docker-compose down
```
