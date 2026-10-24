package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeFaceRepository
        extends JpaRepository<EmployeeFace, Long> {

    Optional<EmployeeFace> findByEmployeeIdAndActiveTrue(
            Long employeeId);

    // Only the ids - never load the heavy face embeddings for a count.
    @Query("select f.employeeId from EmployeeFace f where f.active = true and f.employeeId is not null")
    List<Long> findActiveEmployeeIds();
}
