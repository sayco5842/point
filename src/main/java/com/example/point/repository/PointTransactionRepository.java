package com.example.point.repository;

import com.example.point.domain.PointTransaction;
import com.example.point.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findByUserIdAndOrderIdAndTransactionType(Long userId, String orderId, TransactionType transactionType);
}
