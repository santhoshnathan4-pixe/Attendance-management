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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Professional payslip (black / green / white).
 * Shows the attendance summary, earnings, deductions, the short-time details
 * and the admin's decision with the reason.
 */
@Service
public class PayslipPdfService {

    private static final Color BLACK = new Color(0x11, 0x18, 0x14);
    private static final Color GREEN = new Color(0x16, 0xA3, 0x4A);
    private static final Color LIGHT_GREEN = new Color(0xEC, 0xFD, 0xF3);
    private static final Color LINE = new Color(0xD9, 0xDE, 0xDB);
    private static final Color MUTED = new Color(0x6B, 0x72, 0x6E);
    private static final Color WHITE = Color.WHITE;

    private final PayslipService payslipService;
    private final PayrollPeriodService periodService;
    private final PayrollService payrollService;

    public PayslipPdfService(
            PayslipService payslipService,
            PayrollService payrollService,
            PayrollPeriodService periodService) {

        this.periodService = periodService;
        this.payslipService = payslipService;
        this.payrollService = payrollService;
    }

    public byte[] generatePayslipPdf(Integer employeeId, int year, int month) {

        PayslipResponse payslip = payslipService.generatePayslip(employeeId, year, month);

        PayrollService.LateInfo late = null;
        PayrollAdjustment adjustment = null;

        try {
            for (PayrollService.EmployeePayroll p : payrollService.calculate(year, month).employees()) {
                if (p.employee() != null && employeeId.equals(p.employee().getId())) {
                    late = p.late();
                    adjustment = p.adjustment();
                    break;
                }
            }
        } catch (Exception ignored) {
            // The payslip is still produced without the extra notes.
        }

        YearMonth ym = YearMonth.of(year, month);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        PayrollPeriodService.Period period = periodService.resolve(year, month);
        LocalDate start = period.start();
        LocalDate end = period.end();
        String payPeriod = df.format(start) + " to " + df.format(end);
        String monthTitle = payslip.getMonthName() != null && !payslip.getMonthName().isBlank()
                ? payslip.getMonthName() + " " + year
                : ym.getMonth() + " " + year;

        Font brand = new Font(Font.HELVETICA, 20, Font.BOLD, WHITE);
        Font brandSub = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(0xBB, 0xC4, 0xBF));
        Font slipTitle = new Font(Font.HELVETICA, 14, Font.BOLD, GREEN);
        Font slipMonth = new Font(Font.HELVETICA, 9, Font.NORMAL, WHITE);
        Font section = new Font(Font.HELVETICA, 9, Font.BOLD, GREEN);
        Font label = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);
        Font value = new Font(Font.HELVETICA, 9, Font.BOLD, BLACK);
        Font normal = new Font(Font.HELVETICA, 9, Font.NORMAL, BLACK);
        Font head = new Font(Font.HELVETICA, 8, Font.BOLD, WHITE);
        Font netLabel = new Font(Font.HELVETICA, 11, Font.BOLD, WHITE);
        Font netValue = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(0x4A, 0xDE, 0x80));
        Font small = new Font(Font.HELVETICA, 7, Font.NORMAL, MUTED);

        try {

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ---------- HEADER BAND ----------
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[] { 60f, 40f });

            PdfPCell left = new PdfPCell();
            left.setBackgroundColor(BLACK);
            left.setBorder(Rectangle.NO_BORDER);
            left.setPadding(14);
            left.addElement(new Paragraph("GROWBYTEE", brand));
            left.addElement(new Paragraph("Tirunelveli, Tamil Nadu", brandSub));

            PdfPCell right = new PdfPCell();
            right.setBackgroundColor(BLACK);
            right.setBorder(Rectangle.NO_BORDER);
            right.setPadding(14);
            Paragraph t = new Paragraph("PAYSLIP", slipTitle);
            t.setAlignment(Element.ALIGN_RIGHT);
            Paragraph m = new Paragraph(monthTitle, slipMonth);
            m.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(t);
            right.addElement(m);

            header.addCell(left);
            header.addCell(right);
            doc.add(header);

            PdfPTable bar = new PdfPTable(1);
            bar.setWidthPercentage(100);
            PdfPCell barCell = new PdfPCell(new Phrase(" ", new Font(Font.HELVETICA, 2)));
            barCell.setBackgroundColor(GREEN);
            barCell.setBorder(Rectangle.NO_BORDER);
            bar.addCell(barCell);
            doc.add(bar);

            doc.add(spacer(10));

            // ---------- EMPLOYEE DETAILS ----------
            doc.add(sectionTitle("EMPLOYEE DETAILS", section));

            PdfPTable info = new PdfPTable(4);
            info.setWidthPercentage(100);
            info.setWidths(new float[] { 18f, 32f, 18f, 32f });

            pair(info, "Employee Code", safe(payslip.getEmployeeCode()), label, value);
            pair(info, "Employee Name", safe(payslip.getEmployeeName()), label, value);
            pair(info, "Designation", safe(payslip.getDesignation()), label, value);
            pair(info, "Technology / Dept", safe(payslip.getTechnology()), label, value);
            pair(info, "Role", safe(payslip.getRole()), label, value);
            pair(info, "Joining Date", safe(payslip.getJoiningDate()), label, value);
            pair(info, "Pay Period", payPeriod, label, value);
            pair(info, "Email", safe(payslip.getEmail()), label, value);

            doc.add(info);
            doc.add(spacer(10));

            // ---------- ATTENDANCE SUMMARY ----------
            doc.add(sectionTitle("ATTENDANCE SUMMARY", section));

            PdfPTable att = new PdfPTable(6);
            att.setWidthPercentage(100);

            stat(att, "Working Days", num(payslip.getWorkingDays()), label, value);
            stat(att, "Present Days", num(payslip.getPresentDays()), label, value);
            stat(att, "Paid Leave", num(payslip.getPaidLeaveDays() + payslip.getAutoCoveredDays()), label, value);
            stat(att, "Paid Days", num(payslip.getPaidDays()), label, value);
            stat(att, "LOP Days", num(payslip.getLopDays()), label, value);
            stat(att, "Short Time", late == null ? "-" : minutes(late.deductibleMinutes()), label, value);

            doc.add(att);
            doc.add(spacer(10));

            // ---------- EARNINGS / DEDUCTIONS ----------
            PdfPTable money = new PdfPTable(2);
            money.setWidthPercentage(100);
            money.setWidths(new float[] { 70f, 30f });

            headRow(money, "EARNINGS", "AMOUNT", head);
            moneyRow(money, "Monthly Salary", rs(payslip.getMonthlySalary()), normal, false);

            if (payslip.getOvertimeSalary() > 0) {
                moneyRow(money, "Overtime (" + payslip.getOvertimeDays() + " day"
                        + (payslip.getOvertimeDays() == 1 ? "" : "s") + ")",
                        rs(payslip.getOvertimeSalary()), normal, false);
            }

            double gross = payslip.getMonthlySalary() + payslip.getOvertimeSalary();
            moneyRow(money, "Gross Earnings", rs(gross), value, true);

            headRow(money, "DEDUCTIONS", "AMOUNT", head);
            moneyRow(money, "Loss of Pay (absent / approved cut)", rs(payslip.getLossOfPay()), normal, false);
            moneyRow(money, "Total Deductions", rs(payslip.getLossOfPay()), value, true);

            doc.add(money);

            if (payslip.getCompensatoryOffDays() > 0) {
                Paragraph comp = new Paragraph(
                        "Compensatory off earned this month: " + num(payslip.getCompensatoryOffDays()) + " day(s)",
                        small);
                comp.setSpacingBefore(4);
                doc.add(comp);
            }

            doc.add(spacer(8));

            // ---------- NET SALARY BAND ----------
            PdfPTable net = new PdfPTable(2);
            net.setWidthPercentage(100);
            net.setWidths(new float[] { 60f, 40f });

            PdfPCell nl = new PdfPCell(new Phrase("NET SALARY PAYABLE", netLabel));
            nl.setBackgroundColor(BLACK);
            nl.setBorder(Rectangle.NO_BORDER);
            nl.setPadding(12);
            nl.setVerticalAlignment(Element.ALIGN_MIDDLE);

            PdfPCell nv = new PdfPCell(new Phrase(rs(payslip.getFinalSalary()), netValue));
            nv.setBackgroundColor(BLACK);
            nv.setBorder(Rectangle.NO_BORDER);
            nv.setPadding(12);
            nv.setHorizontalAlignment(Element.ALIGN_RIGHT);
            nv.setVerticalAlignment(Element.ALIGN_MIDDLE);

            net.addCell(nl);
            net.addCell(nv);
            doc.add(net);

            // ---------- SALARY REVIEW NOTE ----------
            String note = reviewNote(late, adjustment);

            if (note != null) {
                doc.add(spacer(10));

                PdfPTable box = new PdfPTable(1);
                box.setWidthPercentage(100);

                PdfPCell c = new PdfPCell();
                c.setBackgroundColor(LIGHT_GREEN);
                c.setBorderColor(GREEN);
                c.setBorderWidth(0.8f);
                c.setPadding(10);
                c.addElement(new Paragraph("SALARY REVIEW NOTE", section));
                c.addElement(new Paragraph(note, normal));

                box.addCell(c);
                doc.add(box);
            }

            doc.add(spacer(14));

            Paragraph footer = new Paragraph(
                    "This is a computer-generated payslip and does not require a signature.",
                    small);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Unable to generate payslip PDF.", e);
        }
    }

    // =============================================
    // REVIEW NOTE (admin decision + reason)
    // =============================================

    private String reviewNote(PayrollService.LateInfo late, PayrollAdjustment adjustment) {

        StringBuilder text = new StringBuilder();

        boolean payFull = adjustment != null && Boolean.TRUE.equals(adjustment.getPayFull());

        if (payFull) {

            text.append("Full salary paid by the admin.");

        } else if (adjustment != null && adjustment.getDeductMinutes() != null) {

            int cut = adjustment.getDeductMinutes();

            text.append(cut == 0
                    ? "Short time was reviewed by the admin. No deduction applied."
                    : "Short time reviewed by the admin. Deducted time: " + minutes(cut) + ".");

        } else if (late != null && late.deductibleMinutes() > 0) {

            text.append("Short time this month: ")
                    .append(minutes(late.deductibleMinutes()))
                    .append(". No deduction applied.");
        }

        if (adjustment != null && adjustment.getReason() != null
                && !adjustment.getReason().isBlank()) {

            if (text.length() > 0) {
                text.append("\n");
            }

            text.append("Reason: ").append(adjustment.getReason().trim());
        }

        return text.length() == 0 ? null : text.toString();
    }

    // =============================================
    // TABLE HELPERS
    // =============================================

    private Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ", new Font(Font.HELVETICA, 2));
        p.setSpacingAfter(height);
        return p;
    }

    private Paragraph sectionTitle(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(4);
        return p;
    }

    private void pair(PdfPTable table, String l, String v, Font lf, Font vf) {

        table.addCell(cell(l, lf, Element.ALIGN_LEFT, null, true));
        table.addCell(cell(v, vf, Element.ALIGN_LEFT, null, true));
    }

    private void stat(PdfPTable table, String l, String v, Font lf, Font vf) {

        PdfPCell c = new PdfPCell();
        c.setBorderColor(LINE);
        c.setBackgroundColor(LIGHT_GREEN);
        c.setPadding(7);
        c.addElement(new Paragraph(l, lf));
        c.addElement(new Paragraph(v, vf));
        table.addCell(c);
    }

    private void headRow(PdfPTable table, String l, String r, Font font) {

        PdfPCell a = cell(l, font, Element.ALIGN_LEFT, BLACK, false);
        PdfPCell b = cell(r, font, Element.ALIGN_RIGHT, BLACK, false);

        table.addCell(a);
        table.addCell(b);
    }

    private void moneyRow(PdfPTable table, String l, String r, Font font, boolean total) {

        Color bg = total ? LIGHT_GREEN : null;

        table.addCell(cell(l, font, Element.ALIGN_LEFT, bg, true));
        table.addCell(cell(r, font, Element.ALIGN_RIGHT, bg, true));
    }

    private PdfPCell cell(String text, Font font, int align, Color bg, boolean bottomLine) {

        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(7);
        c.setHorizontalAlignment(align);
        c.setBorder(bottomLine ? Rectangle.BOTTOM : Rectangle.NO_BORDER);
        c.setBorderColor(LINE);

        if (bg != null) {
            c.setBackgroundColor(bg);
        }

        return c;
    }

    // =============================================
    // FORMATTERS
    // =============================================

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String num(double value) {

        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }

        return String.format("%.2f", value);
    }

    private String rs(double value) {
        return String.format("Rs. %,.2f", value);
    }

    private String minutes(long total) {

        if (total <= 0) {
            return "0 mins";
        }

        long h = total / 60;
        long m = total % 60;

        if (h > 0 && m > 0) {
            return h + (h == 1 ? " hr " : " hrs ") + m + " mins";
        }

        if (h > 0) {
            return h + (h == 1 ? " hr" : " hrs");
        }

        return m + " mins";
    }
}
