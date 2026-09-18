package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/leaves")
public class AdminLeaveController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;
    private final AdminActionHistoryRepository historyRepository;

    public AdminLeaveController(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            AdminRepository adminRepository,
            AdminActionHistoryRepository historyRepository) {

        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.adminRepository = adminRepository;
        this.historyRepository = historyRepository;
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
                            leave.getHalfDaySession(),
                            leave.getPermissionStart(),
                            leave.getPermissionEnd(),
                            leave.getReason(),
                            leave.getStatus(),
                            leave.getCreatedAt()
                    );
                })
                .filter(response -> response != null)
                .toList();
    }

    // =========================================
    // APPROVE / REJECT USING ADMIN PASSWORD
    // =========================================

    @PutMapping("/{id}/status")
    public String updateLeaveStatus(
            @PathVariable Integer id,
            @RequestBody AdminLeaveStatusRequest request) {

        // -----------------------------------------
        // FIND LEAVE
        // -----------------------------------------

        LeaveRequest leave =
                leaveRequestRepository.findById(id)
                        .orElse(null);

        if (leave == null) {
            return "Leave Request Not Found";
        }

        // -----------------------------------------
        // CHECK CURRENT STATUS
        // -----------------------------------------

        if (!"PENDING".equalsIgnoreCase(
                leave.getStatus())) {

            return "Leave Request Already Processed";
        }

        // -----------------------------------------
        // ADMIN EMAIL / PASSWORD VALIDATION
        // -----------------------------------------

        if (request.getAdminEmail() == null ||
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
        // UPDATE STATUS
        // -----------------------------------------

        leave.setStatus(status);

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

            history.setActionDate(
                    java.time.LocalDate.now()
            );

            history.setActionTime(
                    java.time.LocalTime.now()
            );

            historyRepository.save(history);
        }

        // -----------------------------------------
        // RESPONSE
        // -----------------------------------------

        if (status.equals("APPROVED")) {

            return "Leave Approved Successfully";

        } else {

            return "Leave Rejected Successfully";
        }
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

        leave.setStatus("REJECTED");

        leaveRequestRepository.save(leave);

        return "Leave Rejected Successfully";
    }
}