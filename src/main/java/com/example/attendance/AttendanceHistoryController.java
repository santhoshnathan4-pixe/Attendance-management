package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// =========================================================
// ADMIN - ATTENDANCE HISTORY (ATTENDANCE + LEAVE TOGETHER)
//
// For one employee and one month, returns:
//
// 1. Attendance sessions  (Check-In / Check-Out / Status)
// 2. Leave days           (Sick / Casual / LOP / Permission)
//
// so the Attendance History page shows everything that
// happened in that month in one list.
//
// This is a NEW endpoint. Existing endpoints are not changed.
// =========================================================

@RestController
@CrossOrigin(origins = {
                "https://attendance-management-3d-webinar.vercel.app",
                "https://attendance-management-git-main-3d-webinar.vercel.app",
                "https://attendance-management-lhsosyu8t-3d-webinar.vercel.app",
                "https://attendance-management-nine-beige.vercel.app"
})
@RequestMapping("/admin/attendance-history")
public class AttendanceHistoryController {

        private final AttendanceRepository attendanceRepository;
        private final LeaveRequestRepository leaveRequestRepository;
        private final EmployeeRepository employeeRepository;
        private final AdminRepository adminRepository;

        public AttendanceHistoryController(
                        AttendanceRepository attendanceRepository,
                        LeaveRequestRepository leaveRequestRepository,
                        EmployeeRepository employeeRepository,
                        AdminRepository adminRepository) {

                this.adminRepository = adminRepository;
                this.attendanceRepository = attendanceRepository;

                this.leaveRequestRepository = leaveRequestRepository;

                this.employeeRepository = employeeRepository;
        }

        // =====================================================
        // EMPLOYEE MONTH HISTORY
        //
        // recordType = ATTENDANCE or LEAVE
        // Latest date first. On the same date, attendance
        // sessions come first, then leave.
        // =====================================================

        @GetMapping("/employee-month")
        public List<Map<String, Object>> getEmployeeMonth(
                        @RequestParam Integer employeeId,
                        @RequestParam int month,
                        @RequestParam int year) {

                if (month < 1 || month > 12
                                || year < 1900 || year > 9999) {

                        throw new IllegalArgumentException(
                                        "Invalid month or year");
                }

                Employee employee = employeeRepository
                                .findById(employeeId)
                                .orElse(null);

                if (employee == null) {

                        return List.of();
                }

                YearMonth selectedMonth = YearMonth.of(year, month);

                LocalDate startDate = selectedMonth.atDay(1);

                LocalDate endDate = selectedMonth.atEndOfMonth();

                List<Map<String, Object>> records = new ArrayList<>();
                Map<Integer, Admin> adminMap = new java.util.HashMap<>();

                // =================================================
                // 1. ATTENDANCE SESSIONS
                // =================================================

                for (Attendance attendance : attendanceRepository
                                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDescCheckInDesc(
                                                employeeId,
                                                startDate,
                                                endDate)) {

                        Map<String, Object> data = new LinkedHashMap<>();

                        data.put("recordType", "ATTENDANCE");
                        data.put("recordId", attendance.getId());
                        data.put("date", attendance.getAttendanceDate());
                        data.put("checkIn", attendance.getCheckIn());
                        data.put("checkOut", attendance.getCheckOut());
                        data.put("status", attendance.getStatus());
                        data.put("missingCheckoutStatus", attendance.getMissingCheckoutStatus());
                        data.put("missingCheckoutRequestedTime", attendance.getMissingCheckoutRequestedTime());
                        data.put("missingCheckoutReason", attendance.getMissingCheckoutReason());
                        data.put("missingCheckoutReviewedBy", attendance.getMissingCheckoutReviewedBy());
                        data.put("missingCheckoutReviewedAt", attendance.getMissingCheckoutReviewedAt());
                        data.put("missingCheckoutAdminRemark", attendance.getMissingCheckoutAdminRemark());
                        data.put("adminNote", attendanceNote(attendance));

                        records.add(data);
                }

                // =================================================
                // 2. LEAVE DAYS
                //
                // Leave is stored one row per date. All statuses
                // (PENDING / APPROVED / REJECTED) are shown with
                // their status.
                // =================================================

                for (LeaveRequest leave : leaveRequestRepository
                                .findByLeaveDateBetween(
                                                startDate,
                                                endDate)) {

                        if (leave.getEmployeeId() == null
                                        || !employeeId.equals(
                                                        leave.getEmployeeId())
                                        || leave.getLeaveDate() == null) {

                                continue;
                        }

                        Map<String, Object> data = new LinkedHashMap<>();

                        data.put("recordType", "LEAVE");
                        data.put("recordId", leave.getId());
                        data.put("date", leave.getLeaveDate());
                        data.put("leaveType", leave.getLeaveType());
                        data.put("leaveLabel", getLeaveLabel(leave));
                        data.put("leaveDuration", leave.getLeaveDuration());
                        data.put("leaveStatus", leave.getStatus());
                        data.put("reason", leave.getReason());

                        String decidedByName = null;
                        if (leave.getDecidedBy() != null) {
                                Admin decider = adminMap.get(leave.getDecidedBy());
                                if (decider == null) {
                                        decider = adminRepository.findById(leave.getDecidedBy()).orElse(null);
                                        adminMap.put(leave.getDecidedBy(), decider);
                                }
                                if (decider != null) {
                                        decidedByName = decider.getAdminName();
                                }
                        }
                        data.put("decidedByName", decidedByName);
                        data.put("decidedAt", leave.getDecidedAt());
                        data.put("decisionReason", leave.getDecisionReason());
                        data.put("adminNote", leaveNote(leave, decidedByName));

                        records.add(data);
                }

                // =================================================
                // SORT: LATEST DATE FIRST, ATTENDANCE BEFORE LEAVE
                // =================================================

                records.sort(
                                Comparator
                                                .comparing(
                                                                (Map<String, Object> record) -> (LocalDate) record.get("date"))
                                                .reversed()
                                                .thenComparing(
                                                                record -> "LEAVE".equals(
                                                                                record.get("recordType")) ? 1 : 0));

                return records;
        }

