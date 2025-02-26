package com.example.point.service;

import com.example.point.domain.PointPolicy;
import com.example.point.exception.PointOperationException;
import com.example.point.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class PointPolicyService {
    private final PointPolicyRepository policyRepository;

    public PointPolicy getPolicy() {
        return policyRepository.findAll().stream()
                .findFirst()
                .orElse(PointPolicy.defaultPolicy());
    }

    public void validateSavingAmount(int amount) {
        PointPolicy policy = getPolicy();
        if (amount < policy.getMinSaving() || amount > policy.getMaxSaving()) {
            throw new PointOperationException("포인트는 "
                    + policy.getMinSaving() + " 이상 "
                    + policy.getMaxSaving() + " 이하이어야 합니다.");
        }
    }

    public LocalDateTime calculateExpireDate(LocalDateTime now, Integer customExpireDays) {
        PointPolicy policy = getPolicy();
        int expireDays = customExpireDays != null ? customExpireDays : policy.getDefaultExpireDays();
        if (expireDays < policy.getMinExpireDays() || expireDays >= policy.getMaxExpireDays()) {
            throw new PointOperationException("만료일은 최소 "
                    + policy.getMinExpireDays() + "일 이상, 최대 "
                    + policy.getMaxExpireDays() + "일 미만이어야 합니다.");
        }
        return now.plusDays(expireDays);
    }

    public void validatePointPointLimit(int currentFreePoints) {
        PointPolicy policy = getPolicy();
        if (currentFreePoints > policy.getPointLimit()) {
            throw new PointOperationException("무료 포인트 보유 한도를 초과하였습니다. (최대 "
                    + policy.getPointLimit() + "포인트)");
        }
    }
}
