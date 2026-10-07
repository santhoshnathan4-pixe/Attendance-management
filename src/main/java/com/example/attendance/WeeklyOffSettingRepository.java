package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyOffSettingRepository
        extends JpaRepository<WeeklyOffSetting, Long> {

    Optional<WeeklyOffSetting> findByDayOfWeek(
            String dayOfWeek
    );

    Optional<WeeklyOffSetting> findByDayOfWeekAndActiveTrue(
            String dayOfWeek
    );

    List<WeeklyOffSetting>
    findByActiveTrueOrderByDayOfWeekAsc();

    boolean existsByDayOfWeekAndActiveTrue(
            String dayOfWeek
    );
}