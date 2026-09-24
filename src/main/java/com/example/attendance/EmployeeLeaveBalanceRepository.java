package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface EmployeeLeaveBalanceRepository
        extends JpaRepository<EmployeeLeaveBalance, Long> {

    Optional<EmployeeLeaveBalance>
    findByEmployeeIdAndBalanceMonth(
            Integer employeeId,
            LocalDate balanceMonth
    );
}

