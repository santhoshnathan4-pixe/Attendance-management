package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/working-days")
public class AdminWorkingDayController {

    private final WorkingDaySettingRepository workingDaySettingRepository;
    private final WeeklyOffSettingRepository weeklyOffSettingRepository;
    private final AdminActionHistoryRepository historyRepository;

    public AdminWorkingDayController(
            WorkingDaySettingRepository workingDaySettingRepository,
            WeeklyOffSettingRepository weeklyOffSettingRepository,
            AdminActionHistoryRepository historyRepository) {

        this.workingDaySettingRepository =
                workingDaySettingRepository;

        this.weeklyOffSettingRepository =
                weeklyOffSettingRepository;

        this.historyRepository =
                historyRepository;
    }


    // =========================================================
    // DATE-SPECIFIC WORKING DAY SETTINGS
    // =========================================================

    @GetMapping
    public List<WorkingDaySetting> getWorkingDaySettings(
            @RequestParam int year,
            @RequestParam int month) {

        LocalDate startDate =
                LocalDate.of(year, month, 1);

        LocalDate endDate =
                startDate.withDayOfMonth(
                        startDate.lengthOfMonth()
                );

        return workingDaySettingRepository
                .findBySettingDateBetweenAndActiveTrueOrderBySettingDateAsc(
                        startDate,
                        endDate
                );
    }


    @PostMapping
    @Transactional
    public WorkingDaySetting saveWorkingDaySetting(
            @RequestParam LocalDate settingDate,
            @RequestParam String settingType,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String adminName) {

        if (settingDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Date is required"
            );
        }

