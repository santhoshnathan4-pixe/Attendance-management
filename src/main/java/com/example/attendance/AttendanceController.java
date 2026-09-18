package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    // Office Timing
    private static final LocalTime OFFICE_START_TIME =
            LocalTime.of(10, 0);

    private static final LocalTime OFFICE_END_TIME =
            LocalTime.of(17, 30);

    public AttendanceController(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    // =========================
    // TEST API
    // =========================
    @GetMapping("/")
    public String home() {
        return "Attendance API Running Successfully!";
    }

    // =========================
    // CHECK IN
    // =========================
    @PostMapping("/check-in")
    public String checkIn(@RequestParam String email) {

        // Active employee only
        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(email)
                .orElse(null);

        if (employee == null) {
            return "Employee Not Found or Employee is Inactive";
        }

        // Employee attendance only
        if (!"EMPLOYEE".equalsIgnoreCase(employee.getRole())) {
            return "Attendance is available only for employees";
        }

        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        // Duplicate check-in prevention
        if (attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        employee.getId(),
                        today
                )
                .isPresent()) {

            return "Already Checked In";
        }

        Attendance attendance = new Attendance();

        attendance.setEmployeeId(employee.getId());
        attendance.setAttendanceDate(today);
        attendance.setCheckIn(currentTime);

        // =========================
        // ATTENDANCE STATUS
        // =========================

        if (currentTime.isAfter(OFFICE_START_TIME)) {
            attendance.setStatus("LATE");
        } else {
            attendance.setStatus("PRESENT");
        }

        attendanceRepository.save(attendance);

        if ("LATE".equals(attendance.getStatus())) {
            return "Check In Successful - " + employee.getName()
                    + " (LATE)";
        }

        return "Check In Successful - " + employee.getName();
    }

    // =========================
    // CHECK OUT
    // =========================
    @PostMapping("/check-out")
    public String checkOut(@RequestParam String email) {

        // Active employee only
        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(email)
                .orElse(null);

        if (employee == null) {
            return "Employee Not Found or Employee is Inactive";
        }

        // Employee attendance only
        if (!"EMPLOYEE".equalsIgnoreCase(employee.getRole())) {
            return "Attendance is available only for employees";
        }

        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        employee.getId(),
                        today
                )
                .orElse(null);

        // Check-in required
        if (attendance == null) {
            return "Please Check In First";
        }

        // Duplicate checkout prevention
        if (attendance.getCheckOut() != null) {
            return "Already Checked Out";
        }

        attendance.setCheckOut(currentTime);

        // =========================
        // EARLY CHECK-OUT
        // =========================

        if (currentTime.isBefore(OFFICE_END_TIME)) {

            // Preserve LATE status if employee was late
            if ("LATE".equalsIgnoreCase(attendance.getStatus())) {
                attendance.setStatus("LATE / EARLY CHECK-OUT");
            } else {
                attendance.setStatus("EARLY CHECK-OUT");
            }

        } else {

            // Preserve LATE status
            if (!"LATE".equalsIgnoreCase(attendance.getStatus())) {
                attendance.setStatus("PRESENT");
            }
        }

        attendanceRepository.save(attendance);

        if (currentTime.isBefore(OFFICE_END_TIME)) {
            return "Check Out Successful - " + employee.getName()
                    + " (EARLY CHECK-OUT)";
        }

        return "Check Out Successful - " + employee.getName();
    }

    // =========================
    // ATTENDANCE HISTORY
    // =========================
    @GetMapping("/history")
    public List<AttendanceResponse> getHistory(
            @RequestParam String email) {

        // Active employee only
        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(email)
                .orElse(null);

        if (employee == null) {
            return List.of();
        }

        // Employee attendance only
        if (!"EMPLOYEE".equalsIgnoreCase(employee.getRole())) {
            return List.of();
        }

        return attendanceRepository
                .findByEmployeeIdOrderByAttendanceDateDesc(
                        employee.getId()
                )
                .stream()
                .map(attendance -> new AttendanceResponse(
                        employee.getName(),
                        attendance.getAttendanceDate(),
                        attendance.getCheckIn(),
                        attendance.getCheckOut(),
                        attendance.getStatus()
                ))
                .toList();
    }

    // =========================
    // ADMIN - ALL ATTENDANCE
    // =========================
    @GetMapping("/admin/all")
    public List<Attendance> getAllAttendance(
            @RequestParam(required = false) String date) {

        if (date != null && !date.isBlank()) {

            LocalDate attendanceDate =
                    LocalDate.parse(date);

            return attendanceRepository
                    .findByAttendanceDateOrderByAttendanceDateDesc(
                            attendanceDate
                    );
        }

        return attendanceRepository.findAll();
    }
}