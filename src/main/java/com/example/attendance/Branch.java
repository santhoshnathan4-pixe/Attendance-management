
package com.example.attendance;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_name", nullable = false, unique = true)
    private String branchName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "allowed_radius_meters", nullable = false)
    private Integer allowedRadiusMeters = 150;

    @Column(nullable = false)
    private Boolean active = true;

    // =========================
    // VERIFICATION SETTINGS
    // =========================

    @Column(name = "location_verification_enabled", nullable = false)
    private Boolean locationVerificationEnabled = false;

    @Column(name = "face_verification_enabled", nullable = false)
    private Boolean faceVerificationEnabled = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // =========================
    // GETTERS
    // =========================

    public Long getId() {
        return id;
    }

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

    public Boolean getActive() {
        return active;
    }

    public Boolean getLocationVerificationEnabled() {
        return locationVerificationEnabled;
    }

    public Boolean getFaceVerificationEnabled() {
        return faceVerificationEnabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    // =========================
    // SETTERS
    // =========================

    public void setId(Long id) {
        this.id = id;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setAllowedRadiusMeters(Integer allowedRadiusMeters) {
        this.allowedRadiusMeters = allowedRadiusMeters;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setLocationVerificationEnabled(
            Boolean locationVerificationEnabled) {

        this.locationVerificationEnabled = locationVerificationEnabled;
    }

    public void setFaceVerificationEnabled(
            Boolean faceVerificationEnabled) {

        this.faceVerificationEnabled = faceVerificationEnabled;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // =========================
    // AUTOMATIC TIMESTAMPS
    // =========================

    @PrePersist
    protected void onCreate() {

        OffsetDateTime now = OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = OffsetDateTime.now();
    }
}
