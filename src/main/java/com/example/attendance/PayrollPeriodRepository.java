package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollPeriodRepository
        extends JpaRepository<PayrollPeriod, Integer> {

    Optional<PayrollPeriod> findByPayYearAndPayMonth(
            Integer payYear,
            Integer payMonth);
}
