package com.assignment.buyogo_backend_assignment.response;

import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class StatsResponse {
    private String machineId;
    private Instant start;
    private Instant end;
    private long eventsCount;
    private long defectsCount;
    private double avgDefectRate;
    private Status status;

}


