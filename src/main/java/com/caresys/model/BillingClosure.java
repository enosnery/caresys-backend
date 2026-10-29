package com.caresys.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "billing_closures")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BillingClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(length = 180)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "appointments_count", nullable = false)
    private Integer appointmentsCount = 0;

    @Column(name = "total_gross_revenue", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalGrossRevenue = BigDecimal.ZERO;

    @Column(name = "total_tax", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "total_staff_payment", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalStaffPayment = BigDecimal.ZERO;

    @Column(name = "total_clinic_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalClinicValue = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "closed_at")
    private Instant closedAt = Instant.now();

    public BillingClosure() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getAppointmentsCount() { return appointmentsCount; }
    public void setAppointmentsCount(Integer appointmentsCount) { this.appointmentsCount = appointmentsCount; }

    public BigDecimal getTotalGrossRevenue() { return totalGrossRevenue; }
    public void setTotalGrossRevenue(BigDecimal totalGrossRevenue) { this.totalGrossRevenue = totalGrossRevenue; }

    public BigDecimal getTotalTax() { return totalTax; }
    public void setTotalTax(BigDecimal totalTax) { this.totalTax = totalTax; }

    public BigDecimal getTotalStaffPayment() { return totalStaffPayment; }
    public void setTotalStaffPayment(BigDecimal totalStaffPayment) { this.totalStaffPayment = totalStaffPayment; }

    public BigDecimal getTotalClinicValue() { return totalClinicValue; }
    public void setTotalClinicValue(BigDecimal totalClinicValue) { this.totalClinicValue = totalClinicValue; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
