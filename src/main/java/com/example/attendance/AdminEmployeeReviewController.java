package com.example.attendance;

import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = {
                "https://attendance-management-3d-webinar.vercel.app",
                "https://attendance-management-git-main-3d-webinar.vercel.app",
                "https://attendance-management-lhsosyu8t-3d-webinar.vercel.app",
                "https://attendance-management-nine-beige.vercel.app"
})
@RequestMapping("/admin/employees")
public class AdminEmployeeReviewController {

        private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

        private final EmployeeRepository employeeRepository;
        private final AdminRepository adminRepository;
        private final AdminActionHistoryRepository historyRepository;
        private final InternshipStipendRepository stipendRepository;
        private final EmploymentChangeHistoryRepository employmentHistoryRepository;
        private final JavaMailSender mailSender;

        public AdminEmployeeReviewController(
                        EmployeeRepository employeeRepository,
                        AdminRepository adminRepository,
                        AdminActionHistoryRepository historyRepository,
                        InternshipStipendRepository stipendRepository,
                        EmploymentChangeHistoryRepository employmentHistoryRepository,
                        JavaMailSender mailSender) {

                this.employeeRepository = employeeRepository;
                this.adminRepository = adminRepository;
                this.historyRepository = historyRepository;
                this.stipendRepository = stipendRepository;
                this.employmentHistoryRepository = employmentHistoryRepository;
                this.mailSender = mailSender;
        }

        // =====================================================
        // REQUEST
        // =====================================================

        public record ReviewRequest(
                        String adminEmail,
                        String reason,
                        String action,
                        Double newSalary,
                        String newDesignation,
                        Integer extendMonths,
                        String newRole,
                        String newTechnology,
                        LocalDate newEmploymentStartDate,
                        LocalDate newEmploymentEndDate,
                        LocalDate effectiveDate) {
        }

        public record EmployeeChangeNotice(
                        Long id,
                        String actionType,
                        String employeeCode,
                        String employeeName,
                        BigDecimal oldSalary,
                        BigDecimal newSalary,
                        String oldRole,
                        String newRole,
                        String oldDesignation,
                        String newDesignation,
                        LocalDate oldEmploymentStartDate,
                        LocalDate newEmploymentStartDate,
                        LocalDate oldEmploymentEndDate,
                        LocalDate newEmploymentEndDate,
                        LocalDate effectiveDate,
                        String adminName,
                        String reason,
                        OffsetDateTime createdAt) {
        }

        // =====================================================
        // ADMIN REVIEW
        // =====================================================

        @PostMapping("/{id}/review")
        @Transactional
        public String reviewEmployee(
                        @PathVariable Integer id,
                        @RequestBody ReviewRequest request) {

                if (request == null) {
                        throw bad("Request is required");
                }

                Admin admin = findLoggedInAdmin(request.adminEmail());
                validateReason(request.reason());

                String reason = request.reason().trim();

                Employee employee = employeeRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Employee not found"));

                if (!Boolean.TRUE.equals(employee.getActive())) {
                        throw bad("Employee is inactive");
                }

                String action = request.action() == null
                                ? ""
                                : request.action().trim().toUpperCase();

                boolean intern = isInternDesignation(employee.getDesignation());

                switch (action) {

                        case "INCREMENT":
                        case "PROMOTION":
                        case "INCREMENT_PROMOTION":
                        case "YEAR_COMPLETE":

                                if (intern) {
                                        throw bad(
                                                        "This action is for employees. Use the intern options.");
                                }

                                reviewEmployeeAction(
                                                admin, employee, action, request, reason);

                                return "Employee review saved successfully";

                        case "INTERN_EXTEND":

                                requireIntern(intern);

                                extendInternship(admin, employee, request, reason);

                                return "Internship extended successfully";

                        case "INTERN_CERTIFICATE":

                                requireIntern(intern);

                                // Reason + admin + date/time on the SAME employee row.
                                stampLastAction(employee, admin, "INTERN_CERTIFICATE", reason);
                                employeeRepository.save(employee);

                                // The intern must SEE the completion popup, so the account
                                // stays active here. It becomes INACTIVE when the intern
                                // clicks OK (see acknowledgeEmployeeChange).
                                EmploymentChangeHistory certificateNotice = saveInternNotice(
                                                admin,
                                                employee,
                                                "INTERN_CERTIFICATE",
                                                reason,
                                                employee.getInternshipStartDate(),
                                                employee.getInternshipEndDate(),
                                                employee.getInternshipEndDate());

                                saveHistory(
                                                admin,
                                                "INTERNSHIP COMPLETED - CERTIFICATE",
                                                employee,
                                                "Status",
                                                "ACTIVE",
                                                "COMPLETED - INACTIVE AFTER EMPLOYEE OK",
                                                reason);

                                sendChangeEmail(
                                                employee,
                                                certificateNotice,
                                                employee.getTechnology(),
                                                employee.getTechnology(),
                                                false);

                                return "Internship completed. The intern will see the completion popup and the account becomes inactive after the intern clicks OK";

                        case "INTERN_CONVERT":

                                requireIntern(intern);

                                convertInternToEmployee(
                                                admin, employee, request, reason);

                                return "Intern converted to employee successfully";

                        default:
                                throw bad("Invalid review action");
                }
        }

