
package com.example.attendance;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {
                "https://attendance-management-3d-webinar.vercel.app",
                "attendance-management-git-main-3d-webinar.vercel.app",
                "attendance-management-h43414kik-3d-webinar.vercel.app",
                "https://attendance-management-nine-beige.vercel.app"
})
@RequestMapping("/attendance")
public class AttendanceController {

        private final AttendanceRepository attendanceRepository;
        private final EmployeeRepository employeeRepository;
        private final LeaveRequestRepository leaveRequestRepository;
        private final EmployeeLeaveBalanceRepository balanceRepository;
        private final LeavePermissionSettingRepository settingRepository;
        private final ShiftSettingRepository shiftSettingRepository;
        private final EmployeeFaceRepository employeeFaceRepository;
        private final VerificationPolicyRepository verificationPolicyRepository;

        private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
        private static final LocalTime DEFAULT_OFFICE_START_TIME = LocalTime.of(9, 0);
        private static final LocalTime DEFAULT_OFFICE_END_TIME = LocalTime.of(18, 0);
        private static final int GRACE_MINUTES = 5;
        private static final double EARTH_RADIUS_METERS = 6_371_000.0;
        private static final double FACE_SIMILARITY_THRESHOLD = 0.60;

        public AttendanceController(
                        AttendanceRepository attendanceRepository,
                        EmployeeRepository employeeRepository,
                        LeaveRequestRepository leaveRequestRepository,
                        EmployeeLeaveBalanceRepository balanceRepository,
                        LeavePermissionSettingRepository settingRepository,
                        ShiftSettingRepository shiftSettingRepository,
                        EmployeeFaceRepository employeeFaceRepository,
                        VerificationPolicyRepository verificationPolicyRepository) {

                this.attendanceRepository = attendanceRepository;
                this.employeeRepository = employeeRepository;
                this.leaveRequestRepository = leaveRequestRepository;
                this.balanceRepository = balanceRepository;
                this.settingRepository = settingRepository;
                this.shiftSettingRepository = shiftSettingRepository;
                this.employeeFaceRepository = employeeFaceRepository;
                this.verificationPolicyRepository = verificationPolicyRepository;
        }

        // =====================================================
        // TEST API
        // =====================================================

        @GetMapping("/")
        public String home() {
                return "Attendance API Running Successfully!";
        }

        // =====================================================
        // EFFECTIVE VERIFICATION POLICY
        // =====================================================

        private static class EffectiveVerification {

                boolean locationRequired;
                boolean faceRequired;
                boolean locationCaptureEnabled = true;
                boolean photoCaptureEnabled;
                boolean branchActive;
                String branchName = "No Branch";
                String policyName = "Branch Settings";
        }

        private EffectiveVerification getEffectiveVerification(
                        Employee employee,
                        LocalDate date,
                        LocalTime time) {

                EffectiveVerification result = new EffectiveVerification();

                Branch branch = getEmployeeBranch(employee);

                if (branch == null) {
                        result.branchActive = false;
                        result.locationRequired = false;
                        result.faceRequired = false;
                        result.locationCaptureEnabled = false;
                        result.branchName = "No Branch";
                        result.policyName = "No Branch";
                        return result;
                }

                result.branchName = branch.getBranchName();
                result.branchActive = Boolean.TRUE.equals(branch.getActive());

                List<VerificationPolicy> policies = verificationPolicyRepository.findByActiveTrueOrderByIdDesc();

                VerificationPolicy selected = null;
                int selectedPriority = Integer.MAX_VALUE;

                for (VerificationPolicy policy : policies) {

                        if (!isPolicyInScope(policy, branch)) {
                                continue;
                        }

                        if (!isPolicyWithinSchedule(policy, date, time)) {
                                continue;
                        }

                        int priority = getPolicyPriority(policy, employee, branch);

                        if (priority < selectedPriority) {
                                selected = policy;
                                selectedPriority = priority;
                        }
                }

                if (selected != null) {

                        result.policyName = selected.getPolicyName();

                        result.locationRequired = Boolean.TRUE.equals(
                                        selected.getLocationVerificationEnabled());

                        result.faceRequired = Boolean.TRUE.equals(
                                        selected.getFaceVerificationEnabled());

                        result.locationCaptureEnabled = Boolean.TRUE.equals(
                                        selected.getLocationCaptureEnabled());

                        result.photoCaptureEnabled = Boolean.TRUE.equals(
                                        selected.getPhotoCaptureEnabled());

                } else {

                        result.locationRequired = Boolean.TRUE.equals(
                                        branch.getLocationVerificationEnabled());

                        result.faceRequired = Boolean.TRUE.equals(
                                        branch.getFaceVerificationEnabled());

                        result.locationCaptureEnabled = true;
                        result.photoCaptureEnabled = false;
                        result.policyName = "Branch Settings";
                }

                // Inactive branches bypass verification, not photo policy.
                if (!result.branchActive) {
                        result.locationRequired = false;
                        result.faceRequired = false;
                }

                return result;
        }

