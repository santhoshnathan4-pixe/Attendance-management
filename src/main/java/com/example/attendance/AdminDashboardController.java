package com.example.attendance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
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

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {

        Map<String, Object> stats = new HashMap<>();

        // Active employees only
        List<Employee> employees = employeeRepository.findByActiveTrue();

        int totalEmployees = 0;
        int presentToday = 0;

        for (Employee employee : employees) {

            if ("EMPLOYEE".equalsIgnoreCase(employee.getRole())) {

                totalEmployees++;

                if (attendanceRepository
                        .findByEmployeeIdAndAttendanceDate(
                                employee.getId(),
                                LocalDate.now()
                        )
                        .isPresent()) {

                    presentToday++;
                }
            }
        }

        // Pending leave count
        long pendingLeaves = leaveRequestRepository
                .findAll()
                .stream()
                .filter(leave ->
                        "PENDING".equalsIgnoreCase(leave.getStatus()))
                .count();

        int absentToday = totalEmployees - presentToday;

        stats.put("totalEmployees", totalEmployees);
        stats.put("presentToday", presentToday);
        stats.put("absentToday", Math.max(absentToday, 0));
        stats.put("pendingLeaves", pendingLeaves);

        return stats;
    }
}