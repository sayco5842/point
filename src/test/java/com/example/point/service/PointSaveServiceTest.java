package com.example.point.service;

import com.example.point.domain.PointBalance;
import com.example.point.domain.enums.BalanceStatus;
import com.example.point.domain.enums.BalanceType;
import com.example.point.dto.save.PointSaveRequest;
import com.example.point.dto.save.PointSaveResponse;
import com.example.point.repository.PointBalanceRepository;
import com.example.point.repository.PointTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointSaveServiceTest {

    @Mock
    private PointBalanceRepository pointBalanceRepository;

    @Mock
    private PointPolicyService policyService;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @InjectMocks
    private PointSaveService pointSaveService;

    @Test
    @DisplayName("포인트 적립 - 정상 저장")
    void testSave_Success() {
        // Arrange
        Long userId = 1L;
        int amount = 1000;
        Integer expireDays = 365;

        PointSaveRequest request = PointSaveRequest.builder()
                .userId(userId)
                .amount(amount)
                .expireDays(expireDays)
                .build();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireDate = now.plusDays(expireDays);
        when(policyService.calculateExpireDate(any(LocalDateTime.class), eq(expireDays)))
                .thenReturn(expireDate);
        // 사용 가능한 잔액 조회 결과 (현재 ACTIVE한 잔액이 없다고 가정)
        when(pointBalanceRepository.findByUserIdAndStatusOrderByBalanceTypeAscExpireDateAsc(userId, BalanceStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        PointBalance savedBalance = PointBalance.builder()
                .balanceId(1L)
                .userId(userId)
                .amount(amount)
                .remainAmount(amount)
                .balanceType(BalanceType.PURCHASE)
                .createDate(now)
                .expireDate(expireDate)
                .status(BalanceStatus.ACTIVE)
                .build();
        when(pointBalanceRepository.save(any(PointBalance.class))).thenReturn(savedBalance);

        // Act
        PointSaveResponse response = pointSaveService.save(request);

        // Assert
        assertEquals(savedBalance.getBalanceId(), response.getBalanceId());
        assertEquals(savedBalance.getRemainAmount(), request.getAmount(), "요청금액이 잔액으로 적립되어야한다");
    }

    @Test
    @DisplayName("포인트 적립 취소 - 정상 취소")
    void testCancelSave_Success() {
        // Arrange
        Long balanceId = 1L;
        // 적립 내역: remainAmount가 amount와 동일한 상태여야 취소가 가능
        PointBalance balance = PointBalance.builder()
                .balanceId(balanceId)
                .userId(1L)
                .amount(1000)
                .remainAmount(1000)
                .balanceType(BalanceType.PURCHASE)
                .createDate(LocalDateTime.now().minusDays(1))
                .expireDate(LocalDateTime.now().plusDays(30))
                .status(BalanceStatus.ACTIVE)
                .build();
        when(pointBalanceRepository.findById(balanceId))
                .thenReturn(Optional.of(balance));

        // Act
        PointSaveResponse response = pointSaveService.cancelSave(balanceId);

        // Assert
        assertEquals(balanceId, response.getBalanceId());
        assertEquals(BalanceStatus.CANCELED, balance.getStatus(), "잔액 상태가 취소(CANCELED)되어야 한다");
    }
}
