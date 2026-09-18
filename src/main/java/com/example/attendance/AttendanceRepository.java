package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    // Employee - specific date attendance
    Optional<Attendance> findByEmployeeIdAndAttendanceDate(
            Integer employeeId,
            LocalDate attendanceDate
    );

    // Employee - attendance history
    List<Attendance> findByEmployeeIdOrderByAttendanceDateDesc(
            Integer employeeId
    );

    // Monthly attendance report
    List<Attendance> findByAttendanceDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    // Admin - attendance for a specific date
    List<Attendance> findByAttendanceDateOrderByAttendanceDateDesc(
            LocalDate attendanceDate
    );
}