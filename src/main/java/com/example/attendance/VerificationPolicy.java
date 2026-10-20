
package com.example.attendance;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "verification_policies")
public class VerificationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_name", nullable = false, length = 150)
    private String policyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "target_value", length = 255)
    private String targetValue;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "location_verification_enabled", nullable = false)
    private Boolean locationVerificationEnabled = false;

    @Column(name = "face_verification_enabled", nullable = false)
    private Boolean faceVerificationEnabled = false;

    @Column(name = "location_capture_enabled", nullable = false)
    private Boolean locationCaptureEnabled = true;

    @Column(name = "photo_capture_enabled", nullable = false)
    private Boolean photoCaptureEnabled = false;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by_email", nullable = false, length = 255)
    private String createdByEmail;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public Branch getBranch() {
        return branch;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Boolean getLocationVerificationEnabled() {
        return locationVerificationEnabled;
    }

    public Boolean getFaceVerificationEnabled() {
        return faceVerificationEnabled;
    }

    public Boolean getLocationCaptureEnabled() {
        return locationCaptureEnabled;
    }

    public Boolean getPhotoCaptureEnabled() {
        return photoCaptureEnabled;
    }

    public Boolean getActive() {
        return active;
    }

    public String getReason() {
        return reason;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public void setLocationVerificationEnabled(
            Boolean locationVerificationEnabled) {
        this.locationVerificationEnabled = locationVerificationEnabled;
    }

    public void setFaceVerificationEnabled(
            Boolean faceVerificationEnabled) {
        this.faceVerificationEnabled = faceVerificationEnabled;
    }

    public void setLocationCaptureEnabled(Boolean locationCaptureEnabled) {
        this.locationCaptureEnabled = locationCaptureEnabled;
    }

    public void setPhotoCaptureEnabled(Boolean photoCaptureEnabled) {
        this.photoCaptureEnabled = photoCaptureEnabled;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
