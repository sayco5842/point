package com.example.point.service;

import com.example.point.domain.PointBalance;
import com.example.point.domain.PointTransaction;
import com.example.point.domain.enums.BalanceStatus;
import com.example.point.domain.enums.TransactionType;
import com.example.point.dto.use.PointCancelUseRequest;
import com.example.point.dto.use.PointUseRequest;
import com.example.point.dto.use.PointUseResponse;
import com.example.point.exception.PointOperationException;
import com.example.point.repository.PointBalanceRepository;
import com.example.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointUseService {

    private final PointBalanceRepository pointBalanceRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointPolicyService policyService;

    @Transactional
    public PointUseResponse use(Long userId, PointUseRequest request) {
        LocalDateTime now = LocalDateTime.now();
        List<PointBalance> balances = pointBalanceRepository
                .findByUserIdAndStatusOrderByBalanceTypeAscExpireDateAsc(userId, BalanceStatus.ACTIVE);

        // 요청한 사용 금액
        int remainingToUse = request.getUsageAmount();
        int totalUsed = 0;

        List<PointTransaction> transactions = new ArrayList<>();

        // 잔액 목록을 순회하며 포인트를 차감
        for (PointBalance balance : balances) {
            if (remainingToUse <= 0) {
                break;
            }

            // 현재 잔액에서 차감할 수 있는 금액(남은 사용액과 잔액 중 최소값)
            int deduct = Math.min(balance.getRemainAmount(), remainingToUse);

            // 실제 잔액 차감
            balance.deductPoints(deduct);

            // 트랜잭션(사용)이므로 차감 금액은 음수로 기록
            int pointChange = -deduct;
            PointTransaction transaction = PointTransaction.of(
                    balance,
                    TransactionType.USE,
                    now,  // 트랜잭션 발생 시각
                    "포인트 사용: 주문번호 " + request.getOrderId(),
                    request.getOrderId(),
                    pointChange
            );
            transactions.add(transaction);

            // 남은 사용액과 사용 총액 업데이트
            remainingToUse -= deduct;
            totalUsed += deduct;
        }

        // 남은 사용액이 0보다 크다면, 잔액 부족으로 예외 처리
        if (remainingToUse > 0) {
            throw new PointOperationException("사용할 포인트가 잔액을 초과합니다.");
        }

        pointBalanceRepository.saveAll(balances);
        pointTransactionRepository.saveAll(transactions);

        int totalRemaining = calculateTotalRemainingPoints(userId);
        return PointUseResponse.from(userId, request.getOrderId(), totalUsed, totalRemaining);
    }



    @Transactional
    public PointUseResponse cancelUse(PointCancelUseRequest request) {
        List<PointTransaction> usageTransactions = pointTransactionRepository
                .findByUserIdAndOrderIdAndTransactionType(request.getUserId(), request.getOrderId(), TransactionType.USE);

        if (usageTransactions.isEmpty()) {
            throw new PointOperationException("해당 주문의 사용 거래가 존재하지 않습니다.");
        }

        int totalUsed = usageTransactions.stream().mapToInt(tx -> -tx.getPointChange()).sum();

        if (request.getCancelAmount() > totalUsed) {
            throw new PointOperationException("취소할 포인트가 원래 사용된 포인트보다 많습니다.");
        }

        int remainingToCancel = request.getCancelAmount();
        int totalCanceled = 0;
        LocalDateTime now = LocalDateTime.now();
        List<PointTransaction> transactions = new ArrayList<>();
        List<PointBalance> updatedBalances = new ArrayList<>();

        for (PointTransaction tx : usageTransactions) {
            if (remainingToCancel <= 0) break;
            int usedInTx = -tx.getPointChange();
            int cancelAmount = Math.min(usedInTx, remainingToCancel);
            PointBalance balance = pointBalanceRepository.findById(tx.getBalanceId())
                    .orElseThrow(() -> new PointOperationException("연결된 적립 내역이 존재하지 않습니다."));

            if (balance.isExpired(now)) {
                PointBalance newBalance = PointBalance.builder()
                        .userId(balance.getUserId())
                        .amount(cancelAmount)
                        .remainAmount(cancelAmount)
                        .balanceType(balance.getBalanceType())
                        .createDate(now)
                        .expireDate(policyService.calculateExpireDate(now, null))
                        .status(BalanceStatus.ACTIVE)
                        .build();
                updatedBalances.add(newBalance);
                transactions.add(PointTransaction.of(newBalance, TransactionType.CANCEL_USE, now, "만료된 포인트 사용 취소 → 신규 적립됨", null, cancelAmount));
            } else {
                balance.refund(cancelAmount);
                updatedBalances.add(balance);
                transactions.add(PointTransaction.of(balance, TransactionType.CANCEL_USE, now, "포인트 사용 취소", null, cancelAmount));
            }

            remainingToCancel -= cancelAmount;
            totalCanceled += cancelAmount;
        }

        pointBalanceRepository.saveAll(updatedBalances);
        pointTransactionRepository.saveAll(transactions);
        int totalRemaining = calculateTotalRemainingPoints(request.getUserId());

        return PointUseResponse.from(request.getUserId(), request.getOrderId(), totalCanceled, totalRemaining);
    }


    private int calculateTotalRemainingPoints(Long userId) {
        return pointBalanceRepository.findByUserIdAndStatusOrderByBalanceTypeAscExpireDateAsc(userId, BalanceStatus.ACTIVE)
                .stream().mapToInt(PointBalance::getRemainAmount).sum();
    }
}
