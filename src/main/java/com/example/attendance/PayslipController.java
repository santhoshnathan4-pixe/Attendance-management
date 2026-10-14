
package com.example.attendance;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@RestController
@RequestMapping("/admin/payslip")
@CrossOrigin(origins = {
                "https://attendance-management-73dqjuakh-3d-webinar.vercel.app",
                "https://attendance-management-3d-webinar.vercel.app"
})
public class PayslipController {

        private final PayslipService payslipService;
        private final PayslipPdfService payslipPdfService;
        private final JavaMailSender mailSender;

        public PayslipController(PayslipService payslipService,
                        PayslipPdfService payslipPdfService, JavaMailSender mailSender) {
                this.payslipService = payslipService;
                this.payslipPdfService = payslipPdfService;
                this.mailSender = mailSender;
        }

        @GetMapping("/{employeeId}")
        public PayslipResponse getPayslip(@PathVariable Integer employeeId,
                        @RequestParam int year, @RequestParam int month) {
                try {
                        return payslipService.generatePayslip(employeeId, year, month);
                } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Unable to generate payslip.");
                }
        }

        @GetMapping(value = "/{employeeId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public ResponseEntity<byte[]> downloadPayslipPdf(@PathVariable Integer employeeId,
                        @RequestParam int year, @RequestParam int month) {
                try {
                        byte[] pdf = payslipPdfService.generatePayslipPdf(employeeId, year, month);
                        String filename = "Payslip_" + employeeId + "_" + year + "_" + month + ".pdf";
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_PDF);
                        headers.setContentDisposition(ContentDisposition.attachment()
                                        .filename(filename, StandardCharsets.UTF_8).build());
                        headers.setContentLength(pdf.length);
                        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
                } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Unable to generate payslip PDF.");
                }
        }

        @PostMapping("/{employeeId}/email")
        public ResponseEntity<String> sendPayslipEmail(@PathVariable Integer employeeId,
                        @RequestParam int year, @RequestParam int month) {
                try {
                        PayslipResponse payslip = payslipService.generatePayslip(employeeId, year, month);
                        String email = payslip.getEmail();

                        if (email == null || email.isBlank()) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Employee registered email is missing.");
                        }

                        byte[] pdf = payslipPdfService.generatePayslipPdf(employeeId, year, month);
                        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                        helper.setTo(email.trim());
                        helper.setSubject("Payslip - " + monthName + " " + year);
                        helper.setText("Dear " + payslip.getEmployeeName() + ",\n\n"
                                        + "Please find attached your payslip for " + monthName + " " + year
                                        + ".\n\nRegards,\nGROWBYTEE");
                        helper.addAttachment("Payslip_" + employeeId + "_" + year + "_" + month + ".pdf",
                                        () -> new ByteArrayInputStream(pdf));

                        mailSender.send(message);
                        return ResponseEntity.ok("Payslip sent to employee's registered email.");
                } catch (ResponseStatusException e) {
                        throw e;
                } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                } catch (MessagingException | MailException e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Unable to send payslip email. Check SMTP configuration.");
                }
        }
}
