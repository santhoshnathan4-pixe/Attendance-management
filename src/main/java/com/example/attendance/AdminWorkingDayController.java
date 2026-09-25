package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/admin/working-days")
public class AdminWorkingDayController {

    private final WorkingDaySettingRepository workingDaySettingRepository;
    private final AdminActionHistoryRepository historyRepository;

    public AdminWorkingDayController(
            WorkingDaySettingRepository workingDaySettingRepository,
            AdminActionHistoryRepository historyRepository) {

        this.workingDaySettingRepository =
                workingDaySettingRepository;

        this.historyRepository =
                historyRepository;
    }


    // =========================================
    // GET WORKING DAY SETTINGS FOR A MONTH
    // =========================================

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
                .findBySettingDateBetweenOrderBySettingDateAsc(
                        startDate,
                        endDate
                );
    }


    // =========================================
    // ADD / UPDATE WORKING DAY SETTING
    // =========================================

    @PostMapping
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
                !type.equals("WORKING_SATURDAY")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid setting type"
            );
        }


        WorkingDaySetting existingSetting =
                workingDaySettingRepository
                        .findBySettingDate(settingDate)
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


        WorkingDaySetting setting =
                isUpdate
                        ? existingSetting
                        : new WorkingDaySetting();


        setting.setSettingDate(
                settingDate
        );

        setting.setSettingType(
                type
        );

        setting.setReason(
                reason
        );


        if (setting.getCreatedAt() == null) {

            setting.setCreatedAt(
                    LocalDateTime.now()
            );
        }


        setting.setUpdatedAt(
                LocalDateTime.now()
        );


        WorkingDaySetting savedSetting =
                workingDaySettingRepository.save(
                        setting
                );


        // =========================================
        // SAVE ADMIN HISTORY
        // =========================================

        AdminActionHistory history =
                new AdminActionHistory();

        history.setAdminName(
                adminName != null &&
                !adminName.trim().isEmpty()
                        ? adminName.trim()
                        : "Admin"
        );


        history.setAction(
                isUpdate
                        ? "WORKING_DAY_UPDATE"
                        : "WORKING_DAY_ADD"
        );


        history.setActionDate(
                LocalDate.now()
        );


        history.setActionTime(
                LocalTime.now()
        );


        history.setFieldName(
                "Working Day Setting"
        );


        if (isUpdate) {

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

        } else {

            history.setOldValue(
                    "-"
            );
        }


        history.setNewValue(
                "Date: " +
                settingDate +
                ", Type: " +
                type +
                ", Reason: " +
                (reason == null ||
                 reason.trim().isEmpty()
                        ? "-"
                        : reason.trim())
        );


        history.setReason(
                reason
        );


        historyRepository.save(
                history
        );


        return savedSetting;
    }


    // =========================================
    // DELETE / RESET A DATE
    // =========================================

    @DeleteMapping
    public String deleteWorkingDaySetting(
            @RequestParam LocalDate settingDate,
            @RequestParam(required = false) String adminName) {

        WorkingDaySetting setting =
                workingDaySettingRepository
                        .findBySettingDate(settingDate)
                        .orElse(null);


        if (setting == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No working day setting found for this date"
            );
        }


        // =========================================
        // SAVE DETAILS BEFORE DELETE
        // =========================================

        String oldType =
                setting.getSettingType();

        String oldReason =
                setting.getReason();


        workingDaySettingRepository.delete(
                setting
        );


        // =========================================
        // SAVE ADMIN HISTORY
        // =========================================

        AdminActionHistory history =
                new AdminActionHistory();


        history.setAdminName(
                adminName != null &&
                !adminName.trim().isEmpty()
                        ? adminName.trim()
                        : "Admin"
        );


        history.setAction(
                "WORKING_DAY_DELETE"
        );


        history.setActionDate(
                LocalDate.now()
        );


        history.setActionTime(
                LocalTime.now()
        );


        history.setFieldName(
                "Working Day Setting"
        );


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
                "-"
        );


        history.setReason(
                oldReason
        );


        historyRepository.save(
                history
        );


        return "Working day setting removed successfully";
    }
}