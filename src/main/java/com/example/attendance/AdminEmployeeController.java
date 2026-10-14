package com.example.attendance;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/employees")
public class AdminEmployeeController {

        private final EmployeeRepository employeeRepository;
        private final AdminRepository adminRepository;
        private final AdminActionHistoryRepository historyRepository;
        private final BranchRepository branchRepository;
        private final InternshipStipendRepository stipendRepository;

        public AdminEmployeeController(
                        EmployeeRepository employeeRepository,
                        AdminRepository adminRepository,
                        AdminActionHistoryRepository historyRepository,
                        BranchRepository branchRepository,
                        InternshipStipendRepository stipendRepository) {

                this.employeeRepository = employeeRepository;
                this.adminRepository = adminRepository;
                this.historyRepository = historyRepository;
                this.branchRepository = branchRepository;
                this.stipendRepository = stipendRepository;
        }

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

                validateEmployeeRequest(request);

                String email = request.getEmail().trim();
                String role = normalizeValue(request.getRole());
                String technology = request.getTechnology().trim();
                String designation = request.getDesignation().trim();

                boolean emailExists = employeeRepository.findAll()
                                .stream()
                                .anyMatch(existing -> existing.getEmail() != null
                                                && existing.getEmail().trim()
                                                                .equalsIgnoreCase(email));

