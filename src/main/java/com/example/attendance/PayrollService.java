package com.example.attendance;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.Duration;
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
        private final ShiftTimeService shiftTimeService;
        private final PayrollAdjustmentRepository adjustmentRepository;
        private final PayrollPeriodService periodService;

        // Shift shortfall rule: allowed shortfall (late + early) in minutes
        private static final long ALLOWED_SHORTFALL_MINUTES = 30;

        private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

        public PayrollService(
                        EmployeeRepository employeeRepository,
                        AttendanceRepository attendanceRepository,
                        LeaveRequestRepository leaveRequestRepository,
                        WorkingDaySettingRepository workingDaySettingRepository,
                        WeeklyOffSettingRepository weeklyOffSettingRepository,
                        LeavePermissionSettingRepository settingRepository,
                        EmployeeLeaveBalanceRepository balanceRepository,
                        OvertimeRecordRepository overtimeRecordRepository,
                        ShiftTimeService shiftTimeService,
                        PayrollAdjustmentRepository adjustmentRepository,
                        PayrollPeriodService periodService) {

                this.periodService = periodService;
                this.adjustmentRepository = adjustmentRepository;
                this.employeeRepository = employeeRepository;
                this.attendanceRepository = attendanceRepository;
                this.leaveRequestRepository = leaveRequestRepository;
                this.workingDaySettingRepository = workingDaySettingRepository;
                this.weeklyOffSettingRepository = weeklyOffSettingRepository;
                this.settingRepository = settingRepository;
                this.balanceRepository = balanceRepository;
                this.overtimeRecordRepository = overtimeRecordRepository;
                this.shiftTimeService = shiftTimeService;
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
                        double finalSalaryWithOvertime,
                        LateInfo late,
                        PayrollAdjustment adjustment) {
        }

        /**
         * Late / early (shift shortfall) details of one employee for the month.
         *
         * deductibleMinutes = short time of the days that crossed the 30 min tolerance
         * toleratedMinutes = short time of the days inside the tolerance (never cut)
         * appliedDeductMinutes = minutes really cut from salary (after the admin decision)
         */
        public record LateInfo(
                        long deductibleMinutes,
                        long toleratedMinutes,
                        int shortDays,
                        long shiftMinutes,
                        long appliedDeductMinutes) {
        }

        private record Shortfall(
                        double days,
                        long deductibleMinutes,
                        long toleratedMinutes,
                        int shortDays,
                        long shiftMinutes) {
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

                YearMonth yearMonth = YearMonth.of(year, month);

                // Salary dates set by the admin (default: whole calendar month)
                PayrollPeriodService.Period period = periodService.resolve(year, month);

                LocalDate start = period.start();

                LocalDate end = period.end();

                // Leave balance rows are kept per calendar month of the pay month
                LocalDate balanceMonth = yearMonth.atDay(1);

                // Working day settings are read for the full months the period touches
                YearMonth firstMonth = YearMonth.from(start);

                YearMonth lastMonth = YearMonth.from(end);

                // =========================================
                // LOAD ACTIVE WORKING DAY SETTINGS ONLY
                // =========================================

                Map<LocalDate, String> settingTypes = new HashMap<>();

                for (WorkingDaySetting setting : workingDaySettingRepository
                                .findBySettingDateBetweenAndActiveTrueOrderBySettingDateAsc(
                                                firstMonth.atDay(1),
                                                lastMonth.atEndOfMonth())) {

                        if (setting != null &&
                                        setting.getSettingDate() != null &&
                                        setting.getSettingType() != null) {

                                settingTypes.put(
                                                setting.getSettingDate(),
                                                setting.getSettingType());
                        }
                }

                // =========================================
                // LOAD OVERALL RECURRING WEEKLY OFF DAYS
                // =========================================

                List<String> recurringWeeklyOffDays = weeklyOffSettingRepository
                                .findByActiveTrueOrderByDayOfWeekAsc()
                                .stream()
                                .filter(setting -> setting != null &&
                                                setting.getDayOfWeek() != null)
                                .map(setting -> setting.getDayOfWeek())
                                .toList();

                // =========================================
                // CALCULATE WORKING DATES
                // =========================================

                // Each month the period touches is calculated, then only the period dates are kept
                List<LocalDate> workingDates = new ArrayList<>();

                for (YearMonth ym = firstMonth; !ym.isAfter(lastMonth); ym = ym.plusMonths(1)) {

                        for (LocalDate date : WorkingDayCalculator.workingDates(
                                        ym,
                                        settingTypes,
                                        recurringWeeklyOffDays)) {

                                if (!date.isBefore(start) && !date.isAfter(end)) {
                                        workingDates.add(date);
                                }
                        }
                }

                Set<LocalDate> workingSet = new HashSet<>(
                                workingDates);

                // =========================================
                // LOAD ATTENDANCE
                // =========================================

                Map<Integer, Set<LocalDate>> presentByEmployee = new HashMap<>();

                // All sessions (rows) per employee per date, used for the shift rule
                Map<Integer, Map<LocalDate, List<Attendance>>> sessionsByEmployee = new HashMap<>();

                for (Attendance attendance : attendanceRepository
                                .findByAttendanceDateBetween(
                                                start,
                                                end)) {

                        if (attendance.getEmployeeId() == null ||
                                        attendance.getAttendanceDate() == null ||
                                        attendance.getCheckIn() == null) {

                                continue;
                        }

                        presentByEmployee
                                        .computeIfAbsent(
                                                        attendance.getEmployeeId(),
                                                        key -> new HashSet<>())
                                        .add(
                                                        attendance.getAttendanceDate());

                        sessionsByEmployee
                                        .computeIfAbsent(
                                                        attendance.getEmployeeId(),
                                                        key -> new HashMap<>())
                                        .computeIfAbsent(
                                                        attendance.getAttendanceDate(),
                                                        key -> new ArrayList<>())
                                        .add(attendance);
                }

                // =========================================
                // LOAD LEAVE REQUESTS
                // =========================================

                Map<Integer, List<LeaveRequest>> leavesByEmployee = new HashMap<>();

                for (LeaveRequest leave : leaveRequestRepository
                                .findByLeaveDateBetween(
                                                start,
                                                end)) {

                        if (leave.getEmployeeId() == null) {
                                continue;
                        }

                        leavesByEmployee
                                        .computeIfAbsent(
                                                        leave.getEmployeeId(),
                                                        key -> new ArrayList<>())
                                        .add(leave);
                }

                // =========================================
                // LOAD OT RECORDS
                // =========================================

                Map<Integer, List<OvertimeRecord>> overtimeByEmployee = new HashMap<>();

                for (OvertimeRecord overtime : overtimeRecordRepository
                                .findByOtDateBetweenOrderByOtDateAsc(
                                                start,
                                                end)) {

                        if (overtime == null ||
                                        overtime.getEmployeeId() == null) {

                                continue;
                        }

                        overtimeByEmployee
                                        .computeIfAbsent(
                                                        overtime.getEmployeeId(),
                                                        key -> new ArrayList<>())
                                        .add(overtime);
                }

                // =========================================
                // LOAD ADMIN DECISIONS (late hours / full pay)
                // =========================================

                Map<Integer, PayrollAdjustment> adjustmentByEmployee = new HashMap<>();

                for (PayrollAdjustment adjustment : adjustmentRepository
                                .findByPayYearAndPayMonth(
                                                year,
                                                month)) {

                        if (adjustment.getEmployeeId() != null) {

                                adjustmentByEmployee.put(
                                                adjustment.getEmployeeId(),
                                                adjustment);
                        }
                }

                // =========================================
                // LOAD ACTIVE NON-ADMIN EMPLOYEES
                // =========================================

                List<Employee> employees = employeeRepository.findAll()
                                .stream()
                                .filter(e -> Boolean.TRUE.equals(
                                                e.getActive()))
                                .filter(e -> !"ADMIN".equalsIgnoreCase(
                                                e.getRole()))
                                .sorted(
                                                Comparator.comparing(
                                                                Employee::getEmployeeCode,
                                                                Comparator.nullsLast(
                                                                                String.CASE_INSENSITIVE_ORDER)))
                                .toList();

                List<EmployeePayroll> result = new ArrayList<>();

                // =========================================
                // CALCULATE EACH EMPLOYEE
                // =========================================

                for (Employee employee : employees) {

                        Integer employeeId = employee.getId();

                        Set<LocalDate> present = presentByEmployee.getOrDefault(
                                        employeeId,
                                        Set.of());

                        List<LeaveRequest> leaves = leavesByEmployee.getOrDefault(
                                        employeeId,
                                        List.of());

                        Map<LocalDate, Double> paidLeaveByDate = new HashMap<>();

                        double sick = 0;
                        double casual = 0;
                        double half = 0;
                        double lop = 0;
                        int permission = 0;

                        // Dates with approved permission but no start / end time (whole day skipped in shift rule)
                        Set<LocalDate> permissionDates = new HashSet<>();

                        // Approved permission minutes per date (these minutes are never cut from salary)
                        Map<LocalDate, Long> permissionMinutesByDate = new HashMap<>();

                        // =========================================
                        // PROCESS APPROVED LEAVES
                        // =========================================

                        for (LeaveRequest leave : leaves) {

                                if (!"APPROVED".equalsIgnoreCase(
                                                leave.getStatus())
                                                || leave.getLeaveType() == null
                                                || leave.getLeaveDate() == null) {

                                        continue;
                                }

                                String type = leave.getLeaveType()
                                                .trim()
                                                .toUpperCase();

                                // =========================================
                                // PERMISSION
                                // =========================================

                                if (type.equals("PERMISSION")) {

                                        permission++;

                                        long permissionMinutes = 0;

                                        if (leave.getPermissionStart() != null
                                                        && leave.getPermissionEnd() != null
                                                        && leave.getPermissionEnd()
                                                                        .isAfter(leave.getPermissionStart())) {

                                                permissionMinutes = Duration.between(
                                                                leave.getPermissionStart(),
                                                                leave.getPermissionEnd())
                                                                .toMinutes();
                                        }

                                        if (permissionMinutes > 0) {

                                                permissionMinutesByDate.merge(
                                                                leave.getLeaveDate(),
                                                                permissionMinutes,
                                                                Long::sum);

                                        } else {

                                                permissionDates.add(
                                                                leave.getLeaveDate());
                                        }

                                        continue;
                                }

                                double duration = leave.getLeaveDuration() != null
                                                ? leave.getLeaveDuration()
                                                : 1.0;

                                if (duration <= 0) {
                                        continue;
                                }

                                duration = Math.min(
                                                duration,
                                                1.0);

                                LocalDate date = leave.getLeaveDate();

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
                                                                (a, b) -> Math.min(
                                                                                1.0,
                                                                                a + b));
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

                                LeavePermissionSetting setting = resolveSetting(employee);

                                double sickAllowance = setting != null &&
                                                setting.getSickLeave() != null
                                                                ? Math.max(
                                                                                0,
                                                                                setting.getSickLeave())
                                                                : 1.0;

                                double casualAllowance = setting != null &&
                                                setting.getCasualLeave() != null
                                                                ? Math.max(
                                                                                0,
                                                                                setting.getCasualLeave())
                                                                : 1.0;

                                double notUsedYet = Math.max(
                                                0,
                                                sickAllowance +
                                                                casualAllowance -
                                                                sick -
                                                                casual);

                                autoCoverCap = notUsedYet;

                                var balance = balanceRepository
                                                .findByEmployeeIdAndBalanceMonth(
                                                                employeeId,
                                                                balanceMonth);

                                if (balance.isPresent()) {

                                        double sickLeft = balance.get().getSickBalance() != null
                                                        ? Math.max(
                                                                        0,
                                                                        balance.get()
                                                                                        .getSickBalance())
                                                        : 0;

                                        double casualLeft = balance.get().getCasualBalance() != null
                                                        ? Math.max(
                                                                        0,
                                                                        balance.get()
                                                                                        .getCasualBalance())
                                                        : 0;

                                        autoCoverCap = Math.min(
                                                        autoCoverCap,
                                                        sickLeft +
                                                                        casualLeft);
                                }
                        }

                        // =========================================
                        // MONTHLY SALARY
                        // =========================================

                        double monthlySalary = employee.getSalary() != null
                                        ? employee.getSalary()
                                        : 0.0;

                        // =========================================
                        // FINAL PAYROLL MATH
                        // =========================================

                        // =========================================
                        // SHIFT SHORTFALL (late + early, all sessions)
                        // =========================================

                        Shortfall shortfall = calculateShortfall(
                                        employee,
                                        workingSet,
                                        start,
                                        sessionsByEmployee.getOrDefault(
                                                        employeeId,
                                                        Map.of()),
                                        paidLeaveByDate.keySet(),
                                        permissionDates,
                                        permissionMinutesByDate);

                        PayrollAdjustment adjustment = adjustmentByEmployee.get(employeeId);

                        boolean payFull = adjustment != null
                                        && Boolean.TRUE.equals(adjustment.getPayFull());

                        // Default: NO cut (all present days are paid). The admin cuts only the chosen employees.
                        double shortfallDays = 0.0;

                        long appliedDeductMinutes = 0;

                        if (adjustment != null
                                        && adjustment.getDeductMinutes() != null
                                        && shortfall.shiftMinutes() > 0) {

                                appliedDeductMinutes = Math.max(
                                                0,
                                                adjustment.getDeductMinutes());

                                shortfallDays = (double) appliedDeductMinutes
                                                / shortfall.shiftMinutes();
                        }

                        if (payFull) {

                                shortfallDays = 0.0;

                                appliedDeductMinutes = 0;
                        }

                        PayrollMath.Result math = PayrollMath.calculate(
                                        workingDates,
                                        present,
                                        paidLeaveByDate,
                                        shortfallDays,
                                        autoCoverCap,
                                        monthlySalary);

                        // Admin chose full pay: leave, absence and late time are all ignored
                        if (payFull && math.workingDays() > 0) {

                                math = new PayrollMath.Result(
                                                math.workingDays(),
                                                math.presentDays(),
                                                math.paidLeaveDays(),
                                                math.autoCoveredDays(),
                                                (double) math.workingDays(),
                                                0.0,
                                                math.perDaySalary(),
                                                0.0,
                                                PayrollMath.round2(monthlySalary));
                        }

                        // =========================================
                        // DISPLAY ABSENCE / LOP DETAILS
                        // =========================================

                        // Shift shortfall is shown inside LOP days, not as absent days
                        double rawAbsent = Math.max(
                                        0,
                                        math.lopDays() +
                                                        math.autoCoveredDays() -
                                                        shortfallDays);

                        double lopShown = Math.min(
                                        lop,
                                        rawAbsent);

                        double absent = PayrollMath.round2(
                                        Math.max(
                                                        0,
                                                        rawAbsent -
                                                                        lopShown));

                        // =========================================
                        // CALCULATE OVERTIME
                        // =========================================

                        List<OvertimeRecord> overtimeRecords = overtimeByEmployee.getOrDefault(
                                        employeeId,
                                        List.of());

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

                        double perDaySalary = math.perDaySalary();

                        for (OvertimeRecord overtime : overtimeRecords) {

                                if (!Boolean.TRUE.equals(
                                                overtime.getOtEligible())) {

                                        continue;
                                }

                                String benefitType = overtime.getBenefitType() != null
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

                                        double amount = perDaySalary;

                                        if (overtime.getOtAmount() != null &&
                                                        overtime.getOtAmount() > 0) {

                                                amount = overtime.getOtAmount();
                                        }

                                        overtimeSalary += amount;
                                }

                                // =========================================
                                // COMPENSATORY OFF
                                // =========================================

                                else if ("COMP_OFF".equals(
                                                benefitType)) {

                                        double compOff = overtime.getCompOffDays() != null
                                                        ? overtime.getCompOffDays()
                                                        : 1.0;

                                        compensatoryOffDays += Math.max(
                                                        0,
                                                        compOff);
                                }
                        }

                        overtimeSalary = PayrollMath.round2(
                                        overtimeSalary);

                        compensatoryOffDays = PayrollMath.round2(
                                        compensatoryOffDays);

                        double finalSalaryWithOvertime = PayrollMath.round2(
                                        math.finalSalary() +
                                                        overtimeSalary);

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
                                                        finalSalaryWithOvertime,
                                                        new LateInfo(
                                                                        shortfall.deductibleMinutes(),
                                                                        shortfall.toleratedMinutes(),
                                                                        shortfall.shortDays(),
                                                                        shortfall.shiftMinutes(),
                                                                        appliedDeductMinutes),
                                                        adjustment));
                }

                return new MonthPayroll(
                                workingDates.size(),
                                result);
        }

        // =========================================
        // SHIFT SHORTFALL RULE
        //
        // Per past working day with attendance:
        // worked = total time of ALL sessions (check-out required)
        // shortfall = shift minutes - worked
        // shortfall <= 30 min -> full day paid
        // shortfall > 30 min -> only the missing time is cut
        //
        // Skipped: today / future, Sick / Casual / Half day and
        // Permission days.
        // A session without check-out counts as 0 minutes
        // until the admin approves the missing check-out.
        // =========================================

        private Shortfall calculateShortfall(
                        Employee employee,
                        Set<LocalDate> workingSet,
                        LocalDate ruleStart,
                        Map<LocalDate, List<Attendance>> sessionsByDate,
                        Set<LocalDate> leaveDates,
                        Set<LocalDate> permissionDates,
                        Map<LocalDate, Long> permissionMinutesByDate) {

                ShiftTimeService.ShiftTimes shift = shiftTimeService.getShiftTimes(employee);

                long shiftMinutes = shift == null
                                ? 0
                                : shift.totalMinutes();

                if (sessionsByDate.isEmpty() || shiftMinutes <= 0) {
                        return new Shortfall(0.0, 0, 0, 0, Math.max(0, shiftMinutes));
                }

                LocalDate today = LocalDate.now(INDIA_ZONE);

                double total = 0.0;
                long deductibleMinutes = 0;
                long toleratedMinutes = 0;
                int shortDays = 0;

                for (Map.Entry<LocalDate, List<Attendance>> entry : sessionsByDate.entrySet()) {

                        LocalDate date = entry.getKey();

                        if (!workingSet.contains(date)
                                        || date.isBefore(ruleStart)
                                        || !date.isBefore(today)
                                        || leaveDates.contains(date)
                                        || permissionDates.contains(date)) {

                                continue;
                        }

                        long worked = 0;

                        for (Attendance session : entry.getValue()) {

                                LocalTime in = session.getCheckIn();
                                LocalTime out = session.getCheckOut();

                                if (in == null || out == null || !out.isAfter(in)) {
                                        continue;
                                }

                                worked += Duration.between(in, out).toMinutes();
                        }

                        // Approved permission hours are not counted as short time
                        long required = Math.max(
                                        0,
                                        shiftMinutes - permissionMinutesByDate.getOrDefault(date, 0L));

                        long shortfall = required - worked;

                        if (shortfall <= 0) {
                                continue;
                        }

                        if (shortfall <= ALLOWED_SHORTFALL_MINUTES) {
                                toleratedMinutes += shortfall;
                                continue;
                        }

                        deductibleMinutes += shortfall;
                        shortDays++;

                        total += Math.min(
                                        1.0,
                                        (double) shortfall / shiftMinutes);
                }

                return new Shortfall(
                                total,
                                deductibleMinutes,
                                toleratedMinutes,
                                shortDays,
                                shiftMinutes);
        }

        // =========================================
        // RESOLVE LEAVE SETTING
        // =========================================

        private LeavePermissionSetting resolveSetting(
                        Employee employee) {

                if (employee.getId() != null) {

                        var employeeSetting = settingRepository
                                        .findBySettingTypeAndEmployeeId(
                                                        "EMPLOYEE",
                                                        employee.getId());

                        if (employeeSetting.isPresent()) {

                                return employeeSetting.get();
                        }
                }

                if (employee.getRole() != null &&
                                !employee.getRole().isBlank()) {

                        var roleSetting = settingRepository
                                        .findBySettingTypeAndRole(
                                                        "ROLE",
                                                        employee.getRole()
                                                                        .trim()
                                                                        .toUpperCase());

                        if (roleSetting.isPresent()) {

                                return roleSetting.get();
                        }
                }

                return settingRepository
                                .findBySettingType("DEFAULT")
                                .orElse(null);
        }
}