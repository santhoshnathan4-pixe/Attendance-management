package com.example.attendance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/monthly-report")
public class MonthlyAttendanceController {

    private final PayrollService payrollService;

    public MonthlyAttendanceController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // Working days come from Working Day Settings,
    // same value as the Salary page.
    @GetMapping
    public List<MonthlyAttendanceResponse> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {

        PayrollService.MonthPayroll payroll =
                payrollService.calculate(year, month);

        return payroll.employees()
                .stream()
                .map(p -> new MonthlyAttendanceResponse(
                        p.employee().getId(),
                        p.employee().getEmployeeCode(),
                        p.employee().getName(),
                        p.math().presentDays(),
                        p.sickLeave(),
                        p.casualLeave(),
                        p.halfDay(),
                        p.permission(),
                        p.absentDays(),
                        p.lopLeave(),
                        p.math().autoCoveredDays(),
                        payroll.workingDays()))
                .toList();
    }
}