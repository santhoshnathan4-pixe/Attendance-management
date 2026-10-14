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

        public AttendanceHistoryController(
                        AttendanceRepository attendanceRepository,
                        LeaveRequestRepository leaveRequestRepository,
                        EmployeeRepository employeeRepository) {

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