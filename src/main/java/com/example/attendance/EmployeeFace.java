package com.example.attendance;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "employee_faces")
public class EmployeeFace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "face_data", nullable = false, columnDefinition = "TEXT")
    private String faceData;

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // =========================
    // GETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getFaceData() {
        return faceData;
    }

    public OffsetDateTime getRegisteredAt() {
        return registeredAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    // =========================
    // SETTERS
    // =========================

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public void setFaceData(String faceData) {
        this.faceData = faceData;
    }

    public void setRegisteredAt(OffsetDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // =========================
    // AUTOMATIC TIMESTAMPS
    // =========================

    @PrePersist
    protected void onCreate() {

        OffsetDateTime now = OffsetDateTime.now();

        registeredAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = OffsetDateTime.now();
    }
}