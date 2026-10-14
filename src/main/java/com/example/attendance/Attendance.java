
package com.example.attendance;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    @Column(name = "check_in")
    private LocalTime checkIn;

    @Column(name = "check_out")
    private LocalTime checkOut;

    private String status;

    // =====================================================
    // MISSING CHECK-OUT REQUEST - EMPLOYEE SIDE
    // =====================================================

    @Column(name = "missing_checkout_requested_time")
    private LocalTime missingCheckoutRequestedTime;

    @Column(name = "missing_checkout_reason", columnDefinition = "TEXT")
    private String missingCheckoutReason;

    @Column(name = "missing_checkout_status")
    private String missingCheckoutStatus;

    @Column(name = "missing_checkout_submitted_at")
    private LocalDateTime missingCheckoutSubmittedAt;

    // =====================================================
    // MISSING CHECK-OUT REQUEST - ADMIN REVIEW
    // =====================================================

    @Column(name = "missing_checkout_reviewed_by")
    private String missingCheckoutReviewedBy;

    @Column(name = "missing_checkout_reviewed_at")
    private LocalDateTime missingCheckoutReviewedAt;

    @Column(name = "missing_checkout_admin_remark", columnDefinition = "TEXT")
    private String missingCheckoutAdminRemark;

    // =====================================================
    // EMPLOYEE APPROVAL NOTIFICATION ACKNOWLEDGEMENT
    // =====================================================

    @Column(name = "missing_checkout_employee_acknowledged")
    private Boolean missingCheckoutEmployeeAcknowledged = false;

    // =====================================================
    // EXISTING GETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public LocalTime getCheckIn() {
        return checkIn;
    }

    public LocalTime getCheckOut() {
        return checkOut;
    }

    public String getStatus() {
        return status;
    }

    // =====================================================
    // EXISTING SETTERS
    // =====================================================

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public void setCheckIn(LocalTime checkIn) {
        this.checkIn = checkIn;
    }

    public void setCheckOut(LocalTime checkOut) {
        this.checkOut = checkOut;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // =====================================================
    // MISSING CHECK-OUT GETTERS
    // =====================================================

    public LocalTime getMissingCheckoutRequestedTime() {
        return missingCheckoutRequestedTime;
    }

    public String getMissingCheckoutReason() {
        return missingCheckoutReason;
    }

    public String getMissingCheckoutStatus() {
        return missingCheckoutStatus;
    }

    public LocalDateTime getMissingCheckoutSubmittedAt() {
        return missingCheckoutSubmittedAt;
    }

    public String getMissingCheckoutReviewedBy() {
        return missingCheckoutReviewedBy;
    }

    public LocalDateTime getMissingCheckoutReviewedAt() {
        return missingCheckoutReviewedAt;
    }

    public String getMissingCheckoutAdminRemark() {
        return missingCheckoutAdminRemark;
    }

    // =====================================================
    // MISSING CHECK-OUT SETTERS
    // =====================================================

    public void setMissingCheckoutRequestedTime(
            LocalTime missingCheckoutRequestedTime) {
        this.missingCheckoutRequestedTime = missingCheckoutRequestedTime;
    }

    public void setMissingCheckoutReason(String missingCheckoutReason) {
        this.missingCheckoutReason = missingCheckoutReason;
    }

    public void setMissingCheckoutStatus(String missingCheckoutStatus) {
        this.missingCheckoutStatus = missingCheckoutStatus;
    }

    public void setMissingCheckoutSubmittedAt(
            LocalDateTime missingCheckoutSubmittedAt) {
        this.missingCheckoutSubmittedAt = missingCheckoutSubmittedAt;
    }

    public void setMissingCheckoutReviewedBy(String missingCheckoutReviewedBy) {
        this.missingCheckoutReviewedBy = missingCheckoutReviewedBy;
    }

    public void setMissingCheckoutReviewedAt(
            LocalDateTime missingCheckoutReviewedAt) {
        this.missingCheckoutReviewedAt = missingCheckoutReviewedAt;
    }

    public void setMissingCheckoutAdminRemark(
            String missingCheckoutAdminRemark) {
        this.missingCheckoutAdminRemark = missingCheckoutAdminRemark;
    }

    // =====================================================
    // EMPLOYEE ACKNOWLEDGEMENT GETTER / SETTER
    // =====================================================

    public Boolean getMissingCheckoutEmployeeAcknowledged() {
        return missingCheckoutEmployeeAcknowledged;
    }

    public void setMissingCheckoutEmployeeAcknowledged(
            Boolean missingCheckoutEmployeeAcknowledged) {
        this.missingCheckoutEmployeeAcknowledged = missingCheckoutEmployeeAcknowledged;
    }
}
