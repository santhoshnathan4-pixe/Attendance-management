package com.example.attendance;

import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Admin dashboard "Send Warning / Report" button.
 * Emails the employee and records the action in Admin History.
 */
@RestController
@RequestMapping("/admin/missing-checkout")
public class AdminMissingCheckoutWarningController {

    private final AdminRepository adminRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AdminActionHistoryRepository historyRepository;
    private final JavaMailSender mailSender;

    public AdminMissingCheckoutWarningController(
            AdminRepository adminRepository,
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            AdminActionHistoryRepository historyRepository,
            JavaMailSender mailSender) {
        this.adminRepository = adminRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.historyRepository = historyRepository;
        this.mailSender = mailSender;
    }

    public record WarningBody(
            Integer employeeId,
            Integer attendanceId,
            String attendanceDate,
            String message,
            String adminEmail) {
    }

    @PostMapping("/warnings")
    public ResponseEntity<String> sendWarning(@RequestBody WarningBody body) {

        if (body == null || body.adminEmail() == null || body.adminEmail().isBlank()) {
            return bad("Admin login information is missing.");
        }
        Admin admin = adminRepository.findByEmail(body.adminEmail().trim()).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin not found.");
        }
        if (body.message() == null || body.message().isBlank()) {
            return bad("Please enter a warning message.");
        }
        if (body.employeeId() == null || body.attendanceId() == null) {
            return bad("Employee and attendance record are required.");
        }

        Employee employee = employeeRepository.findById(body.employeeId()).orElse(null);
        if (employee == null) {
            return bad("Employee not found.");
        }
        Attendance attendance = attendanceRepository.findById(body.attendanceId()).orElse(null);
        if (attendance == null || !body.employeeId().equals(attendance.getEmployeeId())) {
            return bad("Attendance record does not belong to this employee.");
        }
        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            return bad("Employee registered email is missing.");
        }

        String text = body.message().trim();

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setTo(employee.getEmail().trim());
            helper.setSubject("Attendance notice - " + attendance.getAttendanceDate());
            helper.setText("Dear " + employee.getName() + ",\n\n" + text
                    + "\n\nRegards,\n" + admin.getAdminName() + "\nGROWBYTEE");
            mailSender.send(mime);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Warning could not be emailed: " + e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        AdminActionHistory history = new AdminActionHistory();
        history.setAdminName(admin.getAdminName());
        history.setAction("MISSING_CHECKOUT_WARNING");
        history.setEmployeeId(employee.getId());
        history.setEmployeeCode(employee.getEmployeeCode());
        history.setEmployeeName(employee.getName());
        history.setActionDate(now.toLocalDate());
        history.setActionTime(now.toLocalTime());
        history.setFieldName("Warning");
        history.setOldValue("-");
        history.setNewValue(text);
        history.setReason("Attendance date " + attendance.getAttendanceDate());
        historyRepository.save(history);

        return ResponseEntity.ok("Warning sent.");
    }

    private ResponseEntity<String> bad(String m) {
        return ResponseEntity.badRequest().body(m);
    }
}
