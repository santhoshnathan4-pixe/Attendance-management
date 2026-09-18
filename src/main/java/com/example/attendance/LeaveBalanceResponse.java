package com.example.attendance;

public class LeaveBalanceResponse {

    private double sickUsed;
    private double sickRemaining;

    private double casualUsed;
    private double casualRemaining;

    private long permissionUsed;
    private long permissionRemaining;


    public LeaveBalanceResponse(
            double sickUsed,
            double casualUsed,
            long permissionUsed) {

        this.sickUsed = sickUsed;

        this.sickRemaining =
                Math.max(0, 1.0 - sickUsed);


        this.casualUsed = casualUsed;

        this.casualRemaining =
                Math.max(0, 1.0 - casualUsed);


        this.permissionUsed =
                permissionUsed;

        this.permissionRemaining =
                Math.max(0, 2 - permissionUsed);
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