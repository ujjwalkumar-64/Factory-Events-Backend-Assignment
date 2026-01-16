package com.assignment.buyogo_backend_assignment;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
public class EventControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    private Map<String, Object> event(String id, Instant time, long duration, int defect) {
        return Map.of(
                "eventId", id,
                "eventTime", time.toString(),
                "machineId", "M-1",
                "durationMs", duration,
                "defectCount", defect,
                "factoryId", "F01",
                "lineId", "L1"
        );
    }

    @Test
    void shouldAcceptValidEvent() throws Exception {
        var payload = List.of(event("E-1", Instant.now().minusSeconds(60), 1000, 2));

        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.rejected").value(0));
    }

    @Test
    void shouldRejectFutureEventTime() throws Exception {
        var payload = List.of(event("E-2", Instant.now().plusSeconds(60 * 20), 1000, 2));

        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejected").value(1));
    }

    @Test
    void shouldRejectTooLargeDuration() throws Exception {
        // > 6 hours
        var payload = List.of(event("E-3", Instant.now().minusSeconds(60), 21_600_000L + 1, 2));

        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejected").value(1));
    }

    @Test
    void shouldDedupSameEventTwice() throws Exception {
        var payload = List.of(event("E-4", Instant.now().minusSeconds(60), 1000, 2));

        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deduped").value(1));
    }

    @Test
    void shouldUpdateIfPayloadChanges() throws Exception {
        Instant t = Instant.now().minusSeconds(60);

        var p1 = List.of(event("E-5", t, 1000, 1));
        var p2 = List.of(event("E-5", t, 1000, 9)); // changed defectCount

        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1));
    }

    @Test
    void shouldBeThreadSafeForSameEventId() throws Exception {
        String json = objectMapper.writeValueAsString(
                List.of(event("CONC-1", Instant.now().minusSeconds(120), 1000, 2))
        );

        int threads = 10;
        ExecutorService es = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            es.submit(() -> {
                try {
                    mockMvc.perform(post("/api/v1/events/batch")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                            .andExpect(status().isOk());
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        es.shutdownNow();

        // If thread-safe: subsequent insert should be deduped (exists only once)
        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deduped").value(1));
    }
}