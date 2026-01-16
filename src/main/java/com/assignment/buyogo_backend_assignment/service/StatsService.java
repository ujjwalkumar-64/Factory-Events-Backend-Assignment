package com.assignment.buyogo_backend_assignment.service;

import com.assignment.buyogo_backend_assignment.request.StatsRequest;
import com.assignment.buyogo_backend_assignment.response.DefectLineResponse;
import com.assignment.buyogo_backend_assignment.response.StatsResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public interface StatsService {
    StatsResponse getStats(StatsRequest statsRequest);
    List<DefectLineResponse> getDefectsLine(String factoryId, Instant from, Instant to, int limit);
}
