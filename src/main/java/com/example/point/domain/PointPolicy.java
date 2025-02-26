package com.example.point.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "point_policy")
public class PointPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // 1회 적립 가능 최소 포인트
    @Column(name = "min_saving", nullable = false)
    private int minSaving;

    // 1회 적립 가능 최대 포인트
    @Column(name = "max_saving", nullable = false)
    private int maxSaving;

    // 개인별 포인트 최대 보유 한도
    @Column(name = "point_limit", nullable = false)
    private int pointLimit;

    // 기본 만료일 (일수)
    @Column(name = "default_expire_days", nullable = false)
    private int defaultExpireDays;

    // 최소 만료일 (일수)
    @Column(name = "min_expire_days", nullable = false)
    private int minExpireDays;

    // 최대 만료일 (일수, 5년 미만)
    @Column(name = "max_expire_days", nullable = false)
    private int maxExpireDays;

    // 엔티티 생성일 (업데이트 불가)
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    public static PointPolicy defaultPolicy() {
        PointPolicy policy = new PointPolicy();
        policy.setMinSaving(1); // 최소 1포인트부터 적립 가능
        policy.setMaxSaving(100000); // 최대 100,000포인트까지 적립 가능
        policy.setPointLimit(1000000); // 최대 1,000,000포인트까지 보유 가능
        policy.setDefaultExpireDays(365); // 기본 1년
        policy.setMinExpireDays(1); // 최소 1일
        policy.setMaxExpireDays(1825); // 최대 5년

        return policy;
    }

}
