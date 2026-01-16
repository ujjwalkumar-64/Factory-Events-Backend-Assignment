package com.assignment.buyogo_backend_assignment.benchmark;

import com.assignment.buyogo_backend_assignment.request.EventRequest;
import com.assignment.buyogo_backend_assignment.response.BatchResponse;
import com.assignment.buyogo_backend_assignment.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class BenchmarkRunner implements CommandLineRunner {
    private final EventService eventService;
    private final ApplicationContext applicationContext;

    @Override
    public void run(String... args) throws Exception {
        if (!hasArg(args, "--benchmark")) return;

        log.info("================================================================================");
        log.info("BENCHMARK: Factory Events Ingestion Performance Test");
        log.info("Run with: mvn spring-boot:run -Dspring-boot.run.arguments=\"--benchmark\"");
        log.info("================================================================================");

        warmup();

        runBenchmarkSingleBatch(1000, "BENCH-1K");
        runBenchmarkSingleBatch(5000, "BENCH-5K");

        // Concurrency benchmark: contention + overlap (tests thread-safety logic)
        runConcurrentBenchmarkWithOverlap();

        log.info("================================================================================");
        log.info("BENCHMARK COMPLETE");
        log.info("================================================================================");

        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }

    private boolean hasArg(String[] args, String expected) {
        if (args == null) return false;
        for (String arg : args) {
            if (expected.equals(arg)) return true;
        }
        return false;
    }

    /**
     * Warmup avoids first-run overhead:
     * Hibernate init, connection pool, JIT compilation, caches, etc.
     */
    private void warmup() {
        log.info("\n--- Warmup run (200 events) ---");
        List<EventRequest> warmupEvents = generateEvents(200, "WARMUP", false);

        long start = System.nanoTime();
        eventService.processBatchEvents(warmupEvents);
        long end = System.nanoTime();

        log.info("Warmup duration: {} ms", fmtMs(end - start));
    }

    private void runBenchmarkSingleBatch(int count, String prefix) {
        log.info("\n--- Benchmark: {} Events (Single Batch) ---", count);

        List<EventRequest> events = generateEvents(count, prefix, false);

        long startNs = System.nanoTime();
        BatchResponse response = eventService.processBatchEvents(events);
        long endNs = System.nanoTime();

        double ms = (endNs - startNs) / 1_000_000.0;
        double sec = ms / 1000.0;

        log.info("Results:");
        log.info("  - Accepted: {}", response.getAccepted());
        log.info("  - Deduped: {}", response.getDeduped());
        log.info("  - Updated: {}", response.getUpdated());
        log.info("  - Rejected: {}", response.getRejected());
        log.info("  - Duration: {} ms ({} sec)", String.format("%.3f", ms), String.format("%.3f", sec));
        log.info("  - Throughput: {} events/sec", String.format("%.0f", count / Math.max(sec, 0.000001)));

        if (count == 1000) {
            log.info("  - Status: {}", sec < 1.0 ? "✅ PASS (< 1 sec)" : "❌ FAIL (>= 1 sec)");
        }
    }

    /**
     * Concurrency benchmark:
     * 5 threads × 200 events = 1000 total.
     * ~20% of eventIds are shared across all threads to create contention.
     *
     * This better simulates "multiple sensors push concurrently" on same ids.
     */
    private void runConcurrentBenchmarkWithOverlap() throws Exception {
        log.info("\n--- Benchmark: Concurrent Ingestion (5 threads × 200 events, with overlap) ---");

        final int threads = 5;
        final int perThread = 200;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<Long>> futures = new ArrayList<>();

        long startTotal = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;

            futures.add(executor.submit(() -> {
                List<EventRequest> events = generateEvents(perThread, "THREAD-" + threadId, true);

                long start = System.nanoTime();
                eventService.processBatchEvents(events);
                long end = System.nanoTime();

                return (end - start);
            }));
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        long endTotal = System.nanoTime();

        log.info("Thread Durations:");
        for (int i = 0; i < futures.size(); i++) {
            double ms = futures.get(i).get() / 1_000_000.0;
            log.info("  - Thread {}: {} ms", i, String.format("%.3f", ms));
        }

        double totalMs = (endTotal - startTotal) / 1_000_000.0;
        double totalSec = totalMs / 1000.0;

        log.info("Total Duration: {} ms ({} sec)",
                String.format("%.3f", totalMs),
                String.format("%.3f", totalSec));

        log.info("Throughput: {} events/sec",
                String.format("%.0f", (threads * perThread) / Math.max(totalSec, 0.000001)));
    }

    /**
     * Generates VALID events:
     * - eventTime always in the past (so it never violates future-time validation).
     * - overlapEventIds=true => every 5th event uses a common ID shared across threads.
     */
    private List<EventRequest> generateEvents(int count, String prefix, boolean overlapEventIds) {
        List<EventRequest> events = new ArrayList<>(count);

        Random random = new Random(42); // deterministic
        Instant baseTime = Instant.now().minus(3, ChronoUnit.HOURS);

        for (int i = 0; i < count; i++) {
            String eventId = overlapEventIds && i % 5 == 0
                    ? "COMMON-" + i
                    : prefix + "-" + i;

            Instant eventTime = baseTime.plus(i, ChronoUnit.SECONDS);

            events.add(new EventRequest(
                    eventId,
                    eventTime,
                    "M-" + (i % 10),
                    1000L + random.nextInt(5000),  // duration: 1s to 6s
                    random.nextInt(5),             // defectCount 0-4
                    "F01",
                    "L" + (i % 5)
            ));
        }

        return events;
    }

    private String fmtMs(long durationNs) {
        return String.format("%.3f", durationNs / 1_000_000.0);
    }
}
