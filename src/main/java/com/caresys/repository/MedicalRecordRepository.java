package com.caresys.repository;

import com.caresys.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.id = :patientId ORDER BY m.evolutionDate DESC")
    List<MedicalRecord> findByPatientIdOrderByEvolutionDateDesc(@Param("patientId") Long patientId);

    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.id = :patientId AND m.staff.id = :staffId ORDER BY m.evolutionDate DESC")
    List<MedicalRecord> findByPatientIdAndStaffIdOrderByEvolutionDateDesc(@Param("patientId") Long patientId, @Param("staffId") Long staffId);
}
