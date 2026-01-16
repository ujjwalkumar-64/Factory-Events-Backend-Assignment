package com.assignment.buyogo_backend_assignment.service;

import com.assignment.buyogo_backend_assignment.request.StatsRequest;
import com.assignment.buyogo_backend_assignment.response.StatsResponse;
import org.springframework.stereotype.Service;

@Service
public interface StatsService {
    StatsResponse getStats(StatsRequest statsRequest);
}
