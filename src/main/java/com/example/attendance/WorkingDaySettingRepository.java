
package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkingDaySettingRepository
        extends JpaRepository<WorkingDaySetting, Long> {

    Optional<WorkingDaySetting> findBySettingDate(
            LocalDate settingDate
    );

    List<WorkingDaySetting> findBySettingDateBetweenOrderBySettingDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );

    boolean existsBySettingDate(
            LocalDate settingDate
    );
}