        // =====================================================
        // INCREMENT / PROMOTION / INCREMENT + PROMOTION / YEAR COMPLETE
        // =====================================================

        private void reviewEmployeeAction(
                        Admin admin,
                        Employee employee,
                        String action,
                        ReviewRequest request,
                        String reason) {

                boolean increment = action.equals("INCREMENT")
                                || action.equals("INCREMENT_PROMOTION");

                boolean promotion = action.equals("PROMOTION")
                                || action.equals("INCREMENT_PROMOTION");

                double oldSalary = employee.getSalary() == null
                                ? 0.0
                                : employee.getSalary();

                String oldRole = employee.getRole();
                String oldDesignation = employee.getDesignation();

                LocalDate oldStart = employee.getEmploymentStartDate();
                LocalDate oldEnd = employee.getEmploymentEndDate();

                if (increment) {
                        if (request.newSalary() == null
                                        || request.newSalary() <= oldSalary) {
                                throw bad(
                                                "New salary must be higher than the current salary");
                        }
                }

                String newDesignation = oldDesignation;
                String newRole = oldRole;

                String oldTechnology = employee.getTechnology();
                String newTechnology = oldTechnology;

                if (promotion) {
                        newDesignation = request.newDesignation() == null
                                        ? ""
                                        : request.newDesignation().trim();

                        if (newDesignation.isEmpty()) {
                                throw bad("New designation is required");
                        }

                        if (isInternDesignation(newDesignation)) {
                                throw bad(
                                                "Promotion designation cannot be an intern designation");
                        }

                        // Any role / technology can be selected (any department).
                        if (request.newRole() != null
                                        && !request.newRole().trim().isEmpty()) {
                                newRole = request.newRole().trim();
                        }

                        if (request.newTechnology() != null
                                        && !request.newTechnology().trim().isEmpty()) {
                                newTechnology = request.newTechnology().trim();
                        }

                        boolean roleChanged = !newRole.equalsIgnoreCase(
                                        oldRole == null ? "" : oldRole.trim());

                        boolean technologyChanged = !newTechnology.equalsIgnoreCase(
                                        oldTechnology == null ? "" : oldTechnology.trim());

                        if (roleChanged && !technologyChanged) {
                                throw bad(
                                                "Select the technology / tool for the new role");
                        }

                        if (!roleChanged
                                        && !technologyChanged
                                        && newDesignation.equalsIgnoreCase(
                                                        oldDesignation == null ? "" : oldDesignation.trim())) {
                                throw bad(
                                                "Nothing to promote: role, technology and designation are the same as the current values");
                        }
                }

                // =================================================
                // EMPLOYMENT DATES
                //
                // Increment / Promotion NEVER change the Start Date or
                // End Date by themselves. A date is saved only when the
                // Admin sent a value that is different from the saved
                // one. The original Joining Date is never touched here.
                // =================================================

                LocalDate newStart = oldStart;
                LocalDate newEnd = oldEnd;

                if (request.newEmploymentStartDate() != null
                                && !request.newEmploymentStartDate().equals(oldStart)) {
                        newStart = request.newEmploymentStartDate();
                }

                if (request.newEmploymentEndDate() != null
                                && !request.newEmploymentEndDate().equals(oldEnd)) {
                        newEnd = request.newEmploymentEndDate();
                }

                // Start date changed but End date was not supplied:
                // use the default one year term.
                if (!java.util.Objects.equals(newStart, oldStart)
                                && java.util.Objects.equals(newEnd, oldEnd)
                                && newStart != null) {
                        newEnd = newStart.plusYears(1).minusDays(1);
                }

                if (newStart != null && newEnd != null && newEnd.isBefore(newStart)) {
                        throw bad("Employment end date cannot be before start date");
                }

                // One year completed (no salary / designation change):
                // the new employment end date is required.
                if (action.equals("YEAR_COMPLETE")
                                && java.util.Objects.equals(newEnd, oldEnd)) {
                        throw bad(
                                        "Set the new employment end date for the next year");
                }

                // Effective date: when the change starts (default today).
                // A future date is saved as SCHEDULED and applied on that day.
                LocalDate effectiveDate = request.effectiveDate() != null
                                ? request.effectiveDate()
                                : LocalDate.now(INDIA_ZONE);

                EmploymentChangeHistory history = new EmploymentChangeHistory();

                history.setEmployeeId(employee.getId());
                history.setEmployeeCode(employee.getEmployeeCode());
                history.setEmployeeName(employee.getName());

                history.setActionType(action);

                history.setOldSalary(BigDecimal.valueOf(oldSalary));
                history.setNewSalary(increment
                                ? BigDecimal.valueOf(request.newSalary())
                                : BigDecimal.valueOf(oldSalary));

                history.setOldRole(oldRole);
                history.setNewRole(newRole);

                history.setOldDesignation(oldDesignation);
                history.setNewDesignation(newDesignation);

                history.setOldEmploymentStartDate(oldStart);
                history.setNewEmploymentStartDate(newStart);

                history.setOldEmploymentEndDate(oldEnd);
                history.setNewEmploymentEndDate(newEnd);

                history.setEffectiveDate(effectiveDate);

                history.setAdminName(admin.getAdminName());
                history.setAdminEmail(admin.getEmail());
                history.setReason(reason);

                LocalDate today = LocalDate.now(INDIA_ZONE);

                if (effectiveDate.isAfter(today)) {

                        if (promotion
                                        && newTechnology != null
                                        && !newTechnology.equals(oldTechnology)) {
                                throw bad(
                                                "A technology change cannot be scheduled for a future date. Use today's date as the effective date");
                        }

                        history.setStatus("SCHEDULED");
                        history.setEmployeeAcknowledged(false);

                        employmentHistoryRepository.save(history);

                        sendChangeEmail(employee, history, oldTechnology, newTechnology, true);

                        return;
                }

                history.setStatus("APPLIED");
                history.setEmployeeAcknowledged(false);

                employmentHistoryRepository.save(history);

                if (promotion
                                && newTechnology != null
                                && !newTechnology.equals(oldTechnology)) {

                        employee.setTechnology(newTechnology);

                        saveHistory(
                                        admin,
                                        "PROMOTION",
                                        employee,
                                        "Technology",
                                        oldTechnology,
                                        newTechnology,
                                        reason);
                }

                applyChange(history, employee, admin);

                sendChangeEmail(employee, history, oldTechnology, newTechnology, false);
        }

