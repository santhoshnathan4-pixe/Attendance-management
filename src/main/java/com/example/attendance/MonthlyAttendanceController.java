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

    // =========================================
    // MONTHLY ATTENDANCE REPORT
    // =========================================

    @GetMapping
    public List<MonthlyAttendanceResponse> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {

        YearMonth yearMonth =
                YearMonth.of(year, month);

        LocalDate startDate =
                yearMonth.atDay(1);

        LocalDate endDate =
                yearMonth.atEndOfMonth();

        // =========================================
        // GET ATTENDANCE FOR SELECTED MONTH
        // =========================================

        List<Attendance> attendanceList =
                attendanceRepository.findByAttendanceDateBetween(
                        startDate,
                        endDate
                );

        // =========================================
        // GET ALL ACTIVE NON-ADMIN EMPLOYEES
        // =========================================

        return employeeRepository.findAll()
                .stream()
                .filter(employee ->
                        Boolean.TRUE.equals(
                                employee.getActive()
                        )
                )
                .filter(employee ->
                        !"ADMIN".equalsIgnoreCase(
                                employee.getRole()
                        )
                )
                .map(employee -> {

                    Integer employeeId =
                            employee.getId();

                    // =========================================
                    // PRESENT DAYS
                    // PRESENT + LATE = ATTENDED
                    // =========================================

                    int presentDays =
                            (int) attendanceList.stream()

                                    .filter(attendance ->
                                            employeeId.equals(
                                                    attendance.getEmployeeId()
                                            )
                                    )

                                    .filter(attendance ->
                                            "PRESENT".equalsIgnoreCase(
                                                    attendance.getStatus()
                                            )
                                            ||
                                            "LATE".equalsIgnoreCase(
                                                    attendance.getStatus()
                                            )
                                    )

                                    .count();

                    // =========================================
                    // APPROVED LEAVES
                    // =========================================

                    List<LeaveRequest> monthlyLeaves =
                            leaveRequestRepository
                                    .findByEmployeeIdOrderByLeaveDateDesc(
                                            employeeId
                                    )
                                    .stream()

                                    .filter(leave ->
                                            leave.getLeaveDate() != null
                                    )

                                    .filter(leave ->
                                            !leave.getLeaveDate()
                                                    .isBefore(startDate)
                                    )

                                    .filter(leave ->
                                            !leave.getLeaveDate()
                                                    .isAfter(endDate)
                                    )

                                    .filter(leave ->
                                            "APPROVED".equalsIgnoreCase(
                                                    leave.getStatus()
                                            )
                                    )

                                    .toList();

                    // =========================================
                    // SICK LEAVE
                    // =========================================

                    double sickLeave =
                            monthlyLeaves.stream()

                                    .filter(leave ->
                                            "SICK".equalsIgnoreCase(
                                                    leave.getLeaveType()
                                            )
                                    )

                                    .mapToDouble(leave ->
                                            leave.getLeaveDuration() != null
                                                    ? leave.getLeaveDuration()
                                                    : 1.0
                                    )

                                    .sum();

                    // =========================================
                    // CASUAL LEAVE
                    // =========================================

                    double casualLeave =
                            monthlyLeaves.stream()

                                    .filter(leave ->
                                            "CASUAL".equalsIgnoreCase(
                                                    leave.getLeaveType()
                                            )
                                    )

                                    .mapToDouble(leave ->
                                            leave.getLeaveDuration() != null
                                                    ? leave.getLeaveDuration()
                                                    : 1.0
                                    )

                                    .sum();

                    // =========================================
                    // HALF DAY
                    // =========================================

                    double halfDay =
                            monthlyLeaves.stream()

                                    .filter(leave ->
                                            leave.getLeaveDuration() != null
                                                    &&
                                            Double.compare(
                                                    leave.getLeaveDuration(),
                                                    0.5
                                            ) == 0
                                    )

                                    .mapToDouble(
                                            LeaveRequest::getLeaveDuration
                                    )

                                    .sum();

                    // =========================================
                    // PERMISSION
                    // =========================================

                    int permission =
                            (int) monthlyLeaves.stream()

                                    .filter(leave ->
                                            "PERMISSION".equalsIgnoreCase(
                                                    leave.getLeaveType()
                                            )
                                    )

                                    .count();

                    // =========================================
                    // WORKING DAYS
                    // =========================================

                    int workingDays = 26;

                    // =========================================
                    // ABSENT DAYS
                    // =========================================

                    double absent =
                            Math.max(
                                    0,
                                    workingDays
                                            - presentDays
                                            - sickLeave
                                            - casualLeave
                            );

                    // =========================================
                    // MONTHLY RESPONSE
                    // =========================================

                    return new MonthlyAttendanceResponse(
                            employee.getId(),
                            employee.getEmployeeCode(),
                            employee.getName(),
                            presentDays,
                            sickLeave,
                            casualLeave,
                            halfDay,
                            permission,
                            absent,
                            workingDays
                    );

                })

                .toList();
    }
}