package com.example.point.repository;

import com.example.point.domain.PointBalance;
import com.example.point.domain.enums.BalanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointBalanceRepository extends JpaRepository<PointBalance, Long> {
    List<PointBalance> findByUserIdAndStatusOrderByBalanceTypeAscExpireDateAsc(Long userId, BalanceStatus status);
}
