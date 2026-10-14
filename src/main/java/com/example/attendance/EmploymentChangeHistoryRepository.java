
package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmploymentChangeHistoryRepository
        extends JpaRepository<EmploymentChangeHistory, Long> {

    List<EmploymentChangeHistory>
    findByEmployeeIdOrderByCreatedAtDesc(Integer employeeId);

    List<EmploymentChangeHistory>
    findByEmployeeIdAndEmployeeAcknowledgedFalseOrderByCreatedAtAsc(
            Integer employeeId
    );

    List<EmploymentChangeHistory>
    findByStatusAndEffectiveDateLessThanEqual(
            String status,
            java.time.LocalDate effectiveDate
    );
}