        private boolean isPolicyInScope(
                        VerificationPolicy policy,
                        Branch employeeBranch) {

                Branch policyBranch = policy.getBranch();

                if (policyBranch == null) {
                        return true;
                }

                return employeeBranch != null
                                && policyBranch.getId() != null
                                && policyBranch.getId().equals(employeeBranch.getId());
        }

        private boolean isPolicyWithinSchedule(
                        VerificationPolicy policy,
                        LocalDate date,
                        LocalTime time) {

                if (policy.getStartDate() != null
                                && date.isBefore(policy.getStartDate())) {
                        return false;
                }

                if (policy.getEndDate() != null
                                && date.isAfter(policy.getEndDate())) {
                        return false;
                }

                LocalTime start = policy.getStartTime();
                LocalTime end = policy.getEndTime();

                if (start == null && end == null) {
                        return true;
                }

                if (start == null || end == null) {
                        return false;
                }

                if (start.isAfter(end)) {
                        return !time.isBefore(start) || !time.isAfter(end);
                }

                return !time.isBefore(start) && !time.isAfter(end);
        }

        private int getPolicyPriority(
                        VerificationPolicy policy,
                        Employee employee,
                        Branch branch) {

                String type = normalizeValue(policy.getTargetType());
                String value = policy.getTargetValue();

                if ("EMPLOYEE".equals(type)) {
                        return matchesEmployee(employee, value) ? 1 : Integer.MAX_VALUE;
                }

                // Manager policy is not matched without a confirmed relationship.
                if ("MANAGER".equals(type)) {
                        return Integer.MAX_VALUE;
                }

                if ("DESIGNATION".equals(type)) {
                        return sameValue(employee.getDesignation(), value)
                                        ? 3
                                        : Integer.MAX_VALUE;
                }

                if ("ROLE".equals(type)) {
                        return sameValue(employee.getRole(), value)
                                        ? 4
                                        : Integer.MAX_VALUE;
                }

                if ("INTERN".equals(type)) {
                        return isIntern(employee) ? 5 : Integer.MAX_VALUE;
                }

                if ("STUDENT".equals(type)) {
                        return isStudent(employee) ? 5 : Integer.MAX_VALUE;
                }

                if ("DEFAULT".equals(type)) {

                        if (policy.getBranch() != null
                                        && branch != null
                                        && policy.getBranch().getId() != null
                                        && policy.getBranch().getId().equals(branch.getId())) {
                                return 6;
                        }

                        if (policy.getBranch() == null) {
                                return 7;
                        }
                }

                return Integer.MAX_VALUE;
        }

        private boolean matchesEmployee(Employee employee, String value) {

                if (employee == null || value == null || value.isBlank()) {
                        return false;
                }

                return (employee.getId() != null
                                && value.trim().equals(String.valueOf(employee.getId())))
                                || sameValue(employee.getEmployeeCode(), value)
                                || sameValue(employee.getEmail(), value);
        }

        private boolean isIntern(Employee employee) {

                return sameValue(employee.getEmployeeType(), "INTERN")
                                || containsIgnoreCase(employee.getDesignation(), "intern")
                                || containsIgnoreCase(employee.getRole(), "intern")
                                || employee.getInternshipStartDate() != null
                                || employee.getInternshipEndDate() != null
                                || employee.getStipendType() != null;
        }

        private boolean isStudent(Employee employee) {

                return sameValue(employee.getEmployeeType(), "STUDENT")
                                || containsIgnoreCase(employee.getDesignation(), "student")
                                || containsIgnoreCase(employee.getRole(), "student")
                                || (employee.getCourse() != null
                                                && !employee.getCourse().isBlank());
        }

        private boolean sameValue(String first, String second) {

                return first != null
                                && second != null
                                && first.trim().equalsIgnoreCase(second.trim());
        }

        private boolean containsIgnoreCase(String value, String search) {

                return value != null
                                && value.toLowerCase().contains(search.toLowerCase());
        }

        private String normalizeValue(String value) {
                return value == null ? "" : value.trim().toUpperCase();
        }

        // =====================================================
        // PHOTO CAPTURE VALIDATION
        // =====================================================

