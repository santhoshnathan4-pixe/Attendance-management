package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminRepository adminRepository;
    private final AdminActionHistoryRepository historyRepository;

    public AdminController(
            AdminRepository adminRepository,
            AdminActionHistoryRepository historyRepository) {

        this.adminRepository = adminRepository;
        this.historyRepository = historyRepository;
    }

    // ================================
    // ADMIN LOGIN
    // ================================

    @PostMapping("/login")
    public AdminLoginResponse adminLogin(
            @RequestParam String email,
            @RequestParam String password) {

        Admin admin = adminRepository
                .findByEmail(email == null ? "" : email.trim())
                .filter(found -> PasswordUtil.matches(password, found.getPassword()))
                .orElse(null);

        if (admin == null) {

            return new AdminLoginResponse(
                    false,
                    "Invalid Email or Password",
                    null,
                    null,
                    null
            );
        }

        if (!admin.isActive()) {

            return new AdminLoginResponse(
                    false,
                    "This admin account is switched off. Please contact the owner.",
                    null,
                    null,
                    null
            );
        }

        // An old plain-text password is replaced by a hash on the first successful login.
        if (!PasswordUtil.isHashed(admin.getPassword())) {
            admin.setPassword(PasswordUtil.hash(password));
            adminRepository.save(admin);
        }

        return new AdminLoginResponse(
                true,
                "Admin Login Successful",
                admin.getAdminName(),
                admin.getEmail(),
                admin.getRole()
        );
    }

    // ================================
    // ADMIN ACTION HISTORY
    // ================================

    @GetMapping("/history")
    public List<AdminActionHistory> getHistory() {

        return historyRepository
                .findAllByOrderByActionDateDescActionTimeDesc();
    }
}