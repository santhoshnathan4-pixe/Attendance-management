package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/leave")
public class LeaveRequestController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeavePermissionSettingRepository settingRepository;
    private final EmployeeLeaveBalanceRepository balanceRepository;

    public LeaveRequestController(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            LeavePermissionSettingRepository settingRepository,
            EmployeeLeaveBalanceRepository balanceRepository) {

        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.settingRepository = settingRepository;
        this.balanceRepository = balanceRepository;
    }

    // =====================================================
    // APPLY LEAVE
    // =====================================================

    @PostMapping("/apply")
    public String applyLeave(
            @RequestParam String email,
            @RequestParam String leaveType,
            @RequestParam String leaveDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String permissionStart,
            @RequestParam(required = false) String permissionEnd,
            @RequestParam(required = false) String halfDaySession,
            @RequestParam(required = false) String reason) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElse(null);

        if (employee == null) {
            return "Employee Not Found";
        }

        LocalDate startDate;

        try {
            startDate = LocalDate.parse(leaveDate);
        } catch (Exception e) {
            return "Invalid Leave Date";
        }

        LocalDate finalDate = startDate;

        if (endDate != null && !endDate.isBlank()) {

            try {
                finalDate = LocalDate.parse(endDate);
            } catch (Exception e) {
                return "Invalid End Date";
            }

            if (finalDate.isBefore(startDate)) {
                return "End Date Cannot Be Before Start Date";
            }
        }

        if (leaveType == null || leaveType.isBlank()) {
            return "Invalid Leave Type";
        }

        String type =
                leaveType.trim().toUpperCase();

        // =================================================
        // PERMISSION
        // =================================================

        if (type.equals("PERMISSION")) {

            if (!startDate.equals(finalDate)) {
                return "Permission Can Be Applied For One Date Only";
            }

            return applyPermission(
                    employee,
                    startDate,
                    permissionStart,
                    permissionEnd,
                    reason
            );
        }

        // =================================================
        // HALF DAY
        // =================================================

        if (type.equals("SICK_HALF") ||
                type.equals("CASUAL_HALF")) {

            if (!startDate.equals(finalDate)) {
                return "Half Day Leave Can Be Applied For One Date Only";
            }

            return applyHalfDayLeave(
                    employee,
                    startDate,
                    type,
                    halfDaySession,
                    reason
            );
        }

        // =================================================
        // FULL DAY / MULTI-DAY
        // =================================================

        if (!type.equals("SICK") &&
                !type.equals("CASUAL")) {

            return "Invalid Leave Type";
        }

        return applyCombinedLeave(
                employee,
                startDate,
                finalDate,
                reason
        );
    }

    // =====================================================
    // COMBINED LEAVE
    //
    // AVAILABLE BALANCE:
    // SICK -> CASUAL -> LOP
    // =====================================================

    private String applyCombinedLeave(
            Employee employee,
            LocalDate startDate,
            LocalDate finalDate,
            String reason) {

        // =================================================
        // FIRST CHECK ALL DATES
        // =================================================

        LocalDate checkDate = startDate;

        while (!checkDate.isAfter(finalDate)) {

            List<LeaveRequest> sameDateRequests =
                    leaveRequestRepository
                            .findByEmployeeIdAndLeaveDate(
                                    employee.getId(),
                                    checkDate
                            );

            for (LeaveRequest existing : sameDateRequests) {

                String existingType =
                        existing.getLeaveType();

                if (existingType == null) {
                    continue;
                }

                // Permission is independent from leave.
                if (existingType.equalsIgnoreCase("PERMISSION")) {
                    continue;
                }

                if (existing.getStatus() != null &&
                        existing.getStatus()
                                .equalsIgnoreCase("REJECTED")) {
                    continue;
                }

                return "Leave Already Applied For Date: "
                        + checkDate;
            }

            checkDate = checkDate.plusDays(1);
        }

        // =================================================
        // PROCESS EACH DATE
        // =================================================

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(finalDate)) {

            LocalDate month =
                    currentDate.withDayOfMonth(1);

            EmployeeLeaveBalance balance =
                    getOrCreateBalance(
                            employee,
                            month
                    );

            double sickAvailable =
                    balance.getSickBalance() != null
                            ? Math.max(
                                    0,
                                    balance.getSickBalance()
                            )
                            : 0;

            double casualAvailable =
                    balance.getCasualBalance() != null
                            ? Math.max(
                                    0,
                                    balance.getCasualBalance()
                            )
                            : 0;

            double sickUsed = 0;
            double casualUsed = 0;
            double lop = 0;

            if (sickAvailable >= 1.0) {

                sickUsed = 1.0;

            } else if (sickAvailable > 0) {

                sickUsed = sickAvailable;
            }

            double remainingDay =
                    1.0 - sickUsed;

            if (remainingDay > 0 &&
                    casualAvailable > 0) {

                casualUsed =
                        Math.min(
                                remainingDay,
                                casualAvailable
                        );
            }

            remainingDay =
                    1.0
                            - sickUsed
                            - casualUsed;

            if (remainingDay > 0) {
                lop = remainingDay;
            }

            // =================================================
            // SAVE SICK
            // =================================================

            if (sickUsed > 0) {

                LeaveRequest sickRequest =
                        createLeaveRequest(
                                employee.getId(),
                                "SICK",
                                currentDate,
                                sickUsed,
                                0.0,
                                reason
                        );

                leaveRequestRepository.save(
                        sickRequest
                );

                balance.setSickBalance(
                        Math.max(
                                0,
                                sickAvailable - sickUsed
                        )
                );
            }

            // =================================================
            // SAVE CASUAL
            // =================================================

            if (casualUsed > 0) {

                LeaveRequest casualRequest =
                        createLeaveRequest(
                                employee.getId(),
                                "CASUAL",
                                currentDate,
                                casualUsed,
                                0.0,
                                reason
                        );

                leaveRequestRepository.save(
                        casualRequest
                );

                balance.setCasualBalance(
                        Math.max(
                                0,
                                casualAvailable - casualUsed
                        )
                );
            }

            // =================================================
            // SAVE LOP
            // =================================================

            if (lop > 0) {

                LeaveRequest lopRequest =
                        createLeaveRequest(
                                employee.getId(),
                                "LOP",
                                currentDate,
                                lop,
                                lop,
                                reason
                        );

                leaveRequestRepository.save(
                        lopRequest
                );
            }

            balance.setUpdatedAt(
                    LocalDateTime.now()
            );

            balanceRepository.save(balance);

            currentDate =
                    currentDate.plusDays(1);
        }

        return "Leave Request Submitted Successfully";
    }

    // =====================================================
    // CREATE LEAVE REQUEST
    // =====================================================

    private LeaveRequest createLeaveRequest(
            Integer employeeId,
            String leaveType,
            LocalDate date,
            double duration,
            double lopDays,
            String reason) {

        LeaveRequest request =
                new LeaveRequest();

        request.setEmployeeId(employeeId);
        request.setLeaveType(leaveType);
        request.setLeaveDate(date);
        request.setLeaveDuration(duration);
        request.setLopDays(lopDays);

        request.setHalfDaySession(null);
        request.setPermissionStart(null);
        request.setPermissionEnd(null);

        request.setReason(reason);
        request.setStatus("PENDING");

        request.setCreatedAt(
                LocalDateTime.now()
        );

        return request;
    }

    // =====================================================
    // HALF DAY LEAVE
    // =====================================================

    private String applyHalfDayLeave(
            Employee employee,
            LocalDate date,
            String type,
            String halfDaySession,
            String reason) {

        if (halfDaySession == null ||
                halfDaySession.isBlank()) {

            return "Please Select Morning or Afternoon";
        }

        if (!halfDaySession.equalsIgnoreCase("MORNING") &&
                !halfDaySession.equalsIgnoreCase("AFTERNOON")) {

            return "Invalid Half Day Session";
        }

        List<LeaveRequest> sameDateRequests =
                leaveRequestRepository
                        .findByEmployeeIdAndLeaveDate(
                                employee.getId(),
                                date
                        );

        for (LeaveRequest existing : sameDateRequests) {

            String existingType =
                    existing.getLeaveType();

            if (existingType == null ||
                    existingType.equalsIgnoreCase("PERMISSION")) {
                continue;
            }

            if (existing.getStatus() != null &&
                    existing.getStatus()
                            .equalsIgnoreCase("REJECTED")) {
                continue;
            }

            double existingDuration =
                    existing.getLeaveDuration() != null
                            ? existing.getLeaveDuration()
                            : 1.0;

            if (existingDuration >= 1.0) {
                return "Full Day Leave Already Applied For This Date";
            }

            if (existingDuration == 0.5) {

                String existingSession =
                        existing.getHalfDaySession();

                if (existingSession != null &&
                        existingSession.equalsIgnoreCase(
                                halfDaySession
                        )) {

                    return "This Half Day Session Is Already Applied";
                }
            }
        }

        String actualType =
                type.equals("SICK_HALF")
                        ? "SICK"
                        : "CASUAL";

        EmployeeLeaveBalance balance =
                getOrCreateBalance(
                        employee,
                        date.withDayOfMonth(1)
                );

        if (actualType.equals("SICK")) {

            double sickBalance =
                    balance.getSickBalance() != null
                            ? balance.getSickBalance()
                            : 0;

            if (sickBalance < 0.5) {
                return "Sick Leave Balance Not Available";
            }

            balance.setSickBalance(
                    Math.max(
                            0,
                            sickBalance - 0.5
                    )
            );

        } else {

            double casualBalance =
                    balance.getCasualBalance() != null
                            ? balance.getCasualBalance()
                            : 0;

            if (casualBalance < 0.5) {
                return "Casual Leave Balance Not Available";
            }

            balance.setCasualBalance(
                    Math.max(
                            0,
                            casualBalance - 0.5
                    )
            );
        }

        LeaveRequest request =
                createLeaveRequest(
                        employee.getId(),
                        actualType,
                        date,
                        0.5,
                        0.0,
                        reason
                );

        request.setHalfDaySession(
                halfDaySession.toUpperCase()
        );

        leaveRequestRepository.save(request);

        balance.setUpdatedAt(
                LocalDateTime.now()
        );

        balanceRepository.save(balance);

        return "Leave Request Submitted Successfully";
    }

    // =====================================================
    // PERMISSION
    //
    // RULES:
    //
    // 1. One permission maximum = 90 minutes FIXED.
    // 2. Monthly permission COUNT comes from Admin Setting.
    // 3. Monthly permission HOURS comes from Admin Setting.
    // 4. Count includes APPROVED + PENDING.
    // 5. Hours are calculated from actual start/end time.
    // 6. Hours exceeding the configured allowance are allowed
    //    and will be handled as LOP during salary calculation.
    // =====================================================

    private String applyPermission(
            Employee employee,
            LocalDate date,
            String permissionStart,
            String permissionEnd,
            String reason) {

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

        // =================================================
        // FIXED MAXIMUM PER PERMISSION
        // =================================================

        if (minutes > 90) {
            return "Permission Maximum Is 1 Hour 30 Minutes";
        }

        if (minutes <= 0) {
            return "Permission Duration Must Be Greater Than 0";
        }

        LocalDate month =
                date.withDayOfMonth(1);

        EmployeeLeaveBalance balance =
                getOrCreateBalance(
                        employee,
                        month
                );

        // =================================================
        // ADMIN CONFIGURED PERMISSION COUNT
        // =================================================

        long permissionUsed =
                countPermissionIncludingPending(
                        employee.getId(),
                        month,
                        month.withDayOfMonth(
                                month.lengthOfMonth()
                        )
                );

        int permissionLimit =
                balance.getPermissionBalance() != null
                        ? Math.max(
                                0,
                                balance.getPermissionBalance()
                        )
                        : 2;

        if (permissionUsed >= permissionLimit) {
            return "Monthly Permission Limit Reached";
        }

        // =================================================
        // ADMIN CONFIGURED TOTAL PERMISSION HOURS
        //
        // IMPORTANT:
        //
        // We DO NOT reject when hours exceed the allowance.
        //
        // The permission is still recorded.
        // Salary calculation will later deduct ONLY
        // the excess hours.
        // =================================================

        LeavePermissionSetting setting =
                getEffectiveSetting(employee);

        double allowedPermissionHours =
                setting.getPermissionHours() != null
                        ? Math.max(
                                0,
                                setting.getPermissionHours()
                        )
                        : 3.0;

        long usedPermissionMinutes =
                getPermissionMinutesIncludingPending(
                        employee.getId(),
                        month,
                        month.withDayOfMonth(
                                month.lengthOfMonth()
                        )
                );

        long allowedPermissionMinutes =
                Math.round(
                        allowedPermissionHours * 60.0
                );

        /*
         * Existing usage + this request is allowed even if
         * it exceeds the configured hours.
         *
         * Excess hours are calculated later during salary
         * calculation.
         */
        long totalAfterRequest =
                usedPermissionMinutes + minutes;

        /*
         * Keep this variable intentionally calculated here
         * so the configured policy is evaluated for this
         * permission request.
         *
         * No rejection is performed for excess hours.
         */
        boolean exceedsAllowedHours =
                totalAfterRequest > allowedPermissionMinutes;

        // =================================================
        // SAME DATE PERMISSION CHECK
        // =================================================

        List<LeaveRequest> sameDateRequests =
                leaveRequestRepository
                        .findByEmployeeIdAndLeaveDate(
                                employee.getId(),
                                date
                        );

        for (LeaveRequest existing : sameDateRequests) {

            if (existing.getLeaveType() != null &&
                    existing.getLeaveType()
                            .equalsIgnoreCase("PERMISSION") &&
                    existing.getStatus() != null &&
                    !existing.getStatus()
                            .equalsIgnoreCase("REJECTED")) {

                return "Permission Already Applied For This Date";
            }
        }

        // =================================================
        // SAVE PERMISSION
        // =================================================

        LeaveRequest request =
                createLeaveRequest(
                        employee.getId(),
                        "PERMISSION",
                        date,
                        1.0,
                        0.0,
                        reason
                );

        request.setPermissionStart(start);
        request.setPermissionEnd(end);

        leaveRequestRepository.save(request);

        return "Permission Request Submitted Successfully";
    }

    // =====================================================
    // GET OR CREATE MONTHLY BALANCE
    // =====================================================

    private EmployeeLeaveBalance getOrCreateBalance(
            Employee employee,
            LocalDate month) {

        LocalDate firstDay =
                month.withDayOfMonth(1);

        var existing =
                balanceRepository
                        .findByEmployeeIdAndBalanceMonth(
                                employee.getId(),
                                firstDay
                        );

        if (existing.isPresent()) {
            return existing.get();
        }

        LeavePermissionSetting setting =
                getEffectiveSetting(employee);

        double sickAllowance =
                setting.getSickLeave() != null
                        ? Math.max(
                                0,
                                setting.getSickLeave()
                        )
                        : 1.0;

        double casualAllowance =
                setting.getCasualLeave() != null
                        ? Math.max(
                                0,
                                setting.getCasualLeave()
                        )
                        : 1.0;

        int permissionAllowance =
                setting.getPermissionCount() != null
                        ? Math.max(
                                0,
                                setting.getPermissionCount()
                        )
                        : 2;

        double sickBalance =
                sickAllowance;

        double casualBalance =
                casualAllowance;

        // =================================================
        // CARRY FORWARD
        // =================================================

        LocalDate previousMonth =
                firstDay.minusMonths(1);

        var previous =
                balanceRepository
                        .findByEmployeeIdAndBalanceMonth(
                                employee.getId(),
                                previousMonth
                        );

        if (previous.isPresent()) {

            EmployeeLeaveBalance previousBalance =
                    previous.get();

            if (previousBalance.getSickBalance() != null) {

                sickBalance +=
                        Math.max(
                                0,
                                previousBalance.getSickBalance()
                        );
            }

            if (previousBalance.getCasualBalance() != null) {

                casualBalance +=
                        Math.max(
                                0,
                                previousBalance.getCasualBalance()
                        );
            }
        }

        EmployeeLeaveBalance balance =
                new EmployeeLeaveBalance();

        balance.setEmployeeId(
                employee.getId()
        );

        balance.setBalanceMonth(
                firstDay
        );

        balance.setSickBalance(
                sickBalance
        );

        balance.setCasualBalance(
                casualBalance
        );

        balance.setPermissionBalance(
                permissionAllowance
        );

        LocalDateTime now =
                LocalDateTime.now();

        balance.setCreatedAt(now);
        balance.setUpdatedAt(now);

        return balanceRepository.save(balance);
    }

    // =====================================================
    // EFFECTIVE SETTING
    //
    // EMPLOYEE > ROLE > DEFAULT
    // =====================================================

    private LeavePermissionSetting getEffectiveSetting(
            Employee employee) {

        var employeeSetting =
                settingRepository
                        .findBySettingTypeAndEmployeeId(
                                "EMPLOYEE",
                                employee.getId()
                        );

        if (employeeSetting.isPresent()) {
            return employeeSetting.get();
        }

        if (employee.getRole() != null &&
                !employee.getRole().isBlank()) {

            var roleSetting =
                    settingRepository
                            .findBySettingTypeAndRole(
                                    "ROLE",
                                    employee.getRole()
                                            .trim()
                                            .toUpperCase()
                            );

            if (roleSetting.isPresent()) {
                return roleSetting.get();
            }
        }

        return settingRepository
                .findBySettingType("DEFAULT")
                .orElseGet(() -> {

                    LeavePermissionSetting defaultSetting =
                            new LeavePermissionSetting();

                    defaultSetting.setSettingType(
                            "DEFAULT"
                    );

                    defaultSetting.setSickLeave(1.0);
                    defaultSetting.setCasualLeave(1.0);
                    defaultSetting.setPermissionCount(2);
                    defaultSetting.setPermissionHours(3.0);

                    LocalDateTime now =
                            LocalDateTime.now();

                    defaultSetting.setCreatedAt(now);
                    defaultSetting.setUpdatedAt(now);

                    return settingRepository.save(
                            defaultSetting
                    );
                });
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
    // PERMISSION HOURS
    // APPROVED + PENDING
    //
    // Actual duration comes from:
    // permissionStart -> permissionEnd
    // =====================================================

    private long getPermissionMinutesIncludingPending(
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

        long totalMinutes = 0;

        for (LeaveRequest request : requests) {

            if (request == null) {
                continue;
            }

            String status =
                    request.getStatus();

            if (status == null ||
                    (!status.equalsIgnoreCase("APPROVED")
                            &&
                     !status.equalsIgnoreCase("PENDING"))) {

                continue;
            }

            LocalTime start =
                    request.getPermissionStart();

            LocalTime end =
                    request.getPermissionEnd();

            if (start == null || end == null) {
                continue;
            }

            if (!end.isAfter(start)) {
                continue;
            }

            totalMinutes +=
                    Duration
                            .between(start, end)
                            .toMinutes();
        }

        return totalMinutes;
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
    // ACTUAL MONTHLY BALANCE
    // =====================================================

    @GetMapping("/balance")
    public EmployeeLeaveBalanceResponse getLeaveBalance(
            @RequestParam String email) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElse(null);

        if (employee == null) {

            return new EmployeeLeaveBalanceResponse(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        LocalDate today =
                LocalDate.now();

        LocalDate monthStart =
                today.withDayOfMonth(1);

        EmployeeLeaveBalance balance =
                getOrCreateBalance(
                        employee,
                        monthStart
                );

        double sickRemaining =
                balance.getSickBalance() != null
                        ? Math.max(
                                0,
                                balance.getSickBalance()
                        )
                        : 0;

        double casualRemaining =
                balance.getCasualBalance() != null
                        ? Math.max(
                                0,
                                balance.getCasualBalance()
                        )
                        : 0;

        long permissionRemaining =
                balance.getPermissionBalance() != null
                        ? Math.max(
                                0,
                                balance.getPermissionBalance()
                        )
                        : 0;

        LeavePermissionSetting setting =
                getEffectiveSetting(employee);

        double sickAllowance =
                setting.getSickLeave() != null
                        ? Math.max(
                                0,
                                setting.getSickLeave()
                        )
                        : 1.0;

        double casualAllowance =
                setting.getCasualLeave() != null
                        ? Math.max(
                                0,
                                setting.getCasualLeave()
                        )
                        : 1.0;

        int permissionAllowance =
                setting.getPermissionCount() != null
                        ? Math.max(
                                0,
                                setting.getPermissionCount()
                        )
                        : 2;

        double previousSickCarry =
                getPreviousSickCarry(
                        employee,
                        monthStart
                );

        double previousCasualCarry =
                getPreviousCasualCarry(
                        employee,
                        monthStart
                );

        double totalSickAvailable =
                sickAllowance
                        + previousSickCarry;

        double totalCasualAvailable =
                casualAllowance
                        + previousCasualCarry;

        double sickUsed =
                Math.max(
                        0,
                        totalSickAvailable - sickRemaining
                );

        double casualUsed =
                Math.max(
                        0,
                        totalCasualAvailable - casualRemaining
                );

        long permissionUsed =
                Math.max(
                        0,
                        permissionAllowance
                                - permissionRemaining
                );

        return new EmployeeLeaveBalanceResponse(
                sickUsed,
                sickRemaining,
                casualUsed,
                casualRemaining,
                permissionUsed,
                permissionRemaining
        );
    }

    // =====================================================
    // PREVIOUS SICK CARRY
    // =====================================================

    private double getPreviousSickCarry(
            Employee employee,
            LocalDate currentMonth) {

        LocalDate previousMonth =
                currentMonth.minusMonths(1);

        return balanceRepository
                .findByEmployeeIdAndBalanceMonth(
                        employee.getId(),
                        previousMonth
                )
                .map(balance ->
                        balance.getSickBalance() != null
                                ? Math.max(
                                        0,
                                        balance.getSickBalance()
                                )
                                : 0
                )
                .orElse(0.0);
    }

    // =====================================================
    // PREVIOUS CASUAL CARRY
    // =====================================================

    private double getPreviousCasualCarry(
            Employee employee,
            LocalDate currentMonth) {

        LocalDate previousMonth =
                currentMonth.minusMonths(1);

        return balanceRepository
                .findByEmployeeIdAndBalanceMonth(
                        employee.getId(),
                        previousMonth
                )
                .map(balance ->
                        balance.getCasualBalance() != null
                                ? Math.max(
                                        0,
                                        balance.getCasualBalance()
                                )
                                : 0
                )
                .orElse(0.0);
    }
}