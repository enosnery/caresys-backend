package com.caresys.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;

@Entity
@Table(name = "billing_records")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BillingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    @Column(name = "billing_time", nullable = false)
    private LocalTime billingTime;

    @Column(name = "is_health_plan")
    private Boolean isHealthPlan = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "health_plan_id")
    private HealthPlan healthPlan;

    @Column(name = "health_plan_name", length = 120)
    private String healthPlanName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "staff_value", precision = 10, scale = 2)
    private BigDecimal staffValue = BigDecimal.ZERO;

    @Column(name = "procedure_code", length = 100)
    private String procedureCode;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "billing_status", nullable = false, length = 30)
    private String billingStatus = "pending"; // pending, approved, submitted, paid, closed

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "billing_closure_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private BillingClosure billingClosure;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public BillingRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    @com.fasterxml.jackson.annotation.JsonProperty("appointmentId")
    public Long resolveAppointmentId() {
        return appointment != null ? appointment.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("appointmentStatus")
    public String resolveAppointmentStatus() {
        return appointment != null ? appointment.getStatus() : null;
    }

    public BillingClosure getBillingClosure() { return billingClosure; }
    public void setBillingClosure(BillingClosure billingClosure) { this.billingClosure = billingClosure; }

    @com.fasterxml.jackson.annotation.JsonProperty("billingClosureId")
    public Long resolveBillingClosureId() {
        return billingClosure != null ? billingClosure.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("billingClosureCode")
    public String resolveBillingClosureCode() {
        return billingClosure != null ? billingClosure.getCode() : null;
    }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }

    public LocalDate getBillingDate() { return billingDate; }
    public void setBillingDate(LocalDate billingDate) { this.billingDate = billingDate; }

    public LocalTime getBillingTime() { return billingTime; }
    public void setBillingTime(LocalTime billingTime) { this.billingTime = billingTime; }

    public Boolean getIsHealthPlan() { return isHealthPlan; }
    public void setIsHealthPlan(Boolean isHealthPlan) { this.isHealthPlan = isHealthPlan; }

    public HealthPlan getHealthPlan() { return healthPlan; }
    public void setHealthPlan(HealthPlan healthPlan) { this.healthPlan = healthPlan; }

    public String getHealthPlanName() { return healthPlanName; }
    public void setHealthPlanName(String healthPlanName) { this.healthPlanName = healthPlanName; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public BigDecimal getStaffValue() { return staffValue; }
    public void setStaffValue(BigDecimal staffValue) { this.staffValue = staffValue; }

    public String getProcedureCode() { return procedureCode; }
    public void setProcedureCode(String procedureCode) { this.procedureCode = procedureCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getBillingStatus() { return billingStatus; }
    public void setBillingStatus(String billingStatus) { this.billingStatus = billingStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
