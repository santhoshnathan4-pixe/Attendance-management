package com.example.attendance;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "overtime_records")
public class OvertimeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "ot_date", nullable = false)
    private LocalDate otDate;

    @Column(name = "day_type", nullable = false)
    private String dayType;

    @Column(name = "work_type", nullable = false)
    private String workType;

    @Column(name = "ot_eligible", nullable = false)
    private Boolean otEligible = false;

    @Column(name = "benefit_type")
    private String benefitType;

    @Column(name = "ot_amount")
    private Double otAmount = 0.0;

    @Column(name = "comp_off_days")
    private Double compOffDays = 0.0;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "admin_email")
    private String adminEmail;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // =====================================================
    // GETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public LocalDate getOtDate() {
        return otDate;
    }

    public String getDayType() {
        return dayType;
    }

    public String getWorkType() {
        return workType;
    }

    public Boolean getOtEligible() {
        return otEligible;
    }

    public String getBenefitType() {
        return benefitType;
    }

    public Double getOtAmount() {
        return otAmount;
    }

    public Double getCompOffDays() {
        return compOffDays;
    }

    public String getStatus() {
        return status;
    }

    public String getAdminEmail() {
        return adminEmail;
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


    // =====================================================
    // SETTERS
    // =====================================================

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public void setOtDate(LocalDate otDate) {
        this.otDate = otDate;
    }

    public void setDayType(String dayType) {
        this.dayType = dayType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public void setOtEligible(Boolean otEligible) {
        this.otEligible = otEligible;
    }

    public void setBenefitType(String benefitType) {
        this.benefitType = benefitType;
    }

    public void setOtAmount(Double otAmount) {
        this.otAmount = otAmount;
    }

    public void setCompOffDays(Double compOffDays) {
        this.compOffDays = compOffDays;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
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