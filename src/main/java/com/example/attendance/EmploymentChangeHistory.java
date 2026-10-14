
package com.example.attendance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "employment_change_history")
public class EmploymentChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(name = "employee_name", length = 200)
    private String employeeName;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType;

    @Column(name = "old_salary", precision = 12, scale = 2)
    private BigDecimal oldSalary;

    @Column(name = "new_salary", precision = 12, scale = 2)
    private BigDecimal newSalary;

    @Column(name = "old_role", length = 100)
    private String oldRole;

    @Column(name = "new_role", length = 100)
    private String newRole;

    @Column(name = "old_designation", length = 150)
    private String oldDesignation;

    @Column(name = "new_designation", length = 150)
    private String newDesignation;

    @Column(name = "old_employment_start_date")
    private LocalDate oldEmploymentStartDate;

    @Column(name = "new_employment_start_date")
    private LocalDate newEmploymentStartDate;

    @Column(name = "old_employment_end_date")
    private LocalDate oldEmploymentEndDate;

    @Column(name = "new_employment_end_date")
    private LocalDate newEmploymentEndDate;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "admin_name", length = 200)
    private String adminName;

    @Column(name = "admin_email", length = 255)
    private String adminEmail;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "employee_acknowledged", nullable = false)
    private boolean employeeAcknowledged = false;

    @Column(name = "employee_acknowledged_at")
    private OffsetDateTime employeeAcknowledgedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public BigDecimal getOldSalary() {
        return oldSalary;
    }

    public void setOldSalary(BigDecimal oldSalary) {
        this.oldSalary = oldSalary;
    }

    public BigDecimal getNewSalary() {
        return newSalary;
    }

    public void setNewSalary(BigDecimal newSalary) {
        this.newSalary = newSalary;
    }

    public String getOldRole() {
        return oldRole;
    }

    public void setOldRole(String oldRole) {
        this.oldRole = oldRole;
    }

    public String getNewRole() {
        return newRole;
    }

    public void setNewRole(String newRole) {
        this.newRole = newRole;
    }

    public String getOldDesignation() {
        return oldDesignation;
    }

    public void setOldDesignation(String oldDesignation) {
        this.oldDesignation = oldDesignation;
    }

    public String getNewDesignation() {
        return newDesignation;
    }

    public void setNewDesignation(String newDesignation) {
        this.newDesignation = newDesignation;
    }

    public LocalDate getOldEmploymentStartDate() {
        return oldEmploymentStartDate;
    }

    public void setOldEmploymentStartDate(LocalDate oldEmploymentStartDate) {
        this.oldEmploymentStartDate = oldEmploymentStartDate;
    }

    public LocalDate getNewEmploymentStartDate() {
        return newEmploymentStartDate;
    }

    public void setNewEmploymentStartDate(LocalDate newEmploymentStartDate) {
        this.newEmploymentStartDate = newEmploymentStartDate;
    }

    public LocalDate getOldEmploymentEndDate() {
        return oldEmploymentEndDate;
    }

    public void setOldEmploymentEndDate(LocalDate oldEmploymentEndDate) {
        this.oldEmploymentEndDate = oldEmploymentEndDate;
    }

    public LocalDate getNewEmploymentEndDate() {
        return newEmploymentEndDate;
    }

    public void setNewEmploymentEndDate(LocalDate newEmploymentEndDate) {
        this.newEmploymentEndDate = newEmploymentEndDate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isEmployeeAcknowledged() {
        return employeeAcknowledged;
    }

    public void setEmployeeAcknowledged(boolean employeeAcknowledged) {
        this.employeeAcknowledged = employeeAcknowledged;
    }

    public OffsetDateTime getEmployeeAcknowledgedAt() {
        return employeeAcknowledgedAt;
    }

    public void setEmployeeAcknowledgedAt(
            OffsetDateTime employeeAcknowledgedAt) {
        this.employeeAcknowledgedAt = employeeAcknowledgedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(OffsetDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
