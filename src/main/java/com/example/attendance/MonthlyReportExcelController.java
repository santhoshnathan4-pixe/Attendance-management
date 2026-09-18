package com.example.attendance;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/admin/monthly-report")
public class MonthlyReportExcelController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public MonthlyReportExcelController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    // =========================================================
    // DOWNLOAD MONTHLY REPORT AS EXCEL
    // =========================================================
    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam int year,
            @RequestParam int month) {

        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        int workingDays = 26;

        List<Attendance> attendanceList =
                attendanceRepository.findByAttendanceDateBetween(
                        startDate,
                        endDate
                );

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet(
                    "Monthly Attendance Report"
            );

            // =================================================
            // TITLE
            // =================================================

            Row titleRow = sheet.createRow(0);

            Cell titleCell = titleRow.createCell(0);

            titleCell.setCellValue(
                    "MONTHLY ATTENDANCE & SALARY REPORT - "
                            + yearMonth
            );

            CellStyle titleStyle =
                    workbook.createCellStyle();

            Font titleFont =
                    workbook.createFont();

            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            titleStyle.setFont(titleFont);

            titleCell.setCellStyle(titleStyle);

            // =================================================
            // HEADER
            // =================================================

            Row headerRow = sheet.createRow(2);

            String[] headers = {

                    "Employee Code",
                    "Employee Name",
                    "Role",
                    "Basic Salary",
                    "Working Days",
                    "Present Days",
                    "Sick Leave",
                    "Casual Leave",
                    "Leave Days",
                    "Permission",
                    "Absent Days",
                    "Loss Of Pay",
                    "Final Salary"
            };

            CellStyle headerStyle =
                    workbook.createCellStyle();

            Font headerFont =
                    workbook.createFont();

            headerFont.setBold(true);

            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {

                Cell cell =
                        headerRow.createCell(i);

                cell.setCellValue(headers[i]);

                cell.setCellStyle(headerStyle);
            }

            // =================================================
            // EMPLOYEE DATA
            // =================================================

            List<Employee> employees =
                    employeeRepository.findAll();

            int rowNumber = 3;

            for (Employee employee : employees) {

                // Only employees
                if (!"EMPLOYEE".equalsIgnoreCase(
                        employee.getRole())) {

                    continue;
                }

                Integer employeeId =
                        employee.getId();

                // =============================================
                // PRESENT DAYS
                // =============================================

                int presentDays =
                        (int) attendanceList.stream()
                                .filter(attendance ->
                                        employeeId.equals(
                                                attendance.getEmployeeId()
                                        )
                                )
                                .filter(attendance ->
                                        "PRESENT".equalsIgnoreCase(
                                                attendance.getStatus()
                                        )
                                )
                                .count();

                // =============================================
                // APPROVED LEAVES
                // =============================================

                List<LeaveRequest> monthlyLeaves =
                        leaveRequestRepository
                                .findByEmployeeIdOrderByLeaveDateDesc(
                                        employeeId
                                )
                                .stream()
                                .filter(leave ->
                                        !leave.getLeaveDate()
                                                .isBefore(startDate)
                                )
                                .filter(leave ->
                                        !leave.getLeaveDate()
                                                .isAfter(endDate)
                                )
                                .filter(leave ->
                                        "APPROVED".equalsIgnoreCase(
                                                leave.getStatus()
                                        )
                                )
                                .toList();

                // =============================================
                // SICK LEAVE
                // =============================================

                double sickLeave =
                        monthlyLeaves.stream()
                                .filter(leave ->
                                        "SICK".equalsIgnoreCase(
                                                leave.getLeaveType()
                                        )
                                )
                                .mapToDouble(leave ->
                                        leave.getLeaveDuration() != null
                                                ? leave.getLeaveDuration()
                                                : 1.0
                                )
                                .sum();

                // =============================================
                // CASUAL LEAVE
                // =============================================

                double casualLeave =
                        monthlyLeaves.stream()
                                .filter(leave ->
                                        "CASUAL".equalsIgnoreCase(
                                                leave.getLeaveType()
                                        )
                                )
                                .mapToDouble(leave ->
                                        leave.getLeaveDuration() != null
                                                ? leave.getLeaveDuration()
                                                : 1.0
                                )
                                .sum();

                // =============================================
                // TOTAL LEAVE
                // =============================================

                double leaveDays =
                        sickLeave + casualLeave;

                // =============================================
                // PERMISSION
                // =============================================

                int permission =
                        (int) monthlyLeaves.stream()
                                .filter(leave ->
                                        "PERMISSION".equalsIgnoreCase(
                                                leave.getLeaveType()
                                        )
                                )
                                .count();

                // =============================================
                // ABSENT DAYS
                // =============================================

                double absentDays =
                        Math.max(
                                0,
                                workingDays
                                        - presentDays
                                        - leaveDays
                        );

                // =============================================
                // SALARY
                // =============================================

                double monthlySalary =
                        employee.getSalary() != null
                                ? employee.getSalary()
                                : 0.0;

                double perDaySalary =
                        monthlySalary / workingDays;

                double lossOfPay =
                        absentDays * perDaySalary;

                double finalSalary =
                        monthlySalary - lossOfPay;

                // =============================================
                // EXCEL ROW
                // =============================================

                Row row =
                        sheet.createRow(rowNumber++);

                row.createCell(0)
                        .setCellValue(
                                employee.getEmployeeCode()
                        );

                row.createCell(1)
                        .setCellValue(
                                employee.getName()
                        );

                row.createCell(2)
                        .setCellValue(
                                employee.getRole()
                        );

                row.createCell(3)
                        .setCellValue(
                                monthlySalary
                        );

                row.createCell(4)
                        .setCellValue(
                                workingDays
                        );

                row.createCell(5)
                        .setCellValue(
                                presentDays
                        );

                row.createCell(6)
                        .setCellValue(
                                sickLeave
                        );

                row.createCell(7)
                        .setCellValue(
                                casualLeave
                        );

                row.createCell(8)
                        .setCellValue(
                                leaveDays
                        );

                row.createCell(9)
                        .setCellValue(
                                permission
                        );

                row.createCell(10)
                        .setCellValue(
                                absentDays
                        );

                row.createCell(11)
                        .setCellValue(
                                lossOfPay
                        );

                row.createCell(12)
                        .setCellValue(
                                finalSalary
                        );
            }

            // =================================================
            // AUTO SIZE COLUMNS
            // =================================================

            for (int i = 0; i < headers.length; i++) {

                sheet.autoSizeColumn(i);
            }

            // =================================================
            // CREATE EXCEL FILE
            // =================================================

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            workbook.write(outputStream);

            byte[] excelFile =
                    outputStream.toByteArray();

            // =================================================
            // RESPONSE
            // =================================================

            String fileName =
                    "Monthly_Report_"
                            + year
                            + "_"
                            + String.format("%02d", month)
                            + ".xlsx";

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                                    fileName +
                                    "\""
                    )
                    .contentType(
                            MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            )
                    )
                    .body(excelFile);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}