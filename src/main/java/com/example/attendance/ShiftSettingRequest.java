package com.example.attendance;

import java.time.LocalTime;

public class ShiftSettingRequest {

    private String shiftType;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalTime lateAfter;

    private LocalTime salaryDeductionAfter;

    private LocalTime earlyDeductionBefore;

    private LocalTime halfDayBoundary;

    private Integer graceMinutes;

    private Double workingHours;

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public LocalTime getLateAfter() {
        return lateAfter;
    }

    public void setLateAfter(LocalTime lateAfter) {
        this.lateAfter = lateAfter;
    }

    public LocalTime getSalaryDeductionAfter() {
        return salaryDeductionAfter;
    }

    public void setSalaryDeductionAfter(LocalTime salaryDeductionAfter) {
        this.salaryDeductionAfter = salaryDeductionAfter;
    }

    public LocalTime getEarlyDeductionBefore() {
        return earlyDeductionBefore;
    }

    public void setEarlyDeductionBefore(LocalTime earlyDeductionBefore) {
        this.earlyDeductionBefore = earlyDeductionBefore;
    }

    public LocalTime getHalfDayBoundary() {
        return halfDayBoundary;
    }

    public void setHalfDayBoundary(LocalTime halfDayBoundary) {
        this.halfDayBoundary = halfDayBoundary;
    }

    public Integer getGraceMinutes() {
        return graceMinutes;
    }

    public void setGraceMinutes(Integer graceMinutes) {
        this.graceMinutes = graceMinutes;
    }

    public Double getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(Double workingHours) {
        this.workingHours = workingHours;
    }
}