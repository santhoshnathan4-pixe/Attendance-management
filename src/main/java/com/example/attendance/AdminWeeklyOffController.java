package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/weekly-off")
public class AdminWeeklyOffController {

    private final WeeklyOffSettingRepository weeklyOffSettingRepository;

    public AdminWeeklyOffController(
            WeeklyOffSettingRepository weeklyOffSettingRepository) {

        this.weeklyOffSettingRepository =
                weeklyOffSettingRepository;
    }


    // =========================
    // GET ACTIVE WEEKLY OFF DAYS
    // =========================

    @GetMapping
    public List<WeeklyOffSetting> getWeeklyOffDays() {

        return weeklyOffSettingRepository
                .findByActiveTrueOrderByDayOfWeekAsc();
    }


    // =========================
    // ADD / ACTIVATE WEEKLY OFF
    // =========================

    @PostMapping
    @Transactional
    public WeeklyOffSetting addWeeklyOff(
            @RequestParam String dayOfWeek) {

        if (dayOfWeek == null ||
                dayOfWeek.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Day of week is required"
            );
        }

        String cleanDay =
                dayOfWeek.trim().toUpperCase();

        if (!isValidDay(cleanDay)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid day of week"
            );
        }

        WeeklyOffSetting existing =
                weeklyOffSettingRepository
                        .findByDayOfWeek(cleanDay)
                        .orElse(null);

        LocalDateTime now =
                LocalDateTime.now();

        if (existing != null) {

            existing.setActive(true);
            existing.setUpdatedAt(now);

            return weeklyOffSettingRepository.save(existing);
        }

        WeeklyOffSetting setting =
                new WeeklyOffSetting();

        setting.setDayOfWeek(cleanDay);
        setting.setActive(true);
        setting.setCreatedAt(now);
        setting.setUpdatedAt(now);

        return weeklyOffSettingRepository.save(setting);
    }


    // =========================
    // REMOVE WEEKLY OFF
    // =========================

    @DeleteMapping
    @Transactional
    public String removeWeeklyOff(
            @RequestParam String dayOfWeek) {

        if (dayOfWeek == null ||
                dayOfWeek.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Day of week is required"
            );
        }

        String cleanDay =
                dayOfWeek.trim().toUpperCase();

        WeeklyOffSetting setting =
                weeklyOffSettingRepository
                        .findByDayOfWeek(cleanDay)
                        .orElse(null);

        if (setting == null ||
                !Boolean.TRUE.equals(setting.getActive())) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Weekly off day not found"
            );
        }

        setting.setActive(false);
        setting.setUpdatedAt(LocalDateTime.now());

        weeklyOffSettingRepository.save(setting);

        return "Weekly off removed successfully";
    }


    // =========================
    // VALIDATE DAY
    // =========================

    private boolean isValidDay(String day) {

        return day.equals("MONDAY")
                || day.equals("TUESDAY")
                || day.equals("WEDNESDAY")
                || day.equals("THURSDAY")
                || day.equals("FRIDAY")
                || day.equals("SATURDAY")
                || day.equals("SUNDAY");
    }
}