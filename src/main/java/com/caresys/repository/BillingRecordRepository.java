package com.caresys.repository;

import com.caresys.model.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT COUNT(b) > 0 FROM BillingRecord b WHERE b.appointment.id = :appointmentId")
    boolean existsByAppointmentId(@Param("appointmentId") Long appointmentId);

    @Query("SELECT b FROM BillingRecord b WHERE b.appointment.id = :appointmentId")
    java.util.Optional<BillingRecord> findByAppointmentId(@Param("appointmentId") Long appointmentId);

    @Query("SELECT b FROM BillingRecord b WHERE b.billingClosure.id = :closureId ORDER BY b.billingDate DESC, b.billingTime DESC")
    List<BillingRecord> findByBillingClosureId(@Param("closureId") Long closureId);
}
