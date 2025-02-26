package com.example.point.service;

import com.example.point.domain.PointPolicy;
import com.example.point.exception.PointOperationException;
import com.example.point.repository.PointPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("PointPolicy Test")
@SpringBootTest
class PointPolicyServiceTest {

    @Mock
    private PointPolicyRepository pointPolicyRepository;

    @InjectMocks
    private PointPolicyService pointPolicyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("정책 조회 - 정책이 없을 때 기본정책이 리턴되어야한다.")
    @Test
    void testGetPolicy_NoPolicyFound() {
        // Arrange
        when(pointPolicyRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        PointPolicy policy = pointPolicyService.getPolicy();

        // Assert
        assertEquals(1, policy.getMinSaving());
        assertEquals(100000, policy.getMaxSaving());
        assertEquals(365, policy.getDefaultExpireDays());
    }

    @DisplayName("1회 충전금액 - 정상")
    @Test
    void testValidateSavingAmount_Valid() {
        // Arrange
        PointPolicy mockPolicy = PointPolicy.builder()
                .minSaving(1)
                .maxSaving(100000)
                .build();
        when(pointPolicyRepository.findAll()).thenReturn(Collections.singletonList(mockPolicy));

        // Act & Assert
        assertDoesNotThrow(() -> pointPolicyService.validateSavingAmount(500));
    }

    @DisplayName("1회 충전금액 - 최소적립금액 미만인 경우 에러가 발생한다.")
    @Test
    void testValidateSavingAmount_TooLow() {
        // Arrange
        PointPolicy mockPolicy = PointPolicy.builder()
                .minSaving(1)
                .maxSaving(100000)
                .build();
        when(pointPolicyRepository.findAll()).thenReturn(Collections.singletonList(mockPolicy));

        // Act & Assert
        assertThrows(PointOperationException.class, () -> pointPolicyService.validateSavingAmount(0));
    }

    @DisplayName("1회 충전금액 - 최대적립금액 초과인 경우 에러가 발생한다.")
    @Test
    void testValidateSavingAmount_TooHigh() {
        // Arrange
        PointPolicy mockPolicy = PointPolicy.builder()
                .minSaving(1)
                .maxSaving(100000)
                .build();
        when(pointPolicyRepository.findAll()).thenReturn(Collections.singletonList(mockPolicy));

        // Act & Assert
        assertThrows(PointOperationException.class, () -> pointPolicyService.validateSavingAmount(200000));
    }

    @DisplayName("만료일 - 만료일 계산 정상")
    @Test
    void testCalculateExpireDate_Valid() {
        // Arrange
        PointPolicy mockPolicy = PointPolicy.builder()
                .minExpireDays(1)
                .maxExpireDays(1825)
                .defaultExpireDays(365)
                .build();
        when(pointPolicyRepository.findAll()).thenReturn(Collections.singletonList(mockPolicy));

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);
        int customExpireDays = 100;

        // Act
        LocalDateTime result = pointPolicyService.calculateExpireDate(now, customExpireDays);

        // Assert
        assertEquals(now.plusDays(customExpireDays), result);
    }

    @DisplayName("만료일 - 만료일이 없는경우 기본 만료일 가져옴")
    @Test
    void testCalculateExpireDate_DefaultExpire() {
        // Arrange
        PointPolicy mockPolicy = PointPolicy.builder()
                .minExpireDays(1)
                .maxExpireDays(1825)
                .defaultExpireDays(365)
                .build();
        when(pointPolicyRepository.findAll()).thenReturn(Collections.singletonList(mockPolicy));
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);

        // Act
        LocalDateTime result = pointPolicyService.calculateExpireDate(now, null);

        // Assert
        assertEquals(now.plusDays(365), result);
    }

    @DisplayName("만료일 - 만료일이 최대 만료일보다 큰 경우 에러가 발생한다.")
    @Test
    void testCalculateExpireDate_TooHigh() {
        // Arrange
        PointPolicy mockPolicy = PointPolicy.builder()
                .minExpireDays(1)
                .maxExpireDays(1825)
                .defaultExpireDays(365)
                .build();
        when(pointPolicyRepository.findAll()).thenReturn(Collections.singletonList(mockPolicy));
        LocalDateTime now = LocalDateTime.now();

        // Act & Assert
        assertThrows(PointOperationException.class, () -> pointPolicyService.calculateExpireDate(now, 2000));
    }

    @DisplayName("포인트 보유금액 - 최대 포인트 보유금액보다 작은 경우 정상")
    @Test
    void testValidatePointPointLimit_WithinLimit() {
        // Arrange
        PointPolicy mockPolicy = PointPolicy.builder()
                .pointLimit(500000)
                .build();
        when(pointPolicyRepository.findAll()).thenReturn(Collections.singletonList(mockPolicy));

        // Act & Assert
        assertDoesNotThrow(() -> pointPolicyService.validatePointPointLimit(300000));
    }

    @DisplayName("포인트 보유금액 - 최대 포인트 보유금액보다 큰 경우 에러가 발생한다")
    @Test
    void testValidatePointPointLimit_Exceeded() {
        // Arrange
        PointPolicy mockPolicy = PointPolicy.builder()
                .pointLimit(1000000)
                .build();
        when(pointPolicyRepository.findAll()).thenReturn(Collections.singletonList(mockPolicy));

        // Act & Asset
        assertThrows(PointOperationException.class, () -> pointPolicyService.validatePointPointLimit(1100000));
    }
}
