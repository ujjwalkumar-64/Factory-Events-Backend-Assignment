package com.assignment.buyogo_backend_assignment.controller;

import com.assignment.buyogo_backend_assignment.request.StatsRequest;
import com.assignment.buyogo_backend_assignment.response.StatsResponse;
import com.assignment.buyogo_backend_assignment.service.EventService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/stats")
@AllArgsConstructor
public class StatsController {
    private final EventService eventService;

    @GetMapping()
    public ResponseEntity<StatsResponse> getStats(
            @RequestParam String machineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end)
    {
            StatsRequest statsRequest = new StatsRequest(machineId, start, end);
            StatsResponse statsResponse = eventService.getStats(statsRequest);
            return ResponseEntity.ok(statsResponse);

    }
}
