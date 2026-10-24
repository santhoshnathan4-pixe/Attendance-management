
package com.example.attendance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

        private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
        private static final LocalTime DEFAULT_SHIFT_END = LocalTime.of(18, 0);

        private final EmployeeRepository employeeRepository;
        private final AttendanceRepository attendanceRepository;
        private final LeaveRequestRepository leaveRequestRepository;
        private final LeavePermissionSettingRepository leavePermissionSettingRepository;
        private final ShiftSettingRepository shiftSettingRepository;

        public AdminDashboardController(
                        EmployeeRepository employeeRepository,
                        AttendanceRepository attendanceRepository,
                        LeaveRequestRepository leaveRequestRepository,
                        LeavePermissionSettingRepository leavePermissionSettingRepository,
                        ShiftSettingRepository shiftSettingRepository) {

                this.employeeRepository = employeeRepository;
                this.attendanceRepository = attendanceRepository;
                this.leaveRequestRepository = leaveRequestRepository;
                this.leavePermissionSettingRepository = leavePermissionSettingRepository;
                this.shiftSettingRepository = shiftSettingRepository;
        }

        // =====================================================
        // DASHBOARD STATS
        // =====================================================

        private volatile Map<String, Object> statsCache = null;
        private volatile long statsCacheAt = 0;

        @GetMapping("/stats")
        public Map<String, Object> getDashboardStats() {

                // 20-second cache: many admins / refreshes do not hit the DB each time.
                Map<String, Object> cached = statsCache;
                if (cached != null && System.currentTimeMillis() - statsCacheAt < 20000) {
                        return cached;
                }

                Map<String, Object> fresh = computeDashboardStats();
                statsCache = fresh;
                statsCacheAt = System.currentTimeMillis();
                return fresh;
        }

        private Map<String, Object> computeDashboardStats() {

                Map<String, Object> stats = new HashMap<>();

                LocalDate today = LocalDate.now(INDIA_ZONE);
                LocalTime currentTime = LocalTime.now(INDIA_ZONE);

                List<Employee> employees = getActiveEmployees();

                int totalEmployees = employees.size();

                Map<Integer, List<Attendance>> attendanceByEmployee = getTodayAttendanceByEmployee(today);

                int presentToday = 0;
                int lateToday = 0;
                int earlyCheckoutToday = 0;

                for (Employee employee : employees) {

                        List<Attendance> attendanceList = attendanceByEmployee.get(employee.getId());

                        if (attendanceList == null || attendanceList.isEmpty()) {
                                continue;
                        }

                        presentToday++;

                        boolean late = false;
                        boolean earlyCheckout = false;

                        for (Attendance attendance : attendanceList) {

                                String status = attendance.getStatus();

                                if (status != null) {

                                        String upperStatus = status.toUpperCase();

                                        if (upperStatus.startsWith("LATE")) {
                                                late = true;
                                        }

                                        if (upperStatus.startsWith("EARLY")) {
                                                earlyCheckout = true;
                                        }
                                }
                        }

                        if (late) {
                                lateToday++;
                        }

                        if (earlyCheckout) {
                                earlyCheckoutToday++;
                        }
                }

                /*
                 * IMPORTANT:
                 * Use the same calculation as the Missing Check-Out details popup.
                 * The count represents unresolved attendance records across all dates.
                 */
                int missingCheckOut = getMissingCheckoutDetails(
                                employees, today, currentTime).size();

                int absentToday = totalEmployees - presentToday;

                long pendingLeaves = leaveRequestRepository
                                .findByLeaveDateBetween(today, today)
                                .stream()
                                .filter(leave -> "PENDING".equalsIgnoreCase(leave.getStatus()))
                                .count();

                stats.put("totalEmployees", totalEmployees);
                stats.put("presentToday", presentToday);
                stats.put("lateToday", lateToday);
                stats.put("earlyCheckoutToday", earlyCheckoutToday);
                stats.put("missingCheckOut", missingCheckOut);
                stats.put("absentToday", Math.max(absentToday, 0));
                stats.put("pendingLeaves", pendingLeaves);

                return stats;
        }

        // =====================================================
        // DASHBOARD DETAILS
        // =====================================================

        @GetMapping("/details")
        public List<Map<String, Object>> getDashboardDetails(
                        @RequestParam String type) {

                LocalDate today = LocalDate.now(INDIA_ZONE);

                List<Map<String, Object>> employees = new ArrayList<>();

                List<Employee> activeEmployees = getActiveEmployees();

                Map<Integer, List<Attendance>> attendanceByEmployee = getTodayAttendanceByEmployee(today);

                // =================================================
                // PRESENT
                // =================================================

                if ("PRESENT".equalsIgnoreCase(type)) {

                        for (Employee employee : activeEmployees) {

                                List<Attendance> attendanceList = attendanceByEmployee.get(employee.getId());

                                if (attendanceList == null || attendanceList.isEmpty()) {
                                        continue;
                                }

                                Map<String, Object> data = createEmployeeData(employee);

                                Attendance firstAttendance = attendanceList.get(0);

                                data.put("checkIn", firstAttendance.getCheckIn());
                                data.put("checkOut", getLatestCheckOut(attendanceList));
                                data.put("status", getOverallStatus(attendanceList));

                                employees.add(data);
                        }
                }

                // =================================================
                // LATE
                // =================================================

                else if ("LATE".equalsIgnoreCase(type)) {

                        for (Employee employee : activeEmployees) {

                                List<Attendance> attendanceList = attendanceByEmployee.get(employee.getId());

                                if (attendanceList == null || attendanceList.isEmpty()) {
                                        continue;
                                }

                                Attendance lateAttendance = null;

                                for (Attendance attendance : attendanceList) {

                                        String status = attendance.getStatus();

                                        if (status != null
                                                        && status.toUpperCase().startsWith("LATE")) {

                                                lateAttendance = attendance;
                                                break;
                                        }
                                }

                                if (lateAttendance == null) {
                                        continue;
                                }

                                Map<String, Object> data = createEmployeeData(employee);

                                data.put("checkIn", lateAttendance.getCheckIn());
                                data.put("checkOut", getLatestCheckOut(attendanceList));
                                data.put("status", lateAttendance.getStatus());

                                employees.add(data);
                        }
                }

                // =================================================
                // EARLY CHECKOUT
                // =================================================

                else if ("EARLY".equalsIgnoreCase(type)) {

                        for (Employee employee : activeEmployees) {

                                List<Attendance> attendanceList = attendanceByEmployee.get(employee.getId());

                                if (attendanceList == null || attendanceList.isEmpty()) {
                                        continue;
                                }

                                Attendance earlyAttendance = null;

                                for (Attendance attendance : attendanceList) {

                                        String status = attendance.getStatus();

                                        if (status != null
                                                        && status.toUpperCase().startsWith("EARLY")) {

                                                earlyAttendance = attendance;
                                                break;
                                        }
                                }

                                if (earlyAttendance == null) {
                                        continue;
                                }

                                Map<String, Object> data = createEmployeeData(employee);

                                data.put("checkIn", earlyAttendance.getCheckIn());
                                data.put("checkOut", earlyAttendance.getCheckOut());
                                data.put("status", earlyAttendance.getStatus());

                                employees.add(data);
                        }
                }

                // =================================================
                // MISSING CHECK-OUT - SAME CALCULATION AS DASHBOARD
                // =================================================

                else if ("MISSING_CHECKOUT".equalsIgnoreCase(type)) {

                        LocalTime currentTime = LocalTime.now(INDIA_ZONE);

                        return getMissingCheckoutDetails(
                                        activeEmployees, today, currentTime);
                }

                // =================================================
                // ABSENT
                // =================================================

                else if ("ABSENT".equalsIgnoreCase(type)) {

                        List<LeaveRequest> todayLeaves = leaveRequestRepository
                                        .findByLeaveDateBetween(today, today);

                        for (Employee employee : activeEmployees) {

                                List<Attendance> attendanceList = attendanceByEmployee.get(employee.getId());

                                if (attendanceList != null && !attendanceList.isEmpty()) {
                                        continue;
                                }

                                Map<String, Object> data = createEmployeeData(employee);

                                data.put("attendanceStatus", "ABSENT");

                                LeaveRequest leaveRequest = todayLeaves
                                                .stream()
                                                .filter(leave -> employee.getId()
                                                                .equals(leave.getEmployeeId()))
                                                .findFirst()
                                                .orElse(null);

                                if (leaveRequest != null) {

                                        data.put("leaveRequestId", leaveRequest.getId());
                                        data.put("leaveType", leaveRequest.getLeaveType());
                                        data.put("leaveReason", leaveRequest.getReason());
                                        data.put("leaveStatus", leaveRequest.getStatus());

                                } else {

                                        data.put("leaveRequestId", null);
                                        data.put("leaveType", null);
                                        data.put("leaveReason", null);
                                        data.put("leaveStatus", "NO_LEAVE_REQUEST");
                                }

                                employees.add(data);
                        }
                }

                return employees;
        }

        // =====================================================
        // SHARED MISSING CHECK-OUT CALCULATION
        // Used by BOTH dashboard stats and details popup.
        // =====================================================

        private List<Map<String, Object>> getMissingCheckoutDetails(
                        List<Employee> activeEmployees,
                        LocalDate todayDate,
                        LocalTime currentTime) {

                List<Map<String, Object>> missingRecords = new ArrayList<>();

                // Only open sessions up to today (was: whole table).
                List<Attendance> allAttendance = attendanceRepository
                                .findByCheckOutIsNullAndCheckInIsNotNullAndAttendanceDateLessThanEqual(todayDate);

                // Load the small settings tables ONCE (was: 4 queries per row).
                loadShiftCaches();
                Map<Integer, LocalTime> shiftEndCache = new HashMap<>();

                Map<Integer, Employee> activeEmployeeMap = new HashMap<>();

                for (Employee employee : activeEmployees) {
                        activeEmployeeMap.put(employee.getId(), employee);
                }

                for (Attendance attendance : allAttendance) {

                        if (attendance.getEmployeeId() == null
                                        || attendance.getAttendanceDate() == null
                                        || attendance.getCheckIn() == null
                                        || attendance.getCheckOut() != null) {
                                continue;
                        }

                        Employee employee = activeEmployeeMap.get(attendance.getEmployeeId());

                        if (employee == null) {
                                continue;
                        }

                        LocalDate attendanceDate = attendance.getAttendanceDate();

                        // Ignore future attendance records.
                        if (attendanceDate.isAfter(todayDate)) {
                                continue;
                        }

                        LocalTime effectiveShiftEnd = shiftEndCache.computeIfAbsent(
                                        employee.getId(), k -> getEffectiveShiftEndTime(employee));

                        if (effectiveShiftEnd == null) {
                                effectiveShiftEnd = DEFAULT_SHIFT_END;
                        }

                        /*
                         * For today's attendance, wait until the employee's shift ends.
                         * Past dates are eligible because their shift has already ended.
                         */
                        if (attendanceDate.equals(todayDate)
                                        && currentTime.isBefore(effectiveShiftEnd)) {
                                continue;
                        }

                        Map<String, Object> data = createEmployeeData(employee);

                        data.put("attendanceId", attendance.getId());
                        data.put("attendanceDate", attendanceDate);
                        data.put("checkIn", attendance.getCheckIn());
                        data.put("checkOut", null);
                        data.put("shiftEndTime", effectiveShiftEnd);
                        data.put("status", attendance.getStatus());
                        data.put("attendanceStatus", "MISSING_CHECKOUT");

                        missingRecords.add(data);
                }

                return missingRecords;
        }

        // =====================================================
        // EFFECTIVE SHIFT END TIME
        // Priority: EMPLOYEE -> ROLE -> DEFAULT
        // =====================================================

        private Map<String, LeavePermissionSetting> cEmp = new HashMap<>();
        private Map<String, LeavePermissionSetting> cRole = new HashMap<>();
        private LeavePermissionSetting cDefault = null;
        private Map<String, LocalTime> cShiftEnd = new HashMap<>();

        private synchronized void loadShiftCaches() {

                cEmp = new HashMap<>();
                cRole = new HashMap<>();
                cDefault = null;
                cShiftEnd = new HashMap<>();

                for (LeavePermissionSetting st : leavePermissionSettingRepository.findAll()) {

                        String type = st.getSettingType() == null ? "" : st.getSettingType();

                        if ("EMPLOYEE".equals(type) && st.getEmployeeId() != null) {
                                cEmp.put(String.valueOf(st.getEmployeeId()), st);
                        } else if ("ROLE".equals(type) && st.getRole() != null) {
                                cRole.put(st.getRole().trim().toUpperCase(), st);
                        } else if ("DEFAULT".equals(type)) {
                                cDefault = st;
                        }
                }

                for (ShiftSetting sh : shiftSettingRepository.findAll()) {
                        if (sh.getShiftType() != null && sh.getEndTime() != null) {
                                cShiftEnd.put(sh.getShiftType(), sh.getEndTime());
                        }
                }
        }

        private LocalTime getEffectiveShiftEndTime(Employee employee) {

                String shiftType = "GENERAL";

                LeavePermissionSetting employeeSetting = cEmp.get(String.valueOf(employee.getId()));

                if (employeeSetting != null
                                && employeeSetting.getShiftType() != null
                                && !employeeSetting.getShiftType().isBlank()) {
                        shiftType = normalizeShiftType(employeeSetting.getShiftType());
                }

                if ("GENERAL".equals(shiftType)
                                && employee.getRole() != null
                                && !employee.getRole().isBlank()) {

                        LeavePermissionSetting roleSetting = cRole.get(employee.getRole().trim().toUpperCase());

                        if (roleSetting != null
                                        && roleSetting.getShiftType() != null
                                        && !roleSetting.getShiftType().isBlank()) {
                                shiftType = normalizeShiftType(roleSetting.getShiftType());
                        }
                }

                if ("GENERAL".equals(shiftType)
                                && cDefault != null
                                && cDefault.getShiftType() != null
                                && !cDefault.getShiftType().isBlank()) {
                        shiftType = normalizeShiftType(cDefault.getShiftType());
                }

                LocalTime end = cShiftEnd.get(shiftType);

                return end != null ? end : DEFAULT_SHIFT_END;
        }

        private String normalizeShiftType(String shiftType) {

                if (shiftType == null || shiftType.isBlank()) {
                        return "GENERAL";
                }

                return shiftType.trim()
                                .toUpperCase()
                                .replace("-", "_")
                                .replace(" ", "_");
        }

        // =====================================================
        // ACTIVE EMPLOYEES
        // =====================================================

        private List<Employee> getActiveEmployees() {

                return employeeRepository
                                .findByActiveTrue()
                                .stream()
                                .toList();
        }

        // =====================================================
        // TODAY ATTENDANCE GROUPED BY EMPLOYEE
        // =====================================================

        private Map<Integer, List<Attendance>> getTodayAttendanceByEmployee(
                        LocalDate date) {

                List<Attendance> attendanceList = attendanceRepository
                                .findByAttendanceDateOrderByAttendanceDateDesc(date);

                Map<Integer, List<Attendance>> result = new HashMap<>();

                for (Attendance attendance : attendanceList) {

                        if (attendance.getEmployeeId() == null) {
                                continue;
                        }

                        result.computeIfAbsent(
                                        attendance.getEmployeeId(),
                                        key -> new ArrayList<>()).add(attendance);
                }

                return result;
        }

        // =====================================================
        // EMPLOYEE BASIC DATA
        // =====================================================

        private Map<String, Object> createEmployeeData(Employee employee) {

                Map<String, Object> data = new LinkedHashMap<>();

                data.put("employeeId", employee.getId());
                data.put("employeeCode", employee.getEmployeeCode());
                data.put("employeeName", employee.getName());

                return data;
        }

        // =====================================================
        // LATEST CHECK-OUT
        // =====================================================

        private Object getLatestCheckOut(
                        List<Attendance> attendanceList) {

                for (Attendance attendance : attendanceList) {

                        if (attendance.getCheckOut() != null) {
                                return attendance.getCheckOut();
                        }
                }

                return null;
        }

        // =====================================================
        // OVERALL STATUS
        // =====================================================

        private String getOverallStatus(
                        List<Attendance> attendanceList) {

                for (Attendance attendance : attendanceList) {

                        String status = attendance.getStatus();

                        if (status == null) {
                                continue;
                        }

                        String upperStatus = status.toUpperCase();

                        if (upperStatus.startsWith("LATE")) {
                                return status;
                        }

                        if (upperStatus.startsWith("EARLY")) {
                                return status;
                        }
                }

                return "PRESENT";
        }
}
