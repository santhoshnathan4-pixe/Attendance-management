package com.example.attendance;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
    // GET TODAY'S WORKING DAY SETTING
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
}