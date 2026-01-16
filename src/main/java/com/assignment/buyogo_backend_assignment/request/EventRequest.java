package com.assignment.buyogo_backend_assignment.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record EventRequest(

        @NotBlank(message = "event id is required")
         String eventId,

         @NotNull(message = "event time is required")
         @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" , timezone = "UTC")
         Instant eventTime,


         @NotBlank(message = "machine id is required")
         String machineId,
         @NotNull(message = "durationMs is required")
         Long durationMs,
         @NotNull(message = "defectCount is required")
         Integer defectCount,

          String factoryId ,

          String lineId

) {
}
