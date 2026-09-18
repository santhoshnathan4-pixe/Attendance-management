package com.example.attendance;

public class FirstEmployeePasswordRequest {

    private String email;

    private String newPassword;

    private String confirmPassword;


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
    // NEW PASSWORD
    // =========================================

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }


    // =========================================
    // CONFIRM PASSWORD
    // =========================================

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}