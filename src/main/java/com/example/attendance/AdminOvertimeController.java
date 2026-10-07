package com.example.attendance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/overtime")
public class AdminOvertimeController {

    private final OvertimeRecordRepository overtimeRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final WorkingDaySettingRepository workingDaySettingRepository;

    public AdminOvertimeController(
            OvertimeRecordRepository overtimeRecordRepository,
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            WorkingDaySettingRepository workingDaySettingRepository) {

        this.overtimeRecordRepository = overtimeRecordRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.workingDaySettingRepository = workingDaySettingRepository;
    }


    // =====================================================
    // MONTHLY OT RECORDS
    // =====================================================

    @GetMapping
    public ResponseEntity<?> getMonthlyOvertime(
            @RequestParam int year,
            @RequestParam int month) {

        try {

            YearMonth yearMonth =
                    YearMonth.of(year, month);

            LocalDate start =
                    yearMonth.atDay(1);

            LocalDate end =
                    yearMonth.atEndOfMonth();

            List<OvertimeRecord> records =
                    overtimeRecordRepository
                            .findByOtDateBetweenOrderByOtDateAsc(
                                    start,
                                    end
                            );

            return ResponseEntity.ok(records);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Unable to load overtime records"
                    ));
        }
    }


    // =====================================================
    // GENERATE OT ELIGIBLE RECORDS
    //
    // Full day on:
    // GOVERNMENT_HOLIDAY
    // COMPANY_HOLIDAY
    // WEEKLY_OFF
    //
    // = OT
    //
    // Half day = No OT
    // =====================================================

    @PostMapping("/generate")
    public ResponseEntity<?> generateOvertime(
            @RequestParam int year,
            @RequestParam int month) {

        try {

            YearMonth yearMonth =
                    YearMonth.of(year, month);

            LocalDate start =
                    yearMonth.atDay(1);

            LocalDate end =
                    yearMonth.atEndOfMonth();


            List<WorkingDaySetting> settings =
                    workingDaySettingRepository
                            .findBySettingDateBetweenAndActiveTrueOrderBySettingDateAsc(
                                    start,
                                    end
                            );


            for (WorkingDaySetting setting : settings) {

                if (setting == null ||
                        setting.getSettingDate() == null ||
                        setting.getSettingType() == null) {
                    continue;
                }

                String dayType =
                        setting.getSettingType()
                                .trim()
                                .toUpperCase();

                if (!dayType.equals("GOVERNMENT_HOLIDAY") &&
                        !dayType.equals("COMPANY_HOLIDAY") &&
                        !dayType.equals("WEEKLY_OFF")) {
                    continue;
                }


                LocalDate date =
                        setting.getSettingDate();


                List<Attendance> attendanceList =
                        attendanceRepository
                                .findByAttendanceDateOrderByAttendanceDateDesc(
                                        date
                                );


                for (Employee employee :
                        employeeRepository.findAll()) {

                    if (!Boolean.TRUE.equals(
                            employee.getActive())) {
                        continue;
                    }

                    if ("ADMIN".equalsIgnoreCase(
                            employee.getRole())) {
                        continue;
                    }


                    Integer employeeId =
                            employee.getId();


                    boolean alreadyExists =
                            overtimeRecordRepository
                                    .findByEmployeeIdAndOtDate(
                                            employeeId,
                                            date
                                    )
                                    .isPresent();

                    if (alreadyExists) {
                        continue;
                    }


                    long totalMinutes = 0;

                    for (Attendance attendance :
                            attendanceList) {

                        if (attendance.getEmployeeId() == null ||
                                !attendance.getEmployeeId()
                                        .equals(employeeId)) {
                            continue;
                        }

                        if (attendance.getCheckIn() == null ||
                                attendance.getCheckOut() == null) {
                            continue;
                        }


                        long minutes =
                                java.time.Duration.between(
                                        attendance.getCheckIn(),
                                        attendance.getCheckOut()
                                ).toMinutes();

                        if (minutes > 0) {
                            totalMinutes += minutes;
                        }
                    }


                    /*
                     * Full working day means the employee
                     * completed the configured shift hours.
                     *
                     * Shift-specific working hours will be
                     * integrated through PayrollService later.
                     *
                     * For now:
                     * 8 hours = full day.
                     */

                    boolean fullDay =
                            totalMinutes >= 8 * 60;

                    boolean halfDay =
                            totalMinutes >= 4 * 60 &&
                            totalMinutes < 8 * 60;


                    if (!fullDay) {
                        continue;
                    }


                    OvertimeRecord record =
                            new OvertimeRecord();

                    record.setEmployeeId(employeeId);
                    record.setOtDate(date);
                    record.setDayType(dayType);
                    record.setWorkType("FULL_DAY");
                    record.setOtEligible(true);
                    record.setBenefitType(null);
                    record.setOtAmount(0.0);
                    record.setCompOffDays(0.0);
                    record.setStatus("PENDING");
                    record.setAdminEmail(null);
                    record.setReason(null);

                    LocalDateTime now =
                            LocalDateTime.now();

                    record.setCreatedAt(now);
                    record.setUpdatedAt(now);

                    overtimeRecordRepository.save(record);
                }
            }


            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Overtime records generated successfully"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Unable to generate overtime"
                    ));
        }
    }


    // =====================================================
    // UPDATE OT BENEFIT
    //
    // EXTRA_SALARY
    // COMP_OFF
    // =====================================================

    @PutMapping("/{id}/benefit")
    public ResponseEntity<?> updateBenefit(
            @PathVariable Long id,
            @RequestBody OvertimeBenefitRequest request) {

        try {

            OvertimeRecord record =
                    overtimeRecordRepository
                            .findById(id)
                            .orElse(null);

            if (record == null) {

                return ResponseEntity
                        .notFound()
                        .build();
            }


            if (!Boolean.TRUE.equals(
                    record.getOtEligible())) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "message",
                                "This attendance is not eligible for overtime"
                        ));
            }


            String benefitType =
                    request.getBenefitType() != null
                            ? request.getBenefitType()
                                    .trim()
                                    .toUpperCase()
                            : "";


            if (!benefitType.equals("EXTRA_SALARY") &&
                    !benefitType.equals("COMP_OFF")) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "message",
                                "Benefit type must be EXTRA_SALARY or COMP_OFF"
                        ));
            }


            if (request.getAdminEmail() == null ||
                    request.getAdminEmail().isBlank()) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "message",
                                "Admin email is required"
                        ));
            }


            if (request.getReason() == null ||
                    request.getReason().isBlank()) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "message",
                                "Reason is required"
                        ));
            }


            record.setBenefitType(
                    benefitType
            );


            if (benefitType.equals("EXTRA_SALARY")) {

                record.setCompOffDays(0.0);

                /*
                 * OT amount will be calculated from the
                 * employee salary in PayrollService.
                 */
                record.setOtAmount(0.0);

            } else {

                record.setOtAmount(0.0);
                record.setCompOffDays(1.0);
            }


            record.setAdminEmail(
                    request.getAdminEmail().trim()
            );

            record.setReason(
                    request.getReason().trim()
            );

            record.setStatus("APPLIED");
            record.setUpdatedAt(
                    LocalDateTime.now()
            );


            overtimeRecordRepository.save(record);


            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Overtime benefit updated successfully",
                            "benefitType",
                            benefitType
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Unable to update overtime benefit"
                    ));
        }
    }


    // =====================================================
    // REQUEST DTO
    // =====================================================

    public static class OvertimeBenefitRequest {

        private String benefitType;
        private String adminEmail;
        private String reason;


        public String getBenefitType() {
            return benefitType;
        }

        public String getAdminEmail() {
            return adminEmail;
        }

        public String getReason() {
            return reason;
        }


        public void setBenefitType(
                String benefitType) {

            this.benefitType = benefitType;
        }

        public void setAdminEmail(
                String adminEmail) {

            this.adminEmail = adminEmail;
        }

        public void setReason(
                String reason) {

            this.reason = reason;
        }
    }
}