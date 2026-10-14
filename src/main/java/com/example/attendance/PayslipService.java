
package com.example.attendance;

import org.springframework.stereotype.Service;

import java.time.Month;
import java.util.Locale;

@Service
public class PayslipService {

    private final PayrollService payrollService;

    public PayslipService(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    public PayslipResponse generatePayslip(
            Integer employeeId,
            int year,
            int month) {

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException(
                    "Invalid month."
            );
        }

        PayrollService.MonthPayroll payroll =
                payrollService.calculate(
                        year,
                        month
                );

        PayrollService.EmployeePayroll employeePayroll =
                payroll.employees()
                        .stream()
                        .filter(item ->
                                item.employee()
                                        .getId()
                                        .equals(employeeId)
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Salary data not found for the selected employee."
                                )
                        );

        Employee employee =
                employeePayroll.employee();

        PayrollMath.Result math =
                employeePayroll.math();

        PayslipResponse response =
                new PayslipResponse();

        // =========================================
        // EMPLOYEE DETAILS
        // =========================================

        response.setEmployeeId(
                employee.getId()
        );

        response.setEmployeeCode(
                employee.getEmployeeCode()
        );

        response.setEmployeeName(
                employee.getName()
        );

        response.setEmail(
                employee.getEmail()
        );

        response.setDesignation(
                employee.getDesignation()
        );

        response.setTechnology(
                employee.getTechnology()
        );

        response.setRole(
                employee.getRole()
        );

        response.setJoiningDate(
                employee.getJoiningDate()
        );

        // =========================================
        // PAY PERIOD
        // =========================================

        response.setYear(year);

        response.setMonth(month);

        response.setMonthName(
                Month.of(month)
                        .getDisplayName(
                                java.time.format.TextStyle.FULL,
                                Locale.ENGLISH
                        )
        );

        // =========================================
        // ATTENDANCE SUMMARY
        // =========================================

        response.setWorkingDays(
                math.workingDays()
        );

        response.setPresentDays(
                math.presentDays()
        );

        response.setPaidLeaveDays(
                math.paidLeaveDays()
        );

        response.setAutoCoveredDays(
                math.autoCoveredDays()
        );

        response.setPaidDays(
                math.paidDays()
        );

        response.setLopDays(
                math.lopDays()
        );

        // =========================================
        // SALARY
        // =========================================

        response.setMonthlySalary(
                PayrollMath.round2(
                        employee.getSalary() == null
                                ? 0.0
                                : employee.getSalary()
                )
        );

        response.setPerDaySalary(
                math.perDaySalary()
        );

        response.setLossOfPay(
                math.lossOfPay()
        );

        response.setSalaryBeforeOvertime(
                math.finalSalary()
        );

        // =========================================
        // OVERTIME / COMP-OFF
        // =========================================

        response.setOvertimeDays(
                employeePayroll.overtimeDays()
        );

        response.setOvertimeSalary(
                PayrollMath.round2(
                        employeePayroll.overtimeSalary()
                )
        );

        response.setCompensatoryOffDays(
                PayrollMath.round2(
                        employeePayroll.compensatoryOffDays()
                )
        );

        // =========================================
        // FINAL SALARY
        // Existing PayrollService value
        // =========================================

        response.setFinalSalary(
                PayrollMath.round2(
                        employeePayroll.finalSalaryWithOvertime()
                )
        );

        return response;
    }
}

