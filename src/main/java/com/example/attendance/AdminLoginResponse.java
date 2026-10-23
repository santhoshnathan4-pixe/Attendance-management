package com.example.attendance;

public class AdminLoginResponse {

    private boolean success;
    private String message;
    private String adminName;
    private String email;
    private String role;

    public AdminLoginResponse() {
    }

    public AdminLoginResponse(
            boolean success,
            String message,
            String adminName,
            String email) {

        this(success, message, adminName, email, null);
    }

    public AdminLoginResponse(
            boolean success,
            String message,
            String adminName,
            String email,
            String role) {

        this.success = success;
        this.message = message;
        this.adminName = adminName;
        this.email = email;
        this.role = role;
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

    public String getRole() {
        return role;
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

    public void setRole(String role) {
        this.role = role;
    }
}
