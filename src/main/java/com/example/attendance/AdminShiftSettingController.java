package com.example.attendance;

import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/admin/shift-settings")
@CrossOrigin
public class AdminShiftSettingController {


private final ShiftSettingRepository shiftSettingRepository;

public AdminShiftSettingController(
        ShiftSettingRepository shiftSettingRepository) {
    this.shiftSettingRepository = shiftSettingRepository;
}

// =========================
// DEFAULT SHIFT SETTINGS
// =========================

@PostConstruct
public void createDefaultShiftSettings() {

    createDefaultIfMissing(
            "GENERAL",
            LocalTime.of(9, 0),
            LocalTime.of(18, 0)
    );

    createDefaultIfMissing(
            "SHIFT_1",
            LocalTime.of(9, 0),
            LocalTime.of(18, 0)
    );

    createDefaultIfMissing(
            "SHIFT_2",
            LocalTime.of(10, 0),
            LocalTime.of(19, 0)
    );

    createDefaultIfMissing(
            "SHIFT_3",
            LocalTime.of(13, 0),
            LocalTime.of(22, 0)
    );
}

private void createDefaultIfMissing(
        String shiftType,
        LocalTime startTime,
        LocalTime endTime) {

    if (shiftSettingRepository
            .findByShiftType(shiftType)
            .isPresent()) {

        return;
    }

    ShiftSetting setting =
            new ShiftSetting();

    setting.setShiftType(shiftType);
    setting.setStartTime(startTime);
    setting.setEndTime(endTime);

    setting.setGraceMinutes(5);

    setting.setLateAfter(
            startTime.plusMinutes(5));

    setting.setSalaryDeductionAfter(
            startTime.plusMinutes(30));

    setting.setEarlyDeductionBefore(
            endTime.minusMinutes(30));

    setting.setHalfDayBoundary(
            calculateMidpoint(
                    startTime,
                    endTime));

    setting.setWorkingHours(
            calculateWorkingHours(
                    startTime,
                    endTime));

    LocalDateTime now =
            LocalDateTime.now();

    setting.setCreatedAt(now);
    setting.setUpdatedAt(now);

    shiftSettingRepository.save(setting);
}

// =========================
// GET ALL SHIFT SETTINGS
// =========================

@GetMapping
public List<ShiftSetting> getAllShiftSettings() {

    return shiftSettingRepository.findAll();
}

// =========================
// GET ONE SHIFT SETTING
// =========================

@GetMapping("/{shiftType}")
public ShiftSetting getShiftSetting(
        @PathVariable String shiftType) {

    String normalizedShiftType =
            normalizeShiftType(shiftType);

    return shiftSettingRepository
            .findByShiftType(normalizedShiftType)
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Shift setting not found"));
}

// =========================
// CREATE / UPDATE SHIFT
// =========================

@PostMapping
public ShiftSetting saveShiftSetting(
        @RequestBody ShiftSettingRequest request) {

    validateRequest(request);

    String shiftType =
            normalizeShiftType(
                    request.getShiftType());

    ShiftSetting setting =
            shiftSettingRepository
                    .findByShiftType(shiftType)
                    .orElseGet(ShiftSetting::new);

    LocalTime oldStartTime =
            setting.getStartTime();

    LocalTime oldEndTime =
            setting.getEndTime();

    Integer oldGraceMinutes =
            setting.getGraceMinutes();

    LocalTime oldLateAfter =
            setting.getLateAfter();

    LocalTime oldSalaryDeductionAfter =
            setting.getSalaryDeductionAfter();

    LocalTime oldEarlyDeductionBefore =
            setting.getEarlyDeductionBefore();

    LocalTime oldHalfDayBoundary =
            setting.getHalfDayBoundary();

    Double oldWorkingHours =
            setting.getWorkingHours();

    LocalTime newStartTime =
            request.getStartTime();

    LocalTime newEndTime =
            request.getEndTime();

    setting.setShiftType(shiftType);

    setting.setStartTime(newStartTime);
    setting.setEndTime(newEndTime);

    // =========================
    // GRACE
    // =========================

    Integer graceMinutes =
            request.getGraceMinutes();

    if (graceMinutes == null) {
        graceMinutes = 5;
    }

    setting.setGraceMinutes(graceMinutes);

    // =========================
    // LATE AFTER
    // =========================

    LocalTime lateAfter =
            request.getLateAfter();

    boolean lateWasAutomatic =
            isAutomaticLateAfter(
                    oldStartTime,
                    oldGraceMinutes,
                    oldLateAfter);

    if (lateAfter == null
            || lateWasAutomatic) {

        lateAfter =
                newStartTime.plusMinutes(
                        graceMinutes);
    }

    setting.setLateAfter(lateAfter);

    // =========================
    // SALARY / PERMISSION
    // DEDUCTION AFTER
    // =========================

    LocalTime salaryDeductionAfter =
            request.getSalaryDeductionAfter();

    boolean salaryDeductionWasAutomatic =
            isAutomaticSalaryDeduction(
                    oldStartTime,
                    oldSalaryDeductionAfter);

    if (salaryDeductionAfter == null
            || salaryDeductionWasAutomatic) {

        salaryDeductionAfter =
                newStartTime.plusMinutes(30);
    }

    setting.setSalaryDeductionAfter(
            salaryDeductionAfter);

    // =========================
    // EARLY CHECKOUT
    // =========================

    LocalTime earlyDeductionBefore =
            request.getEarlyDeductionBefore();

    boolean earlyDeductionWasAutomatic =
            isAutomaticEarlyDeduction(
                    oldEndTime,
                    oldEarlyDeductionBefore);

    if (earlyDeductionBefore == null
            || earlyDeductionWasAutomatic) {

        earlyDeductionBefore =
                newEndTime.minusMinutes(30);
    }

    setting.setEarlyDeductionBefore(
            earlyDeductionBefore);

    // =========================
    // HALF DAY
    // =========================

    LocalTime halfDayBoundary =
            request.getHalfDayBoundary();

    boolean halfDayWasAutomatic =
            isAutomaticHalfDay(
                    oldStartTime,
                    oldEndTime,
                    oldHalfDayBoundary);

    if (halfDayBoundary == null
            || halfDayWasAutomatic) {

        halfDayBoundary =
                calculateMidpoint(
                        newStartTime,
                        newEndTime);
    }

    setting.setHalfDayBoundary(
            halfDayBoundary);

    // =========================
    // WORKING HOURS
    // =========================

    Double workingHours =
            request.getWorkingHours();

    boolean workingHoursWasAutomatic =
            isAutomaticWorkingHours(
                    oldStartTime,
                    oldEndTime,
                    oldWorkingHours);

    if (workingHours == null
            || workingHoursWasAutomatic) {

        workingHours =
                calculateWorkingHours(
                        newStartTime,
                        newEndTime);
    }

    setting.setWorkingHours(
            workingHours);

    // =========================
    // TIMESTAMPS
    // =========================

    if (setting.getCreatedAt() == null) {

        setting.setCreatedAt(
                LocalDateTime.now());
    }

    setting.setUpdatedAt(
            LocalDateTime.now());

    return shiftSettingRepository.save(
            setting);
}

// =========================
// DELETE SHIFT SETTING
// =========================

@DeleteMapping("/{shiftType}")
public void deleteShiftSetting(
        @PathVariable String shiftType) {

    String normalizedShiftType =
            normalizeShiftType(shiftType);

    ShiftSetting setting =
            shiftSettingRepository
                    .findByShiftType(
                            normalizedShiftType)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Shift setting not found"));

    shiftSettingRepository.delete(setting);
}

// =========================
// VALIDATION
// =========================

private void validateRequest(
        ShiftSettingRequest request) {

    if (request == null) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Request is required");
    }

