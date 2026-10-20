package com.example.attendance;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Admin decision for ONE employee in ONE month.
 *
 * deductMinutes = null -> automatic rule (short time of the days that crossed the 30 min tolerance is cut)
 * deductMinutes = 0 -> pay for all present days (short time is waived)
 * deductMinutes = N -> only N minutes are cut
 * payFull = true -> full salary / stipend, leave and absence are ignored (example: intern)
 */
@Entity
@Table(name = "payroll_adjustments")
public class PayrollAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "pay_year")
    private Integer payYear;

    @Column(name = "pay_month")
    private Integer payMonth;

    @Column(name = "deduct_minutes")
    private Integer deductMinutes;

    @Column(name = "pay_full", nullable = false)
    private Boolean payFull = false;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "adjusted_by")
    private String adjustedBy;

    @Column(name = "adjusted_at")
    private LocalDateTime adjustedAt;

    @Column(name = "paid", nullable = false)
    private Boolean paid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_by")
    private String paidBy;

    // =========================
    // GETTERS
    // =========================

    public Integer getId() {
        return id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public Integer getPayYear() {
        return payYear;
    }

    public Integer getPayMonth() {
        return payMonth;
    }

    public Integer getDeductMinutes() {
        return deductMinutes;
    }

    public Boolean getPayFull() {
        return payFull;
    }

    public String getReason() {
        return reason;
    }

    public String getAdjustedBy() {
        return adjustedBy;
    }

    public LocalDateTime getAdjustedAt() {
        return adjustedAt;
    }

    public Boolean getPaid() {
        return paid;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getPaidBy() {
        return paidBy;
    }

    // =========================
    // SETTERS
    // =========================

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public void setPayYear(Integer payYear) {
        this.payYear = payYear;
    }

    public void setPayMonth(Integer payMonth) {
        this.payMonth = payMonth;
    }

    public void setDeductMinutes(Integer deductMinutes) {
        this.deductMinutes = deductMinutes;
    }

    public void setPayFull(Boolean payFull) {
        this.payFull = payFull;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setAdjustedBy(String adjustedBy) {
        this.adjustedBy = adjustedBy;
    }

    public void setAdjustedAt(LocalDateTime adjustedAt) {
        this.adjustedAt = adjustedAt;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public void setPaidBy(String paidBy) {
        this.paidBy = paidBy;
    }
}