                if (emailExists) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Employee with this email already exists");
                }

                // SAME PERSON GUARD
                //
                // The same name + contact number is the same person. A new
                // employee row (new id / code) must not be created when only
                // the email was wrong. Use Edit to change the email instead.

                String newName = request.getName().trim();
                String newContact = request.getContactNumber() == null
                                ? ""
                                : request.getContactNumber().trim();

                if (!newContact.isEmpty()) {

                        Optional<Employee> samePerson = employeeRepository.findAll()
                                        .stream()
                                        .filter(existing -> existing.getName() != null
                                                        && existing.getName().trim()
                                                                        .equalsIgnoreCase(newName)
                                                        && existing.getContactNumber() != null
                                                        && existing.getContactNumber().trim()
                                                                        .equals(newContact))
                                        .findFirst();

                        if (samePerson.isPresent()) {

                                Employee found = samePerson.get();

                                throw new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "This employee already exists as "
                                                                + found.getEmployeeCode()
                                                                + (Boolean.TRUE.equals(found.getActive())
                                                                                ? ""
                                                                                : " (inactive)")
                                                                + ". Use Edit to change the email or other details instead of adding a new employee");
                        }
                }

                String employeeCode = generateEmployeeCode();

                while (employeeCodeExists(employeeCode)) {
                        employeeCode = generateNextEmployeeCode(employeeCode);
                }

                Admin admin = findLoggedInAdmin(request.getAdminEmail());

                Employee employee = new Employee();

                employee.setEmployeeCode(employeeCode);
                employee.setName(request.getName().trim());
                employee.setEmail(email);
                employee.setContactNumber(request.getContactNumber());
                employee.setRole(role);
                employee.setTechnology(technology);
                employee.setDesignation(designation);

                // BRANCH ASSIGNMENT

                if (request.getBranchId() != null) {
                        employee.setBranch(findActiveBranch(request.getBranchId()));
                }

                if (request.getSalary() != null) {
                        employee.setSalary(request.getSalary());
                }

                // ORIGINAL JOINING DATE: SET ONLY WHEN CREATING EMPLOYEE

                employee.setJoiningDate(request.getJoiningDate());

                // INTERNSHIP DETAILS

                applyInternshipDetails(employee, request);

                // EMPLOYMENT DETAILS

                applyEmploymentDetails(employee, request);

                employee.setActive(true);

                // REASON + ADMIN + DATE/TIME ON THE EMPLOYEE ROW

                stampLastAction(employee, admin, "ADD", request.getReason());

                Employee savedEmployee;

                try {
                        savedEmployee = employeeRepository.save(employee);
                } catch (DataIntegrityViolationException exception) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Employee ID / Employee Code / Email already exists");
                }

                // MONTHLY STIPEND ROWS (INTERNS ONLY)

                saveInternshipStipends(savedEmployee, request);

                saveHistory(
                                admin,
                                "ADD",
                                savedEmployee,
                                request.getReason());

                return convertToResponse(savedEmployee);
        }

        // =====================================================
        // UPDATE EMPLOYEE
        // =====================================================

        @PutMapping("/{id}")
        public AdminEmployeeResponse updateEmployee(
                        @PathVariable Integer id,
                        @RequestBody AdminEmployeeActionRequest request) {

                Employee employee = employeeRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Employee not found"));

                validateEmployeeRequest(request);

                String email = request.getEmail().trim();
                String role = normalizeValue(request.getRole());
                String technology = request.getTechnology().trim();
                String designation = request.getDesignation().trim();

                boolean emailExists = employeeRepository.findAll()
                                .stream()
                                .anyMatch(existing -> existing.getId() != null
                                                && !existing.getId().equals(employee.getId())
                                                && existing.getEmail() != null
                                                && existing.getEmail().trim()
                                                                .equalsIgnoreCase(email));

                if (emailExists) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Employee with this email already exists");
                }

                String employeeCode = employee.getEmployeeCode();

                if (employeeCode == null || employeeCode.trim().isEmpty()) {

                        employeeCode = generateEmployeeCode();

                        while (employeeCodeExists(employeeCode, employee.getId())) {
                                employeeCode = generateNextEmployeeCode(employeeCode);
                        }
                }

                Admin admin = findLoggedInAdmin(request.getAdminEmail());

                employee.setEmployeeCode(employeeCode);
                employee.setName(request.getName().trim());
                employee.setEmail(email);
                employee.setContactNumber(request.getContactNumber());
                employee.setRole(role);
                employee.setTechnology(technology);
                employee.setDesignation(designation);

                // BRANCH ASSIGNMENT

                if (request.getBranchId() != null) {
                        employee.setBranch(findActiveBranch(request.getBranchId()));
                } else {
                        employee.setBranch(null);
                }

                /*
                 * JOINING DATE PROTECTION
                 *
                 * Never update joiningDate during employee edits.
                 * The original Joining Date must remain unchanged.
                 */

                // INTERNSHIP DETAILS

                applyInternshipDetails(employee, request);

                // EMPLOYMENT DETAILS

                applyEmploymentDetails(employee, request);

                // PRESERVE EXISTING SALARY IF OMITTED

                if (request.getSalary() != null) {
                        employee.setSalary(request.getSalary());
                }

                // REASON + ADMIN + DATE/TIME ON THE SAME EMPLOYEE ROW

                stampLastAction(employee, admin, "EDIT", request.getReason());

                Employee updatedEmployee;

                try {
                        updatedEmployee = employeeRepository.save(employee);
                } catch (DataIntegrityViolationException exception) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Employee ID / Employee Code / Email already exists");
                }

                // MONTHLY STIPEND ROWS (INTERNS ONLY)

                saveInternshipStipends(updatedEmployee, request);

                saveHistory(
                                admin,
                                "EDIT",
                                updatedEmployee,
                                request.getReason());

                return convertToResponse(updatedEmployee);
        }

        // =====================================================
        // STAMP LAST ADMIN ACTION ON THE EMPLOYEE ROW
        // =====================================================

        private void stampLastAction(
                        Employee employee,
                        Admin admin,
                        String actionType,
                        String reason) {

                employee.setLastActionType(actionType);
                employee.setLastActionReason(reason == null ? null : reason.trim());
                employee.setLastActionBy(admin.getAdminName());
                employee.setLastActionByEmail(admin.getEmail());
                employee.setLastActionAt(
                                OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
        }

        // =====================================================
        // IDENTIFY INTERNSHIP DESIGNATION
        // =====================================================

        private boolean isInternshipDesignation(String designation) {

                String normalized = normalizeValue(designation);

                return normalized.equals("INTERN")
                                || normalized.endsWith("_INTERN");
        }

        // =====================================================
        // APPLY INTERNSHIP DETAILS
        // =====================================================

        private void applyInternshipDetails(
                        Employee employee,
                        AdminEmployeeActionRequest request) {

                if (!isInternshipDesignation(request.getDesignation())) {
                        return;
                }

                LocalDate startDate = request.getInternshipStartDate();
                Integer duration = request.getInternshipDurationMonths();

                if (startDate == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Internship start date is required");
                }

                if (duration == null || duration < 1 || duration > 12) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Internship duration must be between 1 and 12 months");
                }

                String stipendType = normalizeValue(request.getStipendType());

                if (!stipendType.equals("PAID")
                                && !stipendType.equals("UNPAID")) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Stipend type must be PAID or UNPAID");
                }

                if (stipendType.equals("PAID")
                                && (request.getStipendAmount() == null
                                                || request.getStipendAmount() <= 0)) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Stipend amount is required for a paid internship");
                }

                LocalDate calculatedEndDate = startDate.plusMonths(duration).minusDays(1);

                if (request.getInternshipEndDate() != null
                                && !request.getInternshipEndDate().equals(calculatedEndDate)) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Internship end date does not match the selected duration");
                }

                employee.setInternshipStartDate(startDate);
                employee.setInternshipDurationMonths(duration);
                employee.setInternshipEndDate(calculatedEndDate);
                employee.setStipendType(stipendType);
        }

        // =====================================================
        // SAVE MONTHLY STIPEND ROWS
        // =====================================================

        private void saveInternshipStipends(
                        Employee employee,
                        AdminEmployeeActionRequest request) {

                if (!isInternshipDesignation(request.getDesignation())) {
                        return;
                }

                LocalDate startDate = employee.getInternshipStartDate();
                LocalDate endDate = employee.getInternshipEndDate();

                if (startDate == null || endDate == null) {
                        return;
                }

                String stipendType = normalizeValue(request.getStipendType());

                double amount = 0.0;

                if (stipendType.equals("PAID")) {
                        amount = request.getStipendAmount();
                }

                YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Kolkata"));
                YearMonth lastMonth = YearMonth.from(endDate);

                for (YearMonth month = YearMonth.from(startDate); !month.isAfter(lastMonth); month = month
                                .plusMonths(1)) {

                        Optional<InternshipStipend> existing = stipendRepository
                                        .findByEmployeeIdAndStipendYearAndStipendMonth(
                                                        employee.getId(),
                                                        month.getYear(),
                                                        month.getMonthValue());

                        // Preserve completed months' stipend history.
                        if (existing.isPresent() && month.isBefore(currentMonth)) {
                                continue;
                        }

                        InternshipStipend stipend = existing.orElseGet(InternshipStipend::new);

                        stipend.setEmployee(employee);
                        stipend.setStipendYear(month.getYear());
                        stipend.setStipendMonth(month.getMonthValue());
                        stipend.setStipendType(stipendType);
                        stipend.setStipendAmount(amount);

                        stipendRepository.save(stipend);
                }
        }

        // =====================================================
        // FIND CURRENT STIPEND AMOUNT FOR RESPONSE
        // =====================================================

        private Double findCurrentStipendAmount(Employee employee) {

                if (employee.getId() == null
                                || !isInternshipDesignation(employee.getDesignation())) {
                        return null;
                }

                List<InternshipStipend> rows = stipendRepository
                                .findByEmployeeIdOrderByStipendYearAscStipendMonthAsc(
                                                employee.getId());

                if (rows.isEmpty()) {
                        return null;
                }

                YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Kolkata"));
                InternshipStipend chosen = rows.get(0);

                for (InternshipStipend row : rows) {

                        YearMonth rowMonth = YearMonth.of(
                                        row.getStipendYear(),
                                        row.getStipendMonth());

                        if (!rowMonth.isAfter(currentMonth)) {
                                chosen = row;
                        }
                }

                return chosen.getStipendAmount();
        }

        // =====================================================
        // APPLY EMPLOYMENT DETAILS
        // =====================================================

        private void applyEmploymentDetails(
                        Employee employee,
                        AdminEmployeeActionRequest request) {

                if (isInternshipDesignation(request.getDesignation())) {
                        return;
                }

                LocalDate startDate = request.getEmploymentStartDate();

                if (startDate == null) {
                        startDate = employee.getEmploymentStartDate();
                }

                if (startDate == null
                                && request.getJoiningDate() != null
                                && !request.getJoiningDate().trim().isEmpty()) {

                        try {
                                startDate = LocalDate.parse(
                                                request.getJoiningDate().trim());
                        } catch (Exception ignored) {
                                // Validation below handles a missing valid date.
                        }
                }

                if (startDate == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Employee start date is required");
                }

                LocalDate endDate = request.getEmploymentEndDate();

                if (endDate == null
                                && employee.getEmploymentStartDate() != null
                                && employee.getEmploymentStartDate().equals(startDate)) {

                        endDate = employee.getEmploymentEndDate();
                }

                // Default contract period: start date + one year - one day.

                if (endDate == null) {
                        endDate = startDate.plusYears(1).minusDays(1);
                }

                if (endDate.isBefore(startDate)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Employee end date cannot be before the start date");
                }

                employee.setEmploymentStartDate(startDate);
                employee.setEmploymentEndDate(endDate);
        }

        // =====================================================
        // DELETE EMPLOYEE
        // =====================================================

        @DeleteMapping("/{id}")
        public String deleteEmployee(
                        @PathVariable Integer id,
                        @RequestBody AdminEmployeeActionRequest request) {

                Employee employee = employeeRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Employee not found"));

                validateReason(request.getReason());

                Admin admin = findLoggedInAdmin(request.getAdminEmail());

                employee.setActive(false);

                stampLastAction(employee, admin, "DELETE", request.getReason());

                employeeRepository.save(employee);

                saveHistory(
                                admin,
                                "DELETE",
                                employee,
                                request.getReason());

                return "Employee deleted successfully";
        }

        // =====================================================
        // EMPLOYEE REQUEST VALIDATION
        // =====================================================

        private void validateEmployeeRequest(
                        AdminEmployeeActionRequest request) {

                if (request == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Employee request is required");
                }

                if (request.getName() == null
                                || request.getName().trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Employee name is required");
                }

                if (request.getEmail() == null
                                || request.getEmail().trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Employee email is required");
                }

                if (request.getRole() == null
                                || request.getRole().trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Employee role is required");
                }

                if (request.getTechnology() == null
                                || request.getTechnology().trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Employee technology is required");
                }

                if (request.getDesignation() == null
                                || request.getDesignation().trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Employee designation is required");
                }

                if (request.getJoiningDate() == null
                                || request.getJoiningDate().trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Joining date is required");
                }

                validateReason(request.getReason());
        }

        // =====================================================
        // FIND ACTIVE BRANCH
        // =====================================================

        private Branch findActiveBranch(Long branchId) {

                return branchRepository.findById(branchId)
                                .filter(branch -> Boolean.TRUE.equals(branch.getActive()))
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Active branch not found"));
        }

        // =====================================================
        // NORMALIZE VALUE
        // =====================================================

        private String normalizeValue(String value) {

                if (value == null) {
                        return "";
                }

                return value.trim()
                                .toUpperCase()
                                .replace("-", "_")
                                .replace(" ", "_");
        }

        // =====================================================
        // FIND LOGGED-IN ADMIN
        // =====================================================

        private Admin findLoggedInAdmin(String email) {

                if (email == null || email.trim().isEmpty()) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Admin login required");
                }

                return adminRepository.findByEmail(email.trim())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Logged-in admin not found"));
        }

        // =====================================================
        // REASON VALIDATION
        // =====================================================

        private void validateReason(String reason) {

                if (reason == null || reason.trim().isEmpty()) {
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

        // =====================================================
        // GENERATE EMPLOYEE CODE
        // =====================================================

        private String generateEmployeeCode() {

                int highestNumber = 0;
                List<Employee> allEmployees = employeeRepository.findAll();

                for (Employee employee : allEmployees) {

                        String code = employee.getEmployeeCode();

                        if (code == null) {
                                continue;
                        }

                        code = code.trim().toUpperCase();

                        if (!code.startsWith("EMP")) {
                                continue;
                        }

                        String numberPart = code.substring(3);

                        try {
                                int number = Integer.parseInt(numberPart);

                                if (number > highestNumber) {
                                        highestNumber = number;
                                }
                        } catch (NumberFormatException ignored) {
                                // Ignore invalid employee codes.
                        }
                }

                return String.format("EMP%03d", highestNumber + 1);
        }

        // =====================================================
        // CHECK EMPLOYEE CODE
        // =====================================================

        private boolean employeeCodeExists(String employeeCode) {

                if (employeeCode == null || employeeCode.trim().isEmpty()) {
                        return false;
                }

                String code = employeeCode.trim();

                return employeeRepository.findAll()
                                .stream()
                                .anyMatch(employee -> employee.getEmployeeCode() != null
                                                && employee.getEmployeeCode().trim()
                                                                .equalsIgnoreCase(code));
        }

        // =====================================================
        // CHECK EMPLOYEE CODE EXCLUDING CURRENT EMPLOYEE
        // =====================================================

        private boolean employeeCodeExists(
                        String employeeCode,
                        Integer currentEmployeeId) {

                if (employeeCode == null || employeeCode.trim().isEmpty()) {
                        return false;
                }

                String code = employeeCode.trim();

                return employeeRepository.findAll()
                                .stream()
                                .anyMatch(employee -> employee.getId() != null
                                                && !employee.getId().equals(currentEmployeeId)
                                                && employee.getEmployeeCode() != null
                                                && employee.getEmployeeCode().trim()
                                                                .equalsIgnoreCase(code));
        }

        // =====================================================
        // GENERATE NEXT EMPLOYEE CODE
        // =====================================================

        private String generateNextEmployeeCode(String currentCode) {

                if (currentCode == null
                                || !currentCode.trim().toUpperCase().startsWith("EMP")) {

                        return generateEmployeeCode();
                }

                String code = currentCode.trim().toUpperCase();
                String numberPart = code.substring(3);

                try {
                        int currentNumber = Integer.parseInt(numberPart);

                        return String.format("EMP%03d", currentNumber + 1);
                } catch (NumberFormatException exception) {
                        return generateEmployeeCode();
                }
        }

        // =====================================================
        // SAVE ADMIN HISTORY
        // =====================================================

        private void saveHistory(
                        Admin admin,
                        String action,
                        Employee employee,
                        String reason) {

                AdminActionHistory history = new AdminActionHistory();

                history.setAdminName(admin.getAdminName());
                history.setAction(action);
                history.setEmployeeId(employee.getId());
                history.setEmployeeCode(employee.getEmployeeCode());
                history.setEmployeeName(employee.getName());
                history.setReason(reason.trim());

                ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

                history.setActionDate(LocalDate.now(indiaZone));
                history.setActionTime(LocalTime.now(indiaZone));

                historyRepository.save(history);
        }

        // =====================================================
        // RESPONSE CONVERTER
        // =====================================================

        private AdminEmployeeResponse convertToResponse(Employee employee) {

                Long branchId = null;
                String branchName = null;

                if (employee.getBranch() != null) {
                        branchId = employee.getBranch().getId();
                        branchName = employee.getBranch().getBranchName();
                }

                AdminEmployeeResponse response = new AdminEmployeeResponse(
                                employee.getId(),
                                employee.getEmployeeCode(),
                                employee.getName(),
                                employee.getEmail(),
                                employee.getContactNumber(),
                                employee.getRole(),
                                employee.getTechnology(),
                                employee.getDesignation(),
                                employee.getSalary(),
                                employee.getJoiningDate(),
                                branchId,
                                branchName);

                response.setInternshipStartDate(
                                employee.getInternshipStartDate());

                response.setInternshipDurationMonths(
                                employee.getInternshipDurationMonths());

                response.setInternshipEndDate(
                                employee.getInternshipEndDate());

                response.setStipendType(
                                employee.getStipendType());

                response.setStipendAmount(
                                findCurrentStipendAmount(employee));

                response.setEmploymentStartDate(
                                employee.getEmploymentStartDate());

                response.setEmploymentEndDate(
                                employee.getEmploymentEndDate());

                return response;
        }
}