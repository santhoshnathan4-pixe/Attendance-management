
package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdminActionHistoryRepository
                extends JpaRepository<AdminActionHistory, Integer> {

        List<AdminActionHistory> findAllByOrderByActionDateDescActionTimeDesc();

        List<AdminActionHistory> findByActionDateBetweenOrderByActionDateDescActionTimeDesc(
                        LocalDate startDate,
                        LocalDate endDate);

        // Find employee actions on or after the anniversary date.
        List<AdminActionHistory> findByEmployeeIdAndActionDateGreaterThanEqualOrderByActionDateDescActionTimeDesc(
                        Integer employeeId,
                        LocalDate anniversaryDate);
}
