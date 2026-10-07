package com.example.attendance;

public class LeaveSettingChangeRequest {

    private Double sickLeave;
    private Double casualLeave;
    private Integer permissionCount;
    private Double permissionHours;

    private String adminEmail;
    private String reason;

    public Double getSickLeave() { return sickLeave; }
    public Double getCasualLeave() { return casualLeave; }
    public Integer getPermissionCount() { return permissionCount; }
    public Double getPermissionHours() { return permissionHours; }
    public String getAdminEmail() { return adminEmail; }
    public String getReason() { return reason; }

    public void setSickLeave(Double sickLeave) { this.sickLeave = sickLeave; }
    public void setCasualLeave(Double casualLeave) { this.casualLeave = casualLeave; }
    public void setPermissionCount(Integer permissionCount) { this.permissionCount = permissionCount; }
    public void setPermissionHours(Double permissionHours) { this.permissionHours = permissionHours; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public void setReason(String reason) { this.reason = reason; }
}