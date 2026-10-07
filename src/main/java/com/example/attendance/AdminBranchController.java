
package com.example.attendance;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/admin/branches")
public class AdminBranchController {

        private final BranchRepository branchRepository;
        private final AdminRepository adminRepository;
        private final AdminActionHistoryRepository historyRepository;

        public AdminBranchController(
                        BranchRepository branchRepository,
                        AdminRepository adminRepository,
                        AdminActionHistoryRepository historyRepository) {

                this.branchRepository = branchRepository;
                this.adminRepository = adminRepository;
                this.historyRepository = historyRepository;
        }

        // =====================================================
        // GET ALL ACTIVE BRANCHES
        // =====================================================

        @GetMapping
        public List<Branch> getAllBranches() {

                return branchRepository
                                .findByActiveTrueOrderByBranchNameAsc();
        }

        // =====================================================
        // GET ALL BRANCHES
        // ACTIVE + INACTIVE
        // ADMIN USE
        // =====================================================

        @GetMapping("/all")
        public List<Branch> getAllBranchesForAdmin() {

                return branchRepository
                                .findAll()
                                .stream()
                                .sorted(
                                                Comparator.comparing(
                                                                Branch::getBranchName,
                                                                String.CASE_INSENSITIVE_ORDER))
                                .toList();
        }

        // =====================================================
        // ADD BRANCH
        // =====================================================

        @PostMapping
        @Transactional
        public Branch addBranch(
                        @RequestBody BranchActionRequest request) {

                validateBranchRequest(request);

                findLoggedInAdmin(
                                request.getAdminEmail());

                String branchName = request.getBranchName()
                                .trim();

                if (branchRepository
                                .findByBranchName(branchName)
                                .isPresent()) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Branch with this name already exists");
                }

                Branch branch = new Branch();

                branch.setBranchName(
                                branchName);

                branch.setLatitude(
                                request.getLatitude());

                branch.setLongitude(
                                request.getLongitude());

                branch.setAllowedRadiusMeters(
                                request.getAllowedRadiusMeters());

                branch.setLocationVerificationEnabled(
                                getBooleanValue(
                                                request.getLocationVerificationEnabled()));

                branch.setFaceVerificationEnabled(
                                getBooleanValue(
                                                request.getFaceVerificationEnabled()));

                branch.setActive(true);

