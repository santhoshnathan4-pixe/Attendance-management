
package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

// =========================================================
// MISSING CHECK-OUT - EMPLOYEE SIDE
//
// Request is saved in the existing attendance table.
// Submission is also recorded in AdminActionHistory.
//
// Submitting a request NEVER changes check_out, attendance
// status or salary. Admin review is handled separately.
// =========================================================

@RestController
@CrossOrigin(origins = {
                "https://attendance-management-3d-webinar.vercel.app",
                "https://attendance-management-git-main-3d-webinar.vercel.app",
                "https://attendance-management-lhsosyu8t-3d-webinar.vercel.app",
                "https://attendance-management-nine-beige.vercel.app"
})
@RequestMapping("/attendance/missing-checkout")
public class MissingCheckoutController {

        private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

        private final AttendanceRepository attendanceRepository;
        private final EmployeeRepository employeeRepository;
        private final AdminActionHistoryRepository historyRepository;

        public MissingCheckoutController(
                        AttendanceRepository attendanceRepository,
                        EmployeeRepository employeeRepository,
                        AdminActionHistoryRepository historyRepository) {

                this.attendanceRepository = attendanceRepository;
                this.employeeRepository = employeeRepository;
                this.historyRepository = historyRepository;
        }

        // =====================================================
        // REQUEST BODY
        // =====================================================

        public record MissingCheckoutRequestBody(
                        String email,
                        Integer attendanceId,
                        String requestedCheckOut,
                        String reason) {
        }

        // =====================================================
        // PENDING POPUP CHECK
        // =====================================================

        @GetMapping("/pending")
        public ResponseEntity<Map<String, Object>> getPending(
                        @RequestParam String email) {

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email)
                                .orElse(null);

                if (employee == null) {
                        return ResponseEntity.noContent().build();
                }

                LocalDate today = LocalDate.now(INDIA_ZONE);

                Attendance attendance = attendanceRepository
                                .findFirstByEmployeeIdAndAttendanceDateBeforeAndCheckOutIsNullAndMissingCheckoutStatusIsNullOrderByAttendanceDateAscCheckInAsc(
                                                employee.getId(), today)
                                .orElse(null);

                if (attendance == null) {
                        return ResponseEntity.noContent().build();
                }

                Map<String, Object> data = new LinkedHashMap<>();

                data.put("attendanceId", attendance.getId());
                data.put("employeeName", employee.getName());
                data.put("employeeCode", employee.getEmployeeCode());
                data.put("attendanceDate", attendance.getAttendanceDate());
                data.put("checkIn", attendance.getCheckIn());

                return ResponseEntity.ok(data);
        }

        // =====================================================
        // SUBMIT REQUEST
        // =====================================================

        @PostMapping("/request")
        @Transactional
        public ResponseEntity<String> submitRequest(
                        @RequestBody MissingCheckoutRequestBody body) {

                if (body == null
                                || body.email() == null
                                || body.attendanceId() == null) {

                        return error(
                                        HttpStatus.BAD_REQUEST,
                                        "Email and attendance record are required.");
                }

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(body.email())
                                .orElse(null);

                if (employee == null) {
                        return error(
                                        HttpStatus.NOT_FOUND,
                                        "Employee Not Found or Employee is Inactive");
                }

                // Validate explanation.

                String reason = body.reason() == null
                                ? ""
                                : body.reason().trim();

                if (reason.isEmpty()) {
                        return error(
                                        HttpStatus.BAD_REQUEST,
                                        "Please enter your explanation.");
                }

                if (reason.length() > 2000) {
                        return error(
                                        HttpStatus.BAD_REQUEST,
                                        "Explanation is too long (maximum 2000 characters).");
                }

                // Validate requested Check-Out time.

                LocalTime requestedTime;

                try {
                        requestedTime = LocalTime.parse(
                                        body.requestedCheckOut());
                } catch (DateTimeParseException | NullPointerException ex) {
                        return error(
                                        HttpStatus.BAD_REQUEST,
                                        "Please enter a valid Check-Out time.");
                }

                // Lock the attendance row.

                Attendance attendance = attendanceRepository
                                .findByIdForUpdate(body.attendanceId())
                                .orElse(null);

                if (attendance == null
                                || !employee.getId().equals(attendance.getEmployeeId())) {

                        return error(
                                        HttpStatus.NOT_FOUND,
                                        "Attendance record not found.");
                }

                // Prevent duplicate requests.

                if (attendance.getMissingCheckoutStatus() != null) {
                        return error(
                                        HttpStatus.CONFLICT,
                                        "A request was already submitted for this attendance session.");
                }

                // Session must be missing Check-Out.

                if (attendance.getCheckOut() != null) {
                        return error(
                                        HttpStatus.BAD_REQUEST,
                                        "This attendance session already has a Check-Out time.");
                }

                if (attendance.getAttendanceDate() == null
                                || !attendance.getAttendanceDate().isBefore(
                                                LocalDate.now(INDIA_ZONE))) {

                        return error(
                                        HttpStatus.BAD_REQUEST,
                                        "A Missing Check-Out request can only be submitted for a previous day.");
                }

                if (attendance.getCheckIn() != null
                                && !requestedTime.isAfter(attendance.getCheckIn())) {

                        return error(
                                        HttpStatus.BAD_REQUEST,
                                        "Check-Out time must be after the Check-In time.");
                }

                // Save the employee request.

                LocalDateTime submittedAt = LocalDateTime.now(INDIA_ZONE);

                attendance.setMissingCheckoutRequestedTime(requestedTime);
                attendance.setMissingCheckoutReason(reason);
                attendance.setMissingCheckoutStatus("PENDING");
                attendance.setMissingCheckoutSubmittedAt(submittedAt);

                attendanceRepository.save(attendance);

                // Save the request-submission audit record.
                // Employee submission is explicitly identified in the action.

                AdminActionHistory history = new AdminActionHistory();

                history.setAdminName("Employee: " + employee.getName());
                history.setAction("MISSING CHECKOUT REQUEST SUBMITTED");
                history.setEmployeeId(employee.getId());
                history.setEmployeeCode(employee.getEmployeeCode());
                history.setEmployeeName(employee.getName());
                history.setActionDate(submittedAt.toLocalDate());
                history.setActionTime(submittedAt.toLocalTime());
                history.setFieldName(
                                "Missing Check-Out Request - Attendance Date: "
                                                + attendance.getAttendanceDate());
                history.setOldValue("Check-Out: Missing");
                history.setNewValue(
                                "Requested Check-Out: " + requestedTime
                                                + " | Status: PENDING");
                history.setReason(reason);

                historyRepository.save(history);

                return ResponseEntity.ok(
                                "Missing Check-Out request submitted for admin review.");
        }

        // =====================================================
        // ERROR RESPONSE
        // =====================================================

        private ResponseEntity<String> error(
                        HttpStatus status,
                        String message) {

                return ResponseEntity
                                .status(status)
                                .body(message);
        }
}
