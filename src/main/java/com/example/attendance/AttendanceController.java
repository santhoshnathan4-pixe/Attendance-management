
package com.example.attendance;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@CrossOrigin(origins = {
        "https://attendance-management-3d-webinar.vercel.app",
        "https://attendance-management-git-main-3d-webinar.vercel.app",
        "https://attendance-management-lhsosyu8t-3d-webinar.vercel.app",
        "https://attendance-management-nine-beige.vercel.app"
})
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeLeaveBalanceRepository balanceRepository;

    // India Time Zone
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    // Office Timing
    private static final LocalTime OFFICE_START_TIME = LocalTime.of(10, 0);

    private static final LocalTime OFFICE_END_TIME = LocalTime.of(17, 30);

    public AttendanceController(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            LeaveRequestRepository leaveRequestRepository,
            EmployeeLeaveBalanceRepository balanceRepository) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.balanceRepository = balanceRepository;
    }

    // =========================
    // TEST API
    // =========================
    @GetMapping("/")
    public String home() {
        return "Attendance API Running Successfully!";
    }

    // =========================
    // CHECK IN
    // =========================
    @PostMapping("/check-in")
    @Transactional
    public String checkIn(@RequestParam String email) {

        // Active employee only
        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(email)
                .orElse(null);

        if (employee == null) {
            return "Employee Not Found or Employee is Inactive";
        }

        LocalDate today = LocalDate.now(INDIA_ZONE);

        LocalTime currentTime = LocalTime.now(INDIA_ZONE);

        // Duplicate check-in prevention
        if (attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        employee.getId(),
                        today)
                .isPresent()) {

            return "Already Checked In";
        }

        // =========================
        // CANCEL APPROVED LEAVE
        // WHEN EMPLOYEE CHECKS IN
        // =========================

        cancelApprovedLeaveForCheckIn(
                employee.getId(),
                today
        );

        Attendance attendance = new Attendance();

        attendance.setEmployeeId(employee.getId());
        attendance.setAttendanceDate(today);
        attendance.setCheckIn(currentTime);

        // =========================
        // ATTENDANCE STATUS
        // =========================

        if (currentTime.isAfter(OFFICE_START_TIME)) {
            attendance.setStatus("LATE");
        } else {
            attendance.setStatus("PRESENT");
        }

        attendanceRepository.save(attendance);

        if ("LATE".equals(attendance.getStatus())) {
            return "Check In Successful - " + employee.getName()
                    + " (LATE)";
        }

        return "Check In Successful - " + employee.getName();
    }

    // =========================
    // CANCEL APPROVED LEAVE
    // WHEN EMPLOYEE COMES TO OFFICE
    // =========================
    private void cancelApprovedLeaveForCheckIn(
            Integer employeeId,
            LocalDate today) {

        List<LeaveRequest> leaveRequests =
                leaveRequestRepository
                        .findByEmployeeIdAndLeaveDate(
                                employeeId,
                                today
                        );

        if (leaveRequests == null ||
                leaveRequests.isEmpty()) {

            return;
        }

        for (LeaveRequest leave : leaveRequests) {

            // Only APPROVED leave is cancelled.
            if (!"APPROVED".equalsIgnoreCase(
                    leave.getStatus())) {

                continue;
            }

            String leaveType =
                    leave.getLeaveType();

            // Permission is separate from normal leave.
            // Do not cancel permission just because
            // employee checked in.
            if (leaveType == null ||
                    leaveType.equalsIgnoreCase("PERMISSION")) {

                continue;
            }

            // =========================
            // RESTORE PAID LEAVE BALANCE
            // =========================

            if (leaveType.equalsIgnoreCase("SICK") ||
                    leaveType.equalsIgnoreCase("CASUAL")) {

                restorePaidLeaveBalance(leave);
            }

            // =========================
            // REMOVE CANCELLED LEAVE
            // =========================

            leaveRequestRepository.delete(leave);
        }
    }

    // =========================
    // RESTORE SICK / CASUAL
    // BALANCE AFTER CHECK-IN
    // =========================
    private void restorePaidLeaveBalance(
            LeaveRequest leave) {

        if (leave.getEmployeeId() == null ||
                leave.getLeaveDate() == null) {

            return;
        }

        LocalDate month =
                leave.getLeaveDate()
                        .withDayOfMonth(1);

        var balanceOptional =
                balanceRepository
                        .findByEmployeeIdAndBalanceMonth(
                                leave.getEmployeeId(),
                                month
                        );

        if (balanceOptional.isEmpty()) {
            return;
        }

        EmployeeLeaveBalance balance =
                balanceOptional.get();

        double duration =
                leave.getLeaveDuration() != null
                        ? leave.getLeaveDuration()
                        : 1.0;

        String leaveType =
                leave.getLeaveType();

        // =========================
        // RESTORE SICK
        // =========================

        if (leaveType.equalsIgnoreCase("SICK")) {

            double currentBalance =
                    balance.getSickBalance() != null
                            ? balance.getSickBalance()
                            : 0.0;

            balance.setSickBalance(
                    currentBalance + duration
            );
        }

        // =========================
        // RESTORE CASUAL
        // =========================

        if (leaveType.equalsIgnoreCase("CASUAL")) {

            double currentBalance =
                    balance.getCasualBalance() != null
                            ? balance.getCasualBalance()
                            : 0.0;

            balance.setCasualBalance(
                    currentBalance + duration
            );
        }

        balance.setUpdatedAt(
                LocalDateTime.now(INDIA_ZONE)
        );

        balanceRepository.save(balance);
    }

    // =========================
    // CHECK OUT
    // =========================
    @PostMapping("/check-out")
    public String checkOut(@RequestParam String email) {

        // Active employee only
        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(email)
                .orElse(null);

        if (employee == null) {
            return "Employee Not Found or Employee is Inactive";
        }

        LocalDate today = LocalDate.now(INDIA_ZONE);

        LocalTime currentTime = LocalTime.now(INDIA_ZONE);

        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        employee.getId(),
                        today)
                .orElse(null);

        // Check-in required
        if (attendance == null) {
            return "Please Check In First";
        }

        // Duplicate checkout prevention
        if (attendance.getCheckOut() != null) {
            return "Already Checked Out";
        }

        attendance.setCheckOut(currentTime);

        // =========================
        // EARLY CHECK-OUT
        // =========================

        if (currentTime.isBefore(OFFICE_END_TIME)) {

            // Preserve LATE status if employee was late
            if ("LATE".equalsIgnoreCase(attendance.getStatus())) {
                attendance.setStatus("LATE / EARLY CHECK-OUT");
            } else {
                attendance.setStatus("EARLY CHECK-OUT");
            }

        } else {

            // Preserve LATE status
            if (!"LATE".equalsIgnoreCase(attendance.getStatus())) {
                attendance.setStatus("PRESENT");
            }
        }

        attendanceRepository.save(attendance);

        if (currentTime.isBefore(OFFICE_END_TIME)) {
            return "Check Out Successful - " + employee.getName()
                    + " (EARLY CHECK-OUT)";
        }

        return "Check Out Successful - " + employee.getName();
    }

    // =========================
    // ATTENDANCE HISTORY
    // =========================
    @GetMapping("/history")
    public List<AttendanceResponse> getHistory(
            @RequestParam String email) {

        // Active employee only
        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(email)
                .orElse(null);

        if (employee == null) {
            return List.of();
        }

        return attendanceRepository
                .findByEmployeeIdOrderByAttendanceDateDesc(
                        employee.getId())
                .stream()
                .map(attendance -> new AttendanceResponse(
                        employee.getName(),
                        attendance.getAttendanceDate(),
                        attendance.getCheckIn(),
                        attendance.getCheckOut(),
                        attendance.getStatus()))
                .toList();
    }

    // =========================
    // ADMIN - ALL ATTENDANCE
    // =========================
    @GetMapping("/admin/all")
    public List<Attendance> getAllAttendance(
            @RequestParam(required = false) String date) {

        if (date != null && !date.isBlank()) {

            LocalDate attendanceDate = LocalDate.parse(date);

            return attendanceRepository
                    .findByAttendanceDateOrderByAttendanceDateDesc(
                            attendanceDate);
        }

        return attendanceRepository.findAll();
    }
}