        // Plain sentence: what the admin did on this record.
        private String attendanceNote(Attendance a) {

                String st = a.getMissingCheckoutStatus();

                if (st == null || st.isBlank()) {
                        return null;
                }

                StringBuilder sb = new StringBuilder("Check-out request ");
                sb.append(st.toUpperCase());

                if (a.getMissingCheckoutRequestedTime() != null) {
                        sb.append(" (asked ").append(a.getMissingCheckoutRequestedTime()).append(")");
                }
                if (a.getMissingCheckoutReviewedBy() != null && !a.getMissingCheckoutReviewedBy().isBlank()) {
                        sb.append(" by ").append(a.getMissingCheckoutReviewedBy());
                }
                if (a.getMissingCheckoutReviewedAt() != null) {
                        sb.append(" on ").append(a.getMissingCheckoutReviewedAt().toLocalDate())
                                        .append(" ").append(a.getMissingCheckoutReviewedAt().toLocalTime().withNano(0));
                }
                if (a.getMissingCheckoutAdminRemark() != null && !a.getMissingCheckoutAdminRemark().isBlank()) {
                        sb.append(". Admin remark: ").append(a.getMissingCheckoutAdminRemark());
                }
                return sb.toString();
        }

        private String leaveNote(LeaveRequest l, String by) {

                if (l.getStatus() == null || "PENDING".equalsIgnoreCase(l.getStatus())) {
                        return null;
                }

                StringBuilder sb = new StringBuilder(l.getStatus().toUpperCase());
                if (by != null) {
                        sb.append(" by ").append(by);
                }
                if (l.getDecidedAt() != null) {
                        sb.append(" on ").append(l.getDecidedAt().toLocalDate())
                                        .append(" ").append(l.getDecidedAt().toLocalTime().withNano(0));
                }
                if (l.getDecisionReason() != null && !l.getDecisionReason().isBlank()) {
                        sb.append(". Reason: ").append(l.getDecisionReason());
                }
                return sb.toString();
        }

        // =====================================================
        // LEAVE LABEL
        //
        // Sick Leave / Casual Leave / LOP / Permission
        // Half-day leave shows "(Half Day)".
        // =====================================================

        private String getLeaveLabel(
                        LeaveRequest leave) {

                String type = leave.getLeaveType() == null
                                ? ""
                                : leave.getLeaveType()
                                                .trim()
                                                .toUpperCase();

                boolean halfDay = leave.getLeaveDuration() != null
                                && leave.getLeaveDuration() == 0.5;

                String label;

                switch (type) {

                        case "SICK":
                        case "SICK_HALF":
                                label = "Sick Leave";
                                break;

                        case "CASUAL":
                        case "CASUAL_HALF":
                                label = "Casual Leave";
                                break;

                        case "LOP":
                                label = "LOP";
                                break;

                        case "PERMISSION":
                                return "Permission";

                        default:
                                label = type.isEmpty() ? "Leave" : type;
                }

                if (halfDay
                                || type.endsWith("_HALF")) {

                        label = label + " (Half Day)";
                }

                return label;
        }
}