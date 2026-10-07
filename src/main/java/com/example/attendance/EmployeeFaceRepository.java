package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeFaceRepository
        extends JpaRepository<EmployeeFace, Long> {

    Optional<EmployeeFace> findByEmployeeIdAndActiveTrue(
            Long employeeId);
}