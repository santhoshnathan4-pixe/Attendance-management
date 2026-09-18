package com.example.attendance;

public class AdminEmployeeResponse {

    private Integer id;

    private String employeeCode;

    private String name;

    private String email;

    private String contactNumber;

    private String role;

    private Double salary;

    private String joiningDate;


    public AdminEmployeeResponse() {
    }


    public AdminEmployeeResponse(
            Integer id,
            String employeeCode,
            String name,
            String email,
            String contactNumber,
            String role,
            Double salary,
            String joiningDate) {

        this.id = id;
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.contactNumber = contactNumber;
        this.role = role;
        this.salary = salary;
        this.joiningDate = joiningDate;
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

    public Double getSalary() {
        return salary;
    }

    public String getJoiningDate() {
        return joiningDate;
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

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }
}