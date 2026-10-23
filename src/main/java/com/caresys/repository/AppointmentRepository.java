package com.caresys.repository;

import com.caresys.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByAppointmentDateOrderByAppointmentTimeAsc(LocalDate date);
    List<Appointment> findByAppointmentDateAndStaffIdOrderByAppointmentTimeAsc(LocalDate date, Long staffId);
    List<Appointment> findByStaffIdOrderByAppointmentDateDescAppointmentTimeAsc(Long staffId);
    List<Appointment> findAllByOrderByAppointmentDateDescAppointmentTimeAsc();

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date AND a.staff.id = :staffId AND a.status <> 'cancelled'")
    List<Appointment> findActiveByDateAndStaff(@Param("date") LocalDate date, @Param("staffId") Long staffId);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date AND a.patient.id = :patientId AND a.status <> 'cancelled'")
    List<Appointment> findActiveByDateAndPatient(@Param("date") LocalDate date, @Param("patientId") Long patientId);
}
