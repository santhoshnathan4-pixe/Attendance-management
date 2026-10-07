
package com.example.attendance;

public class BranchActionRequest {

    private String branchName;

    private Double latitude;

    private Double longitude;

    private Integer allowedRadiusMeters;

    private Boolean locationVerificationEnabled;

    private Boolean faceVerificationEnabled;

    private String adminEmail;

    private String reason;

    // =========================
    // GETTERS
    // =========================

    public String getBranchName() {
        return branchName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getAllowedRadiusMeters() {
        return allowedRadiusMeters;
    }

    public Boolean getLocationVerificationEnabled() {
        return locationVerificationEnabled;
    }

    public Boolean getFaceVerificationEnabled() {
        return faceVerificationEnabled;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getReason() {
        return reason;
    }

    // =========================
    // SETTERS
    // =========================

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setAllowedRadiusMeters(
            Integer allowedRadiusMeters) {

        this.allowedRadiusMeters = allowedRadiusMeters;
    }

    public void setLocationVerificationEnabled(
            Boolean locationVerificationEnabled) {

        this.locationVerificationEnabled = locationVerificationEnabled;
    }

    public void setFaceVerificationEnabled(
            Boolean faceVerificationEnabled) {

        this.faceVerificationEnabled = faceVerificationEnabled;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
