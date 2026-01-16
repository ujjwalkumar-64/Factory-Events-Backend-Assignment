

# BENCHMARK.md

## Performance Benchmarking

This document explains how to benchmark the **Factory Events Backend Assignment** ingestion performance and concurrency behavior.

The project includes a built-in benchmark runner that:
- generates test events
- ingests them through service + DB
- prints duration + throughput
- exits automatically after completion

---

## Quick Start

### 1) Start PostgreSQL
```bash
docker compose up -d
```

### 2) Run benchmark mode
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--benchmark"
```

## Test Environment

Benchmark run environment:

CPU: 12th Gen Intel® Core™ i5-1240P

RAM: 8 GB

OS: Windows 64-bit

Java: 17

Database: PostgreSQL (Docker Compose)

## Benchmark Tests

### Test 1: 1000 Events (Single Batch)

Objective: Process 1000 events in < 1 second (mandatory requirement)

Latest Output:

```
--- Benchmark: 1000 Events (Single Batch) ---
Results:
  - Accepted: 1000
  - Deduped: 0
  - Updated: 0
  - Rejected: 0
  - Duration: 84.065 ms (0.084 sec)
  - Throughput: 11896 events/sec
  - Status: ✅ PASS (< 1 sec)
```

✅ Meets the required criteria: < 1 sec

### Test 2: 5000 Events (Large Batch)

Objective: Validate scalability beyond minimum requirement

Latest Output:

```
--- Benchmark: 5000 Events (Single Batch) ---
Results:
  - Accepted: 5000
  - Deduped: 0
  - Updated: 0
  - Rejected: 0
  - Duration: 343.715 ms (0.344 sec)
  - Throughput: 14547 events/sec
```

### Test 3: Concurrent Ingestion (5 threads × 200 events with overlap)

Objective: Validate thread safety + correctness under concurrent ingestion

Latest Output:

```
--- Benchmark: Concurrent Ingestion (5 threads × 200 events, with overlap) ---
Thread Durations:
  - Thread 0: 69.503 ms
  - Thread 1: 78.644 ms
  - Thread 2: 39.577 ms
  - Thread 3: 49.214 ms
  - Thread 4: 51.782 ms
Total Duration: 88.951 ms (0.089 sec)
Throughput: 11242 events/sec
```

✅ Confirms stable performance and correctness under concurrency.

## Why this is fast (Design Summary)

Instead of per-event:

SELECT existing record

then INSERT/UPDATE

The ingestion is optimized using:

Prefetch existing eventIds using event_id IN (...)

JDBC batch UPSERT (JdbcTemplate.batchUpdate)

PostgreSQL atomic conflict handling:

```sql
INSERT INTO events(...)
VALUES(...)
ON CONFLICT (event_id)
DO UPDATE SET ...
WHERE
  events.payload_hash <> EXCLUDED.payload_hash
  AND EXCLUDED.received_time > events.received_time;
```

This ensures:

minimal DB round trips

correctness under race conditions

high throughput even under concurrent ingestion

## Success Criteria

| Test | Target | Status |
|------|--------|--------|
| 1000 Events | < 1 second | ✅ PASS |
| 5000 Events | stable performance | ✅ PASS |
| Concurrent ingestion | no duplicates, stable upsert | ✅ PASS |

## Screenshots

![1000 Events Benchmark](docs/images/benchmark-1000.png)

![5000 Events Benchmark](docs/images/benchmark-5000.png)

![Concurrent Benchmark](docs/images/benchmark-concurrent.png)