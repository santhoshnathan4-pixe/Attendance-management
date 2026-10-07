package com.example.attendance;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/admin/leaves")
public class AdminLeaveController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;
    private final AdminActionHistoryRepository historyRepository;
    private final EmployeeLeaveBalanceRepository balanceRepository;

    public AdminLeaveController(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            AdminRepository adminRepository,
            AdminActionHistoryRepository historyRepository,
            EmployeeLeaveBalanceRepository balanceRepository) {

        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.adminRepository = adminRepository;
        this.historyRepository = historyRepository;
        this.balanceRepository = balanceRepository;
    }

    // =========================================
    // GET ALL LEAVE REQUESTS
    // =========================================

    @GetMapping
    public List<AdminLeaveResponse> getAllLeaves() {

        return leaveRequestRepository.findAll()
                .stream()
                .map(leave -> {

                    Employee employee =
                            employeeRepository
                                    .findById(leave.getEmployeeId())
                                    .orElse(null);

                    if (employee == null) {
                        return null;
                    }

                    return new AdminLeaveResponse(
                            leave.getId(),
                            employee.getId(),
                            employee.getEmployeeCode(),
                            employee.getName(),
                            leave.getLeaveType(),
                            leave.getLeaveDate(),
                            leave.getLeaveDuration(),
                            leave.getLopDays(),
                            leave.getHalfDaySession(),
                            leave.getPermissionStart(),
                            leave.getPermissionEnd(),
                            leave.getReason(),
                            leave.getStatus(),
                            leave.getCreatedAt(),
                            leave.getRequestGroupId(),
                            leave.getDecisionReason()
                    );
                })
                .filter(response -> response != null)
                .toList();
    }

    // =========================================
    // APPROVE / REJECT USING ADMIN PASSWORD
    // (SINGLE ROW)
    // =========================================

    @PutMapping("/{id}/status")
    public String updateLeaveStatus(
            @PathVariable Integer id,
            @RequestBody AdminLeaveStatusRequest request) {

        LeaveRequest leave =
                leaveRequestRepository.findById(id)
                        .orElse(null);

        if (leave == null) {
            return "Leave Request Not Found";
        }

        if (!"PENDING".equalsIgnoreCase(
                leave.getStatus())) {

            return "Leave Request Already Processed";
        }

        // -----------------------------------------
        // ADMIN EMAIL / PASSWORD VALIDATION
        // -----------------------------------------

        if (request == null ||
                request.getAdminEmail() == null ||
                request.getAdminPassword() == null ||
                request.getAdminEmail().isBlank() ||
                request.getAdminPassword().isBlank()) {

            return "Admin Email and Password Required";
        }

        Admin admin =
                adminRepository
                        .findByEmailAndPassword(
                                request.getAdminEmail(),
                                request.getAdminPassword()
                        )
                        .orElse(null);

        if (admin == null) {
            return "Invalid Admin Email or Password";
        }

        // -----------------------------------------
        // STATUS VALIDATION
        // -----------------------------------------

        String status =
                request.getStatus();

        if (status == null ||
                (!status.equalsIgnoreCase("APPROVED")
                        &&
                 !status.equalsIgnoreCase("REJECTED"))) {

            return "Invalid Leave Status";
        }

        status = status.toUpperCase();

        // -----------------------------------------
        // REASON (REQUIRED WHEN REJECTING)
        // -----------------------------------------

        String reason =
                request.getReason() != null
                        ? request.getReason().trim()
                        : "";

        if (status.equals("REJECTED") &&
                reason.length() < 3) {

            return "Reason Is Required To Reject";
        }

        // -----------------------------------------
        // RESTORE BALANCE WHEN REJECTED
        // -----------------------------------------

        if (status.equals("REJECTED")) {
            restoreLeaveBalance(leave);
        }

        // -----------------------------------------
        // UPDATE STATUS
        // -----------------------------------------

        leave.setStatus(status);

        leave.setDecisionReason(
                reason.isEmpty() ? null : reason
        );

        leave.setDecidedAt(LocalDateTime.now());

        leaveRequestRepository.save(leave);

        // -----------------------------------------
        // EMPLOYEE DETAILS FOR HISTORY
        // -----------------------------------------

        Employee employee =
                employeeRepository
                        .findById(leave.getEmployeeId())
                        .orElse(null);

        // -----------------------------------------
        // SAVE ADMIN HISTORY
        // -----------------------------------------

        if (employee != null) {

            AdminActionHistory history =
                    new AdminActionHistory();

            history.setAdminName(
                    admin.getAdminName()
            );

            history.setAction(
                    status.equals("APPROVED")
                            ? "LEAVE APPROVED"
                            : "LEAVE REJECTED"
            );

            history.setEmployeeId(
                    employee.getId()
            );

            history.setEmployeeCode(
                    employee.getEmployeeCode()
            );

            history.setEmployeeName(
                    employee.getName()
            );

            history.setReason(reason);

            // =========================================
            // INDIA STANDARD TIME (IST)
            // =========================================

            ZoneId indiaZone =
                    ZoneId.of("Asia/Kolkata");

            history.setActionDate(
                    LocalDate.now(indiaZone)
            );

            history.setActionTime(
                    LocalTime.now(indiaZone)
            );

            historyRepository.save(history);
        }

        // -----------------------------------------
        // RESPONSE
        // -----------------------------------------

        if (status.equals("APPROVED")) {
            return "Leave Approved Successfully";
        }

        return "Leave Rejected Successfully";
    }

    // =========================================
    // GROUP DECISION
    // PARTIAL APPROVE / CANCEL SOME DATES
    // =========================================

    @PutMapping("/group/{groupId}/decision")
    @Transactional
    public String decideGroup(
            @PathVariable String groupId,
            @RequestBody AdminLeaveGroupDecisionRequest request) {

        if (request == null ||
                request.adminEmail() == null ||
                request.adminPassword() == null ||
                request.adminEmail().isBlank() ||
                request.adminPassword().isBlank()) {

            return "Admin Email and Password Required";
        }

        Admin admin =
                adminRepository
                        .findByEmailAndPassword(
                                request.adminEmail(),
                                request.adminPassword()
                        )
                        .orElse(null);

        if (admin == null) {
            return "Invalid Admin Email or Password";
        }

        List<LocalDate> approved =
                request.approvedDates() != null
                        ? request.approvedDates()
                        : List.of();

        List<LocalDate> cancelled =
                request.cancelledDates() != null
                        ? request.cancelledDates()
                        : List.of();

        if (approved.isEmpty() && cancelled.isEmpty()) {
            return "Select At Least One Date";
        }

        String reason =
                request.reason() != null
                        ? request.reason().trim()
                        : "";

        if (!cancelled.isEmpty() && reason.length() < 3) {
            return "Reason Is Required To Cancel Dates";
        }

        List<LeaveRequest> rows =
                leaveRequestRepository
                        .findByRequestGroupId(groupId);

        if (rows.isEmpty()) {
            return "Leave Request Not Found";
        }

        ZoneId indiaZone =
                ZoneId.of("Asia/Kolkata");

        LocalDateTime now =
                LocalDateTime.now(indiaZone);

        int approvedCount = 0;
        int cancelledCount = 0;

        for (LeaveRequest row : rows) {

            if (!"PENDING".equalsIgnoreCase(row.getStatus())) {
                continue;
            }

            LocalDate date = row.getLeaveDate();

            if (cancelled.contains(date)) {

                restoreLeaveBalance(row);

                row.setStatus("CANCELLED");
                row.setDecisionReason(reason);
                row.setDecidedAt(now);

                leaveRequestRepository.save(row);
                cancelledCount++;

            } else if (approved.contains(date)) {

                row.setStatus("APPROVED");
                row.setDecisionReason(
                        reason.isEmpty() ? null : reason
                );
                row.setDecidedAt(now);

                leaveRequestRepository.save(row);
                approvedCount++;
            }
        }

        if (approvedCount == 0 && cancelledCount == 0) {
            return "No Pending Rows Found For Selected Dates";
        }

        // -----------------------------------------
        // SAVE ADMIN HISTORY (ONE ROW PER DECISION)
        // -----------------------------------------

        Employee employee =
                employeeRepository
                        .findById(rows.get(0).getEmployeeId())
                        .orElse(null);

        if (employee != null) {

            AdminActionHistory history =
                    new AdminActionHistory();

            history.setAdminName(
                    admin.getAdminName()
            );

            history.setAction(
                    cancelledCount == 0
                            ? "LEAVE APPROVED"
                            : approvedCount == 0
                                    ? "LEAVE CANCELLED"
                                    : "LEAVE PARTIALLY APPROVED"
            );

            history.setEmployeeId(
                    employee.getId()
            );

            history.setEmployeeCode(
                    employee.getEmployeeCode()
            );

            history.setEmployeeName(
                    employee.getName()
            );

            history.setReason(reason);

            history.setActionDate(
                    LocalDate.now(indiaZone)
            );

            history.setActionTime(
                    LocalTime.now(indiaZone)
            );

            historyRepository.save(history);
        }

        return "Leave Decision Saved Successfully";
    }

    // =========================================
    // RESTORE SICK / CASUAL BALANCE
    // =========================================

    private void restoreLeaveBalance(
            LeaveRequest leave) {

        String leaveType =
                leave.getLeaveType();

        // -----------------------------------------
        // ONLY SICK / CASUAL USE BALANCE
        // -----------------------------------------

        if (leaveType == null ||
                (!leaveType.equalsIgnoreCase("SICK")
                        &&
                 !leaveType.equalsIgnoreCase("CASUAL"))) {

            return;
        }

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

        // -----------------------------------------
        // RESTORE SICK BALANCE
        // -----------------------------------------

        if (leaveType.equalsIgnoreCase("SICK")) {

            double currentBalance =
                    balance.getSickBalance() != null
                            ? balance.getSickBalance()
                            : 0.0;

            balance.setSickBalance(
                    currentBalance + duration
            );
        }

        // -----------------------------------------
        // RESTORE CASUAL BALANCE
        // -----------------------------------------

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
                LocalDateTime.now()
        );

        balanceRepository.save(balance);
    }

    // =========================================
    // OLD APPROVE API
    // =========================================

    @PutMapping("/{id}/approve")
    public String approveLeave(@PathVariable Integer id) {

        LeaveRequest leave =
                leaveRequestRepository.findById(id)
                        .orElse(null);

        if (leave == null) {
            return "Leave Request Not Found";
        }

        if (!"PENDING".equalsIgnoreCase(
                leave.getStatus())) {

            return "Leave Request Already Processed";
        }

        leave.setStatus("APPROVED");

        leaveRequestRepository.save(leave);

        return "Leave Approved Successfully";
    }

    // =========================================
    // OLD REJECT API
    // =========================================

    @PutMapping("/{id}/reject")
    public String rejectLeave(@PathVariable Integer id) {

        LeaveRequest leave =
                leaveRequestRepository.findById(id)
                        .orElse(null);

        if (leave == null) {
            return "Leave Request Not Found";
        }

        if (!"PENDING".equalsIgnoreCase(
                leave.getStatus())) {

            return "Leave Request Already Processed";
        }

        // -----------------------------------------
        // RESTORE BALANCE BEFORE REJECT
        // -----------------------------------------

        restoreLeaveBalance(leave);

        leave.setStatus("REJECTED");

        leaveRequestRepository.save(leave);

        return "Leave Rejected Successfully";
    }
}