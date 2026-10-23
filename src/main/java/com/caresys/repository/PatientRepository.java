package com.caresys.repository;

import com.caresys.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findAllByOrderByIdAsc();
    List<Patient> findByNameContainingIgnoreCase(String name);
    List<Patient> findByHasHealthPlan(Boolean hasHealthPlan);

    @Query("SELECT DISTINCT p FROM Patient p WHERE (p.staff.id = :staffId) " +
           "OR EXISTS (SELECT 1 FROM Appointment a WHERE a.patient.id = p.id AND a.staff.id = :staffId) " +
           "OR EXISTS (SELECT 1 FROM MedicalRecord m WHERE m.patient.id = p.id AND m.staff.id = :staffId)")
    List<Patient> findLinkedToStaff(@Param("staffId") Long staffId);
}
