package com.example.attendance.growbytee.academy.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentCourseRepository
        extends JpaRepository<StudentCourse, Long> {

    List<StudentCourse> findByActiveTrueOrderByCourseNameAsc();

    Optional<StudentCourse> findByCourseNameIgnoreCase(
            String courseName
    );

    boolean existsByCourseNameIgnoreCase(
            String courseName
    );
}