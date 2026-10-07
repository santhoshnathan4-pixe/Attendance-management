package com.example.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure salary maths (no database). Used by PayrollService.
 *
 * payable days = present + approved sick/casual leave
 * + absent days covered by available leave balance
 * one day salary = monthly salary / working days
 * salary = one day salary x payable days
 */
public final class PayrollMath {

    private PayrollMath() {
    }

    public record Result(
            int workingDays,
            int presentDays,
            double paidLeaveDays,
            double autoCoveredDays,
            double paidDays,
            double lopDays,
            double perDaySalary,
            double lossOfPay,
            double finalSalary) {
    }

    public static Result calculate(
            List<LocalDate> workingDates,
            Set<LocalDate> presentDates,
            Map<LocalDate, Double> paidLeaveByDate,
            double autoCoverCap,
            double monthlySalary) {

        int workingDays = workingDates.size();

        int presentDays = 0;
        double coverage = 0;
        double leaveOnly = 0;

        for (LocalDate date : workingDates) {

            double attendance = presentDates.contains(date) ? 1.0 : 0.0;

            double leave = Math.min(1.0,
                    paidLeaveByDate.getOrDefault(date, 0.0));

            double covered = Math.min(1.0, attendance + leave);

            if (attendance > 0) {
                presentDays++;
            }

            coverage += covered;
            leaveOnly += Math.max(0.0, covered - attendance);
        }

        double uncovered = Math.max(0.0, workingDays - coverage);

        double autoCovered = Math.min(uncovered, Math.max(0.0, autoCoverCap));

        double paidDays = coverage + autoCovered;

        double lopDays = Math.max(0.0, workingDays - paidDays);

        double perDay = workingDays > 0
                ? monthlySalary / workingDays
                : 0.0;

        double loss = lopDays * perDay;

        double finalSalary = monthlySalary - loss;

        return new Result(
                workingDays,
                presentDays,
                round2(leaveOnly),
                round2(autoCovered),
                round2(paidDays),
                round2(lopDays),
                round2(perDay),
                round2(loss),
                round2(finalSalary));
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}