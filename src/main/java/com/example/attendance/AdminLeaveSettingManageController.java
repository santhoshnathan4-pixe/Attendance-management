package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Edit / delete / history for Leave & Permission Settings.
 * (Create and "save" stay in AdminLeavePermissionController.)
 */
@RestController
@RequestMapping("/admin/leave-settings")
public class AdminLeaveSettingManageController {

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    private final LeavePermissionSettingRepository settingRepository;
    private final LeaveSettingHistoryRepository historyRepository;
    private final AdminRepository adminRepository;

    public AdminLeaveSettingManageController(
            LeavePermissionSettingRepository settingRepository,
            LeaveSettingHistoryRepository historyRepository,
            AdminRepository adminRepository) {

        this.settingRepository = settingRepository;
        this.historyRepository = historyRepository;
        this.adminRepository = adminRepository;
    }

    // =========================
    // EDIT
    // =========================

    @PutMapping("/{id}")
    public LeavePermissionSetting updateSetting(
            @PathVariable Long id,
            @RequestBody LeaveSettingChangeRequest request) {

        String adminEmail = checkAdmin(request.getAdminEmail());
        String reason = checkReason(request.getReason());

        LeavePermissionSetting setting = findSetting(id);

        String oldValues = describe(setting);

        if (request.getSickLeave() != null) {
            if (request.getSickLeave() < 0) {
                throw bad("Sick Leave Cannot Be Negative");
            }
            setting.setSickLeave(request.getSickLeave());
        }

        if (request.getCasualLeave() != null) {
            if (request.getCasualLeave() < 0) {
                throw bad("Casual Leave Cannot Be Negative");
            }
            setting.setCasualLeave(request.getCasualLeave());
        }

        if (request.getPermissionCount() != null) {
            if (request.getPermissionCount() < 0) {
                throw bad("Permission Count Cannot Be Negative");
            }
            setting.setPermissionCount(request.getPermissionCount());
        }

        if (request.getPermissionHours() != null) {
            if (request.getPermissionHours() < 0) {
                throw bad("Permission Hours Cannot Be Negative");
            }
            setting.setPermissionHours(request.getPermissionHours());
        }

        setting.setUpdatedAt(LocalDateTime.now(INDIA_ZONE));

        LeavePermissionSetting saved = settingRepository.save(setting);

        saveHistory("UPDATE", saved, oldValues, describe(saved),
                reason, adminEmail);

        return saved;
    }

    // =========================
    // DELETE (DEFAULT cannot be deleted)
    // =========================

    @DeleteMapping("/{id}")
    public String deleteSetting(
            @PathVariable Long id,
            @RequestParam String adminEmail,
            @RequestParam String reason) {

        String email = checkAdmin(adminEmail);
        String cleanReason = checkReason(reason);

        LeavePermissionSetting setting = findSetting(id);

        if ("DEFAULT".equalsIgnoreCase(setting.getSettingType())) {
            throw bad("Default setting cannot be deleted. Edit it instead.");
        }

        saveHistory("DELETE", setting, describe(setting), null,
                cleanReason, email);

        settingRepository.delete(setting);

        return "Setting deleted successfully";
    }

    // =========================
    // HISTORY
    // =========================

    @GetMapping("/history")
    public List<LeaveSettingHistory> getHistory() {
        return historyRepository.findAllByOrderByCreatedAtDesc();
    }

    // =========================
    // HELPERS
    // =========================

    private LeavePermissionSetting findSetting(Long id) {
        return settingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Setting not found"));
    }

    private String checkAdmin(String email) {

        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Admin login required");
        }

        adminRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Logged-in admin not found"));

        return email.trim();
    }

    private String checkReason(String reason) {

        if (reason == null || reason.trim().length() < 3) {
            throw bad("Reason is required (minimum 3 characters)");
        }

        return reason.trim();
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message);
    }

    private String describe(LeavePermissionSetting s) {
        return "Sick=" + s.getSickLeave()
                + ", Casual=" + s.getCasualLeave()
                + ", Permission Count=" + s.getPermissionCount()
                + ", Permission Hours=" + s.getPermissionHours();
    }

    private void saveHistory(
            String action,
            LeavePermissionSetting setting,
            String oldValues,
            String newValues,
            String reason,
            String adminEmail) {

        LeaveSettingHistory history = new LeaveSettingHistory();

        history.setSettingId(setting.getId());
        history.setAction(action);
        history.setSettingType(setting.getSettingType());
        history.setRole(setting.getRole());
        history.setEmployeeId(setting.getEmployeeId());
        history.setOldValues(oldValues);
        history.setNewValues(newValues);
        history.setReason(reason);
        history.setAdminEmail(adminEmail);
        history.setCreatedAt(LocalDateTime.now(INDIA_ZONE));

        historyRepository.save(history);
    }
}