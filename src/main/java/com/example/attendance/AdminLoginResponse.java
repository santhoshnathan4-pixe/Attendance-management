package com.example.attendance;

public class AdminLoginResponse {

    private boolean success;
    private String message;
    private String adminName;
    private String email;

    public AdminLoginResponse() {
    }

    public AdminLoginResponse(
            boolean success,
            String message,
            String adminName,
            String email) {

        this.success = success;
        this.message = message;
        this.adminName = adminName;
        this.email = email;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getEmail() {
        return email;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}