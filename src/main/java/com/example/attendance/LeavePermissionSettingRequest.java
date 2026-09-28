
package com.example.attendance;

import java.time.LocalTime;

public class LeavePermissionSettingRequest {

    private String settingType;
    private String role;
    private Integer employeeId;

    private Double sickLeave;
    private Double casualLeave;
    private Integer permissionCount;

    private Double permissionHours;

    private LocalTime officeStartTime;
    private LocalTime officeEndTime;

    private String shiftType;


    // =========================
    // GETTERS
    // =========================

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

    public Double getPermissionHours() {
        return permissionHours;
    }

    public LocalTime getOfficeStartTime() {
        return officeStartTime;
    }

    public LocalTime getOfficeEndTime() {
        return officeEndTime;
    }

    public String getShiftType() {
        return shiftType;
    }


    // =========================
    // SETTERS
    // =========================

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

    public void setPermissionHours(Double permissionHours) {
        this.permissionHours = permissionHours;
    }

    public void setOfficeStartTime(LocalTime officeStartTime) {
        this.officeStartTime = officeStartTime;
    }

    public void setOfficeEndTime(LocalTime officeEndTime) {
        this.officeEndTime = officeEndTime;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }
}
