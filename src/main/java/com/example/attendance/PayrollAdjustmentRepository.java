package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollAdjustmentRepository
        extends JpaRepository<PayrollAdjustment, Integer> {

    List<PayrollAdjustment> findByPayYearAndPayMonth(
            Integer payYear,
            Integer payMonth);

    Optional<PayrollAdjustment> findByEmployeeIdAndPayYearAndPayMonth(
            Integer employeeId,
            Integer payYear,
            Integer payMonth);
}