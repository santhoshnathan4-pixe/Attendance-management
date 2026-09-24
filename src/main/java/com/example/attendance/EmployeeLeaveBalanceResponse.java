package com.example.attendance;

public class EmployeeLeaveBalanceResponse {

    private double sickUsed;
    private double sickRemaining;

    private double casualUsed;
    private double casualRemaining;

    private long permissionUsed;
    private long permissionRemaining;

    public EmployeeLeaveBalanceResponse(
            double sickUsed,
            double sickRemaining,
            double casualUsed,
            double casualRemaining,
            long permissionUsed,
            long permissionRemaining) {

        this.sickUsed = sickUsed;
        this.sickRemaining = sickRemaining;

        this.casualUsed = casualUsed;
        this.casualRemaining = casualRemaining;

        this.permissionUsed = permissionUsed;
        this.permissionRemaining = permissionRemaining;
    }

    public double getSickUsed() {
        return sickUsed;
    }

    public double getSickRemaining() {
        return sickRemaining;
    }

    public double getCasualUsed() {
        return casualUsed;
    }

    public double getCasualRemaining() {
        return casualRemaining;
    }

    public long getPermissionUsed() {
        return permissionUsed;
    }

    public long getPermissionRemaining() {
        return permissionRemaining;
    }
}