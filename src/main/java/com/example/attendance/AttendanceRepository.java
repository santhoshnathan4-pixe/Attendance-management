package com.example.attendance;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
                        LocalDate attendanceDate);

        // =====================================================
        // EMPLOYEE - CURRENT OPEN ATTENDANCE SESSION
        //
        // Used for multiple Check-In / Check-Out.
        //
        // If Check-In is completed but Check-Out is not done,
        // this record will be returned.
        // =====================================================

        Optional<Attendance> findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(
                        Integer employeeId,
                        LocalDate attendanceDate);

        // =====================================================
        // EMPLOYEE - ATTENDANCE HISTORY
        //
        // Latest date first.
        // Within the same date, latest Check-In first.
        // =====================================================

        List<Attendance> findByEmployeeIdOrderByAttendanceDateDescCheckInDesc(
                        Integer employeeId);

        // =====================================================
        // EXISTING EMPLOYEE ATTENDANCE HISTORY
        //
        // Kept for compatibility with existing code.
        // =====================================================

        List<Attendance> findByEmployeeIdOrderByAttendanceDateDesc(
                        Integer employeeId);

        // =====================================================
        // MONTHLY ATTENDANCE REPORT
        // =====================================================

        List<Attendance> findByAttendanceDateBetween(
                        LocalDate startDate,
                        LocalDate endDate);

        // =====================================================
        // ADMIN - ATTENDANCE FOR SPECIFIC DATE
        // =====================================================

        List<Attendance> findByAttendanceDateOrderByAttendanceDateDesc(
                        LocalDate attendanceDate);

        // =====================================================
        // EMPLOYEE - SELECTED MONTH ATTENDANCE HISTORY
        //
        // Fetches attendance for one employee within the
        // selected month's date range.
        //
        // Latest date first.
        // Within the same date, latest Check-In first.
        //
        // Existing methods are preserved.
        // =====================================================

        List<Attendance> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDescCheckInDesc(
                        Integer employeeId,
                        LocalDate startDate,
                        LocalDate endDate);

        // =====================================================
        // NEW - MAXIMUM 2 SESSIONS PER DAY
        //
        // Number of sessions the employee already has
        // on the given date.
        // =====================================================

        long countByEmployeeIdAndAttendanceDate(
                        Integer employeeId,
                        LocalDate attendanceDate);

        // =====================================================
        // NEW - MISSING CHECK-OUT POPUP (EMPLOYEE SIDE)
        //
        // Oldest PAST session with no Check-Out and no request.
        //
        // A request that is PENDING / APPROVED / REJECTED has a
        // status, so it is not returned again. This prevents
        // duplicate requests for the same attendance session.
        // =====================================================

        Optional<Attendance> findFirstByEmployeeIdAndAttendanceDateBeforeAndCheckOutIsNullAndMissingCheckoutStatusIsNullOrderByAttendanceDateAscCheckInAsc(
                        Integer employeeId,
                        LocalDate beforeDate);

        // =====================================================
        // NEW - OWNERSHIP CHECK
        //
        // The attendance row must belong to this employee.
        // =====================================================

        Optional<Attendance> findByIdAndEmployeeId(
                        Integer id,
                        Integer employeeId);

        // =====================================================
        // NEW - ADMIN LIST BY REQUEST STATUS
        //
        // PENDING / APPROVED / REJECTED.
        // Newest request first.
        // =====================================================

        List<Attendance> findByMissingCheckoutStatusOrderByMissingCheckoutSubmittedAtDesc(
                        String missingCheckoutStatus);

        // =====================================================
        // EMPLOYEE - APPROVED MISSING CHECK-OUT NOTIFICATIONS
        //
        // Only approved requests that the employee has not yet
        // acknowledged are returned.
        // =====================================================

        List<Attendance> findByEmployeeIdAndMissingCheckoutStatusAndMissingCheckoutEmployeeAcknowledgedFalseOrderByMissingCheckoutReviewedAtDesc(
                        Integer employeeId,
                        String missingCheckoutStatus);

        // =====================================================
        // NEW - ROW LOCK
        //
        // Stops two simultaneous submits or two simultaneous
        // approvals from updating the same attendance record
        // twice. Use inside a @Transactional method.
        // =====================================================

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select a from Attendance a where a.id = :id")
        Optional<Attendance> findByIdForUpdate(
                        @Param("id") Integer id);

}