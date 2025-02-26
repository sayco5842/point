package com.example.point.dto.save;

import lombok.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointSaveRequest {
    private Long userId;
    private int amount;
    private Integer expireDays;
}
