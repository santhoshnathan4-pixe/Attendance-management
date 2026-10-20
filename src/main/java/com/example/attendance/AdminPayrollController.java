package com.example.attendance;

import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Payroll screen support for the Salary page:
 * month list with late / short time, admin decision (hours to cut / full pay),
 * and one-click "Pay" with payslip e-mail.
 *
 * The salary maths itself stays in PayrollService.
 */
@RestController
@RequestMapping("/admin/payroll")
public class AdminPayrollController {

        private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

        private final PayrollService payrollService;
        private final PayrollAdjustmentRepository adjustmentRepository;
        private final AdminRepository adminRepository;
        private final EmployeeRepository employeeRepository;
        private final AdminActionHistoryRepository historyRepository;
        private final PayslipService payslipService;
        private final PayslipPdfService payslipPdfService;
        private final JavaMailSender mailSender;
        private final AttendanceRepository attendanceRepository;
        private final LeaveRequestRepository leaveRequestRepository;
        private final ShiftTimeService shiftTimeService;
        private final PayrollPeriodService periodService;
        private final PayrollPeriodRepository periodRepository;

        public AdminPayrollController(
                        AttendanceRepository attendanceRepository,
                        LeaveRequestRepository leaveRequestRepository,
                        ShiftTimeService shiftTimeService,
                        PayrollService payrollService,
                        PayrollAdjustmentRepository adjustmentRepository,
                        AdminRepository adminRepository,
                        EmployeeRepository employeeRepository,
                        AdminActionHistoryRepository historyRepository,
                        PayslipService payslipService,
                        PayslipPdfService payslipPdfService,
                        JavaMailSender mailSender,
                        PayrollPeriodService periodService,
                        PayrollPeriodRepository periodRepository) {

                this.periodService = periodService;
                this.periodRepository = periodRepository;
                this.attendanceRepository = attendanceRepository;
                this.leaveRequestRepository = leaveRequestRepository;
                this.shiftTimeService = shiftTimeService;
                this.employeeRepository = employeeRepository;
                this.payrollService = payrollService;
                this.adjustmentRepository = adjustmentRepository;
                this.adminRepository = adminRepository;
                this.historyRepository = historyRepository;
                this.payslipService = payslipService;
                this.payslipPdfService = payslipPdfService;
                this.mailSender = mailSender;
        }

        // =========================================
        // RESPONSE / REQUEST MODELS
        // =========================================

        public record PayrollRow(
                        Integer employeeId,
                        String employeeCode,
                        String employeeName,
                        String email,
                        String designation,
                        boolean intern,
                        double monthlySalary,
                        int workingDays,
                        int presentDays,
                        double leaveDays,
                        double lossOfPay,
                        double finalSalary,
                        double overtimeSalary,
                        double perDaySalary,
                        long shortMinutes,
                        long toleratedMinutes,
                        int shortDays,
                        long appliedDeductMinutes,
                        boolean adjusted,
                        boolean payFull,
                        String adjustmentReason,
                        String adjustedBy,
                        boolean paid,
                        String paidAt,
                        boolean needsAttention) {
        }

        public record AdjustRequest(
                        String adminEmail,
                        Integer year,
                        Integer month,
                        Integer deductMinutes,
                        Boolean payFull,
                        String reason,
                        Boolean reset) {
        }

        public record PayRequest(
                        String adminEmail,
                        Integer year,
                        Integer month,
                        List<Integer> employeeIds,
                        Boolean sendEmail) {
        }

        public record PayResult(
                        int paid,
                        int emailed,
                        int skipped,
                        List<String> messages) {
        }

        // =========================================
        // SALARY PERIOD (admin sets start date and end date)
        // =========================================

        public record PeriodResponse(
                        String startDate,
                        String endDate,
                        boolean custom,
                        boolean locked,
                        int days) {
        }

        public record PeriodRequest(
                        String adminEmail,
                        Integer year,
                        Integer month,
                        String startDate,
                        String endDate) {
        }

        @GetMapping("/period")
        public PeriodResponse period(
                        @RequestParam int year,
                        @RequestParam int month) {

                requireYear(year);
                requireMonth(month);

                return buildPeriodResponse(year, month);
        }

