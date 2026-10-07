package com.example.attendance;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_setting_history")
public class LeaveSettingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "setting_type")
    private String settingType;

    @Column(name = "role")
    private String role;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "old_values", length = 500)
    private String oldValues;

    @Column(name = "new_values", length = 500)
    private String newValues;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "admin_email")
    private String adminEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getSettingId() { return settingId; }
    public String getAction() { return action; }
    public String getSettingType() { return settingType; }
    public String getRole() { return role; }
    public Integer getEmployeeId() { return employeeId; }
    public String getOldValues() { return oldValues; }
    public String getNewValues() { return newValues; }
    public String getReason() { return reason; }
    public String getAdminEmail() { return adminEmail; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setSettingId(Long settingId) { this.settingId = settingId; }
    public void setAction(String action) { this.action = action; }
    public void setSettingType(String settingType) { this.settingType = settingType; }
    public void setRole(String role) { this.role = role; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public void setOldValues(String oldValues) { this.oldValues = oldValues; }
    public void setNewValues(String newValues) { this.newValues = newValues; }
    public void setReason(String reason) { this.reason = reason; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}