        // =====================================================
        // EMAIL TO THE EMPLOYEE
        //
        // Sent in the background after a review is saved. A mail
        // problem never stops or rolls back the review.
        // =====================================================

        private void sendChangeEmail(
                        Employee employee,
                        EmploymentChangeHistory history,
                        String oldTechnology,
                        String newTechnology,
                        boolean scheduled) {

                String to = employee.getEmail();

                if (to == null || to.trim().isEmpty()) {
                        return;
                }

                DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                String action = history.getActionType() == null
                                ? ""
                                : history.getActionType();

                String title;

                switch (action) {
                        case "INCREMENT":
                                title = "Salary Increment";
                                break;
                        case "PROMOTION":
                                title = "Promotion";
                                break;
                        case "INCREMENT_PROMOTION":
                                title = "Salary Increment and Promotion";
                                break;
                        case "YEAR_COMPLETE":
                                title = "One Year Completed";
                                break;
                        case "INTERN_CONVERT":
                                title = "Intern Converted to Employee";
                                break;
                        case "INTERN_EXTEND":
                                title = "Internship Extended";
                                break;
                        case "INTERN_CERTIFICATE":
                                title = "Internship Completed";
                                break;
                        default:
                                title = "Employment Update";
                }

                StringBuilder text = new StringBuilder();

                text.append("Dear ").append(employee.getName()).append(",\n\n");

                text.append(scheduled
                                ? "The following change will be effective on "
                                                + (history.getEffectiveDate() == null
                                                                ? "-"
                                                                : history.getEffectiveDate().format(dateFormat))
                                                + ":\n\n"
                                : "The following change has been made to your employment:\n\n");

                text.append("Type: ").append(title).append("\n");

                if (history.getOldSalary() != null
                                && history.getNewSalary() != null
                                && history.getOldSalary().compareTo(history.getNewSalary()) != 0) {
                        text.append("Salary: Rs. ").append(history.getOldSalary())
                                        .append(" -> Rs. ").append(history.getNewSalary()).append("\n");
                }

                if (!java.util.Objects.equals(history.getOldRole(), history.getNewRole())) {
                        text.append("Role: ").append(history.getOldRole())
                                        .append(" -> ").append(history.getNewRole()).append("\n");
                }

                if (!java.util.Objects.equals(oldTechnology, newTechnology)) {
                        text.append("Technology / Tool: ").append(oldTechnology)
                                        .append(" -> ").append(newTechnology).append("\n");
                }

                if (!java.util.Objects.equals(history.getOldDesignation(), history.getNewDesignation())) {
                        text.append("Designation: ").append(history.getOldDesignation())
                                        .append(" -> ").append(history.getNewDesignation()).append("\n");
                }

                if (history.getNewEmploymentStartDate() != null
                                && history.getNewEmploymentEndDate() != null) {
                        text.append(action.startsWith("INTERN_") && !action.equals("INTERN_CONVERT")
                                        ? "Internship period: "
                                        : "Employment period: ")
                                        .append(history.getNewEmploymentStartDate().format(dateFormat))
                                        .append(" to ")
                                        .append(history.getNewEmploymentEndDate().format(dateFormat))
                                        .append("\n");
                }

                if (history.getEffectiveDate() != null) {
                        text.append("Effective date: ")
                                        .append(history.getEffectiveDate().format(dateFormat)).append("\n");
                }

                text.append("\nReason: ").append(history.getReason()).append("\n");

                text.append("\nYou will also see this update on your Employee Dashboard.\n\nRegards,\nGROWBYTEE");

                String subject = "GROWBYTEE - " + title;
                String body = text.toString();
                String recipient = to.trim();

                CompletableFuture.runAsync(() -> {
                        try {
                                MimeMessage message = mailSender.createMimeMessage();

                                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

                                helper.setTo(recipient);
                                helper.setSubject(subject);
                                helper.setText(body);

                                mailSender.send(message);

                        } catch (Exception exception) {
                                System.err.println("Employment change email failed for " + recipient + ": "
                                                + exception.getMessage());
                        }
                });
        }

