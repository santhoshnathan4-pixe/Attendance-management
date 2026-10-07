package com.example.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates the working dates for a month.
 *
 * Rules:
 *
 * 1. Weekly Off is controlled by the overall recurring
 *    weekly-off configuration.
 *
 * 2. If Sunday is configured as WEEKLY OFF,
 *    every Sunday is automatically a weekly off.
 *
 * 3. If Saturday is configured as WEEKLY OFF,
 *    every Saturday is automatically a weekly off.
 *
 * 4. Multiple weekly-off days are supported.
 *
 * 5. Manual date-specific settings can override
 *    the recurring weekly-off rule.
 *
 * 6. GOVERNMENT_HOLIDAY is a non-working day.
 *
 * 7. COMPANY_HOLIDAY is a non-working day.
 *
 * 8. WORKING_DAY can be used to make a specific
 *    date a working day even when its weekday is
 *    configured as a recurring weekly off.
 */
public final class WorkingDayCalculator {

    private WorkingDayCalculator() {
    }


    /**
     * Calculates working dates using:
     *
     * - date-specific settings
     * - overall recurring weekly-off days
     */
    public static List<LocalDate> workingDates(
            YearMonth yearMonth,
            Map<LocalDate, String> settingTypeByDate,
            List<String> recurringWeeklyOffDays) {

        List<LocalDate> result =
                new ArrayList<>();

        Set<DayOfWeek> weeklyOffDays =
                convertToDayOfWeekSet(
                        recurringWeeklyOffDays
                );

        for (int day = 1;
             day <= yearMonth.lengthOfMonth();
             day++) {

            LocalDate date =
                    yearMonth.atDay(day);

            String manualType =
                    settingTypeByDate.get(date);

            String type =
                    manualType == null
                            ? ""
                            : manualType.trim().toUpperCase();


            /*
             * Manual WORKING_DAY has priority.
             *
             * Example:
             * Sunday is normally weekly off,
             * but admin manually marks
             * 2026-10-04 as WORKING_DAY.
             *
             * That date remains a working day.
             */
            if ("WORKING_DAY".equals(type)) {

                result.add(date);

                continue;
            }


            /*
             * Manual holiday / weekly-off settings
             * always make the date non-working.
             */
            if ("WEEKLY_OFF".equals(type)
                    || "GOVERNMENT_HOLIDAY".equals(type)
                    || "COMPANY_HOLIDAY".equals(type)) {

                continue;
            }


            /*
             * No manual setting.
             *
             * Now check the overall recurring
             * weekly-off configuration.
             */
            if (weeklyOffDays.contains(
                    date.getDayOfWeek())) {

                continue;
            }


            /*
             * Normal working day.
             */
            result.add(date);
        }

        return result;
    }


    /**
     * Converts database weekday names such as:
     *
     * SUNDAY
     * SATURDAY
     * MONDAY
     *
     * into Java DayOfWeek values.
     */
    private static Set<DayOfWeek> convertToDayOfWeekSet(
            List<String> recurringWeeklyOffDays) {

        Set<DayOfWeek> result =
                new HashSet<>();

        if (recurringWeeklyOffDays == null) {
            return result;
        }

        for (String day :
                recurringWeeklyOffDays) {

            if (day == null ||
                    day.trim().isEmpty()) {

                continue;
            }

            try {

                result.add(
                        DayOfWeek.valueOf(
                                day.trim().toUpperCase()
                        )
                );

            } catch (IllegalArgumentException ignored) {

                /*
                 * Ignore invalid database values
                 * instead of breaking payroll.
                 */
            }
        }

        return result;
    }


    /**
     * Backward-compatible method.
     *
     * Existing code that has not yet been updated
     * to pass recurring weekly-off days can still
     * use this method.
     *
     * In this version there is no hard-coded
     * Saturday/Sunday rule.
     */
    public static List<LocalDate> workingDates(
            YearMonth yearMonth,
            Map<LocalDate, String> settingTypeByDate) {

        return workingDates(
                yearMonth,
                settingTypeByDate,
                List.of()
        );
    }
}