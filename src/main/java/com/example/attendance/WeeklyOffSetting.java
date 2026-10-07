package com.example.attendance;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_off_settings")
public class WeeklyOffSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_of_week", nullable = false, unique = true)
    private String dayOfWeek;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

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

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public Boolean getActive() {
        return active;
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

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}