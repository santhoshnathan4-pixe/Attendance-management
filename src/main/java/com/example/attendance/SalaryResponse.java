package com.example.attendance;

public class SalaryResponse {

    private Integer employeeId;
    private String employeeCode;
    private String employeeName;

    private Double monthlySalary;
    private Integer workingDays;
    private Integer presentDays;
    private Double leaveDays;
    private Double lossOfPay;
    private Double finalSalary;

    public SalaryResponse(
            Integer employeeId,
            String employeeCode,
            String employeeName,
            Double monthlySalary,
            Integer workingDays,
            Integer presentDays,
            Double leaveDays,
            Double lossOfPay,
            Double finalSalary) {

        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.monthlySalary = monthlySalary;
        this.workingDays = workingDays;
        this.presentDays = presentDays;
        this.leaveDays = leaveDays;
        this.lossOfPay = lossOfPay;
        this.finalSalary = finalSalary;
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

    public Double getMonthlySalary() {
        return monthlySalary;
    }

    public Integer getWorkingDays() {
        return workingDays;
    }

    public Integer getPresentDays() {
        return presentDays;
    }

    public Double getLeaveDays() {
        return leaveDays;
    }

    public Double getLossOfPay() {
        return lossOfPay;
    }

    public Double getFinalSalary() {
        return finalSalary;
    }
}

