package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Admin leave review used by the dashboard.
 *
 * - Admin is already logged in, so only adminEmail is needed
 * (same as Salary update). No admin password.
 * - Partial decision: some dates APPROVED, some REJECTED.
 * - The reason is saved in decision_reason, so the employee can see it
 * in /leave/history.
 * - A rejected date gives the sick/casual balance back.
 */
@RestController
@RequestMapping("/admin/leaves/review")
public class AdminLeaveReviewController {

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;
    private final AdminActionHistoryRepository historyRepository;
    private final EmployeeLeaveBalanceRepository balanceRepository;

    public AdminLeaveReviewController(
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

    public record GroupReviewRequest(
            String adminEmail,
            List<LocalDate> approvedDates,
            List<LocalDate> rejectedDates,
            String reason) {
    }

    public record RowReviewRequest(
            String adminEmail,
            String status,
            String reason) {
    }

    // =========================================
    // ONE LEAVE REQUEST (many dates)
    // =========================================

    @PutMapping("/group/{groupId}")
    @Transactional
    public String reviewGroup(
            @PathVariable String groupId,
            @RequestBody GroupReviewRequest request) {

        Admin admin = findAdmin(request == null ? null : request.adminEmail());

        List<LocalDate> approved = request.approvedDates() != null
                ? request.approvedDates()
                : List.of();

        List<LocalDate> rejected = request.rejectedDates() != null
                ? request.rejectedDates()
                : List.of();

        if (approved.isEmpty() && rejected.isEmpty()) {
            throw bad("Select at least one date");
        }

        String reason = clean(request.reason());

        if (!rejected.isEmpty() && reason.length() < 3) {
            throw bad("Reason is required to reject a date");
        }

        List<LeaveRequest> rows = leaveRequestRepository.findByRequestGroupId(groupId);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Leave request not found");
        }

        LocalDateTime now = LocalDateTime.now(INDIA_ZONE);

        int approvedCount = 0;
        int rejectedCount = 0;

        for (LeaveRequest row : rows) {

            if (!"PENDING".equalsIgnoreCase(row.getStatus())) {
                continue;
            }

            LocalDate date = row.getLeaveDate();

            if (rejected.contains(date)) {

                restoreLeaveBalance(row);

                row.setStatus("REJECTED");
                row.setDecisionReason(reason);
                row.setDecidedAt(now);
                leaveRequestRepository.save(row);
                rejectedCount++;

            } else if (approved.contains(date)) {

                row.setStatus("APPROVED");
                row.setDecisionReason(reason.isEmpty() ? null : reason);
                row.setDecidedAt(now);
                leaveRequestRepository.save(row);
                approvedCount++;
            }
        }

        if (approvedCount == 0 && rejectedCount == 0) {
            throw bad("No pending rows found for the selected dates");
        }

        saveHistory(
                admin,
                rows.get(0).getEmployeeId(),
                rejectedCount == 0
                        ? "LEAVE APPROVED"
                        : approvedCount == 0
                                ? "LEAVE REJECTED"
                                : "LEAVE PARTIALLY APPROVED",
                reason);

        return "Leave decision saved successfully";
    }

    // =========================================
    // OLD ROWS WITHOUT request_group_id
    // =========================================

    @PutMapping("/row/{id}")
    @Transactional
    public String reviewRow(
            @PathVariable Integer id,
            @RequestBody RowReviewRequest request) {

        Admin admin = findAdmin(request == null ? null : request.adminEmail());

        LeaveRequest leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Leave request not found"));

        if (!"PENDING".equalsIgnoreCase(leave.getStatus())) {
            throw bad("Leave request already processed");
        }

        String status = request.status() == null
                ? ""
                : request.status().trim().toUpperCase();

        if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw bad("Invalid leave status");
        }

        String reason = clean(request.reason());

        if (status.equals("REJECTED")) {

            if (reason.length() < 3) {
                throw bad("Reason is required to reject");
            }

            restoreLeaveBalance(leave);
        }

        leave.setStatus(status);
        leave.setDecisionReason(reason.isEmpty() ? null : reason);
        leave.setDecidedAt(LocalDateTime.now(INDIA_ZONE));
        leaveRequestRepository.save(leave);

        saveHistory(
                admin,
                leave.getEmployeeId(),
                status.equals("APPROVED")
                        ? "LEAVE APPROVED"
                        : "LEAVE REJECTED",
                reason);

        return "Leave decision saved successfully";
    }

    // =========================================
    // HELPERS
    // =========================================

    private Admin findAdmin(String email) {

        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Admin login required");
        }

        return adminRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Logged-in admin not found"));
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void saveHistory(
            Admin admin,
            Integer employeeId,
            String action,
            String reason) {

        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        if (employee == null) {
            return;
        }

        AdminActionHistory history = new AdminActionHistory();

        history.setAdminName(admin.getAdminName());
        history.setAction(action);
        history.setEmployeeId(employee.getId());
        history.setEmployeeCode(employee.getEmployeeCode());
        history.setEmployeeName(employee.getName());
        history.setReason(reason);
        history.setActionDate(LocalDate.now(INDIA_ZONE));
        history.setActionTime(LocalTime.now(INDIA_ZONE));

        historyRepository.save(history);
    }

    // Only SICK / CASUAL use balance. LOP and PERMISSION do not.
    private void restoreLeaveBalance(LeaveRequest leave) {

        String type = leave.getLeaveType();

        if (type == null
                || (!type.equalsIgnoreCase("SICK")
                        && !type.equalsIgnoreCase("CASUAL"))) {
            return;
        }

        if (leave.getEmployeeId() == null
                || leave.getLeaveDate() == null) {
            return;
        }

        var optional = balanceRepository.findByEmployeeIdAndBalanceMonth(
                leave.getEmployeeId(),
                leave.getLeaveDate().withDayOfMonth(1));

        if (optional.isEmpty()) {
            return;
        }

        EmployeeLeaveBalance balance = optional.get();

        double duration = leave.getLeaveDuration() != null
                ? leave.getLeaveDuration()
                : 1.0;

        if (type.equalsIgnoreCase("SICK")) {

            balance.setSickBalance(
                    (balance.getSickBalance() != null
                            ? balance.getSickBalance()
                            : 0.0)
                            + duration);

        } else {

            balance.setCasualBalance(
                    (balance.getCasualBalance() != null
                            ? balance.getCasualBalance()
                            : 0.0)
                            + duration);
        }

        balance.setUpdatedAt(LocalDateTime.now(INDIA_ZONE));

        balanceRepository.save(balance);
    }
}