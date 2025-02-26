package com.example.point.dto.use;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointUseRequest {
    private int usageAmount;
    private String orderId;
}
