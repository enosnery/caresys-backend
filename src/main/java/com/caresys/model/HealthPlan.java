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

    @Column(name = "copay_rate", precision = 5, scale = 2)
    private BigDecimal copayRate = BigDecimal.ZERO;

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

    public BigDecimal getCopayRate() { return copayRate; }
    public void setCopayRate(BigDecimal copayRate) { this.copayRate = copayRate; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
