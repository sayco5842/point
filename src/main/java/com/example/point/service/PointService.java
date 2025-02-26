package com.example.point.service;

import com.example.point.dto.save.PointSaveRequest;
import com.example.point.dto.save.PointSaveResponse;
import com.example.point.dto.use.PointCancelUseRequest;
import com.example.point.dto.use.PointUseRequest;
import com.example.point.dto.use.PointUseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointSaveService pointSaveService;
    private final PointUseService pointUseService;

    @Transactional
    public PointSaveResponse save(PointSaveRequest request) {
        return pointSaveService.save(request);
    }

    @Transactional
    public PointSaveResponse cancelSave(Long balanceId) {
        return pointSaveService.cancelSave(balanceId);
    }

    @Transactional
    public PointUseResponse use(Long userId, PointUseRequest request) {
        return pointUseService.use(userId, request);
    }

    @Transactional
    public PointUseResponse cancelUse(PointCancelUseRequest request) {
        return pointUseService.cancelUse(request);
    }
}
