
package com.example.attendance.ai;

import com.example.attendance.Attendance;
import com.example.attendance.AttendanceRepository;
import com.example.attendance.Employee;
import com.example.attendance.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceAiService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final GeminiAiService geminiAiService;

    public AttendanceAiService(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            GeminiAiService geminiAiService) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.geminiAiService = geminiAiService;
    }

    public String analyzeCurrentMonth(String question) {

        if (question == null || question.isBlank()) {
            return "Please enter an attendance-related question.";
        }

        LocalDate today = LocalDate.now();

        LocalDate startDate = today.withDayOfMonth(1);

        LocalDate endDate = today.withDayOfMonth(
                today.lengthOfMonth());

        List<Attendance> attendanceList = attendanceRepository
                .findByAttendanceDateBetween(
                        startDate,
                        endDate);

        List<Employee> activeEmployees = employeeRepository
                .findByActiveTrue();

        StringBuilder data = new StringBuilder();

        data.append("Attendance period: ")
                .append(startDate)
                .append(" to ")
                .append(endDate)
                .append("\n\n");

        data.append("Current date: ")
                .append(today)
                .append("\n\n");

        data.append("Active employee count: ")
                .append(activeEmployees.size())
                .append("\n\n");

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
                        .append(attendance.getAttendanceDate())
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

        String prompt = """
                You are an Attendance Management AI assistant
                for a company's administrator.

                The administrator may ask questions in English,
                Tamil, or Tanglish.

                Use ONLY the attendance data supplied below.

                IMPORTANT RULES:

                1. Never invent employees, dates, attendance,
                   counts, or statuses.

                2. Never use salary information.

                3. Answer the administrator's exact question first.

                4. Use Employee Code and Employee Name when
                   identifying employees.

                5. If the question asks for a count, calculate the
                   count only from the supplied attendance records.

                6. If the question asks who has the highest or
                   lowest count, calculate it from the supplied
                   attendance records.

                7. Understand questions about:
                   - Late attendance
                   - Present attendance
                   - Early check-out
                   - Late and early check-out
                   - Missing check-out
                   - Employee attendance
                   - Attendance count
                   - Monthly attendance summary
                   - Employee-wise attendance
                   - Highest attendance count
                   - Lowest attendance count

                8. Understand Tamil and Tanglish questions such as:
                   "இந்த மாதம் யார் அதிகமாக late வந்திருக்கிறார்கள்?"
                   "யார் அதிகமாக late?"
                   "EMP002 எத்தனை நாள் late?"
                   "இந்த மாதம் total attendance எவ்வளவு?"
                   "யாருக்கு checkout இல்லை?"
                   "இந்த மாத attendance summary சொல்லு"

                9. If an employee is identified by Employee Code,
                   use that code to identify the employee.

                10. If the requested information cannot be determined
                    from the supplied data, clearly say that the
                    information is not available.

                11. Do not assume that an employee is absent simply
                    because there is no attendance record.
                    Absence requires sufficient working-day and
                    attendance information.

                12. Do not treat missing check-out as absence.

                13. Keep the response simple, professional and concise.

                14. When giving multiple employees, use a clear list
                    with Employee Code, Employee Name and count.

                15. The supplied attendance data represents the
                    current calendar month.

                Administrator question:
                %s

                Attendance data:
                %s
                """.formatted(
                question.trim(),
                data.toString());

        return geminiAiService
                .generateResponse(prompt);
    }
}