        // =====================================================
        // APPLY A CHANGE
        // =====================================================

        private void applyChange(
                        EmploymentChangeHistory history,
                        Employee employee,
                        Admin admin) {

                String action = history.getActionType();

                if (history.getNewSalary() != null
                                && (action.equals("INCREMENT")
                                                || action.equals("INCREMENT_PROMOTION"))) {

                        employee.setSalary(history.getNewSalary().doubleValue());

                        saveHistory(
                                        admin,
                                        "SALARY UPDATE",
                                        employee,
                                        "Salary",
                                        String.valueOf(history.getOldSalary()),
                                        String.valueOf(history.getNewSalary()),
                                        history.getReason());
                }

                if (history.getNewRole() != null
                                && !history.getNewRole().equals(history.getOldRole())) {
                        employee.setRole(history.getNewRole());

                        saveHistory(
                                        admin,
                                        "PROMOTION",
                                        employee,
                                        "Role",
                                        history.getOldRole(),
                                        history.getNewRole(),
                                        history.getReason());
                }

                if (history.getNewDesignation() != null
                                && (action.equals("PROMOTION")
                                                || action.equals("INCREMENT_PROMOTION"))) {

                        employee.setDesignation(history.getNewDesignation());

                        saveHistory(
                                        admin,
                                        "PROMOTION",
                                        employee,
                                        "Designation",
                                        history.getOldDesignation(),
                                        history.getNewDesignation(),
                                        history.getReason());
                }

                // Employment dates are saved ONLY when they were changed.
                // The original Joining Date is never changed.
                boolean employmentDatesChanged = !java.util.Objects.equals(
                                history.getOldEmploymentStartDate(),
                                history.getNewEmploymentStartDate())
                                || !java.util.Objects.equals(
                                                history.getOldEmploymentEndDate(),
                                                history.getNewEmploymentEndDate());

                if (employmentDatesChanged
                                && (action.equals("INCREMENT")
                                                || action.equals("PROMOTION")
                                                || action.equals("INCREMENT_PROMOTION")
                                                || action.equals("YEAR_COMPLETE"))) {

                        employee.setEmploymentStartDate(
                                        history.getNewEmploymentStartDate());

                        employee.setEmploymentEndDate(
                                        history.getNewEmploymentEndDate());

                        if (action.equals("YEAR_COMPLETE")) {
                                saveHistory(
                                                admin,
                                                "YEAR COMPLETED",
                                                employee,
                                                "Employment End Date",
                                                String.valueOf(history.getOldEmploymentEndDate()),
                                                String.valueOf(history.getNewEmploymentEndDate()),
                                                history.getReason());
                        }
                }

                // Reason + admin + date/time are updated on the SAME employee row.
                stampLastAction(employee, admin, action, history.getReason());

                employeeRepository.save(employee);

                history.setStatus("APPLIED");
                history.setAppliedAt(OffsetDateTime.now(INDIA_ZONE));

                employmentHistoryRepository.save(history);
        }

