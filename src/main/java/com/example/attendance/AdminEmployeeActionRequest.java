
package com.example.attendance;

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

    // Admin verification details only
    private String adminEmail;
    private String adminPassword;

    // Reason for ADD / EDIT / DELETE
    private String reason;


    // =========================================
    // EMPLOYEE CODE
    // =========================================

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }


    // =========================================
    // NAME
    // =========================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // =========================================
    // EMAIL
    // =========================================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    // =========================================
    // CONTACT NUMBER
    // =========================================

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }


    // =========================================
    // ROLE
    // =========================================

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    // =========================================
    // TECHNOLOGY
    // =========================================

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }


    // =========================================
    // DESIGNATION
    // =========================================

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }


    // =========================================
    // BRANCH ID
    // =========================================

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }


    // =========================================
    // SALARY
    // =========================================

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }


    // =========================================
    // JOINING DATE
    // =========================================

    public String getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }


    // =========================================
    // ADMIN EMAIL
    // =========================================

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }


    // =========================================
    // ADMIN PASSWORD
    // =========================================

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }


    // =========================================
    // REASON
    // =========================================

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}

