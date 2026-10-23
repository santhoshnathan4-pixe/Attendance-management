package com.example.attendance;

import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@CrossOrigin(
        origins = {
                "https://attendance-management-73dqjuakh-3d-webinar.vercel.app",
                "https://attendance-management-3d-webinar.vercel.app"
        }
)
@RequestMapping("/employee")
public class EmployeeController {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final int OTP_MINUTES = 10;
    private static final int OTP_MAX_TRIES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmployeeRepository employeeRepository;
    private final JavaMailSender mailSender;

    public EmployeeController(
            EmployeeRepository employeeRepository,
            JavaMailSender mailSender) {

        this.employeeRepository = employeeRepository;
        this.mailSender = mailSender;
    }

    public record ChangePasswordBody(String email, String currentPassword, String newPassword) {
    }

    public record KeepPasswordBody(String email, String password) {
    }

    public record ForgotBody(String email) {
    }

    public record ResetBody(String email, String otp, String newPassword) {
    }

    // =========================================
    // EMPLOYEE LOGIN
    //   Invalid Email or Password
    //   FIRST_LOGIN_REQUIRED      (no password yet: employee creates one)
    //   PASSWORD_CHANGE_OFFER     (admin created the password: change it or keep it)
    //   Login Successful
    // =========================================

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password) {

        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(email == null ? "" : email.trim())
                .orElse(null);

        if (employee == null) {
            return "Invalid Email or Password";
        }

        if (employee.getPassword() == null || employee.getPassword().trim().isEmpty()) {
            return "FIRST_LOGIN_REQUIRED";
        }

        if (!PasswordUtil.matches(password, employee.getPassword())) {
            return "Invalid Email or Password";
        }

        // an old plain-text password is replaced by a hash after a good login
        if (!PasswordUtil.isHashed(employee.getPassword())) {
            employee.setPassword(PasswordUtil.hash(password));
            employeeRepository.save(employee);
        }

        if (employee.isPasswordChangePending()) {
            return "PASSWORD_CHANGE_OFFER";
        }

        return "Login Successful";
    }

    // =========================================
    // FIRST TIME PASSWORD SETUP (employee had no password)
    // =========================================

    @PostMapping("/first-password")
    public String setFirstPassword(
            @RequestBody FirstEmployeePasswordRequest request) {

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw bad("Employee email required");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw bad("New password required");
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
            throw bad("Confirm password required");
        }

        String passwordError = InputValidator.passwordError(request.getNewPassword(), request.getEmail());

        if (passwordError != null) {
            throw bad(passwordError);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw bad("Passwords do not match");
        }

        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(request.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found or inactive"));

        if (employee.getPassword() != null && !employee.getPassword().trim().isEmpty()) {
            throw bad("Password already set. Please login.");
        }

        employee.setPassword(PasswordUtil.hash(request.getNewPassword()));
        employee.setPasswordChangePending(false);
        employeeRepository.save(employee);

        return "Password set successfully";
    }

    // =========================================
    // FIRST LOGIN WITH ADMIN PASSWORD: CHANGE IT NOW
    // =========================================

    @PostMapping("/change-password")
    public String changePassword(@RequestBody ChangePasswordBody body) {

        Employee employee = verified(body == null ? null : body.email(),
                body == null ? null : body.currentPassword());

        String passwordError = InputValidator.passwordError(body.newPassword(), employee.getEmail());

        if (passwordError != null) {
            throw bad(passwordError);
        }

        if (body.newPassword().equals(body.currentPassword())) {
            throw bad("The new password must be different from the current one.");
        }

        employee.setPassword(PasswordUtil.hash(body.newPassword()));
        employee.setPasswordChangePending(false);
        employeeRepository.save(employee);

        return "Password changed";
    }

    // =========================================
    // FIRST LOGIN WITH ADMIN PASSWORD: KEEP IT
    // =========================================

    @PostMapping("/keep-password")
    public String keepPassword(@RequestBody KeepPasswordBody body) {

        Employee employee = verified(body == null ? null : body.email(),
                body == null ? null : body.password());

        employee.setPasswordChangePending(false);
        employeeRepository.save(employee);

        return "OK";
    }

    // =========================================
    // FORGOT PASSWORD: STEP 1, EMAIL A 6-DIGIT CODE
    // The answer is always the same, so nobody can find out
    // which emails are registered.
    // =========================================

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestBody ForgotBody body) {

        String generic = "If this email is registered, a 6-digit code has been sent to it.";

        if (body == null || body.email() == null || body.email().isBlank()) {
            return generic;
        }

        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(body.email().trim())
                .orElse(null);

        if (employee == null) {
            return generic;
        }

        LocalDateTime now = LocalDateTime.now(INDIA);

        // one code per minute
        if (employee.getResetOtpExpiresAt() != null
                && employee.getResetOtpExpiresAt().minusMinutes(OTP_MINUTES).plusMinutes(1).isAfter(now)) {
            return generic;
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setTo(employee.getEmail().trim());
            helper.setSubject("Your GROWBYTEE password reset code");
            helper.setText("Dear " + employee.getName() + ",\n\n"
                    + "Your password reset code is: " + otp + "\n\n"
                    + "It is valid for " + OTP_MINUTES + " minutes. "
                    + "If you did not ask for this, ignore this email; your password is unchanged.\n\n"
                    + "GROWBYTEE");
            mailSender.send(mime);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The email could not be sent. Please try again later.");
        }

        employee.setResetOtpHash(PasswordUtil.hash(otp));
        employee.setResetOtpExpiresAt(now.plusMinutes(OTP_MINUTES));
        employee.setResetOtpAttempts(0);
        employeeRepository.save(employee);

        return generic;
    }

    // =========================================
    // FORGOT PASSWORD: STEP 2, CODE + NEW PASSWORD
    // =========================================

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody ResetBody body) {

        String wrong = "The code is wrong or has expired. Ask for a new code.";

        if (body == null || body.email() == null || body.otp() == null || body.newPassword() == null) {
            throw bad("Email, code and new password are required.");
        }

        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(body.email().trim())
                .orElseThrow(() -> bad(wrong));

        LocalDateTime now = LocalDateTime.now(INDIA);

        if (employee.getResetOtpHash() == null
                || employee.getResetOtpExpiresAt() == null
                || employee.getResetOtpExpiresAt().isBefore(now)
                || employee.getResetOtpAttempts() >= OTP_MAX_TRIES) {
            throw bad(wrong);
        }

        if (!PasswordUtil.matches(body.otp().trim(), employee.getResetOtpHash())) {
            employee.setResetOtpAttempts(employee.getResetOtpAttempts() + 1);
            employeeRepository.save(employee);
            throw bad(wrong);
        }

        String passwordError = InputValidator.passwordError(body.newPassword(), employee.getEmail());

        if (passwordError != null) {
            throw bad(passwordError);
        }

        employee.setPassword(PasswordUtil.hash(body.newPassword()));
        employee.setPasswordChangePending(false);
        employee.setResetOtpHash(null);
        employee.setResetOtpExpiresAt(null);
        employee.setResetOtpAttempts(0);
        employeeRepository.save(employee);

        return "Password changed. Please sign in.";
    }

    // =========================================
    // HELPERS
    // =========================================

    private Employee verified(String email, String password) {

        if (email == null || password == null) {
            throw bad("Email and password are required.");
        }

        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(email.trim())
                .orElse(null);

        if (employee == null || !PasswordUtil.matches(password, employee.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is not correct.");
        }

        return employee;
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    // =========================================
    // EMPLOYEE DETAILS
    // =========================================

    @GetMapping("/details")
    public EmployeeDetailsResponse getEmployeeDetails(
            @RequestParam String email) {

        Employee employee =
                employeeRepository
                        .findByEmailAndActiveTrue(email)
                        .orElse(null);

        if (employee == null) {
            return null;
        }

        EmployeeDetailsResponse response =
                new EmployeeDetailsResponse();

        response.setId(employee.getId());

        response.setEmployeeCode(
                employee.getEmployeeCode()
        );

        response.setName(
                employee.getName()
        );

        response.setEmail(
                employee.getEmail()
        );

        response.setRole(
                employee.getRole()
        );

        response.setJoiningDate(
                employee.getJoiningDate()
        );

        response.setContactNumber(
                employee.getContactNumber()
        );

        return response;
    }
}

