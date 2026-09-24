package com.example.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AdminLeaveResponse {

    private Integer id;
    private Integer employeeId;
    private String employeeCode;
    private String employeeName;

    private String leaveType;
    private LocalDate leaveDate;

    private Double leaveDuration;
    private Double lopDays;

    private String halfDaySession;

    private LocalTime permissionStart;
    private LocalTime permissionEnd;

    private String reason;
    private String status;
    private LocalDateTime createdAt;

    public AdminLeaveResponse(
            Integer id,
            Integer employeeId,
            String employeeCode,
            String employeeName,
            String leaveType,
            LocalDate leaveDate,
            Double leaveDuration,
            Double lopDays,
            String halfDaySession,
            LocalTime permissionStart,
            LocalTime permissionEnd,
            String reason,
            String status,
            LocalDateTime createdAt) {

        this.id = id;
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.leaveType = leaveType;
        this.leaveDate = leaveDate;
        this.leaveDuration = leaveDuration;
        this.lopDays = lopDays;
        this.halfDaySession = halfDaySession;
        this.permissionStart = permissionStart;
        this.permissionEnd = permissionEnd;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public LocalDate getLeaveDate() {
        return leaveDate;
    }

    public Double getLeaveDuration() {
        return leaveDuration;
    }

    public Double getLopDays() {
        return lopDays;
    }

    public String getHalfDaySession() {
        return halfDaySession;
    }

    public LocalTime getPermissionStart() {
        return permissionStart;
    }

    public LocalTime getPermissionEnd() {
        return permissionEnd;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

