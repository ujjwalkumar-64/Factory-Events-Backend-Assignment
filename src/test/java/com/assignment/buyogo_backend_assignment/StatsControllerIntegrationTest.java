package com.assignment.buyogo_backend_assignment;


import com.assignment.buyogo_backend_assignment.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class StatsControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void cleanup() {
        eventRepository.deleteAll();
    }


    private Map<String, Object> event(String id, Instant time, int defect, String lineId) {
        return Map.of(
                "eventId", id,
                "eventTime", time.toString(),
                "machineId", "M-1",
                "durationMs", 1000L,
                "defectCount", defect,
                "factoryId", "F01",
                "lineId", lineId
        );
    }

    @Test
    void statsShouldIgnoreMinusOneDefects() throws Exception {
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        var payload = List.of(
                event("S-1", start.plusSeconds(10), 2, "L1"),
                event("S-2", start.plusSeconds(20), -1, "L1"), // ignored
                event("S-3", start.plusSeconds(30), 3, "L1")
        );

        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/stats")
                        .param("machineId", "M-1")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventsCount").value(3))
                .andExpect(jsonPath("$.defectsCount").value(5));
    }

    @Test
    void topDefectLinesShouldReturnSorted() throws Exception {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        var payload = List.of(
                event("T-1", from.plusSeconds(10), 2, "L1"),
                event("T-2", from.plusSeconds(20), 5, "L2"),
                event("T-3", from.plusSeconds(30), 1, "L1"),
                event("T-4", from.plusSeconds(40), -1, "L2")
        );

        mockMvc.perform(post("/api/v1/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/stats/top-defect-lines")
                        .param("factoryId", "F01")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lineId").value("L2"))
                .andExpect(jsonPath("$[0].totalDefects").value(5));
    }
}
