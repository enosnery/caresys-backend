package com.caresys.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "health_plans")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HealthPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "staff_value", precision = 10, scale = 2)
    private BigDecimal staffValue = BigDecimal.ZERO;

    @Column(name = "expected_payment_days")
    private Integer expectedPaymentDays = 30;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public HealthPlan() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public BigDecimal getStaffValue() { return staffValue; }
    public void setStaffValue(BigDecimal staffValue) { this.staffValue = staffValue; }

    public Integer getExpectedPaymentDays() { return expectedPaymentDays; }
    public void setExpectedPaymentDays(Integer expectedPaymentDays) { this.expectedPaymentDays = expectedPaymentDays; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
