package com.assignment.buyogo_backend_assignment.controller;

import com.assignment.buyogo_backend_assignment.request.EventRequest;
import com.assignment.buyogo_backend_assignment.response.BatchResponse;
import com.assignment.buyogo_backend_assignment.service.EventService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@AllArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping("/batch")
    ResponseEntity<BatchResponse> processBatch(
            @Valid @RequestBody List<EventRequest> eventRequestList
    ){
        BatchResponse response= eventService.processBatchEvents(eventRequestList);
        return ResponseEntity.ok(response);
    }
}
