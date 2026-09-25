package com.example.attendance;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
            targetDate = LocalDate.now();
        } else {
            targetDate = LocalDate.parse(date);
        }

        Optional<WorkingDaySetting> setting =
                workingDaySettingRepository
                        .findBySettingDate(targetDate);

        return setting.orElse(null);
    }

    // =========================================
    // GET UPCOMING WORKING DAY SETTINGS
    // =========================================
    //
    // Employee should see a setting from the day
    // admin creates it until the setting date ends.
    //
    // Example:
    // Admin adds 29-09-2026 on 25-09-2026
    //
    // Employee will receive the alert from:
    // 25-09-2026
    //
    // Alert remains until:
    // 29-09-2026 night
    //
    // From:
    // 30-09-2026
    //
    // It will automatically disappear.
    // =========================================

    @GetMapping("/upcoming")
    public List<WorkingDaySetting> getUpcomingSettings(
            @RequestParam(required = false) String date) {

        LocalDate fromDate;

        if (date == null || date.trim().isEmpty()) {
            fromDate = LocalDate.now();
        } else {
            fromDate = LocalDate.parse(date);
        }

        LocalDate endDate =
                fromDate.plusMonths(1);

        return workingDaySettingRepository
                .findBySettingDateBetweenOrderBySettingDateAsc(
                        fromDate,
                        endDate
                );
    }
}