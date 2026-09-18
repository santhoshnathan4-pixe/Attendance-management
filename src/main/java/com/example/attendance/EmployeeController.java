package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/employee")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // =========================================
    // EMPLOYEE LOGIN
    // =========================================

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password) {

        Employee employee =
                employeeRepository
                        .findByEmailAndActiveTrue(email)
                        .orElse(null);

        // Employee not found
        if (employee == null) {
            return "Invalid Email or Password";
        }

        // =========================================
        // FIRST TIME LOGIN
        // =========================================

        if (employee.getPassword() == null ||
                employee.getPassword().trim().isEmpty()) {

            return "FIRST_LOGIN_REQUIRED";
        }

        // =========================================
        // NORMAL LOGIN
        // =========================================

        if (employee.getPassword().equals(password)) {

            return "Login Successful";
        }

        return "Invalid Email or Password";
    }


    // =========================================
    // FIRST TIME PASSWORD SETUP
    // =========================================

    @PostMapping("/first-password")
    public String setFirstPassword(
            @RequestBody FirstEmployeePasswordRequest request) {

        if (request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee email required"
            );
        }

        if (request.getNewPassword() == null ||
                request.getNewPassword().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password required"
            );
        }

        if (request.getConfirmPassword() == null ||
                request.getConfirmPassword().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Confirm password required"
            );
        }

        // =========================================
        // PASSWORD LENGTH
        // =========================================

        if (request.getNewPassword().length() < 6) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must contain at least 6 characters"
            );
        }

        // =========================================
        // PASSWORD MATCH
        // =========================================

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passwords do not match"
            );
        }

        // =========================================
        // FIND ACTIVE EMPLOYEE
        // =========================================

        Employee employee =
                employeeRepository
                        .findByEmailAndActiveTrue(
                                request.getEmail()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Employee not found or inactive"
                                )
                        );

        // =========================================
        // FIRST TIME ONLY
        // =========================================

        if (employee.getPassword() != null &&
                !employee.getPassword().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password already set. Please login."
            );
        }

        // =========================================
        // SAVE EMPLOYEE PASSWORD
        // =========================================

        employee.setPassword(
                request.getNewPassword()
        );

        employeeRepository.save(employee);

        return "Password set successfully";
    }


    // =========================================
    // EMPLOYEE DETAILS
    // =========================================

    @GetMapping("/details")
    public EmployeeDetailsResponse getEmployeeDetails(
            @RequestParam String email) {

        Employee employee =
                employeeRepository
                        .findByEmailAndActiveTrue(email)
                        .orElse(null);

        if (employee == null) {
            return null;
        }

        EmployeeDetailsResponse response =
                new EmployeeDetailsResponse();

        response.setId(employee.getId());

        response.setEmployeeCode(
                employee.getEmployeeCode()
        );

        response.setName(
                employee.getName()
        );

        response.setEmail(
                employee.getEmail()
        );

        response.setRole(
                employee.getRole()
        );

        response.setJoiningDate(
                employee.getJoiningDate()
        );

        response.setContactNumber(
                employee.getContactNumber()
        );

        return response;
    }
}