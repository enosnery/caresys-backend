package com.caresys.repository;

import com.caresys.model.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {
    List<BillingRecord> findAllByOrderByBillingDateDescBillingTimeDesc();
    List<BillingRecord> findByStaffIdOrderByBillingDateDescBillingTimeDesc(Long staffId);
    List<BillingRecord> findByIsHealthPlan(Boolean isHealthPlan);
    List<BillingRecord> findByStaffIdAndIsHealthPlan(Long staffId, Boolean isHealthPlan);
    List<BillingRecord> findByHealthPlanId(Long healthPlanId);
    List<BillingRecord> findByBillingDate(LocalDate date);
    boolean existsByAppointmentId(Long appointmentId);
}
