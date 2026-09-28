package com.example.attendance;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/shift-policy")
@CrossOrigin
public class AdminShiftPolicyController {

    private final ShiftPolicyService shiftPolicyService;

    public AdminShiftPolicyController(
            ShiftPolicyService shiftPolicyService) {

        this.shiftPolicyService = shiftPolicyService;
    }

    // =========================
    // GET EMPLOYEE EFFECTIVE SHIFT
    // =========================

    @GetMapping("/employee/{employeeId}")
    public ShiftSetting getEmployeeEffectiveShift(
            @PathVariable Integer employeeId) {

        return shiftPolicyService
                .getEffectiveShift(employeeId);
    }

    // =========================
    // GET GENERAL SHIFT
    // =========================

    @GetMapping("/general")
    public ShiftSetting getGeneralShift() {

        return shiftPolicyService
                .getGeneralShift();
    }

    // =========================
    // GET SPECIFIC SHIFT
    // =========================

    @GetMapping("/{shiftType}")
    public ShiftSetting getShift(
            @PathVariable String shiftType) {

        return shiftPolicyService
                .getShift(shiftType);
    }
}