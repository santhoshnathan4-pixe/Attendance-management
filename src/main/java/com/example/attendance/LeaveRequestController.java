package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/leave")
public class LeaveRequestController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveRequestController(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository) {

        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
    }

    // =====================================================
    // APPLY LEAVE
    // =====================================================

    @PostMapping("/apply")
    public String applyLeave(
            @RequestParam String email,
            @RequestParam String leaveType,
            @RequestParam String leaveDate,
            @RequestParam(required = false) String permissionStart,
            @RequestParam(required = false) String permissionEnd,
            @RequestParam(required = false) String halfDaySession,
            @RequestParam(required = false) String reason) {

        // =================================================
        // FIND EMPLOYEE
        // =================================================

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElse(null);

        if (employee == null) {
            return "Employee Not Found";
        }

        // =================================================
        // PARSE DATE
        // =================================================

        LocalDate date;

        try {
            date = LocalDate.parse(leaveDate);
        } catch (Exception e) {
            return "Invalid Leave Date";
        }

        // =================================================
        // LEAVE TYPE
        // =================================================

        if (leaveType == null || leaveType.isBlank()) {
            return "Invalid Leave Type";
        }

        String type = leaveType.toUpperCase();

        // =================================================
        // SAME DATE VALIDATION
        // =================================================

        List<LeaveRequest> sameDateRequests =
                leaveRequestRepository
                        .findByEmployeeIdAndLeaveDate(
                                employee.getId(),
                                date
                        );

        // Permission is separate from leave.
        // SICK / CASUAL / HALF DAY only.
        if (!type.equals("PERMISSION")) {

            for (LeaveRequest existing : sameDateRequests) {

                String existingType =
                        existing.getLeaveType();

                // Ignore permission
                if (existingType == null ||
                        existingType.equalsIgnoreCase("PERMISSION")) {
                    continue;
                }

                double existingDuration =
                        existing.getLeaveDuration() != null
                                ? existing.getLeaveDuration()
                                : 1.0;

                // -----------------------------------------
                // NEW FULL DAY
                // -----------------------------------------

                if (type.equals("SICK") ||
                        type.equals("CASUAL")) {

                    return "Leave Already Applied For This Date";
                }

                // -----------------------------------------
                // NEW HALF DAY
                // -----------------------------------------

                if (type.equals("SICK_HALF") ||
                        type.equals("CASUAL_HALF")) {

                    // Existing full day
                    if (existingDuration >= 1.0) {
                        return "Full Day Leave Already Applied For This Date";
                    }

                    // Existing half day
                    if (existingDuration == 0.5) {

                        String existingSession =
                                existing.getHalfDaySession();

                        // Same session
                        if (existingSession != null &&
                                halfDaySession != null &&
                                existingSession.equalsIgnoreCase(
                                        halfDaySession
                                )) {

                            return "This Half Day Session Is Already Applied";
                        }

                        // Different session allowed
                    }
                }
            }
        }

        // =================================================
        // MONTH RANGE
        // =================================================

        LocalDate monthStart =
                date.withDayOfMonth(1);

        LocalDate monthEnd =
                date.withDayOfMonth(
                        date.lengthOfMonth()
                );

        // =================================================
        // SICK LEAVE
        // =================================================

        if (type.equals("SICK")) {

            double used =
                    getTotalLeaveDaysIncludingPending(
                            employee.getId(),
                            "SICK",
                            monthStart,
                            monthEnd
                    );

            if (used >= 1.0) {
                return "Sick Leave Limit Reached";
            }
        }

        // =================================================
        // CASUAL LEAVE
        // =================================================

        else if (type.equals("CASUAL")) {

            double used =
                    getTotalLeaveDaysIncludingPending(
                            employee.getId(),
                            "CASUAL",
                            monthStart,
                            monthEnd
                    );

            if (used >= 1.0) {
                return "Casual Leave Limit Reached";
            }
        }

        // =================================================
        // SICK HALF DAY
        // =================================================

        else if (type.equals("SICK_HALF")) {

            double used =
                    getTotalLeaveDaysIncludingPending(
                            employee.getId(),
                            "SICK",
                            monthStart,
                            monthEnd
                    );

            if (used >= 1.0) {
                return "Sick Leave Limit Reached";
            }

            if (halfDaySession == null ||
                    halfDaySession.isBlank()) {

                return "Please Select Morning or Afternoon";
            }

            if (!halfDaySession.equalsIgnoreCase("MORNING")
                    &&
                    !halfDaySession.equalsIgnoreCase("AFTERNOON")) {

                return "Invalid Half Day Session";
            }
        }

        // =================================================
        // CASUAL HALF DAY
        // =================================================

        else if (type.equals("CASUAL_HALF")) {

            double used =
                    getTotalLeaveDaysIncludingPending(
                            employee.getId(),
                            "CASUAL",
                            monthStart,
                            monthEnd
                    );

            if (used >= 1.0) {
                return "Casual Leave Limit Reached";
            }

            if (halfDaySession == null ||
                    halfDaySession.isBlank()) {

                return "Please Select Morning or Afternoon";
            }

            if (!halfDaySession.equalsIgnoreCase("MORNING")
                    &&
                    !halfDaySession.equalsIgnoreCase("AFTERNOON")) {

                return "Invalid Half Day Session";
            }
        }

        // =================================================
        // PERMISSION
        // =================================================

        else if (type.equals("PERMISSION")) {

            long count =
                    countPermissionIncludingPending(
                            employee.getId(),
                            monthStart,
                            monthEnd
                    );

            if (count >= 2) {
                return "Monthly Permission Limit Reached";
            }

            if (permissionStart == null ||
                    permissionEnd == null) {

                return "Permission Start and End Time Required";
            }

            LocalTime start;
            LocalTime end;

            try {

                start =
                        LocalTime.parse(permissionStart);

                end =
                        LocalTime.parse(permissionEnd);

            } catch (Exception e) {

                return "Invalid Permission Time";
            }

            if (!end.isAfter(start)) {
                return "Permission End Time Must Be After Start Time";
            }

            long minutes =
                    Duration
                            .between(start, end)
                            .toMinutes();

            if (minutes > 90) {
                return "Permission Maximum Is 1 Hour 30 Minutes";
            }
        }

        // =================================================
        // INVALID TYPE
        // =================================================

        else {

            return "Invalid Leave Type";
        }

        // =================================================
        // CREATE REQUEST
        // =================================================

        LeaveRequest request =
                new LeaveRequest();

        request.setEmployeeId(
                employee.getId()
        );

        // =================================================
        // SET LEAVE TYPE & DURATION
        // =================================================

        if (type.equals("SICK_HALF")) {

            request.setLeaveType("SICK");
            request.setLeaveDuration(0.5);

        } else if (type.equals("CASUAL_HALF")) {

            request.setLeaveType("CASUAL");
            request.setLeaveDuration(0.5);

        } else {

            request.setLeaveType(type);
            request.setLeaveDuration(1.0);
        }

        // =================================================
        // DATE
        // =================================================

        request.setLeaveDate(date);

        // =================================================
        // HALF DAY SESSION
        // =================================================

        if (type.equals("SICK_HALF")
                ||
                type.equals("CASUAL_HALF")) {

            request.setHalfDaySession(
                    halfDaySession.toUpperCase()
            );

        } else {

            request.setHalfDaySession(null);
        }

        // =================================================
        // PERMISSION TIME
        // =================================================

        if (type.equals("PERMISSION")) {

            request.setPermissionStart(
                    LocalTime.parse(permissionStart)
            );

            request.setPermissionEnd(
                    LocalTime.parse(permissionEnd)
            );

        } else {

            request.setPermissionStart(null);
            request.setPermissionEnd(null);
        }

        // =================================================
        // REASON
        // =================================================

        request.setReason(reason);

        // =================================================
        // STATUS
        // =================================================

        request.setStatus("PENDING");

        // =================================================
        // CREATED TIME
        // =================================================

        request.setCreatedAt(
                java.time.LocalDateTime.now()
        );

        // =================================================
        // SAVE
        // =================================================

        leaveRequestRepository.save(request);

        return "Leave Request Submitted Successfully";
    }


    // =====================================================
    // TOTAL LEAVE DAYS
    // APPROVED + PENDING
    // Used only for preventing limit bypass during apply.
    // =====================================================

    private double getTotalLeaveDaysIncludingPending(
            Integer employeeId,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate) {

        List<LeaveRequest> requests =
                leaveRequestRepository
                        .findByEmployeeIdAndLeaveTypeAndLeaveDateBetween(
                                employeeId,
                                leaveType,
                                startDate,
                                endDate
                        );

        double total = 0;

        for (LeaveRequest request : requests) {

            String status =
                    request.getStatus();

            // Count APPROVED and PENDING.
            // REJECTED should not block new applications.
            if (status != null &&
                    (status.equalsIgnoreCase("APPROVED")
                            ||
                     status.equalsIgnoreCase("PENDING"))) {

                if (request.getLeaveDuration() != null) {

                    total +=
                            request.getLeaveDuration();

                } else {

                    total += 1.0;
                }
            }
        }

        return total;
    }


    // =====================================================
    // PERMISSION COUNT
    // APPROVED + PENDING
    // =====================================================

    private long countPermissionIncludingPending(
            Integer employeeId,
            LocalDate startDate,
            LocalDate endDate) {

        List<LeaveRequest> requests =
                leaveRequestRepository
                        .findByEmployeeIdAndLeaveTypeAndLeaveDateBetween(
                                employeeId,
                                "PERMISSION",
                                startDate,
                                endDate
                        );

        long count = 0;

        for (LeaveRequest request : requests) {

            String status =
                    request.getStatus();

            if (status != null &&
                    (status.equalsIgnoreCase("APPROVED")
                            ||
                     status.equalsIgnoreCase("PENDING"))) {

                count++;
            }
        }

        return count;
    }


    // =====================================================
    // LEAVE HISTORY
    // =====================================================

    @GetMapping("/history")
    public List<LeaveRequest> getLeaveHistory(
            @RequestParam String email) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElse(null);

        if (employee == null) {
            return List.of();
        }

        return leaveRequestRepository
                .findByEmployeeIdOrderByLeaveDateDesc(
                        employee.getId()
                );
    }


    // =====================================================
    // LEAVE BALANCE
    // APPROVED ONLY
    // =====================================================

    @GetMapping("/balance")
    public LeaveBalanceResponse getLeaveBalance(
            @RequestParam String email) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElse(null);

        if (employee == null) {

            return new LeaveBalanceResponse(
                    0,
                    0,
                    0
            );
        }

        LocalDate today =
                LocalDate.now();

        LocalDate monthStart =
                today.withDayOfMonth(1);

        LocalDate monthEnd =
                today.withDayOfMonth(
                        today.lengthOfMonth()
                );

        // =================================================
        // APPROVED SICK LEAVE ONLY
        // =================================================

        double sickUsed =
                getApprovedLeaveDays(
                        employee.getId(),
                        "SICK",
                        monthStart,
                        monthEnd
                );

        // =================================================
        // APPROVED CASUAL LEAVE ONLY
        // =================================================

        double casualUsed =
                getApprovedLeaveDays(
                        employee.getId(),
                        "CASUAL",
                        monthStart,
                        monthEnd
                );

        // =================================================
        // APPROVED PERMISSION ONLY
        // =================================================

        long permissionUsed =
                countApprovedPermissions(
                        employee.getId(),
                        monthStart,
                        monthEnd
                );

        return new LeaveBalanceResponse(
                sickUsed,
                casualUsed,
                permissionUsed
        );
    }


    // =====================================================
    // APPROVED LEAVE DAYS
    // =====================================================

    private double getApprovedLeaveDays(
            Integer employeeId,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate) {

        List<LeaveRequest> requests =
                leaveRequestRepository
                        .findByEmployeeIdAndLeaveTypeAndLeaveDateBetween(
                                employeeId,
                                leaveType,
                                startDate,
                                endDate
                        );

        double total = 0;

        for (LeaveRequest request : requests) {

            // ONLY APPROVED
            if (request.getStatus() != null &&
                    request.getStatus()
                            .equalsIgnoreCase("APPROVED")) {

                if (request.getLeaveDuration() != null) {

                    total +=
                            request.getLeaveDuration();

                } else {

                    total += 1.0;
                }
            }
        }

        return total;
    }


    // =====================================================
    // APPROVED PERMISSION COUNT
    // =====================================================

    private long countApprovedPermissions(
            Integer employeeId,
            LocalDate startDate,
            LocalDate endDate) {

        List<LeaveRequest> requests =
                leaveRequestRepository
                        .findByEmployeeIdAndLeaveTypeAndLeaveDateBetween(
                                employeeId,
                                "PERMISSION",
                                startDate,
                                endDate
                        );

        long count = 0;

        for (LeaveRequest request : requests) {

            // ONLY APPROVED
            if (request.getStatus() != null &&
                    request.getStatus()
                            .equalsIgnoreCase("APPROVED")) {

                count++;
            }
        }

        return count;
    }
}