    String shiftType =
            normalizeShiftType(
                    request.getShiftType());

    if (!shiftType.equals("GENERAL")
            && !shiftType.equals("SHIFT_1")
            && !shiftType.equals("SHIFT_2")
            && !shiftType.equals("SHIFT_3")) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Shift type must be GENERAL, SHIFT_1, SHIFT_2 or SHIFT_3");
    }

    // =========================
    // START TIME
    // =========================

    if (request.getStartTime() == null) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Start time is required");
    }

    // =========================
    // END TIME
    // =========================

    if (request.getEndTime() == null) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "End time is required");
    }

    // =========================
    // END AFTER START
    // =========================

    if (!request.getEndTime()
            .isAfter(
                    request.getStartTime())) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "End time must be after start time");
    }

    // =========================
    // GRACE
    // =========================

    if (request.getGraceMinutes() != null
            && request.getGraceMinutes() < 0) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Grace minutes cannot be negative");
    }

    // =========================
    // WORKING HOURS
    // =========================

    if (request.getWorkingHours() != null
            && request.getWorkingHours() <= 0) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Working hours must be greater than zero");
    }

    // =========================
    // LATE TIME
    // =========================

    if (request.getLateAfter() != null
            && request.getLateAfter()
            .isBefore(
                    request.getStartTime())) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Late time cannot be before start time");
    }

    // =========================
    // SALARY DEDUCTION TIME
    // =========================

    if (request.getSalaryDeductionAfter() != null
            && request.getSalaryDeductionAfter()
            .isBefore(
                    request.getStartTime())) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Salary deduction time cannot be before start time");
    }

    // =========================
    // EARLY DEDUCTION
    // =========================

    if (request.getEarlyDeductionBefore() != null
            && request.getEarlyDeductionBefore()
            .isAfter(
                    request.getEndTime())) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Early deduction time cannot be after end time");
    }

    // =========================
    // HALF DAY
    // =========================

    if (request.getHalfDayBoundary() != null) {

        LocalTime boundary =
                request.getHalfDayBoundary();

        if (!boundary.isAfter(
                request.getStartTime())
                || !boundary.isBefore(
                request.getEndTime())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Half-day boundary must be between start and end time");
        }
    }
}

