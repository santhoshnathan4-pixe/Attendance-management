package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/salary")
public class AdminSalaryController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AdminRepository adminRepository;
    private final AdminActionHistoryRepository historyRepository;
    private final WorkingDaySettingRepository workingDaySettingRepository;

    public AdminSalaryController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository,
            AdminRepository adminRepository,
            AdminActionHistoryRepository historyRepository,
            WorkingDaySettingRepository workingDaySettingRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.adminRepository = adminRepository;
        this.historyRepository = historyRepository;
        this.workingDaySettingRepository = workingDaySettingRepository;
    }

    // =========================================
    // GET ALL EMPLOYEE SALARY
    // =========================================

    @GetMapping
    public List<AdminSalaryResponse> getAllSalary() {

        return employeeRepository.findAll()
                .stream()
                .filter(employee ->
                        Boolean.TRUE.equals(employee.getActive()))
                .filter(employee ->
                        !"ADMIN".equalsIgnoreCase(employee.getRole()))
                .sorted(
                        Comparator.comparing(
                                Employee::getEmployeeCode,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                )
                .map(employee ->
                        new AdminSalaryResponse(
                                employee.getId(),
                                employee.getEmployeeCode(),
                                employee.getName(),
                                employee.getSalary(),
                                employee.getJoiningDate()
                        )
                )
                .toList();
    }

    // =========================================
    // UPDATE EMPLOYEE SALARY
    // =========================================

    @PutMapping("/{employeeId}")
    public String updateSalary(
            @PathVariable Integer employeeId,
            @RequestBody AdminSalaryUpdateRequest request) {

        Admin admin = findLoggedInAdmin(
                request.getAdminEmail()
        );

        if (request.getNewSalary() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Salary is required"
            );
        }

        if (request.getNewSalary() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Salary cannot be negative"
            );
        }

        validateReason(
                request.getReason()
        );

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Employee not found"
                                )
                        );

        if (!Boolean.TRUE.equals(employee.getActive())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee is inactive"
            );
        }

        if ("ADMIN".equalsIgnoreCase(employee.getRole())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Salary cannot be updated for admin"
            );
        }

        Double oldSalary =
                employee.getSalary() != null
                        ? employee.getSalary()
                        : 0.0;

        Double newSalary =
                request.getNewSalary();

        if (Double.compare(oldSalary, newSalary) == 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No salary change detected"
            );
        }

        employee.setSalary(newSalary);

        employeeRepository.save(employee);

        saveHistory(
                admin,
                "SALARY UPDATE",
                employee,
                "Salary",
                String.valueOf(oldSalary),
                String.valueOf(newSalary),
                request.getReason()
        );

        return "Salary updated successfully";
    }

    // =========================================
    // CALCULATE MONTHLY SALARY
    // =========================================

    @GetMapping("/calculate")
    public List<SalaryResponse> calculateSalary(
            @RequestParam int year,
            @RequestParam int month) {

        YearMonth yearMonth =
                YearMonth.of(year, month);

        LocalDate startDate =
                yearMonth.atDay(1);

        LocalDate endDate =
                yearMonth.atEndOfMonth();

        // =========================================
        // COMPANY DEFAULT WORKING DAYS
        //
        // Default = 26
        //
        // Working-day settings can adjust this.
        // =========================================

        int workingDays =
                calculateWorkingDays(
                        startDate,
                        endDate
                );

        return employeeRepository.findAll()
                .stream()
                .filter(employee ->
                        Boolean.TRUE.equals(employee.getActive()))
                .filter(employee ->
                        !"ADMIN".equalsIgnoreCase(employee.getRole()))
                .sorted(
                        Comparator.comparing(
                                Employee::getEmployeeCode,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                )
                .map(employee -> {

                    // =========================================
                    // ATTENDANCE
                    // =========================================

                    List<Attendance> attendanceList =
                            attendanceRepository
                                    .findByEmployeeIdOrderByAttendanceDateDesc(
                                            employee.getId());

                    /*
                     * One employee can have only one attendance
                     * record per date.
                     *
                     * Attendance with check-in is treated as
                     * attendance coverage for that date.
                     *
                     * Timing duration is NOT used to change
                     * the daily salary value.
                     */

                    Map<LocalDate, Double> paidDayCoverage =
                            new HashMap<>();

                    attendanceList.stream()
                            .filter(attendance ->
                                    attendance.getAttendanceDate() != null)
                            .filter(attendance ->
                                    !attendance.getAttendanceDate()
                                            .isBefore(startDate))
                            .filter(attendance ->
                                    !attendance.getAttendanceDate()
                                            .isAfter(endDate))
                            .filter(attendance ->
                                    attendance.getCheckIn() != null)
                            .forEach(attendance ->
                                    paidDayCoverage.merge(
                                            attendance.getAttendanceDate(),
                                            1.0,
                                            Math::max
                                    )
                            );

                    // =========================================
                    // APPROVED SICK / CASUAL LEAVE
                    // =========================================

                    List<LeaveRequest> leaveList =
                            leaveRequestRepository
                                    .findByEmployeeIdOrderByLeaveDateDesc(
                                            employee.getId());

                    /*
                     * Leave is processed date-by-date.
                     *
                     * Only APPROVED SICK/CASUAL records are
                     * considered paid leave.
                     *
                     * PERMISSION is intentionally excluded.
                     *
                     * Multiple records for the same date
                     * cannot make the paid-day coverage exceed
                     * 1.0.
                     */

                    Map<LocalDate, Double> approvedLeaveCoverage =
                            new HashMap<>();

                    for (LeaveRequest leave : leaveList) {

                        if (leave == null ||
                                leave.getLeaveDate() == null) {

                            continue;
                        }

                        LocalDate leaveDate =
                                leave.getLeaveDate();

                        if (leaveDate.isBefore(startDate) ||
                                leaveDate.isAfter(endDate)) {

                            continue;
                        }

                        if (!"APPROVED".equalsIgnoreCase(
                                leave.getStatus())) {

                            continue;
                        }

                        String leaveType =
                                leave.getLeaveType();

                        if (leaveType == null) {
                            continue;
                        }

                        /*
                         * Permission is an hourly permission.
                         * It must NEVER become one full salary day.
                         */
                        if ("PERMISSION".equalsIgnoreCase(
                                leaveType)) {

                            continue;
                        }

                        if (!"SICK".equalsIgnoreCase(leaveType) &&
                                !"CASUAL".equalsIgnoreCase(leaveType)) {

                            continue;
                        }

                        double duration =
                                leave.getLeaveDuration() != null
                                        ? leave.getLeaveDuration()
                                        : 1.0;

                        /*
                         * Only valid positive leave duration.
                         */
                        if (duration <= 0) {
                            continue;
                        }

                        /*
                         * A leave record can never contribute
                         * more than one day.
                         */
                        duration =
                                Math.min(duration, 1.0);

                        approvedLeaveCoverage.merge(
                                leaveDate,
                                duration,
                                (oldValue, newValue) ->
                                        Math.min(
                                                1.0,
                                                oldValue + newValue
                                        )
                        );
                    }

                    // =========================================
                    // COMBINE ATTENDANCE + LEAVE
                    // =========================================

                    /*
                     * For every date:
                     *
                     * Attendance + half-day leave
                     * = maximum 1 paid day.
                     *
                     * Attendance + full-day leave
                     * = maximum 1 paid day.
                     *
                     * This prevents double salary counting.
                     */

                    Map<LocalDate, Double> totalPaidCoverage =
                            new HashMap<>();

                    paidDayCoverage.forEach(
                            (date, attendanceValue) -> {

                                double leaveValue =
                                        approvedLeaveCoverage
                                                .getOrDefault(
                                                        date,
                                                        0.0
                                                );

                                double total =
                                        Math.min(
                                                1.0,
                                                attendanceValue
                                                        + leaveValue
                                        );

                                totalPaidCoverage.put(
                                        date,
                                        total
                                );
                            }
                    );

                    approvedLeaveCoverage.forEach(
                            (date, leaveValue) -> {

                                if (!totalPaidCoverage
                                        .containsKey(date)) {

                                    totalPaidCoverage.put(
                                            date,
                                            Math.min(
                                                    1.0,
                                                    leaveValue
                                            )
                                    );
                                }
                            }
                    );

                    // =========================================
                    // PAID DAYS
                    // =========================================

                    double paidDays =
                            totalPaidCoverage.values()
                                    .stream()
                                    .mapToDouble(
                                            Double::doubleValue
                                    )
                                    .sum();

                    /*
                     * Salary cannot have more paid attendance
                     * days than the month's working days.
                     */
                    paidDays =
                            Math.min(
                                    paidDays,
                                    workingDays
                            );

                    // =========================================
                    // DISPLAY PRESENT DAYS
                    // =========================================

                    /*
                     * Existing SalaryResponse has an integer
                     * presentDays field.
                     *
                     * Full attendance days are counted here.
                     * Half-day leave combined with attendance
                     * still remains one paid day overall.
                     */

                    int presentDays =
                            (int) Math.floor(
                                    paidDayCoverage.values()
                                            .stream()
                                            .mapToDouble(
                                                    Double::doubleValue
                                            )
                                            .sum()
                            );

                    /*
                     * Do not let display presentDays exceed
                     * working days.
                     */
                    presentDays =
                            Math.min(
                                    presentDays,
                                    workingDays
                            );

                    // =========================================
                    // TOTAL PAID LEAVE
                    // =========================================

                    double leaveDays =
                            approvedLeaveCoverage.values()
                                    .stream()
                                    .mapToDouble(
                                            Double::doubleValue
                                    )
                                    .sum();

                    /*
                     * Leave which overlaps with attendance
                     * should not be displayed as additional
                     * salary days.
                     */
                    double leaveOnlyDays =
                            approvedLeaveCoverage
                                    .entrySet()
                                    .stream()
                                    .mapToDouble(entry -> {

                                        double attendanceValue =
                                                paidDayCoverage
                                                        .getOrDefault(
                                                                entry.getKey(),
                                                                0.0
                                                        );

                                        return Math.max(
                                                0.0,
                                                Math.min(
                                                        1.0,
                                                        entry.getValue()
                                                ) - attendanceValue
                                        );
                                    })
                                    .sum();

                    /*
                     * Salary leaveDays represents the actual
                     * paid leave contribution after overlap.
                     */
                    leaveDays =
                            Math.max(
                                    0.0,
                                    leaveOnlyDays
                            );

                    // =========================================
                    // MONTHLY SALARY
                    // =========================================

                    double monthlySalary =
                            employee.getSalary() != null
                                    ? employee.getSalary()
                                    : 0.0;

                    // =========================================
                    // PER DAY SALARY
                    //
                    // IMPORTANT:
                    //
                    // Office timing duration is NOT used here.
                    //
                    // 9:00 - 18:00
                    // 10:00 - 17:30
                    // 10:00 - 18:00
                    //
                    // All have the SAME daily salary.
                    // =========================================

                    double perDaySalary =
                            workingDays > 0
                                    ? monthlySalary / workingDays
                                    : 0.0;

                    // =========================================
                    // LOSS OF PAY
                    // =========================================

                    double lopDays =
                            Math.max(
                                    0.0,
                                    workingDays - paidDays
                            );

                    double lossOfPay =
                            lopDays * perDaySalary;

                    // =========================================
                    // FINAL SALARY
                    // =========================================

                    double finalSalary =
                            monthlySalary - lossOfPay;

                    return new SalaryResponse(
                            employee.getId(),
                            employee.getEmployeeCode(),
                            employee.getName(),
                            monthlySalary,
                            workingDays,
                            presentDays,
                            Math.round(
                                    leaveDays * 100.0
                            ) / 100.0,
                            Math.round(
                                    lossOfPay * 100.0
                            ) / 100.0,
                            Math.round(
                                    finalSalary * 100.0
                            ) / 100.0
                    );

                })
                .toList();
    }

    // =========================================
    // CALCULATE WORKING DAYS
    // =========================================
    //
    // Default company working days = 26.
    //
    // GOVERNMENT_HOLIDAY  -> -1
    // COMPANY_HOLIDAY     -> -1
    // WORKING_SATURDAY    -> +1
    //
    // A date can have only one setting because
    // setting_date is UNIQUE in the database.
    // =========================================

    private int calculateWorkingDays(
            LocalDate startDate,
            LocalDate endDate) {

        int workingDays = 26;

        List<WorkingDaySetting> settings =
                workingDaySettingRepository
                        .findBySettingDateBetweenOrderBySettingDateAsc(
                                startDate,
                                endDate
                        );

        for (WorkingDaySetting setting : settings) {

            if (setting == null ||
                    setting.getSettingDate() == null ||
                    setting.getSettingType() == null) {

                continue;
            }

            String type =
                    setting.getSettingType()
                            .trim()
                            .toUpperCase();

            if ("GOVERNMENT_HOLIDAY".equals(type) ||
                    "COMPANY_HOLIDAY".equals(type)) {

                workingDays--;

            } else if ("WORKING_SATURDAY".equals(type)) {

                workingDays++;
            }
        }

        return Math.max(
                workingDays,
                0
        );
    }

    // =========================================
    // FIND LOGGED-IN ADMIN
    // =========================================

    private Admin findLoggedInAdmin(
            String email) {

        if (email == null ||
                email.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Admin login required"
            );
        }

        return adminRepository
                .findByEmail(
                        email.trim()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Logged-in admin not found"
                        )
                );
    }

    // =========================================
    // REASON VALIDATION
    // =========================================

    private void validateReason(
            String reason) {

        if (reason == null ||
                reason.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reason is required for this operation"
            );
        }

        if (reason.trim().length() < 3) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please enter a valid reason"
            );
        }
    }

    // =========================================
    // SAVE HISTORY
    // =========================================

    private void saveHistory(
            Admin admin,
            String action,
            Employee employee,
            String fieldName,
            String oldValue,
            String newValue,
            String reason) {

        AdminActionHistory history =
                new AdminActionHistory();

        // -----------------------------------------
        // ADMIN
        // -----------------------------------------

        history.setAdminName(
                admin.getAdminName()
        );

        // -----------------------------------------
        // ACTION
        // -----------------------------------------

        history.setAction(
                action
        );

        // -----------------------------------------
        // EMPLOYEE DETAILS
        // -----------------------------------------

        history.setEmployeeId(
                employee.getId()
        );

        history.setEmployeeCode(
                employee.getEmployeeCode()
        );

        history.setEmployeeName(
                employee.getName()
        );

        // -----------------------------------------
        // REASON
        // -----------------------------------------

        history.setReason(
                reason.trim()
        );

        // -----------------------------------------
        // DATE & TIME - INDIA (IST)
        // -----------------------------------------

        ZoneId indiaZone =
                ZoneId.of("Asia/Kolkata");

        history.setActionDate(
                LocalDate.now(indiaZone)
        );

        history.setActionTime(
                LocalTime.now(indiaZone)
        );

        // -----------------------------------------
        // FIELD
        // -----------------------------------------

        history.setFieldName(
                fieldName
        );

        history.setOldValue(
                oldValue
        );

        history.setNewValue(
                newValue
        );

        // -----------------------------------------
        // SAVE
        // -----------------------------------------

        historyRepository.save(history);
    }
}