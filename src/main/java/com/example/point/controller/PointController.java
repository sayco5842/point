package com.example.point.controller;

import com.example.point.dto.save.PointCancelSaveRequest;
import com.example.point.dto.save.PointSaveRequest;
import com.example.point.dto.save.PointSaveResponse;
import com.example.point.dto.use.PointCancelUseRequest;
import com.example.point.dto.use.PointUseRequest;
import com.example.point.dto.use.PointUseResponse;
import com.example.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/points")
public class PointController {
    private final PointService pointService;

    @PostMapping("/save")
    public ResponseEntity<PointSaveResponse> savePoints(@RequestBody PointSaveRequest request) {
        return ResponseEntity.ok(pointService.save(request));
    }

    @PostMapping("/save/cancel")
    public ResponseEntity<PointSaveResponse> cancelPoints(@RequestBody PointCancelSaveRequest request) {
        return ResponseEntity.ok(pointService.cancelSave(request.getBalanceId()));
    }

    @PostMapping("/use")
    public ResponseEntity<PointUseResponse> usePoints(@RequestParam Long userId,
                                                      @RequestBody PointUseRequest usageRequest) {
        return ResponseEntity.ok(pointService.use(userId, usageRequest));
    }

    @PostMapping("/use/cancel")
    public ResponseEntity<PointUseResponse> cancelUsePoints(@RequestBody PointCancelUseRequest request) {
        return ResponseEntity.ok(pointService.cancelUse(request));
    }
}
