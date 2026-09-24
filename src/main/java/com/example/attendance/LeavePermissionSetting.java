
package com.example.attendance;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_permission_settings")
public class LeavePermissionSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_type", nullable = false)
    private String settingType;

    @Column(name = "role")
    private String role;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "sick_leave", nullable = false)
    private Double sickLeave = 1.00;

    @Column(name = "casual_leave", nullable = false)
    private Double casualLeave = 1.00;

    @Column(name = "permission_count", nullable = false)
    private Integer permissionCount = 2;

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

    public String getSettingType() {
        return settingType;
    }

    public String getRole() {
        return role;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public Double getSickLeave() {
        return sickLeave;
    }

    public Double getCasualLeave() {
        return casualLeave;
    }

    public Integer getPermissionCount() {
        return permissionCount;
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

    public void setSettingType(String settingType) {
        this.settingType = settingType;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public void setSickLeave(Double sickLeave) {
        this.sickLeave = sickLeave;
    }

    public void setCasualLeave(Double casualLeave) {
        this.casualLeave = casualLeave;
    }

    public void setPermissionCount(Integer permissionCount) {
        this.permissionCount = permissionCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

