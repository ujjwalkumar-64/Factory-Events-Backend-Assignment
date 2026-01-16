package com.assignment.buyogo_backend_assignment.response;

import lombok.*;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class DefectLineResponse {
    private String lineId;
    private long totalDefects;
    private long eventCount;
    private double defectsPercent;
}
