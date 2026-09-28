package com.example.attendance;

import org.springframework.stereotype.Service;

@Service
public class ShiftPolicyService {

    private final ShiftSettingRepository shiftSettingRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final EmployeeRepository employeeRepository;

    public ShiftPolicyService(
            ShiftSettingRepository shiftSettingRepository,
            ShiftAssignmentRepository shiftAssignmentRepository,
            EmployeeRepository employeeRepository) {

        this.shiftSettingRepository = shiftSettingRepository;
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.employeeRepository = employeeRepository;
    }

    // =========================
    // GET EFFECTIVE SHIFT
    // =========================

    public ShiftSetting getEffectiveShift(
            Integer employeeId) {

        if (employeeId == null) {
            return getGeneralShift();
        }

        // ==========================================
        // 1. EMPLOYEE-SPECIFIC SHIFT
        // ==========================================

        var employeeAssignment =
                shiftAssignmentRepository
                        .findByAssignmentTypeAndEmployeeId(
                                "EMPLOYEE",
                                employeeId);

        if (employeeAssignment.isPresent()) {

            return getShift(
                    employeeAssignment
                            .get()
                            .getShiftType());
        }

        // ==========================================
        // 2. ROLE-SPECIFIC SHIFT
        // ==========================================

        var employee =
                employeeRepository
                        .findById(employeeId)
                        .orElse(null);

        if (employee != null
                && employee.getRole() != null) {

            String role =
                    normalizeRole(employee.getRole());

            var roleAssignment =
                    shiftAssignmentRepository
                            .findByAssignmentTypeAndRole(
                                    "ROLE",
                                    role);

            if (roleAssignment.isPresent()) {

                return getShift(
                        roleAssignment
                                .get()
                                .getShiftType());
            }
        }

        // ==========================================
        // 3. GENERAL SHIFT
        // ==========================================

        return getGeneralShift();
    }

    // =========================
    // GET SHIFT
    // =========================

    public ShiftSetting getShift(
            String shiftType) {

        String normalizedShiftType =
                normalizeShiftType(shiftType);

        return shiftSettingRepository
                .findByShiftType(normalizedShiftType)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Shift setting not found: "
                                        + normalizedShiftType));
    }

    // =========================
    // GENERAL SHIFT
    // =========================

    public ShiftSetting getGeneralShift() {

        return getShift("GENERAL");
    }

    // =========================
    // ROLE NORMALIZATION
    // =========================

    private String normalizeRole(
            String role) {

        return role
                .trim()
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");
    }

    // =========================
    // SHIFT NORMALIZATION
    // =========================

    private String normalizeShiftType(
            String shiftType) {

        if (shiftType == null) {
            throw new IllegalStateException(
                    "Shift type is required");
        }

        return shiftType
                .trim()
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");
    }
}