        @PutMapping("/period")
        public PeriodResponse savePeriod(@RequestBody PeriodRequest request) {

                Admin admin = findLoggedInAdmin(request.adminEmail());

                int year = requireYear(request.year());
                int month = requireMonth(request.month());

                if (isMonthPaid(year, month)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Salary is already paid for this month. The dates cannot be changed.");
                }

                LocalDate start = parseDate(request.startDate(), "Start date");
                LocalDate end = parseDate(request.endDate(), "End date");

                if (end.isBefore(start)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "End date cannot be before the start date");
                }

                if (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1 > 62) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "A salary period cannot be longer than 62 days");
                }

                // Two saved periods must never share a date (attendance would be paid twice)
                for (PayrollPeriod other : periodRepository.findAll()) {

                        boolean same = other.getPayYear() == year && other.getPayMonth() == month;

                        if (!same
                                        && !end.isBefore(other.getStartDate())
                                        && !start.isAfter(other.getEndDate())) {

                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "These dates overlap the saved period of "
                                                                + Month.of(other.getPayMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                                                + " " + other.getPayYear()
                                                                + " (" + other.getStartDate() + " to " + other.getEndDate() + ")");
                        }
                }

                PayrollPeriod saved = periodRepository
                                .findByPayYearAndPayMonth(year, month)
                                .orElseGet(() -> {
                                        PayrollPeriod created = new PayrollPeriod();
                                        created.setPayYear(year);
                                        created.setPayMonth(month);
                                        return created;
                                });

                saved.setStartDate(start);
                saved.setEndDate(end);
                saved.setUpdatedBy(admin.getAdminName());
                saved.setUpdatedAt(LocalDateTime.now(INDIA_ZONE));

                periodRepository.save(saved);

                return buildPeriodResponse(year, month);
        }

        // Back to the normal calendar month
        @DeleteMapping("/period")
        public PeriodResponse resetPeriod(
                        @RequestParam String adminEmail,
                        @RequestParam int year,
                        @RequestParam int month) {

                findLoggedInAdmin(adminEmail);

                requireYear(year);
                requireMonth(month);

                if (isMonthPaid(year, month)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Salary is already paid for this month. The dates cannot be changed.");
                }

                periodRepository
                                .findByPayYearAndPayMonth(year, month)
                                .ifPresent(item -> periodRepository.delete(item));

                return buildPeriodResponse(year, month);
        }

        private PeriodResponse buildPeriodResponse(int year, int month) {

                PayrollPeriodService.Period period = periodService.resolve(year, month);

                return new PeriodResponse(
                                period.start().toString(),
                                period.end().toString(),
                                period.custom(),
                                isMonthPaid(year, month),
                                (int) (java.time.temporal.ChronoUnit.DAYS.between(period.start(), period.end()) + 1));
        }

        private boolean isMonthPaid(int year, int month) {

                return adjustmentRepository
                                .findByPayYearAndPayMonth(year, month)
                                .stream()
                                .anyMatch(item -> Boolean.TRUE.equals(item.getPaid()));
        }

        private LocalDate parseDate(String value, String name) {

                try {
                        return LocalDate.parse(value == null ? "" : value.trim());
                } catch (Exception exception) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        name + " is required (yyyy-mm-dd)");
                }
        }

        // =========================================
        // MONTH PAYROLL (with late time + admin decision)
        // =========================================

        @GetMapping("/month")
        public List<PayrollRow> month(
                        @RequestParam int year,
                        @RequestParam int month) {

                PayrollService.MonthPayroll payroll = payrollService.calculate(year, month);

                List<PayrollRow> rows = new ArrayList<>();

                for (PayrollService.EmployeePayroll p : payroll.employees()) {

                        Employee employee = p.employee();
                        PayrollMath.Result math = p.math();
                        PayrollService.LateInfo late = p.late();
                        PayrollAdjustment adjustment = p.adjustment();

                        boolean payFull = adjustment != null
                                        && Boolean.TRUE.equals(adjustment.getPayFull());

                        boolean paid = adjustment != null
                                        && Boolean.TRUE.equals(adjustment.getPaid());

                        boolean adjusted = adjustment != null
                                        && (adjustment.getDeductMinutes() != null || payFull);

                        // Needs the admin's attention: crossed the tolerance and no decision taken yet
                        boolean needsAttention = late != null
                                        && late.deductibleMinutes() > 0
                                        && !adjusted
                                        && !paid;

                        rows.add(new PayrollRow(
                                        employee.getId(),
                                        employee.getEmployeeCode(),
                                        employee.getName(),
                                        employee.getEmail(),
                                        employee.getDesignation(),
                                        isInternDesignation(employee.getDesignation()),
                                        employee.getSalary() != null ? employee.getSalary() : 0.0,
                                        math.workingDays(),
                                        math.presentDays(),
                                        PayrollMath.round2(
                                                        math.paidLeaveDays() + math.autoCoveredDays()),
                                        math.lossOfPay(),
                                        math.finalSalary(),
                                        p.overtimeSalary(),
                                        math.perDaySalary(),
                                        late != null ? late.deductibleMinutes() : 0,
                                        late != null ? late.toleratedMinutes() : 0,
                                        late != null ? late.shortDays() : 0,
                                        late != null ? late.appliedDeductMinutes() : 0,
                                        adjusted,
                                        payFull,
                                        adjustment != null ? adjustment.getReason() : null,
                                        adjustment != null ? adjustment.getAdjustedBy() : null,
                                        paid,
                                        adjustment != null && adjustment.getPaidAt() != null
                                                        ? adjustment.getPaidAt().toString()
                                                        : null,
                                        needsAttention));
                }

                return rows;
        }

        // =========================================
        // DAY-WISE DETAILS (why the salary is cut)
        // =========================================

        public record DayRow(
                        String date,
                        String sessions,
                        long workedMinutes,
                        long requiredMinutes,
                        long shortMinutes,
                        String note) {
        }

        @GetMapping("/{employeeId}/days")
        public List<DayRow> days(
                        @PathVariable Integer employeeId,
                        @RequestParam int year,
                        @RequestParam int month) {

                Employee employee = findEmployee(employeeId);

                // Salary dates set by the admin (default: whole calendar month)
                PayrollPeriodService.Period period = periodService.resolve(requireYear(year), requireMonth(month));

                LocalDate start = period.start();
                LocalDate end = period.end();

                ShiftTimeService.ShiftTimes shift = shiftTimeService.getShiftTimes(employee);

                long shiftMinutes = shift == null ? 0 : shift.totalMinutes();

                Map<LocalDate, List<Attendance>> byDate = new TreeMap<>();

                for (Attendance attendance : attendanceRepository.findByAttendanceDateBetween(start, end)) {

                        if (!employeeId.equals(attendance.getEmployeeId())
                                        || attendance.getAttendanceDate() == null
                                        || attendance.getCheckIn() == null) {
                                continue;
                        }

                        byDate.computeIfAbsent(attendance.getAttendanceDate(), key -> new ArrayList<>())
                                        .add(attendance);
                }

                Map<LocalDate, String> leaveNote = new HashMap<>();
                Map<LocalDate, Long> permissionMinutes = new HashMap<>();

                for (LeaveRequest leave : leaveRequestRepository.findByLeaveDateBetween(start, end)) {

                        if (!employeeId.equals(leave.getEmployeeId())
                                        || leave.getLeaveDate() == null
                                        || !"APPROVED".equalsIgnoreCase(leave.getStatus())
                                        || leave.getLeaveType() == null) {
                                continue;
                        }

                        String type = leave.getLeaveType().trim().toUpperCase();

                        if (type.equals("PERMISSION")) {

                                if (leave.getPermissionStart() != null
                                                && leave.getPermissionEnd() != null
                                                && leave.getPermissionEnd().isAfter(leave.getPermissionStart())) {

                                        permissionMinutes.merge(
                                                        leave.getLeaveDate(),
                                                        Duration.between(
                                                                        leave.getPermissionStart(),
                                                                        leave.getPermissionEnd()).toMinutes(),
                                                        Long::sum);
                                }

                        } else {

                                leaveNote.put(leave.getLeaveDate(), type + " leave");
                        }
                }

                LocalDate today = LocalDate.now(INDIA_ZONE);

                List<DayRow> rows = new ArrayList<>();

                for (Map.Entry<LocalDate, List<Attendance>> entry : byDate.entrySet()) {

                        LocalDate date = entry.getKey();

                        long worked = 0;
                        boolean noCheckOut = false;
                        StringBuilder sessions = new StringBuilder();

                        for (Attendance session : entry.getValue()) {

                                if (sessions.length() > 0) {
                                        sessions.append(" | ");
                                }

                                sessions.append(session.getCheckIn().withNano(0))
                                                .append(" - ")
                                                .append(session.getCheckOut() == null
                                                                ? "no check-out"
                                                                : session.getCheckOut().withNano(0));

                                if (session.getCheckOut() == null) {
                                        noCheckOut = true;
                                } else if (session.getCheckOut().isAfter(session.getCheckIn())) {
                                        worked += Duration.between(
                                                        session.getCheckIn(),
                                                        session.getCheckOut()).toMinutes();
                                }
                        }

                        long required = Math.max(0, shiftMinutes - permissionMinutes.getOrDefault(date, 0L));

                        long shortMinutes = Math.max(0, required - worked);

                        String note;

                        if (!date.isBefore(today)) {
                                note = "Today / future - not counted yet";
                                shortMinutes = 0;
                        } else if (leaveNote.containsKey(date)) {
                                note = leaveNote.get(date) + " - not cut";
                                shortMinutes = 0;
                        } else if (shortMinutes <= 0) {
                                note = "Full shift";
                        } else if (shortMinutes <= 30) {
                                note = "Within 30 min rule - not cut";
                        } else {
                                note = noCheckOut
                                                ? "Check-out missing - cut until the admin approves the check-out"
                                                : "Worked less than the shift - short time is cut";
                        }

                        rows.add(new DayRow(
                                        date.toString(),
                                        sessions.toString(),
                                        worked,
                                        required,
                                        shortMinutes,
                                        note));
                }

                return rows;
        }

        // =========================================
        // ADMIN DECISION FOR ONE EMPLOYEE / MONTH
        //
        // reset = true -> back to the automatic rule
        // deductMinutes = 0 -> pay all present days (late time waived)
        // deductMinutes = N -> cut only N minutes
        // payFull = true -> full salary / stipend
        // =========================================

        @PutMapping("/adjust/{employeeId}")
        public String adjust(
                        @PathVariable Integer employeeId,
                        @RequestBody AdjustRequest request) {

                Admin admin = findLoggedInAdmin(request.adminEmail());

                int year = requireYear(request.year());
                int month = requireMonth(request.month());

                Employee employee = findEmployee(employeeId);

                PayrollAdjustment adjustment = adjustmentRepository
                                .findByEmployeeIdAndPayYearAndPayMonth(employeeId, year, month)
                                .orElseGet(() -> {
                                        PayrollAdjustment created = new PayrollAdjustment();
                                        created.setEmployeeId(employeeId);
                                        created.setPayYear(year);
                                        created.setPayMonth(month);
                                        return created;
                                });

                if (Boolean.TRUE.equals(adjustment.getPaid())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "This month is already marked as paid");
                }

                String before = describe(adjustment);

                if (Boolean.TRUE.equals(request.reset())) {

                        adjustment.setDeductMinutes(null);
                        adjustment.setPayFull(false);
                        adjustment.setReason(null);

                } else {

                        if (request.reason() == null || request.reason().trim().length() < 3) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Please enter a reason (minimum 3 characters)");
                        }

                        if (request.deductMinutes() != null && request.deductMinutes() < 0) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Minutes cannot be negative");
                        }

                        adjustment.setDeductMinutes(request.deductMinutes());
                        adjustment.setPayFull(Boolean.TRUE.equals(request.payFull()));
                        adjustment.setReason(request.reason().trim());
                }

                adjustment.setAdjustedBy(admin.getAdminName());
                adjustment.setAdjustedAt(LocalDateTime.now(INDIA_ZONE));

                adjustmentRepository.save(adjustment);

                saveHistory(
                                admin,
                                "PAYROLL ADJUSTMENT",
                                employee,
                                "Payroll " + year + "-" + month,
                                before,
                                describe(adjustment),
                                Boolean.TRUE.equals(request.reset())
                                                ? "Reset to automatic rule"
                                                : request.reason().trim());

                return "Payroll decision saved";
        }

        // =========================================
        // PAY (ONE CLICK FOR ALL, OR SELECTED EMPLOYEES)
        // Marks the month as paid and e-mails the payslip.
        // Interns are marked paid, their stipend statement is sent from the Salary page.
        // =========================================

        @PostMapping("/pay")
        public PayResult pay(@RequestBody PayRequest request) {

                Admin admin = findLoggedInAdmin(request.adminEmail());

                int year = requireYear(request.year());
                int month = requireMonth(request.month());

                boolean sendEmail = !Boolean.FALSE.equals(request.sendEmail());

                PayrollService.MonthPayroll payroll = payrollService.calculate(year, month);

                int paid = 0;
                int emailed = 0;
                int skipped = 0;

                List<String> messages = new ArrayList<>();

                for (PayrollService.EmployeePayroll p : payroll.employees()) {

                        Employee employee = p.employee();

                        if (request.employeeIds() != null
                                        && !request.employeeIds().contains(employee.getId())) {
                                continue;
                        }

                        PayrollAdjustment adjustment = adjustmentRepository
                                        .findByEmployeeIdAndPayYearAndPayMonth(employee.getId(), year, month)
                                        .orElseGet(() -> {
                                                PayrollAdjustment created = new PayrollAdjustment();
                                                created.setEmployeeId(employee.getId());
                                                created.setPayYear(year);
                                                created.setPayMonth(month);
                                                return created;
                                        });

                        if (Boolean.TRUE.equals(adjustment.getPaid())) {
                                skipped++;
                                continue;
                        }

                        boolean intern = isInternDesignation(employee.getDesignation());

                        double salary = employee.getSalary() != null ? employee.getSalary() : 0.0;

                        if (!intern && salary <= 0) {
                                skipped++;
                                messages.add(employee.getName() + ": salary is not set, skipped");
                                continue;
                        }

                        adjustment.setPaid(true);
                        adjustment.setPaidAt(LocalDateTime.now(INDIA_ZONE));
                        adjustment.setPaidBy(admin.getAdminName());

                        adjustmentRepository.save(adjustment);

                        saveHistory(
                                        admin,
                                        "SALARY PAID",
                                        employee,
                                        "Payroll " + year + "-" + month,
                                        "",
                                        "PAID",
                                        "Salary marked as paid");

                        paid++;

                        if (sendEmail && !intern) {

                                String result = sendPayslipMail(employee, year, month);

                                if (result == null) {
                                        emailed++;
                                } else {
                                        messages.add(employee.getName() + ": " + result);
                                }
                        }
                }

                return new PayResult(paid, emailed, skipped, messages);
        }

        // =========================================
        // PAYSLIP E-MAIL (returns null when sent, otherwise the reason)
        // =========================================

        private String sendPayslipMail(Employee employee, int year, int month) {

                try {

                        PayslipResponse payslip = payslipService.generatePayslip(employee.getId(), year, month);

                        String email = payslip.getEmail();

                        if (email == null || email.isBlank()) {
                                email = employee.getEmail();
                        }

                        if (email == null || email.isBlank()) {
                                return "email is missing";
                        }

                        byte[] pdf = payslipPdfService.generatePayslipPdf(employee.getId(), year, month);

                        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                        helper.setTo(email.trim());
                        helper.setSubject("Payslip - " + monthName + " " + year);
                        helper.setText("Dear " + payslip.getEmployeeName() + ",\n\n"
                                        + "Your salary for " + monthName + " " + year
                                        + " has been processed. Please find your payslip attached.\n\nRegards,\nGROWBYTEE");
                        helper.addAttachment(
                                        "Payslip_" + employee.getId() + "_" + year + "_" + month + ".pdf",
                                        () -> new ByteArrayInputStream(pdf));

                        mailSender.send(message);

                        return null;

                } catch (Exception exception) {

                        return "payslip e-mail failed";
                }
        }

        // =========================================
        // HELPERS
        // =========================================

        private boolean isInternDesignation(String designation) {

                if (designation == null) {
                        return false;
                }

                String value = designation.trim().toLowerCase();

                return value.equals("intern") || value.endsWith(" intern");
        }

        private String describe(PayrollAdjustment adjustment) {

                if (adjustment == null || adjustment.getId() == null) {
                        return "AUTO";
                }

                if (Boolean.TRUE.equals(adjustment.getPayFull())) {
                        return "FULL PAY";
                }

                if (adjustment.getDeductMinutes() != null) {
                        return "CUT " + adjustment.getDeductMinutes() + " MIN";
                }

                return "AUTO";
        }

        private int requireYear(Integer year) {

                if (year == null || year < 2000 || year > 2100) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid year is required");
                }

                return year;
        }

        private int requireMonth(Integer month) {

                if (month == null || month < 1 || month > 12) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid month is required");
                }

                return month;
        }

        private Employee findEmployee(Integer employeeId) {

                Employee employee = employeeLookup(employeeId);

                if ("ADMIN".equalsIgnoreCase(employee.getRole())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Payroll is not available for admin");
                }

                return employee;
        }

        private Employee employeeLookup(Integer employeeId) {

                return employeeRepository.findById(employeeId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Employee not found"));
        }

        private Admin findLoggedInAdmin(String email) {

                if (email == null || email.trim().isEmpty()) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Admin login required");
                }

                return adminRepository
                                .findByEmail(email.trim())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Logged-in admin not found"));
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