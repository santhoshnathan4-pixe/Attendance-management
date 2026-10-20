package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Month calendar for the employee dashboard.
 *
 * Returns, for every date of the month that is NOT a normal working day:
 * WEEKLY_OFF (recurring weekly off or a dated weekly off), COMPANY_HOLIDAY,
 * GOVERNMENT_HOLIDAY, or WORKING_DAY (a date that is forced to be a working
 * day).
 *
 * Same rules as the payroll / working day calculation, so the calendar and the
 * salary always agree. The leave shown on the calendar is read separately and
 * is
 * only the logged-in employee's own.
 */
@RestController
@RequestMapping("/employee/calendar")
@CrossOrigin(origins = {
        "https://attendance-management-73dqjuakh-3d-webinar.vercel.app",
        "https://attendance-management-3d-webinar.vercel.app"
})
public class EmployeeCalendarController {

    private final WorkingDaySettingRepository workingDaySettingRepository;
    private final WeeklyOffSettingRepository weeklyOffSettingRepository;

    public EmployeeCalendarController(
            WorkingDaySettingRepository workingDaySettingRepository,
            WeeklyOffSettingRepository weeklyOffSettingRepository) {

        this.workingDaySettingRepository = workingDaySettingRepository;
        this.weeklyOffSettingRepository = weeklyOffSettingRepository;
    }

    public record CalendarDay(
            String settingDate,
            String settingType,
            String reason) {
    }

    @GetMapping
    public List<CalendarDay> month(
            @RequestParam int year,
            @RequestParam int month) {

        if (year < 2000 || year > 2100 || month < 1 || month > 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid year and month are required");
        }

        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        Map<LocalDate, WorkingDaySetting> settingByDate = new HashMap<>();

        for (WorkingDaySetting setting : workingDaySettingRepository
                .findBySettingDateBetweenAndActiveTrueOrderBySettingDateAsc(start, end)) {

            if (setting != null
                    && setting.getSettingDate() != null
                    && setting.getSettingType() != null) {

                settingByDate.put(setting.getSettingDate(), setting);
            }
        }

        List<String> recurringWeeklyOffDays = weeklyOffSettingRepository
                .findByActiveTrueOrderByDayOfWeekAsc()
                .stream()
                .filter(setting -> setting != null && setting.getDayOfWeek() != null)
                .map(setting -> setting.getDayOfWeek())
                .toList();

        List<CalendarDay> days = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            WorkingDaySetting setting = settingByDate.get(date);

            if (setting != null) {

                String type = setting.getSettingType().trim().toUpperCase();

                if (type.equals("WORKING_SATURDAY")) {
                    type = "WORKING_DAY";
                }

                days.add(new CalendarDay(
                        date.toString(),
                        type,
                        reasonOf(setting)));

                continue;
            }

            if (isRecurringOff(date.getDayOfWeek(), recurringWeeklyOffDays)) {

                days.add(new CalendarDay(
                        date.toString(),
                        "WEEKLY_OFF",
                        ""));
            }
        }

        return days;
    }

    private boolean isRecurringOff(DayOfWeek day, List<String> recurring) {

        String full = day.name();
        String shortName = full.substring(0, 3);

        for (String value : recurring) {

            String text = value.trim();

            if (text.equalsIgnoreCase(full)
                    || (text.length() == 3 && text.equalsIgnoreCase(shortName))) {
                return true;
            }
        }

        return false;
    }

    // The reason / message of the setting (read by name so this file does not
    // depend on the entity layout)
    private String reasonOf(WorkingDaySetting setting) {

        try {

            Object value = setting.getClass().getMethod("getReason").invoke(setting);

            return value == null ? "" : value.toString();

        } catch (Exception exception) {

            return "";
        }
    }
}