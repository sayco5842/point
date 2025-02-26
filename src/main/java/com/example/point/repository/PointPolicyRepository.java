package com.example.point.repository;

import com.example.point.domain.PointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {
}
