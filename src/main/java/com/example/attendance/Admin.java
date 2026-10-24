package com.example.attendance;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
public class Admin {

    public static final String OWNER = "OWNER";
    public static final String HR = "HR";
    public static final String MANAGER = "MANAGER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "admin_name")
    private String adminName;

    @Column(name = "email", unique = true)
    private String email;

    /** Stored as a hash (see PasswordUtil). */
    @Column(name = "password")
    private String password;

    /** OWNER, HR or MANAGER. Existing admins are OWNER. */
    @Column(name = "role")
    private String role;

    /** Existing table already has this column; a new admin is active. */
    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Only HR and MANAGER are limited roles. Anything else (empty, "ADMIN", "OWNER")
     * is an original admin and counts as OWNER.
     */
    public String getRole() {
        String r = role == null ? "" : role.trim().toUpperCase();
        return HR.equals(r) || MANAGER.equals(r) ? r : OWNER;
    }

    public boolean isActive() {
        return active == null || active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
