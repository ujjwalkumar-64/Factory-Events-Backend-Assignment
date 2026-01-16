# Factory Events Backend Assignment (Buyogo)

A high-throughput backend service for ingesting factory telemetry events in batches, supporting **deduplication**, **conditional updates**, and **analytics queries** on stored events.

This project focuses on:
- Correctness under concurrency (thread-safe dedupe/update)
- High ingestion performance (1000 events < 1 sec)
- Query endpoints for stats and defect analytics
- Documentation + Swagger

---

## ✅ Tech Stack
- Java 17
- Spring Boot
- PostgreSQL
- Spring Data JPA (queries) + JDBC Batch (fast ingestion)
- Swagger OpenAPI (`springdoc-openapi`)

---

## 1) Architecture

### High-level components
Client/Sensor ---> REST API (Spring Boot Controllers)
|
v
Service Layer
(validation + hashing +
dedupe/update policy)
|
v
Repository Layer (DB interaction)
- JPA for fetch/stat queries
- JDBC batch for high throughput UPSERT
|
v
PostgreSQL Database


### Why this architecture?
- **Controller Layer**: receives HTTP requests, maps JSON to DTOs.
- **Service Layer**: contains all business rules:
  - event validation
  - payload hashing
  - dedupe/update decisions
  - bulk ingestion
- **Repository Layer**:
  - `EventRepository` (JPA) for querying / stats
  - `EventBulkRepository` (JdbcTemplate) for bulk upsert
- **Database**:
  - PostgreSQL is used because the assignment requires strong UPSERT/constraint semantics and concurrency correctness.

---

## 2) Data Model

Each event contains:
- `eventId` (unique id, used for dedupe/update)
- `eventTime` (time when event occurred)
- `receivedTime` (set by backend when request is processed)
- `machineId`
- `durationMs`
- `defectCount`
- `factoryId`
- `lineId`
- `payloadHash` (computed by backend from payload fields)

### Unique constraint
`event_id` is unique in DB.

This ensures:
- no duplicates
- correct concurrent behavior

---

## 3) Dedupe / Update Logic (Core Requirement)

### What is dedupe?
If the same `eventId` arrives multiple times with the same payload, it should not create multiple DB rows. It should be counted as **deduped**.

### What is an update?
If the same `eventId` arrives again but with a different payload, we update the stored event **only if** the new event is the latest one.

---

### 3.1 How payloads are compared (Payload Hash)

Instead of comparing multiple fields directly every time, the service computes:

`payloadHash = SHA-256(machineId + durationMs + defectCount + factoryId + lineId + eventTime)`

So:
- Same payload → same hash
- Different payload → different hash

This makes dedupe comparison extremely fast:
- compare `payloadHash` strings, not multiple fields

---

### 3.2 Who wins? ("winning record" decision)

Each incoming request does NOT provide `receivedTime`.
Backend sets it using:

`receivedTime = Instant.now()`

This means the backend controls ordering.

**Winning record rule:**
If two events have same `eventId` but different payload:
- The event with **newer receivedTime** wins
- Older events are ignored (counted as deduped)

---

### 3.3 Atomic + Thread-safe implementation in PostgreSQL

This logic is enforced at DB-level using atomic UPSERT:

```sql
INSERT INTO events(...)
VALUES (...)
ON CONFLICT (event_id)
DO UPDATE SET ...
WHERE
  events.payload_hash <> EXCLUDED.payload_hash
  AND EXCLUDED.received_time > events.received_time;

This guarantees:
✅ no duplicates
✅ correct update ordering under concurrency
✅ safe even when multiple threads ingest same eventId simultaneously

This is critical because in distributed environments multiple sensors/clients may send the same event concurrently.

4) Validation Rules

For each event:

Reject if eventTime is > 15 minutes in the future

Reject if durationMs < 0 or durationMs > 6 hours

Invalid events are counted under rejected with rejection reasons.

5) API Endpoints
5.1 Batch ingest events

POST /api/v1/events/batch

Accepts JSON array of events.

Response includes:

accepted

deduped

updated

rejected

rejections[]

5.2 Machine Stats

GET /api/v1/stats?machineId=...&start=...&end=...

Returns metrics for a machine in time window:

event count

total defects (ignores defectCount = -1)

average defect rate

health status

5.3 Top Defect Lines

GET /api/v1/stats/top-defect-lines?factoryId=...&start=...&end=...&limit=...

Returns list ordered by total defects:

lineId

eventCount

totalDefects

defectsPercent

6) Swagger API Documentation

After running the application:

http://localhost:8092/swagger-ui/index.html

7) How to Run
7.1 Start PostgreSQL (Docker)
docker compose up -d

7.2 Run application
./mvnw spring-boot:run

8) Benchmark (Performance Requirement)

Benchmark runner is integrated and can be executed using:

./mvnw spring-boot:run -Dspring-boot.run.arguments="--benchmark"

Latest benchmark results

✅ 1000 Events (Single Batch): 332.508 ms (0.333 sec)
✅ 5000 Events (Single Batch): 504.904 ms (0.505 sec)
✅ Concurrent (5×200 overlap): 155.567 ms (0.156 sec)

This meets and exceeds the requirement:
1000 events ingested in under 1 second

How performance was achieved

No per-event DB queries

Prefetch existing events with event_id IN (...)

JDBC batch UPSERT to minimize DB round-trips

Atomic conflict resolution inside PostgreSQL

9) Notes

defectCount = -1 is ignored in defect aggregation queries, as specified in assignment.

Dedup/update correctness is enforced at DB level via atomic UPSERT, ensuring thread safety.

10) Tests

Test suite (integration tests with PostgreSQL/Testcontainers) will be added to validate:

validation rules

dedupe/update correctness

query correctness

concurrency safety


---

# ✅ What to do now (fast checklist)
1. Create `README.md` in repo root  
2. Paste above content  
3. Commit + push

---

If you want, I can also provide **BENCHMARK.md** and **DESIGN.md** quickly (extra bonus), but README alone is mandatory and this one is fully compliant and strong.
::contentReference[oaicite:0]{index=0}
