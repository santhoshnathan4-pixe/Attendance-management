
package com.example.attendance;

public class LeavePermissionSettingRequest {

    private String settingType;
    private String role;
    private Integer employeeId;

    private Double sickLeave;
    private Double casualLeave;
    private Integer permissionCount;


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
}

