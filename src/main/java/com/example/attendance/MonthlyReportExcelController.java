
package com.example.attendance;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.YearMonth;

@RestController
@RequestMapping("/admin/monthly-report")
public class MonthlyReportExcelController {

        private final PayrollService payrollService;

        public MonthlyReportExcelController(PayrollService payrollService) {
                this.payrollService = payrollService;
        }

        // Same numbers as the Monthly Report and Salary pages.
        @GetMapping("/excel")
        public ResponseEntity<byte[]> downloadExcel(
                        @RequestParam int year,
                        @RequestParam int month) {

                YearMonth yearMonth =
                                YearMonth.of(year, month);

                PayrollService.MonthPayroll payroll =
                                payrollService.calculate(
                                                year,
                                                month);

                try (Workbook workbook = new XSSFWorkbook()) {

                        Sheet sheet = workbook.createSheet(
                                        "Monthly Attendance Report");


                        // =====================================================
                        // TITLE
                        // =====================================================

                        Row titleRow =
                                        sheet.createRow(0);

                        Cell titleCell =
                                        titleRow.createCell(0);

                        titleCell.setCellValue(
                                        "MONTHLY ATTENDANCE & SALARY REPORT - "
                                                        + yearMonth);


                        CellStyle titleStyle =
                                        workbook.createCellStyle();

                        Font titleFont =
                                        workbook.createFont();

                        titleFont.setBold(true);

                        titleFont.setFontHeightInPoints(
                                        (short) 14);

                        titleStyle.setFont(titleFont);

                        titleCell.setCellStyle(
                                        titleStyle);


                        // =====================================================
                        // HEADERS
                        // =====================================================

                        Row headerRow =
                                        sheet.createRow(2);

                        String[] headers = {

                                        "Employee Code",
                                        "Employee Name",
                                        "Role",
                                        "Basic Salary",
                                        "Working Days",
                                        "Present Days",
                                        "Absent Days",
                                        "LOP Leave",
                                        "Sick Leave",
                                        "Casual Leave",
                                        "Permission",
                                        "Covered By Leave Balance",
                                        "Loss Of Pay",
                                        "Final Salary",

                                        // OT
                                        "OT Days",
                                        "OT Benefit",
                                        "OT Salary",
                                        "Comp-Off Days",
                                        "Final Salary With OT"
                        };


                        CellStyle headerStyle =
                                        workbook.createCellStyle();

                        Font headerFont =
                                        workbook.createFont();

                        headerFont.setBold(true);

                        headerStyle.setFont(
                                        headerFont);


                        for (int i = 0;
                             i < headers.length;
                             i++) {

                                Cell cell =
                                                headerRow.createCell(i);

                                cell.setCellValue(
                                                headers[i]);

                                cell.setCellStyle(
                                                headerStyle);
                        }


                        // =====================================================
                        // DATA
                        // =====================================================

                        int rowNumber = 3;


                        for (PayrollService.EmployeePayroll p :
                                        payroll.employees()) {

                                Employee employee =
                                                p.employee();

                                PayrollMath.Result math =
                                                p.math();


                                double monthlySalary =
                                                employee.getSalary() != null
                                                                ? employee.getSalary()
                                                                : 0.0;


                                Row row =
                                                sheet.createRow(
                                                                rowNumber++);


                                // =================================================
                                // EXISTING PAYROLL DATA
                                // =================================================

                                row.createCell(0)
                                                .setCellValue(
                                                                employee.getEmployeeCode() != null
                                                                                ? employee.getEmployeeCode()
                                                                                : "");


                                row.createCell(1)
                                                .setCellValue(
                                                                employee.getName() != null
                                                                                ? employee.getName()
                                                                                : "");


                                row.createCell(2)
                                                .setCellValue(
                                                                employee.getRole() != null
                                                                                ? employee.getRole()
                                                                                : "");


                                row.createCell(3)
                                                .setCellValue(
                                                                monthlySalary);


                                row.createCell(4)
                                                .setCellValue(
                                                                math.workingDays());


                                row.createCell(5)
                                                .setCellValue(
                                                                math.presentDays());


                                row.createCell(6)
                                                .setCellValue(
                                                                p.absentDays());


                                row.createCell(7)
                                                .setCellValue(
                                                                p.lopLeave());


                                row.createCell(8)
                                                .setCellValue(
                                                                p.sickLeave());


                                row.createCell(9)
                                                .setCellValue(
                                                                p.casualLeave());


                                row.createCell(10)
                                                .setCellValue(
                                                                p.permission());


                                row.createCell(11)
                                                .setCellValue(
                                                                math.autoCoveredDays());


                                row.createCell(12)
                                                .setCellValue(
                                                                math.lossOfPay());


                                row.createCell(13)
                                                .setCellValue(
                                                                math.finalSalary());


                                // =================================================
                                // OT DATA
                                // =================================================

                                row.createCell(14)
                                                .setCellValue(
                                                                p.overtimeDays());


                                /*
                                 * The benefit type is not stored directly
                                 * inside EmployeePayroll.
                                 *
                                 * Therefore display the effective benefit
                                 * based on the calculated OT values.
                                 */

                                String otBenefit = "";

                                if (p.overtimeSalary() > 0) {

                                        otBenefit =
                                                        "EXTRA_SALARY";

                                } else if (p.compensatoryOffDays() > 0) {

                                        otBenefit =
                                                        "COMP_OFF";
                                }


                                row.createCell(15)
                                                .setCellValue(
                                                                otBenefit);


                                row.createCell(16)
                                                .setCellValue(
                                                                p.overtimeSalary());


                                row.createCell(17)
                                                .setCellValue(
                                                                p.compensatoryOffDays());


                                row.createCell(18)
                                                .setCellValue(
                                                                p.finalSalaryWithOvertime());
                        }


                        // =====================================================
                        // AUTO SIZE
                        // =====================================================

                        for (int i = 0;
                             i < headers.length;
                             i++) {

                                sheet.autoSizeColumn(i);
                        }


                        // =====================================================
                        // CREATE EXCEL FILE
                        // =====================================================

                        ByteArrayOutputStream outputStream =
                                        new ByteArrayOutputStream();

                        workbook.write(
                                        outputStream);


                        String fileName =
                                        "Monthly_Report_"
                                                        + year
                                                        + "_"
                                                        + String.format(
                                                                        "%02d",
                                                                        month)
                                                        + ".xlsx";


                        return ResponseEntity.ok()
                                        .header(
                                                        HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\""
                                                                        + fileName
                                                                        + "\"")
                                        .contentType(
                                                        MediaType.parseMediaType(
                                                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                        .body(
                                                        outputStream.toByteArray());
                }

                catch (Exception e) {

                        return ResponseEntity
                                        .internalServerError()
                                        .build();
                }
        }
}

