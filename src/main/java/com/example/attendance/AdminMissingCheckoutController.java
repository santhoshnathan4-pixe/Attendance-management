
package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {
                "https://attendance-management-3d-webinar.vercel.app",
                "https://attendance-management-git-main-3d-webinar.vercel.app",
                "https://attendance-management-lhsosyu8t-3d-webinar.vercel.app",
                "https://attendance-management-nine-beige.vercel.app"
})
@RequestMapping("/admin/missing-checkout")
public class AdminMissingCheckoutController {

        private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

        private static final LocalTime DEFAULT_OFFICE_START_TIME = LocalTime.of(9, 0);

        private static final LocalTime DEFAULT_OFFICE_END_TIME = LocalTime.of(18, 0);

        private static final int GRACE_MINUTES = 5;

        private final AttendanceRepository attendanceRepository;
        private final EmployeeRepository employeeRepository;
        private final AdminRepository adminRepository;
        private final AdminActionHistoryRepository historyRepository;
        private final LeavePermissionSettingRepository settingRepository;
        private final ShiftSettingRepository shiftSettingRepository;

        public AdminMissingCheckoutController(
                        AttendanceRepository attendanceRepository,
                        EmployeeRepository employeeRepository,
                        AdminRepository adminRepository,
                        AdminActionHistoryRepository historyRepository,
                        LeavePermissionSettingRepository settingRepository,
                        ShiftSettingRepository shiftSettingRepository) {

                this.attendanceRepository = attendanceRepository;
                this.employeeRepository = employeeRepository;
                this.adminRepository = adminRepository;
                this.historyRepository = historyRepository;
                this.settingRepository = settingRepository;
                this.shiftSettingRepository = shiftSettingRepository;
        }

        public record ReviewBody(String adminEmail, String remark) {
        }

        public record AttendanceCorrectionBody(
                        String adminEmail,
                        LocalTime checkIn,
                        LocalTime checkOut,
                        String remark) {
        }

        // =====================================================
        // ADMIN VALIDATION
        // =====================================================

        private Admin findAdmin(String adminEmail) {
                if (adminEmail == null || adminEmail.isBlank()) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Only an admin can review this request.");
                }