// =========================
// AUTOMATIC LATE CHECK
// =========================

private boolean isAutomaticLateAfter(
        LocalTime oldStart,
        Integer oldGrace,
        LocalTime oldLateAfter) {

    if (oldStart == null
            || oldLateAfter == null) {

        return true;
    }

    int grace =
            oldGrace == null
                    ? 5
                    : oldGrace;

    LocalTime expected =
            oldStart.plusMinutes(grace);

    return oldLateAfter.equals(expected);
}

// =========================
// AUTOMATIC SALARY CHECK
// =========================

private boolean isAutomaticSalaryDeduction(
        LocalTime oldStart,
        LocalTime oldSalaryDeduction) {

    if (oldStart == null
            || oldSalaryDeduction == null) {

        return true;
    }

    LocalTime expected =
            oldStart.plusMinutes(30);

    return oldSalaryDeduction.equals(expected);
}

// =========================
// AUTOMATIC EARLY CHECK
// =========================

private boolean isAutomaticEarlyDeduction(
        LocalTime oldEnd,
        LocalTime oldEarlyDeduction) {

    if (oldEnd == null
            || oldEarlyDeduction == null) {

        return true;
    }

    LocalTime expected =
            oldEnd.minusMinutes(30);

    return oldEarlyDeduction.equals(expected);
}

// =========================
// AUTOMATIC HALF DAY CHECK
// =========================

private boolean isAutomaticHalfDay(
        LocalTime oldStart,
        LocalTime oldEnd,
        LocalTime oldHalfDay) {

    if (oldStart == null
            || oldEnd == null
            || oldHalfDay == null) {

        return true;
    }

    LocalTime expected =
            calculateMidpoint(
                    oldStart,
                    oldEnd);

    return oldHalfDay.equals(expected);
}

// =========================
// AUTOMATIC WORKING HOURS CHECK
// =========================

private boolean isAutomaticWorkingHours(
        LocalTime oldStart,
        LocalTime oldEnd,
        Double oldWorkingHours) {

    if (oldStart == null
            || oldEnd == null
            || oldWorkingHours == null) {

        return true;
    }

    double expected =
            calculateWorkingHours(
                    oldStart,
                    oldEnd);

    return Math.abs(
            oldWorkingHours - expected) < 0.0001;
}

// =========================
// CALCULATE MIDPOINT
// =========================

private LocalTime calculateMidpoint(
        LocalTime start,
        LocalTime end) {

    long seconds =
            Duration.between(
                    start,
                    end)
                    .getSeconds();

    return start.plusSeconds(
            seconds / 2);
}

// =========================
// CALCULATE WORKING HOURS
// =========================

private double calculateWorkingHours(
        LocalTime start,
        LocalTime end) {

    long seconds =
            Duration.between(
                    start,
                    end)
                    .getSeconds();

    return seconds / 3600.0;
}

// =========================
// NORMALIZE SHIFT TYPE
// =========================

private String normalizeShiftType(
        String shiftType) {

    if (shiftType == null) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Shift type is required");
    }

    return shiftType
            .trim()
            .toUpperCase()
            .replace("-", "_")
            .replace(" ", "_");
}


}
