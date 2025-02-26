package com.example.point.domain;

import com.example.point.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "point_transactions")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "balance_id")
    private Long balanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    // 거래로 인한 포인트 변화 (적립은 양수, 사용/취소는 음수)
    @Column(name = "point_change", nullable = false)
    private int pointChange;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "description")
    private String description;

    public static PointTransaction of(PointBalance balance,
                                      TransactionType transactionType,
                                      LocalDateTime transactionDate,
                                      String description,
                                      String orderId,
                                      int pointChange) {
        return PointTransaction.builder()
                .balanceId(balance.getBalanceId())
                .userId(balance.getUserId())
                .transactionType(transactionType)
                .transactionDate(transactionDate)
                .description(description)
                .orderId(orderId)
                .pointChange(pointChange)
                .build();
    }
}
