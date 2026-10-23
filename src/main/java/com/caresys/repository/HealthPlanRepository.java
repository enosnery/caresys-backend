package com.caresys.repository;

import com.caresys.model.HealthPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthPlanRepository extends JpaRepository<HealthPlan, Long> {
    List<HealthPlan> findAllByOrderByIdAsc();
    List<HealthPlan> findByActiveTrue();
    boolean existsByCode(String code);
}

