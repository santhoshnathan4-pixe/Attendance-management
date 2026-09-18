package com.example.attendance;

public class AdminEmployeePasswordRequest {

    private String newPassword;
    private String adminEmail;
    private String adminPassword;

    // =========================
    // GET NEW EMPLOYEE PASSWORD
    // =========================

    public String getNewPassword() {
        return newPassword;
    }

    // =========================
    // SET NEW EMPLOYEE PASSWORD
    // =========================

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    // =========================
    // GET ADMIN EMAIL
    // =========================

    public String getAdminEmail() {
        return adminEmail;
    }

    // =========================
    // SET ADMIN EMAIL
    // =========================

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    // =========================
    // GET ADMIN PASSWORD
    // =========================

    public String getAdminPassword() {
        return adminPassword;
    }

    // =========================
    // SET ADMIN PASSWORD
    // =========================

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}