                try {

                        Branch savedBranch = branchRepository.save(branch);

                        // =================================================
                        // SAVE ADD BRANCH HISTORY
                        // =================================================

                        saveBranchHistory(
                                        request.getAdminEmail(),
                                        "ADD BRANCH",
                                        savedBranch.getBranchName(),
                                        "-",
                                        buildBranchDetails(
                                                        savedBranch),
                                        request.getReason());

                        return savedBranch;

                } catch (DataIntegrityViolationException exception) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Branch with this name already exists");
                }
        }

        // =====================================================
        // UPDATE BRANCH
        // =====================================================

        @PutMapping("/{id}")
        @Transactional
        public Branch updateBranch(
                        @PathVariable Long id,
                        @RequestBody BranchActionRequest request) {

                validateBranchRequest(request);

                findLoggedInAdmin(
                                request.getAdminEmail());

                Branch branch = branchRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Branch not found"));

                // =================================================
                // STORE OLD VALUES
                // =================================================

                String oldBranchDetails = buildBranchDetails(
                                branch);

                String branchName = request.getBranchName()
                                .trim();

                branchRepository
                                .findByBranchName(branchName)
                                .ifPresent(existing -> {

                                        if (!existing.getId()
                                                        .equals(branch.getId())) {

                                                throw new ResponseStatusException(
                                                                HttpStatus.CONFLICT,
                                                                "Branch with this name already exists");
                                        }
                                });

                // =================================================
                // UPDATE BASIC BRANCH DETAILS
                // =================================================

                branch.setBranchName(
                                branchName);

                branch.setLatitude(
                                request.getLatitude());

                branch.setLongitude(
                                request.getLongitude());

                branch.setAllowedRadiusMeters(
                                request.getAllowedRadiusMeters());

                // =================================================
                // UPDATE VERIFICATION SETTINGS
                // =================================================

                branch.setLocationVerificationEnabled(
                                getBooleanValue(
                                                request.getLocationVerificationEnabled()));

                branch.setFaceVerificationEnabled(
                                getBooleanValue(
                                                request.getFaceVerificationEnabled()));

                try {

                        Branch savedBranch = branchRepository.save(branch);

                        // =================================================
                        // SAVE EDIT BRANCH HISTORY
                        // =================================================

                        saveBranchHistory(
                                        request.getAdminEmail(),
                                        "EDIT BRANCH",
                                        savedBranch.getBranchName(),
                                        oldBranchDetails,
                                        buildBranchDetails(
                                                        savedBranch),
                                        request.getReason());

                        return savedBranch;

                } catch (DataIntegrityViolationException exception) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Branch with this name already exists");
                }
        }

        // =====================================================
        // DELETE / DEACTIVATE BRANCH
        // =====================================================

        @DeleteMapping("/{id}")
        @Transactional
        public String deleteBranch(
                        @PathVariable Long id,
                        @RequestBody BranchActionRequest request) {

                validateReason(
                                request.getReason());

                findLoggedInAdmin(
                                request.getAdminEmail());

                Branch branch = branchRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Branch not found"));

                String branchName = branch.getBranchName();

                // =================================================
                // DEACTIVATE ONLY
                //
                // Location / Face settings are preserved.
                // They will be used again when branch is activated.
                // =================================================

                branch.setActive(false);

                branchRepository.save(branch);

                // =================================================
                // SAVE DEACTIVATE HISTORY
                // =================================================

                saveBranchHistory(
                                request.getAdminEmail(),
                                "DEACTIVATE BRANCH",
                                branchName,
                                "Active",
                                "Inactive",
                                request.getReason());

                return "Branch deleted successfully";
        }

        // =====================================================
        // ACTIVATE BRANCH
        // =====================================================

        @PutMapping("/{id}/activate")
        @Transactional
        public String activateBranch(
                        @PathVariable Long id,
                        @RequestBody BranchActionRequest request) {

                validateReason(
                                request.getReason());

                findLoggedInAdmin(
                                request.getAdminEmail());

                Branch branch = branchRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Branch not found"));

                if (Boolean.TRUE.equals(
                                branch.getActive())) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Branch is already active");
                }

                String branchName = branch.getBranchName();

                // =================================================
                // ACTIVATE BRANCH
                //
                // Existing Location / Face settings are preserved.
                // =================================================

                branch.setActive(true);

                branchRepository.save(branch);

                // =================================================
                // SAVE ACTIVATE HISTORY
                // =================================================

                saveBranchHistory(
                                request.getAdminEmail(),
                                "ACTIVATE BRANCH",
                                branchName,
                                "Inactive",
                                "Active",
                                request.getReason());

                return "Branch activated successfully";
        }

        // =====================================================
        // SAVE BRANCH HISTORY
        // =====================================================

        private void saveBranchHistory(
                        String adminEmail,
                        String action,
                        String branchName,
                        String oldValue,
                        String newValue,
                        String reason) {

                AdminActionHistory history = new AdminActionHistory();

                // Branch actions do not belong
                // to an employee.
                history.setEmployeeId(
                                null);

                history.setEmployeeCode(
                                null);

                history.setEmployeeName(
                                "Branch: " + branchName);

                // Logged-in admin email
                history.setAdminName(
                                adminEmail);

                history.setAction(
                                action);

                history.setActionDate(
                                LocalDate.now());

                history.setActionTime(
                                LocalTime.now());

                history.setFieldName(
                                "Branch");

                history.setOldValue(
                                oldValue);

                history.setNewValue(
                                newValue);

                history.setReason(
                                reason);

                historyRepository.save(
                                history);
        }

        // =====================================================
        // BUILD BRANCH DETAILS
        // =====================================================

        private String buildBranchDetails(
                        Branch branch) {

                return "Branch Name=" +
                                branch.getBranchName() +

                                ", Latitude=" +
                                branch.getLatitude() +

                                ", Longitude=" +
                                branch.getLongitude() +

                                ", Radius=" +
                                branch.getAllowedRadiusMeters() +

                                "m" +

                                ", Location Verification=" +
                                Boolean.TRUE.equals(
                                                branch.getLocationVerificationEnabled())
                                +

                                ", Face Verification=" +
                                Boolean.TRUE.equals(
                                                branch.getFaceVerificationEnabled());
        }

        // =====================================================
        // BOOLEAN VALUE
        // =====================================================

        private Boolean getBooleanValue(
                        Boolean value) {

                return Boolean.TRUE.equals(value);
        }

        // =====================================================
        // VALIDATE BRANCH REQUEST
        // =====================================================

        private void validateBranchRequest(
                        BranchActionRequest request) {

                if (request == null) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Branch request is required");
                }

                if (request.getBranchName() == null
                                || request.getBranchName()
                                                .trim()
                                                .isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Branch name is required");
                }

                if (request.getLatitude() == null) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Branch latitude is required");
                }

                if (request.getLongitude() == null) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Branch longitude is required");
                }

                if (request.getLatitude() < -90
                                || request.getLatitude() > 90) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid branch latitude");
                }

                if (request.getLongitude() < -180
                                || request.getLongitude() > 180) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid branch longitude");
                }

                if (request.getAllowedRadiusMeters() == null
                                || request.getAllowedRadiusMeters() <= 0) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Allowed radius must be greater than zero");
                }

                validateReason(
                                request.getReason());
        }

        // =====================================================
        // FIND LOGGED-IN ADMIN
        // =====================================================

        private Admin findLoggedInAdmin(
                        String email) {

                if (email == null
                                || email.trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Admin login required");
                }

                return adminRepository
                                .findByEmail(
                                                email.trim())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Logged-in admin not found"));
        }

        // =====================================================
        // REASON VALIDATION
        // =====================================================

        private void validateReason(
                        String reason) {

                if (reason == null
                                || reason.trim().isEmpty()) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Reason is required for this operation");
                }

                if (reason.trim().length() < 3) {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Please enter a valid reason");
                }
        }
}
