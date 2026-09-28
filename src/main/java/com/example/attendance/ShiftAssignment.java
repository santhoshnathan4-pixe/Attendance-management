package com.example.attendance;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "shift_assignments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_shift_assignment_role",
            columnNames = {"assignment_type", "role"}
        ),
        @UniqueConstraint(
            name = "uk_shift_assignment_employee",
            columnNames = {"assignment_type", "employee_id"}
        )
    }
)
public class ShiftAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ROLE / EMPLOYEE
    @Column(name = "assignment_type", nullable = false)
    private String assignmentType;

    // Used when assignment type is ROLE
    @Column(name = "role")
    private String role;

    // Used when assignment type is EMPLOYEE
    @Column(name = "employee_id")
    private Integer employeeId;

    // GENERAL / SHIFT_1 / SHIFT_2 / SHIFT_3
    @Column(name = "shift_type", nullable = false)
    private String shiftType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getAssignmentType() {
        return assignmentType;
    }

    public String getRole() {
        return role;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getShiftType() {
        return shiftType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}