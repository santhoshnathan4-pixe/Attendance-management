package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/shift-assignments")
@CrossOrigin
public class AdminShiftAssignmentController {

    private final ShiftAssignmentRepository assignmentRepository;
    private final ShiftSettingRepository shiftSettingRepository;
    private final EmployeeRepository employeeRepository;

    public AdminShiftAssignmentController(
            ShiftAssignmentRepository assignmentRepository,
            ShiftSettingRepository shiftSettingRepository,
            EmployeeRepository employeeRepository) {

        this.assignmentRepository = assignmentRepository;
        this.shiftSettingRepository = shiftSettingRepository;
        this.employeeRepository = employeeRepository;
    }

    // =========================
    // GET ALL ASSIGNMENTS
    // =========================

    @GetMapping
    public List<ShiftAssignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    // =========================
    // ROLE ASSIGNMENT
    // =========================

    @GetMapping("/role/{role}")
    public ShiftAssignment getRoleAssignment(
            @PathVariable String role) {

        String normalizedRole = normalizeRole(role);

        return assignmentRepository
                .findByAssignmentTypeAndRole(
                        "ROLE",
                        normalizedRole)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Role shift assignment not found"));
    }

    // =========================
    // EMPLOYEE ASSIGNMENT
    // =========================

    @GetMapping("/employee/{employeeId}")
    public ShiftAssignment getEmployeeAssignment(
            @PathVariable Integer employeeId) {

        return assignmentRepository
                .findByAssignmentTypeAndEmployeeId(
                        "EMPLOYEE",
                        employeeId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Employee shift assignment not found"));
    }

    // =========================
    // CREATE / UPDATE
    // =========================

    @PostMapping
    public ShiftAssignment saveAssignment(
            @RequestBody ShiftAssignmentRequest request) {

        validateRequest(request);

        String assignmentType =
                request.getAssignmentType()
                        .trim()
                        .toUpperCase();

        String shiftType =
                normalizeShiftType(request.getShiftType());

        ShiftAssignment assignment;

        if ("ROLE".equals(assignmentType)) {

            String role =
                    normalizeRole(request.getRole());

            assignment =
                    assignmentRepository
                            .findByAssignmentTypeAndRole(
                                    "ROLE",
                                    role)
                            .orElseGet(ShiftAssignment::new);

            assignment.setAssignmentType("ROLE");
            assignment.setRole(role);
            assignment.setEmployeeId(null);

        } else {

            Integer employeeId =
                    request.getEmployeeId();

            assignment =
                    assignmentRepository
                            .findByAssignmentTypeAndEmployeeId(
                                    "EMPLOYEE",
                                    employeeId)
                            .orElseGet(ShiftAssignment::new);

            assignment.setAssignmentType("EMPLOYEE");
            assignment.setEmployeeId(employeeId);
            assignment.setRole(null);
        }

        assignment.setShiftType(shiftType);

        if (assignment.getCreatedAt() == null) {
            assignment.setCreatedAt(LocalDateTime.now());
        }

        assignment.setUpdatedAt(LocalDateTime.now());

        return assignmentRepository.save(assignment);
    }

    // =========================
    // DELETE ROLE ASSIGNMENT
    // =========================

    @DeleteMapping("/role/{role}")
    public void deleteRoleAssignment(
            @PathVariable String role) {

        String normalizedRole =
                normalizeRole(role);

        ShiftAssignment assignment =
                assignmentRepository
                        .findByAssignmentTypeAndRole(
                                "ROLE",
                                normalizedRole)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Role shift assignment not found"));

        assignmentRepository.delete(assignment);
    }

    // =========================
    // DELETE EMPLOYEE ASSIGNMENT
    // =========================

    @DeleteMapping("/employee/{employeeId}")
    public void deleteEmployeeAssignment(
            @PathVariable Integer employeeId) {

        ShiftAssignment assignment =
                assignmentRepository
                        .findByAssignmentTypeAndEmployeeId(
                                "EMPLOYEE",
                                employeeId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Employee shift assignment not found"));

        assignmentRepository.delete(assignment);
    }

    // =========================
    // VALIDATION
    // =========================

    private void validateRequest(
            ShiftAssignmentRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request is required");
        }

        if (request.getAssignmentType() == null
                || request.getAssignmentType().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Assignment type is required");
        }

        String assignmentType =
                request.getAssignmentType()
                        .trim()
                        .toUpperCase();

        if (!assignmentType.equals("ROLE")
                && !assignmentType.equals("EMPLOYEE")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Assignment type must be ROLE or EMPLOYEE");
        }

        if (request.getShiftType() == null
                || request.getShiftType().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Shift type is required");
        }

        String shiftType =
                normalizeShiftType(request.getShiftType());

        if (!shiftType.equals("GENERAL")
                && !shiftType.equals("SHIFT_1")
                && !shiftType.equals("SHIFT_2")
                && !shiftType.equals("SHIFT_3")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid shift type");
        }

        if ("ROLE".equals(assignmentType)) {

            if (request.getRole() == null
                    || request.getRole().trim().isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Role is required");
            }

        } else {

            if (request.getEmployeeId() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Employee ID is required");
            }

            if (!employeeRepository.existsById(
                    request.getEmployeeId())) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Employee not found");
            }
        }

        if (!shiftSettingRepository
                .findByShiftType(shiftType)
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Shift setting does not exist");
        }
    }

    // =========================
    // NORMALIZE ROLE
    // =========================

    private String normalizeRole(String role) {

        if (role == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role is required");
        }

        return role
                .trim()
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");
    }

    // =========================
    // NORMALIZE SHIFT TYPE
    // =========================

    private String normalizeShiftType(
            String shiftType) {

        if (shiftType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Shift type is required");
        }

        return shiftType
                .trim()
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");
    }
}