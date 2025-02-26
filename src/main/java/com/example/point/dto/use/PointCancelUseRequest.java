package com.example.point.dto.use;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointCancelUseRequest {
    private Long userId;
    private String orderId;
    private int cancelAmount;
}
