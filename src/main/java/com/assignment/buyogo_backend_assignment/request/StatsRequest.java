package com.assignment.buyogo_backend_assignment.request;

import java.time.Instant;

public record StatsRequest(
        String machineId,
        Instant start,
        Instant end
) {
}
