package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftAssignmentRepository
        extends JpaRepository<ShiftAssignment, Long> {

    Optional<ShiftAssignment>
    findByAssignmentTypeAndRole(
            String assignmentType,
            String role);

    Optional<ShiftAssignment>
    findByAssignmentTypeAndEmployeeId(
            String assignmentType,
            Integer employeeId);
}