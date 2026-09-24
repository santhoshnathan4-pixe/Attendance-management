package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/leave-settings")
public class AdminLeavePermissionController {

    private final LeavePermissionSettingRepository settingRepository;
    private final EmployeeRepository employeeRepository;

    public AdminLeavePermissionController(
            LeavePermissionSettingRepository settingRepository,
            EmployeeRepository employeeRepository) {

        this.settingRepository = settingRepository;
        this.employeeRepository = employeeRepository;
    }


    // =========================
    // GET ALL SETTINGS
    // =========================

    @GetMapping
    public List<LeavePermissionSetting> getAllSettings() {

        return settingRepository.findAll();
    }


    // =========================
    // GET EMPLOYEE EFFECTIVE SETTING
    // PRIORITY:
    // EMPLOYEE > ROLE > DEFAULT
    // =========================

    @GetMapping("/employee/{employeeId}")
    public LeavePermissionSetting getEmployeeSetting(
            @PathVariable Integer employeeId) {

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElse(null);

        if (employee == null) {
            throw new RuntimeException("Employee Not Found");
        }


        // =========================
        // 1. EMPLOYEE SETTING
        // =========================

        var employeeSetting =
                settingRepository
                        .findBySettingTypeAndEmployeeId(
                                "EMPLOYEE",
                                employeeId
                        );

        if (employeeSetting.isPresent()) {
            return employeeSetting.get();
        }


        // =========================
        // 2. ROLE SETTING
        // =========================

        if (employee.getRole() != null) {

            var roleSetting =
                    settingRepository
                            .findBySettingTypeAndRole(
                                    "ROLE",
                                    employee.getRole()
                            );

            if (roleSetting.isPresent()) {
                return roleSetting.get();
            }
        }


        // =========================
        // 3. DEFAULT SETTING
        // =========================

        return settingRepository
                .findBySettingType("DEFAULT")
                .orElseThrow(() ->
                        new RuntimeException(
                                "Default Leave Setting Not Found"
                        )
                );
    }


    // =========================
    // CREATE / UPDATE SETTING
    // =========================

    @PostMapping
    public LeavePermissionSetting saveSetting(
            @RequestBody LeavePermissionSettingRequest request) {

        if (request.getSettingType() == null ||
                request.getSettingType().isBlank()) {

            throw new RuntimeException(
                    "Setting Type Required"
            );
        }

        String settingType =
                request.getSettingType()
                        .trim()
                        .toUpperCase();


        if (!settingType.equals("DEFAULT") &&
                !settingType.equals("ROLE") &&
                !settingType.equals("EMPLOYEE")) {

            throw new RuntimeException(
                    "Invalid Setting Type"
            );
        }


        // =========================
        // FIND EXISTING SETTING
        // =========================

        LeavePermissionSetting setting;


        if (settingType.equals("DEFAULT")) {

            setting =
                    settingRepository
                            .findBySettingType("DEFAULT")
                            .orElseGet(
                                    LeavePermissionSetting::new
                            );

        } else if (settingType.equals("ROLE")) {

            if (request.getRole() == null ||
                    request.getRole().isBlank()) {

                throw new RuntimeException(
                        "Role Required"
                );
            }

            setting =
                    settingRepository
                            .findBySettingTypeAndRole(
                                    "ROLE",
                                    request.getRole()
                                            .trim()
                                            .toUpperCase()
                            )
                            .orElseGet(
                                    LeavePermissionSetting::new
                            );

        } else {

            if (request.getEmployeeId() == null) {

                throw new RuntimeException(
                        "Employee ID Required"
                );
            }

            if (!employeeRepository.existsById(
                    request.getEmployeeId())) {

                throw new RuntimeException(
                        "Employee Not Found"
                );
            }

            setting =
                    settingRepository
                            .findBySettingTypeAndEmployeeId(
                                    "EMPLOYEE",
                                    request.getEmployeeId()
                            )
                            .orElseGet(
                                    LeavePermissionSetting::new
                            );
        }


        // =========================
        // SET BASIC INFORMATION
        // =========================

        setting.setSettingType(settingType);


        if (settingType.equals("ROLE")) {

            setting.setRole(
                    request.getRole()
                            .trim()
                            .toUpperCase()
            );

            setting.setEmployeeId(null);

        } else if (settingType.equals("EMPLOYEE")) {

            setting.setRole(null);

            setting.setEmployeeId(
                    request.getEmployeeId()
            );

        } else {

            setting.setRole(null);
            setting.setEmployeeId(null);
        }


        // =========================
        // LEAVE VALUES
        // =========================

        if (request.getSickLeave() != null) {

            if (request.getSickLeave() < 0) {
                throw new RuntimeException(
                        "Sick Leave Cannot Be Negative"
                );
            }

            setting.setSickLeave(
                    request.getSickLeave()
            );
        }


        if (request.getCasualLeave() != null) {

            if (request.getCasualLeave() < 0) {
                throw new RuntimeException(
                        "Casual Leave Cannot Be Negative"
                );
            }

            setting.setCasualLeave(
                    request.getCasualLeave()
            );
        }


        if (request.getPermissionCount() != null) {

            if (request.getPermissionCount() < 0) {
                throw new RuntimeException(
                        "Permission Count Cannot Be Negative"
                );
            }

            setting.setPermissionCount(
                    request.getPermissionCount()
            );
        }


        // =========================
        // TIMESTAMPS
        // =========================

        LocalDateTime now =
                LocalDateTime.now();

        if (setting.getCreatedAt() == null) {
            setting.setCreatedAt(now);
        }

        setting.setUpdatedAt(now);


        return settingRepository.save(setting);
    }
}