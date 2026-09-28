package com.example.attendance.growbytee.academy.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentBatchRepository
        extends JpaRepository<StudentBatch, Long> {

    List<StudentBatch> findAllByOrderByBatchNameAsc();

    List<StudentBatch> findByActiveTrueOrderByBatchNameAsc();

    List<StudentBatch> findByCourseIdOrderByBatchNameAsc(
            Long courseId
    );

    Optional<StudentBatch> findByBatchNameIgnoreCase(
            String batchName
    );

    boolean existsByBatchNameIgnoreCase(
            String batchName
    );
}