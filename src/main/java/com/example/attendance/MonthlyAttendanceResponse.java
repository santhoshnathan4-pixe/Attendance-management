package com.example.attendance;

public class MonthlyAttendanceResponse {

    private String employeeCode;
    private String employeeName;

    private int presentDays;
    private double sickLeave;
    private double casualLeave;
    private double halfDay;
    private int permission;
    private double absent;

    public MonthlyAttendanceResponse(
            String employeeCode,
            String employeeName,
            int presentDays,
            double sickLeave,
            double casualLeave,
            double halfDay,
            int permission,
            double absent) {

        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.presentDays = presentDays;
        this.sickLeave = sickLeave;
        this.casualLeave = casualLeave;
        this.halfDay = halfDay;
        this.permission = permission;
        this.absent = absent;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public int getPresentDays() {
        return presentDays;
    }

    public double getSickLeave() {
        return sickLeave;
    }

    public double getCasualLeave() {
        return casualLeave;
    }

    public double getHalfDay() {
        return halfDay;
    }

    public int getPermission() {
        return permission;
    }

    public double getAbsent() {
        return absent;
    }
}

