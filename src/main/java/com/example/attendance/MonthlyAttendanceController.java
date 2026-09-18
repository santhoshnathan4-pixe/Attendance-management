package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/admin/monthly-report")
public class MonthlyAttendanceController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public MonthlyAttendanceController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @GetMapping
    public List<MonthlyAttendanceResponse> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {

        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Attendance> attendanceList =
                attendanceRepository.findByAttendanceDateBetween(
                        startDate,
                        endDate
                );

        return employeeRepository.findAll()
                .stream()
                .filter(employee ->
                        "EMPLOYEE".equalsIgnoreCase(employee.getRole()))
                .map(employee -> {

                    Integer employeeId = employee.getId();

                    // Present days
                    int presentDays = (int) attendanceList.stream()
                            .filter(attendance ->
                                    employeeId.equals(
                                            attendance.getEmployeeId()))
                            .filter(attendance ->
                                    "PRESENT".equalsIgnoreCase(
                                            attendance.getStatus()))
                            .count();

                    // Get all approved leaves for selected month
                    List<LeaveRequest> monthlyLeaves =
                            leaveRequestRepository
                                    .findByEmployeeIdOrderByLeaveDateDesc(
                                            employeeId
                                    )
                                    .stream()
                                    .filter(leave ->
                                            !leave.getLeaveDate()
                                                    .isBefore(startDate))
                                    .filter(leave ->
                                            !leave.getLeaveDate()
                                                    .isAfter(endDate))
                                    .filter(leave ->
                                            "APPROVED".equalsIgnoreCase(
                                                    leave.getStatus()))
                                    .toList();

                    // Sick Leave
                    double sickLeave = monthlyLeaves.stream()
                            .filter(leave ->
                                    "SICK".equalsIgnoreCase(
                                            leave.getLeaveType()))
                            .mapToDouble(leave ->
                                    leave.getLeaveDuration() != null
                                            ? leave.getLeaveDuration()
                                            : 1.0)
                            .sum();

                    // Casual Leave
                    double casualLeave = monthlyLeaves.stream()
                            .filter(leave ->
                                    "CASUAL".equalsIgnoreCase(
                                            leave.getLeaveType()))
                            .mapToDouble(leave ->
                                    leave.getLeaveDuration() != null
                                            ? leave.getLeaveDuration()
                                            : 1.0)
                            .sum();

                    // Half Day
                    double halfDay = monthlyLeaves.stream()
                            .filter(leave ->
                                    leave.getLeaveDuration() != null
                                            && leave.getLeaveDuration() == 0.5)
                            .mapToDouble(LeaveRequest::getLeaveDuration)
                            .sum();

                    // Permission
                    int permission = (int) monthlyLeaves.stream()
                            .filter(leave ->
                                    "PERMISSION".equalsIgnoreCase(
                                            leave.getLeaveType()))
                            .count();

                    // Working days
                    int workingDays = 26;

                    // Absent days
                    double absent = Math.max(
                            0,
                            workingDays
                                    - presentDays
                                    - sickLeave
                                    - casualLeave
                    );

                    return new MonthlyAttendanceResponse(
                            employee.getEmployeeCode(),
                            employee.getName(),
                            presentDays,
                            sickLeave,
                            casualLeave,
                            halfDay,
                            permission,
                            absent
                    );
                })
                .toList();
    }
}