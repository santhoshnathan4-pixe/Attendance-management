
package com.example.attendance;

import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/admin/shift-settings")
@CrossOrigin
public class AdminShiftSettingController {

        private final ShiftSettingRepository shiftSettingRepository;

        public AdminShiftSettingController(
                        ShiftSettingRepository shiftSettingRepository) {
                this.shiftSettingRepository = shiftSettingRepository;
        }

        // Create only the default General Shift if it does not exist.
        // Existing shift settings are never overwritten during startup.
        @PostConstruct
        public void createDefaultShiftSettings() {
                createDefaultIfMissing(
                                "GENERAL",
                                LocalTime.of(9, 0),
                                LocalTime.of(18, 0));
        }

        private void createDefaultIfMissing(
                        String shiftType,
                        LocalTime startTime,
                        LocalTime endTime) {

                if (shiftSettingRepository.findByShiftType(shiftType).isPresent()) {
                        return;
                }

                ShiftSetting setting = new ShiftSetting();
                setting.setShiftType(shiftType);
                setting.setStartTime(startTime);
                setting.setEndTime(endTime);
                setting.setGraceMinutes(5);
                setting.setLateAfter(startTime.plusMinutes(5));
                setting.setSalaryDeductionAfter(startTime.plusMinutes(30));
                setting.setEarlyDeductionBefore(endTime.minusMinutes(30));
                setting.setHalfDayBoundary(calculateMidpoint(startTime, endTime));
                setting.setWorkingHours(calculateWorkingHours(startTime, endTime));

                LocalDateTime now = LocalDateTime.now();
                setting.setCreatedAt(now);
                setting.setUpdatedAt(now);

                shiftSettingRepository.save(setting);
        }

        @GetMapping
        public List<ShiftSetting> getAllShiftSettings() {
                return shiftSettingRepository.findAll();
        }

        @GetMapping("/{shiftType}")
        public ShiftSetting getShiftSetting(@PathVariable String shiftType) {
                String normalized = normalizeShiftType(shiftType);

                return shiftSettingRepository.findByShiftType(normalized)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Shift setting not found"));
        }

        @PostMapping
        public ShiftSetting saveShiftSetting(
                        @RequestBody ShiftSettingRequest request) {

                validateRequest(request);

                String shiftType = normalizeShiftType(request.getShiftType());

                ShiftSetting setting = shiftSettingRepository
                                .findByShiftType(shiftType)
                                .orElseGet(ShiftSetting::new);

                LocalTime oldStart = setting.getStartTime();
                LocalTime oldEnd = setting.getEndTime();
                Integer oldGrace = setting.getGraceMinutes();
                LocalTime oldLateAfter = setting.getLateAfter();
                LocalTime oldSalaryDeduction = setting.getSalaryDeductionAfter();
                LocalTime oldEarlyDeduction = setting.getEarlyDeductionBefore();
                LocalTime oldHalfDay = setting.getHalfDayBoundary();
                Double oldWorkingHours = setting.getWorkingHours();

                LocalTime start = request.getStartTime();
                LocalTime end = request.getEndTime();

                setting.setShiftType(shiftType);
                setting.setStartTime(start);
                setting.setEndTime(end);

                int grace = request.getGraceMinutes() == null
                                ? 5
                                : request.getGraceMinutes();

                setting.setGraceMinutes(grace);

                LocalTime lateAfter = request.getLateAfter();
                if (lateAfter == null
                                || isAutomaticLateAfter(oldStart, oldGrace, oldLateAfter)) {
                        lateAfter = start.plusMinutes(grace);
                }
                setting.setLateAfter(lateAfter);

                LocalTime salaryDeduction = request.getSalaryDeductionAfter();
                if (salaryDeduction == null
                                || isAutomaticSalaryDeduction(oldStart, oldSalaryDeduction)) {
                        salaryDeduction = start.plusMinutes(30);
                }
                setting.setSalaryDeductionAfter(salaryDeduction);

                LocalTime earlyDeduction = request.getEarlyDeductionBefore();
                if (earlyDeduction == null
                                || isAutomaticEarlyDeduction(oldEnd, oldEarlyDeduction)) {
                        earlyDeduction = end.minusMinutes(30);
                }
                setting.setEarlyDeductionBefore(earlyDeduction);

                LocalTime halfDay = request.getHalfDayBoundary();
                if (halfDay == null
                                || isAutomaticHalfDay(oldStart, oldEnd, oldHalfDay)) {
                        halfDay = calculateMidpoint(start, end);
                }
                setting.setHalfDayBoundary(halfDay);

                Double workingHours = request.getWorkingHours();
                if (workingHours == null
                                || isAutomaticWorkingHours(oldStart, oldEnd, oldWorkingHours)) {
                        workingHours = calculateWorkingHours(start, end);
                }
                setting.setWorkingHours(workingHours);

                LocalDateTime now = LocalDateTime.now();
                if (setting.getCreatedAt() == null) {
                        setting.setCreatedAt(now);
                }
                setting.setUpdatedAt(now);

                return shiftSettingRepository.save(setting);
        }

