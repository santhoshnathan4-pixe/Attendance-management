package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/admin/salary")
public class AdminSalaryController {

        private final EmployeeRepository employeeRepository;
        private final AdminRepository adminRepository;
        private final AdminActionHistoryRepository historyRepository;
        private final PayrollService payrollService;

        public AdminSalaryController(
                        EmployeeRepository employeeRepository,
                        AdminRepository adminRepository,
                        AdminActionHistoryRepository historyRepository,
                        PayrollService payrollService) {

                this.employeeRepository = employeeRepository;
                this.adminRepository = adminRepository;
                this.historyRepository = historyRepository;
                this.payrollService = payrollService;
        }

        // =========================================
        // GET ALL EMPLOYEE SALARY
        // =========================================

        @GetMapping
        public List<AdminSalaryResponse> getAllSalary() {

                return employeeRepository.findAll()
                                .stream()
                                .filter(employee -> Boolean.TRUE.equals(employee.getActive()))
                                .filter(employee -> !"ADMIN".equalsIgnoreCase(employee.getRole()))
                                .sorted(
                                                Comparator.comparing(
                                                                Employee::getEmployeeCode,
                                                                Comparator.nullsLast(
                                                                                String.CASE_INSENSITIVE_ORDER)))
                                .map(employee -> new AdminSalaryResponse(
                                                employee.getId(),
                                                employee.getEmployeeCode(),
                                                employee.getName(),
                                                employee.getSalary(),
                                                employee.getJoiningDate()))
                                .toList();
        }

        // =========================================
        // UPDATE EMPLOYEE SALARY
        // =========================================

        @PutMapping("/{employeeId}")
        public String updateSalary(
                        @PathVariable Integer employeeId,
                        @RequestBody AdminSalaryUpdateRequest request) {

                Admin admin = findLoggedInAdmin(
                                request.getAdminEmail());

                if (request.getNewSalary() == null) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Salary is required");
                }

                if (request.getNewSalary() < 0) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Salary cannot be negative");
                }

                validateReason(
                                request.getReason());

                Employee employee = employeeRepository.findById(employeeId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Employee not found"));

                if (!Boolean.TRUE.equals(employee.getActive())) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Employee is inactive");
                }

                if ("ADMIN".equalsIgnoreCase(employee.getRole())) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Salary cannot be updated for admin");
                }

                Double oldSalary = employee.getSalary() != null
                                ? employee.getSalary()
                                : 0.0;

                Double newSalary = request.getNewSalary();

                if (Double.compare(oldSalary, newSalary) == 0) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "No salary change detected");
                }

                employee.setSalary(newSalary);

                employeeRepository.save(employee);

                saveHistory(
                                admin,
                                "SALARY UPDATE",
                                employee,
                                "Salary",
                                String.valueOf(oldSalary),
                                String.valueOf(newSalary),
                                request.getReason());

                return "Salary updated successfully";
        }

        // =========================================
        // CALCULATE MONTHLY SALARY
        //
        // working days = month - Sundays - holidays
        // one day = monthly salary / working days
        // payable days = present + approved sick/casual
        // + absent days covered by leave balance
        // salary = one day x payable days
        //
        // All the maths is inside PayrollService.
        // =========================================

        @GetMapping("/calculate")
        public List<SalaryResponse> calculateSalary(
                        @RequestParam int year,
                        @RequestParam int month) {

                PayrollService.MonthPayroll payroll = payrollService.calculate(year, month);

                return payroll.employees()
                                .stream()
                                .map(p -> {

                                        Employee employee = p.employee();
                                        PayrollMath.Result math = p.math();

                                        double monthlySalary = employee.getSalary() != null
                                                        ? employee.getSalary()
                                                        : 0.0;

                                        return new SalaryResponse(
                                                        employee.getId(),
                                                        employee.getEmployeeCode(),
                                                        employee.getName(),
                                                        monthlySalary,
                                                        math.workingDays(),
                                                        math.presentDays(),
                                                        PayrollMath.round2(
                                                                        math.paidLeaveDays()
                                                                                        + math.autoCoveredDays()),
                                                        math.lossOfPay(),
                                                        math.finalSalary());
                                })
                                .toList();
        }

        // =========================================
        // FIND LOGGED-IN ADMIN
        // =========================================

        private Admin findLoggedInAdmin(
                        String email) {

                if (email == null ||
                                email.trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Admin login required");
                }

                return adminRepository
                                .findByEmail(
                                                email.trim())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Logged-in admin not found"));
        }

        // =========================================
        // REASON VALIDATION
        // =========================================

        private void validateReason(
                        String reason) {

                if (reason == null ||
                                reason.trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Reason is required for this operation");
                }

                if (reason.trim().length() < 3) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Please enter a valid reason");
                }
        }

        // =========================================
        // SAVE HISTORY
        // =========================================

        private void saveHistory(
                        Admin admin,
                        String action,
                        Employee employee,
                        String fieldName,
                        String oldValue,
                        String newValue,
                        String reason) {

                AdminActionHistory history = new AdminActionHistory();

                history.setAdminName(
                                admin.getAdminName());

                history.setAction(
                                action);

                history.setEmployeeId(
                                employee.getId());

                history.setEmployeeCode(
                                employee.getEmployeeCode());

                history.setEmployeeName(
                                employee.getName());

                history.setReason(
                                reason.trim());

                ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

                history.setActionDate(
                                LocalDate.now(indiaZone));

                history.setActionTime(
                                LocalTime.now(indiaZone));

                history.setFieldName(
                                fieldName);

                history.setOldValue(
                                oldValue);

                history.setNewValue(
                                newValue);

                historyRepository.save(history);
        }
}