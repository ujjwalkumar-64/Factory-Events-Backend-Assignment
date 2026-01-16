package com.assignment.buyogo_backend_assignment.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class RejectionDetail {
    private String eventId;
    private String reason;
}