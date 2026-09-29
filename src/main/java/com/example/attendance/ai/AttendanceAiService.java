package com.example.attendance.ai;

import com.example.attendance.AdminActionHistory;
import com.example.attendance.AdminActionHistoryRepository;
import com.example.attendance.Attendance;
import com.example.attendance.AttendanceRepository;
import com.example.attendance.Employee;
import com.example.attendance.EmployeeLeaveBalance;
import com.example.attendance.EmployeeLeaveBalanceRepository;
import com.example.attendance.EmployeeRepository;
import com.example.attendance.LeaveRequest;
import com.example.attendance.LeaveRequestRepository;
import com.example.attendance.ShiftSetting;
import com.example.attendance.ShiftSettingRepository;
import com.example.attendance.WorkingDaySetting;
import com.example.attendance.WorkingDaySettingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceAiService {

        private final AttendanceRepository attendanceRepository;
        private final EmployeeRepository employeeRepository;
        private final LeaveRequestRepository leaveRequestRepository;
        private final EmployeeLeaveBalanceRepository balanceRepository;
        private final WorkingDaySettingRepository workingDaySettingRepository;
        private final ShiftSettingRepository shiftSettingRepository;
        private final AdminActionHistoryRepository historyRepository;
        private final GeminiAiService geminiAiService;

        public AttendanceAiService(
                        AttendanceRepository attendanceRepository,
                        EmployeeRepository employeeRepository,
                        LeaveRequestRepository leaveRequestRepository,
                        EmployeeLeaveBalanceRepository balanceRepository,
                        WorkingDaySettingRepository workingDaySettingRepository,
                        ShiftSettingRepository shiftSettingRepository,
                        AdminActionHistoryRepository historyRepository,
                        GeminiAiService geminiAiService) {

                this.attendanceRepository = attendanceRepository;
                this.employeeRepository = employeeRepository;
                this.leaveRequestRepository = leaveRequestRepository;
                this.balanceRepository = balanceRepository;
                this.workingDaySettingRepository = workingDaySettingRepository;
                this.shiftSettingRepository = shiftSettingRepository;
                this.historyRepository = historyRepository;
                this.geminiAiService = geminiAiService;
        }

        public String analyzeCurrentMonth(String question) {

                if (question == null || question.isBlank()) {
                        return "Please enter a valid question.";
                }

                LocalDate today = LocalDate.now();

                LocalDate startDate = today.withDayOfMonth(1);

                LocalDate endDate = today.withDayOfMonth(
                                today.lengthOfMonth());

                YearMonth yearMonth = YearMonth.from(today);

                List<Attendance> attendanceList = attendanceRepository.findByAttendanceDateBetween(
                                startDate,
                                endDate);

                List<Employee> activeEmployees = employeeRepository.findByActiveTrue();

                List<LeaveRequest> leaveRequestList = leaveRequestRepository.findByLeaveDateBetween(
                                startDate,
                                endDate);

                StringBuilder data = new StringBuilder();

                data.append("Attendance period: ")
                                .append(startDate)
                                .append(" to ")
                                .append(endDate)
                                .append("\n\n");

                data.append("Current date: ")
                                .append(today)
                                .append("\n\n");

                data.append("Current month: ")
                                .append(yearMonth)
                                .append("\n\n");

                data.append("Active employee count: ")
                                .append(activeEmployees.size())
                                .append("\n\n");

                // =========================================
                // EMPLOYEE DATA
                // =========================================

                data.append("EMPLOYEE DATA")
                                .append("\n")
                                .append("====================")
                                .append("\n");

                if (activeEmployees.isEmpty()) {

                        data.append(
                                        "No active employee records were found.\n");

                } else {

                        activeEmployees.stream()
                                        .sorted(
                                                        Comparator.comparing(
                                                                        Employee::getEmployeeCode,
                                                                        Comparator.nullsLast(
                                                                                        String.CASE_INSENSITIVE_ORDER)))
                                        .forEach(employee -> {

                                                data.append("Employee Code: ")
                                                                .append(
                                                                                employee.getEmployeeCode() != null
                                                                                                ? employee.getEmployeeCode()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Employee Name: ")
                                                                .append(
                                                                                employee.getName() != null
                                                                                                ? employee.getName()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Email: ")
                                                                .append(
                                                                                employee.getEmail() != null
                                                                                                ? employee.getEmail()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Role: ")
                                                                .append(
                                                                                employee.getRole() != null
                                                                                                ? employee.getRole()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Technology: ")
                                                                .append(
                                                                                employee.getTechnology() != null
                                                                                                ? employee.getTechnology()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Designation: ")
                                                                .append(
                                                                                employee.getDesignation() != null
                                                                                                ? employee.getDesignation()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Joining Date: ")
                                                                .append(
                                                                                employee.getJoiningDate() != null
                                                                                                ? employee.getJoiningDate()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Contact Number: ")
                                                                .append(
                                                                                employee.getContactNumber() != null
                                                                                                ? employee.getContactNumber()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Active: ")
                                                                .append(
                                                                                Boolean.TRUE.equals(
                                                                                                employee.getActive()))
                                                                .append("\n");

                                                data.append("--------------------\n");
                                        });
                }

                // =========================================
                // ATTENDANCE DATA
                // =========================================

                data.append("\n")
                                .append("ATTENDANCE DATA")
                                .append("\n")
                                .append("====================")
                                .append("\n");

                if (attendanceList.isEmpty()) {

                        data.append(
                                        "No attendance records were found for this period.\n");

                } else {

                        for (Attendance attendance : attendanceList) {

                                String employeeName = "Unknown";

                                String employeeCode = "Unknown";

                                if (attendance.getEmployeeId() != null) {

                                        Employee employee = employeeRepository
                                                        .findById(
                                                                        attendance.getEmployeeId())
                                                        .orElse(null);

                                        if (employee != null) {

                                                employeeName = employee.getName();

                                                employeeCode = employee.getEmployeeCode();
                                        }
                                }

                                data.append("Employee Code: ")
                                                .append(employeeCode)
                                                .append("\n");

                                data.append("Employee Name: ")
                                                .append(employeeName)
                                                .append("\n");

                                data.append("Date: ")
                                                .append(
                                                                attendance.getAttendanceDate())
                                                .append("\n");

                                data.append("Check-in: ")
                                                .append(
                                                                attendance.getCheckIn() != null
                                                                                ? attendance.getCheckIn()
                                                                                : "Missing")
                                                .append("\n");

                                data.append("Check-out: ")
                                                .append(
                                                                attendance.getCheckOut() != null
                                                                                ? attendance.getCheckOut()
                                                                                : "Missing")
                                                .append("\n");

                                data.append("Status: ")
                                                .append(
                                                                attendance.getStatus() != null
                                                                                ? attendance.getStatus()
                                                                                : "Missing")
                                                .append("\n");

                                data.append("--------------------\n");
                        }
                }

                // =========================================
                // LEAVE / PERMISSION DATA
                // =========================================

                data.append("\n")
                                .append("LEAVE AND PERMISSION DATA")
                                .append("\n")
                                .append("====================")
                                .append("\n");

                if (leaveRequestList.isEmpty()) {

                        data.append(
                                        "No leave or permission records were found for this period.\n");

                } else {

                        for (LeaveRequest request : leaveRequestList) {

                                String employeeName = "Unknown";

                                String employeeCode = "Unknown";

                                if (request.getEmployeeId() != null) {

                                        Employee employee = employeeRepository
                                                        .findById(
                                                                        request.getEmployeeId())
                                                        .orElse(null);

                                        if (employee != null) {

                                                employeeName = employee.getName();

                                                employeeCode = employee.getEmployeeCode();
                                        }
                                }

                                data.append("Employee Code: ")
                                                .append(employeeCode)
                                                .append("\n");

                                data.append("Employee Name: ")
                                                .append(employeeName)
                                                .append("\n");

                                data.append("Date: ")
                                                .append(request.getLeaveDate())
                                                .append("\n");

                                data.append("Leave Type: ")
                                                .append(
                                                                request.getLeaveType() != null
                                                                                ? request.getLeaveType()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Leave Duration: ")
                                                .append(
                                                                request.getLeaveDuration() != null
                                                                                ? request.getLeaveDuration()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("LOP Days: ")
                                                .append(
                                                                request.getLopDays() != null
                                                                                ? request.getLopDays()
                                                                                : 0.00)
                                                .append("\n");

                                data.append("Half Day Session: ")
                                                .append(
                                                                request.getHalfDaySession() != null
                                                                                ? request.getHalfDaySession()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Permission Start: ")
                                                .append(
                                                                request.getPermissionStart() != null
                                                                                ? request.getPermissionStart()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Permission End: ")
                                                .append(
                                                                request.getPermissionEnd() != null
                                                                                ? request.getPermissionEnd()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Reason: ")
                                                .append(
                                                                request.getReason() != null
                                                                                ? request.getReason()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Status: ")
                                                .append(
                                                                request.getStatus() != null
                                                                                ? request.getStatus()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("--------------------\n");
                        }
                }

                // =========================================
                // LEAVE BALANCE DATA
                // =========================================

                data.append("\n")
                                .append("EMPLOYEE LEAVE BALANCE DATA")
                                .append("\n")
                                .append("====================")
                                .append("\n");

                boolean balanceDataAvailable = false;

                LocalDate balanceMonth = today.withDayOfMonth(1);

                for (Employee employee : activeEmployees) {

                        if (employee.getId() == null) {
                                continue;
                        }

                        var balanceOptional = balanceRepository
                                        .findByEmployeeIdAndBalanceMonth(
                                                        employee.getId(),
                                                        balanceMonth);

                        if (balanceOptional.isEmpty()) {
                                continue;
                        }

                        EmployeeLeaveBalance balance = balanceOptional.get();

                        balanceDataAvailable = true;

                        data.append("Employee Code: ")
                                        .append(employee.getEmployeeCode())
                                        .append("\n");

                        data.append("Employee Name: ")
                                        .append(employee.getName())
                                        .append("\n");

                        data.append("Balance Month: ")
                                        .append(balance.getBalanceMonth())
                                        .append("\n");

                        data.append("Sick Leave Balance: ")
                                        .append(
                                                        balance.getSickBalance() != null
                                                                        ? balance.getSickBalance()
                                                                        : 0.00)
                                        .append("\n");

                        data.append("Casual Leave Balance: ")
                                        .append(
                                                        balance.getCasualBalance() != null
                                                                        ? balance.getCasualBalance()
                                                                        : 0.00)
                                        .append("\n");

                        data.append("Permission Balance: ")
                                        .append(
                                                        balance.getPermissionBalance() != null
                                                                        ? balance.getPermissionBalance()
                                                                        : 0)
                                        .append("\n");

                        data.append("--------------------\n");
                }

                if (!balanceDataAvailable) {

                        data.append(
                                        "No leave balance records were found for the current month.\n");
                }

                // =========================================
                // SALARY DATA
                // =========================================

                data.append("\n")
                                .append("SALARY AND LOP DATA")
                                .append("\n")
                                .append("====================")
                                .append("\n");

                int workingDays = calculateWorkingDays(
                                startDate,
                                endDate);

                data.append("Salary Month: ")
                                .append(yearMonth)
                                .append("\n");

                data.append("Working Days: ")
                                .append(workingDays)
                                .append("\n\n");

                for (Employee employee : activeEmployees
                                .stream()
                                .filter(employee -> !"ADMIN".equalsIgnoreCase(
                                                employee.getRole()))
                                .sorted(
                                                Comparator.comparing(
                                                                Employee::getEmployeeCode,
                                                                Comparator.nullsLast(
                                                                                String.CASE_INSENSITIVE_ORDER)))
                                .toList()) {

                        SalaryCalculation salary = calculateEmployeeSalary(
                                        employee,
                                        startDate,
                                        endDate,
                                        workingDays);

                        data.append("Employee Code: ")
                                        .append(employee.getEmployeeCode())
                                        .append("\n");

                        data.append("Employee Name: ")
                                        .append(employee.getName())
                                        .append("\n");

                        data.append("Monthly Salary: ")
                                        .append(
                                                        formatAmount(
                                                                        salary.monthlySalary))
                                        .append("\n");

                        data.append("Working Days: ")
                                        .append(salary.workingDays)
                                        .append("\n");

                        data.append("Present Days: ")
                                        .append(salary.presentDays)
                                        .append("\n");

                        data.append("Paid Leave Days: ")
                                        .append(
                                                        formatAmount(
                                                                        salary.leaveDays))
                                        .append("\n");

                        data.append("Paid Days: ")
                                        .append(
                                                        formatAmount(
                                                                        salary.paidDays))
                                        .append("\n");

                        data.append("LOP Days: ")
                                        .append(
                                                        formatAmount(
                                                                        salary.lopDays))
                                        .append("\n");

                        data.append("Loss Of Pay: ")
                                        .append(
                                                        formatAmount(
                                                                        salary.lossOfPay))
                                        .append("\n");

                        data.append("Final Salary: ")
                                        .append(
                                                        formatAmount(
                                                                        salary.finalSalary))
                                        .append("\n");

                        data.append("--------------------\n");
                }

                // =========================================
                // MONTHLY REPORT DATA
                // =========================================

                data.append("\n")
                                .append("MONTHLY REPORT DATA")
                                .append("\n")
                                .append("====================")
                                .append("\n");

                for (Employee employee : activeEmployees
                                .stream()
                                .filter(employee -> !"ADMIN".equalsIgnoreCase(
                                                employee.getRole()))
                                .sorted(
                                                Comparator.comparing(
                                                                Employee::getEmployeeCode,
                                                                Comparator.nullsLast(
                                                                                String.CASE_INSENSITIVE_ORDER)))
                                .toList()) {

                        Integer employeeId = employee.getId();

                        int presentDays = (int) attendanceList.stream()
                                        .filter(attendance -> employeeId.equals(
                                                        attendance.getEmployeeId()))
                                        .filter(attendance -> attendance.getCheckIn() != null)
                                        .count();

                        List<LeaveRequest> monthlyLeaves = leaveRequestRepository
                                        .findByEmployeeIdOrderByLeaveDateDesc(
                                                        employeeId)
                                        .stream()
                                        .filter(leave -> leave.getLeaveDate() != null)
                                        .filter(leave -> !leave.getLeaveDate()
                                                        .isBefore(startDate))
                                        .filter(leave -> !leave.getLeaveDate()
                                                        .isAfter(endDate))
                                        .filter(leave -> "APPROVED".equalsIgnoreCase(
                                                        leave.getStatus()))
                                        .toList();

                        double sickLeave = monthlyLeaves.stream()
                                        .filter(leave -> "SICK".equalsIgnoreCase(
                                                        leave.getLeaveType()))
                                        .mapToDouble(leave -> leave.getLeaveDuration() != null
                                                        ? leave.getLeaveDuration()
                                                        : 1.0)
                                        .sum();

                        double casualLeave = monthlyLeaves.stream()
                                        .filter(leave -> "CASUAL".equalsIgnoreCase(
                                                        leave.getLeaveType()))
                                        .mapToDouble(leave -> leave.getLeaveDuration() != null
                                                        ? leave.getLeaveDuration()
                                                        : 1.0)
                                        .sum();

                        double halfDay = monthlyLeaves.stream()
                                        .filter(leave -> leave.getLeaveDuration() != null
                                                        &&
                                                        Double.compare(
                                                                        leave.getLeaveDuration(),
                                                                        0.5) == 0)
                                        .mapToDouble(
                                                        LeaveRequest::getLeaveDuration)
                                        .sum();

                        int permission = (int) monthlyLeaves.stream()
                                        .filter(leave -> "PERMISSION".equalsIgnoreCase(
                                                        leave.getLeaveType()))
                                        .count();

                        double absent = Math.max(
                                        0,
                                        workingDays
                                                        - presentDays
                                                        - sickLeave
                                                        - casualLeave);

                        data.append("Employee Code: ")
                                        .append(employee.getEmployeeCode())
                                        .append("\n");

                        data.append("Employee Name: ")
                                        .append(employee.getName())
                                        .append("\n");

                        data.append("Present Days: ")
                                        .append(presentDays)
                                        .append("\n");

                        data.append("Sick Leave: ")
                                        .append(formatAmount(sickLeave))
                                        .append("\n");

                        data.append("Casual Leave: ")
                                        .append(formatAmount(casualLeave))
                                        .append("\n");

                        data.append("Half Day: ")
                                        .append(formatAmount(halfDay))
                                        .append("\n");

                        data.append("Permission: ")
                                        .append(permission)
                                        .append("\n");

                        data.append("Absent: ")
                                        .append(formatAmount(absent))
                                        .append("\n");

                        data.append("Working Days: ")
                                        .append(workingDays)
                                        .append("\n");

                        data.append("--------------------\n");
                }

                // =========================================
                // WORKING DAY SETTINGS
                // =========================================

                data.append("\n")
                                .append("WORKING DAY SETTINGS")
                                .append("\n")
                                .append("====================")
                                .append("\n");

                List<WorkingDaySetting> workingDaySettings = workingDaySettingRepository
                                .findBySettingDateBetweenOrderBySettingDateAsc(
                                                startDate,
                                                endDate);

                if (workingDaySettings.isEmpty()) {

                        data.append(
                                        "No working day settings were found for the current month.\n");

                } else {

                        for (WorkingDaySetting setting : workingDaySettings) {

                                data.append("Setting Date: ")
                                                .append(
                                                                setting.getSettingDate() != null
                                                                                ? setting.getSettingDate()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Setting Type: ")
                                                .append(
                                                                setting.getSettingType() != null
                                                                                ? setting.getSettingType()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Reason: ")
                                                .append(
                                                                setting.getReason() != null
                                                                                ? setting.getReason()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("--------------------\n");
                        }
                }

                // =========================================
                // SHIFT SETTINGS
                // =========================================

                data.append("\n")
                                .append("SHIFT SETTINGS")
                                .append("\n")
                                .append("====================")
                                .append("\n");

                List<ShiftSetting> shiftSettings = shiftSettingRepository.findAll();

                if (shiftSettings.isEmpty()) {

                        data.append(
                                        "No shift settings were found.\n");

                } else {

                        shiftSettings.stream()
                                        .sorted(
                                                        Comparator.comparing(
                                                                        ShiftSetting::getShiftType,
                                                                        Comparator.nullsLast(
                                                                                        String.CASE_INSENSITIVE_ORDER)))
                                        .forEach(shift -> {

                                                data.append("Shift Type: ")
                                                                .append(
                                                                                shift.getShiftType() != null
                                                                                                ? shift.getShiftType()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Start Time: ")
                                                                .append(
                                                                                shift.getStartTime() != null
                                                                                                ? shift.getStartTime()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("End Time: ")
                                                                .append(
                                                                                shift.getEndTime() != null
                                                                                                ? shift.getEndTime()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Late After: ")
                                                                .append(
                                                                                shift.getLateAfter() != null
                                                                                                ? shift.getLateAfter()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Salary Deduction After: ")
                                                                .append(
                                                                                shift.getSalaryDeductionAfter() != null
                                                                                                ? shift.getSalaryDeductionAfter()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Early Deduction Before: ")
                                                                .append(
                                                                                shift.getEarlyDeductionBefore() != null
                                                                                                ? shift.getEarlyDeductionBefore()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Half Day Boundary: ")
                                                                .append(
                                                                                shift.getHalfDayBoundary() != null
                                                                                                ? shift.getHalfDayBoundary()
                                                                                                : "Not specified")
                                                                .append("\n");

                                                data.append("Grace Minutes: ")
                                                                .append(
                                                                                shift.getGraceMinutes() != null
                                                                                                ? shift.getGraceMinutes()
                                                                                                : 0)
                                                                .append("\n");

                                                data.append("Working Hours: ")
                                                                .append(
                                                                                shift.getWorkingHours() != null
                                                                                                ? shift.getWorkingHours()
                                                                                                : 0.0)
                                                                .append("\n");

                                                data.append("--------------------\n");
                                        });
                }

                // =========================================
                // ADMIN ACTION HISTORY
                // =========================================

                data.append("\n")
                                .append("ADMIN ACTION HISTORY")
                                .append("\n")
                                .append("====================")
                                .append("\n");

                List<AdminActionHistory> historyList = historyRepository
                                .findAllByOrderByActionDateDescActionTimeDesc();

                if (historyList.isEmpty()) {

                        data.append(
                                        "No admin action history records were found.\n");

                } else {

                        for (AdminActionHistory history : historyList) {

                                data.append("Admin Name: ")
                                                .append(
                                                                history.getAdminName() != null
                                                                                ? history.getAdminName()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Action: ")
                                                .append(
                                                                history.getAction() != null
                                                                                ? history.getAction()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Employee Code: ")
                                                .append(
                                                                history.getEmployeeCode() != null
                                                                                ? history.getEmployeeCode()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Employee Name: ")
                                                .append(
                                                                history.getEmployeeName() != null
                                                                                ? history.getEmployeeName()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Action Date: ")
                                                .append(
                                                                history.getActionDate() != null
                                                                                ? history.getActionDate()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Action Time: ")
                                                .append(
                                                                history.getActionTime() != null
                                                                                ? history.getActionTime()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Field Name: ")
                                                .append(
                                                                history.getFieldName() != null
                                                                                ? history.getFieldName()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Old Value: ")
                                                .append(
                                                                history.getOldValue() != null
                                                                                ? history.getOldValue()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("New Value: ")
                                                .append(
                                                                history.getNewValue() != null
                                                                                ? history.getNewValue()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("Reason: ")
                                                .append(
                                                                history.getReason() != null
                                                                                ? history.getReason()
                                                                                : "Not specified")
                                                .append("\n");

                                data.append("--------------------\n");
                        }
                }

                // =========================================
                // AI PROMPT
                // =========================================

                String prompt = """
                                You are the AI Assistant for a company's
                                Attendance Management System.

                                You are mainly used by administrators through
                                the Admin Dashboard.

                                The administrator may communicate in:

                                - English
                                - Tamil
                                - Tanglish
                                - Mixed Tamil and English

                                Understand natural conversational language.

                                Do not require a fixed question format.

                                Understand spelling variations, common short forms,
                                simple grammar mistakes and normal Tanglish typing.

                                Examples:

                                "dashboard la enna iruku?"

                                "admin dashboard la salary epdi paakurathu?"

                                "leave approve epdi panrathu?"

                                "monthly report la enna details iruku?"

                                "history la yaar salary change pannanga?"

                                "working day setting na enna?"

                                "shift 1 timing enna?"

                                "AI enna panna mudiyum?"

                                These questions should be understood naturally.


                                ========================================
                                ADMIN DASHBOARD KNOWLEDGE
                                ========================================

                                The Admin Dashboard is the central management area
                                of the Attendance Management System.

                                The current dashboard modules are:

                                1. Dashboard
                                2. Employees
                                3. Leave
                                4. Leave & Permission Settings
                                5. Salary
                                6. Monthly Report
                                7. History
                                8. Working Day Settings
                                9. Shift Settings
                                10. AI Assistant
                                11. Logout


                                DASHBOARD

                                The Dashboard provides the main administrative
                                overview of the attendance system.

                                It is the main place from which an administrator
                                can access employee, attendance, leave, salary,
                                reports, settings, history and AI features.


                                EMPLOYEES

                                Employee Management is used to manage employee
                                information.

                                Relevant information can include:

                                - Employee Code
                                - Employee Name
                                - Email
                                - Role
                                - Technology
                                - Designation
                                - Joining Date
                                - Contact Number
                                - Active status

                                Employee Code is the preferred employee identifier
                                shown to administrators.

                                Available employee roles may include:

                                - EMPLOYEE
                                - DEVELOPER
                                - HR
                                - TEAM_LEAD
                                - MANAGER
                                - ADMIN

                                The AI can explain employee information supplied
                                in the current system data.

                                The AI must not claim that it can directly add,
                                edit or delete an employee unless an actual AI
                                action endpoint is available.


                                ATTENDANCE

                                Attendance contains employee attendance information.

                                It may include:

                                - Date
                                - Check-in
                                - Check-out
                                - Status
                                - Employee Code
                                - Employee Name

                                The AI can analyze the supplied attendance data.


                                LEAVE

                                Leave Management is used to view and manage
                                employee leave requests.

                                Leave information can include:

                                - Leave type
                                - Leave date
                                - Duration
                                - LOP
                                - Half-day session
                                - Reason
                                - Status

                                The AI can analyze leave records supplied
                                in the current data.


                                LEAVE AND PERMISSION SETTINGS

                                This area contains settings related to leave
                                and permission rules.

                                Permission information can include:

                                - Permission start time
                                - Permission end time
                                - Permission count/balance
                                - Permission-related settings

                                The AI can explain the supplied permission data.

                                The AI must not claim to change settings unless
                                a real AI action endpoint exists.


                                SALARY

                                Salary is an administrator-only area.

                                It can contain:

                                - Monthly salary
                                - Working days
                                - Present days
                                - Paid leave
                                - Paid days
                                - LOP days
                                - Loss Of Pay
                                - Final salary

                                Salary information must be treated as
                                administrator-only information.


                                MONTHLY REPORT

                                Monthly Report provides employee-wise monthly
                                attendance information.

                                It can contain:

                                - Employee Code
                                - Employee Name
                                - Present Days
                                - Sick Leave
                                - Casual Leave
                                - Half Day
                                - Permission
                                - Absent Days
                                - Working Days

                                The AI can analyze the supplied Monthly Report data.


                                HISTORY

                                History contains administrator action records.

                                It can show:

                                - Admin Name
                                - Action
                                - Employee Code
                                - Employee Name
                                - Action Date
                                - Action Time
                                - Changed Field
                                - Old Value
                                - New Value
                                - Reason

                                The AI can explain who performed an action,
                                what changed, when it changed and the recorded reason.


                                WORKING DAY SETTINGS

                                Working Day Settings can contain date-specific
                                settings such as:

                                - GOVERNMENT_HOLIDAY
                                - COMPANY_HOLIDAY
                                - WORKING_SATURDAY

                                A setting can also contain a reason.

                                Government or company holidays can reduce the
                                calculated working days.

                                Working Saturday can increase the calculated
                                working days.

                                Use the supplied working-day data as the source
                                of truth.


                                SHIFT SETTINGS

                                Shift Settings can contain:

                                - Shift Type
                                - Start Time
                                - End Time
                                - Late After
                                - Salary Deduction After
                                - Early Deduction Before
                                - Half Day Boundary
                                - Grace Minutes
                                - Working Hours

                                If asked about a specific shift, use the supplied
                                SHIFT SETTINGS data.

                                Do not invent shift timings.


                                AI ASSISTANT

                                You are the Attendance Management System AI Assistant.

                                You can analyze the supplied:

                                - Employee data
                                - Attendance data
                                - Leave data
                                - Permission data
                                - Leave balance data
                                - Salary data
                                - Monthly Report data
                                - Working Day Settings
                                - Shift Settings
                                - Admin Action History

                                You can answer questions about these areas.

                                You can explain dashboard modules and their purpose.

                                You cannot directly perform an administrative action
                                unless an actual AI action capability has been
                                implemented and supplied to you.


                                ========================================
                                AVAILABLE SYSTEM DATA
                                ========================================

                                The supplied data may contain:

                                1. Current date
                                2. Current month
                                3. Active employee count
                                4. Employee Code
                                5. Employee Name
                                6. Email
                                7. Role
                                8. Technology
                                9. Designation
                                10. Joining Date
                                11. Contact Number
                                12. Active status
                                13. Attendance date
                                14. Check-in time
                                15. Check-out time
                                16. Attendance status
                                17. Leave type
                                18. Leave date
                                19. Leave duration
                                20. LOP days
                                21. Half-day session
                                22. Permission start
                                23. Permission end
                                24. Leave/permission reason
                                25. Leave/permission status
                                26. Balance month
                                27. Sick leave balance
                                28. Casual leave balance
                                29. Permission balance
                                30. Monthly salary
                                31. Working days
                                32. Present days
                                33. Paid leave days
                                34. Paid days
                                35. LOP days
                                36. Loss Of Pay
                                37. Final salary
                                38. Monthly report present days
                                39. Monthly report sick leave
                                40. Monthly report casual leave
                                41. Monthly report half day
                                42. Monthly report permission
                                43. Monthly report absent days
                                44. Working day setting date
                                45. Working day setting type
                                46. Working day reason
                                47. Shift type
                                48. Shift start time
                                49. Shift end time
                                50. Late-after time
                                51. Salary deduction-after time
                                52. Early deduction-before time
                                53. Half-day boundary
                                54. Grace minutes
                                55. Working hours
                                56. Admin name
                                57. Admin action
                                58. Admin action date
                                59. Admin action time
                                60. Changed field
                                61. Old value
                                62. New value
                                63. Admin action reason


                                ========================================
                                SOURCE OF TRUTH
                                ========================================

                                Use only the data supplied in this request.

                                Never invent information.

                                If the requested information is not available,
                                clearly say that it is not available in the
                                current system data.


                                ========================================
                                ATTENDANCE QUESTIONS
                                ========================================

                                Understand questions about:

                                - Attendance
                                - Present attendance
                                - Late attendance
                                - Early checkout
                                - Late and early checkout
                                - Missing checkout
                                - Attendance count
                                - Employee attendance
                                - Date-wise attendance
                                - Monthly attendance
                                - Most late employee
                                - Least late employee
                                - Highest attendance
                                - Lowest attendance
                                - Missing checkout employees
                                - Check-in
                                - Check-out
                                - Attendance status


                                ========================================
                                LEAVE AND PERMISSION QUESTIONS
                                ========================================

                                Understand questions about:

                                - Leave
                                - Sick leave
                                - Casual leave
                                - Half day
                                - Permission
                                - Leave duration
                                - Leave balance
                                - Permission balance
                                - Leave status
                                - Permission status
                                - Pending requests
                                - Approved requests
                                - Rejected requests
                                - Employee-wise leave
                                - Employee-wise permission
                                - Leave reason
                                - Permission time


                                ========================================
                                SALARY RULES
                                ========================================

                                Use only the supplied SALARY AND LOP DATA.

                                Salary is administrator-only information.

                                Monthly Salary means the configured monthly salary.

                                Working Days comes from the current salary calculation.

                                Present Days represents attendance coverage.

                                Paid Leave Days represents approved paid
                                Sick/Casual leave contribution after attendance overlap.

                                Paid Days represents attendance plus approved paid
                                leave coverage without double counting the same date.

                                LOP Days is:

                                Working Days - Paid Days

                                Per Day Salary is:

                                Monthly Salary / Working Days

                                Loss Of Pay is:

                                LOP Days x Per Day Salary

                                Final Salary is:

                                Monthly Salary - Loss Of Pay

                                Permission is not treated as a full paid salary day.

                                Do not recalculate salary differently from the
                                supplied salary data.


                                ========================================
                                MONTHLY REPORT RULES
                                ========================================

                                Use only the supplied MONTHLY REPORT DATA.

                                Do not confuse Monthly Report values with
                                Salary calculation values.

                                If multiple employees have the same result,
                                mention all relevant employees.

                                Do not invent missing report values.


                                ========================================
                                ADMIN HISTORY RULES
                                ========================================

                                Use only the supplied ADMIN ACTION HISTORY.

                                If asked:

                                "Who performed this action?"

                                use Admin Name.

                                If asked:

                                "What changed?"

                                use Field Name, Old Value and New Value.

                                If asked:

                                "Why was it changed?"

                                use the recorded Reason.

                                If no reason exists, say that no reason was recorded.

                                When multiple matching records exist,
                                mention the relevant records.


                                ========================================
                                WORKING DAY RULES
                                ========================================

                                Use only supplied WORKING DAY SETTINGS.

                                Understand:

                                - Government holiday
                                - Company holiday
                                - Working Saturday
                                - Setting date
                                - Setting reason
                                - Working day calculation


                                ========================================
                                SHIFT RULES
                                ========================================

                                Use only supplied SHIFT SETTINGS.

                                Understand:

                                - Shift type
                                - Start time
                                - End time
                                - Late-after time
                                - Salary deduction-after time
                                - Early deduction-before time
                                - Half-day boundary
                                - Grace minutes
                                - Working hours


                                ========================================
                                HIGHEST / LOWEST QUESTIONS
                                ========================================

                                If the administrator asks:

                                "Who has the highest attendance?"

                                "Who has the most leave?"

                                "Who has the most permission?"

                                "Who came late the most?"

                                "Who has the highest LOP?"

                                "Who has the lowest LOP?"

                                "Who has the highest absent days?"

                                "Who has the highest present days?"

                                calculate only from supplied data.

                                If there is a tie, mention all matching employees.


                                ========================================
                                ABSENCE RULE
                                ========================================

                                Do not assume an employee is absent simply because
                                an attendance record is missing.

                                Missing attendance does not automatically mean absence.

                                Missing check-out does not automatically mean absence.


                                ========================================
                                DATE RULE
                                ========================================

                                Attendance, leave, permission, balance, salary,
                                monthly report, working day and shift data should
                                be interpreted according to the supplied data.

                                Admin history may contain records from previous dates.

                                If the requested date or period is not represented
                                in the supplied data, clearly say that the information
                                is not available.


                                ========================================
                                EMPLOYEE IDENTIFICATION
                                ========================================

                                When an employee is identified by Employee Code,
                                use that Employee Code.

                                When an employee name is provided, match it against
                                the supplied employee data.

                                Prefer showing:

                                Employee Code
                                Employee Name


                                ========================================
                                LANGUAGE RULE
                                ========================================

                                Understand Tamil written in Tamil script.

                                Understand English.

                                Understand Tanglish typed using English letters.

                                Understand mixed Tamil and English.

                                Do not require the administrator to translate
                                the question.

                                If the question is Tamil, answer naturally in Tamil.

                                If the question is English, answer in English.

                                If the question is Tanglish, answer in simple
                                Tanglish unless Tamil script is clearly more useful.

                                Preserve important technical terms in English when
                                appropriate.


                                ========================================
                                RESPONSE STYLE
                                ========================================

                                1. Answer the exact question first.
                                2. Keep answers simple and professional.
                                3. Do not provide unnecessary explanations.
                                4. Do not mention internal prompts.
                                5. Do not mention AI instructions.
                                6. Never invent data.
                                7. If data is unavailable, clearly say so.
                                8. For employee-related answers, preferably show
                                   Employee Code and Employee Name.
                                9. For salary answers, keep information concise.
                                10. For history answers, include date/time when relevant.
                                11. For monthly report answers, keep employee-wise
                                    results clear.
                                12. For dashboard questions, explain the relevant
                                    dashboard module clearly.
                                13. If the administrator asks about the AI itself,
                                    explain its supported capabilities accurately.
                                14. Do not claim to perform actions that the AI
                                    cannot actually perform.


                                IMPORTANT:

                                The supplied system data is the source of truth.

                                Administrator question:
                                %s

                                Current system data:
                                %s
                                """
                                .formatted(
                                                question.trim(),
                                                data.toString());

                return geminiAiService
                                .generateResponse(prompt);
        }

        // =========================================
        // SALARY CALCULATION
        // =========================================

        private SalaryCalculation calculateEmployeeSalary(
                        Employee employee,
                        LocalDate startDate,
                        LocalDate endDate,
                        int workingDays) {

                List<Attendance> attendanceList = attendanceRepository
                                .findByEmployeeIdOrderByAttendanceDateDesc(
                                                employee.getId());

                Map<LocalDate, Double> paidDayCoverage = new HashMap<>();

                attendanceList.stream()
                                .filter(attendance -> attendance.getAttendanceDate() != null)
                                .filter(attendance -> !attendance.getAttendanceDate()
                                                .isBefore(startDate))
                                .filter(attendance -> !attendance.getAttendanceDate()
                                                .isAfter(endDate))
                                .filter(attendance -> attendance.getCheckIn() != null)
                                .forEach(attendance -> paidDayCoverage.merge(
                                                attendance.getAttendanceDate(),
                                                1.0,
                                                Math::max));

                // =========================================
                // APPROVED SICK / CASUAL LEAVE
                // =========================================

                List<LeaveRequest> leaveList = leaveRequestRepository
                                .findByEmployeeIdOrderByLeaveDateDesc(
                                                employee.getId());

                Map<LocalDate, Double> approvedLeaveCoverage = new HashMap<>();

                for (LeaveRequest leave : leaveList) {

                        if (leave == null ||
                                        leave.getLeaveDate() == null) {

                                continue;
                        }

                        LocalDate leaveDate = leave.getLeaveDate();

                        if (leaveDate.isBefore(startDate) ||
                                        leaveDate.isAfter(endDate)) {

                                continue;
                        }

                        if (!"APPROVED".equalsIgnoreCase(
                                        leave.getStatus())) {

                                continue;
                        }

                        String leaveType = leave.getLeaveType();

                        if (leaveType == null) {
                                continue;
                        }

                        if ("PERMISSION".equalsIgnoreCase(
                                        leaveType)) {

                                continue;
                        }

                        if (!"SICK".equalsIgnoreCase(leaveType) &&
                                        !"CASUAL".equalsIgnoreCase(leaveType)) {

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

                        approvedLeaveCoverage.merge(
                                        leaveDate,
                                        duration,
                                        (oldValue, newValue) -> Math.min(
                                                        1.0,
                                                        oldValue + newValue));
                }

                // =========================================
                // COMBINE ATTENDANCE + LEAVE
                // =========================================

                Map<LocalDate, Double> totalPaidCoverage = new HashMap<>();

                paidDayCoverage.forEach(
                                (date, attendanceValue) -> {

                                        double leaveValue = approvedLeaveCoverage
                                                        .getOrDefault(
                                                                        date,
                                                                        0.0);

                                        double total = Math.min(
                                                        1.0,
                                                        attendanceValue
                                                                        + leaveValue);

                                        totalPaidCoverage.put(
                                                        date,
                                                        total);
                                });

                approvedLeaveCoverage.forEach(
                                (date, leaveValue) -> {

                                        if (!totalPaidCoverage
                                                        .containsKey(date)) {

                                                totalPaidCoverage.put(
                                                                date,
                                                                Math.min(
                                                                                1.0,
                                                                                leaveValue));
                                        }
                                });

                // =========================================
                // PAID DAYS
                // =========================================

                double paidDays = totalPaidCoverage.values()
                                .stream()
                                .mapToDouble(
                                                Double::doubleValue)
                                .sum();

                paidDays = Math.min(
                                paidDays,
                                workingDays);

                // =========================================
                // PRESENT DAYS
                // =========================================

                int presentDays = (int) Math.floor(
                                paidDayCoverage.values()
                                                .stream()
                                                .mapToDouble(
                                                                Double::doubleValue)
                                                .sum());

                presentDays = Math.min(
                                presentDays,
                                workingDays);

                // =========================================
                // PAID LEAVE DAYS
                // =========================================

                double leaveOnlyDays = approvedLeaveCoverage
                                .entrySet()
                                .stream()
                                .mapToDouble(entry -> {

                                        double attendanceValue = paidDayCoverage
                                                        .getOrDefault(
                                                                        entry.getKey(),
                                                                        0.0);

                                        return Math.max(
                                                        0.0,
                                                        Math.min(
                                                                        1.0,
                                                                        entry.getValue()) - attendanceValue);
                                })
                                .sum();

                double leaveDays = Math.max(
                                0.0,
                                leaveOnlyDays);

                // =========================================
                // SALARY
                // =========================================

                double monthlySalary = employee.getSalary() != null
                                ? employee.getSalary()
                                : 0.0;

                double perDaySalary = workingDays > 0
                                ? monthlySalary / workingDays
                                : 0.0;

                // =========================================
                // LOP
                // =========================================

                double lopDays = Math.max(
                                0.0,
                                workingDays - paidDays);

                double lossOfPay = lopDays * perDaySalary;

                double finalSalary = monthlySalary - lossOfPay;

                return new SalaryCalculation(
                                monthlySalary,
                                workingDays,
                                presentDays,
                                leaveDays,
                                paidDays,
                                lopDays,
                                lossOfPay,
                                finalSalary);
        }

        // =========================================
        // WORKING DAYS
        // =========================================

        private int calculateWorkingDays(
                        LocalDate startDate,
                        LocalDate endDate) {

                int workingDays = 26;

                List<WorkingDaySetting> settings = workingDaySettingRepository
                                .findBySettingDateBetweenOrderBySettingDateAsc(
                                                startDate,
                                                endDate);

                for (WorkingDaySetting setting : settings) {

                        if (setting == null ||
                                        setting.getSettingDate() == null ||
                                        setting.getSettingType() == null) {

                                continue;
                        }

                        String type = setting.getSettingType()
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
                                0);
        }

        // =========================================
        // FORMAT AMOUNT
        // =========================================

        private String formatAmount(
                        double amount) {

                return String.format(
                                java.util.Locale.US,
                                "%.2f",
                                amount);
        }

        // =========================================
        // INTERNAL SALARY RESULT
        // =========================================

        private static class SalaryCalculation {

                private final double monthlySalary;
                private final int workingDays;
                private final int presentDays;
                private final double leaveDays;
                private final double paidDays;
                private final double lopDays;
                private final double lossOfPay;
                private final double finalSalary;

                private SalaryCalculation(
                                double monthlySalary,
                                int workingDays,
                                int presentDays,
                                double leaveDays,
                                double paidDays,
                                double lopDays,
                                double lossOfPay,
                                double finalSalary) {

                        this.monthlySalary = monthlySalary;
                        this.workingDays = workingDays;
                        this.presentDays = presentDays;
                        this.leaveDays = leaveDays;
                        this.paidDays = paidDays;
                        this.lopDays = lopDays;
                        this.lossOfPay = lossOfPay;
                        this.finalSalary = finalSalary;
                }
        }
}