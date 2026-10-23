package com.caresys.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "patients")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal height;

    @Column(name = "chief_complaint", nullable = false, columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "has_health_plan")
    private Boolean hasHealthPlan = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "health_plan_id")
    private HealthPlan healthPlan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Patient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }

    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }

    public Boolean getHasHealthPlan() { return hasHealthPlan; }
    public void setHasHealthPlan(Boolean hasHealthPlan) { this.hasHealthPlan = hasHealthPlan; }

    public HealthPlan getHealthPlan() { return healthPlan; }
    public void setHealthPlan(HealthPlan healthPlan) { this.healthPlan = healthPlan; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
