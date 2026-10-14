package com.example.attendance;

public class PayslipResponse {

    private Integer employeeId;
    private String employeeCode;
    private String employeeName;
    private String email;
    private String designation;
    private String technology;
    private String role;
    private String joiningDate;

    private int year;
    private int month;
    private String monthName;

    private int workingDays;
    private int presentDays;
    private double paidLeaveDays;
    private double autoCoveredDays;
    private double paidDays;
    private double lopDays;

    private double monthlySalary;
    private double perDaySalary;
    private double lossOfPay;
    private double salaryBeforeOvertime;

    private int overtimeDays;
    private double overtimeSalary;
    private double compensatoryOffDays;

    private double finalSalary;

    public PayslipResponse() {
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public int getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(int workingDays) {
        this.workingDays = workingDays;
    }

    public int getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(int presentDays) {
        this.presentDays = presentDays;
    }

    public double getPaidLeaveDays() {
        return paidLeaveDays;
    }

    public void setPaidLeaveDays(double paidLeaveDays) {
        this.paidLeaveDays = paidLeaveDays;
    }

    public double getAutoCoveredDays() {
        return autoCoveredDays;
    }

    public void setAutoCoveredDays(double autoCoveredDays) {
        this.autoCoveredDays = autoCoveredDays;
    }

    public double getPaidDays() {
        return paidDays;
    }

    public void setPaidDays(double paidDays) {
        this.paidDays = paidDays;
    }

    public double getLopDays() {
        return lopDays;
    }

    public void setLopDays(double lopDays) {
        this.lopDays = lopDays;
    }

    public double getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(double monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    public double getPerDaySalary() {
        return perDaySalary;
    }

    public void setPerDaySalary(double perDaySalary) {
        this.perDaySalary = perDaySalary;
    }

    public double getLossOfPay() {
        return lossOfPay;
    }

    public void setLossOfPay(double lossOfPay) {
        this.lossOfPay = lossOfPay;
    }

    public double getSalaryBeforeOvertime() {
        return salaryBeforeOvertime;
    }

    public void setSalaryBeforeOvertime(double salaryBeforeOvertime) {
        this.salaryBeforeOvertime = salaryBeforeOvertime;
    }

    public int getOvertimeDays() {
        return overtimeDays;
    }

    public void setOvertimeDays(int overtimeDays) {
        this.overtimeDays = overtimeDays;
    }

    public double getOvertimeSalary() {
        return overtimeSalary;
    }

    public void setOvertimeSalary(double overtimeSalary) {
        this.overtimeSalary = overtimeSalary;
    }

    public double getCompensatoryOffDays() {
        return compensatoryOffDays;
    }

    public void setCompensatoryOffDays(double compensatoryOffDays) {
        this.compensatoryOffDays = compensatoryOffDays;
    }

    public double getFinalSalary() {
        return finalSalary;
    }

    public void setFinalSalary(double finalSalary) {
        this.finalSalary = finalSalary;
    }
}