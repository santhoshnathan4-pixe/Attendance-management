package com.example.attendance;

public class AdminSalaryResponse {

    private Integer employeeId;
    private String employeeCode;
    private String employeeName;
    private Double salary;
    private String joiningDate;

    public AdminSalaryResponse(
            Integer employeeId,
            String employeeCode,
            String employeeName,
            Double salary,
            String joiningDate) {

        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.salary = salary;
        this.joiningDate = joiningDate;
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

    public Double getSalary() {
        return salary;
    }

    public String getJoiningDate() {
        return joiningDate;
    }
}