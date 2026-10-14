
package com.example.attendance;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
public class PayslipPdfService {

    private final PayslipService payslipService;

    public PayslipPdfService(PayslipService payslipService) {
        this.payslipService = payslipService;
    }

    public byte[] generatePayslipPdf(
            Integer employeeId,
            int year,
            int month) {

        PayslipResponse payslip =
                payslipService.generatePayslip(
                        employeeId,
                        year,
                        month
                );

        YearMonth yearMonth =
                YearMonth.of(year, month);

        LocalDate periodStart =
                yearMonth.atDay(1);

        LocalDate periodEnd =
                yearMonth.atEndOfMonth();

        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy");

        String payPeriod =
                dateFormatter.format(periodStart)
                        + " to "
                        + dateFormatter.format(periodEnd);

        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(
                            PageSize.A4,
                            40,
                            40,
                            40,
                            40
                    );

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();

            // =========================================
            // FONTS
            // =========================================

            Font companyFont =
                    new Font(
                            Font.HELVETICA,
                            20,
                            Font.BOLD
                    );

            Font locationFont =
                    new Font(
                            Font.HELVETICA,
                            9,
                            Font.NORMAL
                    );

            Font titleFont =
                    new Font(
                            Font.HELVETICA,
                            15,
                            Font.BOLD
                    );

            Font sectionFont =
                    new Font(
                            Font.HELVETICA,
                            11,
                            Font.BOLD
                    );

            Font normalFont =
                    new Font(
                            Font.HELVETICA,
                            9,
                            Font.NORMAL
                    );

            Font boldFont =
                    new Font(
                            Font.HELVETICA,
                            9,
                            Font.BOLD
                    );

            Font finalSalaryFont =
                    new Font(
                            Font.HELVETICA,
                            14,
                            Font.BOLD
                    );

            // =========================================
            // COMPANY HEADER
            // =========================================

            Paragraph companyName =
                    new Paragraph(
                            "GROWBYTEE",
                            companyFont
                    );

            companyName.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(companyName);

            Paragraph companyLocation =
                    new Paragraph(
                            "Tirunelveli, Tamil Nadu",
                            locationFont
                    );

            companyLocation.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(companyLocation);

            document.add(
                    new Paragraph(" ")
            );

            Paragraph title =
                    new Paragraph(
                            "SALARY STATEMENT",
                            titleFont
                    );

            title.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(title);

            Paragraph period =
                    new Paragraph(
                            "Pay Period: " + payPeriod,
                            normalFont
                    );

            period.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(period);

            document.add(
                    new Paragraph(" ")
            );

            // =========================================
            // EMPLOYEE DETAILS
            // =========================================

            document.add(
                    createSectionTitle(
                            "EMPLOYEE DETAILS",
                            sectionFont
                    )
            );

            PdfPTable employeeTable =
                    new PdfPTable(2);

            employeeTable.setWidthPercentage(100);

            employeeTable.setWidths(
                    new float[]{35f, 65f}
            );

            addDetailRow(
                    employeeTable,
                    "Employee Code",
                    safe(payslip.getEmployeeCode()),
                    boldFont,
                    normalFont
            );

            addDetailRow(
                    employeeTable,
                    "Employee Name",
                    safe(payslip.getEmployeeName()),
                    boldFont,
                    normalFont
            );

            addDetailRow(
                    employeeTable,
                    "Designation",
                    safe(payslip.getDesignation()),
                    boldFont,
                    normalFont
            );

            addDetailRow(
                    employeeTable,
                    "Technology / Department",
                    safe(payslip.getTechnology()),
                    boldFont,
                    normalFont
            );

            addDetailRow(
                    employeeTable,
                    "Role",
                    safe(payslip.getRole()),
                    boldFont,
                    normalFont
            );

            addDetailRow(
                    employeeTable,
                    "Joining Date",
                    safe(payslip.getJoiningDate()),
                    boldFont,
                    normalFont
            );

            addDetailRow(
                    employeeTable,
                    "Registered Email",
                    safe(payslip.getEmail()),
                    boldFont,
                    normalFont
            );

            document.add(employeeTable);

            document.add(
                    new Paragraph(" ")
            );

            // =========================================
            // ATTENDANCE SUMMARY
            // =========================================

            document.add(
                    createSectionTitle(
                            "ATTENDANCE & PAYROLL SUMMARY",
                            sectionFont
                    )
            );

            PdfPTable attendanceTable =
                    new PdfPTable(2);

            attendanceTable.setWidthPercentage(100);

            attendanceTable.setWidths(
                    new float[]{65f, 35f}
            );

            addAmountRow(
                    attendanceTable,
                    "Working Days",
                    String.valueOf(
                            payslip.getWorkingDays()
                    ),
                    boldFont,
                    normalFont
            );

            addAmountRow(
                    attendanceTable,
                    "Present Days",
                    formatNumber(
                            payslip.getPresentDays()
                    ),
                    boldFont,
                    normalFont
            );

            addAmountRow(
                    attendanceTable,
                    "Paid Leave Days",
                    formatNumber(
                            payslip.getPaidLeaveDays()
                    ),
                    boldFont,
                    normalFont
            );

            if (payslip.getAutoCoveredDays() > 0) {

                addAmountRow(
                        attendanceTable,
                        "Auto Covered Days",
                        formatNumber(
                                payslip.getAutoCoveredDays()
                        ),
                        boldFont,
                        normalFont
                );
            }

            addAmountRow(
                    attendanceTable,
                    "Paid Days",
                    formatNumber(
                            payslip.getPaidDays()
                    ),
                    boldFont,
                    normalFont
            );

            addAmountRow(
                    attendanceTable,
                    "LOP Days",
                    formatNumber(
                            payslip.getLopDays()
                    ),
                    boldFont,
                    normalFont
            );

            document.add(attendanceTable);

            document.add(
                    new Paragraph(" ")
            );

            // =========================================
            // SALARY DETAILS
            // =========================================

            document.add(
                    createSectionTitle(
                            "SALARY DETAILS",
                            sectionFont
                    )
            );

            PdfPTable salaryTable =
                    new PdfPTable(2);

            salaryTable.setWidthPercentage(100);

            salaryTable.setWidths(
                    new float[]{65f, 35f}
            );

            addAmountRow(
                    salaryTable,
                    "Monthly Salary",
                    money(
                            payslip.getMonthlySalary()
                    ),
                    boldFont,
                    normalFont
            );

            addAmountRow(
                    salaryTable,
                    "Per Day Salary",
                    money(
                            payslip.getPerDaySalary()
                    ),
                    boldFont,
                    normalFont
            );

            addAmountRow(
                    salaryTable,
                    "LOP Deduction",
                    money(
                            payslip.getLossOfPay()
                    ),
                    boldFont,
                    normalFont
            );

            addAmountRow(
                    salaryTable,
                    "Salary After LOP",
                    money(
                            payslip.getSalaryBeforeOvertime()
                    ),
                    boldFont,
                    normalFont
            );

            document.add(salaryTable);

            // =========================================
            // OVERTIME / COMP-OFF
            // Show only when applicable
            // =========================================

            boolean hasOvertime =
                    payslip.getOvertimeDays() > 0
                            || payslip.getOvertimeSalary() > 0;

            boolean hasCompOff =
                    payslip.getCompensatoryOffDays() > 0;

            if (hasOvertime || hasCompOff) {

                document.add(
                        new Paragraph(" ")
                );

                document.add(
                        createSectionTitle(
                                "ADDITIONAL BENEFITS",
                                sectionFont
                        )
                );

                PdfPTable additionalTable =
                        new PdfPTable(2);

                additionalTable.setWidthPercentage(
                        100
                );

                additionalTable.setWidths(
                        new float[]{65f, 35f}
                );

                if (hasOvertime) {

                    addAmountRow(
                            additionalTable,
                            "Overtime Days",
                            String.valueOf(
                                    payslip.getOvertimeDays()
                            ),
                            boldFont,
                            normalFont
                    );

                    addAmountRow(
                            additionalTable,
                            "Overtime Salary",
                            money(
                                    payslip.getOvertimeSalary()
                            ),
                            boldFont,
                            normalFont
                    );
                }

                if (hasCompOff) {

                    addAmountRow(
                            additionalTable,
                            "Compensatory Off Days",
                            formatNumber(
                                    payslip.getCompensatoryOffDays()
                            ),
                            boldFont,
                            normalFont
                    );
                }

                document.add(additionalTable);
            }

            document.add(
                    new Paragraph(" ")
            );

            // =========================================
            // FINAL SALARY
            // =========================================

            PdfPTable finalTable =
                    new PdfPTable(2);

            finalTable.setWidthPercentage(100);

            finalTable.setWidths(
                    new float[]{65f, 35f}
            );

            PdfPCell finalLabel =
                    new PdfPCell(
                            new Phrase(
                                    "NET SALARY PAYABLE",
                                    finalSalaryFont
                            )
                    );

            finalLabel.setPadding(10);
            finalLabel.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            PdfPCell finalAmount =
                    new PdfPCell(
                            new Phrase(
                                    money(
                                            payslip.getFinalSalary()
                                    ),
                                    finalSalaryFont
                            )
                    );

            finalAmount.setPadding(10);
            finalAmount.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );
            finalAmount.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            finalTable.addCell(finalLabel);
            finalTable.addCell(finalAmount);

