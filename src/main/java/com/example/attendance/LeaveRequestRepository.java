package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Integer> {

    List<LeaveRequest> findByEmployeeIdOrderByLeaveDateDesc(
            Integer employeeId
    );

    long countByEmployeeIdAndLeaveTypeAndLeaveDateBetween(
            Integer employeeId,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate
    );

    List<LeaveRequest> findByEmployeeIdAndLeaveTypeAndLeaveDateBetween(
            Integer employeeId,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate
    );

    // =========================================
    // FIND ALL LEAVE RECORDS FOR SAME DATE
    // =========================================

    List<LeaveRequest> findByEmployeeIdAndLeaveDate(
            Integer employeeId,
            LocalDate leaveDate
    );
}

