package com.example.attendance;

import org.springframework.stereotype.Service;

import java.time.LocalTime;

// =========================================================
// SHIFT TIME SERVICE
//
// Gives the effective shift start / end time of an employee
// for the salary calculation (PayrollService).
//
// SHIFT PRIORITY (same as AttendanceController):
// EMPLOYEE > ROLE > DEFAULT
//
// Shift type comes from LeavePermissionSetting.
// Actual timing comes from ShiftSetting.
// =========================================================

@Service
public class ShiftTimeService {

    // Default fallback timing (same as AttendanceController)
    private static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(9, 0);

    private static final LocalTime DEFAULT_SHIFT_END = LocalTime.of(18, 0);

    private final LeavePermissionSettingRepository settingRepository;
    private final ShiftSettingRepository shiftSettingRepository;

    public ShiftTimeService(
            LeavePermissionSettingRepository settingRepository,
            ShiftSettingRepository shiftSettingRepository) {

        this.settingRepository = settingRepository;

        this.shiftSettingRepository = shiftSettingRepository;
    }

    // =====================================================
    // SHIFT TIMES
    // =====================================================

    public record ShiftTimes(
            LocalTime start,
            LocalTime end) {

        // Full shift length in minutes. Example: 10:00 to 17:30 = 450
        public long totalMinutes() {

            return java.time.Duration
                    .between(start, end)
                    .toMinutes();
        }
    }

    // =====================================================
    // GET EFFECTIVE SHIFT TIMES OF AN EMPLOYEE
    // =====================================================

    public ShiftTimes getShiftTimes(
            Employee employee) {

        String shiftType = "GENERAL";

        // =================================================
        // 1. EMPLOYEE SETTING
        // =================================================

        if (employee.getId() != null) {

            var employeeSetting = settingRepository
                    .findBySettingTypeAndEmployeeId(
                            "EMPLOYEE",
                            employee.getId());

            if (employeeSetting.isPresent()
                    && employeeSetting.get().getShiftType() != null
                    && !employeeSetting.get().getShiftType().isBlank()) {

                shiftType = normalizeShiftType(
                        employeeSetting.get().getShiftType());
            }
        }

        // =================================================
        // 2. ROLE SETTING
        // =================================================

        if ("GENERAL".equals(shiftType)
                && employee.getRole() != null
                && !employee.getRole().isBlank()) {

            var roleSetting = settingRepository
                    .findBySettingTypeAndRole(
                            "ROLE",
                            employee.getRole()
                                    .trim()
                                    .toUpperCase());

            if (roleSetting.isPresent()
                    && roleSetting.get().getShiftType() != null
                    && !roleSetting.get().getShiftType().isBlank()) {

                shiftType = normalizeShiftType(
                        roleSetting.get().getShiftType());
            }
        }

        // =================================================
        // 3. DEFAULT SETTING
        // =================================================

        if ("GENERAL".equals(shiftType)) {

            var defaultSetting = settingRepository
                    .findBySettingType(
                            "DEFAULT");

            if (defaultSetting.isPresent()
                    && defaultSetting.get().getShiftType() != null
                    && !defaultSetting.get().getShiftType().isBlank()) {

                shiftType = normalizeShiftType(
                        defaultSetting.get().getShiftType());
            }
        }

        // =================================================
        // ACTUAL SHIFT TIMING
        // =================================================

        ShiftSetting shift = shiftSettingRepository
                .findByShiftType(shiftType)
                .orElseGet(() -> shiftSettingRepository
                        .findByShiftType("GENERAL")
                        .orElse(null));

        LocalTime start = shift != null
                && shift.getStartTime() != null
                        ? shift.getStartTime()
                        : DEFAULT_SHIFT_START;

        LocalTime end = shift != null
                && shift.getEndTime() != null
                        ? shift.getEndTime()
                        : DEFAULT_SHIFT_END;

        return new ShiftTimes(
                start,
                end);
    }

    // =====================================================
    // NORMALIZE SHIFT TYPE
    // =====================================================

    private String normalizeShiftType(
            String shiftType) {

        if (shiftType == null
                || shiftType.isBlank()) {

            return "GENERAL";
        }

        return shiftType
                .trim()
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");
    }
}