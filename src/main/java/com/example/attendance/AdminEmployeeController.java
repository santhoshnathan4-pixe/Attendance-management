package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

    @GetMapping
    public List<AdminEmployeeResponse> getAllEmployees() {

        return employeeRepository.findByActiveTrue()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @PostMapping
    public AdminEmployeeResponse addEmployee(
            @RequestBody AdminEmployeeActionRequest request) {

        Admin admin = verifyAdmin(
                request.getAdminEmail(),
                request.getAdminPassword()
        );

        Employee employee = new Employee();

        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setContactNumber(request.getContactNumber());
        employee.setRole(request.getRole());
        employee.setSalary(request.getSalary());
        employee.setJoiningDate(request.getJoiningDate());

        // New employee is active
        employee.setActive(true);

        Employee savedEmployee =
                employeeRepository.save(employee);

        saveHistory(
                admin,
                "ADD",
                savedEmployee
        );

        return convertToResponse(savedEmployee);
    }

    @PutMapping("/{id}")
    public AdminEmployeeResponse updateEmployee(
            @PathVariable Integer id,
            @RequestBody AdminEmployeeActionRequest request) {

        Admin admin = verifyAdmin(
                request.getAdminEmail(),
                request.getAdminPassword()
        );

        Employee employee =
                employeeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Employee not found"
                                )
                        );

        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setContactNumber(request.getContactNumber());
        employee.setRole(request.getRole());
        employee.setSalary(request.getSalary());
        employee.setJoiningDate(request.getJoiningDate());

        Employee updatedEmployee =
                employeeRepository.save(employee);

        saveHistory(
                admin,
                "EDIT",
                updatedEmployee
        );

        return convertToResponse(updatedEmployee);
    }

    @DeleteMapping("/{id}")
    public String deleteEmployee(
            @PathVariable Integer id,
            @RequestBody AdminEmployeeActionRequest request) {

        Admin admin = verifyAdmin(
                request.getAdminEmail(),
                request.getAdminPassword()
        );

        Employee employee =
                employeeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Employee not found"
                                )
                        );

        // Soft delete
        employee.setActive(false);

        employeeRepository.save(employee);

        // Keep DELETE in audit history
        saveHistory(
                admin,
                "DELETE",
                employee
        );

        return "Employee deleted successfully";
    }

    private Admin verifyAdmin(
            String email,
            String password) {

        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Admin email required"
            );
        }

        if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Admin password required"
            );
        }

        return adminRepository
                .findByEmailAndPassword(
                        email,
                        password
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid admin email or password"
                        )
                );
    }

    private void saveHistory(
            Admin admin,
            String action,
            Employee employee) {

        AdminActionHistory history =
                new AdminActionHistory();

        history.setAdminName(
                admin.getAdminName()
        );

        history.setAction(action);

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

        historyRepository.save(history);
    }

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