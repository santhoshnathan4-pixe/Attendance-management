
package com.example.attendance;

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
            String joiningDate) {

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

}
