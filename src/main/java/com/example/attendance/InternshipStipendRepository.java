package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InternshipStipendRepository
        extends JpaRepository<InternshipStipend, Integer> {

    List<InternshipStipend> findByEmployeeIdOrderByStipendYearAscStipendMonthAsc(
            Integer employeeId
    );

    Optional<InternshipStipend> findByEmployeeIdAndStipendYearAndStipendMonth(
            Integer employeeId,
            Integer stipendYear,
            Integer stipendMonth
    );

    List<InternshipStipend> findByEmployeeIdAndStipendYearOrderByStipendMonthAsc(
            Integer employeeId,
            Integer stipendYear
    );
}