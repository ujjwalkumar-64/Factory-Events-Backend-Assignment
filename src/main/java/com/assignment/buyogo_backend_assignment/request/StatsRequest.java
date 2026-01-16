package com.assignment.buyogo_backend_assignment.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record StatsRequest(
        @NotBlank(message = "machine id is required")
        String machineId,
        @NotNull(message = "start event time is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" , timezone = "UTC")
        Instant start,

        @NotNull(message = "end event time is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" , timezone = "UTC")
        Instant end
) {
}
