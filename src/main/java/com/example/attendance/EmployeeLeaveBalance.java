package com.example.attendance;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_leave_balances")
public class EmployeeLeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "balance_month", nullable = false)
    private LocalDate balanceMonth;

    @Column(name = "sick_balance", nullable = false)
    private Double sickBalance = 0.00;

    @Column(name = "casual_balance", nullable = false)
    private Double casualBalance = 0.00;

    @Column(name = "permission_balance", nullable = false)
    private Integer permissionBalance = 0;

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

    public Integer getEmployeeId() {
        return employeeId;
    }

    public LocalDate getBalanceMonth() {
        return balanceMonth;
    }

    public Double getSickBalance() {
        return sickBalance;
    }

    public Double getCasualBalance() {
        return casualBalance;
    }

    public Integer getPermissionBalance() {
        return permissionBalance;
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

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public void setBalanceMonth(LocalDate balanceMonth) {
        this.balanceMonth = balanceMonth;
    }

    public void setSickBalance(Double sickBalance) {
        this.sickBalance = sickBalance;
    }

    public void setCasualBalance(Double casualBalance) {
        this.casualBalance = casualBalance;
    }

    public void setPermissionBalance(Integer permissionBalance) {
        this.permissionBalance = permissionBalance;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

