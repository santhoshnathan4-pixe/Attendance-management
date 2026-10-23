package com.example.attendance;

import java.time.LocalDate;

public class AdminEmployeeActionRequest {

    private String employeeCode;
    private String name;
    private String email;
    private String contactNumber;

    // Employee role
    private String role;

    // Employee technology
    private String technology;

    // Employee designation
    private String designation;

    // Employee branch
    private Long branchId;

    private Double salary;
    private String joiningDate;

    // Internship details
    private LocalDate internshipStartDate;
    private Integer internshipDurationMonths;
    private LocalDate internshipEndDate;
    private String stipendType;

    // Monthly stipend amount (saved in internship_stipend table)
    private Double stipendAmount;

    // Employment details
    private LocalDate employmentStartDate;
    private LocalDate employmentEndDate;

    // Admin verification details only
    private String adminEmail;
    private String adminPassword;

    // Reason for ADD / EDIT / DELETE
    private String reason;

    // SHIFT (optional; GENERAL when empty on a new employee)
    private String shiftType;

    // PASSWORD (optional: admin creates the first password; employee is asked to change it at first login)
    private String initialPassword;

    // EMPLOYEE CODE
    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    // NAME
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // EMAIL
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // CONTACT NUMBER
    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    // ROLE
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // TECHNOLOGY
    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    // DESIGNATION
    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    // BRANCH ID
    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    // SALARY
    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    // JOINING DATE
    public String getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    // INTERNSHIP START DATE
    public LocalDate getInternshipStartDate() {
        return internshipStartDate;
    }

    public void setInternshipStartDate(LocalDate internshipStartDate) {
        this.internshipStartDate = internshipStartDate;
    }

    // INTERNSHIP DURATION
    public Integer getInternshipDurationMonths() {
        return internshipDurationMonths;
    }

    public void setInternshipDurationMonths(Integer internshipDurationMonths) {
        this.internshipDurationMonths = internshipDurationMonths;
    }

    // INTERNSHIP END DATE
    public LocalDate getInternshipEndDate() {
        return internshipEndDate;
    }

    public void setInternshipEndDate(LocalDate internshipEndDate) {
        this.internshipEndDate = internshipEndDate;
    }

    // STIPEND TYPE
    public String getStipendType() {
        return stipendType;
    }

    public void setStipendType(String stipendType) {
        this.stipendType = stipendType;
    }

    // STIPEND AMOUNT
    public Double getStipendAmount() {
        return stipendAmount;
    }

    public void setStipendAmount(Double stipendAmount) {
        this.stipendAmount = stipendAmount;
    }

    // EMPLOYMENT START DATE
    public LocalDate getEmploymentStartDate() {
        return employmentStartDate;
    }

    public void setEmploymentStartDate(LocalDate employmentStartDate) {
        this.employmentStartDate = employmentStartDate;
    }

    // EMPLOYMENT END DATE
    public LocalDate getEmploymentEndDate() {
        return employmentEndDate;
    }

    public void setEmploymentEndDate(LocalDate employmentEndDate) {
        this.employmentEndDate = employmentEndDate;
    }

    // ADMIN EMAIL
    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    // ADMIN PASSWORD
    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    // REASON
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // SHIFT
    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    // INITIAL PASSWORD
    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }
}
