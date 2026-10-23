package com.caresys.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "staff")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String role; // Especialidade fisioterapêutica

    @Column(length = 50)
    private String crefito;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Long requestedUserId;

    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    private boolean userIdProvided = false;

    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Staff() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCrefito() { return crefito; }
    public void setCrefito(String crefito) { this.crefito = crefito; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @com.fasterxml.jackson.annotation.JsonProperty("userId")
    public Long getUserId() {
        return user != null ? user.getId() : requestedUserId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("userId")
    public void setUserId(Long userId) {
        this.requestedUserId = userId;
        this.userIdProvided = true;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Long getRequestedUserId() { return requestedUserId; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isUserIdProvided() { return userIdProvided; }

    @com.fasterxml.jackson.annotation.JsonProperty("username")
    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("userName")
    public String getUserName() {
        return user != null ? user.getName() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("userRole")
    public String getUserRole() {
        return user != null ? user.getRole() : null;
    }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
