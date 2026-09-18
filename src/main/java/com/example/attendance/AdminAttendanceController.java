package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/attendance")
public class AdminAttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AdminAttendanceController(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    // =========================================
    // GET ALL ATTENDANCE
    // =========================================

    @GetMapping
    public List<AdminAttendanceResponse> getAllAttendance() {

        return attendanceRepository.findAll()
                .stream()
                .map(attendance -> {

                    Employee employee =
                            employeeRepository
                                    .findById(
                                            attendance.getEmployeeId()
                                    )
                                    .orElse(null);

                    if (employee == null) {
                        return null;
                    }

                    return new AdminAttendanceResponse(
                            employee.getId(),
                            employee.getEmployeeCode(),
                            employee.getName(),
                            attendance.getAttendanceDate(),
                            attendance.getCheckIn(),
                            attendance.getCheckOut(),
                            attendance.getStatus()
                    );
                })
                .filter(response -> response != null)
                .toList();
    }
}

