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
                .findByEmailAndPassword(email, password)
                .orElse(null);

        if (admin == null) {

            return new AdminLoginResponse(
                    false,
                    "Invalid Email or Password",
                    null,
                    null
            );
        }

        return new AdminLoginResponse(
                true,
                "Admin Login Successful",
                admin.getAdminName(),
                admin.getEmail()
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