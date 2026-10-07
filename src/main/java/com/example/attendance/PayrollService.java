package com.example.attendance;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PayrollService {

    public static final boolean AUTO_COVER_ABSENCE_WITH_LEAVE_BALANCE = true;

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final WorkingDaySettingRepository workingDaySettingRepository;
    private final WeeklyOffSettingRepository weeklyOffSettingRepository;
    private final LeavePermissionSettingRepository settingRepository;
    private final EmployeeLeaveBalanceRepository balanceRepository;
    private final OvertimeRecordRepository overtimeRecordRepository;

    public PayrollService(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository,
            WorkingDaySettingRepository workingDaySettingRepository,
            WeeklyOffSettingRepository weeklyOffSettingRepository,
            LeavePermissionSettingRepository settingRepository,
            EmployeeLeaveBalanceRepository balanceRepository,
            OvertimeRecordRepository overtimeRecordRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.workingDaySettingRepository = workingDaySettingRepository;
        this.weeklyOffSettingRepository = weeklyOffSettingRepository;
        this.settingRepository = settingRepository;
        this.balanceRepository = balanceRepository;
        this.overtimeRecordRepository = overtimeRecordRepository;
    }


    public record EmployeePayroll(
            Employee employee,
            PayrollMath.Result math,
            double sickLeave,
            double casualLeave,
            double halfDay,
            int permission,
            double lopLeave,
            double absentDays,
            int overtimeDays,
            double overtimeSalary,
            double compensatoryOffDays,
            double finalSalaryWithOvertime) {
    }


    public record MonthPayroll(
            int workingDays,
            List<EmployeePayroll> employees) {
    }


    // =========================================
    // CALCULATE MONTH PAYROLL
    // =========================================

    public MonthPayroll calculate(
            int year,
            int month) {

        YearMonth yearMonth =
                YearMonth.of(year, month);

        LocalDate start =
                yearMonth.atDay(1);

        LocalDate end =
                yearMonth.atEndOfMonth();


        // =========================================
        // LOAD ACTIVE WORKING DAY SETTINGS ONLY
        // =========================================

        Map<LocalDate, String> settingTypes =
                new HashMap<>();

        for (WorkingDaySetting setting :
                workingDaySettingRepository
                        .findBySettingDateBetweenAndActiveTrueOrderBySettingDateAsc(
                                start,
                                end)) {

            if (setting != null &&
                    setting.getSettingDate() != null &&
                    setting.getSettingType() != null) {

                settingTypes.put(
                        setting.getSettingDate(),
                        setting.getSettingType()
                );
            }
        }


        // =========================================
        // LOAD OVERALL RECURRING WEEKLY OFF DAYS
        // =========================================

        List<String> recurringWeeklyOffDays =
                weeklyOffSettingRepository
                        .findByActiveTrueOrderByDayOfWeekAsc()
                        .stream()
                        .filter(setting ->
                                setting != null &&
                                setting.getDayOfWeek() != null
                        )
                        .map(setting ->
                                setting.getDayOfWeek()
                        )
                        .toList();


        // =========================================
        // CALCULATE WORKING DATES
        // =========================================

        List<LocalDate> workingDates =
                WorkingDayCalculator.workingDates(
                        yearMonth,
                        settingTypes,
                        recurringWeeklyOffDays
                );

        Set<LocalDate> workingSet =
                new HashSet<>(
                        workingDates
                );


        // =========================================
        // LOAD ATTENDANCE
        // =========================================

        Map<Integer, Set<LocalDate>> presentByEmployee =
                new HashMap<>();

        for (Attendance attendance :
                attendanceRepository
                        .findByAttendanceDateBetween(
                                start,
                                end
                        )) {

            if (attendance.getEmployeeId() == null ||
                    attendance.getAttendanceDate() == null ||
                    attendance.getCheckIn() == null) {

                continue;
            }

            presentByEmployee
                    .computeIfAbsent(
                            attendance.getEmployeeId(),
                            key -> new HashSet<>()
                    )
                    .add(
                            attendance.getAttendanceDate()
                    );
        }


        // =========================================
        // LOAD LEAVE REQUESTS
        // =========================================

        Map<Integer, List<LeaveRequest>> leavesByEmployee =
                new HashMap<>();

        for (LeaveRequest leave :
                leaveRequestRepository
                        .findByLeaveDateBetween(
                                start,
                                end
                        )) {

            if (leave.getEmployeeId() == null) {
                continue;
            }

            leavesByEmployee
                    .computeIfAbsent(
                            leave.getEmployeeId(),
                            key -> new ArrayList<>()
                    )
                    .add(leave);
        }


        // =========================================
        // LOAD OT RECORDS
        // =========================================

        Map<Integer, List<OvertimeRecord>> overtimeByEmployee =
                new HashMap<>();

        for (OvertimeRecord overtime :
                overtimeRecordRepository
                        .findByOtDateBetweenOrderByOtDateAsc(
                                start,
                                end
                        )) {

            if (overtime == null ||
                    overtime.getEmployeeId() == null) {

                continue;
            }

            overtimeByEmployee
                    .computeIfAbsent(
                            overtime.getEmployeeId(),
                            key -> new ArrayList<>()
                    )
                    .add(overtime);
        }


        // =========================================
        // LOAD ACTIVE NON-ADMIN EMPLOYEES
        // =========================================

        List<Employee> employees =
                employeeRepository.findAll()
                        .stream()
                        .filter(e ->
                                Boolean.TRUE.equals(
                                        e.getActive()
                                )
                        )
                        .filter(e ->
                                !"ADMIN".equalsIgnoreCase(
                                        e.getRole()
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        Employee::getEmployeeCode,
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                        )
                        .toList();


        List<EmployeePayroll> result =
                new ArrayList<>();


        // =========================================
        // CALCULATE EACH EMPLOYEE
        // =========================================

        for (Employee employee : employees) {

            Integer employeeId =
                    employee.getId();

            Set<LocalDate> present =
                    presentByEmployee.getOrDefault(
                            employeeId,
                            Set.of()
                    );

            List<LeaveRequest> leaves =
                    leavesByEmployee.getOrDefault(
                            employeeId,
                            List.of()
                    );


            Map<LocalDate, Double> paidLeaveByDate =
                    new HashMap<>();

            double sick = 0;
            double casual = 0;
            double half = 0;
            double lop = 0;
            int permission = 0;


            // =========================================
            // PROCESS APPROVED LEAVES
            // =========================================

            for (LeaveRequest leave : leaves) {

                if (!"APPROVED".equalsIgnoreCase(
                            leave.getStatus()
                        )
                        || leave.getLeaveType() == null
                        || leave.getLeaveDate() == null) {

                    continue;
                }


                String type =
                        leave.getLeaveType()
                                .trim()
                                .toUpperCase();


                // =========================================
                // PERMISSION
                // =========================================

                if (type.equals("PERMISSION")) {

                    permission++;

                    continue;
                }


                double duration =
                        leave.getLeaveDuration() != null
                                ? leave.getLeaveDuration()
                                : 1.0;


                if (duration <= 0) {
                    continue;
                }


                duration =
                        Math.min(
                                duration,
                                1.0
                        );


                LocalDate date =
                        leave.getLeaveDate();


                // =========================================
                // SICK / CASUAL LEAVE
                // =========================================

                if (type.equals("SICK") ||
                        type.equals("CASUAL")) {

                    if (type.equals("SICK")) {

                        sick += duration;

                    } else {

                        casual += duration;
                    }


                    if (duration == 0.5) {

                        half += 0.5;
                    }


                    if (workingSet.contains(date)) {

                        paidLeaveByDate.merge(
                                date,
                                duration,
                                (a, b) ->
                                        Math.min(
                                                1.0,
                                                a + b
                                        )
                        );
                    }
                }


                // =========================================
                // LOP LEAVE
                // =========================================

                else if (type.equals("LOP")) {

                    if (workingSet.contains(date) &&
                            !present.contains(date)) {

                        lop += duration;
                    }
                }
            }


            // =========================================
            // AUTO COVER ABSENCE
            // =========================================

            double autoCoverCap = 0;


            if (AUTO_COVER_ABSENCE_WITH_LEAVE_BALANCE) {

                LeavePermissionSetting setting =
                        resolveSetting(employee);


                double sickAllowance =
                        setting != null &&
                        setting.getSickLeave() != null
                                ? Math.max(
                                        0,
                                        setting.getSickLeave()
                                )
                                : 1.0;


                double casualAllowance =
                        setting != null &&
                        setting.getCasualLeave() != null
                                ? Math.max(
                                        0,
                                        setting.getCasualLeave()
                                )
                                : 1.0;


                double notUsedYet =
                        Math.max(
                                0,
                                sickAllowance +
                                casualAllowance -
                                sick -
                                casual
                        );


                autoCoverCap =
                        notUsedYet;


                var balance =
                        balanceRepository
                                .findByEmployeeIdAndBalanceMonth(
                                        employeeId,
                                        start
                                );


                if (balance.isPresent()) {

                    double sickLeft =
                            balance.get().getSickBalance() != null
                                    ? Math.max(
                                            0,
                                            balance.get()
                                                    .getSickBalance()
                                    )
                                    : 0;


                    double casualLeft =
                            balance.get().getCasualBalance() != null
                                    ? Math.max(
                                            0,
                                            balance.get()
                                                    .getCasualBalance()
                                    )
                                    : 0;


                    autoCoverCap =
                            Math.min(
                                    autoCoverCap,
                                    sickLeft +
                                    casualLeft
                            );
                }
            }


            // =========================================
            // MONTHLY SALARY
            // =========================================

            double monthlySalary =
                    employee.getSalary() != null
                            ? employee.getSalary()
                            : 0.0;


            // =========================================
            // FINAL PAYROLL MATH
            // =========================================

            PayrollMath.Result math =
                    PayrollMath.calculate(
                            workingDates,
                            present,
                            paidLeaveByDate,
                            autoCoverCap,
                            monthlySalary
                    );


            // =========================================
            // DISPLAY ABSENCE / LOP DETAILS
            // =========================================

            double rawAbsent =
                    math.lopDays() +
                    math.autoCoveredDays();


            double lopShown =
                    Math.min(
                            lop,
                            rawAbsent
                    );


            double absent =
                    PayrollMath.round2(
                            Math.max(
                                    0,
                                    rawAbsent -
                                    lopShown
                            )
                    );


            // =========================================
            // CALCULATE OVERTIME
            // =========================================

            List<OvertimeRecord> overtimeRecords =
                    overtimeByEmployee.getOrDefault(
                            employeeId,
                            List.of()
                    );


            int overtimeDays = 0;

            double overtimeSalary = 0.0;

            double compensatoryOffDays = 0.0;


            /*
             * OT amount is based on the same
             * per-day salary used by payroll.
             *
             * Example:
             *
             * Salary = 25000
             * Working Days = 27
             *
             * Per Day = 925.93
             *
             * One Extra Salary OT = 925.93
             */

            double perDaySalary =
                    math.perDaySalary();


            for (OvertimeRecord overtime :
                    overtimeRecords) {

                if (!Boolean.TRUE.equals(
                        overtime.getOtEligible())) {

                    continue;
                }


                String benefitType =
                        overtime.getBenefitType() != null
                                ? overtime.getBenefitType()
                                        .trim()
                                        .toUpperCase()
                                : "";


                /*
                 * Only APPLIED OT records should
                 * affect payroll.
                 */

                if (!"APPLIED".equalsIgnoreCase(
                        overtime.getStatus())) {

                    continue;
                }


                overtimeDays++;


                // =========================================
                // EXTRA SALARY
                // =========================================

                if ("EXTRA_SALARY".equals(
                        benefitType)) {

                    double amount =
                            perDaySalary;


                    if (overtime.getOtAmount() != null &&
                            overtime.getOtAmount() > 0) {

                        amount =
                                overtime.getOtAmount();
                    }


                    overtimeSalary += amount;
                }


                // =========================================
                // COMPENSATORY OFF
                // =========================================

                else if ("COMP_OFF".equals(
                        benefitType)) {

                    double compOff =
                            overtime.getCompOffDays() != null
                                    ? overtime.getCompOffDays()
                                    : 1.0;


                    compensatoryOffDays +=
                            Math.max(
                                    0,
                                    compOff
                            );
                }
            }


            overtimeSalary =
                    PayrollMath.round2(
                            overtimeSalary
                    );


            compensatoryOffDays =
                    PayrollMath.round2(
                            compensatoryOffDays
                    );


            double finalSalaryWithOvertime =
                    PayrollMath.round2(
                            math.finalSalary() +
                            overtimeSalary
                    );


            // =========================================
            // ADD EMPLOYEE PAYROLL RESULT
            // =========================================

            result.add(
                    new EmployeePayroll(
                            employee,
                            math,
                            PayrollMath.round2(sick),
                            PayrollMath.round2(casual),
                            PayrollMath.round2(half),
                            permission,
                            PayrollMath.round2(lopShown),
                            absent,
                            overtimeDays,
                            overtimeSalary,
                            compensatoryOffDays,
                            finalSalaryWithOvertime
                    )
            );
        }


        return new MonthPayroll(
                workingDates.size(),
                result
        );
    }


    // =========================================
    // RESOLVE LEAVE SETTING
    // =========================================

    private LeavePermissionSetting resolveSetting(
            Employee employee) {

        if (employee.getId() != null) {

            var employeeSetting =
                    settingRepository
                            .findBySettingTypeAndEmployeeId(
                                    "EMPLOYEE",
                                    employee.getId()
                            );

            if (employeeSetting.isPresent()) {

                return employeeSetting.get();
            }
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
                .orElse(null);
    }
}