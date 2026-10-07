
package com.example.attendance;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(
        origins = {
                "https://attendance-management-73dqjuakh-3d-webinar.vercel.app",
                "https://attendance-management-3d-webinar.vercel.app"
        }
)
@RequestMapping("/employee/working-days")
public class EmployeeWorkingDayController {

    private final WorkingDaySettingRepository workingDaySettingRepository;

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    public EmployeeWorkingDayController(
            WorkingDaySettingRepository workingDaySettingRepository) {

        this.workingDaySettingRepository =
                workingDaySettingRepository;
    }

    // =========================================
    // GET SETTING FOR A SPECIFIC DATE
    // =========================================

    @GetMapping("/today")
    public WorkingDaySetting getTodaySetting(
            @RequestParam(required = false) String date) {

        LocalDate targetDate;

        if (date == null || date.trim().isEmpty()) {

            targetDate =
                    LocalDate.now(INDIA_ZONE);

        } else {

            targetDate =
                    LocalDate.parse(date);
        }

        Optional<WorkingDaySetting> setting =
                workingDaySettingRepository
                        .findBySettingDateAndActiveTrue(
                                targetDate
                        );

        return setting.orElse(null);
    }

    // =========================================
    // GET UPCOMING WORKING DAY SETTINGS
    // =========================================
    //
    // Employee Dashboard receives only ACTIVE
    // working-day settings.
    //
    // Settings are returned from the requested
    // date up to one month ahead.
    //
    // Example:
    //
    // Today       : 07-10-2026
    // Announcement: 09-10-2026
    // Holiday     : 10-10-2026
    //
    // Both active settings can be returned to
    // Employee Dashboard.
    //
    // Inactive / deleted settings are NOT returned.
    // =========================================

    @GetMapping("/upcoming")
    public List<WorkingDaySetting> getUpcomingSettings(
            @RequestParam(required = false) String date) {

        LocalDate fromDate;

        if (date == null || date.trim().isEmpty()) {

            fromDate =
                    LocalDate.now(INDIA_ZONE);

        } else {

            fromDate =
                    LocalDate.parse(date);
        }

        LocalDate endDate =
                fromDate.plusMonths(1);

        return workingDaySettingRepository
                .findBySettingDateBetweenAndActiveTrueOrderBySettingDateAsc(
                        fromDate,
                        endDate
                );
    }
}

