package com.example.attendance;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "https://attendance-management-3d-webinar.vercel.app",
        "https://attendance-management-git-main-3d-webinar.vercel.app",
        "https://attendance-management-lhsosyu8t-3d-webinar.vercel.app",
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

    // India Time Zone
    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    // Default fallback timing
    private static final LocalTime DEFAULT_OFFICE_START_TIME =
            LocalTime.of(9, 0);

    private static final LocalTime DEFAULT_OFFICE_END_TIME =
            LocalTime.of(18, 0);

    // Existing 5-minute grace rule
    private static final int GRACE_MINUTES = 5;

    // GPS distance calculation
    private static final double EARTH_RADIUS_METERS =
            6_371_000.0;

    // Existing face similarity threshold
    private static final double FACE_SIMILARITY_THRESHOLD =
            0.60;


    public AttendanceController(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            LeaveRequestRepository leaveRequestRepository,
            EmployeeLeaveBalanceRepository balanceRepository,
            LeavePermissionSettingRepository settingRepository,
            ShiftSettingRepository shiftSettingRepository,
            EmployeeFaceRepository employeeFaceRepository) {

        this.attendanceRepository =
                attendanceRepository;

        this.employeeRepository =
                employeeRepository;

        this.leaveRequestRepository =
                leaveRequestRepository;

        this.balanceRepository =
                balanceRepository;

        this.settingRepository =
                settingRepository;

        this.shiftSettingRepository =
                shiftSettingRepository;

        this.employeeFaceRepository =
                employeeFaceRepository;
    }


    // =====================================================
    // TEST API
    // =====================================================

    @GetMapping("/")
    public String home() {

        return "Attendance API Running Successfully!";
    }


    // =====================================================
    // GET BRANCH VERIFICATION SETTINGS
    // =====================================================

    @GetMapping("/verification-settings")
    public Map<String, Object> getVerificationSettings(
            @RequestParam String email) {

        Map<String, Object> response =
                new LinkedHashMap<>();


        Employee employee =
                employeeRepository
                        .findByEmailAndActiveTrue(
                                email)
                        .orElse(null);


        if (employee == null) {

            response.put(
                    "success",
                    false);

            response.put(
                    "message",
                    "Employee Not Found or Employee is Inactive");

            response.put(
                    "locationRequired",
                    false);

            response.put(
                    "faceRequired",
                    false);

            response.put(
                    "branchActive",
                    false);

            return response;
        }


        Branch branch =
                getEmployeeBranch(employee);


        // =================================================
        // NO ASSIGNED BRANCH
        // =================================================

        if (branch == null) {

            response.put(
                    "success",
                    true);

            response.put(
                    "branchActive",
                    false);

            response.put(
                    "locationRequired",
                    false);

            response.put(
                    "faceRequired",
                    false);

            response.put(
                    "branchName",
                    "No Branch");

            return response;
        }


        boolean branchActive =
                Boolean.TRUE.equals(
                        branch.getActive());


        // =================================================
        // INACTIVE BRANCH
        //
        // No GPS
        // No Face
        // Direct Attendance
        // =================================================

        if (!branchActive) {

            response.put(
                    "success",
                    true);

            response.put(
                    "branchActive",
                    false);

            response.put(
                    "locationRequired",
                    false);

            response.put(
                    "faceRequired",
                    false);

            response.put(
                    "branchName",
                    branch.getBranchName());

            return response;
        }


        // =================================================
        // ACTIVE BRANCH
        // READ EACH SETTING INDEPENDENTLY
        // =================================================

        boolean locationRequired =
                Boolean.TRUE.equals(
                        branch
                                .getLocationVerificationEnabled());


        boolean faceRequired =
                Boolean.TRUE.equals(
                        branch
                                .getFaceVerificationEnabled());


        response.put(
                "success",
                true);

        response.put(
                "branchActive",
                true);

        response.put(
                "locationRequired",
                locationRequired);

        response.put(
                "faceRequired",
                faceRequired);

        response.put(
                "branchName",
                branch.getBranchName());


        return response;
    }


    // =====================================================
    // GET EFFECTIVE SHIFT
    //
    // SHIFT PRIORITY:
    // EMPLOYEE > ROLE > DEFAULT
    //
    // Shift type comes from LeavePermissionSetting.
    // Actual timing comes from ShiftSetting.
    // =====================================================

    private ShiftSetting getEffectiveShiftSetting(
            Employee employee) {

        String shiftType = "GENERAL";


        // =================================================
        // 1. EMPLOYEE SETTING
        // =================================================

        if (employee.getId() != null) {

            var employeeSetting =
                    settingRepository
                            .findBySettingTypeAndEmployeeId(
                                    "EMPLOYEE",
                                    employee.getId());

            if (employeeSetting.isPresent()) {

                String configuredShift =
                        employeeSetting.get()
                                .getShiftType();

                if (configuredShift != null
                        && !configuredShift.isBlank()) {

                    shiftType =
                            normalizeShiftType(
                                    configuredShift);
                }
            }
        }


        // =================================================
        // 2. ROLE SETTING
        // =================================================

        if ("GENERAL".equals(shiftType)
                && employee.getRole() != null
                && !employee.getRole().isBlank()) {

            var roleSetting =
                    settingRepository
                            .findBySettingTypeAndRole(
                                    "ROLE",
                                    employee.getRole()
                                            .trim()
                                            .toUpperCase());

            if (roleSetting.isPresent()) {

                String configuredShift =
                        roleSetting.get()
                                .getShiftType();

                if (configuredShift != null
                        && !configuredShift.isBlank()) {

                    shiftType =
                            normalizeShiftType(
                                    configuredShift);
                }
            }
        }


        // =================================================
        // 3. DEFAULT SETTING
        // =================================================

        if ("GENERAL".equals(shiftType)) {

            var defaultSetting =
                    settingRepository
                            .findBySettingType(
                                    "DEFAULT");

            if (defaultSetting.isPresent()) {

                String configuredShift =
                        defaultSetting.get()
                                .getShiftType();

                if (configuredShift != null
                        && !configuredShift.isBlank()) {

                    shiftType =
                            normalizeShiftType(
                                    configuredShift);
                }
            }
        }


        // =================================================
        // GET ACTUAL SHIFT TIMING
        // =================================================

        return shiftSettingRepository
                .findByShiftType(shiftType)
                .orElseGet(() ->
                        shiftSettingRepository
                                .findByShiftType("GENERAL")
                                .orElse(null));
    }


    // =====================================================
    // NORMALIZE SHIFT TYPE
    // =====================================================

    private String normalizeShiftType(
            String shiftType) {

        if (shiftType == null
                || shiftType.isBlank()) {

            return "GENERAL";
        }

        return shiftType
                .trim()
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");
    }


    // =====================================================
    // GET EFFECTIVE START TIME
    //
    // IMPORTANT:
    // Timing comes from ShiftSetting.
    // =====================================================

    private LocalTime getOfficeStartTime(
            Employee employee) {

        ShiftSetting shift =
                getEffectiveShiftSetting(
                        employee);

        if (shift != null
                && shift.getStartTime() != null) {

            return shift.getStartTime();
        }

        return DEFAULT_OFFICE_START_TIME;
    }


    // =====================================================
    // GET EFFECTIVE END TIME
    //
    // IMPORTANT:
    // Timing comes from ShiftSetting.
    // =====================================================

    private LocalTime getOfficeEndTime(
            Employee employee) {

        ShiftSetting shift =
                getEffectiveShiftSetting(
                        employee);

        if (shift != null
                && shift.getEndTime() != null) {

            return shift.getEndTime();
        }

        return DEFAULT_OFFICE_END_TIME;
    }


    // =====================================================
    // GET EMPLOYEE ASSIGNED BRANCH
    // =====================================================

    private Branch getEmployeeBranch(
            Employee employee) {

        return employee.getBranch();
    }


    // =====================================================
    // CHECK LOCATION AGAINST
    // EMPLOYEE ASSIGNED BRANCH
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


        if (employeeLatitude < -90
                || employeeLatitude > 90) {

            return false;
        }


        if (employeeLongitude < -180
                || employeeLongitude > 180) {

            return false;
        }


        if (branch.getLatitude() == null
                || branch.getLongitude() == null
                || branch.getAllowedRadiusMeters() == null) {

            return false;
        }


        double distance =
                calculateDistanceInMeters(
                        employeeLatitude,
                        employeeLongitude,
                        branch.getLatitude(),
                        branch.getLongitude());


        return distance <=
                branch.getAllowedRadiusMeters();
    }


    // =====================================================
    // HAVERSINE DISTANCE
    // =====================================================

    private double calculateDistanceInMeters(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2) {

        double lat1Radians =
                Math.toRadians(latitude1);

        double lat2Radians =
                Math.toRadians(latitude2);

        double latitudeDifference =
                Math.toRadians(
                        latitude2 - latitude1);

        double longitudeDifference =
                Math.toRadians(
                        longitude2 - longitude1);


        double a =
                Math.sin(
                        latitudeDifference / 2)
                        * Math.sin(
                        latitudeDifference / 2)
                        +
                        Math.cos(lat1Radians)
                                *
                                Math.cos(lat2Radians)
                                *
                                Math.sin(
                                        longitudeDifference / 2)
                                *
                                Math.sin(
                                        longitudeDifference / 2);


        double c =
                2 *
                        Math.atan2(
                                Math.sqrt(a),
                                Math.sqrt(1 - a));


        return EARTH_RADIUS_METERS * c;
    }


    // =====================================================
    // FACE VERIFICATION
    // =====================================================

    private String verifyEmployeeFace(
            Employee employee,
            String faceData) {

        if (faceData == null
                || faceData.trim().isEmpty()) {

            return "FACE_DATA_REQUIRED";
        }


        EmployeeFace registeredFace =
                employeeFaceRepository
                        .findByEmployeeIdAndActiveTrue(
                                employee.getId()
                                        .longValue())
                        .orElse(null);


        if (registeredFace == null) {

            return "FACE_NOT_REGISTERED";
        }


        try {

            List<Double> registeredEmbedding =
                    parseFaceEmbedding(
                            registeredFace.getFaceData());

            List<Double> currentEmbedding =
                    parseFaceEmbedding(
                            faceData);


            if (registeredEmbedding.isEmpty()
                    || currentEmbedding.isEmpty()) {

                return "INVALID_FACE_DATA";
            }


            if (registeredEmbedding.size()
                    != currentEmbedding.size()) {

                return "FACE_DATA_FORMAT_MISMATCH";
            }


            double similarity =
                    calculateCosineSimilarity(
                            registeredEmbedding,
                            currentEmbedding);


            if (similarity >=
                    FACE_SIMILARITY_THRESHOLD) {

                return "FACE_MATCH";
            }


            return "FACE_MISMATCH";


        } catch (Exception ex) {

            return "INVALID_FACE_DATA";
        }
    }


    // =====================================================
    // PARSE FACE EMBEDDING
    // =====================================================

    private List<Double> parseFaceEmbedding(
            String faceData) {

        List<Double> values =
                new ArrayList<>();


        if (faceData == null
                || faceData.trim().isEmpty()) {

            return values;
        }


        String data =
                faceData.trim();


        if (!data.startsWith("[")
                || !data.endsWith("]")) {

            return values;
        }


        data =
                data.substring(
                                1,
                                data.length() - 1)
                        .trim();


        if (data.isEmpty()) {

            return values;
        }


        String[] numbers =
                data.split(",");


        for (String number : numbers) {

            String value =
                    number.trim();


            if (value.isEmpty()) {

                return List.of();
            }


            values.add(
                    Double.parseDouble(
                            value));
        }


        return values;
    }


    // =====================================================
    // COSINE SIMILARITY
    // =====================================================

    private double calculateCosineSimilarity(
            List<Double> first,
            List<Double> second) {

        double dotProduct = 0.0;

        double firstMagnitude = 0.0;

        double secondMagnitude = 0.0;


        for (int i = 0;
             i < first.size();
             i++) {

            double firstValue =
                    first.get(i);

            double secondValue =
                    second.get(i);


            dotProduct +=
                    firstValue *
                            secondValue;


            firstMagnitude +=
                    firstValue *
                            firstValue;


            secondMagnitude +=
                    secondValue *
                            secondValue;
        }


        if (firstMagnitude == 0.0
                || secondMagnitude == 0.0) {

            return 0.0;
        }


        return dotProduct /
                (Math.sqrt(firstMagnitude)
                        *
                        Math.sqrt(secondMagnitude));
    }


    // =====================================================
    // FORMAT DURATION
    // =====================================================

    private String formatDuration(
            long totalMinutes) {

        if (totalMinutes <= 0) {

            return "0 mins";
        }


        long hours =
                totalMinutes / 60;

        long minutes =
                totalMinutes % 60;


        if (hours > 0 && minutes > 0) {

            return hours
                    + (hours == 1
                    ? " hr "
                    : " hrs ")
                    + minutes
                    + " mins";
        }


        if (hours > 0) {

            return hours
                    + (hours == 1
                    ? " hr"
                    : " hrs");
        }


        return minutes + " mins";
    }


    // =====================================================
    // CALCULATE LATE MINUTES
    // =====================================================

    private long calculateLateMinutes(
            LocalTime officeStartTime,
            LocalTime checkInTime) {

        if (checkInTime == null
                || officeStartTime == null) {

            return 0;
        }


        if (!checkInTime.isAfter(
                officeStartTime)) {

            return 0;
        }


        return Duration.between(
                        officeStartTime,
                        checkInTime)
                .toMinutes();
    }


    // =====================================================
    // CALCULATE EARLY CHECK-OUT MINUTES
    // =====================================================

    private long calculateEarlyCheckoutMinutes(
            LocalTime checkOutTime,
            LocalTime officeEndTime) {

        if (checkOutTime == null
                || officeEndTime == null) {

            return 0;
        }


        if (!checkOutTime.isBefore(
                officeEndTime)) {

            return 0;
        }


        return Duration.between(
                        checkOutTime,
                        officeEndTime)
                .toMinutes();
    }


    // =====================================================
    // CHECK-IN
    //
    // MULTIPLE CHECK-IN / CHECK-OUT SUPPORT
    // =====================================================

    @PostMapping("/check-in")
    @Transactional
    public String checkIn(
            @RequestParam String email,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String faceData) {

        Employee employee =
                employeeRepository
                        .findByEmailAndActiveTrue(
                                email)
                        .orElse(null);


        if (employee == null) {

            return "Employee Not Found or Employee is Inactive";
        }


        Branch assignedBranch =
                getEmployeeBranch(employee);


        // =================================================
        // BRANCH VERIFICATION
        // =================================================

        if (assignedBranch != null
                && Boolean.TRUE.equals(
                assignedBranch.getActive())) {

            // Location ON -> GPS verification
            if (Boolean.TRUE.equals(
                    assignedBranch
                            .getLocationVerificationEnabled())) {

                if (!isInsideAssignedBranch(
                        assignedBranch,
                        latitude,
                        longitude)) {

                    return "Attendance Not Allowed - "
                            + "You are outside your assigned branch location";
                }
            }


            // Face ON -> Face verification
            if (Boolean.TRUE.equals(
                    assignedBranch
                            .getFaceVerificationEnabled())) {

                String faceResult =
                        verifyEmployeeFace(
                                employee,
                                faceData);


                if ("FACE_DATA_REQUIRED"
                        .equals(faceResult)) {

                    return "Attendance Not Allowed - "
                            + "Face verification is required";
                }


                if ("FACE_NOT_REGISTERED"
                        .equals(faceResult)) {

                    return "Attendance Not Allowed - "
                            + "Face is not registered. "
                            + "Please complete face registration";
                }


                if ("FACE_MISMATCH"
                        .equals(faceResult)) {

                    return "Attendance Not Allowed - "
                            + "Face verification failed";
                }


                if (!"FACE_MATCH"
                        .equals(faceResult)) {

                    return "Attendance Not Allowed - "
                            + "Invalid face verification data";
                }
            }
        }


        LocalDate today =
                LocalDate.now(
                        INDIA_ZONE);

        LocalTime currentTime =
                LocalTime.now(
                        INDIA_ZONE);


        // =================================================
        // CHECK FOR OPEN SESSION
        // =================================================

        var openAttendance =
                attendanceRepository
                        .findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(
                                employee.getId(),
                                today);


        if (openAttendance.isPresent()) {

            return "Already Checked In - "
                    + "Please Check Out First";
        }


        // =================================================
        // CHECK FIRST SESSION OF DAY
        // =================================================

        boolean firstSessionOfDay = true;


        List<Attendance> employeeAttendance =
                attendanceRepository
                        .findByEmployeeIdOrderByAttendanceDateDescCheckInDesc(
                                employee.getId());


        for (Attendance existingAttendance :
                employeeAttendance) {

            if (today.equals(
                    existingAttendance
                            .getAttendanceDate())) {

                firstSessionOfDay = false;

                break;
            }
        }


        // =================================================
        // CANCEL APPROVED LEAVE
        // =================================================

        if (firstSessionOfDay) {

            cancelApprovedLeaveForCheckIn(
                    employee.getId(),
                    today);
        }


        // =================================================
        // CREATE NEW ATTENDANCE SESSION
        // =================================================

        Attendance attendance =
                new Attendance();


        attendance.setEmployeeId(
                employee.getId());


        attendance.setAttendanceDate(
                today);


        attendance.setCheckIn(
                currentTime);


        // =================================================
        // FIRST SESSION STATUS
        // =================================================

        if (firstSessionOfDay) {

            LocalTime officeStartTime =
                    getOfficeStartTime(
                            employee);


            LocalTime graceEndTime =
                    officeStartTime
                            .plusMinutes(
                                    GRACE_MINUTES);


            if (!currentTime.isAfter(
                    graceEndTime)) {

                attendance.setStatus(
                        "PRESENT");

            } else {

                long lateMinutes =
                        calculateLateMinutes(
                                officeStartTime,
                                currentTime);


                attendance.setStatus(
                        "LATE - "
                                + formatDuration(
                                lateMinutes));
            }

        } else {

            attendance.setStatus(
                    "RE-ENTRY");
        }


        attendanceRepository.save(
                attendance);


        String branchName =
                assignedBranch != null
                        ? assignedBranch.getBranchName()
                        : "No Branch";


        if (attendance.getStatus() != null
                && attendance.getStatus()
                .startsWith("LATE")) {

            return "Check In Successful - "
                    + employee.getName()
                    + " ("
                    + attendance.getStatus()
                    + ") - "
                    + branchName;
        }


        if (!firstSessionOfDay) {

            return "Check In Successful - "
                    + employee.getName()
                    + " (RE-ENTRY) - "
                    + branchName;
        }


        return "Check In Successful - "
                + employee.getName()
                + " - "
                + branchName;
    }


    // =====================================================
    // CANCEL APPROVED LEAVE
    // =====================================================

    private void cancelApprovedLeaveForCheckIn(
            Integer employeeId,
            LocalDate today) {

        List<LeaveRequest> leaveRequests =
                leaveRequestRepository
                        .findByEmployeeIdAndLeaveDate(
                                employeeId,
                                today);


        if (leaveRequests == null
                || leaveRequests.isEmpty()) {

            return;
        }


        for (LeaveRequest leave :
                leaveRequests) {

            if (!"APPROVED".equalsIgnoreCase(
                    leave.getStatus())) {

                continue;
            }


            String leaveType =
                    leave.getLeaveType();


            if (leaveType == null
                    || leaveType.equalsIgnoreCase(
                    "PERMISSION")) {

                continue;
            }


            if (leaveType.equalsIgnoreCase("SICK")
                    || leaveType.equalsIgnoreCase("CASUAL")) {

                restorePaidLeaveBalance(
                        leave);
            }


            leaveRequestRepository.delete(
                    leave);
        }
    }


    // =====================================================
    // RESTORE SICK / CASUAL BALANCE
    // =====================================================

    private void restorePaidLeaveBalance(
            LeaveRequest leave) {

        if (leave.getEmployeeId() == null
                || leave.getLeaveDate() == null) {

            return;
        }


        LocalDate month =
                leave.getLeaveDate()
                        .withDayOfMonth(1);


        var balanceOptional =
                balanceRepository
                        .findByEmployeeIdAndBalanceMonth(
                                leave.getEmployeeId(),
                                month);


        if (balanceOptional.isEmpty()) {

            return;
        }


        EmployeeLeaveBalance balance =
                balanceOptional.get();


        double duration =
                leave.getLeaveDuration() != null
                        ? leave.getLeaveDuration()
                        : 1.0;


        String leaveType =
                leave.getLeaveType();


        if (leaveType.equalsIgnoreCase(
                "SICK")) {

            double currentBalance =
                    balance.getSickBalance() != null
                            ? balance.getSickBalance()
                            : 0.0;


            balance.setSickBalance(
                    currentBalance +
                            duration);
        }


        if (leaveType.equalsIgnoreCase(
                "CASUAL")) {

            double currentBalance =
                    balance.getCasualBalance() != null
                            ? balance.getCasualBalance()
                            : 0.0;


            balance.setCasualBalance(
                    currentBalance +
                            duration);
        }


        balance.setUpdatedAt(
                LocalDateTime.now(
                        INDIA_ZONE));


        balanceRepository.save(
                balance);
    }


    // =====================================================
    // CHECK OUT
    // =====================================================

    @PostMapping("/check-out")
    @Transactional
    public String checkOut(
            @RequestParam String email,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String faceData) {

        Employee employee =
                employeeRepository
                        .findByEmailAndActiveTrue(
                                email)
                        .orElse(null);


        if (employee == null) {

            return "Employee Not Found or Employee is Inactive";
        }


        Branch assignedBranch =
                getEmployeeBranch(employee);


        // =================================================
        // BRANCH VERIFICATION
        // =================================================

        if (assignedBranch != null
                && Boolean.TRUE.equals(
                assignedBranch.getActive())) {

            // Location ON -> GPS verification
            if (Boolean.TRUE.equals(
                    assignedBranch.getLocationVerificationEnabled())) {

                if (!isInsideAssignedBranch(
                        assignedBranch,
                        latitude,
                        longitude)) {

                    return "Check-out Not Allowed - "
                            + "You are outside your assigned branch location";
                }
            }


            // Face ON -> Face verification
            if (Boolean.TRUE.equals(
                    assignedBranch.getFaceVerificationEnabled())) {

                String faceResult =
                        verifyEmployeeFace(
                                employee,
                                faceData);


                if ("FACE_DATA_REQUIRED"
                        .equals(faceResult)) {

                    return "Check-out Not Allowed - "
                            + "Face verification is required";
                }


                if ("FACE_NOT_REGISTERED"
                        .equals(faceResult)) {

                    return "Check-out Not Allowed - "
                            + "Face is not registered. "
                            + "Please complete face registration";
                }


                if ("FACE_MISMATCH"
                        .equals(faceResult)) {

                    return "Check-out Not Allowed - "
                            + "Face verification failed";
                }


                if (!"FACE_MATCH"
                        .equals(faceResult)) {

                    return "Check-out Not Allowed - "
                            + "Invalid face verification data";
                }
            }
        }


        LocalDate today =
                LocalDate.now(
                        INDIA_ZONE);

        LocalTime currentTime =
                LocalTime.now(
                        INDIA_ZONE);


        // =================================================
        // FIND CURRENT OPEN SESSION
        // =================================================

        Attendance attendance =
                attendanceRepository
                        .findFirstByEmployeeIdAndAttendanceDateAndCheckOutIsNullOrderByCheckInDesc(
                                employee.getId(),
                                today)
                        .orElse(null);


        if (attendance == null) {

            return "Please Check In First";
        }


        attendance.setCheckOut(
                currentTime);


        // =================================================
        // DYNAMIC OFFICE END TIME
        // =================================================

        LocalTime officeEndTime =
                getOfficeEndTime(
                        employee);


        // =================================================
        // EARLY CHECK-OUT
        // =================================================

        if (currentTime.isBefore(
                officeEndTime)) {

            long earlyMinutes =
                    calculateEarlyCheckoutMinutes(
                            currentTime,
                            officeEndTime);


            String earlyDuration =
                    formatDuration(
                            earlyMinutes);


            String currentStatus =
                    attendance.getStatus();


            if (currentStatus != null
                    && currentStatus.startsWith(
                    "LATE - ")) {

                attendance.setStatus(
                        currentStatus
                                + " / EARLY CHECK-OUT - "
                                + earlyDuration);

            } else if ("RE-ENTRY".equalsIgnoreCase(
                    currentStatus)) {

                attendance.setStatus(
                        "RE-ENTRY / EARLY CHECK-OUT - "
                                + earlyDuration);

            } else {

                attendance.setStatus(
                        "EARLY CHECK-OUT - "
                                + earlyDuration);
            }

        } else {

            // =========================
            // OFFICE END TIME REACHED
            // =========================

            if (attendance.getStatus() != null
                    && attendance.getStatus()
                    .startsWith("LATE - ")) {

                attendance.setStatus(
                        attendance.getStatus());

            } else if ("RE-ENTRY".equalsIgnoreCase(
                    attendance.getStatus())) {

                attendance.setStatus(
                        "RE-ENTRY");

            } else {

                attendance.setStatus(
                        "PRESENT");
            }
        }


        attendanceRepository.save(
                attendance);


        String branchName =
                assignedBranch != null
                        ? assignedBranch.getBranchName()
                        : "No Branch";


        if (currentTime.isBefore(
                officeEndTime)) {

            long earlyMinutes =
                    calculateEarlyCheckoutMinutes(
                            currentTime,
                            officeEndTime);


            return "Check Out Successful - "
                    + employee.getName()
                    + " (EARLY CHECK-OUT - "
                    + formatDuration(
                    earlyMinutes)
                    + ") - "
                    + branchName;
        }


        if (attendance.getStatus() != null
                && attendance.getStatus()
                .startsWith("LATE - ")) {

            return "Check Out Successful - "
                    + employee.getName()
                    + " ("
                    + attendance.getStatus()
                    + ") - "
                    + branchName;
        }


        return "Check Out Successful - "
                + employee.getName()
                + " - "
                + branchName;
    }


    // =====================================================
    // ATTENDANCE HISTORY
    // =====================================================

    @GetMapping("/history")
    public List<AttendanceResponse> getHistory(
            @RequestParam String email) {

        Employee employee =
                employeeRepository
                        .findByEmailAndActiveTrue(
                                email)
                        .orElse(null);


        if (employee == null) {

            return List.of();
        }


        return attendanceRepository
                .findByEmployeeIdOrderByAttendanceDateDescCheckInDesc(
                        employee.getId())
                .stream()
                .map(attendance ->
                        new AttendanceResponse(
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

        if (date != null
                && !date.isBlank()) {

            LocalDate attendanceDate =
                    LocalDate.parse(date);


            return attendanceRepository
                    .findByAttendanceDateOrderByAttendanceDateDesc(
                            attendanceDate);
        }


        return attendanceRepository.findAll();
    }
}