        private String validatePhotoIfRequired(
                        EffectiveVerification settings,
                        String photoData,
                        String action) {

                if (!settings.photoCaptureEnabled) {
                        return null;
                }

                if (photoData == null || photoData.isBlank()) {
                        return action + " Not Allowed - Photo capture is required";
                }

                String photo = photoData.trim();

                // Accept a Base64 data URL or raw Base64 content.
                if (photo.startsWith("data:image/")) {

                        int commaIndex = photo.indexOf(',');

                        if (commaIndex < 0
                                        || !photo.substring(0, commaIndex)
                                                        .toLowerCase().contains(";base64")) {
                                return action + " Not Allowed - Invalid photo data";
                        }

                        photo = photo.substring(commaIndex + 1);
                }

                if (photo.isBlank()) {
                        return action + " Not Allowed - Invalid photo data";
                }

                // Prevent unreasonably large request payloads.
                if (photo.length() > 10_000_000) {
                        return action + " Not Allowed - Photo is too large";
                }

                try {
                        java.util.Base64.getDecoder().decode(photo);
                } catch (IllegalArgumentException ex) {
                        return action + " Not Allowed - Invalid photo data";
                }

                return null;
        }

        private String normalizePhotoData(String photoData) {

                if (photoData == null) {
                        return null;
                }

                String photo = photoData.trim();

                if (photo.startsWith("data:image/")) {
                        int commaIndex = photo.indexOf(',');

                        if (commaIndex >= 0) {
                                return photo.substring(commaIndex + 1);
                        }
                }

                return photo;
        }

        // =====================================================
        // GET EFFECTIVE VERIFICATION SETTINGS
        // =====================================================

        @GetMapping("/verification-settings")
        public Map<String, Object> getVerificationSettings(
                        @RequestParam String email) {

                Map<String, Object> response = new LinkedHashMap<>();

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email)
                                .orElse(null);

                if (employee == null) {
                        response.put("success", false);
                        response.put("message", "Employee Not Found or Employee is Inactive");
                        response.put("locationRequired", false);
                        response.put("faceRequired", false);
                        response.put("locationCaptureEnabled", false);
                        response.put("photoCaptureEnabled", false);
                        response.put("branchActive", false);
                        return response;
                }

                LocalDate today = LocalDate.now(INDIA_ZONE);
                LocalTime now = LocalTime.now(INDIA_ZONE);

                EffectiveVerification settings = getEffectiveVerification(employee, today, now);

                response.put("success", true);
                response.put("branchActive", settings.branchActive);
                response.put("branchName", settings.branchName);
                response.put("locationRequired", settings.locationRequired);
                response.put("faceRequired", settings.faceRequired);
                response.put("locationCaptureEnabled", settings.locationCaptureEnabled);
                response.put("photoCaptureEnabled", settings.photoCaptureEnabled);
                response.put("policyName", settings.policyName);

