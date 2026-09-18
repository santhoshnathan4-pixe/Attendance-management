package com.example.attendance;

public class EmployeeDetailsResponse {

    private Integer id;

    private String employeeCode;

    private String name;

    private String email;

    private String role;

    private String joiningDate;

    private String contactNumber;


    public EmployeeDetailsResponse() {
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

    public String getRole() {
        return role;
    }

    public String getJoiningDate() {
        return joiningDate;
    }

    public String getContactNumber() {
        return contactNumber;
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

    public void setRole(String role) {
        this.role = role;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}