                return adminRepository.findByEmail(adminEmail.trim())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "Only an admin can review this request."));
        }

        private boolean isAdmin(String adminEmail) {
                return adminEmail != null
                                && !adminEmail.isBlank()
                                && adminRepository.findByEmail(adminEmail.trim()).isPresent();
        }

        // =====================================================
        // LIST MISSING CHECK-OUT REQUESTS
        // =====================================================

        @GetMapping("/requests")
        public ResponseEntity<?> getRequests(
                        @RequestParam(defaultValue = "PENDING") String status) {

                String requestStatus = status.trim().toUpperCase();

                if (!List.of("PENDING", "APPROVED", "REJECTED")
                                .contains(requestStatus)) {
                        return error(
                                        HttpStatus.BAD_REQUEST,
                                        "Status must be PENDING, APPROVED or REJECTED.");
                }

                List<Attendance> requests = attendanceRepository
                                .findByMissingCheckoutStatusOrderByMissingCheckoutSubmittedAtDesc(
                                                requestStatus);

                List<Integer> employeeIds = requests.stream()
                                .map(Attendance::getEmployeeId)
                                .filter(id -> id != null)
                                .distinct()
                                .toList();

                Map<Integer, Employee> employeeMap = new HashMap<>();

                for (Employee employee : employeeRepository.findAllById(employeeIds)) {
                        employeeMap.put(employee.getId(), employee);
                }

                List<Map<String, Object>> response = new ArrayList<>();

                for (Attendance attendance : requests) {
                        Employee employee = employeeMap.get(attendance.getEmployeeId());

                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("attendanceId", attendance.getId());
                        data.put("employeeId", attendance.getEmployeeId());
                        data.put("employeeName",
                                        employee != null ? employee.getName() : "-");
                        data.put("employeeCode",
                                        employee != null ? employee.getEmployeeCode() : "-");
                        data.put("attendanceDate", attendance.getAttendanceDate());
                        data.put("checkIn", attendance.getCheckIn());
                        data.put("currentCheckOut", attendance.getCheckOut());
                        data.put("requestedCheckOut",
                                        attendance.getMissingCheckoutRequestedTime());
                        data.put("reason", attendance.getMissingCheckoutReason());
                        data.put("submittedAt", attendance.getMissingCheckoutSubmittedAt());
                        data.put("requestStatus", attendance.getMissingCheckoutStatus());
                        data.put("reviewedBy", attendance.getMissingCheckoutReviewedBy());
                        data.put("reviewedAt", attendance.getMissingCheckoutReviewedAt());
                        data.put("adminRemark", attendance.getMissingCheckoutAdminRemark());

                        response.add(data);
                }

                return ResponseEntity.ok(response);
        }

        // =====================================================
        // APPROVE MISSING CHECK-OUT
        // =====================================================

        @PostMapping("/{attendanceId}/approve")
        @Transactional
        public ResponseEntity<String> approve(
                        @PathVariable Integer attendanceId,
                        @RequestBody ReviewBody body) {

                if (body == null || !isAdmin(body.adminEmail())) {
                        return error(HttpStatus.FORBIDDEN,
                                        "Only an admin can review this request.");
                }

                Admin admin = findAdmin(body.adminEmail());

                Attendance attendance = attendanceRepository
                                .findByIdForUpdate(attendanceId)
                                .orElse(null);

                if (attendance == null) {
                        return error(HttpStatus.NOT_FOUND,
                                        "Attendance record not found.");
                }

                if (!"PENDING".equals(attendance.getMissingCheckoutStatus())) {
                        return error(HttpStatus.CONFLICT,
                                        "This request is not pending. It may already be reviewed.");
                }

                if (attendance.getCheckOut() != null) {
                        return error(HttpStatus.CONFLICT,
                                        "This attendance session already has a Check-Out time.");
                }

                LocalTime requestedCheckOut = attendance.getMissingCheckoutRequestedTime();

                if (requestedCheckOut == null) {
                        return error(HttpStatus.BAD_REQUEST,
                                        "The request has no Check-Out time.");
                }

                LocalTime checkIn = attendance.getCheckIn();

                if (checkIn != null && !requestedCheckOut.isAfter(checkIn)) {
                        return error(HttpStatus.BAD_REQUEST,
                                        "Check-Out must be later than Check-In.");
                }

                LocalTime oldCheckOut = attendance.getCheckOut();
                LocalDateTime reviewedAt = LocalDateTime.now(INDIA_ZONE);
                String remark = clean(body.remark());

                attendance.setCheckOut(requestedCheckOut);
                attendance.setMissingCheckoutStatus("APPROVED");
                attendance.setMissingCheckoutReviewedBy(body.adminEmail().trim());
                attendance.setMissingCheckoutReviewedAt(reviewedAt);
                attendance.setMissingCheckoutAdminRemark(remark);
                attendance.setMissingCheckoutEmployeeAcknowledged(false);

                Employee employee = findEmployee(attendance);
                if (employee != null) {
                        attendance.setStatus(calculateCorrectedStatus(
                                        employee, attendance, attendance.getStatus()));
                }

                attendanceRepository.save(attendance);

                String reason = combineReasons(
                                "Employee request: " + safeText(
                                                attendance.getMissingCheckoutReason()),
                                "Admin remark: " + safeText(remark));

                saveHistory(
                                admin, "MISSING CHECKOUT APPROVED", employee, attendance,
                                "Check-Out", valueOf(oldCheckOut),
                                valueOf(requestedCheckOut), reason, reviewedAt);

                return ResponseEntity.ok(
                                "Request approved. Check-Out time updated and history saved.");
        }

        // =====================================================
        // REJECT MISSING CHECK-OUT
        // =====================================================

        @PostMapping("/{attendanceId}/reject")
        @Transactional
        public ResponseEntity<String> reject(
                        @PathVariable Integer attendanceId,
                        @RequestBody ReviewBody body) {

                if (body == null || !isAdmin(body.adminEmail())) {
                        return error(HttpStatus.FORBIDDEN,
                                        "Only an admin can review this request.");
                }

                Admin admin = findAdmin(body.adminEmail());
                String remark = clean(body.remark());

                if (remark == null) {
                        return error(HttpStatus.BAD_REQUEST,
                                        "Please enter a reason for rejecting this request.");
                }

                Attendance attendance = attendanceRepository
                                .findByIdForUpdate(attendanceId)
                                .orElse(null);

                if (attendance == null) {
                        return error(HttpStatus.NOT_FOUND,
                                        "Attendance record not found.");
                }

                if (!"PENDING".equals(attendance.getMissingCheckoutStatus())) {
                        return error(HttpStatus.CONFLICT,
                                        "This request is not pending. It may already be reviewed.");
                }

                LocalDateTime reviewedAt = LocalDateTime.now(INDIA_ZONE);

                attendance.setMissingCheckoutStatus("REJECTED");
                attendance.setMissingCheckoutReviewedBy(body.adminEmail().trim());
                attendance.setMissingCheckoutReviewedAt(reviewedAt);
                attendance.setMissingCheckoutAdminRemark(remark);

                attendanceRepository.save(attendance);

                Employee employee = findEmployee(attendance);

                saveHistory(
                                admin, "MISSING CHECKOUT REJECTED", employee, attendance,
                                "Request Status", "PENDING", "REJECTED",
                                combineReasons(
                                                "Employee request: " + safeText(
                                                                attendance.getMissingCheckoutReason()),
                                                "Admin remark: " + remark),
                                reviewedAt);

                return ResponseEntity.ok(
                                "Request rejected. Attendance times unchanged and history saved.");
        }

        // =====================================================
        // CORRECT CHECK-IN / CHECK-OUT
        // PUT /admin/missing-checkout/{attendanceId}/correct
        // =====================================================

        @PutMapping("/{attendanceId}/correct")
        @Transactional
        public ResponseEntity<String> correctAttendance(
                        @PathVariable Integer attendanceId,
                        @RequestBody AttendanceCorrectionBody body) {

                if (body == null || !isAdmin(body.adminEmail())) {
                        return error(HttpStatus.FORBIDDEN,
                                        "Only an admin can correct attendance.");
                }

                Admin admin = findAdmin(body.adminEmail());
                String remark = clean(body.remark());

                if (remark == null || remark.length() < 3) {
                        return error(HttpStatus.BAD_REQUEST,
                                        "Please enter a valid correction reason.");
                }

                if (body.checkIn() == null || body.checkOut() == null) {
                        return error(HttpStatus.BAD_REQUEST,
                                        "Both Check-In and Check-Out times are required.");
                }

                if (!body.checkOut().isAfter(body.checkIn())) {
                        return error(HttpStatus.BAD_REQUEST,
                                        "Check-Out must be later than Check-In.");
                }

                Attendance attendance = attendanceRepository
                                .findByIdForUpdate(attendanceId)
                                .orElse(null);

                if (attendance == null) {
                        return error(HttpStatus.NOT_FOUND,
                                        "Attendance record not found.");
                }

                if (!"PENDING".equals(attendance.getMissingCheckoutStatus())) {
                        return error(HttpStatus.CONFLICT,
                                        "Only a pending Missing Check-Out request can be corrected here.");
                }

                LocalTime oldCheckIn = attendance.getCheckIn();
                LocalTime oldCheckOut = attendance.getCheckOut();
                LocalDateTime reviewedAt = LocalDateTime.now(INDIA_ZONE);

                Employee employee = findEmployee(attendance);
                if (employee == null) {
                        return error(HttpStatus.NOT_FOUND,
                                        "Employee record not found for this attendance.");
                }

                attendance.setCheckIn(body.checkIn());
                attendance.setCheckOut(body.checkOut());
                attendance.setMissingCheckoutStatus("APPROVED");
                attendance.setMissingCheckoutReviewedBy(body.adminEmail().trim());
                attendance.setMissingCheckoutReviewedAt(reviewedAt);
                attendance.setMissingCheckoutAdminRemark(remark);
                attendance.setMissingCheckoutEmployeeAcknowledged(false);

                // Recalculate status from corrected times and the employee's shift.
                attendance.setStatus(calculateCorrectedStatus(
                                employee, attendance, attendance.getStatus()));

                attendanceRepository.save(attendance);

                if (!sameTime(oldCheckIn, body.checkIn())) {
                        saveHistory(
                                        admin, "ATTENDANCE CORRECTED", employee, attendance,
                                        "Check-In", valueOf(oldCheckIn), valueOf(body.checkIn()),
                                        remark, reviewedAt);
                }

                if (!sameTime(oldCheckOut, body.checkOut())) {
                        saveHistory(
                                        admin, "ATTENDANCE CORRECTED", employee, attendance,
                                        "Check-Out", valueOf(oldCheckOut), valueOf(body.checkOut()),
                                        combineReasons(
                                                        "Employee request: " + safeText(
                                                                        attendance.getMissingCheckoutReason()),
                                                        "Admin correction: " + remark),
                                        reviewedAt);
                }

                saveHistory(
                                admin, "MISSING CHECKOUT CORRECTED", employee, attendance,
                                "Request Status", "PENDING", "APPROVED",
                                combineReasons(
                                                "Employee request: " + safeText(
                                                                attendance.getMissingCheckoutReason()),
                                                "Admin correction: " + remark),
                                reviewedAt);

                return ResponseEntity.ok(
                                "Attendance corrected successfully. Check-In, Check-Out, status and history updated.");
        }

        // =====================================================
        // STATUS CALCULATION - MATCHES ATTENDANCE CONTROLLER
        // =====================================================

        private String calculateCorrectedStatus(
                        Employee employee,
                        Attendance attendance,
                        String previousStatus) {

                LocalTime checkIn = attendance.getCheckIn();
                LocalTime checkOut = attendance.getCheckOut();

                if (checkIn == null) {
                        return previousStatus;
                }

                ShiftSetting shift = getEffectiveShiftSetting(employee);

                LocalTime officeStart = shift != null && shift.getStartTime() != null
                                ? shift.getStartTime()
                                : DEFAULT_OFFICE_START_TIME;

                LocalTime officeEnd = shift != null && shift.getEndTime() != null
                                ? shift.getEndTime()
                                : DEFAULT_OFFICE_END_TIME;

                boolean reEntry = previousStatus != null
                                && previousStatus.toUpperCase().startsWith("RE-ENTRY");

                String status;

                if (reEntry) {
                        status = "RE-ENTRY";
                } else if (checkIn.isAfter(officeStart.plusMinutes(GRACE_MINUTES))) {
                        long lateMinutes = Duration.between(officeStart, checkIn).toMinutes();
                        status = "LATE - " + formatDuration(lateMinutes);
                } else {
                        status = "PRESENT";
                }

                if (checkOut != null && checkOut.isBefore(officeEnd)) {
                        long earlyMinutes = Duration.between(checkOut, officeEnd).toMinutes();
                        String early = "EARLY CHECK-OUT - " + formatDuration(earlyMinutes);

                        if (status.startsWith("LATE - ")) {
                                status += " / " + early;
                        } else if ("RE-ENTRY".equals(status)) {
                                status += " / " + early;
                        } else {
                                status = early;
                        }
                }

                return status;
        }

        private ShiftSetting getEffectiveShiftSetting(Employee employee) {
                String shiftType = "GENERAL";

                if (employee.getId() != null) {
                        var employeeSetting = settingRepository
                                        .findBySettingTypeAndEmployeeId(
                                                        "EMPLOYEE", employee.getId());

                        if (employeeSetting.isPresent()) {
                                String configured = employeeSetting.get().getShiftType();
                                if (configured != null && !configured.isBlank()) {
                                        shiftType = normalizeShiftType(configured);
                                }
                        }
                }

                if ("GENERAL".equals(shiftType)
                                && employee.getRole() != null
                                && !employee.getRole().isBlank()) {

                        var roleSetting = settingRepository
                                        .findBySettingTypeAndRole(
                                                        "ROLE",
                                                        employee.getRole().trim().toUpperCase());

                        if (roleSetting.isPresent()) {
                                String configured = roleSetting.get().getShiftType();
                                if (configured != null && !configured.isBlank()) {
                                        shiftType = normalizeShiftType(configured);
                                }
                        }
                }

                if ("GENERAL".equals(shiftType)) {
                        var defaultSetting = settingRepository.findBySettingType("DEFAULT");

                        if (defaultSetting.isPresent()) {
                                String configured = defaultSetting.get().getShiftType();
                                if (configured != null && !configured.isBlank()) {
                                        shiftType = normalizeShiftType(configured);
                                }
                        }
                }

                return shiftSettingRepository.findByShiftType(shiftType)
                                .orElseGet(() -> shiftSettingRepository
                                                .findByShiftType("GENERAL")
                                                .orElse(null));
        }

        private String normalizeShiftType(String shiftType) {
                if (shiftType == null || shiftType.isBlank()) {
                        return "GENERAL";
                }

                return shiftType.trim().toUpperCase()
                                .replace("-", "_").replace(" ", "_");
        }

        private String formatDuration(long totalMinutes) {
                if (totalMinutes <= 0) {
                        return "0 mins";
                }

                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;

                if (hours > 0 && minutes > 0) {
                        return hours + (hours == 1 ? " hr " : " hrs ")
                                        + minutes + " mins";
                }

                if (hours > 0) {
                        return hours + (hours == 1 ? " hr" : " hrs");
                }

                return minutes + " mins";
        }

        // =====================================================
        // EMPLOYEE - APPROVED / CORRECTED ATTENDANCE NOTIFICATIONS
        // =====================================================

        @GetMapping("/employee-notifications")
        public ResponseEntity<?> getEmployeeNotifications(
                        @RequestParam String email) {

                if (email == null || email.isBlank()) {
                        return error(HttpStatus.BAD_REQUEST,
                                        "Employee email is required.");
                }

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email.trim())
                                .orElse(null);

                if (employee == null) {
                        return error(HttpStatus.FORBIDDEN,
                                        "Active employee not found.");
                }

                List<Attendance> records = attendanceRepository
                                .findByEmployeeIdAndMissingCheckoutStatusAndMissingCheckoutEmployeeAcknowledgedFalseOrderByMissingCheckoutReviewedAtDesc(
                                                employee.getId(), "APPROVED");

                List<Map<String, Object>> response = new ArrayList<>();

                for (Attendance attendance : records) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("attendanceId", attendance.getId());
                        item.put("attendanceDate", attendance.getAttendanceDate());
                        item.put("checkIn", attendance.getCheckIn());
                        item.put("checkOut", attendance.getCheckOut());
                        item.put("adminRemark",
                                        attendance.getMissingCheckoutAdminRemark());
                        item.put("reviewedAt",
                                        attendance.getMissingCheckoutReviewedAt());

                        response.add(item);
                }

                return ResponseEntity.ok(response);
        }

        // =====================================================
        // EMPLOYEE - ACKNOWLEDGE NOTIFICATION
        // =====================================================

        @PostMapping("/{attendanceId}/acknowledge")
        @Transactional
        public ResponseEntity<String> acknowledgeEmployeeNotification(
                        @PathVariable Integer attendanceId,
                        @RequestParam String email) {

                if (email == null || email.isBlank()) {
                        return error(HttpStatus.BAD_REQUEST,
                                        "Employee email is required.");
                }

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email.trim())
                                .orElse(null);

                if (employee == null) {
                        return error(HttpStatus.FORBIDDEN,
                                        "Active employee not found.");
                }

                Attendance attendance = attendanceRepository
                                .findByIdAndEmployeeId(attendanceId, employee.getId())
                                .orElse(null);

                if (attendance == null) {
                        return error(HttpStatus.NOT_FOUND,
                                        "Attendance record not found for this employee.");
                }

                if (!"APPROVED".equals(attendance.getMissingCheckoutStatus())) {
                        return error(HttpStatus.CONFLICT,
                                        "This Missing Check-Out request is not approved.");
                }

                if (!Boolean.TRUE.equals(
                                attendance.getMissingCheckoutEmployeeAcknowledged())) {
                        attendance.setMissingCheckoutEmployeeAcknowledged(true);
                        attendanceRepository.save(attendance);
                }

                return ResponseEntity.ok(
                                "Approval notification acknowledged successfully.");
        }

        // =====================================================
        // ADMIN ACTION HISTORY
        // =====================================================

        private void saveHistory(
                        Admin admin,
                        String action,
                        Employee employee,
                        Attendance attendance,
                        String fieldName,
                        String oldValue,
                        String newValue,
                        String reason,
                        LocalDateTime actionDateTime) {

                AdminActionHistory history = new AdminActionHistory();
                history.setAdminName(admin.getAdminName());
                history.setAction(action);
                history.setEmployeeId(attendance.getEmployeeId());

                if (employee != null) {
                        history.setEmployeeCode(employee.getEmployeeCode());
                        history.setEmployeeName(employee.getName());
                } else {
                        history.setEmployeeCode("-");
                        history.setEmployeeName("-");
                }

                history.setActionDate(actionDateTime.toLocalDate());
                history.setActionTime(actionDateTime.toLocalTime());
                history.setFieldName(fieldName);
                history.setOldValue(oldValue);
                history.setNewValue(newValue);
                history.setReason(reason);

                historyRepository.save(history);
        }

        // =====================================================
        // HELPERS
        // =====================================================

        private Employee findEmployee(Attendance attendance) {
                if (attendance.getEmployeeId() == null) {
                        return null;
                }

                return employeeRepository.findById(attendance.getEmployeeId())
                                .orElse(null);
        }

        private boolean sameTime(LocalTime first, LocalTime second) {
                return first == null ? second == null : first.equals(second);
        }

        private String valueOf(LocalTime value) {
                return value == null ? "-" : value.toString();
        }

        private String safeText(String value) {
                return value == null || value.isBlank() ? "-" : value.trim();
        }

        private String combineReasons(String first, String second) {
                return first + " | " + second;
        }

        private String clean(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }

                String text = value.trim();
                return text.length() > 2000 ? text.substring(0, 2000) : text;
        }

        private ResponseEntity<String> error(
                        HttpStatus status,
                        String message) {
                return ResponseEntity.status(status).body(message);
        }
}