        @DeleteMapping("/{shiftType}")
        public void deleteShiftSetting(@PathVariable String shiftType) {
                String normalized = normalizeShiftType(shiftType);

                if ("GENERAL".equals(normalized)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "General Shift cannot be deleted");
                }

                ShiftSetting setting = shiftSettingRepository
                                .findByShiftType(normalized)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Shift setting not found"));

                shiftSettingRepository.delete(setting);
        }

        private void validateRequest(ShiftSettingRequest request) {
                if (request == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Request is required");
                }

                String shiftType = normalizeShiftType(request.getShiftType());

                if (shiftType.length() > 100) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Shift name must not exceed 100 characters");
                }

                if (request.getStartTime() == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Start time is required");
                }

                if (request.getEndTime() == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "End time is required");
                }

                if (!request.getEndTime().isAfter(request.getStartTime())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "End time must be after start time");
                }

                if (request.getGraceMinutes() != null
                                && request.getGraceMinutes() < 0) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Grace minutes cannot be negative");
                }

                if (request.getWorkingHours() != null
                                && request.getWorkingHours() <= 0) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Working hours must be greater than zero");
                }

                if (request.getLateAfter() != null
                                && request.getLateAfter().isBefore(request.getStartTime())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Late time cannot be before start time");
                }

                if (request.getSalaryDeductionAfter() != null
                                && request.getSalaryDeductionAfter()
                                                .isBefore(request.getStartTime())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Salary deduction time cannot be before start time");
                }

                if (request.getEarlyDeductionBefore() != null
                                && request.getEarlyDeductionBefore()
                                                .isAfter(request.getEndTime())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Early deduction time cannot be after end time");
                }

                if (request.getHalfDayBoundary() != null) {
                        LocalTime boundary = request.getHalfDayBoundary();

                        if (!boundary.isAfter(request.getStartTime())
                                        || !boundary.isBefore(request.getEndTime())) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Half-day boundary must be between start and end time");
                        }
                }
        }

        private boolean isAutomaticLateAfter(
                        LocalTime oldStart,
                        Integer oldGrace,
                        LocalTime oldLateAfter) {

                if (oldStart == null || oldLateAfter == null) {
                        return true;
                }

                int grace = oldGrace == null ? 5 : oldGrace;
                return oldLateAfter.equals(oldStart.plusMinutes(grace));
        }

        private boolean isAutomaticSalaryDeduction(
                        LocalTime oldStart,
                        LocalTime oldSalaryDeduction) {

                if (oldStart == null || oldSalaryDeduction == null) {
                        return true;
                }

                return oldSalaryDeduction.equals(oldStart.plusMinutes(30));
        }

        private boolean isAutomaticEarlyDeduction(
                        LocalTime oldEnd,
                        LocalTime oldEarlyDeduction) {

                if (oldEnd == null || oldEarlyDeduction == null) {
                        return true;
                }

                return oldEarlyDeduction.equals(oldEnd.minusMinutes(30));
        }

        private boolean isAutomaticHalfDay(
                        LocalTime oldStart,
                        LocalTime oldEnd,
                        LocalTime oldHalfDay) {

                if (oldStart == null || oldEnd == null || oldHalfDay == null) {
                        return true;
                }

                return oldHalfDay.equals(calculateMidpoint(oldStart, oldEnd));
        }

        private boolean isAutomaticWorkingHours(
                        LocalTime oldStart,
                        LocalTime oldEnd,
                        Double oldWorkingHours) {

                if (oldStart == null || oldEnd == null || oldWorkingHours == null) {
                        return true;
                }

                return Math.abs(
                                oldWorkingHours - calculateWorkingHours(oldStart, oldEnd)) < 0.0001;
        }

        private LocalTime calculateMidpoint(LocalTime start, LocalTime end) {
                long seconds = Duration.between(start, end).getSeconds();
                return start.plusSeconds(seconds / 2);
        }

        private double calculateWorkingHours(LocalTime start, LocalTime end) {
                long seconds = Duration.between(start, end).getSeconds();
                return seconds / 3600.0;
        }

        private String normalizeShiftType(String shiftType) {
                if (shiftType == null || shiftType.trim().isEmpty()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Shift name is required");
                }

                String normalized = shiftType.trim()
                                .toUpperCase(Locale.ROOT)
                                .replaceAll("[\\s-]+", "_");

                if (!normalized.matches("[A-Z0-9_]+")) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Shift name can contain only letters, numbers, spaces and hyphens");
                }

                return normalized;
        }
}