            document.add(finalTable);

            document.add(
                    new Paragraph(" ")
            );

            // =========================================
            // FOOTER
            // =========================================

            Paragraph footer =
                    new Paragraph(
                            "This is a computer-generated salary statement.",
                            locationFont
                    );

            footer.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(footer);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to generate payslip PDF.",
                    e
            );
        }
    }

    // =============================================
    // SECTION TITLE
    // =============================================

    private Paragraph createSectionTitle(
            String text,
            Font font) {

        Paragraph paragraph =
                new Paragraph(
                        text,
                        font
                );

        paragraph.setSpacingAfter(5);

        return paragraph;
    }

    // =============================================
    // EMPLOYEE DETAIL ROW
    // =============================================

    private void addDetailRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                label,
                                labelFont
                        )
                );

        labelCell.setPadding(7);

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                value,
                                valueFont
                        )
                );

        valueCell.setPadding(7);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // =============================================
    // AMOUNT / SUMMARY ROW
    // =============================================

    private void addAmountRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                label,
                                labelFont
                        )
                );

        labelCell.setPadding(7);

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                value,
                                valueFont
                        )
                );

        valueCell.setPadding(7);

        valueCell.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // =============================================
    // SAFE STRING
    // =============================================

    private String safe(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }

    // =============================================
    // NUMBER FORMAT
    // =============================================

    private String formatNumber(double value) {

        if (value == Math.floor(value)) {
            return String.valueOf(
                    (int) value
            );
        }

        return String.format(
                "%.2f",
                value
        );
    }

    // =============================================
    // MONEY FORMAT
    // =============================================

    private String money(double value) {

        return String.format(
                "Rs. %.2f",
                value
        );
    }
}

