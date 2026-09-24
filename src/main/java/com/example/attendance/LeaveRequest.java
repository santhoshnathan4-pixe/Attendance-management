package com.example.attendance;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "leave_type")
    private String leaveType;

    @Column(name = "leave_date")
    private LocalDate leaveDate;

    @Column(name = "leave_duration")
    private Double leaveDuration;

    @Column(name = "lop_days")
    private Double lopDays = 0.00;

    @Column(name = "half_day_session")
    private String halfDaySession;

    @Column(name = "permission_start")
    private LocalTime permissionStart;

    @Column(name = "permission_end")
    private LocalTime permissionEnd;

    private String reason;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;


    // =========================
    // GETTERS
    // =========================

    public Integer getId() {
        return id;
    }

    public Integer getEmployeeId() {
        return employeeId;
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


    // =========================
    // SETTERS
    // =========================

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public void setLeaveDate(LocalDate leaveDate) {
        this.leaveDate = leaveDate;
    }

    public void setLeaveDuration(Double leaveDuration) {
        this.leaveDuration = leaveDuration;
    }

    public void setLopDays(Double lopDays) {
        this.lopDays = lopDays;
    }

    public void setHalfDaySession(String halfDaySession) {
        this.halfDaySession = halfDaySession;
    }

    public void setPermissionStart(LocalTime permissionStart) {
        this.permissionStart = permissionStart;
    }

    public void setPermissionEnd(LocalTime permissionEnd) {
        this.permissionEnd = permissionEnd;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

