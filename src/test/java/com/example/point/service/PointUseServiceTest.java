package com.example.point.service;

import com.example.point.domain.PointBalance;
import com.example.point.domain.PointTransaction;
import com.example.point.domain.enums.BalanceStatus;
import com.example.point.domain.enums.BalanceType;
import com.example.point.domain.enums.TransactionType;
import com.example.point.dto.use.PointCancelUseRequest;
import com.example.point.dto.use.PointUseRequest;
import com.example.point.dto.use.PointUseResponse;
import com.example.point.exception.PointOperationException;
import com.example.point.repository.PointBalanceRepository;
import com.example.point.repository.PointTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointUseServiceTest {

    @Mock
    private PointBalanceRepository pointBalanceRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private PointPolicyService policyService;

    @InjectMocks
    private PointUseService pointUseService;

    @Test
    @DisplayName("포인트 사용 - 잔액 충분하여 정상 차감")
    void testUsePoints_Success() {
        // Arrange
        Long userId = 1L;
        PointUseRequest request = PointUseRequest.builder()
                .usageAmount(1200)
                .orderId("ORDER-1234")
                .build();

        // 사용 가능한 포인트 잔액 2건: balance1(1000 remain), balance2(500 remain)
        PointBalance balance1 = PointBalance.builder()
                .balanceId(1L)
                .userId(userId)
                .amount(1000)
                .remainAmount(1000)
                .balanceType(BalanceType.PURCHASE)
                .expireDate(LocalDateTime.now().plusDays(30))
                .status(BalanceStatus.ACTIVE)
                .build();

        PointBalance balance2 = PointBalance.builder()
                .balanceId(2L)
                .userId(userId)
                .amount(500)
                .remainAmount(500)
                .balanceType(BalanceType.PURCHASE)
                .expireDate(LocalDateTime.now().plusDays(40))
                .status(BalanceStatus.ACTIVE)
                .build();

        when(pointBalanceRepository.findByUserIdAndStatusOrderByBalanceTypeAscExpireDateAsc(userId, BalanceStatus.ACTIVE))
                .thenReturn(Arrays.asList(balance1, balance2));

        // Act
        PointUseResponse response = pointUseService.use(userId, request);

        // Assert
        // 1) 첫 번째 잔액에서 1000 차감, 두 번째 잔액에서 200 차감 → 총 1200 사용
        assertEquals(1200, response.getUsedAmount());
        // 2) 첫 번째 잔액 remainAmount = 0, 두 번째 잔액 remainAmount = 300
        assertEquals(0, balance1.getRemainAmount());
        assertEquals(300, balance2.getRemainAmount());
    }

    @Test
    @DisplayName("포인트 사용 - 잔액 부족으로 예외 발생")
    void testUsePoints_InsufficientBalance() {
        // Arrange
        Long userId = 1L;
        PointUseRequest request = PointUseRequest.builder()
                .usageAmount(1500)
                .orderId("ORDER-9999")
                .build();

        // 사용 가능한 포인트 잔액이 총 1000밖에 없음
        PointBalance balance = PointBalance.builder()
                .balanceId(1L)
                .userId(userId)
                .amount(1000)
                .remainAmount(1000)
                .balanceType(BalanceType.PURCHASE)
                .expireDate(LocalDateTime.now().plusDays(30))
                .status(BalanceStatus.ACTIVE)
                .build();

        when(pointBalanceRepository.findByUserIdAndStatusOrderByBalanceTypeAscExpireDateAsc(userId, BalanceStatus.ACTIVE))
                .thenReturn(Collections.singletonList(balance));

        // Act & Assert
        assertThrows(PointOperationException.class, () -> pointUseService.use(userId, request));
    }

    @Test
    @DisplayName("포인트 사용 취소 - 정상 취소")
    void testCancelUse_Success() {
        // Arrange
        PointCancelUseRequest request = PointCancelUseRequest.builder()
                .userId(1L)
                .orderId("ORDER-ABC")
                .cancelAmount(300)
                .build();

        // 기존에 -500 포인트를 사용한 트랜잭션이 1건 있다고 가정
        PointTransaction usageTx = PointTransaction.builder()
                .transactionId(10L)
                .balanceId(1L)
                .userId(1L)
                .transactionType(TransactionType.USE)
                .pointChange(-500) // 500 사용
                .transactionDate(LocalDateTime.now().minusMinutes(5))
                .orderId("ORDER-ABC")
                .build();

        when(pointTransactionRepository.findByUserIdAndOrderIdAndTransactionType(
                1L, "ORDER-ABC", TransactionType.USE))
                .thenReturn(Collections.singletonList(usageTx));

        // 사용된 포인트가 500이므로 300 취소 가능
        PointBalance balance = PointBalance.builder()
                .balanceId(1L)
                .userId(1L)
                .amount(500)
                .remainAmount(0) // 이미 전부 사용했다고 가정
                .balanceType(BalanceType.PURCHASE)
                .expireDate(LocalDateTime.now().plusDays(10))
                .status(BalanceStatus.ACTIVE)
                .build();

        when(pointBalanceRepository.findById(1L))
                .thenReturn(Optional.of(balance));

        // Act
        PointUseResponse response = pointUseService.cancelUse(request);

        // Assert
        // 1) 300 포인트가 복구되었으므로 remainAmount는 300이 됨
        assertEquals(300, balance.getRemainAmount());
        // 2) 총 300 포인트 취소
        assertEquals(300, response.getUsedAmount());
    }

    @Test
    @DisplayName("포인트 사용 취소 - 주문 사용 이력 없음")
    void testCancelUse_NoUsageTransactions() {
        // Arrange
        PointCancelUseRequest request = PointCancelUseRequest.builder()
                .userId(2L)
                .orderId("ORDER-XYZ")
                .cancelAmount(100)
                .build();

        // 해당 주문에 대한 USE 트랜잭션이 없다고 가정
        when(pointTransactionRepository.findByUserIdAndOrderIdAndTransactionType(
                2L, "ORDER-XYZ", TransactionType.USE))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(PointOperationException.class, () -> pointUseService.cancelUse(request));
    }

    @Test
    @DisplayName("포인트 사용 취소 - 취소 요청 금액이 사용된 금액보다 많음")
    void testCancelUse_ExceedUsedAmount() {
        // Arrange
        PointCancelUseRequest request = PointCancelUseRequest.builder()
                .userId(1L)
                .orderId("ORDER-ABC")
                .cancelAmount(600)
                .build();

        // 기존에 -500 포인트를 사용한 트랜잭션이 있다고 가정
        PointTransaction usageTx = PointTransaction.builder()
                .transactionId(10L)
                .balanceId(1L)
                .userId(1L)
                .transactionType(TransactionType.USE)
                .pointChange(-500)
                .transactionDate(LocalDateTime.now().minusMinutes(5))
                .orderId("ORDER-ABC")
                .build();

        when(pointTransactionRepository.findByUserIdAndOrderIdAndTransactionType(
                1L, "ORDER-ABC", TransactionType.USE))
                .thenReturn(Collections.singletonList(usageTx));

        // Act & Assert
        // 취소 금액이 500보다 크므로 예외 발생
        assertThrows(PointOperationException.class, () -> pointUseService.cancelUse(request));
    }

    @Test
    @DisplayName("포인트 사용 취소 - 만료된 잔액은 신규 적립, 미만료 잔액은 환불 적용")
    void testCancelUse_ExpiredAndNotExpiredCombination() {
        // Arrange
        Long userId = 1L;
        String orderId = "ORDER-C";
        int cancelAmount = 1100;

        PointCancelUseRequest request = PointCancelUseRequest.builder()
                .userId(userId)
                .orderId(orderId)
                .cancelAmount(cancelAmount)
                .build();

        // 사용 내역: A에서 1000포인트 사용, B에서 200포인트 사용 (총 1200 사용됨)
        PointTransaction txA = PointTransaction.builder()
                .transactionId(1L)
                .balanceId(1L)
                .userId(userId)
                .transactionType(TransactionType.USE)
                .pointChange(-1000)
                .transactionDate(LocalDateTime.now().minusMinutes(10))
                .orderId(orderId)
                .build();
        PointTransaction txB = PointTransaction.builder()
                .transactionId(2L)
                .balanceId(2L)
                .userId(userId)
                .transactionType(TransactionType.USE)
                .pointChange(-200)
                .transactionDate(LocalDateTime.now().minusMinutes(5))
                .orderId(orderId)
                .build();

        when(pointTransactionRepository.findByUserIdAndOrderIdAndTransactionType(
                userId, orderId, TransactionType.USE))
                .thenReturn(Arrays.asList(txA, txB));

        // Balance A: 만료됨 (expireDate가 과거, remainAmount 0)
        PointBalance balanceA = PointBalance.builder()
                .balanceId(1L)
                .userId(userId)
                .amount(1000)
                .remainAmount(0)
                .balanceType(BalanceType.PURCHASE)
                .expireDate(LocalDateTime.now().minusDays(1))
                .status(BalanceStatus.ACTIVE)
                .build();
        // Balance B: 미만료 (remainAmount 300, 원래 500 중 200 사용됨)
        PointBalance balanceB = PointBalance.builder()
                .balanceId(2L)
                .userId(userId)
                .amount(500)
                .remainAmount(300)
                .balanceType(BalanceType.PURCHASE)
                .expireDate(LocalDateTime.now().plusDays(10))
                .status(BalanceStatus.ACTIVE)
                .build();

        when(pointBalanceRepository.findById(1L)).thenReturn(Optional.of(balanceA));
        when(pointBalanceRepository.findById(2L)).thenReturn(Optional.of(balanceB));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpireDate = now.plusDays(365);
        when(policyService.calculateExpireDate(any(LocalDateTime.class), eq(null)))
                .thenReturn(newExpireDate);

        when(pointBalanceRepository.findByUserIdAndStatusOrderByBalanceTypeAscExpireDateAsc(userId, BalanceStatus.ACTIVE))
                .thenReturn(Arrays.asList(
                        // 만료된 Balance A는 신규 적립
                        PointBalance.builder()
                                .balanceId(3L)
                                .userId(userId)
                                .amount(1000)
                                .remainAmount(1000)
                                .balanceType(BalanceType.PURCHASE)
                                .expireDate(newExpireDate)
                                .status(BalanceStatus.ACTIVE)
                                .build(),
                        balanceB
                ));

        // Act
        PointUseResponse response = pointUseService.cancelUse(request);

        // Assert
        // - Balance A는 만료되어 신규 적립되어 1000 포인트가 적립됨.
        // - Balance B는 환불되어 remainAmount가 300 -> 400이 됨.
        // - 전체 취소 금액은 1100, 전체 남은 포인트는 1000 + 400 = 1400 이어야 함.
        assertEquals(1100, response.getUsedAmount(), "취소된 포인트 금액이 1100이어야 함");
        assertEquals(1400, response.getTotalRemaining(), "총 남은 포인트가 1400이어야 함");
        // 추가 검증: Balance B의 remainAmount가 400인지 확인
        assertEquals(400, balanceB.getRemainAmount(), "Balance B의 잔액이 400이어야 함");
    }

}
