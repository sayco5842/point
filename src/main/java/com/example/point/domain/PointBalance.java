package com.example.point.domain;

import com.example.point.domain.enums.BalanceStatus;
import com.example.point.domain.enums.BalanceType;
import com.example.point.dto.save.PointSaveRequest;
import com.example.point.exception.PointOperationException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "point_balances")
public class PointBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long balanceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int amount;

    @Column(name = "remain_amount", nullable = false)
    private int remainAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_type", nullable = false)
    private BalanceType balanceType;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "expire_date", nullable = false)
    private LocalDateTime expireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BalanceStatus status;

    public static PointBalance of(PointSaveRequest request, LocalDateTime expireDate) {
        return PointBalance.builder()
                .userId(request.getUserId())
                .amount(request.getAmount())
                .remainAmount(request.getAmount())
                .balanceType(BalanceType.PURCHASE)
                .createDate(LocalDateTime.now())
                .expireDate(expireDate)
                .status(BalanceStatus.ACTIVE)
                .build();
    }

    public void cancel() {
        if (this.remainAmount != this.amount) {
            throw new PointOperationException("일부 사용된 포인트는 취소할 수 없습니다.");
        }
        this.status = BalanceStatus.CANCELED;
    }

    public void deductPoints(int points) {
        if (points > this.remainAmount) {
            throw new PointOperationException("차감할 포인트가 남은 포인트보다 많습니다.");
        }
        this.remainAmount -= points;
    }

    public void refund(int refundAmount) {
        this.remainAmount += refundAmount;
    }

    public boolean isExpired(LocalDateTime now) {
        return this.expireDate.isBefore(now);
    }
}
