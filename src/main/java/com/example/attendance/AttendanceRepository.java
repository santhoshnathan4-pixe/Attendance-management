
package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Integer> {

    // =====================================================
    // EMPLOYEE - SPECIFIC DATE ATTENDANCE
    //
    // Existing method preserved.
    // =====================================================

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(
            Integer employeeId,
            LocalDate attendanceDate
    );


    // =====================================================
    // EMPLOYEE - CURRENT OPEN ATTENDANCE SESSION
    //
    // Used for multiple Check-In / Check-Out.
    //
    // If Check-In is completed but Check-Out is not done,
    // this record will be returned.
    // =====================================================

    Optional<Attendance>
    findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(
            Integer employeeId,
            LocalDate attendanceDate
    );


    // =====================================================
    // EMPLOYEE - ATTENDANCE HISTORY
    //
    // Latest date first.
    // Within the same date, latest Check-In first.
    // =====================================================

    List<Attendance>
    findByEmployeeIdOrderByAttendanceDateDescCheckInDesc(
            Integer employeeId
    );


    // =====================================================
    // EXISTING EMPLOYEE ATTENDANCE HISTORY
    //
    // Kept for compatibility with existing code.
    // =====================================================

    List<Attendance> findByEmployeeIdOrderByAttendanceDateDesc(
            Integer employeeId
    );


    // =====================================================
    // MONTHLY ATTENDANCE REPORT
    // =====================================================

    List<Attendance> findByAttendanceDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );


    // =====================================================
    // ADMIN - ATTENDANCE FOR SPECIFIC DATE
    // =====================================================

    List<Attendance>
    findByAttendanceDateOrderByAttendanceDateDesc(
            LocalDate attendanceDate
    );
}

