
package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@RestController
@RequestMapping("/admin/verification-policies")
@CrossOrigin
public class VerificationPolicyController {

    private final VerificationPolicyRepository policyRepository;
    private final BranchRepository branchRepository;
    private final AdminRepository adminRepository;
    private final EmployeeRepository employeeRepository;

    public VerificationPolicyController(
            VerificationPolicyRepository policyRepository,
            BranchRepository branchRepository,
            AdminRepository adminRepository,
            EmployeeRepository employeeRepository) {
        this.policyRepository = policyRepository;
        this.branchRepository = branchRepository;
        this.adminRepository = adminRepository;
        this.employeeRepository = employeeRepository;
    }

    // =====================================================
    // POLICY DROPDOWN OPTIONS
    // No password, salary or other sensitive fields returned
    // =====================================================

    @GetMapping("/options")
    public ResponseEntity<?> getPolicyOptions(
            @RequestParam String adminEmail) {

        if (!isAdmin(adminEmail)) {
            return error(HttpStatus.FORBIDDEN, "Admin access required.");
        }

        List<Employee> employees = employeeRepository.findByActiveTrue();

        List<EmployeeOption> employeeOptions = employees.stream()
                .sorted(Comparator.comparing(
                        e -> safe(e.getName()),
                        String.CASE_INSENSITIVE_ORDER))
                .map(e -> new EmployeeOption(
                        e.getId(),
                        e.getEmployeeCode(),
                        e.getName(),
                        e.getEmail(),
                        e.getRole(),
                        e.getDesignation(),
                        e.getEmployeeType(),
                        e.getCourse()))
                .toList();

        List<String> roles = employees.stream()
                .map(Employee::getRole)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> designations = employees.stream()
                .map(Employee::getDesignation)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<EmployeeOption> interns = employees.stream()
                .filter(this::isIntern)
                .map(this::toEmployeeOption)
                .toList();

        List<EmployeeOption> students = employees.stream()
                .filter(this::isStudent)
                .map(this::toEmployeeOption)
                .toList();

        return ResponseEntity.ok(Map.of(
                "employees", employeeOptions,
                "roles", roles,
                "designations", designations,
                "interns", interns,
                "students", students,
                "managerMappingAvailable", false));
    }

    private EmployeeOption toEmployeeOption(Employee employee) {
        return new EmployeeOption(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole(),
                employee.getDesignation(),
                employee.getEmployeeType(),
                employee.getCourse());
    }

    private boolean isIntern(Employee employee) {
        return contains(employee.getEmployeeType(), "intern")
                || contains(employee.getDesignation(), "intern")
                || contains(employee.getRole(), "intern")
                || employee.getInternshipStartDate() != null
                || employee.getInternshipEndDate() != null
                || employee.getInternshipDurationMonths() != null
                || employee.getStipendType() != null;
    }

    private boolean isStudent(Employee employee) {
        return contains(employee.getEmployeeType(), "student")
                || contains(employee.getDesignation(), "student")
                || contains(employee.getRole(), "student")
                || (employee.getCourse() != null
                        && !employee.getCourse().trim().isEmpty());
    }

    private boolean contains(String value, String search) {
        return value != null
                && value.toLowerCase().contains(search);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    // =====================================================
    // LIST ACTIVE POLICIES
    // =====================================================

    @GetMapping
    public ResponseEntity<?> getPolicies(
            @RequestParam String adminEmail) {

        if (!isAdmin(adminEmail)) {
            return error(HttpStatus.FORBIDDEN, "Admin access required.");
        }

        return ResponseEntity.ok(
                policyRepository.findByActiveTrueOrderByIdDesc());
    }

    // =====================================================
    // CREATE POLICY
    // =====================================================

    @PostMapping
    public ResponseEntity<?> createPolicy(
            @RequestBody PolicyRequest request) {

        if (request == null || !isAdmin(request.adminEmail)) {
            return error(HttpStatus.FORBIDDEN, "Admin access required.");
        }

        String validation = validate(request);
        if (validation != null) {
            return error(HttpStatus.BAD_REQUEST, validation);
        }

        VerificationPolicy policy = new VerificationPolicy();
        applyRequest(policy, request);
        policy.setCreatedByEmail(request.adminEmail.trim());
        policy.setActive(true);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(policyRepository.save(policy));
    }

    // =====================================================
    // UPDATE POLICY
    // =====================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePolicy(
            @PathVariable Long id,
            @RequestBody PolicyRequest request) {

        if (request == null || !isAdmin(request.adminEmail)) {
            return error(HttpStatus.FORBIDDEN, "Admin access required.");
        }

        String validation = validate(request);
        if (validation != null) {
            return error(HttpStatus.BAD_REQUEST, validation);
        }

        VerificationPolicy policy = policyRepository.findById(id)
                .orElse(null);

        if (policy == null) {
            return error(HttpStatus.NOT_FOUND, "Policy not found.");
        }

        applyRequest(policy, request);
        return ResponseEntity.ok(policyRepository.save(policy));
    }

