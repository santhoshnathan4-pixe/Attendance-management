package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin Access: the owner adds and removes other admins (HR, Manager).
 *
 * Every owner action asks for the owner's own password again, so a stolen
 * browser tab alone cannot create or remove an admin. Passwords are stored
 * only as hashes, so nobody (including the owner) can read another admin's password.
 */
@RestController
@RequestMapping("/admin/admins")
public class AdminAccessController {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final AdminRepository adminRepository;
    private final AdminActionHistoryRepository historyRepository;

    public AdminAccessController(
            AdminRepository adminRepository,
            AdminActionHistoryRepository historyRepository) {

        this.adminRepository = adminRepository;
        this.historyRepository = historyRepository;
    }

    // ----- request / response shapes -----

    public record AuthBody(String adminEmail, String adminPassword) {
    }

    public record CreateBody(
            String adminEmail,
            String adminPassword,
            String name,
            String email,
            String role,
            String password) {
    }

    public record RoleBody(String adminEmail, String adminPassword, String role) {
    }

    public record PasswordBody(String email, String currentPassword, String newPassword) {
    }

    public record AdminRow(
            Integer id,
            String name,
            String email,
            String role,
            String createdBy,
            String createdAt,
            boolean you) {
    }

    // =====================================================
    // LIST
    // =====================================================

    @PostMapping("/list")
    public List<AdminRow> list(@RequestBody AuthBody body) {

        Admin owner = requireOwner(body == null ? null : body.adminEmail(),
                body == null ? null : body.adminPassword());

        return adminRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                .map(a -> new AdminRow(
                        a.getId(),
                        a.getAdminName(),
                        a.getEmail(),
                        a.getRole(),
                        a.getCreatedBy(),
                        a.getCreatedAt() == null ? null : a.getCreatedAt().format(STAMP),
                        a.getId().equals(owner.getId())))
                .toList();
    }

    // =====================================================
    // ADD
    // =====================================================

    @PostMapping
    public AdminRow add(@RequestBody CreateBody body) {

        if (body == null) {
            throw bad("Request is empty.");
        }

        Admin owner = requireOwner(body.adminEmail(), body.adminPassword());

        String name = body.name() == null ? "" : body.name().trim();
        String email = body.email() == null ? "" : body.email().trim().toLowerCase();
        String role = body.role() == null ? "" : body.role().trim().toUpperCase();

        if (name.length() < 2 || name.length() > 60) {
            throw bad("Enter the admin's name (2 to 60 letters).");
        }

        if (!InputValidator.isValidEmail(email)) {
            throw bad("Enter a valid email address (example: name@gmail.com).");
        }

        if (!role.equals(Admin.HR) && !role.equals(Admin.MANAGER)) {
            throw bad("Role must be HR or Manager.");
        }

        String passwordError = InputValidator.passwordError(body.password(), email);

        if (passwordError != null) {
            throw bad(passwordError);
        }

        if (adminRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An admin with this email already exists.");
        }

        Admin admin = new Admin();
        admin.setAdminName(name);
        admin.setEmail(email);
        admin.setPassword(PasswordUtil.hash(body.password()));
        admin.setRole(role);
        admin.setActive(true);
        admin.setCreatedBy(owner.getAdminName());
        admin.setCreatedAt(LocalDateTime.now(INDIA));

        Admin saved = adminRepository.save(admin);

        log(owner, "ADD ADMIN", saved.getAdminName() + " (" + saved.getEmail() + ")", null, role);

        return toRow(saved, owner);
    }

    // =====================================================
    // CHANGE ROLE
    // =====================================================

    @PostMapping("/{id}/role")
    public AdminRow changeRole(@PathVariable Integer id, @RequestBody RoleBody body) {

        if (body == null) {
            throw bad("Request is empty.");
        }

        Admin owner = requireOwner(body.adminEmail(), body.adminPassword());
        Admin target = findOther(id, owner);

        String role = body.role() == null ? "" : body.role().trim().toUpperCase();

        if (!role.equals(Admin.HR) && !role.equals(Admin.MANAGER)) {
            throw bad("Role must be HR or Manager.");
        }

        String old = target.getRole();
        target.setRole(role);
        adminRepository.save(target);

        log(owner, "CHANGE ADMIN ROLE", target.getAdminName() + " (" + target.getEmail() + ")", old, role);

        return toRow(target, owner);
    }

    // =====================================================
    // REMOVE
    // =====================================================

    @PostMapping("/{id}/remove")
    public String remove(@PathVariable Integer id, @RequestBody AuthBody body) {

        Admin owner = requireOwner(body == null ? null : body.adminEmail(),
                body == null ? null : body.adminPassword());

        Admin target = findOther(id, owner);

        adminRepository.delete(target);

        log(owner, "REMOVE ADMIN", target.getAdminName() + " (" + target.getEmail() + ")", target.getRole(), null);

        return "Admin removed.";
    }

    // =====================================================
    // CHANGE MY OWN PASSWORD (any admin)
    // =====================================================

    @PostMapping("/change-password")
    public String changePassword(@RequestBody PasswordBody body) {

        if (body == null || body.email() == null) {
            throw bad("Request is empty.");
        }

        Admin admin = adminRepository
                .findByEmailAndPassword(body.email(), body.currentPassword())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Current password is not correct."));

        String passwordError = InputValidator.passwordError(body.newPassword(), admin.getEmail());

        if (passwordError != null) {
            throw bad(passwordError);
        }

        if (body.newPassword().equals(body.currentPassword())) {
            throw bad("The new password must be different from the current one.");
        }

        admin.setPassword(PasswordUtil.hash(body.newPassword()));
        adminRepository.save(admin);

        log(admin, "ADMIN PASSWORD CHANGED", admin.getAdminName() + " (" + admin.getEmail() + ")", null, null);

        return "Password changed.";
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private Admin requireOwner(String email, String password) {

        Admin admin = adminRepository
                .findByEmailAndPassword(email, password)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Your password is not correct."));

        if (!Admin.OWNER.equals(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the owner can manage admin access.");
        }

        return admin;
    }

    /** The admin to change: must exist, must not be the owner himself, must not be an owner. */
    private Admin findOther(Integer id, Admin owner) {

        Admin target = adminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Admin not found."));

        if (target.getId().equals(owner.getId())) {
            throw bad("You cannot change your own access here.");
        }

        if (Admin.OWNER.equals(target.getRole())) {
            throw bad("An owner account cannot be changed or removed here.");
        }

        return target;
    }

    private AdminRow toRow(Admin a, Admin viewer) {

        return new AdminRow(
                a.getId(),
                a.getAdminName(),
                a.getEmail(),
                a.getRole(),
                a.getCreatedBy(),
                a.getCreatedAt() == null ? null : a.getCreatedAt().format(STAMP),
                a.getId().equals(viewer.getId()));
    }

    private void log(Admin by, String action, String subject, String oldValue, String newValue) {

        AdminActionHistory history = new AdminActionHistory();

        history.setAdminName(by.getAdminName());
        history.setAction(action);
        history.setEmployeeId(null);
        history.setEmployeeCode(null);
        history.setEmployeeName("Admin: " + subject);
        history.setActionDate(LocalDate.now(INDIA));
        history.setActionTime(LocalTime.now(INDIA));
        history.setFieldName("Role");
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setReason(null);

        historyRepository.save(history);
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
