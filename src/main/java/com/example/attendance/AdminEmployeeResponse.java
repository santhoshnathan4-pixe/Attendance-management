
package com.example.attendance;

import java.time.LocalDate;

public class AdminEmployeeResponse {

    private Integer id;

    private String employeeCode;

    private String name;

    private String email;

    private String contactNumber;

    private String role;

    private String technology;

    private String designation;

    private Double salary;

    private String joiningDate;

    // Employee branch
    private Long branchId;

    private String branchName;

    // Internship details
    private LocalDate internshipStartDate;

    private Integer internshipDurationMonths;

    private LocalDate internshipEndDate;

    private String stipendType;

    // Employment details
    private LocalDate employmentStartDate;

    private LocalDate employmentEndDate;

    public AdminEmployeeResponse() {
    }

    public AdminEmployeeResponse(
            Integer id,
            String employeeCode,
            String name,
            String email,
            String contactNumber,
            String role,
            String technology,
            String designation,
            Double salary,
            String joiningDate,
            Long branchId,
            String branchName) {

        this.id = id;
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.contactNumber = contactNumber;
        this.role = role;
        this.technology = technology;
        this.designation = designation;
        this.salary = salary;
        this.joiningDate = joiningDate;
        this.branchId = branchId;
        this.branchName = branchName;
    }

    // =========================
    // GETTERS
    // =========================

    public Integer getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getRole() {
        return role;
    }

    public String getTechnology() {
        return technology;
    }

    public String getDesignation() {
        return designation;
    }

    public Double getSalary() {
        return salary;
    }

    public String getJoiningDate() {
        return joiningDate;
    }

    public Long getBranchId() {
        return branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public LocalDate getInternshipStartDate() {
        return internshipStartDate;
    }

    public Integer getInternshipDurationMonths() {
        return internshipDurationMonths;
    }

    public LocalDate getInternshipEndDate() {
        return internshipEndDate;
    }

    public String getStipendType() {
        return stipendType;
    }

    public LocalDate getEmploymentStartDate() {
        return employmentStartDate;
    }

    public LocalDate getEmploymentEndDate() {
        return employmentEndDate;
    }

    // =========================
    // SETTERS
    // =========================

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public void setInternshipStartDate(
            LocalDate internshipStartDate) {
        this.internshipStartDate = internshipStartDate;
    }

    public void setInternshipDurationMonths(
            Integer internshipDurationMonths) {
        this.internshipDurationMonths = internshipDurationMonths;
    }

    public void setInternshipEndDate(
            LocalDate internshipEndDate) {
        this.internshipEndDate = internshipEndDate;
    }

    public void setStipendType(String stipendType) {
        this.stipendType = stipendType;
    }

    public void setEmploymentStartDate(
            LocalDate employmentStartDate) {
        this.employmentStartDate = employmentStartDate;
    }

    public void setEmploymentEndDate(
            LocalDate employmentEndDate) {
        this.employmentEndDate = employmentEndDate;
    }

    private Double stipendAmount;

    public Double getStipendAmount() {
        return stipendAmount;
    }

    public void setStipendAmount(Double stipendAmount) {
        this.stipendAmount = stipendAmount;
    }
}
