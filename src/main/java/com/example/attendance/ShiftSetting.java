package com.example.attendance;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shift_settings")
public class ShiftSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // GENERAL / SHIFT_1 / SHIFT_2 / SHIFT_3
    @Column(name = "shift_type", nullable = false, unique = true)
    private String shiftType;

    // Shift start time
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    // Shift end time
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Attendance becomes LATE after this time
    @Column(name = "late_after")
    private LocalTime lateAfter;

    // Salary deduction starts after this time
    @Column(name = "salary_deduction_after")
    private LocalTime salaryDeductionAfter;

    // Salary deduction applies for early checkout before this time
    @Column(name = "early_deduction_before")
    private LocalTime earlyDeductionBefore;

    // Half-day boundary
    @Column(name = "half_day_boundary")
    private LocalTime halfDayBoundary;

    // Grace period in minutes
    @Column(name = "grace_minutes", nullable = false)
    private Integer graceMinutes = 5;

    // Actual working hours for the shift
    @Column(name = "working_hours", nullable = false)
    private Double workingHours;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================
    // GETTERS
    // =========================================

    public Long getId() {
        return id;
    }

    public String getShiftType() {
        return shiftType;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public LocalTime getLateAfter() {
        return lateAfter;
    }

    public LocalTime getSalaryDeductionAfter() {
        return salaryDeductionAfter;
    }

    public LocalTime getEarlyDeductionBefore() {
        return earlyDeductionBefore;
    }

    public LocalTime getHalfDayBoundary() {
        return halfDayBoundary;
    }

    public Integer getGraceMinutes() {
        return graceMinutes;
    }

    public Double getWorkingHours() {
        return workingHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // =========================================
    // SETTERS
    // =========================================

    public void setId(Long id) {
        this.id = id;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public void setLateAfter(LocalTime lateAfter) {
        this.lateAfter = lateAfter;
    }

    public void setSalaryDeductionAfter(
            LocalTime salaryDeductionAfter) {

        this.salaryDeductionAfter =
                salaryDeductionAfter;
    }

    public void setEarlyDeductionBefore(
            LocalTime earlyDeductionBefore) {

        this.earlyDeductionBefore =
                earlyDeductionBefore;
    }

    public void setHalfDayBoundary(
            LocalTime halfDayBoundary) {

        this.halfDayBoundary =
                halfDayBoundary;
    }

    public void setGraceMinutes(
            Integer graceMinutes) {

        this.graceMinutes =
                graceMinutes;
    }

    public void setWorkingHours(
            Double workingHours) {

        this.workingHours =
                workingHours;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt =
                createdAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {

        this.updatedAt =
                updatedAt;
    }
}