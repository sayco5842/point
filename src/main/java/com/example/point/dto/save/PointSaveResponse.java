package com.example.point.dto.save;

import com.example.point.domain.enums.BalanceStatus;
import com.example.point.domain.enums.BalanceType;
import com.example.point.domain.PointBalance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PointSaveResponse {
    private Long balanceId;
    private Long userId;
    private int amount;
    private int remainAmount;
    private BalanceType balanceType;
    private LocalDateTime createDate;
    private LocalDateTime expireDate;
    private BalanceStatus status;

    public static PointSaveResponse from(PointBalance balance) {
        return new PointSaveResponse(
                balance.getBalanceId(),
                balance.getUserId(),
                balance.getAmount(),
                balance.getRemainAmount(),
                balance.getBalanceType(),
                balance.getCreateDate(),
                balance.getExpireDate(),
                balance.getStatus()
        );
    }
}
