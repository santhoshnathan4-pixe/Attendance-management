package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OvertimeRecordRepository
        extends JpaRepository<OvertimeRecord, Long> {

    // =====================================================
    // EMPLOYEE - SPECIFIC DATE
    // =====================================================

    Optional<OvertimeRecord> findByEmployeeIdAndOtDate(
            Integer employeeId,
            LocalDate otDate
    );


    // =====================================================
    // EMPLOYEE - OT HISTORY
    // =====================================================

    List<OvertimeRecord>
    findByEmployeeIdOrderByOtDateDesc(
            Integer employeeId
    );


    // =====================================================
    // MONTHLY OT REPORT
    // =====================================================

    List<OvertimeRecord>
    findByOtDateBetweenOrderByOtDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );


    // =====================================================
    // EMPLOYEE - MONTHLY OT
    // =====================================================

    List<OvertimeRecord>
    findByEmployeeIdAndOtDateBetweenOrderByOtDateAsc(
            Integer employeeId,
            LocalDate startDate,
            LocalDate endDate
    );


    // =====================================================
    // PENDING OT
    // =====================================================

    List<OvertimeRecord>
    findByStatusOrderByOtDateAsc(
            String status
    );


    // =====================================================
    // PENDING OT FOR SPECIFIC MONTH
    // =====================================================

    List<OvertimeRecord>
    findByStatusAndOtDateBetweenOrderByOtDateAsc(
            String status,
            LocalDate startDate,
            LocalDate endDate
    );
}