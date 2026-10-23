package com.example.attendance;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_code")
    private String employeeCode;

    private String name;

    private String email;

    private String password;

    @Column(name = "employee_type")
    private String employeeType;

    private String role;

    private String technology;

    private String designation;

    private String course;

    private Double salary;

    @Column(name = "joining_date")
    private String joiningDate;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // =========================
    // INTERNSHIP DETAILS
    // =========================

    @Column(name = "internship_start_date")
    private LocalDate internshipStartDate;

    @Column(name = "internship_duration_months")
    private Integer internshipDurationMonths;

    @Column(name = "internship_end_date")
    private LocalDate internshipEndDate;

    @Column(name = "stipend_type")
    private String stipendType;

    // =========================
    // EMPLOYMENT DETAILS
    // =========================

    @Column(name = "employment_start_date")
    private LocalDate employmentStartDate;

    @Column(name = "employment_end_date")
    private LocalDate employmentEndDate;

    // =========================
    // LAST ADMIN ACTION (reason, who, date and time)
    // Updated on the SAME row for Add, Edit, Delete and every Review.
    // =========================

    @Column(name = "last_action_type")
    private String lastActionType;

    @Column(name = "last_action_reason", columnDefinition = "TEXT")
    private String lastActionReason;

    @Column(name = "last_action_by")
    private String lastActionBy;

    @Column(name = "last_action_by_email")
    private String lastActionByEmail;

    @Column(name = "last_action_at")
    private OffsetDateTime lastActionAt;

    // =========================
    // BRANCH
    // =========================

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id")
    private Branch branch;


    // =========================
    // PASSWORD SAFETY
    // =========================

    /** True while the employee still uses the password the admin created. */
    @Column(name = "password_change_pending")
    private Boolean passwordChangePending = false;

    /** Forgot-password code (stored only as a hash) and its expiry. */
    @Column(name = "reset_otp_hash")
    private String resetOtpHash;

    @Column(name = "reset_otp_expires_at")
    private java.time.LocalDateTime resetOtpExpiresAt;

    @Column(name = "reset_otp_attempts")
    private Integer resetOtpAttempts = 0;

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

    public String getPassword() {
        return password;
    }

    public String getEmployeeType() {
        return employeeType;
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

    public String getCourse() {
        return course;
    }

    public Double getSalary() {
        return salary;
    }

    public String getJoiningDate() {
        return joiningDate;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public Boolean getActive() {
        return active;
    }

    public Branch getBranch() {
        return branch;
    }

    public LocalDate getInternshipStartDate() {
        return internshipStartDate;
    }

    public Integer getInternshipDurationMonths() {
        return internshipDurationMonths;
    }

    public LocalDate getInternshipEndDate() {
        return internshipEndDate;
    }

    public String getStipendType() {
        return stipendType;
    }

    public LocalDate getEmploymentStartDate() {
        return employmentStartDate;
    }

    public LocalDate getEmploymentEndDate() {
        return employmentEndDate;
    }

    public String getLastActionType() {
        return lastActionType;
    }

    public String getLastActionReason() {
        return lastActionReason;
    }

    public String getLastActionBy() {
        return lastActionBy;
    }

    public String getLastActionByEmail() {
        return lastActionByEmail;
    }

    public OffsetDateTime getLastActionAt() {
        return lastActionAt;
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

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmployeeType(String employeeType) {
        this.employeeType = employeeType;
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

    public void setCourse(String course) {
        this.course = course;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public void setInternshipStartDate(LocalDate internshipStartDate) {
        this.internshipStartDate = internshipStartDate;
    }

    public void setInternshipDurationMonths(Integer internshipDurationMonths) {
        this.internshipDurationMonths = internshipDurationMonths;
    }

    public void setInternshipEndDate(LocalDate internshipEndDate) {
        this.internshipEndDate = internshipEndDate;
    }

    public void setStipendType(String stipendType) {
        this.stipendType = stipendType;
    }

    public void setEmploymentStartDate(LocalDate employmentStartDate) {
        this.employmentStartDate = employmentStartDate;
    }

    public void setEmploymentEndDate(LocalDate employmentEndDate) {
        this.employmentEndDate = employmentEndDate;
    }

    public void setLastActionType(String lastActionType) {
        this.lastActionType = lastActionType;
    }

    public void setLastActionReason(String lastActionReason) {
        this.lastActionReason = lastActionReason;
    }

    public void setLastActionBy(String lastActionBy) {
        this.lastActionBy = lastActionBy;
    }

    public void setLastActionByEmail(String lastActionByEmail) {
        this.lastActionByEmail = lastActionByEmail;
    }

    public void setLastActionAt(OffsetDateTime lastActionAt) {
        this.lastActionAt = lastActionAt;
    }

    public boolean isPasswordChangePending() {
        return Boolean.TRUE.equals(passwordChangePending);
    }

    public void setPasswordChangePending(Boolean passwordChangePending) {
        this.passwordChangePending = passwordChangePending;
    }

    public String getResetOtpHash() {
        return resetOtpHash;
    }

    public void setResetOtpHash(String resetOtpHash) {
        this.resetOtpHash = resetOtpHash;
    }

    public java.time.LocalDateTime getResetOtpExpiresAt() {
        return resetOtpExpiresAt;
    }

    public void setResetOtpExpiresAt(java.time.LocalDateTime resetOtpExpiresAt) {
        this.resetOtpExpiresAt = resetOtpExpiresAt;
    }

    public int getResetOtpAttempts() {
        return resetOtpAttempts == null ? 0 : resetOtpAttempts;
    }

    public void setResetOtpAttempts(Integer resetOtpAttempts) {
        this.resetOtpAttempts = resetOtpAttempts;
    }
}
