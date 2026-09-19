package com.example.attendance;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/employees")
public class AdminEmployeeController {

    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;
    private final AdminActionHistoryRepository historyRepository;

    public AdminEmployeeController(
            EmployeeRepository employeeRepository,
            AdminRepository adminRepository,
            AdminActionHistoryRepository historyRepository) {

        this.employeeRepository = employeeRepository;
        this.adminRepository = adminRepository;
        this.historyRepository = historyRepository;
    }

    // =====================================================
    // ALLOWED ROLES
    // =====================================================

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "EMPLOYEE",
            "DEVELOPER",
            "HR",
            "TEAM_LEAD",
            "MANAGER",
            "ADMIN"
    );

    // =====================================================
    // GET ALL ACTIVE EMPLOYEES
    // =====================================================

    @GetMapping
    public List<AdminEmployeeResponse> getAllEmployees() {

        return employeeRepository.findByActiveTrue()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // =====================================================
    // ADD EMPLOYEE
    // =====================================================

    @PostMapping
    public AdminEmployeeResponse addEmployee(
            @RequestBody AdminEmployeeActionRequest request) {

        // =================================================
        // BASIC VALIDATION
        // =================================================

        if (request.getName() == null
                || request.getName().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee name is required"
            );
        }

        if (request.getEmail() == null
                || request.getEmail().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee email is required"
            );
        }

        if (request.getRole() == null
                || request.getRole().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee role is required"
            );
        }

        if (request.getJoiningDate() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Joining date is required"
            );
        }

        // =================================================
        // CLEAN INPUT
        // =================================================

        String email =
                request.getEmail().trim();

        String role =
                request.getRole()
                        .trim()
                        .toUpperCase();

        // =================================================
        // ROLE VALIDATION
        // =================================================

        if (!ALLOWED_ROLES.contains(role)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid employee role. Allowed roles: EMPLOYEE, DEVELOPER, HR, TEAM_LEAD, MANAGER, ADMIN"
            );
        }

        // =================================================
        // DUPLICATE EMAIL CHECK
        //
        // IMPORTANT:
        // This happens BEFORE admin password verification.
        // =================================================

        boolean emailExists =
                employeeRepository
                        .findAll()
                        .stream()
                        .anyMatch(existing ->

                                existing.getEmail() != null
                                        && existing.getEmail()
                                        .trim()
                                        .equalsIgnoreCase(email)
                        );

        if (emailExists) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee with this email already exists"
            );
        }

        // =================================================
        // GENERATE EMPLOYEE CODE
        // =================================================

        String generatedEmployeeCode =
                generateEmployeeCode();

        // =================================================
        // EXTRA EMPLOYEE CODE SAFETY CHECK
        // =================================================

        while (employeeCodeExists(
                generatedEmployeeCode)) {

            generatedEmployeeCode =
                    generateNextEmployeeCode(
                            generatedEmployeeCode
                    );
        }

        // =================================================
        // NOW VERIFY ADMIN
        // =================================================

        Admin admin =
                verifyAdmin(
                        request.getAdminEmail(),
                        request.getAdminPassword()
                );

        // =================================================
        // CREATE EMPLOYEE
        // =================================================

        Employee employee =
                new Employee();

        // Employee Code generated by backend
        employee.setEmployeeCode(
                generatedEmployeeCode
        );

        employee.setName(
                request.getName().trim()
        );

        employee.setEmail(
                email
        );

        employee.setContactNumber(
                request.getContactNumber()
        );

        employee.setRole(
                role
        );

        // =================================================
        // SALARY
        // =================================================

        if (request.getSalary() != null) {

            employee.setSalary(
                    request.getSalary()
            );
        }

        employee.setJoiningDate(
                request.getJoiningDate()
        );

        // =================================================
        // ACTIVE
        // =================================================

        employee.setActive(true);

        // =================================================
        // SAVE EMPLOYEE
        //
        // Employee ID is generated by database.
        // Employee Code is generated above.
        // =================================================

        Employee savedEmployee;

        try {

            savedEmployee =
                    employeeRepository.save(employee);

        } catch (DataIntegrityViolationException exception) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee ID / Employee Code / Email already exists"
            );
        }

        // =================================================
        // SAVE ADMIN HISTORY
        // =================================================

        saveHistory(
                admin,
                "ADD",
                savedEmployee
        );

        return convertToResponse(
                savedEmployee
        );
    }

    // =====================================================
    // UPDATE EMPLOYEE
    // =====================================================

    @PutMapping("/{id}")
    public AdminEmployeeResponse updateEmployee(
            @PathVariable Integer id,
            @RequestBody AdminEmployeeActionRequest request) {

        // =================================================
        // FIND EMPLOYEE
        // =================================================

        Employee employee =
                employeeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Employee not found"
                                )
                        );

        // =================================================
        // BASIC VALIDATION
        // =================================================

        if (request.getName() == null
                || request.getName().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee name is required"
            );
        }

        if (request.getEmail() == null
                || request.getEmail().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee email is required"
            );
        }

        if (request.getRole() == null
                || request.getRole().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee role is required"
            );
        }

        if (request.getJoiningDate() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Joining date is required"
            );
        }

        // =================================================
        // CLEAN INPUT
        // =================================================

        String email =
                request.getEmail().trim();

        String role =
                request.getRole()
                        .trim()
                        .toUpperCase();

        // =================================================
        // ROLE VALIDATION
        // =================================================

        if (!ALLOWED_ROLES.contains(role)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid employee role. Allowed roles: EMPLOYEE, DEVELOPER, HR, TEAM_LEAD, MANAGER, ADMIN"
            );
        }

        // =================================================
        // DUPLICATE EMAIL CHECK
        //
        // IMPORTANT:
        // Happens BEFORE admin password verification.
        //
        // Current employee's own email is allowed.
        // =================================================

        boolean emailExists =
                employeeRepository
                        .findAll()
                        .stream()
                        .anyMatch(existing ->

                                existing.getId() != null

                                        && !existing.getId()
                                        .equals(employee.getId())

                                        && existing.getEmail() != null

                                        && existing.getEmail()
                                        .trim()
                                        .equalsIgnoreCase(email)
                        );

        if (emailExists) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee with this email already exists"
            );
        }

        // =================================================
        // EMPLOYEE CODE MUST NOT CHANGE
        // =================================================

        String existingEmployeeCode =
                employee.getEmployeeCode();

        // =================================================
        // OLD RECORD WITHOUT EMPLOYEE CODE
        //
        // Generate one automatically.
        // =================================================

        if (existingEmployeeCode == null
                || existingEmployeeCode.trim().isEmpty()) {

            existingEmployeeCode =
                    generateEmployeeCode();

            while (employeeCodeExists(
                    existingEmployeeCode,
                    employee.getId())) {

                existingEmployeeCode =
                        generateNextEmployeeCode(
                                existingEmployeeCode
                        );
            }
        }

        // =================================================
        // VERIFY ADMIN
        // =================================================

        Admin admin =
                verifyAdmin(
                        request.getAdminEmail(),
                        request.getAdminPassword()
                );

        // =================================================
        // UPDATE EMPLOYEE
        // =================================================

        employee.setEmployeeCode(
                existingEmployeeCode
        );

        employee.setName(
                request.getName().trim()
        );

        employee.setEmail(
                email
        );

        employee.setContactNumber(
                request.getContactNumber()
        );

        employee.setRole(
                role
        );

        /*
         * Salary is managed separately.
         *
         * Therefore salary is NOT changed here.
         */

        employee.setJoiningDate(
                request.getJoiningDate()
        );

        Employee updatedEmployee;

        try {

            updatedEmployee =
                    employeeRepository.save(employee);

        } catch (DataIntegrityViolationException exception) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee ID / Employee Code / Email already exists"
            );
        }

        // =================================================
        // SAVE ADMIN HISTORY
        // =================================================

        saveHistory(
                admin,
                "EDIT",
                updatedEmployee
        );

        return convertToResponse(
                updatedEmployee
        );
    }

    // =====================================================
    // DELETE EMPLOYEE
    // =====================================================

    @DeleteMapping("/{id}")
    public String deleteEmployee(
            @PathVariable Integer id,
            @RequestBody AdminEmployeeActionRequest request) {

        // =================================================
        // FIND EMPLOYEE
        // =================================================

        Employee employee =
                employeeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Employee not found"
                                )
                        );

        // =================================================
        // VERIFY ADMIN
        // =================================================

        Admin admin =
                verifyAdmin(
                        request.getAdminEmail(),
                        request.getAdminPassword()
                );

        // =================================================
        // SOFT DELETE
        // =================================================

        employee.setActive(false);

        employeeRepository.save(employee);

        // =================================================
        // SAVE DELETE HISTORY
        // =================================================

        saveHistory(
                admin,
                "DELETE",
                employee
        );

        return "Employee deleted successfully";
    }

    // =====================================================
    // GENERATE EMPLOYEE CODE
    // =====================================================

    private String generateEmployeeCode() {

        int highestNumber = 0;

        /*
         * Use ALL employees.
         *
         * This prevents deleted employee codes
         * from being reused.
         *
         * Example:
         *
         * EMP001
         * EMP002
         * EMP003 -> deleted
         *
         * New employee:
         *
         * EMP004
         */

        List<Employee> allEmployees =
                employeeRepository.findAll();

        for (Employee employee : allEmployees) {

            String code =
                    employee.getEmployeeCode();

            if (code == null) {
                continue;
            }

            code =
                    code.trim()
                            .toUpperCase();

            if (!code.startsWith("EMP")) {
                continue;
            }

            String numberPart =
                    code.substring(3);

            try {

                int number =
                        Integer.parseInt(numberPart);

                if (number > highestNumber) {

                    highestNumber =
                            number;
                }

            } catch (NumberFormatException ignored) {

                // Ignore invalid employee codes
            }
        }

        int nextNumber =
                highestNumber + 1;

        return String.format(
                "EMP%03d",
                nextNumber
        );
    }

    // =====================================================
    // CHECK EMPLOYEE CODE
    // =====================================================

    private boolean employeeCodeExists(
            String employeeCode) {

        if (employeeCode == null
                || employeeCode.trim().isEmpty()) {

            return false;
        }

        String code =
                employeeCode.trim();

        return employeeRepository
                .findAll()
                .stream()
                .anyMatch(employee ->

                        employee.getEmployeeCode() != null

                                && employee.getEmployeeCode()
                                .trim()
                                .equalsIgnoreCase(code)
                );
    }

    // =====================================================
    // CHECK EMPLOYEE CODE
    // EXCLUDING CURRENT EMPLOYEE
    // =====================================================

    private boolean employeeCodeExists(
            String employeeCode,
            Integer currentEmployeeId) {

        if (employeeCode == null
                || employeeCode.trim().isEmpty()) {

            return false;
        }

        String code =
                employeeCode.trim();

        return employeeRepository
                .findAll()
                .stream()
                .anyMatch(employee ->

                        employee.getId() != null

                                && !employee.getId()
                                .equals(currentEmployeeId)

                                && employee.getEmployeeCode() != null

                                && employee.getEmployeeCode()
                                .trim()
                                .equalsIgnoreCase(code)
                );
    }

    // =====================================================
    // GENERATE NEXT EMPLOYEE CODE
    // =====================================================

    private String generateNextEmployeeCode(
            String currentCode) {

        if (currentCode == null
                || !currentCode
                .trim()
                .toUpperCase()
                .startsWith("EMP")) {

            return generateEmployeeCode();
        }

        String code =
                currentCode
                        .trim()
                        .toUpperCase();

        String numberPart =
                code.substring(3);

        try {

            int currentNumber =
                    Integer.parseInt(numberPart);

            return String.format(
                    "EMP%03d",
                    currentNumber + 1
            );

        } catch (NumberFormatException exception) {

            return generateEmployeeCode();
        }
    }

    // =====================================================
    // ADMIN PASSWORD VERIFICATION
    // =====================================================

    private Admin verifyAdmin(
            String email,
            String password) {

        if (email == null
                || email.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Admin email required"
            );
        }

        if (password == null
                || password.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Admin password required"
            );
        }

        return adminRepository
                .findByEmailAndPassword(
                        email.trim(),
                        password
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid admin email or password"
                        )
                );
    }

    // =====================================================
    // SAVE ADMIN HISTORY
    // =====================================================

    private void saveHistory(
            Admin admin,
            String action,
            Employee employee) {

        AdminActionHistory history =
                new AdminActionHistory();

        history.setAdminName(
                admin.getAdminName()
        );

        history.setAction(
                action
        );

        history.setEmployeeId(
                employee.getId()
        );

        history.setEmployeeCode(
                employee.getEmployeeCode()
        );

        history.setEmployeeName(
                employee.getName()
        );

        history.setActionDate(
                LocalDate.now()
        );

        history.setActionTime(
                LocalTime.now()
        );

        historyRepository.save(
                history
        );
    }

    // =====================================================
    // RESPONSE CONVERTER
    // =====================================================

    private AdminEmployeeResponse convertToResponse(
            Employee employee) {

        return new AdminEmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getName(),
                employee.getEmail(),
                employee.getContactNumber(),
                employee.getRole(),
                employee.getSalary(),
                employee.getJoiningDate()
        );
    }
}