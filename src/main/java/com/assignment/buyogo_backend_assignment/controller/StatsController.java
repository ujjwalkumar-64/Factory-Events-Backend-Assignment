package com.assignment.buyogo_backend_assignment.controller;

import com.assignment.buyogo_backend_assignment.request.StatsRequest;
import com.assignment.buyogo_backend_assignment.response.DefectLineResponse;
import com.assignment.buyogo_backend_assignment.response.StatsResponse;
import com.assignment.buyogo_backend_assignment.service.StatsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@AllArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping()
    public ResponseEntity<StatsResponse> getStats(
            @RequestParam @NotBlank(message = "machine id is required") String machineId,
            @RequestParam @NotNull(message = "start event time is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @NotNull(message = "end event time is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    )

    {
            StatsRequest statsRequest = new StatsRequest(machineId, start, end);
            StatsResponse statsResponse = statsService.getStats(statsRequest);
            return ResponseEntity.ok(statsResponse);

    }

    @GetMapping("/top-defect-lines")
    public ResponseEntity<List<DefectLineResponse>> getTopDefectLines(
            @RequestParam @NotBlank(message = "factory id is required") String factoryId,
            @RequestParam @NotNull(message = "start event time is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @NotNull(message = "end event time is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,

            @RequestParam(defaultValue = "10") int limit
    ) {
        List<DefectLineResponse> topLines = statsService.getDefectsLine(factoryId, from, to, limit);
        return ResponseEntity.ok(topLines);
    }
}
