package com.caresys.repository;

import com.caresys.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientIdOrderByEvolutionDateDesc(Long patientId);
    List<MedicalRecord> findByPatientIdAndStaffIdOrderByEvolutionDateDesc(Long patientId, Long staffId);
}