        // =====================================================
        // EMPLOYEE DASHBOARD: APPLY DUE CHANGES AND GET NOTICES
        //
        // GET /admin/employees/changes/pending?email=employee@email.com
        // =====================================================

        @GetMapping("/changes/pending")
        @Transactional
        public List<EmployeeChangeNotice> getEmployeeChangeNotices(
                        @RequestParam String email) {

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email.trim())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Active employee not found"));

                LocalDate today = LocalDate.now(INDIA_ZONE);

                List<EmploymentChangeHistory> dueChanges = employmentHistoryRepository
                                .findByStatusAndEffectiveDateLessThanEqual(
                                                "SCHEDULED", today);

                for (EmploymentChangeHistory history : dueChanges) {
                        if (!history.getEmployeeId().equals(employee.getId())) {
                                continue;
                        }

                        Admin admin = findAdminForHistory(history);

                        applyChange(history, employee, admin);
                }

                return employmentHistoryRepository
                                .findByEmployeeIdAndEmployeeAcknowledgedFalseOrderByCreatedAtAsc(
                                                employee.getId())
                                .stream()
                                .filter(history -> "APPLIED".equalsIgnoreCase(
                                                history.getStatus()))
                                .map(this::toNotice)
                                .toList();
        }

        // =====================================================
        // EMPLOYEE ACKNOWLEDGEMENT
        //
        // POST /admin/employees/changes/{historyId}/acknowledge?email=...
        // =====================================================

        @PostMapping("/changes/{historyId}/acknowledge")
        @Transactional
        public String acknowledgeEmployeeChange(
                        @PathVariable Long historyId,
                        @RequestParam String email) {

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email.trim())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Active employee not found"));

                EmploymentChangeHistory history = employmentHistoryRepository.findById(historyId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Employment change history not found"));

                if (!history.getEmployeeId().equals(employee.getId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "This change does not belong to the logged-in employee");
                }

                if (!"APPLIED".equalsIgnoreCase(history.getStatus())) {
                        throw bad("This employment change has not been applied yet");
                }

                if (!history.isEmployeeAcknowledged()) {
                        history.setEmployeeAcknowledged(true);
                        history.setEmployeeAcknowledgedAt(
                                        OffsetDateTime.now(INDIA_ZONE));

                        employmentHistoryRepository.save(history);

                        // Internship completed (certificate only): the intern is made
                        // INACTIVE only AFTER the intern has seen and confirmed the popup.
                        if ("INTERN_CERTIFICATE".equalsIgnoreCase(history.getActionType())
                                        && Boolean.TRUE.equals(employee.getActive())) {

                                employee.setActive(false);
                                employeeRepository.save(employee);

                                saveHistory(
                                                findAdminForHistory(history),
                                                "INTERNSHIP COMPLETED - CERTIFICATE",
                                                employee,
                                                "Status",
                                                "COMPLETED - INACTIVE AFTER EMPLOYEE OK",
                                                "INACTIVE",
                                                history.getReason());
                        }
                }

                return "Employment change acknowledged successfully";
        }

        private EmployeeChangeNotice toNotice(
                        EmploymentChangeHistory history) {

                return new EmployeeChangeNotice(
                                history.getId(),
                                history.getActionType(),
                                history.getEmployeeCode(),
                                history.getEmployeeName(),
                                history.getOldSalary(),
                                history.getNewSalary(),
                                history.getOldRole(),
                                history.getNewRole(),
                                history.getOldDesignation(),
                                history.getNewDesignation(),
                                history.getOldEmploymentStartDate(),
                                history.getNewEmploymentStartDate(),
                                history.getOldEmploymentEndDate(),
                                history.getNewEmploymentEndDate(),
                                history.getEffectiveDate(),
                                history.getAdminName(),
                                history.getReason(),
                                history.getCreatedAt());
        }

        private Admin findAdminForHistory(
                        EmploymentChangeHistory history) {

                if (history.getAdminEmail() != null
                                && !history.getAdminEmail().trim().isEmpty()) {

                        return adminRepository.findByEmail(
                                        history.getAdminEmail().trim())
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.UNAUTHORIZED,
                                                        "Admin associated with the scheduled change was not found"));
                }

                throw new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Scheduled change is missing its admin email");
        }

        // =====================================================
        // INTERN NOTICE (popup row for Extend / Certificate)
        //
        // Saved as APPLIED + not acknowledged so the intern sees
        // the popup on the Employee Dashboard until OK is clicked.
        // Intern notices are never applied by applyChange.
        // =====================================================

        private EmploymentChangeHistory saveInternNotice(
                        Admin admin,
                        Employee employee,
                        String actionType,
                        String reason,
                        LocalDate periodStart,
                        LocalDate oldEnd,
                        LocalDate newEnd) {

                LocalDate today = LocalDate.now(INDIA_ZONE);

                BigDecimal salary = BigDecimal.valueOf(
                                employee.getSalary() == null ? 0.0 : employee.getSalary());

                EmploymentChangeHistory history = new EmploymentChangeHistory();

                history.setEmployeeId(employee.getId());
                history.setEmployeeCode(employee.getEmployeeCode());
                history.setEmployeeName(employee.getName());

                history.setActionType(actionType);

                history.setOldSalary(salary);
                history.setNewSalary(salary);

                history.setOldRole(employee.getRole());
                history.setNewRole(employee.getRole());

                history.setOldDesignation(employee.getDesignation());
                history.setNewDesignation(employee.getDesignation());

                history.setOldEmploymentStartDate(periodStart);
                history.setNewEmploymentStartDate(periodStart);

                history.setOldEmploymentEndDate(oldEnd);
                history.setNewEmploymentEndDate(newEnd);

                history.setEffectiveDate(today);

                history.setAdminName(admin.getAdminName());
                history.setAdminEmail(admin.getEmail());
                history.setReason(reason);

                history.setStatus("APPLIED");
                history.setEmployeeAcknowledged(false);
                history.setAppliedAt(OffsetDateTime.now(INDIA_ZONE));

                employmentHistoryRepository.save(history);

                return history;
        }

        // =====================================================
        // INTERN: EXTEND
        // =====================================================

        private void extendInternship(
                        Admin admin,
                        Employee employee,
                        ReviewRequest request,
                        String reason) {

                Integer extend = request.extendMonths();

                if (extend == null || extend < 1) {
                        throw bad("Extension months are required");
                }

                LocalDate startDate = employee.getInternshipStartDate();
                Integer oldDuration = employee.getInternshipDurationMonths();

                if (startDate == null || oldDuration == null) {
                        throw bad(
                                        "Internship details are missing for this employee");
                }

                int newDuration = oldDuration + extend;

                if (newDuration > 12) {
                        throw bad("Total internship cannot be more than 12 months");
                }

                LocalDate oldEnd = employee.getInternshipEndDate();

                LocalDate newEnd = startDate
                                .plusMonths(newDuration)
                                .minusDays(1);

                employee.setInternshipDurationMonths(newDuration);
                employee.setInternshipEndDate(newEnd);

                stampLastAction(employee, admin, "INTERN_EXTEND", reason);

                employeeRepository.save(employee);

                addStipendRowsForExtension(
                                employee,
                                oldEnd != null ? oldEnd : startDate,
                                newEnd);

                saveHistory(
                                admin,
                                "INTERNSHIP EXTENDED",
                                employee,
                                "Internship Duration",
                                oldDuration + " months",
                                newDuration + " months",
                                reason);

                EmploymentChangeHistory extendNotice = saveInternNotice(
                                admin,
                                employee,
                                "INTERN_EXTEND",
                                reason,
                                startDate,
                                oldEnd,
                                newEnd);

                sendChangeEmail(
                                employee,
                                extendNotice,
                                employee.getTechnology(),
                                employee.getTechnology(),
                                false);
        }

        private void addStipendRowsForExtension(
                        Employee employee,
                        LocalDate oldEnd,
                        LocalDate newEnd) {

                List<InternshipStipend> rows = stipendRepository
                                .findByEmployeeIdOrderByStipendYearAscStipendMonthAsc(
                                                employee.getId());

                String stipendType = employee.getStipendType();
                double amount = 0.0;

                if (!rows.isEmpty()) {
                        InternshipStipend last = rows.get(rows.size() - 1);

                        Double lastAmount = last.getStipendAmount();
                        amount = lastAmount == null ? 0.0 : lastAmount;
                }

                YearMonth lastMonth = YearMonth.from(newEnd);

                for (YearMonth month = YearMonth.from(oldEnd); !month.isAfter(lastMonth); month = month.plusMonths(1)) {

                        boolean exists = stipendRepository
                                        .findByEmployeeIdAndStipendYearAndStipendMonth(
                                                        employee.getId(),
                                                        month.getYear(),
                                                        month.getMonthValue())
                                        .isPresent();

                        if (exists) {
                                continue;
                        }

                        InternshipStipend stipend = new InternshipStipend();

                        stipend.setEmployee(employee);
                        stipend.setStipendYear(month.getYear());
                        stipend.setStipendMonth(month.getMonthValue());
                        stipend.setStipendType(stipendType);
                        stipend.setStipendAmount(amount);

                        stipendRepository.save(stipend);
                }
        }

        // =====================================================
        // INTERN: CONVERT TO EMPLOYEE
        // =====================================================

        private void convertInternToEmployee(
                        Admin admin,
                        Employee employee,
                        ReviewRequest request,
                        String reason) {

                String newDesignation = request.newDesignation() == null
                                ? ""
                                : request.newDesignation().trim();

                if (newDesignation.isEmpty()) {
                        throw bad("New designation is required");
                }

                if (isInternDesignation(newDesignation)) {
                        throw bad("Select a non-intern designation");
                }

                if (request.newSalary() == null || request.newSalary() <= 0) {
                        throw bad("Salary is required");
                }

                String oldDesignation = employee.getDesignation();

                String oldRole = employee.getRole();
                String oldTechnology = employee.getTechnology();

                double oldSalary = employee.getSalary() == null
                                ? 0.0
                                : employee.getSalary();

                LocalDate today = LocalDate.now(INDIA_ZONE);

                LocalDate oldStart = employee.getEmploymentStartDate();
                LocalDate oldEnd = employee.getEmploymentEndDate();

                // Any role / technology can be given when the intern becomes an employee.
                if (request.newRole() != null
                                && !request.newRole().trim().isEmpty()
                                && !request.newRole().trim().equalsIgnoreCase(
                                                oldRole == null ? "" : oldRole.trim())) {

                        if (request.newTechnology() == null
                                        || request.newTechnology().trim().isEmpty()) {
                                throw bad("Select the technology / tool for the new role");
                        }

                        employee.setRole(request.newRole().trim());
                }

                if (request.newTechnology() != null
                                && !request.newTechnology().trim().isEmpty()) {
                        employee.setTechnology(request.newTechnology().trim());
                }

                employee.setDesignation(newDesignation);
                employee.setSalary(request.newSalary());

                employee.setEmploymentStartDate(today);
                employee.setEmploymentEndDate(
                                today.plusYears(1).minusDays(1));

                stampLastAction(employee, admin, "INTERN_CONVERT", reason);

                employeeRepository.save(employee);

                removeFutureStipendRows(employee, today);

                saveHistory(
                                admin,
                                "INTERN CONVERTED TO EMPLOYEE",
                                employee,
                                "Designation",
                                oldDesignation,
                                newDesignation,
                                reason);

                saveHistory(
                                admin,
                                "SALARY UPDATE",
                                employee,
                                "Salary",
                                String.valueOf(oldSalary),
                                String.valueOf(request.newSalary()),
                                reason);

                if (!java.util.Objects.equals(oldRole, employee.getRole())) {
                        saveHistory(
                                        admin,
                                        "PROMOTION",
                                        employee,
                                        "Role",
                                        oldRole,
                                        employee.getRole(),
                                        reason);
                }

                if (!java.util.Objects.equals(oldTechnology, employee.getTechnology())) {
                        saveHistory(
                                        admin,
                                        "PROMOTION",
                                        employee,
                                        "Technology",
                                        oldTechnology,
                                        employee.getTechnology(),
                                        reason);
                }

                EmploymentChangeHistory history = new EmploymentChangeHistory();

                history.setEmployeeId(employee.getId());
                history.setEmployeeCode(employee.getEmployeeCode());
                history.setEmployeeName(employee.getName());
                history.setActionType("INTERN_CONVERT");
                history.setOldSalary(BigDecimal.valueOf(oldSalary));
                history.setNewSalary(BigDecimal.valueOf(request.newSalary()));
                history.setOldRole(oldRole);
                history.setNewRole(employee.getRole());
                history.setOldDesignation(oldDesignation);
                history.setNewDesignation(newDesignation);
                history.setOldEmploymentStartDate(oldStart);
                history.setNewEmploymentStartDate(today);
                history.setOldEmploymentEndDate(oldEnd);
                history.setNewEmploymentEndDate(
                                today.plusYears(1).minusDays(1));
                history.setEffectiveDate(today);
                history.setAdminName(admin.getAdminName());
                history.setAdminEmail(admin.getEmail());
                history.setReason(reason);
                history.setStatus("APPLIED");
                history.setEmployeeAcknowledged(false);
                history.setAppliedAt(OffsetDateTime.now(INDIA_ZONE));

                employmentHistoryRepository.save(history);

                sendChangeEmail(employee, history, oldTechnology, employee.getTechnology(), false);
        }

        private void removeFutureStipendRows(
                        Employee employee,
                        LocalDate today) {

                YearMonth currentMonth = YearMonth.from(today);

                for (InternshipStipend row : stipendRepository
                                .findByEmployeeIdOrderByStipendYearAscStipendMonthAsc(
                                                employee.getId())) {

                        YearMonth rowMonth = YearMonth.of(
                                        row.getStipendYear(),
                                        row.getStipendMonth());

                        if (rowMonth.isAfter(currentMonth)) {
                                stipendRepository.delete(row);
                        }
                }
        }

        // =====================================================
        // HELPERS
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
                employee.setLastActionAt(OffsetDateTime.now(INDIA_ZONE));
        }

        private void requireIntern(boolean intern) {
                if (!intern) {
                        throw bad("This action is only for interns");
                }
        }

        private boolean isInternDesignation(String designation) {
                if (designation == null) {
                        return false;
                }

                String normalized = designation.trim()
                                .toUpperCase()
                                .replace("-", "_")
                                .replace(" ", "_");

                return normalized.equals("INTERN")
                                || normalized.endsWith("_INTERN");
        }

        private ResponseStatusException bad(String message) {
                return new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                message);
        }

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

        private void validateReason(String reason) {
                if (reason == null || reason.trim().length() < 3) {
                        throw bad(
                                        "Reason is required (minimum 3 characters)");
                }
        }

        private void saveHistory(
                        Admin admin,
                        String action,
                        Employee employee,
                        String fieldName,
                        String oldValue,
                        String newValue,
                        String reason) {

                AdminActionHistory history = new AdminActionHistory();

                history.setAdminName(admin.getAdminName());
                history.setAction(action);
                history.setEmployeeId(employee.getId());
                history.setEmployeeCode(employee.getEmployeeCode());
                history.setEmployeeName(employee.getName());
                history.setReason(reason);

                history.setActionDate(LocalDate.now(INDIA_ZONE));
                history.setActionTime(LocalTime.now(INDIA_ZONE));

                history.setFieldName(fieldName);
                history.setOldValue(oldValue);
                history.setNewValue(newValue);

                historyRepository.save(history);
        }
}