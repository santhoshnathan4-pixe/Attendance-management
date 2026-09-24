
package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/working-days")
public class AdminWorkingDayController {

    private final WorkingDaySettingRepository workingDaySettingRepository;

    public AdminWorkingDayController(
            WorkingDaySettingRepository workingDaySettingRepository) {

        this.workingDaySettingRepository =
                workingDaySettingRepository;
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
            @RequestParam(required = false) String reason) {

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

        WorkingDaySetting setting =
                workingDaySettingRepository
                        .findBySettingDate(settingDate)
                        .orElseGet(
                                WorkingDaySetting::new
                        );

        setting.setSettingDate(settingDate);
        setting.setSettingType(type);
        setting.setReason(reason);

        if (setting.getCreatedAt() == null) {

            setting.setCreatedAt(
                    java.time.LocalDateTime.now()
            );
        }

        setting.setUpdatedAt(
                java.time.LocalDateTime.now()
        );

        return workingDaySettingRepository.save(
                setting
        );
    }


    // =========================================
    // DELETE / RESET A DATE
    // =========================================

    @DeleteMapping
    public String deleteWorkingDaySetting(
            @RequestParam LocalDate settingDate) {

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

        workingDaySettingRepository.delete(
                setting
        );

        return "Working day setting removed successfully";
    }
}