        if (settingType == null ||
                settingType.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Setting type is required"
            );
        }

        String type =
                settingType.trim().toUpperCase();

        if (!type.equals("GOVERNMENT_HOLIDAY") &&
                !type.equals("COMPANY_HOLIDAY") &&
                !type.equals("WORKING_DAY") &&
                !type.equals("WEEKLY_OFF")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid setting type"
            );
        }

        if (reason == null ||
                reason.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reason is required"
            );
        }

        String cleanReason =
                reason.trim();

        String cleanAdminName =
                adminName != null &&
                !adminName.trim().isEmpty()
                        ? adminName.trim()
                        : "Admin";

        LocalDateTime now =
                LocalDateTime.now();

        WorkingDaySetting existingSetting =
                workingDaySettingRepository
                        .findBySettingDateAndActiveTrue(settingDate)
                        .orElse(null);

        boolean isUpdate =
                existingSetting != null;

        String oldType =
                isUpdate
                        ? existingSetting.getSettingType()
                        : null;

        String oldReason =
                isUpdate
                        ? existingSetting.getReason()
                        : null;

        if (existingSetting != null) {

            existingSetting.setActive(false);
            existingSetting.setUpdatedAt(now);

            workingDaySettingRepository.save(
                    existingSetting
            );
        }

        WorkingDaySetting newSetting =
                new WorkingDaySetting();

        newSetting.setSettingDate(settingDate);
        newSetting.setSettingType(type);
        newSetting.setReason(cleanReason);
        newSetting.setActive(true);
        newSetting.setCreatedAt(now);
        newSetting.setUpdatedAt(now);

        WorkingDaySetting savedSetting =
                workingDaySettingRepository.save(
                        newSetting
                );

        AdminActionHistory history =
                new AdminActionHistory();

        history.setAdminName(cleanAdminName);

        history.setAction(
                isUpdate
                        ? "WORKING_DAY_UPDATE"
                        : "WORKING_DAY_ADD"
        );

        history.setActionDate(LocalDate.now());
        history.setActionTime(LocalTime.now());
        history.setFieldName("Working Day Setting");

        if (isUpdate) {

            history.setOldValue(
                    "Date: " +
                    settingDate +
                    ", Type: " +
                    (oldType == null ? "-" : oldType) +
                    ", Reason: " +
                    (oldReason == null ? "-" : oldReason)
            );

        } else {

            history.setOldValue("-");
        }

        history.setNewValue(
                "Date: " +
                settingDate +
                ", Type: " +
                type +
                ", Reason: " +
                cleanReason
        );

        history.setReason(cleanReason);

        historyRepository.save(history);

        return savedSetting;
    }


    // =========================================================
    // LEGACY DATE-SPECIFIC SUNDAY BULK SETTING
    // =========================================================
    //
    // Existing functionality is preserved.
    // The new recurring Weekly Off feature does NOT depend
    // on this endpoint.
    //

    @PostMapping("/sundays")
    @Transactional
    public List<WorkingDaySetting> applySundays(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String adminName) {

        if (month < 1 || month > 12) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid month"
            );
        }

        LocalDate startDate =
                LocalDate.of(year, month, 1);

        LocalDate endDate =
                startDate.withDayOfMonth(
                        startDate.lengthOfMonth()
                );

        String cleanReason =
                reason != null &&
                !reason.trim().isEmpty()
                        ? reason.trim()
                        : "Weekly Off - Sunday";

        String cleanAdminName =
                adminName != null &&
                !adminName.trim().isEmpty()
                        ? adminName.trim()
                        : "Admin";

        LocalDateTime now =
                LocalDateTime.now();

        List<WorkingDaySetting> savedSettings =
                new ArrayList<>();

        LocalDate currentDate =
                startDate;

        while (!currentDate.isAfter(endDate)) {

            if (currentDate.getDayOfWeek()
                    .getValue() == 7) {

                WorkingDaySetting existingSetting =
                        workingDaySettingRepository
                                .findBySettingDateAndActiveTrue(
                                        currentDate
                                )
                                .orElse(null);

                boolean isUpdate =
                        existingSetting != null;

                String oldType =
                        isUpdate
                                ? existingSetting.getSettingType()
                                : null;

                String oldReason =
                        isUpdate
                                ? existingSetting.getReason()
                                : null;

                if (isUpdate &&
                        "WEEKLY_OFF".equalsIgnoreCase(
                                oldType
                        )) {

                    savedSettings.add(existingSetting);

                    currentDate =
                            currentDate.plusDays(1);

                    continue;
                }

                if (existingSetting != null) {

                    existingSetting.setActive(false);
                    existingSetting.setUpdatedAt(now);

                    workingDaySettingRepository.save(
                            existingSetting
                    );
                }

                WorkingDaySetting newSetting =
                        new WorkingDaySetting();

                newSetting.setSettingDate(currentDate);
                newSetting.setSettingType("WEEKLY_OFF");
                newSetting.setReason(cleanReason);
                newSetting.setActive(true);
                newSetting.setCreatedAt(now);
                newSetting.setUpdatedAt(now);

                WorkingDaySetting savedSetting =
                        workingDaySettingRepository.save(
                                newSetting
                        );

                savedSettings.add(savedSetting);

                AdminActionHistory history =
                        new AdminActionHistory();

                history.setAdminName(cleanAdminName);

                history.setAction(
                        isUpdate
                                ? "WEEKLY_OFF_UPDATE"
                                : "WEEKLY_OFF_ADD"
                );

                history.setActionDate(LocalDate.now());
                history.setActionTime(LocalTime.now());
                history.setFieldName("Weekly Off");

                if (isUpdate) {

                    history.setOldValue(
                            "Date: " +
                            currentDate +
                            ", Type: " +
                            (oldType == null
                                    ? "-"
                                    : oldType) +
                            ", Reason: " +
                            (oldReason == null
                                    ? "-"
                                    : oldReason)
                    );

                } else {

                    history.setOldValue("-");
                }

                history.setNewValue(
                        "Date: " +
                        currentDate +
                        ", Type: WEEKLY_OFF" +
                        ", Reason: " +
                        cleanReason
                );

                history.setReason(cleanReason);

                historyRepository.save(history);
            }

            currentDate =
                    currentDate.plusDays(1);
        }

        return savedSettings;
    }


    // =========================================================
    // OVERALL RECURRING WEEKLY OFF
    // =========================================================

    /**
     * Returns all currently active recurring weekly-off days.
     *
     * Example:
     * SUNDAY
     * SATURDAY
     */
    @GetMapping("/weekly-offs")
    public List<WeeklyOffSetting> getWeeklyOffSettings() {

        return weeklyOffSettingRepository
                .findByActiveTrueOrderByDayOfWeekAsc();
    }


    /**
     * Add or activate one recurring weekly-off day.
     *
     * Example:
     * SUNDAY
     * SATURDAY
     */
    @PostMapping("/weekly-offs")
    @Transactional
    public WeeklyOffSetting addWeeklyOff(
            @RequestParam String dayOfWeek,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String adminName) {

        if (dayOfWeek == null ||
                dayOfWeek.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Day of week is required"
            );
        }

        String cleanDay =
                dayOfWeek.trim().toUpperCase();

        validateDayOfWeek(cleanDay);

        if (reason == null ||
                reason.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reason is required"
            );
        }

        String cleanReason =
                reason.trim();

        String cleanAdminName =
                adminName != null &&
                !adminName.trim().isEmpty()
                        ? adminName.trim()
                        : "Admin";

        LocalDateTime now =
                LocalDateTime.now();

        WeeklyOffSetting setting =
                weeklyOffSettingRepository
                        .findByDayOfWeek(cleanDay)
                        .orElse(null);

        boolean isUpdate =
                setting != null;

        if (setting == null) {

            setting =
                    new WeeklyOffSetting();

            setting.setDayOfWeek(cleanDay);
            setting.setCreatedAt(now);

        } else {

            if (Boolean.TRUE.equals(
                    setting.getActive())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        cleanDay +
                        " is already configured as Weekly Off"
                );
            }
        }

        setting.setActive(true);
        setting.setUpdatedAt(now);

        WeeklyOffSetting savedSetting =
                weeklyOffSettingRepository.save(setting);


        // =========================
        // ACTION HISTORY
        // =========================

        AdminActionHistory history =
                new AdminActionHistory();

        history.setAdminName(cleanAdminName);

        history.setAction(
                isUpdate
                        ? "WEEKLY_OFF_REACTIVATE"
                        : "WEEKLY_OFF_ADD"
        );

        history.setActionDate(LocalDate.now());
        history.setActionTime(LocalTime.now());
        history.setFieldName("Recurring Weekly Off");

        history.setOldValue(
                isUpdate
                        ? "Day: " +
                          cleanDay +
                          ", Active: false"
                        : "-"
        );

        history.setNewValue(
                "Day: " +
                cleanDay +
                ", Active: true"
        );

        history.setReason(cleanReason);

        historyRepository.save(history);

        return savedSetting;
    }


    /**
     * Remove one recurring weekly-off day.
     *
     * Example:
     * DELETE /admin/working-days/weekly-offs/SATURDAY
     *
     * This does not delete the database row.
     * It marks the configuration inactive so history is preserved.
     */
    @DeleteMapping("/weekly-offs/{dayOfWeek}")
    @Transactional
    public String removeWeeklyOff(
            @PathVariable String dayOfWeek,
            @RequestParam String reason,
            @RequestParam(required = false) String adminName) {

        if (dayOfWeek == null ||
                dayOfWeek.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Day of week is required"
            );
        }

        String cleanDay =
                dayOfWeek.trim().toUpperCase();

        validateDayOfWeek(cleanDay);

        if (reason == null ||
                reason.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delete reason is required"
            );
        }

        String cleanReason =
                reason.trim();

        String cleanAdminName =
                adminName != null &&
                !adminName.trim().isEmpty()
                        ? adminName.trim()
                        : "Admin";

        WeeklyOffSetting setting =
                weeklyOffSettingRepository
                        .findByDayOfWeekAndActiveTrue(
                                cleanDay
                        )
                        .orElse(null);

        if (setting == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No active recurring Weekly Off found for " +
                    cleanDay
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        setting.setActive(false);
        setting.setUpdatedAt(now);

        weeklyOffSettingRepository.save(setting);


        // =========================
        // ACTION HISTORY
        // =========================

        AdminActionHistory history =
                new AdminActionHistory();

        history.setAdminName(cleanAdminName);
        history.setAction("WEEKLY_OFF_REMOVE");
        history.setActionDate(LocalDate.now());
        history.setActionTime(LocalTime.now());
        history.setFieldName("Recurring Weekly Off");

        history.setOldValue(
                "Day: " +
                cleanDay +
                ", Active: true"
        );

        history.setNewValue(
                "Day: " +
                cleanDay +
                ", Active: false"
        );

        history.setReason(cleanReason);

        historyRepository.save(history);

        return cleanDay +
                " recurring Weekly Off removed successfully";
    }


    // =========================================================
    // DELETE DATE-SPECIFIC WORKING DAY SETTING
    // =========================================================

    @DeleteMapping
    @Transactional
    public String deleteWorkingDaySetting(
            @RequestParam LocalDate settingDate,
            @RequestParam String reason,
            @RequestParam(required = false) String adminName) {

        if (reason == null ||
                reason.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delete reason is required"
            );
        }

        WorkingDaySetting setting =
                workingDaySettingRepository
                        .findBySettingDateAndActiveTrue(
                                settingDate
                        )
                        .orElse(null);

        if (setting == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No active working day setting found for this date"
            );
        }

        String oldType =
                setting.getSettingType();

        String oldReason =
                setting.getReason();

        String deleteReason =
                reason.trim();

        String cleanAdminName =
                adminName != null &&
                !adminName.trim().isEmpty()
                        ? adminName.trim()
                        : "Admin";

        LocalDateTime now =
                LocalDateTime.now();

        setting.setActive(false);
        setting.setUpdatedAt(now);

        workingDaySettingRepository.save(setting);


        // =========================
        // ACTION HISTORY
        // =========================

        AdminActionHistory history =
                new AdminActionHistory();

        history.setAdminName(cleanAdminName);
        history.setAction("WORKING_DAY_DELETE");
        history.setActionDate(LocalDate.now());
        history.setActionTime(LocalTime.now());
        history.setFieldName("Working Day Setting");

        history.setOldValue(
                "Date: " +
                settingDate +
                ", Type: " +
                (oldType == null
                        ? "-"
                        : oldType) +
                ", Reason: " +
                (oldReason == null
                        ? "-"
                        : oldReason)
        );

        history.setNewValue(
                "Deleted, Reason: " +
                deleteReason
        );

        history.setReason(deleteReason);

        historyRepository.save(history);

        return "Working day setting removed successfully";
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateDayOfWeek(
            String dayOfWeek) {

        if (!dayOfWeek.equals("MONDAY") &&
                !dayOfWeek.equals("TUESDAY") &&
                !dayOfWeek.equals("WEDNESDAY") &&
                !dayOfWeek.equals("THURSDAY") &&
                !dayOfWeek.equals("FRIDAY") &&
                !dayOfWeek.equals("SATURDAY") &&
                !dayOfWeek.equals("SUNDAY")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid day of week"
            );
        }
    }
}