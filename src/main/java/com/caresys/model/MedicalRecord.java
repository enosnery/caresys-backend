package com.caresys.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "medical_records")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "evolution_date", nullable = false)
    private Instant evolutionDate = Instant.now();

    @Column(name = "physical_evolution", nullable = false, columnDefinition = "TEXT")
    private String physicalEvolution;

    @Column(name = "treatment_conducted", nullable = false, columnDefinition = "TEXT")
    private String treatmentConducted;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public MedicalRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }

    public Instant getEvolutionDate() { return evolutionDate; }
    public void setEvolutionDate(Instant evolutionDate) { this.evolutionDate = evolutionDate; }

    public String getPhysicalEvolution() { return physicalEvolution; }
    public void setPhysicalEvolution(String physicalEvolution) { this.physicalEvolution = physicalEvolution; }

    public String getTreatmentConducted() { return treatmentConducted; }
    public void setTreatmentConducted(String treatmentConducted) { this.treatmentConducted = treatmentConducted; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
