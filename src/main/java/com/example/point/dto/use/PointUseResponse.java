package com.example.point.dto.use;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointUseResponse {
    private Long userId;
    private String orderId;
    private int usedAmount;
    private int totalRemaining;
    private LocalDateTime transactionDate;

    public static PointUseResponse from(Long userId, String orderId, int usedAmount, int newTotalRemaining) {
        return PointUseResponse.builder()
                .userId(userId)
                .orderId(orderId)
                .usedAmount(usedAmount)
                .totalRemaining(newTotalRemaining)
                .transactionDate(LocalDateTime.now())
                .build();
    }
}
