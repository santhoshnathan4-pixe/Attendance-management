
package com.example.attendance;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "working_day_settings")
public class WorkingDaySetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_date", nullable = false, unique = true)
    private LocalDate settingDate;

    @Column(name = "setting_type", nullable = false)
    private String settingType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // =========================
    // GETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public LocalDate getSettingDate() {
        return settingDate;
    }

    public String getSettingType() {
        return settingType;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


    // =========================
    // SETTERS
    // =========================

    public void setId(Long id) {
        this.id = id;
    }

    public void setSettingDate(LocalDate settingDate) {
        this.settingDate = settingDate;
    }

    public void setSettingType(String settingType) {
        this.settingType = settingType;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

