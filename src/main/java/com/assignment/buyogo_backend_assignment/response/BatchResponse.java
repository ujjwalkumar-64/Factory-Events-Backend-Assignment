package com.assignment.buyogo_backend_assignment.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchResponse
{
    private int accepted;
    private int deduped;
    private int updated;
    private int rejected;
    private List<RejectionDetail> rejections;
}
