package com.example.attendance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public AdminDashboardController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }


    // =====================================================
    // DASHBOARD STATS
    // =====================================================

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {

        Map<String, Object> stats = new HashMap<>();

        LocalDate today = LocalDate.now();

        // All active employees
        List<Employee> employees = getActiveEmployees();

        int totalEmployees = employees.size();

        Map<Integer, List<Attendance>> attendanceByEmployee =
                getTodayAttendanceByEmployee(today);

        int presentToday = 0;
        int lateToday = 0;
        int earlyCheckoutToday = 0;

        for (Employee employee : employees) {

            List<Attendance> attendanceList =
                    attendanceByEmployee.get(employee.getId());

            if (attendanceList == null || attendanceList.isEmpty()) {
                continue;
            }

            // Employee has attendance today
            presentToday++;

            boolean late = false;
            boolean earlyCheckout = false;

            for (Attendance attendance : attendanceList) {

                String status = attendance.getStatus();

                if (status == null) {
                    continue;
                }

                String upperStatus =
                        status.toUpperCase();

                if (upperStatus.startsWith("LATE")) {
                    late = true;
                }

                if (upperStatus.startsWith("EARLY")) {
                    earlyCheckout = true;
                }
            }

            if (late) {
                lateToday++;
            }

            if (earlyCheckout) {
                earlyCheckoutToday++;
            }
        }

        int absentToday =
                totalEmployees - presentToday;


        // Pending leave count
        long pendingLeaves =
                leaveRequestRepository
                        .findAll()
                        .stream()
                        .filter(leave ->
                                leave.getLeaveDate() != null
                                        && today.equals(
                                        leave.getLeaveDate())
                                        && "PENDING".equalsIgnoreCase(
                                        leave.getStatus()))
                        .count();


        stats.put(
                "totalEmployees",
                totalEmployees
        );

        stats.put(
                "presentToday",
                presentToday
        );

        stats.put(
                "lateToday",
                lateToday
        );

        stats.put(
                "earlyCheckoutToday",
                earlyCheckoutToday
        );

        stats.put(
                "absentToday",
                Math.max(absentToday, 0)
        );

        stats.put(
                "pendingLeaves",
                pendingLeaves
        );

        return stats;
    }


    // =====================================================
    // DASHBOARD ATTENDANCE DETAILS
    //
    // type:
    // PRESENT
    // LATE
    // EARLY
    // ABSENT
    // =====================================================

    @GetMapping("/details")
    public List<Map<String, Object>> getDashboardDetails(
            @RequestParam String type) {

        LocalDate today = LocalDate.now();

        List<Map<String, Object>> employees =
                new ArrayList<>();

        List<Employee> activeEmployees =
                getActiveEmployees();

        Map<Integer, List<Attendance>> attendanceByEmployee =
                getTodayAttendanceByEmployee(today);


        // =================================================
        // PRESENT
        // =================================================

        if ("PRESENT".equalsIgnoreCase(type)) {

            for (Employee employee : activeEmployees) {

                List<Attendance> attendanceList =
                        attendanceByEmployee.get(
                                employee.getId()
                        );

                if (attendanceList == null
                        || attendanceList.isEmpty()) {

                    continue;
                }

                Map<String, Object> data =
                        createEmployeeData(employee);

                Attendance firstAttendance =
                        attendanceList.get(0);

                data.put(
                        "checkIn",
                        firstAttendance.getCheckIn()
                );

                data.put(
                        "checkOut",
                        getLatestCheckOut(
                                attendanceList
                        )
                );

                data.put(
                        "status",
                        getOverallStatus(
                                attendanceList
                        )
                );

                employees.add(data);
            }
        }


        // =================================================
        // LATE
        // =================================================

        else if ("LATE".equalsIgnoreCase(type)) {

            for (Employee employee : activeEmployees) {

                List<Attendance> attendanceList =
                        attendanceByEmployee.get(
                                employee.getId()
                        );

                if (attendanceList == null
                        || attendanceList.isEmpty()) {

                    continue;
                }

                Attendance lateAttendance =
                        null;

                for (Attendance attendance :
                        attendanceList) {

                    String status =
                            attendance.getStatus();

                    if (status != null
                            && status
                            .toUpperCase()
                            .startsWith("LATE")) {

                        lateAttendance =
                                attendance;

                        break;
                    }
                }

                if (lateAttendance == null) {
                    continue;
                }

                Map<String, Object> data =
                        createEmployeeData(employee);

                data.put(
                        "checkIn",
                        lateAttendance.getCheckIn()
                );

                data.put(
                        "checkOut",
                        getLatestCheckOut(
                                attendanceList
                        )
                );

                data.put(
                        "status",
                        lateAttendance.getStatus()
                );

                employees.add(data);
            }
        }


        // =================================================
        // EARLY CHECKOUT
        // =================================================

        else if ("EARLY".equalsIgnoreCase(type)) {

            for (Employee employee : activeEmployees) {

                List<Attendance> attendanceList =
                        attendanceByEmployee.get(
                                employee.getId()
                        );

                if (attendanceList == null
                        || attendanceList.isEmpty()) {

                    continue;
                }

                Attendance earlyAttendance =
                        null;

                for (Attendance attendance :
                        attendanceList) {

                    String status =
                            attendance.getStatus();

                    if (status != null
                            && status
                            .toUpperCase()
                            .startsWith("EARLY")) {

                        earlyAttendance =
                                attendance;

                        break;
                    }
                }

                if (earlyAttendance == null) {
                    continue;
                }

                Map<String, Object> data =
                        createEmployeeData(employee);

                data.put(
                        "checkIn",
                        earlyAttendance.getCheckIn()
                );

                data.put(
                        "checkOut",
                        earlyAttendance.getCheckOut()
                );

                data.put(
                        "status",
                        earlyAttendance.getStatus()
                );

                employees.add(data);
            }
        }


        // =================================================
        // ABSENT
        // =================================================

        else if ("ABSENT".equalsIgnoreCase(type)) {

            List<LeaveRequest> todayLeaves =
                    leaveRequestRepository
                            .findAll()
                            .stream()
                            .filter(leave ->
                                    leave.getLeaveDate() != null
                                            && today.equals(
                                            leave.getLeaveDate()))
                            .toList();


            for (Employee employee :
                    activeEmployees) {

                List<Attendance> attendanceList =
                        attendanceByEmployee.get(
                                employee.getId()
                        );

                if (attendanceList != null
                        && !attendanceList.isEmpty()) {

                    continue;
                }


                Map<String, Object> data =
                        createEmployeeData(employee);

                data.put(
                        "attendanceStatus",
                        "ABSENT"
                );


                LeaveRequest leaveRequest =
                        todayLeaves
                                .stream()
                                .filter(leave ->
                                        employee.getId()
                                                .equals(
                                                        leave.getEmployeeId()))
                                .findFirst()
                                .orElse(null);


                if (leaveRequest != null) {

                    data.put(
                            "leaveRequestId",
                            leaveRequest.getId()
                    );

                    data.put(
                            "leaveType",
                            leaveRequest.getLeaveType()
                    );

                    data.put(
                            "leaveReason",
                            leaveRequest.getReason()
                    );

                    data.put(
                            "leaveStatus",
                            leaveRequest.getStatus()
                    );

                } else {

                    data.put(
                            "leaveRequestId",
                            null
                    );

                    data.put(
                            "leaveType",
                            null
                    );

                    data.put(
                            "leaveReason",
                            null
                    );

                    data.put(
                            "leaveStatus",
                            "NO_LEAVE_REQUEST"
                    );
                }

                employees.add(data);
            }
        }


        return employees;
    }


    // =====================================================
    // ACTIVE EMPLOYEES
    // =====================================================

    private List<Employee> getActiveEmployees() {

        return employeeRepository
                .findByActiveTrue()
                .stream()
                .toList();
    }


    // =====================================================
    // TODAY ATTENDANCE GROUPED BY EMPLOYEE
    // =====================================================

    private Map<Integer, List<Attendance>>
    getTodayAttendanceByEmployee(
            LocalDate date) {

        List<Attendance> attendanceList =
                attendanceRepository
                        .findByAttendanceDateOrderByAttendanceDateDesc(
                                date
                        );

        Map<Integer, List<Attendance>> result =
                new HashMap<>();


        for (Attendance attendance :
                attendanceList) {

            if (attendance.getEmployeeId() == null) {
                continue;
            }

            result.computeIfAbsent(
                    attendance.getEmployeeId(),
                    key -> new ArrayList<>()
            ).add(attendance);
        }

        return result;
    }


    // =====================================================
    // EMPLOYEE BASIC DATA
    // =====================================================

    private Map<String, Object>
    createEmployeeData(
            Employee employee) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "employeeId",
                employee.getId()
        );

        data.put(
                "employeeCode",
                employee.getEmployeeCode()
        );

        data.put(
                "employeeName",
                employee.getName()
        );

        return data;
    }


    // =====================================================
    // LATEST CHECK-OUT
    // =====================================================

    private Object getLatestCheckOut(
            List<Attendance> attendanceList) {

        for (Attendance attendance :
                attendanceList) {

            if (attendance.getCheckOut() != null) {

                return attendance.getCheckOut();
            }
        }

        return null;
    }


    // =====================================================
    // OVERALL STATUS
    // =====================================================

    private String getOverallStatus(
            List<Attendance> attendanceList) {

        for (Attendance attendance :
                attendanceList) {

            String status =
                    attendance.getStatus();

            if (status == null) {
                continue;
            }

            String upperStatus =
                    status.toUpperCase();


            if (upperStatus.startsWith("LATE")) {

                return status;
            }


            if (upperStatus.startsWith("EARLY")) {

                return status;
            }
        }

        return "PRESENT";
    }
}