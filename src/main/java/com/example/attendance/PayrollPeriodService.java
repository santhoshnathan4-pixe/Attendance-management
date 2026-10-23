package com.example.attendance;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Gives the exact salary dates for a pay month.
 * Admin set period -> those dates. Not set -> 1st to last day of the month.
 */
@Service
public class PayrollPeriodService {

    public record Period(
            LocalDate start,
            LocalDate end,
            boolean custom) {
    }

    private final PayrollPeriodRepository periodRepository;

    public PayrollPeriodService(PayrollPeriodRepository periodRepository) {
        this.periodRepository = periodRepository;
    }

    public Period resolve(
            int year,
            int month) {

        return periodRepository
                .findByPayYearAndPayMonth(year, month)
                .map(saved -> new Period(
                        saved.getStartDate(),
                        saved.getEndDate(),
                        true))
                .orElseGet(() -> {

                    YearMonth yearMonth = YearMonth.of(year, month);

                    return new Period(
                            yearMonth.atDay(1),
                            yearMonth.atEndOfMonth(),
                            false);
                });
    }
}
