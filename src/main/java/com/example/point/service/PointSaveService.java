package com.example.point.service;

import com.example.point.domain.PointBalance;
import com.example.point.domain.PointTransaction;
import com.example.point.domain.enums.BalanceStatus;
import com.example.point.domain.enums.TransactionType;
import com.example.point.dto.save.PointSaveRequest;
import com.example.point.dto.save.PointSaveResponse;
import com.example.point.exception.PointOperationException;
import com.example.point.repository.PointBalanceRepository;
import com.example.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointSaveService {

    private final PointBalanceRepository pointBalanceRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointPolicyService policyService;
    private static final String SAVE = "포인트 적립";
    private static final String CANCEL_SAVE = "포인트 적립 취소";

    @Transactional
    public PointSaveResponse save(PointSaveRequest request) {
        validateSave(request);
        LocalDateTime expireDate = policyService.calculateExpireDate(LocalDateTime.now(), request.getExpireDays());
        PointBalance balance = PointBalance.of(request, expireDate);
        PointBalance savedBalance = pointBalanceRepository.save(balance);
        createTransaction(savedBalance, TransactionType.SAVE, SAVE);

        return PointSaveResponse.from(savedBalance);
    }

    @Transactional
    public PointSaveResponse cancelSave(Long balanceId) {
        PointBalance balance = pointBalanceRepository.findById(balanceId)
                .orElseThrow(() -> new PointOperationException("해당 포인트 적립 내역이 존재하지 않습니다."));

        balance.cancel();
        pointBalanceRepository.save(balance);
        createTransaction(balance, TransactionType.CANCEL_SAVE, CANCEL_SAVE);

        return PointSaveResponse.from(balance);
    }

    private void createTransaction(PointBalance balance, TransactionType type, String description) {
        PointTransaction transaction = PointTransaction.of(balance, type, LocalDateTime.now(), description, null, balance.getRemainAmount());
        pointTransactionRepository.save(transaction);
    }

    private void validateSave(PointSaveRequest request) {
        // 1회 적립 가능 최소/최대 포인트 검증
        policyService.validateSavingAmount(request.getAmount());

        // 개인별 포인트 최대 보유 한도 검증
        List<PointBalance> balances = pointBalanceRepository.findByUserIdAndStatusOrderByBalanceTypeAscExpireDateAsc(request.getUserId(), BalanceStatus.ACTIVE);
        int currentFreePoints = balances.stream().mapToInt(PointBalance::getRemainAmount).sum();
        policyService.validatePointPointLimit(currentFreePoints + request.getAmount());
    }
}