                return response;
        }

        // =====================================================
        // GET EFFECTIVE SHIFT
        // PRIORITY: EMPLOYEE > ROLE > DEFAULT
        // =====================================================

        private ShiftSetting getEffectiveShiftSetting(Employee employee) {

                String shiftType = "GENERAL";

                if (employee.getId() != null) {

                        var employeeSetting = settingRepository
                                        .findBySettingTypeAndEmployeeId("EMPLOYEE", employee.getId());

                        if (employeeSetting.isPresent()) {

                                String configuredShift = employeeSetting.get().getShiftType();

                                if (configuredShift != null && !configuredShift.isBlank()) {
                                        shiftType = normalizeShiftType(configuredShift);
                                }
                        }
                }

                if ("GENERAL".equals(shiftType)
                                && employee.getRole() != null
                                && !employee.getRole().isBlank()) {

                        var roleSetting = settingRepository
                                        .findBySettingTypeAndRole(
                                                        "ROLE", employee.getRole().trim().toUpperCase());

                        if (roleSetting.isPresent()) {

                                String configuredShift = roleSetting.get().getShiftType();

                                if (configuredShift != null && !configuredShift.isBlank()) {
                                        shiftType = normalizeShiftType(configuredShift);
                                }
                        }
                }

                if ("GENERAL".equals(shiftType)) {

                        var defaultSetting = settingRepository.findBySettingType("DEFAULT");

                        if (defaultSetting.isPresent()) {

                                String configuredShift = defaultSetting.get().getShiftType();

                                if (configuredShift != null && !configuredShift.isBlank()) {
                                        shiftType = normalizeShiftType(configuredShift);
                                }
                        }
                }

                return shiftSettingRepository.findByShiftType(shiftType)
                                .orElseGet(() -> shiftSettingRepository
                                                .findByShiftType("GENERAL")
                                                .orElse(null));
        }

        private String normalizeShiftType(String shiftType) {

                if (shiftType == null || shiftType.isBlank()) {
                        return "GENERAL";
                }

                return shiftType.trim().toUpperCase()
                                .replace("-", "_")
                                .replace(" ", "_");
        }

        private LocalTime getOfficeStartTime(Employee employee) {

                ShiftSetting shift = getEffectiveShiftSetting(employee);

                if (shift != null && shift.getStartTime() != null) {
                        return shift.getStartTime();
                }

                return DEFAULT_OFFICE_START_TIME;
        }

        private LocalTime getOfficeEndTime(Employee employee) {

                ShiftSetting shift = getEffectiveShiftSetting(employee);

                if (shift != null && shift.getEndTime() != null) {
                        return shift.getEndTime();
                }

                return DEFAULT_OFFICE_END_TIME;
        }

        private Branch getEmployeeBranch(Employee employee) {
                return employee.getBranch();
        }

        // =====================================================
        // LOCATION PRE-CHECK (the page asks this BEFORE opening the camera)
        // check-in / check-out still verify the location again on the server
        // =====================================================

        @GetMapping("/location-check")
        public Map<String, Object> locationCheck(
                        @RequestParam String email,
                        @RequestParam(required = false) Double latitude,
                        @RequestParam(required = false) Double longitude) {

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("allowed", false);

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email)
                                .orElse(null);

                if (employee == null) {
                        response.put("message", "Employee Not Found or Employee is Inactive");
                        return response;
                }

                EffectiveVerification settings = getEffectiveVerification(
                                employee,
                                LocalDate.now(INDIA_ZONE),
                                LocalTime.now(INDIA_ZONE));

                if (!settings.branchActive || !settings.locationRequired) {
                        response.put("allowed", true);
                        response.put("message", "Location check is not required.");
                        return response;
                }

                Branch branch = getEmployeeBranch(employee);

                if (branch == null
                                || branch.getLatitude() == null
                                || branch.getLongitude() == null
                                || branch.getAllowedRadiusMeters() == null) {
                        response.put("message", "Your branch location is not set. Please contact your admin.");
                        return response;
                }

                if (!isValidCoordinate(latitude, longitude)) {
                        response.put("message", "Your location could not be read. Turn on location and try again.");
                        return response;
                }

                double distance = calculateDistanceInMeters(
                                latitude,
                                longitude,
                                branch.getLatitude(),
                                branch.getLongitude());

                int radius = branch.getAllowedRadiusMeters();

                response.put("distanceMeters", Math.round(distance));
                response.put("allowedRadiusMeters", radius);
                response.put("branchName", branch.getBranchName());

                if (distance <= radius) {
                        response.put("allowed", true);
                        response.put("message", "You are at " + branch.getBranchName() + ".");
                } else {
                        response.put("message", "You are about " + Math.round(distance)
                                        + " m away from " + branch.getBranchName()
                                        + ". Attendance is allowed only within " + radius + " m.");
                }

                return response;
        }

        // =====================================================
        // LOCATION VALIDATION
        // =====================================================

        private boolean isInsideAssignedBranch(
                        Branch branch,
                        Double employeeLatitude,
                        Double employeeLongitude) {

                if (branch == null
                                || employeeLatitude == null
                                || employeeLongitude == null) {
                        return false;
                }

                if (!Double.isFinite(employeeLatitude)
                                || !Double.isFinite(employeeLongitude)
                                || employeeLatitude < -90
                                || employeeLatitude > 90
                                || employeeLongitude < -180
                                || employeeLongitude > 180) {
                        return false;
                }

                if (branch.getLatitude() == null
                                || branch.getLongitude() == null
                                || branch.getAllowedRadiusMeters() == null) {
                        return false;
                }

                double distance = calculateDistanceInMeters(
                                employeeLatitude,
                                employeeLongitude,
                                branch.getLatitude(),
                                branch.getLongitude());

                return distance <= branch.getAllowedRadiusMeters();
        }

        private double calculateDistanceInMeters(
                        double latitude1,
                        double longitude1,
                        double latitude2,
                        double longitude2) {

                double lat1Radians = Math.toRadians(latitude1);
                double lat2Radians = Math.toRadians(latitude2);
                double latitudeDifference = Math.toRadians(latitude2 - latitude1);
                double longitudeDifference = Math.toRadians(longitude2 - longitude1);

                double a = Math.sin(latitudeDifference / 2)
                                * Math.sin(latitudeDifference / 2)
                                + Math.cos(lat1Radians)
                                                * Math.cos(lat2Radians)
                                                * Math.sin(longitudeDifference / 2)
                                                * Math.sin(longitudeDifference / 2);

                double c = 2 * Math.atan2(
                                Math.sqrt(a),
                                Math.sqrt(Math.max(0.0, 1.0 - a)));

                return EARTH_RADIUS_METERS * c;
        }

        private boolean isValidCoordinate(Double latitude, Double longitude) {

                return latitude != null
                                && longitude != null
                                && Double.isFinite(latitude)
                                && Double.isFinite(longitude)
                                && latitude >= -90
                                && latitude <= 90
                                && longitude >= -180
                                && longitude <= 180;
        }

        private void captureCheckInLocation(
                        Attendance attendance,
                        EffectiveVerification settings,
                        Double latitude,
                        Double longitude) {

                if (!settings.locationCaptureEnabled
                                || !isValidCoordinate(latitude, longitude)) {
                        return;
                }

                attendance.setCheckInLatitude(
                                new BigDecimal(Double.toString(latitude)));

                attendance.setCheckInLongitude(
                                new BigDecimal(Double.toString(longitude)));
        }

        private void captureCheckOutLocation(
                        Attendance attendance,
                        EffectiveVerification settings,
                        Double latitude,
                        Double longitude) {

                if (!settings.locationCaptureEnabled
                                || !isValidCoordinate(latitude, longitude)) {
                        return;
                }

                attendance.setCheckOutLatitude(
                                new BigDecimal(Double.toString(latitude)));

                attendance.setCheckOutLongitude(
                                new BigDecimal(Double.toString(longitude)));
        }

        // =====================================================
        // FACE VERIFICATION
        // =====================================================

        private String verifyEmployeeFace(Employee employee, String faceData) {

                if (faceData == null || faceData.trim().isEmpty()) {
                        return "FACE_DATA_REQUIRED";
                }

                EmployeeFace registeredFace = employeeFaceRepository
                                .findByEmployeeIdAndActiveTrue(employee.getId().longValue())
                                .orElse(null);

                if (registeredFace == null) {
                        return "FACE_NOT_REGISTERED";
                }

                try {

                        List<Double> registeredEmbedding = parseFaceEmbedding(registeredFace.getFaceData());

                        List<Double> currentEmbedding = parseFaceEmbedding(faceData);

                        if (registeredEmbedding.isEmpty()
                                        || currentEmbedding.isEmpty()) {
                                return "INVALID_FACE_DATA";
                        }

                        if (registeredEmbedding.size() != currentEmbedding.size()) {
                                return "FACE_DATA_FORMAT_MISMATCH";
                        }

                        double similarity = calculateCosineSimilarity(
                                        registeredEmbedding, currentEmbedding);

                        return similarity >= FACE_SIMILARITY_THRESHOLD
                                        ? "FACE_MATCH"
                                        : "FACE_MISMATCH";

                } catch (Exception ex) {
                        return "INVALID_FACE_DATA";
                }
        }

        private List<Double> parseFaceEmbedding(String faceData) {

                List<Double> values = new ArrayList<>();

                if (faceData == null || faceData.trim().isEmpty()) {
                        return values;
                }

                String data = faceData.trim();

                if (!data.startsWith("[") || !data.endsWith("]")) {
                        return values;
                }

                data = data.substring(1, data.length() - 1).trim();

                if (data.isEmpty()) {
                        return values;
                }

                String[] numbers = data.split(",");

                for (String number : numbers) {

                        String value = number.trim();

                        if (value.isEmpty()) {
                                return List.of();
                        }

                        double parsed = Double.parseDouble(value);

                        if (!Double.isFinite(parsed)) {
                                return List.of();
                        }

                        values.add(parsed);
                }

                return values;
        }

        private double calculateCosineSimilarity(
                        List<Double> first,
                        List<Double> second) {

                double dotProduct = 0.0;
                double firstMagnitude = 0.0;
                double secondMagnitude = 0.0;

                for (int i = 0; i < first.size(); i++) {

                        double firstValue = first.get(i);
                        double secondValue = second.get(i);

                        dotProduct += firstValue * secondValue;
                        firstMagnitude += firstValue * firstValue;
                        secondMagnitude += secondValue * secondValue;
                }

                if (firstMagnitude == 0.0 || secondMagnitude == 0.0) {
                        return 0.0;
                }

                return dotProduct
                                / (Math.sqrt(firstMagnitude) * Math.sqrt(secondMagnitude));
        }

        private String validateFaceIfRequired(
                        Employee employee,
                        EffectiveVerification settings,
                        String faceData,
                        String action) {

                if (!settings.branchActive || !settings.faceRequired) {
                        return null;
                }

                String faceResult = verifyEmployeeFace(employee, faceData);

                switch (faceResult) {

                        case "FACE_DATA_REQUIRED":
                                return action + " Not Allowed - Face verification is required";

                        case "FACE_NOT_REGISTERED":
                                return action + " Not Allowed - Face is not registered. "
                                                + "Please complete face registration";

                        case "FACE_MISMATCH":
                                return action + " Not Allowed - Face verification failed";

                        case "FACE_MATCH":
                                return null;

                        default:
                                return action + " Not Allowed - Invalid face verification data";
                }
        }

        // =====================================================
        // FORMAT DURATION
        // =====================================================

        private String formatDuration(long totalMinutes) {

                if (totalMinutes <= 0) {
                        return "0 mins";
                }

                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;

                if (hours > 0 && minutes > 0) {
                        return hours + (hours == 1 ? " hr " : " hrs ")
                                        + minutes + " mins";
                }

                if (hours > 0) {
                        return hours + (hours == 1 ? " hr" : " hrs");
                }

                return minutes + " mins";
        }

        private long calculateLateMinutes(
                        LocalTime officeStartTime,
                        LocalTime checkInTime) {

                if (checkInTime == null || officeStartTime == null
                                || !checkInTime.isAfter(officeStartTime)) {
                        return 0;
                }

                return Duration.between(officeStartTime, checkInTime).toMinutes();
        }

        private long calculateEarlyCheckoutMinutes(
                        LocalTime checkOutTime,
                        LocalTime officeEndTime) {

                if (checkOutTime == null || officeEndTime == null
                                || !checkOutTime.isBefore(officeEndTime)) {
                        return 0;
                }

                return Duration.between(checkOutTime, officeEndTime).toMinutes();
        }

        // =====================================================
        // CHECK-IN / RE-ENTRY
        // =====================================================

        @PostMapping("/check-in")
        @Transactional
        public String checkIn(
                        @RequestParam String email,
                        @RequestParam(required = false) Double latitude,
                        @RequestParam(required = false) Double longitude,
                        @RequestParam(required = false) String faceData,
                        @RequestParam(required = false) String checkInPhoto) {

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email)
                                .orElse(null);

                if (employee == null) {
                        return "Employee Not Found or Employee is Inactive";
                }

                LocalDate today = LocalDate.now(INDIA_ZONE);
                LocalTime currentTime = LocalTime.now(INDIA_ZONE);

                EffectiveVerification settings = getEffectiveVerification(employee, today, currentTime);

                // Validate required photo before changing attendance or leave data.
                String photoError = validatePhotoIfRequired(
                                settings, checkInPhoto, "Check In");

                if (photoError != null) {
                        return photoError;
                }

                Branch assignedBranch = getEmployeeBranch(employee);

                if (settings.branchActive && settings.locationRequired
                                && !isInsideAssignedBranch(
                                                assignedBranch, latitude, longitude)) {

                        return "Attendance Not Allowed - "
                                        + "You are outside your assigned branch location";
                }

                String faceError = validateFaceIfRequired(
                                employee, settings, faceData, "Attendance");

                if (faceError != null) {
                        return faceError;
                }

                var openAttendance = attendanceRepository
                                .findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(
                                                employee.getId(), today);

                if (openAttendance.isPresent()) {
                        return "Already Checked In - Please Check Out First";
                }

                long todaySessionCount = attendanceRepository
                                .countByEmployeeIdAndAttendanceDate(employee.getId(), today);

                if (todaySessionCount >= 2) {
                        return "Check In Not Allowed - Maximum 2 attendance sessions per day "
                                        + "are already completed";
                }

                boolean firstSessionOfDay = true;

                List<Attendance> employeeAttendance = attendanceRepository
                                .findByEmployeeIdOrderByAttendanceDateDescCheckInDesc(
                                                employee.getId());

                for (Attendance existingAttendance : employeeAttendance) {

                        if (today.equals(existingAttendance.getAttendanceDate())) {
                                firstSessionOfDay = false;
                                break;
                        }
                }

                if (firstSessionOfDay) {
                        cancelApprovedLeaveForCheckIn(employee.getId(), today);
                }

                Attendance attendance = new Attendance();

                attendance.setEmployeeId(employee.getId());
                attendance.setAttendanceDate(today);
                attendance.setCheckIn(currentTime);

                captureCheckInLocation(
                                attendance, settings, latitude, longitude);

                if (settings.photoCaptureEnabled) {
                        attendance.setCheckInPhoto(normalizePhotoData(checkInPhoto));
                }

                if (firstSessionOfDay) {

                        LocalTime officeStartTime = getOfficeStartTime(employee);
                        LocalTime graceEndTime = officeStartTime.plusMinutes(GRACE_MINUTES);

                        if (!currentTime.isAfter(graceEndTime)) {
                                attendance.setStatus("PRESENT");
                        } else {
                                long lateMinutes = calculateLateMinutes(
                                                officeStartTime, currentTime);

                                attendance.setStatus(
                                                "LATE - " + formatDuration(lateMinutes));
                        }

                } else {
                        attendance.setStatus("RE-ENTRY");
                }

                attendanceRepository.save(attendance);

                String branchName = assignedBranch != null
                                ? assignedBranch.getBranchName()
                                : "No Branch";

                if (attendance.getStatus() != null
                                && attendance.getStatus().startsWith("LATE")) {

                        return "Check In Successful - " + employee.getName()
                                        + " (" + attendance.getStatus() + ") - " + branchName;
                }

                if (!firstSessionOfDay) {
                        return "Check In Successful - " + employee.getName()
                                        + " (RE-ENTRY) - " + branchName;
                }

                return "Check In Successful - " + employee.getName()
                                + " - " + branchName;
        }

        // =====================================================
        // AUTO-CANCEL LEAVE WHEN EMPLOYEE CHECKS IN
        // Sick / Casual / Half-day leave of the day is cancelled (kept as
        // history with status CANCELLED + reason), balance is given back,
        // so neither leave balance nor salary is affected.
        // Permission is never cancelled.
        // =====================================================

        private static final String CHECK_IN_CANCEL_REASON = "Cancelled automatically - employee came to office that day";

        private void cancelApprovedLeaveForCheckIn(
                        Integer employeeId,
                        LocalDate today) {

                List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeIdAndLeaveDate(
                                employeeId, today);

                if (leaveRequests == null || leaveRequests.isEmpty()) {
                        return;
                }

                LocalDateTime now = LocalDateTime.now(INDIA_ZONE);

                for (LeaveRequest leave : leaveRequests) {

                        String status = leave.getStatus() == null
                                        ? ""
                                        : leave.getStatus().trim().toUpperCase();

                        // Only live requests; rejected / already cancelled are left as they are.
                        if (!status.equals("APPROVED") && !status.equals("PENDING")) {
                                continue;
                        }

                        String leaveType = leave.getLeaveType();

                        if (leaveType == null
                                        || leaveType.equalsIgnoreCase("PERMISSION")) {
                                continue;
                        }

                        // Balance is taken at APPLY time (pending too), so give it back for
                        // both PENDING and APPROVED. LOP rows never took any balance.
                        if (leaveType.equalsIgnoreCase("SICK")
                                        || leaveType.equalsIgnoreCase("CASUAL")) {
                                restorePaidLeaveBalance(leave);
                        }

                        leave.setStatus("CANCELLED");
                        leave.setDecisionReason(CHECK_IN_CANCEL_REASON);
                        leave.setDecidedAt(now);
                        leave.setEmployeeDecisionSeen(false);

                        leaveRequestRepository.save(leave);
                }
        }

        private void restorePaidLeaveBalance(LeaveRequest leave) {

                if (leave.getEmployeeId() == null || leave.getLeaveDate() == null) {
                        return;
                }

                LocalDate month = leave.getLeaveDate().withDayOfMonth(1);

                var balanceOptional = balanceRepository
                                .findByEmployeeIdAndBalanceMonth(
                                                leave.getEmployeeId(), month);

                if (balanceOptional.isEmpty()) {
                        return;
                }

                EmployeeLeaveBalance balance = balanceOptional.get();

                double duration = leave.getLeaveDuration() != null
                                ? leave.getLeaveDuration()
                                : 1.0;

                // Only the paid part was taken from the balance; LOP days never were.
                double lop = leave.getLopDays() != null ? leave.getLopDays() : 0.0;
                double giveBack = Math.max(0.0, duration - lop);

                if (giveBack <= 0) {
                        return;
                }

                String leaveType = leave.getLeaveType();

                if (leaveType.equalsIgnoreCase("SICK")) {

                        double current = balance.getSickBalance() != null
                                        ? balance.getSickBalance()
                                        : 0.0;

                        balance.setSickBalance(current + giveBack);
                }

                if (leaveType.equalsIgnoreCase("CASUAL")) {

                        double current = balance.getCasualBalance() != null
                                        ? balance.getCasualBalance()
                                        : 0.0;

                        balance.setCasualBalance(current + giveBack);
                }

                balance.setUpdatedAt(LocalDateTime.now(INDIA_ZONE));
                balanceRepository.save(balance);
        }

        // =====================================================
        // CHECK-OUT
        // =====================================================

        @PostMapping("/check-out")
        @Transactional
        public String checkOut(
                        @RequestParam String email,
                        @RequestParam(required = false) Double latitude,
                        @RequestParam(required = false) Double longitude,
                        @RequestParam(required = false) String faceData,
                        @RequestParam(required = false) String checkOutPhoto) {

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email)
                                .orElse(null);

                if (employee == null) {
                        return "Employee Not Found or Employee is Inactive";
                }

                LocalDate today = LocalDate.now(INDIA_ZONE);
                LocalTime currentTime = LocalTime.now(INDIA_ZONE);

                EffectiveVerification settings = getEffectiveVerification(employee, today, currentTime);

                // Validate required photo before changing the checkout record.
                String photoError = validatePhotoIfRequired(
                                settings, checkOutPhoto, "Check Out");

                if (photoError != null) {
                        return photoError;
                }

                Branch assignedBranch = getEmployeeBranch(employee);

                if (settings.branchActive && settings.locationRequired
                                && !isInsideAssignedBranch(
                                                assignedBranch, latitude, longitude)) {

                        return "Check-out Not Allowed - "
                                        + "You are outside your assigned branch location";
                }

                String faceError = validateFaceIfRequired(
                                employee, settings, faceData, "Check-out");

                if (faceError != null) {
                        return faceError;
                }

                Attendance attendance = attendanceRepository
                                .findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(
                                                employee.getId(), today)
                                .orElse(null);

                if (attendance == null) {
                        return "Please Check In First";
                }

                attendance.setCheckOut(currentTime);

                captureCheckOutLocation(
                                attendance, settings, latitude, longitude);

                if (settings.photoCaptureEnabled) {
                        attendance.setCheckOutPhoto(normalizePhotoData(checkOutPhoto));
                }

                LocalTime officeEndTime = getOfficeEndTime(employee);

                if (currentTime.isBefore(officeEndTime)) {

                        long earlyMinutes = calculateEarlyCheckoutMinutes(
                                        currentTime, officeEndTime);

                        String earlyDuration = formatDuration(earlyMinutes);
                        String currentStatus = attendance.getStatus();

                        if (currentStatus != null && currentStatus.startsWith("LATE - ")) {
                                attendance.setStatus(
                                                currentStatus + " / EARLY CHECK-OUT - " + earlyDuration);

                        } else if ("RE-ENTRY".equalsIgnoreCase(currentStatus)) {
                                attendance.setStatus(
                                                "RE-ENTRY / EARLY CHECK-OUT - " + earlyDuration);

                        } else {
                                attendance.setStatus("EARLY CHECK-OUT - " + earlyDuration);
                        }

                } else {

                        if (attendance.getStatus() != null
                                        && attendance.getStatus().startsWith("LATE - ")) {
                                // Preserve late status.

                        } else if ("RE-ENTRY".equalsIgnoreCase(attendance.getStatus())) {
                                attendance.setStatus("RE-ENTRY");

                        } else {
                                attendance.setStatus("PRESENT");
                        }
                }

                attendanceRepository.save(attendance);

                String branchName = assignedBranch != null
                                ? assignedBranch.getBranchName()
                                : "No Branch";

                if (currentTime.isBefore(officeEndTime)) {

                        long earlyMinutes = calculateEarlyCheckoutMinutes(
                                        currentTime, officeEndTime);

                        return "Check Out Successful - " + employee.getName()
                                        + " (EARLY CHECK-OUT - "
                                        + formatDuration(earlyMinutes) + ") - " + branchName;
                }

                if (attendance.getStatus() != null
                                && attendance.getStatus().startsWith("LATE - ")) {

                        return "Check Out Successful - " + employee.getName()
                                        + " (" + attendance.getStatus() + ") - " + branchName;
                }

                return "Check Out Successful - " + employee.getName()
                                + " - " + branchName;
        }

        // =====================================================
        // EMPLOYEE ATTENDANCE HISTORY
        // =====================================================

        @GetMapping("/history")
        public List<AttendanceResponse> getHistory(
                        @RequestParam String email) {

                Employee employee = employeeRepository
                                .findByEmailAndActiveTrue(email)
                                .orElse(null);

                if (employee == null) {
                        return List.of();
                }

                return attendanceRepository
                                .findByEmployeeIdOrderByAttendanceDateDescCheckInDesc(
                                                employee.getId())
                                .stream()
                                .map(attendance -> new AttendanceResponse(
                                                attendance.getId(),
                                                employee.getName(),
                                                attendance.getAttendanceDate(),
                                                attendance.getCheckIn(),
                                                attendance.getCheckOut(),
                                                attendance.getStatus()))
                                .toList();
        }

        // =====================================================
        // ADMIN - SELECTED EMPLOYEE MONTHLY ATTENDANCE
        // =====================================================

        @GetMapping("/admin/employee-month")
        public List<AttendanceResponse> getEmployeeMonthlyAttendance(
                        @RequestParam Integer employeeId,
                        @RequestParam int month,
                        @RequestParam int year) {

                if (month < 1 || month > 12 || year < 1900 || year > 9999) {
                        throw new IllegalArgumentException("Invalid month or year");
                }

                Employee employee = employeeRepository
                                .findById(employeeId)
                                .orElse(null);

                if (employee == null) {
                        return List.of();
                }

                YearMonth selectedMonth = YearMonth.of(year, month);
                LocalDate startDate = selectedMonth.atDay(1);
                LocalDate endDate = selectedMonth.atEndOfMonth();

                return attendanceRepository
                                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDescCheckInDesc(
                                                employeeId, startDate, endDate)
                                .stream()
                                .map(attendance -> new AttendanceResponse(
                                                attendance.getId(),
                                                employee.getName(),
                                                attendance.getAttendanceDate(),
                                                attendance.getCheckIn(),
                                                attendance.getCheckOut(),
                                                attendance.getStatus()))
                                .toList();
        }

        // =====================================================
        // ADMIN - ALL ATTENDANCE
        // =====================================================

        @GetMapping("/admin/all")
        public List<Attendance> getAllAttendance(
                        @RequestParam(required = false) String date) {

                if (date != null && !date.isBlank()) {

                        LocalDate attendanceDate = LocalDate.parse(date);

                        return attendanceRepository
                                        .findByAttendanceDateOrderByAttendanceDateDesc(
                                                        attendanceDate);
                }

                return attendanceRepository.findAll();
        }
}
