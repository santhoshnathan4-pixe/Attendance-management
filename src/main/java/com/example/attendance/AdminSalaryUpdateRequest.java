
package com.example.attendance;

public class AdminSalaryUpdateRequest {

    private Double newSalary;

    private String adminEmail;

    private String reason;


    public Double getNewSalary() {
        return newSalary;
    }

    public void setNewSalary(Double newSalary) {
        this.newSalary = newSalary;
    }


    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }


    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

