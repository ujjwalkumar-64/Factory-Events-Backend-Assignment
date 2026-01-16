package com.assignment.buyogo_backend_assignment.service;

import com.assignment.buyogo_backend_assignment.request.EventRequest;
import com.assignment.buyogo_backend_assignment.request.StatsRequest;
import com.assignment.buyogo_backend_assignment.response.BatchResponse;
import com.assignment.buyogo_backend_assignment.response.StatsResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public interface EventService {
    BatchResponse processBatchEvents(List<EventRequest> eventRequest);
    StatsResponse getStats(StatsRequest statsRequest);
}