    // =====================================================
    // DEACTIVATE POLICY
    // =====================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivatePolicy(
            @PathVariable Long id,
            @RequestParam String adminEmail,
            @RequestParam String reason) {

        if (!isAdmin(adminEmail)) {
            return error(HttpStatus.FORBIDDEN, "Admin access required.");
        }

        if (reason == null || reason.trim().length() < 3) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Please provide a reason (minimum 3 characters).");
        }

        VerificationPolicy policy = policyRepository.findById(id)
                .orElse(null);

        if (policy == null) {
            return error(HttpStatus.NOT_FOUND, "Policy not found.");
        }

        policy.setActive(false);
        policy.setReason(reason.trim());

        return ResponseEntity.ok(policyRepository.save(policy));
    }

    // =====================================================
    // APPLY REQUEST FIELDS
    // =====================================================

    private void applyRequest(
            VerificationPolicy policy,
            PolicyRequest request) {

        policy.setPolicyName(request.policyName.trim());
        policy.setTargetType(request.targetType.trim().toUpperCase());

        policy.setTargetValue(
                request.targetValue == null
                        || request.targetValue.trim().isEmpty()
                                ? null
                                : request.targetValue.trim());

        policy.setBranch(
                request.branchId == null
                        ? null
                        : branchRepository.findById(request.branchId)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Branch not found.")));

        policy.setStartDate(request.startDate);
        policy.setEndDate(request.endDate);
        policy.setStartTime(request.startTime);
        policy.setEndTime(request.endTime);

        policy.setLocationVerificationEnabled(
                Boolean.TRUE.equals(request.locationVerificationEnabled));

        policy.setFaceVerificationEnabled(
                Boolean.TRUE.equals(request.faceVerificationEnabled));

        policy.setLocationCaptureEnabled(
                request.locationCaptureEnabled == null
                        || request.locationCaptureEnabled);

        policy.setPhotoCaptureEnabled(
                Boolean.TRUE.equals(request.photoCaptureEnabled));

        policy.setReason(request.reason.trim());
    }

    // =====================================================
    // VALIDATION
    // =====================================================

    private String validate(PolicyRequest request) {

        if (request.policyName == null
                || request.policyName.trim().isEmpty()) {
            return "Policy name is required.";
        }

        if (request.targetType == null) {
            return "Target type is required.";
        }

        String type = request.targetType.trim().toUpperCase();

        if (!List.of(
                "DEFAULT", "ROLE", "DESIGNATION", "MANAGER",
                "INTERN", "STUDENT", "EMPLOYEE").contains(type)) {
            return "Invalid target type.";
        }

        if (List.of("ROLE", "DESIGNATION", "MANAGER", "EMPLOYEE")
                .contains(type)
                && (request.targetValue == null
                        || request.targetValue.trim().isEmpty())) {
            return "Target value is required for " + type + ".";
        }

        if (request.startDate != null
                && request.endDate != null
                && request.endDate.isBefore(request.startDate)) {
            return "End date cannot be before start date.";
        }

        if ((request.startTime == null) != (request.endTime == null)) {
            return "Enter both start time and end time.";
        }

        if (request.reason == null
                || request.reason.trim().length() < 3) {
            return "Please provide a reason (minimum 3 characters).";
        }

        if (request.adminEmail == null
                || request.adminEmail.trim().isEmpty()) {
            return "Admin email is required.";
        }

        return null;
    }

    // =====================================================
    // ADMIN CHECK
    // =====================================================

    private boolean isAdmin(String email) {
        return email != null
                && !email.trim().isEmpty()
                && adminRepository.findByEmail(email.trim()).isPresent();
    }

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status,
            String message) {

        return ResponseEntity.status(status)
                .body(Map.of("message", message));
    }

    // =====================================================
    // SAFE DROPDOWN RESPONSE DTO
    // =====================================================

    public static class EmployeeOption {
        public Integer id;
        public String employeeCode;
        public String name;
        public String email;
        public String role;
        public String designation;
        public String employeeType;
        public String course;

        public EmployeeOption(
                Integer id,
                String employeeCode,
                String name,
                String email,
                String role,
                String designation,
                String employeeType,
                String course) {
            this.id = id;
            this.employeeCode = employeeCode;
            this.name = name;
            this.email = email;
            this.role = role;
            this.designation = designation;
            this.employeeType = employeeType;
            this.course = course;
        }
    }

    // =====================================================
    // REQUEST DTO
    // =====================================================

    public static class PolicyRequest {
        public String adminEmail;
        public String policyName;
        public Long branchId;
        public String targetType;
        public String targetValue;

        public LocalDate startDate;
        public LocalDate endDate;
        public LocalTime startTime;
        public LocalTime endTime;

        public Boolean locationVerificationEnabled;
        public Boolean faceVerificationEnabled;
        public Boolean locationCaptureEnabled;
        public Boolean photoCaptureEnabled;

        public String reason;
    }
}
