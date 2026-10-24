package com.example.attendance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Data for the dashboard graph "Attendance, last days".
 * Read-only: it only counts, it never changes anything.
 */
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardChartController {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("EEE dd", Locale.ENGLISH);

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    public AdminDashboardChartController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /** GET /admin/dashboard/trend?days=7  ->  [{date,label,present,late,total}] oldest first */
    @GetMapping("/trend")
    public List<Map<String, Object>> trend(@RequestParam(defaultValue = "7") int days) {

        int count = Math.max(1, Math.min(days, 31));
        int total = employeeRepository.findByActiveTrue().size();
        LocalDate today = LocalDate.now(INDIA);

        List<Map<String, Object>> result = new ArrayList<>();

        // ONE query for the whole range (was one query per day).
        Map<LocalDate, List<Attendance>> byDay = new HashMap<>();
        for (Attendance a : attendanceRepository
                .findByAttendanceDateBetween(today.minusDays(count - 1), today)) {
            if (a.getAttendanceDate() != null) {
                byDay.computeIfAbsent(a.getAttendanceDate(), k -> new ArrayList<>()).add(a);
            }
        }

        for (int i = count - 1; i >= 0; i--) {

            LocalDate day = today.minusDays(i);

            Set<Integer> present = new HashSet<>();
            Set<Integer> late = new HashSet<>();

            for (Attendance a : byDay.getOrDefault(day, new ArrayList<>())) {

                if (a.getEmployeeId() == null) {
                    continue;
                }

                present.add(a.getEmployeeId());

                String status = a.getStatus();

                if (status != null && status.toUpperCase().startsWith("LATE")) {
                    late.add(a.getEmployeeId());
                }
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day.toString());
            row.put("label", day.format(LABEL));
            row.put("present", present.size());
            row.put("late", late.size());
            row.put("total", total);
            result.add(row);
        }

        return result;
    }
}
